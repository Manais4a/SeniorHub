package com.seniorhub.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.seniorhub.R
import com.seniorhub.models.User

/**
 * AccessibilityManager - Handles all accessibility-related functionality
 * 
 * This utility class provides methods to enhance app accessibility for senior users,
 * including text scaling, high contrast mode, and other senior-friendly features.
 */
object AccessibilityManager {
    
    private const val PREF_ACCESSIBILITY = "accessibility_prefs"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_VOICE_ASSISTANCE = "voice_assistance"
    private const val KEY_APP_LANGUAGE = "app_language"
    
    // Default values
    private const val DEFAULT_TEXT_SIZE_SP = 16f
    private const val DEFAULT_HIGH_CONTRAST = false
    private const val DEFAULT_VOICE_ASSISTANCE = true
    private const val DEFAULT_LANGUAGE = "en"

    /**
     * Apply user's accessibility preferences to the app
     */
    fun applyAccessibilitySettings(context: Context, user: User) {
        // Apply text size
        val textSize = user.textSize.takeIf { it > 0 } ?: DEFAULT_TEXT_SIZE_SP
        setGlobalTextSize(context, textSize)
        
        // Apply high contrast mode
        setHighContrastMode(context, user.highContrastMode)
        
        // Apply preferred language if different from system
        // Note: This would require app restart to take full effect
        setAppLanguage(context, user.preferredLanguage)
    }
    
