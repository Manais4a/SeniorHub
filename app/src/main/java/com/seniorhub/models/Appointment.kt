package com.seniorhub.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Appointment - Represents a medical appointment for senior citizens
 *
 * This model manages all aspects of medical appointments including:
 * - Scheduling and timing information
 * - Healthcare provider details
 * - Appointment type and purpose
 * - Reminder and notification settings
 * - Location and contact information
 */
@Parcelize
data class Appointment(
    var id: String = "",

    // Basic appointment information
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var appointmentType: String = "", // "checkup", "specialist", "emergency", "follow_up"
    var status: String = "scheduled", // "scheduled", "confirmed", "completed", "cancelled", "missed"

    // Healthcare provider information
    var doctorName: String = "",
    var doctorSpecialty: String = "",
    var facilityName: String = "",
    var facilityAddress: String = "",
    var facilityPhone: String = "",
    var doctorNotes: String = "",

    // Scheduling details
    var dateTime: Long = 0, // Unix timestamp
    var duration: Int = 30, // Duration in minutes
    var timeZone: String = "",
    var isRecurring: Boolean = false,
    var recurringPattern: String = "", // "weekly", "monthly", "yearly"
    var recurringEndDate: Date? = null,

    // Location and logistics
    var roomNumber: String = "",
    var department: String = "",
    var parkingInfo: String = "",
    var specialInstructions: String = "",
    var preparationNotes: String = "",

    // Insurance and billing
    var insuranceRequired: Boolean = true,
    var copayAmount: Double = 0.0,
    var authorizationNumber: String = "",
    var referralRequired: Boolean = false,

    // Reminders and notifications
    var reminderEnabled: Boolean = true,
    var reminderTime: List<Int> = listOf(1440, 60), // Minutes before appointment
    var notificationSent: Boolean = false,
    var confirmationRequired: Boolean = false,
    var confirmed: Boolean = false,
    var confirmationDeadline: Date? = null,

    // Transportation
    var transportationNeeded: Boolean = false,
    var transportationType: String = "", // "family", "taxi", "medical_transport", "public"
    var transportationBooked: Boolean = false,
    var transportationNotes: String = "",

    // Follow-up and results
    var followUpRequired: Boolean = false,
    var followUpDate: Date? = null,
    var resultsPending: Boolean = false,
    var resultsReceived: Boolean = false,
    var resultsSummary: String = "",

    // Emergency contact for appointment
    var emergencyContactName: String = "",
    var emergencyContactPhone: String = "",

    // System fields
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var isActive: Boolean = true,
    var isSynced: Boolean = false
) : Parcelable {

    companion object {
        private const val DATE_FORMAT = "MMM dd, yyyy"
        private const val TIME_FORMAT = "h:mm a"

        // Appointment types
        const val TYPE_CHECKUP = "checkup"
        const val TYPE_SPECIALIST = "specialist"
        const val TYPE_EMERGENCY = "emergency"
        const val TYPE_FOLLOW_UP = "follow_up"

        // Appointment statuses
        const val STATUS_SCHEDULED = "scheduled"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_MISSED = "missed"
    }

    /**
     * Get formatted appointment date and time
     * @return Human-readable date and time string
     */
    fun getFormattedDateTime(): String {
        if (dateTime == 0L) return "Not scheduled"

        val date = Date(dateTime)
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())

        return "${dateFormat.format(date)} at ${timeFormat.format(date)}"
    }

    /**
     * Get appointment date only
     * @return Formatted date string
     */
    fun getFormattedDate(): String {
        if (dateTime == 0L) return "Not scheduled"

        val date = Date(dateTime)
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(date)
    }

    /**
     * Get appointment time only
     * @return Formatted time string
     */
    fun getFormattedTime(): String {
        if (dateTime == 0L) return "Not scheduled"

        val date = Date(dateTime)
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        return timeFormat.format(date)
    }

    /**
     * Check if appointment is upcoming
     * @return True if appointment is in the future
     */
    fun isUpcoming(): Boolean {
        return dateTime > System.currentTimeMillis() && status == STATUS_SCHEDULED
    }

    /**
     * Check if appointment is overdue/missed
     * @return True if appointment time has passed and status is still scheduled
     */
    fun isMissed(): Boolean {
        return dateTime < System.currentTimeMillis() && status == STATUS_SCHEDULED
    }

    /**
     * Get days until appointment
     * @return Number of days until appointment (negative if past)
     */
    fun getDaysUntilAppointment(): Int {
        if (dateTime == 0L) return Int.MAX_VALUE

        val appointmentDate = Calendar.getInstance().apply {
            timeInMillis = dateTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = appointmentDate.timeInMillis - today.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Get status color for UI display
     * @return Color identifier based on appointment status
     */
    fun getStatusColor(): String {
        return when (status.lowercase()) {
            STATUS_SCHEDULED -> if (isUpcoming()) "blue" else "red"
            STATUS_CONFIRMED -> "green"
            STATUS_COMPLETED -> "gray"
            STATUS_CANCELLED -> "orange"
            STATUS_MISSED -> "red"
            else -> "gray"
        }
    }

    /**
     * Get priority level based on appointment type and timing
     * @return Priority level: "low", "medium", "high", "urgent"
     */
    fun getPriority(): String {
        val daysUntil = getDaysUntilAppointment()

        return when {
            appointmentType.lowercase() == TYPE_EMERGENCY -> "urgent"
            isMissed() -> "urgent"
            daysUntil == 0 -> "high" // Today
            daysUntil == 1 -> "high" // Tomorrow
            daysUntil <= 7 -> "medium" // This week
            else -> "low"
        }
    }

    /**
     * Check if reminder should be sent
     * @return True if reminder is due
     */
    fun shouldSendReminder(): Boolean {
        if (!reminderEnabled || notificationSent) return false

        val currentTime = System.currentTimeMillis()
        val appointmentTime = dateTime

        return reminderTime.any { reminderMinutes ->
            val reminderTime = appointmentTime - (reminderMinutes * 60 * 1000)
            currentTime >= reminderTime && currentTime < appointmentTime
        }
    }

    /**
     * Get complete appointment summary for display
     * @return Formatted appointment summary
     */
    fun getAppointmentSummary(): String {
        val dateTime = getFormattedDateTime()
        val location = if (facilityName.isNotBlank()) facilityName else "Location TBD"
        val doctor = if (doctorName.isNotBlank()) "with Dr. $doctorName" else ""

        return "$title $doctor\n$dateTime\n$location"
    }

    /**
     * Validate appointment data
     * @return List of validation errors
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (userId.isBlank()) errors.add("User ID is required")
        if (title.isBlank()) errors.add("Appointment title is required")
        if (dateTime == 0L) errors.add("Appointment date and time is required")
        if (doctorName.isBlank()) errors.add("Doctor name is required")
        if (facilityName.isBlank()) errors.add("Facility name is required")

        // Check if appointment is in the past when creating
        if (dateTime < System.currentTimeMillis() && status == STATUS_SCHEDULED) {
            errors.add("Cannot schedule appointment in the past")
        }

        return errors
    }

    /**
     * Convert to map for database storage
     * @return Map representation of appointment
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "description" to description,
            "appointmentType" to appointmentType,
            "status" to status,
            "doctorName" to doctorName,
            "doctorSpecialty" to doctorSpecialty,
            "facilityName" to facilityName,
            "facilityAddress" to facilityAddress,
            "facilityPhone" to facilityPhone,
            "doctorNotes" to doctorNotes,
            "dateTime" to dateTime,
            "duration" to duration,
            "timeZone" to timeZone,
            "isRecurring" to isRecurring,
            "recurringPattern" to recurringPattern,
            "recurringEndDate" to recurringEndDate,
            "roomNumber" to roomNumber,
            "department" to department,
            "parkingInfo" to parkingInfo,
            "specialInstructions" to specialInstructions,
            "preparationNotes" to preparationNotes,
            "insuranceRequired" to insuranceRequired,
            "copayAmount" to copayAmount,
            "authorizationNumber" to authorizationNumber,
            "referralRequired" to referralRequired,
            "reminderEnabled" to reminderEnabled,
            "reminderTime" to reminderTime,
            "notificationSent" to notificationSent,
            "confirmationRequired" to confirmationRequired,
            "confirmed" to confirmed,
            "confirmationDeadline" to confirmationDeadline,
            "transportationNeeded" to transportationNeeded,
            "transportationType" to transportationType,
            "transportationBooked" to transportationBooked,
            "transportationNotes" to transportationNotes,
            "followUpRequired" to followUpRequired,
            "followUpDate" to followUpDate,
            "resultsPending" to resultsPending,
            "resultsReceived" to resultsReceived,
            "resultsSummary" to resultsSummary,
            "emergencyContactName" to emergencyContactName,
            "emergencyContactPhone" to emergencyContactPhone,
            "isActive" to isActive,
            "isSynced" to isSynced,
            "createdAt" to createdAt,
            "updatedAt" to Date()
        )
    }
}