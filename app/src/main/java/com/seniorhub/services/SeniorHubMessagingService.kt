package com.seniorhub.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seniorhub.R
import com.seniorhub.activities.MainActivity

class SeniorHubMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SeniorHubFCM"
        private const val CHANNEL_ID = "seniorhub_notifications"
        private const val CHANNEL_NAME = "SeniorHub Notifications"
        private const val CHANNEL_DESCRIPTION = "Emergency alerts and notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            handleNotificationMessage(it)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: ""
        val seniorName = data["seniorName"] ?: "Unknown"
        val location = data["location"] ?: ""
        val message = data["message"] ?: "Emergency alert"

        when (type) {
            "emergency" -> {
                showEmergencyNotification(
                    title = "🚨 Emergency Alert",
                    message = message,
                    seniorName = seniorName,
                    location = location
                )
            }
            "sos" -> {
                showEmergencyNotification(
                    title = "🚨 SOS Emergency",
                    message = message,
                    seniorName = seniorName,
                    location = location
                )
            }
            else -> {
                showGeneralNotification(
                    title = "SeniorHub Notification",
                    message = message
                )
            }
        }
    }

    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        showGeneralNotification(
            title = notification.title ?: "SeniorHub",
            message = notification.body ?: "You have a new notification"
        )
    }

    private fun showEmergencyNotification(
        title: String,
        message: String,
        seniorName: String,
        location: String
    ) {
        EmergencyNotificationService.showEmergencyNotification(
            context = this,
            title = title,
            message = message,
            seniorName = seniorName,
            location = location
        )
    }

    private fun showGeneralNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed Token: $token")

        // Send token to your server if needed
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Implement token registration with your backend
        // This is where you would send the FCM token to your server
        // so it can send targeted notifications to this device
        Log.d(TAG, "FCM Token: $token")
    }
}
