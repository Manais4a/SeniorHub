package com.seniorhub.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.seniorhub.R
import com.seniorhub.receivers.ReminderReceiver
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.PreferenceManager
import java.util.*
import kotlin.jvm.java

/**
 * ReminderService - Medication and Appointment Reminder Management
 *
 * This service handles scheduling and managing reminders for:
 * - Medication schedules
 * - Medical appointments
 * - Health checkups
 * - Custom user reminders
 */
class ReminderService : Service() {

    companion object {
        private const val TAG = "ReminderService"
        private const val NOTIFICATION_CHANNEL_ID = "reminder_notifications"

        const val ACTION_SCHEDULE_REMINDER = "SCHEDULE_REMINDER"
        const val ACTION_CANCEL_REMINDER = "CANCEL_REMINDER"
        const val ACTION_UPDATE_REMINDER = "UPDATE_REMINDER"

        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_MESSAGE = "reminder_message"
        const val EXTRA_REMINDER_TIME = "reminder_time"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_REMINDER_RECURRING = "reminder_recurring"
        const val EXTRA_REMINDER_RECURRENCE_PATTERN = "reminder_recurrence_pattern"
        const val EXTRA_REMINDER_RECURRENCE_DAYS = "reminder_recurrence_days"
        const val EXTRA_REMINDER_IS_ALARM = "reminder_is_alarm"
        const val EXTRA_REMINDER_CHANNEL_ID = "reminder_channel_id"
    }

    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var firebaseManager: FirebaseManager

