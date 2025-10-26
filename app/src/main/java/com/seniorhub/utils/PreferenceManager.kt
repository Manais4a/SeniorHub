package com.seniorhub.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Preference Manager for Senior Hub Application
 *
 * Manages user preferences with senior-friendly defaults:
 * - Large text sizes by default
 * - High contrast mode availability
 * - Voice assistance preferences
 * - Emergency contact settings
 *
 * Following Jakob Nielsen's Heuristics:
 * - User control and freedom through customizable settings
 * - Consistency and standards in preference management
 */
class PreferenceManager private constructor(context: Context) {

    companion object {
        // Preference keys for senior-friendly settings
        private const val PREF_FONT_SIZE = "font_size"
        private const val PREF_HIGH_CONTRAST = "high_contrast"
        private const val PREF_DARK_THEME = "dark_theme"
        private const val PREF_VOICE_ASSISTANCE = "voice_assistance"
        private const val PREF_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val PREF_LARGE_BUTTONS = "large_buttons"
        private const val PREF_SIMPLE_MODE = "simple_mode"
        private const val PREF_EMERGENCY_CONTACTS_SET = "emergency_contacts_set"
        private const val PREF_FIRST_TIME_USER = "first_time_user"
        private const val PREF_USER_NAME = "user_name"
        private const val PREF_USER_AGE = "user_age"
        private const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val PREF_USER_ID = "user_id"
        private const val PREF_USER_EMAIL = "user_email"
        private const val PREF_REMEMBER_ME = "remember_me"
        private const val PREF_TEXT_SIZE = "text_size"
        private const val PREF_HIGH_CONTRAST_MODE = "high_contrast_mode"
        private const val PREF_VOICE_ASSISTANCE_ENABLED = "voice_assistance_enabled"
        private const val PREF_FCM_TOKEN = "fcm_token"
        private const val PREF_IS_USER_LOGGED_IN = "is_user_logged_in"
        private const val PREF_ARE_SERVICES_ENABLED = "are_services_enabled"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_OFFLINE_MODE = "offline_mode_enabled"

        // Default values optimized for senior citizens
        private const val DEFAULT_FONT_SIZE = 20f // Larger default font
        private const val DEFAULT_HIGH_CONTRAST = false
        private const val DEFAULT_DARK_THEME = false
        private const val DEFAULT_VOICE_ASSISTANCE = true // Enabled by default
        private const val DEFAULT_HAPTIC_FEEDBACK = true
        private const val DEFAULT_LARGE_BUTTONS = true
        private const val DEFAULT_SIMPLE_MODE = true
        private const val DEFAULT_NOTIFICATIONS_ENABLED = true

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = PreferenceManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: throw IllegalStateException(
                    "PreferenceManager must be initialized before use. Call initialize() first."
                )
            }
        }
    }

    var userId: String
        get() = sharedPreferences.getString("user_id", "") ?: ""
        set(value) = sharedPreferences.edit().putString("user_id", value).apply()

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("seniorhub_preferences", Context.MODE_PRIVATE)

    // Font size for accessibility
    var fontSize: Float
        get() = sharedPreferences.getFloat(PREF_FONT_SIZE, DEFAULT_FONT_SIZE)
        set(value) = sharedPreferences.edit().putFloat(PREF_FONT_SIZE, value).apply()

    // High contrast mode for vision accessibility
    var isHighContrastEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_HIGH_CONTRAST, DEFAULT_HIGH_CONTRAST)
        set(value) = sharedPreferences.edit().putBoolean(PREF_HIGH_CONTRAST, value).apply()

    // Dark theme preference
    var isDarkThemeEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_DARK_THEME, DEFAULT_DARK_THEME)
        set(value) = sharedPreferences.edit().putBoolean(PREF_DARK_THEME, value).apply()

    // Voice assistance for hands-free operation
    var isVoiceAssistanceEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_VOICE_ASSISTANCE, DEFAULT_VOICE_ASSISTANCE)
        set(value) = sharedPreferences.edit().putBoolean(PREF_VOICE_ASSISTANCE, value).apply()

    // Haptic feedback for touch confirmation
    var isHapticFeedbackEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK)
        set(value) = sharedPreferences.edit().putBoolean(PREF_HAPTIC_FEEDBACK, value).apply()

    // Large buttons for easier interaction
    var isLargeButtonsEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_LARGE_BUTTONS, DEFAULT_LARGE_BUTTONS)
        set(value) = sharedPreferences.edit().putBoolean(PREF_LARGE_BUTTONS, value).apply()

    // Simple mode for reduced complexity
    var isSimpleModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_SIMPLE_MODE, DEFAULT_SIMPLE_MODE)
        set(value) = sharedPreferences.edit().putBoolean(PREF_SIMPLE_MODE, value).apply()

    // Emergency contacts setup status
    var areEmergencyContactsSet: Boolean
        get() = sharedPreferences.getBoolean(PREF_EMERGENCY_CONTACTS_SET, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_EMERGENCY_CONTACTS_SET, value).apply()

    // First time user flag for onboarding
    var isFirstTimeUser: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_TIME_USER, true)
        set(value) = sharedPreferences.edit().putBoolean(PREF_FIRST_TIME_USER, value).apply()

    // User personal information
    var userName: String
        get() = sharedPreferences.getString(PREF_USER_NAME, "") ?: ""
        set(value) = sharedPreferences.edit().putString(PREF_USER_NAME, value).apply()

    var userAge: Int
        get() = sharedPreferences.getInt(PREF_USER_AGE, 0)
        set(value) = sharedPreferences.edit().putInt(PREF_USER_AGE, value).apply()

    // Notifications preference
    var areNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            PREF_NOTIFICATIONS_ENABLED,
            DEFAULT_NOTIFICATIONS_ENABLED
        )
        set(value) = sharedPreferences.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, value).apply()

    var userEmail: String
        get() = sharedPreferences.getString(PREF_USER_EMAIL, "") ?: ""
        set(value) = sharedPreferences.edit().putString(PREF_USER_EMAIL, value).apply()

    var isUserLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(PREF_IS_USER_LOGGED_IN, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_IS_USER_LOGGED_IN, value).apply()

    var areServicesEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_ARE_SERVICES_ENABLED, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_ARE_SERVICES_ENABLED, value).apply()

    var fcmToken: String
        get() = sharedPreferences.getString(PREF_FCM_TOKEN, "") ?: ""
        set(value) = sharedPreferences.edit().putString(PREF_FCM_TOKEN, value).apply()

    // Application language (en default, tl Tagalog, ceb Cebuano)
    var language: String
        get() = sharedPreferences.getString(PREF_LANGUAGE, "en") ?: "en"
        set(value) = sharedPreferences.edit().putString(PREF_LANGUAGE, value).apply()

    // Offline mode toggle
    var offlineModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_OFFLINE_MODE, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_OFFLINE_MODE, value).apply()

    // Health tracking preferences
    var healthDataSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean("health_data_sync_enabled", true)
        set(value) = sharedPreferences.edit().putBoolean("health_data_sync_enabled", value).apply()

    var healthRemindersEnabled: Boolean
        get() = sharedPreferences.getBoolean("health_reminders_enabled", true)
        set(value) = sharedPreferences.edit().putBoolean("health_reminders_enabled", value).apply()

    var healthDataRetentionDays: Int
        get() = sharedPreferences.getInt("health_data_retention_days", 365)
        set(value) = sharedPreferences.edit().putInt("health_data_retention_days", value).apply()

    var lastHealthSyncTime: Long
        get() = sharedPreferences.getLong("last_health_sync_time", 0)
        set(value) = sharedPreferences.edit().putLong("last_health_sync_time", value).apply()

    /**
     * Reset all preferences to senior-friendly defaults
     */
    fun resetToDefaults() {
        sharedPreferences.edit().apply {
            putFloat(PREF_FONT_SIZE, DEFAULT_FONT_SIZE)
            putBoolean(PREF_HIGH_CONTRAST, DEFAULT_HIGH_CONTRAST)
            putBoolean(PREF_DARK_THEME, DEFAULT_DARK_THEME)
            putBoolean(PREF_VOICE_ASSISTANCE, DEFAULT_VOICE_ASSISTANCE)
            putBoolean(PREF_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK)
            putBoolean(PREF_LARGE_BUTTONS, DEFAULT_LARGE_BUTTONS)
            putBoolean(PREF_SIMPLE_MODE, DEFAULT_SIMPLE_MODE)
            putBoolean(PREF_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
            apply()
        }
    }

    /**
     * Clear all user data (for logout)
     */
    fun clearUserData() {
        sharedPreferences.edit().apply {
            remove(PREF_USER_NAME)
            remove(PREF_USER_AGE)
            remove(PREF_USER_ID)
            remove(PREF_USER_EMAIL)
            remove(PREF_EMERGENCY_CONTACTS_SET)
            apply()
        }
    }
}