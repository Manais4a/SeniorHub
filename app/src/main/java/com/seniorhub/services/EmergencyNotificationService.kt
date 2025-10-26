package com.seniorhub.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.seniorhub.R
import com.seniorhub.activities.MainActivity

/**
 * EmergencyNotificationService - FCM service for emergency notifications
 * 
 * This service handles Firebase Cloud Messaging notifications for emergency alerts
 * and provides fallback to SMS when FCM is not available.
 */
class EmergencyNotificationService {
    
    companion object {
        private const val CHANNEL_ID = "emergency_notifications"
        private const val CHANNEL_NAME = "Emergency Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for emergency alerts and SOS messages"
        private const val NOTIFICATION_ID = 1001
        
        /**
         * Create notification channel for emergency notifications
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        /**
         * Show emergency notification
         */
        fun showEmergencyNotification(
            context: Context,
            title: String,
            message: String,
            seniorName: String,
            location: String? = null
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create intent to open MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("emergency_alert", true)
                putExtra("senior_name", seniorName)
                putExtra("location", location)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(buildNotificationText(message, seniorName, location)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setLights(android.graphics.Color.RED, 1000, 1000)
                .setOngoing(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        
        /**
         * Build notification text with location information
         */
        private fun buildNotificationText(message: String, seniorName: String, location: String?): String {
            return buildString {
                appendLine("🚨 EMERGENCY ALERT 🚨")
                appendLine()
                appendLine("Senior: $seniorName")
                appendLine()
                appendLine(message)
                if (!location.isNullOrEmpty()) {
                    appendLine()
                    appendLine("Location: $location")
                }
                appendLine()
                appendLine("Tap to open SeniorHub app")
            }
        }
        
        /**
         * Cancel emergency notification
         */
        fun cancelEmergencyNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
