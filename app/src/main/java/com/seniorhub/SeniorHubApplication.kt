package com.seniorhub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.seniorhub.firebase.FirebaseConfig
import com.seniorhub.utils.PreferenceManager
import com.seniorhub.utils.LocaleUtils
import com.cloudinary.android.MediaManager
import com.seniorhub.utils.CloudinaryManager

/**
 * Senior Hub Application Class
 *
 * This class handles app-wide initialization including:
 * - Notification channels for senior-friendly alerts
 * - Accessibility configurations
 * - Preference management initialization
 * - Firebase initialization
 *
 * Following Jakob Nielsen's Heuristics:
 * - System status visibility through proper notification channels
 * - Error prevention through proper initialization
 */
class SeniorHubApplication : Application() {

    companion object {
        // Notification channels for different types of senior alerts
        const val MEDICATION_CHANNEL_ID = "medication_reminders"
        const val EMERGENCY_CHANNEL_ID = "emergency_alerts"
        const val APPOINTMENT_CHANNEL_ID = "appointment_reminders"
        const val HEALTH_CHANNEL_ID = "health_notifications"
        const val FAMILY_CHANNEL_ID = "family_updates"
        const val GENERAL_CHANNEL_ID = "general_notifications"

        lateinit var instance: SeniorHubApplication
            private set
    }

    private fun initializeFirebase() {
        try {
            FirebaseConfig.initializeFirebase(this)
            android.util.Log.d("SeniorHubApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("SeniorHubApp", "Firebase initialization failed: ${e.message}")
            throw e
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase
        try {
            initializeFirebase()
        } catch (e: Exception) {
            // Firebase initialization failed, continue without it for now
            android.util.Log.e("SeniorHubApp", "Firebase initialization failed: ${e.message}")
        }

        // Create notification channels for senior-friendly alerts
        createNotificationChannels()

        // Initialize preferences for accessibility settings
        PreferenceManager.initialize(this)

        // Apply saved app language at startup (AppCompat API updates all Activities)
        LocaleUtils.applyAppLocale(PreferenceManager.getInstance().language)

        // Initialize Cloudinary image service early so profile uploads work
        try {
            CloudinaryManager.initialize(this)
            android.util.Log.d("SeniorHubApp", "Cloudinary initialized")
        } catch (e: Exception) {
            android.util.Log.e("SeniorHubApp", "Cloudinary initialization failed: ${e.message}")
        }
    }

    /**
     * Create notification channels with senior-friendly settings
     * High importance for emergency and medication reminders
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Emergency alerts - Highest priority with sound and vibration
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency notifications and SOS alerts"
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true) // Bypass Do Not Disturb for emergencies
            }

            // Medication reminders - High priority for health safety
            val medicationChannel = NotificationChannel(
                MEDICATION_CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important medication and health reminders"
                enableLights(true)
                enableVibration(true)
            }

            // Appointment reminders - High priority for healthcare
            val appointmentChannel = NotificationChannel(
                APPOINTMENT_CHANNEL_ID,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Doctor appointments and health checkup reminders"
                enableLights(true)
                enableVibration(true)
            }

            // Health notifications - Medium priority for tracking
            val healthChannel = NotificationChannel(
                HEALTH_CHANNEL_ID,
                "Health Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Health monitoring and wellness updates"
                enableLights(true)
            }

            // Family updates - Medium priority for social connection
            val familyChannel = NotificationChannel(
                FAMILY_CHANNEL_ID,
                "Family Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Messages and updates from family members"
                enableLights(true)
            }

            // General notifications - Lower priority for app updates
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "App updates and general information"
            }

            // Register all channels
            notificationManager.createNotificationChannels(
                listOf(
                    emergencyChannel,
                    medicationChannel,
                    appointmentChannel,
                    healthChannel,
                    familyChannel,
                    generalChannel
                )
            )
        }
    }
}