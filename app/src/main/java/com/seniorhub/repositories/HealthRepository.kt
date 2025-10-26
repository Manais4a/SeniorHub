package com.seniorhub.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.seniorhub.models.HealthRecord
import com.seniorhub.models.HealthSummary
import com.seniorhub.models.Appointment
import com.seniorhub.utils.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * HealthRepository - Comprehensive Health Data Management
 *
 * Handles all health-related database operations including:
 * - Health records management (CRUD operations)
 * - Health metrics tracking (blood pressure, heart rate, etc.)
 * - Appointment management
 * - Health summary generation
 * - Medication tracking
 * - Real-time data synchronization for senior citizens
 */
class HealthRepository private constructor() {

    companion object {
        private const val TAG = "HealthRepository"
        private const val HEALTH_RECORDS_PATH = "health_records"
        private const val HEALTH_SUMMARY_PATH = "health_summary"
        private const val APPOINTMENTS_PATH = "appointments"
        private const val MEDICATIONS_PATH = "medications"

        @Volatile
        private var INSTANCE: HealthRepository? = null

        /**
         * Get singleton instance of HealthRepository
         */
        fun getInstance(): HealthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthRepository().also { INSTANCE = it }
            }
        }
    }

    // Simple in-memory cache for demo purposes
    private val healthRecordsCache = mutableMapOf<String, MutableList<HealthRecord>>()
    private val healthSummaryCache = mutableMapOf<String, HealthSummary>()
    private val appointmentsCache = mutableMapOf<String, MutableList<Appointment>>()

    /**
     * Save a health record for a user
     * @param userId String ID of the user
     * @param healthRecord HealthRecord to save
     * @return Result indicating success or failure
     */
    suspend fun saveHealthRecord(userId: String, healthRecord: HealthRecord): Result<String> {
        // Generate ID if not provided
        val recordId = if (healthRecord.id.isEmpty()) {
            UUID.randomUUID().toString()
        } else {
            healthRecord.id
        }

        // Create updated health record with all properties
        val updatedHealthRecord = healthRecord.copy(
            id = recordId,
            seniorId = userId,
            seniorName = userId, // In real app, get actual name
            timestamp = healthRecord.timestamp, // Keep original timestamp
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")

            // Validate required fields
            if (updatedHealthRecord.type.isBlank()) {
                return Result.Error(IllegalArgumentException("Health record type cannot be empty"))
            }
            if (updatedHealthRecord.value.isBlank()) {
                return Result.Error(IllegalArgumentException("Health record value cannot be empty"))
            }

            // Save to Firebase
            val docRef = healthRecordsCollection.document(recordId)
            docRef.set(updatedHealthRecord).await()

            // Update health summary
            updateHealthSummary(userId, updatedHealthRecord)

            Log.d(TAG, "Health record saved successfully to Firebase: $recordId for user: $userId")
            Result.Success(recordId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving health record to Firebase: ${e.message}", e)
            
            // Fallback: Try to save to local cache if Firebase fails
            try {
                val userRecords = healthRecordsCache.getOrPut(userId) { mutableListOf() }
                userRecords.add(updatedHealthRecord)
                Log.d(TAG, "Health record saved to local cache as fallback: $recordId")
                Result.Success(recordId)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Error saving to local cache: ${fallbackError.message}", fallbackError)
                Result.Error(e) // Return original Firebase error
            }
        }
    }

    /**
     * Get all health records for a user
     * @param userId String ID of the user
     * @return Result with list of health records
     */
    suspend fun getHealthRecords(userId: String): Result<List<HealthRecord>> {
        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")

            val querySnapshot = healthRecordsCollection
                .whereEqualTo("seniorId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val records = mutableListOf<HealthRecord>()
            for (document in querySnapshot) {
                try {
                    val healthRecord = document.toObject(HealthRecord::class.java)
                    healthRecord.id = document.id
                    records.add(healthRecord)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing health record document ${document.id}: ${e.message}")
                    continue
                }
            }

            Log.d(TAG, "Retrieved ${records.size} health records from Firebase for user: $userId")
            Result.Success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving health records from Firebase: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get health summary for a user
     * @param userId String ID of the user
     * @return Result with health summary
     */
    suspend fun getHealthSummary(userId: String): Result<HealthSummary> {
        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            // Generate summary from recent health records
            val recordsResult = getHealthRecords(userId)
            if (recordsResult is Result.Success) {
                val records = recordsResult.data
                val summary = generateHealthSummaryFromRecords(userId, records)
                Log.d(TAG, "Retrieved health summary for user: $userId")
                Result.Success(summary)
            } else {
                // Return default summary if no records found
                val defaultSummary = generateDefaultHealthSummary(userId)
                Result.Success(defaultSummary)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving health summary: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get latest health records for each type for the metrics grid
     * @param userId String ID of the user
     * @return Result with latest health records by type
     */
    suspend fun getLatestHealthRecords(userId: String): Result<Map<String, HealthRecord>> {
        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")

            val querySnapshot = healthRecordsCollection
                .whereEqualTo("seniorId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val records = mutableListOf<HealthRecord>()
            for (document in querySnapshot) {
                try {
                    val healthRecord = document.toObject(HealthRecord::class.java)
                    healthRecord.id = document.id
                    records.add(healthRecord)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing health record document ${document.id}: ${e.message}")
                    continue
                }
            }

            // Get latest record for each type
            val latestRecords = mutableMapOf<String, HealthRecord>()
            val types = listOf("blood_pressure", "blood_sugar", "weight", "heart_rate")
            
            types.forEach { type ->
                val latestRecord = records.filter { it.type == type }.maxByOrNull { it.timestamp }
                if (latestRecord != null) {
                    latestRecords[type] = latestRecord
                }
            }

            Log.d(TAG, "Retrieved latest health records for user: $userId")
            Result.Success(latestRecords)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving latest health records: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get upcoming appointments for a user
     * @param userId String ID of the user
     * @return Result with list of upcoming appointments
     */
    suspend fun getUpcomingAppointments(userId: String): Result<List<Appointment>> {
        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            // Simulate network delay
            delay(200)

            // Get from cache or create sample data
            val allAppointments = appointmentsCache[userId] ?: generateSampleAppointments(userId)

            // Filter for upcoming appointments
            val currentTime = Date()
            val upcomingAppointments = allAppointments
                .filter { it.dateTime > currentTime.time } // Fixed: Use Long comparison instead of after()
                .sortedBy { it.dateTime }
                .take(10) // Limit to next 10 appointments

            Log.d(
                TAG,
                "Retrieved ${upcomingAppointments.size} upcoming appointments for user: $userId"
            )
            Result.Success(upcomingAppointments)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving appointments: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Save an appointment
     * @param userId String ID of the user
     * @param appointment Appointment to save
     * @return Result indicating success or failure
     */
    suspend fun saveAppointment(userId: String, appointment: Appointment): Result<String> {
        return try {
            if (userId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID cannot be empty"))
            }

            // Generate ID if not provided
            val appointmentId = if (appointment.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                appointment.id
            }

            // Set appointment properties
            appointment.id = appointmentId
            appointment.userId = userId
            appointment.createdAt = Date()

            // Simulate network delay
            delay(300)

            // Save to cache
            val userAppointments = appointmentsCache.getOrPut(userId) { mutableListOf() }

            // Remove existing appointment with same ID if updating
            userAppointments.removeAll { it.id == appointmentId }
            userAppointments.add(appointment)

            Log.d(TAG, "Appointment saved successfully: $appointmentId for user: $userId")
            Result.Success(appointmentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving appointment: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Delete a health record
     * @param userId String ID of the user
     * @param recordId String ID of the record to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteHealthRecord(userId: String, recordId: String): Result<Unit> {
        return try {
            if (userId.isBlank() || recordId.isBlank()) {
                return Result.Error(IllegalArgumentException("User ID and Record ID cannot be empty"))
            }

            // Simulate network delay
            delay(200)

            // Remove from cache
            val userRecords = healthRecordsCache[userId]
            val removed = userRecords?.removeAll { it.id == recordId } == true

            if (removed) {
                Log.d(TAG, "Health record deleted successfully: $recordId")
                Result.Success(Unit)
            } else {
                Log.w(TAG, "Health record not found for deletion: $recordId")
                Result.Error(Exception("Health record not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting health record: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get health record statistics for a user
     * @param userId String ID of the user
     * @return Result with health statistics
     */
    suspend fun getHealthStatistics(userId: String): Result<Map<String, Int>> {
        return try {
            val recordsResult = getHealthRecords(userId)
            if (recordsResult is Result.Success) {
                val records = recordsResult.data
                val stats = mutableMapOf<String, Int>()

                // Count records by type
                records?.groupBy { it.type }?.forEach { (type, typeRecords) ->
                    stats[type] = typeRecords.size
                }

                // Add total count
                if (records != null) {
                    stats["total"] = records.size
                }

                Result.Success(stats.toMap())
            } else {
                Result.Error(Exception("Failed to get health records"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting health statistics: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Clear all cached data (useful for logout or testing)
     */
    fun clearCache() {
        healthRecordsCache.clear()
        healthSummaryCache.clear()
        appointmentsCache.clear()
        Log.d(TAG, "Health repository cache cleared")
    }

    /**
     * Update health summary based on new health record
     */
    private suspend fun updateHealthSummary(userId: String, healthRecord: HealthRecord) {
        try {
            // Health summary will be generated dynamically from records
            // No need to maintain cache since we're using Firebase
            Log.d(TAG, "Health record saved for user: $userId, summary will be generated on demand")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update health summary: ${e.message}", e)
        }
    }

    /**
     * Generate health summary from existing records
     */
    private fun generateHealthSummaryFromRecords(userId: String, records: List<HealthRecord>?): HealthSummary {
        return try {
            val latestBP = records?.find { it.type.lowercase() == "blood_pressure" }?.value ?: "N/A"
            val latestHR = records?.find { it.type.lowercase() == "heart_rate" }?.value ?: "N/A"
            val latestBS = records?.find { it.type.lowercase() == "blood_sugar" }?.value ?: "N/A"
            val latestWeight = records?.find { it.type.lowercase() == "weight" }?.value ?: "N/A"

            HealthSummary(
                userId = userId,
                bloodPressure = latestBP,
                heartRate = latestHR,
                bloodSugar = latestBS,
                weight = latestWeight,
                lastUpdated = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health summary: ${e.message}", e)
            generateDefaultHealthSummary(userId)
        }
    }

    /**
     * Generate default health summary
     */
    private fun generateDefaultHealthSummary(userId: String): HealthSummary {
        return HealthSummary(
            userId = userId,
            bloodPressure = "N/A",
            heartRate = "N/A",
            bloodSugar = "N/A",
            weight = "N/A",
            lastUpdated = Date()
        )
    }

    /**
     * Generate sample health records for demonstration
     */
    private fun generateSampleHealthRecords(userId: String): List<HealthRecord> {
        val sampleRecords = mutableListOf<HealthRecord>()
        val recordTypes = listOf("blood_pressure", "heart_rate", "blood_sugar", "weight")

        // Generate 10 sample records over the past week
        repeat(10) { index ->
            val type = recordTypes[index % recordTypes.size]
            val daysAgo = index.toLong()
            val timestamp = Date(System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000))

            val (value, unit) = when (type) {
                "blood_pressure" -> Pair("${120 + (index * 2)}/${80 + index}", "mmHg")
                "heart_rate" -> Pair("${70 + index}", "bpm")
                "blood_sugar" -> Pair("${95 + index}", "mg/dL")
                "weight" -> Pair("${65 + (index * 0.1)}", "kg")
                else -> Pair("Normal", "")
            }
        }

        // Cache the sample records
        healthRecordsCache[userId] = sampleRecords.toMutableList()

        return sampleRecords
    }

    /**
     * Generate sample appointments for demonstration
     */
    private fun generateSampleAppointments(userId: String): MutableList<Appointment> {
        val sampleAppointments = mutableListOf<Appointment>()
        val doctors = listOf(
            "Dr. Maria Santos" to "Cardiologist",
            "Dr. Jose Cruz" to "General Physician",
            "Dr. Elena Rodriguez" to "Neurologist"
        )

        // Generate 5 future appointments
        repeat(5) { index ->
            val daysFromNow = (index + 1) * 7L // Weekly appointments
            val appointmentTime = System.currentTimeMillis() + (daysFromNow * 24 * 60 * 60 * 1000) // Fixed: Use Long timestamp
            val (doctorName, specialty) = doctors[index % doctors.size]

            sampleAppointments.add(
                Appointment(
                    id = "appointment_${userId}_$index",
                    userId = userId,
                    doctorName = doctorName,
                    doctorSpecialty = specialty, // Fixed: Use correct parameter name
                    dateTime = appointmentTime, // Fixed: Use Long timestamp
                    facilityName = "Medical Center ${index + 1}", // Fixed: Use correct parameter name
                    description = "Regular checkup appointment", // Fixed: Use correct parameter name
                    status = "scheduled",
                    reminderEnabled = true, // Fixed: Use correct parameter name
                    createdAt = Date()
                )
            )
        }

        // Cache the sample appointments
        appointmentsCache[userId] = sampleAppointments

        return sampleAppointments
    }
}