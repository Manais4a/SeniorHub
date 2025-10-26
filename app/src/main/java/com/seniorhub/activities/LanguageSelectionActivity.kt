package com.seniorhub.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.seniorhub.R
import com.seniorhub.utils.PreferenceManager
import com.seniorhub.utils.AccessibilityManager
import com.seniorhub.utils.LocaleUtils

/**
 * LanguageSelectionActivity - Senior-friendly language selection interface
 *
 * Features:
 * - Multi-language support (English, Tagalog, Cebuano)
 * - Offline mode toggle
 * - High contrast and accessibility support
 * - Large touch targets for senior users
 * - Bilingual interface (English + Filipino)
 *
 * @author SeniorHub Team
 * @version 1.0.0
 */
class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    // UI Components
    private lateinit var cardEnglish: MaterialCardView
    private lateinit var cardTagalog: MaterialCardView
    private lateinit var cardCebuano: MaterialCardView
    private lateinit var switchOfflineMode: SwitchMaterial
    private lateinit var btnContinue: com.google.android.material.button.MaterialButton

    // Check icons for selected language (these are the ImageViews inside each card)
    private lateinit var checkEnglish: ImageView
    private lateinit var checkTagalog: ImageView
    private lateinit var checkCebuano: ImageView

    private var selectedLanguage = "en" // Default to English
    private var isOfflineModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)

        initializeComponents()
        setupUI()
        loadCurrentSettings()
        applyAccessibilitySettings()
        setupBackPressedCallback()
    }

    /**
     * Initialize all components and dependencies
     */
    private fun initializeComponents() {
        preferenceManager = PreferenceManager.getInstance()

        // Initialize UI components
        cardEnglish = findViewById(R.id.cardEnglish)
        cardTagalog = findViewById(R.id.cardTagalog)
        cardCebuano = findViewById(R.id.cardCebuano)
        switchOfflineMode = findViewById(R.id.switchOfflineMode)
        btnContinue = findViewById(R.id.btnContinue)

        // Initialize check icons - these are the ImageViews inside each card
        // Each card has a LinearLayout with an ImageView at the end (index 2)
        val englishLinearLayout = cardEnglish.getChildAt(0) as LinearLayout
        val tagalogLinearLayout = cardTagalog.getChildAt(0) as LinearLayout
        val cebuanoLinearLayout = cardCebuano.getChildAt(0) as LinearLayout
        
        checkEnglish = englishLinearLayout.getChildAt(2) as ImageView
        checkTagalog = tagalogLinearLayout.getChildAt(2) as ImageView
        checkCebuano = cebuanoLinearLayout.getChildAt(2) as ImageView
    }

    /**
     * Setup all UI components with click listeners and accessibility
     */
    private fun setupUI() {
        // Setup language selection cards
        cardEnglish.setOnClickListener {
            selectLanguage("en")
        }

        cardTagalog.setOnClickListener {
            selectLanguage("tl")
        }

        cardCebuano.setOnClickListener {
            selectLanguage("ceb")
        }

        // Setup offline mode switch
        switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            isOfflineModeEnabled = isChecked
            preferenceManager.offlineModeEnabled = isChecked
            showToast("Offline mode ${if (isChecked) "enabled" else "disabled"}")
        }

        // Setup continue button
        btnContinue.setOnClickListener {
            saveSettingsAndContinue()
        }

        // Apply accessibility settings
        if (AccessibilityManager.isVoiceAssistanceEnabled(this)) {
            setupVoiceAssistance()
        }
    }

    /**
     * Load current settings from preferences
     */
    private fun loadCurrentSettings() {
        // Load current language
        selectedLanguage = preferenceManager.language
        updateLanguageSelection()

        // Load offline mode setting
        isOfflineModeEnabled = preferenceManager.offlineModeEnabled
        switchOfflineMode.isChecked = isOfflineModeEnabled
    }

    /**
     * Select a language and update UI
     */
    private fun selectLanguage(languageCode: String) {
        selectedLanguage = languageCode
        updateLanguageSelection()

        // Provide haptic feedback
        if (AccessibilityManager.isHapticFeedbackEnabled(this)) {
            btnContinue.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        // Show selection feedback
        val languageName = when (languageCode) {
            "en" -> "English"
            "tl" -> "Tagalog"
            "ceb" -> "Cebuano"
            else -> "English"
        }
        showToast("Selected: $languageName")
    }

    /**
     * Update language selection UI
     */
    private fun updateLanguageSelection() {
        // Hide all check icons first
        checkEnglish.visibility = android.view.View.GONE
        checkTagalog.visibility = android.view.View.GONE
        checkCebuano.visibility = android.view.View.GONE

        // Show check icon for selected language
        try {
            when (selectedLanguage) {
                "en" -> {
                    checkEnglish.visibility = android.view.View.VISIBLE
                    cardEnglish.setCardBackgroundColor(getColor(R.color.senior_profile_light))
                    cardEnglish.strokeColor = getColor(R.color.senior_profile)
                }
                "tl" -> {
                    checkTagalog.visibility = android.view.View.VISIBLE
                    cardTagalog.setCardBackgroundColor(getColor(R.color.senior_health_light))
                    cardTagalog.strokeColor = getColor(R.color.senior_health)
                }
                "ceb" -> {
                    checkCebuano.visibility = android.view.View.VISIBLE
                    cardCebuano.setCardBackgroundColor(getColor(R.color.senior_benefits_light))
                    cardCebuano.strokeColor = getColor(R.color.senior_benefits)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LanguageSelectionActivity", "Error setting card colors: ${e.message}")
            // Fallback to default colors
            try {
                when (selectedLanguage) {
                    "en" -> {
                        checkEnglish.visibility = android.view.View.VISIBLE
                        cardEnglish.setCardBackgroundColor(getColor(android.R.color.white))
                        cardEnglish.strokeColor = getColor(android.R.color.darker_gray)
                    }
                    "tl" -> {
                        checkTagalog.visibility = android.view.View.VISIBLE
                        cardTagalog.setCardBackgroundColor(getColor(android.R.color.white))
                        cardTagalog.strokeColor = getColor(android.R.color.darker_gray)
                    }
                    "ceb" -> {
                        checkCebuano.visibility = android.view.View.VISIBLE
                        cardCebuano.setCardBackgroundColor(getColor(android.R.color.white))
                        cardCebuano.strokeColor = getColor(android.R.color.darker_gray)
                    }
                }
            } catch (fallbackException: Exception) {
                android.util.Log.e("LanguageSelectionActivity", "Fallback colors also failed: ${fallbackException.message}")
            }
        }

        // Reset other cards to default colors
        resetCardColors()
    }

    /**
     * Reset card colors to default
     */
    private fun resetCardColors() {
        try {
            if (selectedLanguage != "en") {
                cardEnglish.setCardBackgroundColor(getColor(R.color.senior_profile_light))
                cardEnglish.strokeColor = getColor(R.color.senior_profile)
            }
            if (selectedLanguage != "tl") {
                cardTagalog.setCardBackgroundColor(getColor(R.color.senior_health_light))
                cardTagalog.strokeColor = getColor(R.color.senior_health)
            }
            if (selectedLanguage != "ceb") {
                cardCebuano.setCardBackgroundColor(getColor(R.color.senior_benefits_light))
                cardCebuano.strokeColor = getColor(R.color.senior_benefits)
            }
        } catch (e: Exception) {
            android.util.Log.e("LanguageSelectionActivity", "Error resetting card colors: ${e.message}")
            // Fallback to default colors
            try {
                if (selectedLanguage != "en") {
                    cardEnglish.setCardBackgroundColor(getColor(android.R.color.white))
                    cardEnglish.strokeColor = getColor(android.R.color.darker_gray)
                }
                if (selectedLanguage != "tl") {
                    cardTagalog.setCardBackgroundColor(getColor(android.R.color.white))
                    cardTagalog.strokeColor = getColor(android.R.color.darker_gray)
                }
                if (selectedLanguage != "ceb") {
                    cardCebuano.setCardBackgroundColor(getColor(android.R.color.white))
                    cardCebuano.strokeColor = getColor(android.R.color.darker_gray)
                }
            } catch (fallbackException: Exception) {
                android.util.Log.e("LanguageSelectionActivity", "Fallback reset colors also failed: ${fallbackException.message}")
            }
        }
    }

    /**
     * Save settings and continue to main app
     */
    private fun saveSettingsAndContinue() {
        // Save language preference
        preferenceManager.language = selectedLanguage

        // Save offline mode preference
        preferenceManager.offlineModeEnabled = isOfflineModeEnabled

        // Apply language change for the whole app (immediate UI update)
        LocaleUtils.applyAppLocale(selectedLanguage)

        // Show success message
        showToast("Settings saved successfully!")

        // Navigate to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    /**
     * Setup voice assistance for accessibility
     */
    private fun setupVoiceAssistance() {
        // Voice assistance setup would go here
        // This is a placeholder for future implementation
    }

    /**
     * Apply accessibility settings for senior users
     */
    private fun applyAccessibilitySettings() {
        val textSize = AccessibilityManager.getCurrentTextSize(this)
        val isHighContrast = AccessibilityManager.isHighContrastModeEnabled(this)

        // Apply font size adjustments
        if (textSize > 16f) {
            // Apply larger text size to UI elements
            // This would typically involve updating text views with larger font sizes
        }

        // Apply high contrast mode if enabled
        if (isHighContrast) {
            // Apply high contrast colors and styling
            // This would typically involve updating colors and backgrounds
        }
    }

    /**
     * Show toast message with accessibility support
     */
    private fun showToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)

        // Make toast accessible
        if (AccessibilityManager.isVoiceAssistanceEnabled(this)) {
            // Announce toast message for screen readers
            // This would typically involve using AccessibilityManager to announce the message
        }

        toast.show()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Allow back navigation to previous screen
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh settings when returning to activity
        loadCurrentSettings()
    }

    companion object {
        /**
         * Create intent to start LanguageSelectionActivity
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, LanguageSelectionActivity::class.java)
        }

        /**
         * Check if language selection is needed
         */
        fun isLanguageSelectionNeeded(context: Context): Boolean {
            val prefs = PreferenceManager.getInstance()
            return prefs.language.isEmpty() || prefs.language == "en"
        }
    }
}

