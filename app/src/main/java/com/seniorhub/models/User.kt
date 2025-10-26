package com.seniorhub.models

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.*


data class User(
    var id: String = "",

    // === PERSONAL INFORMATION ===
    var firstName: String = "",
    var lastName: String = "",
    var birthDate: Date? = null,
    var gender: String = "",
    var profileImageUrl: String = "",
    var username: String = "",
    var age: Int = 0,

    // === CONTACT INFORMATION ===
    var phoneNumber: String = "",
    var email: String = "",
    var houseNumberAndStreet: String = "",
    var barangay: String = "",
    var city: String = "Davao City",
    var province: String = "Davao Del Sur",
    var zipCode: String = "8000",

    // === PERSONAL DETAILS ===
    var maritalStatus: String = "",
    var sssNumber: String = "",
    var gsisNumber: String = "",
    var oscaNumber: String = "",
    var philHealthNumber: String = "",

    // === EMERGENCY CONTACTS ===
    var emergencyContacts: List<EmergencyContact> = emptyList(),

    // === ACCESSIBILITY PREFERENCES ===
    var preferredLanguage: String = "en",
    var textSize: Float = 18f, // Default larger text for seniors
    var highContrastMode: Boolean = false,
    var voiceAssistanceEnabled: Boolean = true,
    var notificationsEnabled: Boolean = true,
    var largeButtonsEnabled: Boolean = true,
    var offlineModeEnabled: Boolean = false,

    // === SYSTEM FIELDS ===
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),
    var lastLogin: Timestamp = Timestamp.now(),
    var isActive: Boolean = true,
    var accountVerified: Boolean = false, // Changed to false - only admin can verify
    var isEmailVerified: Boolean = false,
    var language: String = "en",
    var isVerified: Boolean = false, // Changed to false - only admin can verify
    var medicalHistory: String = "",
    var role: String = "senior_citizen", // "senior_citizen", "family_member", "admin"
    var lastUpdatedBy: String = "user" // Track who made the last update: "user" or "admin"
) : Serializable {

    /**
     * Get user's full name
     * @return Formatted full name
     */
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }

    /**
     * Calculate user's age from birth date
     * @return Age in years or null if birth date not set
     */
    fun getAge(): Int? {
        return birthDate?.let {
            val today = Calendar.getInstance()
            val birthCalendar = Calendar.getInstance().apply { time = it }

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            // Adjust if birthday hasn't occurred this year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        }
    }

    /**
     * Get primary emergency contact
     * @return Primary emergency contact or first contact if no primary set
     */
    fun getPrimaryEmergencyContact(): EmergencyContact? {
        return emergencyContacts.find { it.isPrimary } ?: emergencyContacts.firstOrNull()
    }

    /**
     * Check if user has any medical conditions
     * @return True if user has medical conditions
     */


    /**
     * Convert user to map for Firebase storage
     * @return Map representation of user data
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "birthDate" to (birthDate?.let { Timestamp(it) } ?: ""),
            "gender" to gender,
            "profileImageUrl" to profileImageUrl,
            "username" to username,
            "age" to age,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "houseNumberAndStreet" to houseNumberAndStreet,
            "barangay" to barangay,
            "city" to city,
            "province" to province,
            "zipCode" to zipCode,
            "maritalStatus" to maritalStatus,
            "sssNumber" to sssNumber,
            "gsisNumber" to gsisNumber,
            "oscaNumber" to oscaNumber,
            "philHealthNumber" to philHealthNumber,
            "emergencyContacts" to emergencyContacts.map { ec ->
                mapOf(
                    "name" to ec.name,
                    "phoneNumber" to ec.phoneNumber,
                    "relationship" to ec.relationship,
                    "isPrimary" to ec.isPrimary,
                    "isActive" to ec.isActive
                )
            },
            "preferredLanguage" to preferredLanguage,
            "textSize" to textSize,
            "highContrastMode" to highContrastMode,
            "voiceAssistanceEnabled" to voiceAssistanceEnabled,
            "notificationsEnabled" to notificationsEnabled,
            "largeButtonsEnabled" to largeButtonsEnabled,
            "offlineModeEnabled" to offlineModeEnabled,
            "isActive" to isActive,
            "accountVerified" to accountVerified,
            "isEmailVerified" to isEmailVerified,
            "language" to language,
            "isVerified" to isVerified,
            "medicalHistory" to medicalHistory,
            "role" to role.lowercase(),
            "lastUpdatedBy" to lastUpdatedBy,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastLogin" to lastLogin
        )
    }

    /**
     * Validate user data completeness
     * @return List of missing required fields
     */
    fun validateRequiredFields(): List<String> {
        val missingFields = mutableListOf<String>()

        if (firstName.isBlank()) missingFields.add("First Name")
        if (lastName.isBlank()) missingFields.add("Last Name")
        if (email.isBlank()) missingFields.add("Email")
        if (phoneNumber.isBlank()) missingFields.add("Phone Number")

        return missingFields
    }

    /**
     * Get formatted address string
     * @return Complete formatted address
     */
    fun getFormattedAddress(): String {
        val parts = listOf(houseNumberAndStreet, barangay, city, province, zipCode).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    /**
     * Check if user profile is complete
     * @return True if all essential fields are filled
     */
    fun isProfileComplete(): Boolean {
        return validateRequiredFields().isEmpty() &&
                houseNumberAndStreet.isNotBlank() &&
                emergencyContacts.isNotEmpty()
    }
}

/**
 * Medication - Represents a medication with dosage and scheduling information
 */
data class Medication(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var dosage: String = "",
    var frequency: String = "",
    var timeOfDay: List<String> = emptyList(), // e.g., ["08:00", "20:00"]
    var instructions: String = "",
    var prescribedBy: String = "",
    var prescriptionDate: Date? = null,
    var startDate: Date? = null,
    var endDate: Date? = null,
    var reminderEnabled: Boolean = true,
    var isActive: Boolean = true,
    var sideEffects: List<String> = emptyList(),
    var notes: String = ""
) : Serializable {

    /**
     * Convert medication to map for Firebase storage
     * @return Map representation of medication
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "dosage" to dosage,
            "frequency" to frequency,
            "timeOfDay" to timeOfDay,
            "instructions" to instructions,
            "prescribedBy" to prescribedBy,
            "prescriptionDate" to (prescriptionDate?.let { Timestamp(it) } ?: ""),
            "startDate" to (startDate?.let { Timestamp(it) } ?: ""),
            "endDate" to (endDate?.let { Timestamp(it) } ?: ""),
            "reminderEnabled" to reminderEnabled,
            "isActive" to isActive,
            "sideEffects" to sideEffects,
            "notes" to notes
        )
    }

    /**
     * Check if medication is currently active
     * @return True if medication should be taken now
     */
    fun isCurrentlyActive(): Boolean {
        val now = Date()
        val startOk = startDate?.let { now.after(it) } ?: true
        val endOk = endDate?.let { now.before(it) } ?: true
        return isActive && startOk && endOk
    }

    /**
     * Get formatted medication summary
     * @return Human-readable medication description
     */
    fun getSummary(): String {
        return "$name - $dosage ($frequency)"
    }

    /**
     * Get next scheduled time for today
     * @return Next scheduled time or null if none today
     */
    fun getNextScheduledTime(): String? {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeString = String.format("%02d:%02d", currentHour, currentMinute)

        return timeOfDay.filter { time ->
            time > currentTimeString
        }.minOrNull()
    }
}


/** Removed duplicate HealthSummary and Appointment definitions. Using separate files. */