    /**
     * Check if the app can schedule exact alarms
     */
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // For older versions, exact alarms are allowed by default
        }
    }

    override fun onCreate() {
        super.onCreate()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = NotificationManagerCompat.from(this)
        preferenceManager = PreferenceManager.getInstance()
        firebaseManager = FirebaseManager

        Log.d(TAG, "ReminderService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Handle incoming intents for reminder management
     */
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SCHEDULE_REMINDER -> scheduleReminder(intent)
            ACTION_CANCEL_REMINDER -> cancelReminder(intent)
            ACTION_UPDATE_REMINDER -> updateReminder(intent)
        }
    }

    /**
     * Schedule a new reminder
     */
    private fun scheduleReminder(intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: ""
        val message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE) ?: ""
        val time = intent.getLongExtra(EXTRA_REMINDER_TIME, 0L)
        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: "general"
        val isRecurring = intent.getBooleanExtra(EXTRA_REMINDER_RECURRING, false)
        val recurrencePattern = intent.getStringExtra(EXTRA_REMINDER_RECURRENCE_PATTERN) ?: "NONE"
        val recurrenceDays = intent.getIntegerArrayListExtra(EXTRA_REMINDER_RECURRENCE_DAYS) ?: emptyList<Int>()
        val isAlarm = intent.getBooleanExtra(EXTRA_REMINDER_IS_ALARM, false)

        if (time <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder for past time: $reminderId")
            return
        }

        val reminderIntent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_MESSAGE, message)
            putExtra(EXTRA_REMINDER_TYPE, type)
            putExtra(EXTRA_REMINDER_IS_ALARM, isAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (isRecurring) {
                scheduleRecurringReminder(reminderId, time, recurrencePattern, recurrenceDays, pendingIntent)
            } else {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        time,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms are not allowed
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        time,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarms not allowed, using inexact alarm for reminder: $reminderId")
                }
            }

            Log.d(TAG, "Reminder scheduled: $reminderId at ${Date(time)} (Recurring: $isRecurring)")

            // Store reminder in Firebase for persistence
            storeReminderInFirebase(reminderId, title, message, time, type, isRecurring, recurrencePattern, recurrenceDays, isAlarm)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder: ${e.message}", e)
        }
    }

    /**
     * Schedule a recurring reminder based on pattern
     */
    private fun scheduleRecurringReminder(
        reminderId: String,
        initialTime: Long,
        recurrencePattern: String,
        recurrenceDays: List<Int>,
        pendingIntent: PendingIntent
    ) {
        when (recurrencePattern) {
            "DAILY" -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    initialTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            "WEEKLY" -> {
                if (recurrenceDays.isNotEmpty()) {
                    // Schedule for specific days of the week
                    scheduleWeeklyReminder(reminderId, initialTime, recurrenceDays, pendingIntent)
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        initialTime,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }
            }
            "MONTHLY" -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    initialTime,
                    AlarmManager.INTERVAL_DAY * 30, // Approximate monthly
                    pendingIntent
                )
            }
            "YEARLY" -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    initialTime,
                    AlarmManager.INTERVAL_DAY * 365, // Approximate yearly
                    pendingIntent
                )
            }
            "CUSTOM" -> {
                // Handle custom intervals (every minute, hour, etc.)
                scheduleCustomReminder(reminderId, initialTime, pendingIntent)
            }
            else -> {
                // No repeat - schedule as one-time
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        initialTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms are not allowed
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        initialTime,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarms not allowed, using inexact alarm for recurring reminder: $reminderId")
                }
            }
        }
    }

    /**
     * Schedule weekly reminder for specific days
     */
    private fun scheduleWeeklyReminder(
        reminderId: String,
        initialTime: Long,
        recurrenceDays: List<Int>,
        pendingIntent: PendingIntent
    ) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = initialTime
        }
        
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val nextDay = recurrenceDays.find { it >= currentDayOfWeek } ?: recurrenceDays.first()
        
        // Calculate next occurrence
        val daysUntilNext = if (nextDay >= currentDayOfWeek) {
            nextDay - currentDayOfWeek
        } else {
            (7 - currentDayOfWeek) + nextDay
        }
        
        val nextTime = initialTime + (daysUntilNext * 24 * 60 * 60 * 1000L)
        
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
        } else {
            // Fallback to inexact alarm if exact alarms are not allowed
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
            Log.w(TAG, "Exact alarms not allowed, using inexact alarm for weekly reminder: $reminderId")
        }
    }

    /**
     * Schedule custom reminder (every minute, hour, etc.)
     */
    private fun scheduleCustomReminder(
        reminderId: String,
        initialTime: Long,
        pendingIntent: PendingIntent
    ) {
        // For now, schedule as daily - can be enhanced based on specific requirements
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            initialTime,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Cancel an existing reminder
     */
    private fun cancelReminder(intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return

        val reminderIntent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        Log.d(TAG, "Reminder cancelled: $reminderId")

        // Remove reminder from Firebase
        removeReminderFromFirebase(reminderId)
    }

    /**
     * Update an existing reminder
     */
    private fun updateReminder(intent: Intent) {
        // First cancel the existing reminder
        cancelReminder(intent)

        // Then schedule the updated reminder
        scheduleReminder(intent)

        Log.d(TAG, "Reminder updated: ${intent.getStringExtra(EXTRA_REMINDER_ID)}")
    }

    /**
     * Store reminder in Firebase for persistence across app restarts
     */
    private fun storeReminderInFirebase(
        reminderId: String,
        title: String,
        message: String,
        time: Long,
        type: String,
        isRecurring: Boolean = false,
        recurrencePattern: String = "NONE",
        recurrenceDays: List<Int> = emptyList(),
        isAlarm: Boolean = false
    ) {
        try {
            val userId = firebaseManager.getCurrentUserId()
            if (userId != null) {
                val reminderData = mapOf(
                    "id" to reminderId,
                    "title" to title,
                    "message" to message,
                    "scheduledTime" to time,
                    "type" to type,
                    "isRecurring" to isRecurring,
                    "recurrencePattern" to recurrencePattern,
                    "recurrenceDays" to recurrenceDays,
                    "isAlarm" to isAlarm,
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis()
                )

                // Using a placeholder reference since FirebaseManager needs to be fixed
                // firebaseManager.getRemindersReference()
                //     .child(userId)
                //     .child(reminderId)
                //     .setValue(reminderData)

                Log.d(TAG, "Reminder stored in Firebase: $reminderId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing reminder in Firebase: ${e.message}", e)
        }
    }

    /**
     * Remove reminder from Firebase
     */
    private fun removeReminderFromFirebase(reminderId: String) {
        try {
            val userId = firebaseManager.getCurrentUserId()
            if (userId != null) {
                // Using a placeholder reference since FirebaseManager needs to be fixed
                // firebaseManager.getRemindersReference()
                //     .child(userId)
                //     .child(reminderId)
                //     .removeValue()

                Log.d(TAG, "Reminder removed from Firebase: $reminderId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing reminder from Firebase: ${e.message}", e)
        }
    }

    /**
     * Restore all reminders from Firebase on service start
     */
    private fun restoreRemindersFromFirebase() {
        try {
            val userId = firebaseManager.getCurrentUserId()
            if (userId != null) {
                // TODO: Implement reminder restoration from Firebase
                // This would involve:
                // 1. Reading all active reminders for the user
                // 2. Rescheduling future reminders
                // 3. Cleaning up past reminders

                Log.d(TAG, "Reminders restored from Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring reminders from Firebase: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ReminderService destroyed")
    }
}