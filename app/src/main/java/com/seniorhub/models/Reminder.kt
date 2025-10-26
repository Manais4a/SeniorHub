package com.seniorhub.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * Reminder Model
 * Represents various types of reminders for senior citizens
 */
@Parcelize
data class Reminder(
    val id: String = "",
    val userId: String = "",
    val seniorUserName: String = "", // Senior user's name for personalization
    val title: String = "",
    val description: String = "",
    val type: ReminderType = ReminderType.MEDICATION,
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    val scheduledTime: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurrencePattern: RecurrencePattern = RecurrencePattern.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: List<Int> = emptyList(), // 0=Sunday, 1=Monday, etc.
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Long = 0,
    val snoozeCount: Int = 0,
    val maxSnoozes: Int = 3,
    val snoozeInterval: Int = 15, // minutes
    val nextReminderTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "user", // "user", "family", "doctor", "system"
    val category: String = "",
    val tags: List<String> = emptyList(),
    val location: String = "",
    val isLocationBased: Boolean = false,
    val locationRadius: Int = 100, // meters
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val voiceReminder: Boolean = false,
    val voiceMessage: String = "",
    val attachmentUrl: String = "",
    val reminderData: Map<String, String> = emptyMap(), // Additional data specific to reminder type
    
    // New fields based on activity_reminders.xml UI
    val medicineName: String = "", // For medication reminders
    val medicineFrequency: String = "", // e.g., "Once daily", "Twice daily"
    val doctorName: String = "", // For appointment reminders
    val personName: String = "", // For birthday reminders
    val relationship: String = "", // For birthday reminders (family, friend, etc.)
    val exerciseActivity: String = "", // For exercise reminders
    val exerciseDuration: String = "", // For exercise reminders (e.g., "30 minutes")
    val selectedDate: Long = 0, // Selected date for the reminder
    val selectedTime: Long = 0, // Selected time for the reminder
    val reminderStatus: ReminderStatus = ReminderStatus.ACTIVE, // Current status of the reminder
    val lastTriggered: Long = 0, // When the reminder was last triggered
    val triggerCount: Int = 0, // How many times the reminder has been triggered
    val isEnabled: Boolean = true, // Whether the reminder is enabled (toggle switch)
    val notificationId: Int = 0, // Android notification ID
    val alarmId: Int = 0, // Android alarm ID
    val reminderColor: String = "#007bff", // Color for the reminder card
    val reminderIcon: String = "ic_notification", // Icon for the reminder
    val notes: String = "", // Additional notes for the reminder
    val isUrgent: Boolean = false, // Whether the reminder is urgent
    val reminderGroup: String = "", // Group reminders together
    val estimatedDuration: Int = 0, // Estimated duration in minutes
    val preparationTime: Int = 0, // Preparation time in minutes before the actual reminder
    val followUpReminder: Boolean = false, // Whether to send a follow-up reminder
    val followUpTime: Long = 0, // Time for follow-up reminder
    val reminderMethod: ReminderMethod = ReminderMethod.NOTIFICATION, // How to deliver the reminder
    val customSound: String = "", // Custom sound file for the reminder
    val reminderTemplate: String = "", // Template used to create this reminder
    val familyNotification: Boolean = false, // Whether to notify family members
    val familyMembers: List<String> = emptyList(), // List of family member IDs to notify
    val reminderHistory: List<ReminderHistory> = emptyList() // History of reminder interactions
) : Parcelable

@Parcelize
enum class ReminderType : Parcelable {
    MEDICATION,
    APPOINTMENT,
    BIRTHDAY,
    EXERCISE,
    MEAL,
    HYDRATION,
    BLOOD_PRESSURE_CHECK,
    BLOOD_SUGAR_CHECK,
    WEIGHT_CHECK,
    VITAMIN,
    DOCTOR_VISIT,
    PHARMACY_PICKUP,
    BILL_PAYMENT,
    SOCIAL_ACTIVITY,
    FAMILY_CALL,
    FRIEND_VISIT,
    RELIGIOUS_SERVICE,
    COMMUNITY_EVENT,
    BENEFIT_CLAIM,
    ID_RENEWAL,
    VACCINATION,
    HEALTH_CHECKUP,
    EYE_EXAM,
    DENTAL_CHECKUP,
    HEARING_TEST,
    PHYSICAL_THERAPY,
    OCCUPATIONAL_THERAPY,
    COUNSELING,
    SUPPORT_GROUP,
    VOLUNTEER_WORK,
    HOBBY_TIME,
    READING_TIME,
    GARDENING,
    WALKING,
    OTHER
}

@Parcelize
enum class ReminderPriority : Parcelable {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Parcelize
enum class RecurrencePattern : Parcelable {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

@Parcelize
enum class ReminderStatus : Parcelable {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    SNOOZED,
    EXPIRED,
    DISABLED
}

@Parcelize
enum class ReminderMethod : Parcelable {
    NOTIFICATION,
    SMS,
    EMAIL,
    PHONE_CALL,
    VOICE_MESSAGE,
    PUSH_NOTIFICATION
}

@Parcelize
data class ReminderHistory(
    val id: String = "",
    val reminderId: String = "",
    val action: ReminderAction = ReminderAction.CREATED,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val userId: String = ""
) : Parcelable

@Parcelize
enum class ReminderAction : Parcelable {
    CREATED,
    UPDATED,
    COMPLETED,
    CANCELLED,
    SNOOZED,
    TRIGGERED,
    DISABLED,
    ENABLED,
    DELETED
}

/**
 * Reminder Template Model
 * Predefined reminder templates for common activities
 */
@Parcelize
data class ReminderTemplate(
    val id: String = "",
    val name: String = "",
    val type: ReminderType = ReminderType.MEDICATION,
    val title: String = "",
    val description: String = "",
    val defaultDuration: Int = 30, // minutes
    val defaultPriority: ReminderPriority = ReminderPriority.MEDIUM,
    val isRecurring: Boolean = false,
    val defaultRecurrencePattern: RecurrencePattern = RecurrencePattern.NONE,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val iconName: String = "",
    val color: String = "#007bff",
    val isActive: Boolean = true,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Reminder Statistics Model
 * Statistics and analytics for reminders
 */
@Parcelize
data class ReminderStats(
    val userId: String = "",
    val period: String = "", // "daily", "weekly", "monthly"
    val startDate: Long = 0,
    val endDate: Long = 0,
    val totalReminders: Int = 0,
    val completedReminders: Int = 0,
    val missedReminders: Int = 0,
    val snoozedReminders: Int = 0,
    val completionRate: Double = 0.0,
    val averageResponseTime: Long = 0, // milliseconds
    val mostMissedType: ReminderType = ReminderType.MEDICATION,
    val mostCompletedType: ReminderType = ReminderType.MEDICATION,
    val bestDay: String = "",
    val worstDay: String = "",
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable

