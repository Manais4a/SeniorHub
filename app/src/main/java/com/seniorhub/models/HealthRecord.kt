package com.seniorhub.models

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.*

/**
 * HealthRecord data model for health tracking records
 */
data class HealthRecord(
    var id: String = "",
    var seniorId: String = "",
    var seniorName: String = "",
    var type: String = "", // "blood_pressure", "blood_sugar", "weight", "heart_rate"
    var value: String = "", // The actual measurement value
    var unit: String = "", // "mmHg", "mg/dL", "kg", "bpm"
    var notes: String = "",
    var recordedBy: String = "user", // "user", "admin"
    var timestamp: Date = Date(),
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now()
) : Serializable {

    // Added to force re-compilation and ensure properties are recognized
    companion object {
        const val COLLECTION_NAME = "health_records"
    }

    /**
     * Get formatted value with unit
     */
    fun getFormattedValue(): String {
        return when {
            value.isEmpty() -> "N/A"
            unit.isEmpty() -> value
            else -> "$value $unit"
        }
    }

    /**
     * Get health type display name
     */
    fun getTypeDisplay(): String {
        return when (type) {
            "blood_pressure" -> "Blood Pressure"
            "blood_sugar" -> "Blood Sugar"
            "weight" -> "Weight"
            "heart_rate" -> "Heart Rate"
            else -> type.replace("_", " ").split(" ").joinToString(" ") { 
                it.capitalize() 
            }
        }
    }

    /**
     * Get health type icon
     */
    fun getTypeIcon(): String {
        return when (type) {
            "blood_pressure" -> "🩸"
            "blood_sugar" -> "🍯"
            "weight" -> "⚖️"
            "heart_rate" -> "❤️"
            else -> "📊"
        }
    }

    /**
     * Check if this is a blood pressure reading
     */
    fun isBloodPressure(): Boolean {
        return type == "blood_pressure"
    }

    /**
     * Check if this is a blood sugar reading
     */
    fun isBloodSugar(): Boolean {
        return type == "blood_sugar"
    }

    /**
     * Check if this is a weight reading
     */
    fun isWeight(): Boolean {
        return type == "weight"
    }

    /**
     * Check if this is a heart rate reading
     */
    fun isHeartRate(): Boolean {
        return type == "heart_rate"
    }

    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp
        return "${calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${calendar.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    }

    /**
     * Get formatted date
     */
    fun getFormattedDate(): String {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
    }

    /**
     * Check if this record is recent (within last 24 hours)
     */
    fun isRecent(): Boolean {
        val now = Date()
        val diffMillis = now.time - timestamp.time
        val hours24 = 24 * 60 * 60 * 1000L
        return diffMillis <= hours24
    }

    /**
     * Get formatted display value with proper units
     */
    fun getDisplayValue(): String {
        return when (type.lowercase()) {
            "blood_pressure" -> "$value mmHg"
            "heart_rate" -> "$value bpm"
            "blood_sugar" -> "$value mg/dL"
            "weight" -> "$value $unit"
            else -> if (unit.isNotBlank()) "$value $unit" else value
        }
    }

    /**
     * Check if this record has valid data
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && 
               seniorId.isNotBlank() && 
               type.isNotBlank() && 
               value.isNotBlank()
    }

    /**
     * Get age of this record in days
     */
    fun getAgeInDays(): Long {
        val now = Date()
        val diffMillis = now.time - timestamp.time
        return diffMillis / (24 * 60 * 60 * 1000)
    }
}
