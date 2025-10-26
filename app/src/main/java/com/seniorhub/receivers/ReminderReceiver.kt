package com.seniorhub.receivers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.seniorhub.R
import com.seniorhub.activities.MainActivity
import com.seniorhub.services.ReminderService

/**
 * ReminderReceiver - Handles reminder alarms and displays notifications
 *
 * This receiver is triggered by AlarmManager when reminders are due
 * and displays appropriate notifications to the user
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIFICATION_BASE_ID = 2000
        private const val INBOX_GROUP_KEY = "reminder_inbox_group"
        private const val INBOX_NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Reminder receiver triggered with action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

        when (intent.action) {
            "SNOOZE_REMINDER" -> {
                val reminderId = intent.getStringExtra(ReminderService.EXTRA_REMINDER_ID) ?: return
                handleSnoozeReminder(context, reminderId)
            }
            "COMPLETE_REMINDER" -> {
                val reminderId = intent.getStringExtra(ReminderService.EXTRA_REMINDER_ID) ?: return
                handleCompleteReminder(context, reminderId)
            }
            else -> {
                // Regular reminder notification
                val reminderId = intent.getStringExtra(ReminderService.EXTRA_REMINDER_ID) ?: return
                val title = intent.getStringExtra(ReminderService.EXTRA_REMINDER_TITLE) ?: "Reminder"
                val message = intent.getStringExtra(ReminderService.EXTRA_REMINDER_MESSAGE) ?: ""
                val type = intent.getStringExtra(ReminderService.EXTRA_REMINDER_TYPE) ?: "general"

                Log.d(TAG, "Showing notification for reminder: $reminderId, title: $title, message: $message, type: $type")
                showReminderNotification(context, reminderId, title, message, type)
            }
        }
    }

    /**
     * Show reminder notification to user
     */
    private fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        message: String,
        type: String
    ) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open main activity when notification is tapped
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminderId)
            putExtra("open_reminders", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Choose appropriate icon based on reminder type
        val iconRes = when (type) {
            "medication" -> R.drawable.ic_medication
            "appointment" -> R.drawable.ic_calendar
            "health" -> R.drawable.ic_heart
            else -> R.drawable.ic_reminder
        }

        // Create action buttons for the notification
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "SNOOZE_REMINDER"
            putExtra(ReminderService.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "COMPLETE_REMINDER"
            putExtra(ReminderService.EXTRA_REMINDER_ID, reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build individual notification with custom reminder sound and enhanced vibration
        val customSoundUri = getReminderSoundUri(context, type)
        Log.d(TAG, "Building notification with sound URI: $customSoundUri")
        
        val individualNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(getReminderVibrationPattern(type)) // Enhanced vibration pattern based on type
            .setLights(context.getColor(R.color.focus_blue), 2000, 1000) // Blue light for reminders
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSound(customSoundUri) // Custom reminder sound
            .addAction(R.drawable.ic_snooze, "Snooze 15min", snoozePendingIntent)
            .addAction(R.drawable.ic_check, "Mark Done", completePendingIntent)
            .setGroup(INBOX_GROUP_KEY)
            .setFullScreenIntent(pendingIntent, true) // Make it a heads-up notification
            .setTimeoutAfter(300000) // Auto-dismiss after 5 minutes
            .build()

        // Build inbox-style summary notification
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Reminders")
            .setSummaryText("You have active reminders")

        // Add this reminder to the inbox
        inboxStyle.addLine("$title - $message")

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Reminders")
            .setContentText("You have active reminders")
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentIntent(pendingIntent)
            .setStyle(inboxStyle)
            .setGroup(INBOX_GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND) // Use default sound for summary notifications
            .build()

        // Show both notifications
        val notificationId = NOTIFICATION_BASE_ID + reminderId.hashCode()
        notificationManager.notify(notificationId, individualNotification)
        notificationManager.notify(INBOX_NOTIFICATION_ID, summaryNotification)

        Log.d(TAG, "Reminder notification displayed: $reminderId with notification ID: $notificationId")
        Log.d(TAG, "Notification title: $title, message: $message, sound URI: $customSoundUri")

        // TODO: Mark reminder as completed/acknowledged in Firebase
        // TODO: Schedule next occurrence if it's a recurring reminder
    }

    /**
     * Handle snooze reminder action
     */
    @SuppressLint("ScheduleExactAlarm")
    private fun handleSnoozeReminder(context: Context, reminderId: String) {
        Log.d(TAG, "Snoozing reminder: $reminderId")
        
        // Cancel current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = NOTIFICATION_BASE_ID + reminderId.hashCode()
        notificationManager.cancel(notificationId)
        
        // Schedule reminder for 15 minutes later
        val snoozeTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes
        
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderService.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderService.EXTRA_REMINDER_TITLE, "Snoozed Reminder")
            putExtra(ReminderService.EXTRA_REMINDER_MESSAGE, "This reminder was snoozed for 15 minutes")
            putExtra(ReminderService.EXTRA_REMINDER_TYPE, "general")
        }
        
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 3000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        try {
            // Check if we can schedule exact alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozePendingIntent
                    )
                    Log.d(TAG, "Exact alarm scheduled for snooze: $reminderId")
                } else {
                    // Fallback to inexact alarm if exact alarms are not allowed
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozePendingIntent
                    )
                    Log.w(TAG, "Exact alarms not allowed, using inexact alarm for snooze: $reminderId")
                }
            } else {
                // For older Android versions, use exact alarm
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    snoozePendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled for snooze (legacy): $reminderId")
            }
            
            // Show confirmation toast
            android.widget.Toast.makeText(context, "Reminder snoozed for 15 minutes", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when scheduling snooze alarm: ${e.message}", e)
            // Show error message to user
            android.widget.Toast.makeText(context, "Cannot snooze reminder: Permission denied", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling snooze alarm: ${e.message}", e)
            // Show error message to user
            android.widget.Toast.makeText(context, "Failed to snooze reminder: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle complete reminder action
     */
    private fun handleCompleteReminder(context: Context, reminderId: String) {
        Log.d(TAG, "Completing reminder: $reminderId")
        
        // Cancel current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = NOTIFICATION_BASE_ID + reminderId.hashCode()
        notificationManager.cancel(notificationId)
        
        // Show completion confirmation
        android.widget.Toast.makeText(context, "Reminder marked as completed", android.widget.Toast.LENGTH_SHORT).show()
        
        // TODO: Update reminder status in database/Firebase
        // TODO: If recurring, schedule next occurrence
    }

    /**
     * Create notification channel for reminders (Android O+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SeniorHub Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for medications, appointments, birthdays, and exercise"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                    lightColor = context.getColor(R.color.focus_blue)
                    vibrationPattern = longArrayOf(0, 600, 300, 600)
                    // Use default notification sound for channel, custom sound applied per notification
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )
                    // Enable bypassing Do Not Disturb for critical reminders
                    setBypassDnd(true)
                    // Set lockscreen visibility
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Reminder notification channel created")
            }
        }
    }
    
    /**
     * Get the appropriate sound URI for reminder notifications based on type
     */
    private fun getReminderSoundUri(context: Context, type: String): android.net.Uri {
        return try {
            // Use custom reminder sound for all reminder types
            val customSoundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.reminder_sound}")
            Log.d(TAG, "Using custom sound URI: $customSoundUri")
            customSoundUri
        } catch (e: Exception) {
            Log.e(TAG, "Error creating custom sound URI: ${e.message}")
            // Fallback to default notification sound if custom sound fails
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    /**
     * Get vibration pattern based on reminder type
     */
    private fun getReminderVibrationPattern(type: String): LongArray {
        return when (type) {
            "medication" -> {
                // Strong vibration for medication (critical) - longer and more intense
                longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
            }
            "appointment" -> {
                // Medium vibration for appointments - clear pattern
                longArrayOf(0, 800, 400, 800, 400, 800)
            }
            "birthday" -> {
                // Gentle but noticeable vibration for birthdays
                longArrayOf(0, 600, 300, 600, 300, 600)
            }
            "exercise" -> {
                // Energetic vibration for exercise - rhythmic pattern
                longArrayOf(0, 400, 200, 400, 200, 400, 200, 400)
            }
            else -> {
                // Default vibration pattern - clear and noticeable
                longArrayOf(0, 600, 300, 600)
            }
        }
    }
}