    /**
     * Set global text size for the app
     */
    fun setGlobalTextSize(context: Context, textSizeSp: Float) {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_TEXT_SIZE, textSizeSp).apply()
    }
    
    /**
     * Get the current text size setting
     */
    fun getCurrentTextSize(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP)
    }
    
    /**
     * Enable or disable high contrast mode
     */
    fun setHighContrastMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        
        if (enabled) {
            // Apply high contrast theme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            // Follow system theme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    fun isHighContrastModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HIGH_CONTRAST, DEFAULT_HIGH_CONTRAST)
    }
    
    /**
     * Set voice assistance preference
     */
    fun setVoiceAssistanceEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VOICE_ASSISTANCE, enabled).apply()
    }
    
    /**
     * Check if voice assistance is enabled
     */
    fun isVoiceAssistanceEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VOICE_ASSISTANCE, DEFAULT_VOICE_ASSISTANCE)
    }
    
    /**
     * Set app language (requires app restart for full effect)
     */
    fun setAppLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE, languageCode).apply()
        
        // Note: To fully apply language changes, the activity needs to be recreated
        // This is typically handled by the base activity
    }
    
    /**
     * Get current app language setting
     */
    fun getAppLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * Apply text appearance to a TextView based on accessibility settings
     */
    fun applyTextAppearance(context: Context, textView: TextView) {
        val textSize = getCurrentTextSize(context)
        val isHighContrast = isHighContrastModeEnabled(context)
        
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        
        if (isHighContrast) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.high_contrast_primary))
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.high_contrast_background))
            textView.typeface = Typeface.DEFAULT_BOLD
        } else {
            textView.setTextColor(ContextCompat.getColorStateList(context, R.color.primary_text))
            textView.background = null
            textView.typeface = Typeface.DEFAULT
        }
    }
    
    /**
     * Check if the device has accessibility services enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return accessibilityManager?.isEnabled == true || accessibilityManager?.isTouchExplorationEnabled == true
    }
    
    /**
     * Create a spannable string with the current accessibility settings
     */
    fun createAccessibleSpannable(context: Context, text: String, style: Int): SpannableString {
        val spannable = SpannableString(text)
        spannable.setSpan(
            TextAppearanceSpan(context, style),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }
    
    /**
     * Increase contrast of a color for better visibility
     */
    fun getContrastColor(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        
        // Calculate luminance (perceived brightness)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        
        // Return black for light colors, white for dark colors
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
    
    /**
     * Check if the device is in dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }
    
    /**
     * Set up click listeners with accessibility in mind
     */
    fun setupAccessibleClick(view: View, action: () -> Unit) {
        view.setOnClickListener {
            // Add haptic feedback
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // Execute the action
            action()
        }
        
        // Add content description for accessibility
        if (view.contentDescription == null) {
            view.contentDescription = view.context.getString(R.string.app_name)
        }
    }

    /**
     * Apply accessibility settings to multiple text views at once
     */
    fun applyAccessibilityToViews(context: Context, vararg textViews: TextView) {
        textViews.forEach { textView ->
            applyTextAppearance(context, textView)
        }
    }

    /**
     * Get recommended minimum touch target size for seniors
     */
    fun getMinimumTouchTargetSize(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (48 * density).toInt() // 48dp minimum as per accessibility guidelines
    }

    /**
     * Check if text size is within recommended range for seniors
     */
    fun isTextSizeAccessible(textSizeSp: Float): Boolean {
        return textSizeSp >= 14f // Minimum 14sp for senior accessibility
    }

    /**
     * Get scaled text size based on user preference
     */
    fun getScaledTextSize(context: Context, baseTextSize: Float): Float {
        val userTextSize = getCurrentTextSize(context)
        val scaleFactor = userTextSize / DEFAULT_TEXT_SIZE_SP
        return baseTextSize * scaleFactor
    }

    /**
     * Apply color contrast enhancement for better visibility
     */
    fun enhanceColorContrast(originalColor: Int, isHighContrast: Boolean): Int {
        return if (isHighContrast) {
            getContrastColor(originalColor)
        } else {
            originalColor
        }
    }

    /**
     * Set up view for better accessibility navigation
     */
    fun setupAccessibleNavigation(
        view: View,
        nextFocusDownId: Int? = null,
        nextFocusUpId: Int? = null
    ) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        nextFocusDownId?.let { view.nextFocusDownId = it }
        nextFocusUpId?.let { view.nextFocusUpId = it }
    }

    /**
     * Check if haptic feedback is enabled for the user
     */
    fun isHapticFeedbackEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        return prefs.getBoolean("haptic_feedback_enabled", true) // Default to enabled
    }

    /**
     * Set haptic feedback preference
     */
    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_ACCESSIBILITY, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
    }

    /**
     * Apply accessibility settings specifically for health tracking views
     * Ensures health metrics are easily readable for senior users
     */
    fun applyHealthAccessibilitySettings(context: Context, vararg textViews: TextView) {
        val textSize = getCurrentTextSize(context)
        val isHighContrast = isHighContrastModeEnabled(context)
        
        textViews.forEach { textView ->
            // Apply larger text size for health metrics
            val healthTextSize = if (textSize > 16f) textSize + 2f else textSize + 4f
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, healthTextSize)
            
            // Apply high contrast if enabled
            if (isHighContrast) {
                textView.setTextColor(ContextCompat.getColor(context, R.color.high_contrast_primary))
                textView.typeface = Typeface.DEFAULT_BOLD
            }
            
            // Ensure minimum touch target size
            val minSize = getMinimumTouchTargetSize(context)
            textView.minHeight = minSize
        }
    }

    /**
     * Check if health data should be announced for accessibility
     */
    fun shouldAnnounceHealthData(context: Context): Boolean {
        return isVoiceAssistanceEnabled(context) && isAccessibilityEnabled(context)
    }

    /**
     * Get accessibility-friendly health value format
     */
    fun formatHealthValueForAccessibility(value: String, unit: String, type: String): String {
        return when (type.lowercase()) {
            "blood_pressure" -> "Blood pressure: $value millimeters of mercury"
            "heart_rate" -> "Heart rate: $value beats per minute"
            "blood_sugar" -> "Blood sugar: $value milligrams per deciliter"
            "weight" -> "Weight: $value $unit"
            else -> "$type: $value $unit"
        }
    }
}
