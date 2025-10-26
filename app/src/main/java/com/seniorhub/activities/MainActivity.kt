package com.seniorhub.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.seniorhub.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import android.widget.TextView
import com.seniorhub.utils.FirebaseManager
import android.telephony.SmsManager
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.Result
import com.seniorhub.utils.ImageLoader
import kotlinx.coroutines.launch
import java.util.*
import com.seniorhub.firebase.FirebaseConfig
import com.google.firebase.Timestamp
import com.seniorhub.services.EmergencyService
import com.seniorhub.services.LocationService
import android.location.Location

/**
 * MainActivity - Main Dashboard for Senior Citizens
 *
 * This activity provides the main interface for senior citizens with:
 * - Large, easy-to-touch buttons for all major functions
 * - Voice assistance capabilities
 * - Emergency SOS feature prominently displayed
 * - Clear visual hierarchy and high contrast options
 *
 * Following Jakob Nielsen's 10 Usability Heuristics:
 * 1. Visibility of system status - Clear feedback for all actions
 * 2. Match system and real world - Familiar icons and language
 * 3. User control and freedom - Easy navigation and undo options
 * 4. Consistency and standards - Consistent UI patterns
 * 5. Error prevention - Confirmation dialogs for critical actions
 * 6. Recognition rather than recall - Visual cues and clear labels
 * 7. Flexibility and efficiency - Quick access to common functions
 * 8. Aesthetic and minimalist design - Clean, uncluttered interface
 * 9. Help users recognize and recover from errors - Clear error messages
 * 10. Help and documentation - Context-sensitive help available
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isVoiceAssistanceEnabled = true

    // Services
    private lateinit var emergencyService: EmergencyService
    private lateinit var locationService: LocationService

    // Permission request launcher for SMS and Location
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (smsGranted && locationGranted) {
            // All required permissions granted
            showEmergencyConfirmation()
        } else {
            // Some permissions denied
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        try {
            FirebaseManager.initialize(this)
            Log.d("MainActivity", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing Firebase: ${e.message}", e)
            Toast.makeText(this, "Error initializing app. Please restart the application.", Toast.LENGTH_LONG).show()
        }

        // Initialize services
        emergencyService = EmergencyService(this)
        locationService = LocationService(this)

        // Initialize text-to-speech for voice assistance
        initializeTextToSpeech()

        // Set up the UI
        setupActionBar()
        setupClickListeners()
        setupAccessibilityFeatures()

        // Update UI based on user preferences
        updateUIForAccessibility()

        // Load user data and update header
        loadUserData()
    }

    /**
     * Initialize text-to-speech for voice assistance
     */
    private fun initializeTextToSpeech() {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this, this)
        }
        isVoiceAssistanceEnabled = true
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(
                    this,
                    "Language not supported for voice assistance",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isVoiceAssistanceEnabled = true
                speakText("Welcome to Senior Hub. How can I help you today?")
            }
        }
    }

    /**
     * Set up the action bar with proper title and accessibility
     */
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "SeniorHub"
            setDisplayShowTitleEnabled(true)
        }
    }

    /**
     * Set up click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        try {
            // Emergency SOS button - Most important feature
            findViewById<MaterialButton>(R.id.btnSOS)?.setOnClickListener {
                try {
                    handleSOSButtonClick()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in SOS button click: ${e.message}", e)
                    Toast.makeText(this, "Error with emergency button. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Voice assistance toggle
            findViewById<ImageButton>(R.id.btnVoiceAssistance)?.setOnClickListener {
                try {
                    enforceVoiceAssistanceAlwaysOn()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in voice assistance button: ${e.message}", e)
                    Toast.makeText(this, "Error with voice assistance. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Logout button
            findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
                try {
                    showLogoutConfirmation()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in logout button: ${e.message}", e)
                    Toast.makeText(this, "Error with logout. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Profile icon in header
            findViewById<ImageButton>(R.id.btnProfile)?.setOnClickListener {
                try {
                    navigateToProfile()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening profile: ${e.message}", e)
                    Toast.makeText(this, "Error opening profile. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Feature cards
            setupFeatureCards()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up click listeners: ${e.message}", e)
            Toast.makeText(this, "Error setting up interface. Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupFeatureCards() {
        try {
            // Emergency Services Card
            findViewById<CardView>(R.id.cardEmergency)?.setOnClickListener {
                try {
                    speakAndNavigate(
                        "Opening Emergency Services",
                        EmergencyServicesActivity::class.java
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening Emergency Services: ${e.message}", e)
                    Toast.makeText(this, "Error opening Emergency Services. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Reminders Card
            findViewById<CardView>(R.id.cardReminders)?.setOnClickListener {
                try {
                    speakAndNavigate(
                        "Opening Reminders",
                        RemindersActivity::class.java
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening Reminders: ${e.message}", e)
                    Toast.makeText(this, "Error opening Reminders. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Health Tracking Card
            findViewById<CardView>(R.id.cardHealth)?.setOnClickListener {
                try {
                    navigateToHealth()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening Health Tracking: ${e.message}", e)
                    Toast.makeText(this, "Error opening Health Tracking. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Language Selection Card
            findViewById<CardView>(R.id.cardLanguage)?.setOnClickListener {
                try {
                    speakAndNavigate(
                        "Opening Language Selection",
                        LanguageSelectionActivity::class.java
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening Language Selection: ${e.message}", e)
                    Toast.makeText(this, "Error opening Language Selection. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Benefits Card
            findViewById<CardView>(R.id.cardBenefits)?.setOnClickListener {
                try {
                    speakAndNavigate(
                        "Opening Benefits",
                        BenefitsActivity::class.java
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening Benefits: ${e.message}", e)
                    Toast.makeText(this, "Error opening Benefits. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Social Features Card
            findViewById<CardView>(R.id.cardSocial)?.setOnClickListener {
                try {
                    Log.d("MainActivity", "Social Features Card clicked")
                    speakText("Opening Social Features")
                    val intent = Intent(this, SocialActivity::class.java)
                    startActivity(intent)
                    Log.d("MainActivity", "SocialActivity intent started successfully")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error opening SocialActivity: ${e.message}", e)
                    Toast.makeText(this, "Error opening Social Features. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up feature cards: ${e.message}", e)
            Toast.makeText(this, "Error setting up feature cards. Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Load user data and update header
     */
    private fun loadUserData() {
        try {
            Log.d("MainActivity", "Starting loadUserData...")
            
            if (FirebaseManager.isUserLoggedIn()) {
                val currentUser = FirebaseManager.getCurrentUser()
                val userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Senior User"
                Log.d("MainActivity", "User logged in: $userName")
                
                // Try to load profile image URL from repository
                lifecycleScope.launch {
                    try {
                        Log.d("MainActivity", "Fetching user data from repository...")
                        when (val result = UserRepository.getInstance().getCurrentUser()) {
                            is Result.Success -> {
                                val user = result.data?.user
                                Log.d("MainActivity", "User data fetched: ${user != null}")
                                
                                if (user != null) {
                                    val displayName = "${user.firstName} ${user.lastName}".trim()
                                    val finalUserName = if (displayName.isNotEmpty()) displayName else userName
                                    Log.d("MainActivity", "Final user name: $finalUserName")
                                    
                                    // Prefer Firestore profileImageUrl; fall back to Firebase Auth photoUrl
                                    val photoFromFirestore = user.profileImageUrl
                                    val photoFromAuth = FirebaseManager.getCurrentUser()?.photoUrl?.toString() ?: ""
                                    val resolvedPhotoUrl = when {
                                        !photoFromFirestore.isNullOrEmpty() && photoFromFirestore.startsWith("http") -> photoFromFirestore
                                        !photoFromAuth.isNullOrEmpty() && photoFromAuth.startsWith("http") -> photoFromAuth
                                        else -> ""
                                    }

                                    if (resolvedPhotoUrl.isNotEmpty()) {
                                        Log.d("MainActivity", "Profile image URL found: $resolvedPhotoUrl")
                                        bindHeaderWithImageUrl(finalUserName, resolvedPhotoUrl)
                                    } else {
                                        Log.d("MainActivity", "No valid profile image URL found, using default")
                                        bindHeader(finalUserName, R.drawable.ic_profile)
                                    }
                                } else {
                                    Log.d("MainActivity", "User data is null, using default")
                                    bindHeader(userName, R.drawable.ic_profile)
                                }
                            }
                            is Result.Error -> {
                                Log.e("MainActivity", "Error loading user: ${result.exception.message}", result.exception)
                                bindHeader(userName, R.drawable.ic_profile)
                            }
                            is Result.Loading<*> -> {
                                Log.d("MainActivity", "Loading user data...")
                                bindHeader("Loading...", R.drawable.ic_profile)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error in loadUserData coroutine: ${e.message}", e)
                        bindHeader(userName, R.drawable.ic_profile)
                    }
                }
            } else {
                Log.d("MainActivity", "User not logged in")
                bindHeader("Guest User", R.drawable.ic_profile)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading user data: ${e.message}", e)
            bindHeader("Senior User", R.drawable.ic_profile)
        }
    }

    private fun bindHeaderWithImageUrl(name: String?, photoUrl: String) {
        val nameView: TextView? = findViewById(R.id.tvUserName)
        val welcomeView: TextView? = findViewById(R.id.tvWelcome)
        val imageView: ImageView? = findViewById(R.id.ivUserProfile)

        nameView?.text = name ?: ""
        welcomeView?.text = getString(R.string.welcome_back)

        if (imageView != null) {
            try {
                Log.d("MainActivity", "Loading profile image with ImageLoader: $photoUrl")
                Log.d("MainActivity", "Image view found: ${imageView != null}")
                
                // Validate the photo URL first
                if (photoUrl.isNotEmpty() && photoUrl.startsWith("http")) {
                    // Use ImageLoader for consistent image loading with optimized Cloudinary URL
                    val optimizedUrl = ImageLoader.getOptimizedCloudinaryUrl(photoUrl, 400, 400)
                    Log.d("MainActivity", "Original URL: $photoUrl")
                    Log.d("MainActivity", "Optimized URL: $optimizedUrl")
                    
                    ImageLoader.loadProfileImage(
                        this,
                        imageView,
                        optimizedUrl,
                        R.drawable.ic_profile,
                        R.drawable.ic_profile
                    )
                } else {
                    Log.w("MainActivity", "Invalid photo URL: $photoUrl")
                    imageView.setImageResource(R.drawable.ic_profile)
                }

                imageView.contentDescription = if (name.isNullOrBlank()) {
                    getString(R.string.profile)
                } else {
                    getString(R.string.profile) + ": " + name
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading profile image: ${e.message}", e)
                imageView.setImageResource(R.drawable.ic_profile)
            }
        } else {
            Log.e("MainActivity", "ImageView is null!")
        }
    }

    /**
     * Populate header card when user data is available.
     * Safely handles empty state so UI remains subtle until data is set.
     */
    private fun bindHeader(name: String?, photoResId: Int? = null) {
        val nameView: TextView? = findViewById(R.id.tvUserName)
        val welcomeView: TextView? = findViewById(R.id.tvWelcome)
        val imageView: ImageView? = findViewById(R.id.ivUserProfile)

        nameView?.text = name ?: ""
        welcomeView?.text = getString(R.string.welcome_back)

        if (imageView != null && photoResId != null) {
            Glide.with(this)
                .load(photoResId)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)

            imageView.contentDescription = if (name.isNullOrBlank()) {
                getString(R.string.profile)
            } else {
                getString(R.string.profile) + ": " + name
            }
        }
    }

    /**
     * Set up accessibility features for senior citizens
     */
    private fun setupAccessibilityFeatures() {
        // Set content descriptions for all interactive elements
        findViewById<MaterialButton>(R.id.btnSOS)?.contentDescription =
            "Emergency SOS Button - Tap to send emergency alert to your emergency contact"

        findViewById<ImageButton>(R.id.btnVoiceAssistance)?.contentDescription =
            "Toggle Voice Assistance - Tap to enable or disable voice guidance"

        findViewById<ImageButton>(R.id.btnLogout)?.contentDescription =
            "Logout - Tap to sign out of your account"
    }

    /**
     * Handle SOS button click with proper confirmation
     */
    private fun handleSOSButtonClick() {
        // Check required permissions for emergency alert (SMS and Location only)
        val requiredPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.SEND_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (requiredPermissions.isNotEmpty()) {
            // Request required permissions
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            // All permissions granted
            showEmergencyConfirmation()
        }
    }

    /**
     * Show emergency confirmation dialog
     */
    private fun showEmergencyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Emergency Alert")
            .setMessage("This will:\n• Send SMS to your Emergency Contact\n• Share your location via Google Maps\n• Log the emergency alert")
            .setPositiveButton("Yes, Send Alert") { _, _ ->
                activateEmergencySOS()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                speakText("Emergency alert cancelled")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Activate emergency SOS - Sends SMS to emergency contact and calls emergency services
     */
    private fun activateEmergencySOS() {
        try {
            // Get current user name for emergency alert
            val currentUser = FirebaseManager.getCurrentUser()
            val seniorName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Senior User"

            // Launch coroutine to handle emergency alert
            lifecycleScope.launch {
                try {
                    // Get current location
                    val location = locationService.getCurrentLocation()

                    // Send emergency alert with location to emergency contacts only
                    when (val result = emergencyService.sendEmergencyAlert(
                        emergencyType = "SOS Button",
                        seniorName = seniorName,
                        location = location
                    )) {
                        is Result.Success -> {
                            // Also call emergency services (911)
                            callEmergencyServices("911")
                            
                            // Provide feedback - alert sent successfully
                            speakText("Emergency Alert Sent to your Emergency Contact. Calling emergency services. Help is on the way.")
                            Toast.makeText(this@MainActivity, "Emergency alert sent to your emergency contact with location. Calling emergency services. Help is on the way.", Toast.LENGTH_LONG).show()
                        }
                        is Result.Error -> {
                            // Still call emergency services even if SMS fails
                            callEmergencyServices("911")
                            
                            // Alert failed to send
                            speakText("Failed to send SMS alert. Calling emergency services directly. Please contact your emergency contact manually.")
                            Toast.makeText(this@MainActivity, "Failed to send SMS alert. Calling emergency services directly. Please contact your emergency contact manually.", Toast.LENGTH_LONG).show()
                        }
                        is Result.Loading<*> -> {
                            // Handle loading state if needed
                            speakText("Sending emergency alert...")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in emergency SOS: ${e.message}", e)
                    // Still call emergency services as fallback
                    callEmergencyServices("911")
                    
                    // Alert failed
                    speakText("Error sending emergency alert. Calling emergency services directly. Please contact your emergency contact manually.")
                    Toast.makeText(this@MainActivity, "Error sending emergency alert. Calling emergency services directly. Please contact your emergency contact manually.", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            // Handle error
            Toast.makeText(
                this,
                "Unable to send emergency alert. Please contact your emergency contact manually.",
                Toast.LENGTH_LONG
            ).show()
            speakText("Unable to send emergency alert. Please contact your emergency contact manually.")
        }
    }

    /**
     * Call emergency services directly
     */
    private fun callEmergencyServices(phoneNumber: String) {
        try {
            val emergencyIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(emergencyIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error calling emergency services: ${e.message}", e)
            // Fallback to dial intent
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(dialIntent)
        }
    }

    private fun logEmergencyAlert(type: String, details: String, triggeredBy: String) {
        try {
            if (!FirebaseConfig.isInitialized()) return
            val db = FirebaseConfig.getFirestore()
            val user = FirebaseConfig.getAuth().currentUser
            val data = hashMapOf(
                "type" to type,
                "status" to "ACTIVE",
                "timestamp" to Timestamp.now(),
                "seniorId" to (user?.uid ?: ""),
                "seniorName" to (user?.displayName ?: user?.email ?: ""),
                "details" to details,
                "triggeredBy" to triggeredBy
            )
            db.collection(FirebaseConfig.COLLECTION_EMERGENCY_ALERTS)
                .add(data)
        } catch (_: Exception) { }
    }


    /**
     * Show permission denied dialog
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("SMS and Location permissions are required for emergency alerts. Please enable them in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Toggle voice assistance on/off
     */
    private fun enforceVoiceAssistanceAlwaysOn() {
        isVoiceAssistanceEnabled = true
        if (textToSpeech == null) {
            initializeTextToSpeech()
        }
        Toast.makeText(this, "Voice assistance is always enabled", Toast.LENGTH_SHORT).show()
        speakText("Voice assistance is always enabled")
    }

    /**
     * Show logout confirmation
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Perform logout and return to login screen
     */
    private fun performLogout() {
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        speakText("Logged out successfully")
        finish()
    }

    /**
     * Navigate to Profile with authentication check
     */
    private fun navigateToProfile() {
        if (checkAuthenticationAndNavigate("Profile")) {
            speakAndNavigate("Opening Profile", ProfileActivity::class.java)
        }
    }

    /**
     * Navigate to Health with authentication check
     */
    private fun navigateToHealth() {
        if (checkAuthenticationAndNavigate("Health Tracking")) {
            speakAndNavigate("Opening Health Tracking", HealthActivity::class.java)
        }
    }

    /**
     * Check authentication before navigation
     */
    private fun checkAuthenticationAndNavigate(featureName: String): Boolean {
        return try {
            if (FirebaseManager.isUserLoggedIn()) {
                true
            } else {
                showLoginRequiredDialog(featureName)
                false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking authentication: ${e.message}")
            showLoginRequiredDialog(featureName)
            false
        }
    }

    /**
     * Show dialog when login is required
     */
    private fun showLoginRequiredDialog(featureName: String) {
        AlertDialog.Builder(this)
            .setTitle("Login Required")
            .setMessage("You need to be logged in to access $featureName. Would you like to login now?")
            .setPositiveButton("Login") { _, _ ->
                navigateToLogin()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                speakText("$featureName requires login. Please login first.")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Navigate to login screen
     */
    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            speakText("Redirecting to login screen")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error navigating to login: ${e.message}")
            Toast.makeText(this, "Error opening login screen", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Speak text and navigate to activity
     */
    private fun speakAndNavigate(text: String, activityClass: Class<*>) {
        try {
            // Check if activity class exists
            if (activityClass == null) {
                Log.e("MainActivity", "Activity class is null")
                Toast.makeText(this, "Activity not found. Please try again.", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Speak text safely
            try {
                speakText(text)
            } catch (e: Exception) {
                Log.w("MainActivity", "Error speaking text: ${e.message}")
                // Continue without speaking if TTS fails
            }
            
            // Create and start intent safely
            val intent = Intent(this, activityClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error navigating to ${activityClass.simpleName}: ${e.message}", e)
            
            // Provide specific error messages based on the activity
            val errorMessage = when (activityClass.simpleName) {
                "SocialActivity" -> "Social services are temporarily unavailable. Please check your internet connection."
                "EmergencyServicesActivity" -> "Emergency services are not available. Please try again or contact support."
                "RemindersActivity" -> "Reminders feature is not available. Please try again."
                "HealthActivity" -> "Health tracking is not available. Please try again."
                "BenefitsActivity" -> "Benefits information is not available. Please try again."
                "LanguageSelectionActivity" -> "Language selection is not available. Please try again."
                else -> "Error opening ${activityClass.simpleName}. Please try again."
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Speak text using text-to-speech
     */
    private fun speakText(text: String) {
        try {
            if (isVoiceAssistanceEnabled && textToSpeech != null && !text.isBlank()) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Error speaking text: ${e.message}")
            // Continue silently if TTS fails
        }
    }

    /**
     * Apply accessibility settings
     */
    private fun applyAccessibilitySettings() {
        // This will be implemented to apply theme, font size, etc.
    }

    /**
     * Update UI based on accessibility preferences
     */
    private fun updateUIForAccessibility() {
        isVoiceAssistanceEnabled = true
    }

    override fun onResume() {
        super.onResume()
        // Always keep voice assistance enabled
        isVoiceAssistanceEnabled = true
        if (textToSpeech == null) {
            initializeTextToSpeech()
        }

        // Refresh user data and profile image when returning to MainActivity
        // This ensures profile image updates are reflected immediately
        loadUserData()
    }

    override fun onStart() {
        super.onStart()
        // Also refresh user data when activity starts
        loadUserData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // No menu for now to avoid resource issues
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                speakText("Help information: This is the main screen of Senior Hub. Tap any card to access that feature, or tap and hold for help.")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}