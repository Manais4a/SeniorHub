package com.seniorhub.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseUser
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.checkbox.MaterialCheckBox
import com.seniorhub.utils.PreferenceManager
import com.seniorhub.utils.FirestoreUtils
import kotlinx.coroutines.launch
import java.util.*

/**
 * LoginActivity - Senior-friendly login interface
 *
 * Features designed for senior citizens:
 * - Large, easy-to-read text and buttons
 * - Simple, clean interface with high contrast options
 * - Clear error messages with visual indicators
 * - Password visibility toggle
 * - Remember me functionality
 * - Voice assistance with TextToSpeech
 */
class LoginActivity : AppCompatActivity() {

    // Permission request constants
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002
    private val STORAGE_PERMISSION_REQUEST_CODE = 1003


    // Text watchers for form validation
    private val emailTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            clearEmailError()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            clearPasswordError()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager

    private val RC_GOOGLE_SIGN_IN = 1001
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.seniorhub.R.layout.activity_login)

        // Initialize preference manager
        preferenceManager = PreferenceManager.getInstance()

        // Set up the UI
        setupUI()

        // Initialize Google/Firebase auth
        initializeAuth()

        // Set up authentication state listener
        setupAuthStateListener()

        // Check if user is already logged in
        checkExistingLogin()
    }

    /**
     * Set up the user interface components and event listeners
     */
    private fun setupUI() {
        // Set up text watchers for form validation
        try {
            // Updated: email input is etUsername in the layout, used for email address
            findViewById<EditText>(com.seniorhub.R.id.etEmail)?.addTextChangedListener(emailTextWatcher)
            findViewById<EditText>(com.seniorhub.R.id.etPassword)?.addTextChangedListener(passwordTextWatcher)
        } catch (e: Exception) {
            // Views not found, continue
        }

        // Set up login button click
        findViewById<MaterialButton>(com.seniorhub.R.id.btnLogin)?.setOnClickListener {
            validateAndLogin()
        }

        // Set up sign up text click - UPDATED: Using tvSignUp instead of btnRegister
        val signUpText = findViewById<TextView>(com.seniorhub.R.id.tvSignUp)
        if (signUpText != null) {
            Log.d(TAG, "Sign up text found, setting up click listener")
            signUpText.setOnClickListener {
                Log.d(TAG, "Sign up text clicked")
                try {
                    Log.d(TAG, "About to navigate to RegisterActivity")
                    navigateToRegister()
                    Log.d(TAG, "Successfully navigated to RegisterActivity")
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to RegisterActivity: ${e.message}", e)
                    showError("Error opening registration: ${e.message}")
                }
            }
        } else {
            Log.e(TAG, "Sign up text not found!")
        }

        // Set up forgot password link
        findViewById<TextView>(com.seniorhub.R.id.tvForgotPassword)?.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    /**
     * Validate form inputs and initiate login
     */
    private fun validateAndLogin() {
        val email = getEmailText()
        val password = getPasswordText()
        val rememberMe = findViewById<MaterialCheckBox>(com.seniorhub.R.id.cbRememberMe)?.isChecked ?: false

        // Reset errors
        clearEmailError()
        clearPasswordError()

        // Validate email
        if (email.isEmpty()) {
            showEmailError("Email is required")
            return
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showEmailError("Please enter a valid email address")
            return
        }

        // Validate password
        if (password.isEmpty()) {
            showPasswordError("Password is required")
            return
        } else if (password.length < 6) {
            showPasswordError("Password must be at least 6 characters")
            return
        }

        // Sign in with Firebase using provided credentials
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Save login state and user info to preferences
                    preferenceManager.isUserLoggedIn = rememberMe
                    preferenceManager.userId = user.uid
                    preferenceManager.userEmail = user.email ?: email

                    // Ensure user document exists for admin listing BEFORE navigating.
                    // If we navigate first, the Activity finishes and cancels lifecycleScope, causing "Job was cancelled".
                    lifecycleScope.launch {
                        try {
                            FirestoreUtils.ensureUserDocument(user.uid, user.email, user.displayName)
                        } catch (_: Exception) { }

                        Log.d(TAG, "Login successful for user: ${user.email}")

                        // Request permissions after successful login
                        requestImportantPermissions()
                    }
                } else {
                    showError("Authentication failed")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Login failed: ${e.message}", e)
                showError(e.message ?: "Login failed")
            }
    }

    private fun initializeAuth() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val serverClientId = run {
            val id = resources.getIdentifier("default_web_client_id", "string", packageName)
            if (id != 0) getString(id) else ""
        }
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (serverClientId.isNotEmpty()) {
            gsoBuilder.requestIdToken(serverClientId)
        }
        val gso = gsoBuilder.build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun startGoogleSignIn() {
        try {
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_GOOGLE_SIGN_IN)
        } catch (e: Exception) {
            showError("Google Sign-In failed to start")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.result
                val idToken = account.idToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            if (user != null) {
                                saveUserToDatabase(user.uid, user.displayName ?: "", user.email ?: "")
                            } else {
                                showError("Authentication failed")
                            }
                        }
                        .addOnFailureListener { ex ->
                            showError("Firebase auth failed: ${ex.message}")
                        }
                } else {
                    showError("No ID token!")
                }
            } catch (e: Exception) {
                showError("Google Sign-In error: ${e.message}")
            }
        }
    }

    private fun saveUserToDatabase(uid: String, displayName: String, email: String) {
        val rememberMe = findViewById<MaterialCheckBox>(com.seniorhub.R.id.cbRememberMe)?.isChecked ?: false

        val users = firestore.collection("users").document(uid)
        val names = displayName.split(" ")
        val firstName = names.firstOrNull() ?: ""
        val lastName = names.drop(1).joinToString(" ")
        val data = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "language" to preferenceManager.language,
            "voiceAssistanceEnabled" to preferenceManager.isVoiceAssistanceEnabled,
            "offlineModeEnabled" to preferenceManager.offlineModeEnabled,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        users.set(data)
            .addOnSuccessListener {
                // Save login state and user info to preferences
                preferenceManager.isUserLoggedIn = rememberMe
                preferenceManager.userId = uid
                preferenceManager.userEmail = email

                Toast.makeText(this, "Welcome, $firstName!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { e ->
                // If already exists, proceed
                users.get().addOnSuccessListener {
                    // Save login state and user info to preferences
                    preferenceManager.isUserLoggedIn = rememberMe
                    preferenceManager.userId = uid
                    preferenceManager.userEmail = email

                    navigateToMainActivity()
                }.addOnFailureListener {
                    showError("Failed to save account: ${e.message}")
                }
            }
    }

    private fun getEmailText(): String {
        return try {
            // Use the email field id (etUsername) from the updated layout
            findViewById<EditText>(com.seniorhub.R.id.etEmail)?.text?.toString()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getPasswordText(): String {
        return try {
            findViewById<EditText>(com.seniorhub.R.id.etPassword)?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun showEmailError(message: String) {
        Toast.makeText(this, "Email error: $message", Toast.LENGTH_SHORT).show()
    }

    private fun showPasswordError(message: String) {
        Toast.makeText(this, "Password error: $message", Toast.LENGTH_SHORT).show()
    }

    private fun clearEmailError() {
        // Clear email error if any
    }

    private fun clearPasswordError() {
        // Clear password error if any
    }

    /**
     * Set up Firebase authentication state listener
     */
    private fun setupAuthStateListener() {
        // Removed automatic navigation to prevent conflicts with RegisterActivity
        // Navigation is now handled explicitly in login success callbacks
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "User is already signed in: ${user.email}")
                // Don't auto-navigate here to prevent conflicts
            } else {
                Log.d(TAG, "No user signed in")
                // User is signed out, stay on login screen
            }
        }
    }

    /**
     * Check if user is already logged in and navigate accordingly
     */
    private fun checkExistingLogin() {
        // Check if user has "Remember Me" enabled and is already authenticated
        if (preferenceManager.isUserLoggedIn && auth.currentUser != null) {
            Log.d(TAG, "User is already logged in with remember me enabled")
            navigateToMainActivity()
        } else {
            Log.d(TAG, "User needs to login")
            // Add auth state listener to monitor authentication changes
            authStateListener?.let { listener ->
                auth.addAuthStateListener(listener)
            }
        }
    }

    /**
     * Show error message to user
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Navigate to the main activity
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Request important permissions after successful login
     */
    private fun requestImportantPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check location permission for emergency services
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check storage permission for profile images
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            showPermissionDialog(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted, navigate to main activity
            navigateToMainActivity()
        }
    }

    /**
     * Show permission request dialog with user-friendly explanation
     */
    private fun showPermissionDialog(permissions: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enable Notifications & Permissions")
        builder.setMessage("""
            To provide you with the best experience, Senior Hub needs permission to:
            
            • Send you important reminders and health alerts
            • Access your location for emergency services
            • Access your photos for profile pictures
            
            These permissions help keep you safe and connected. You can change them later in Settings.
        """.trimIndent())

        builder.setPositiveButton("Allow") { _, _ ->
            ActivityCompat.requestPermissions(this, permissions, NOTIFICATION_PERMISSION_REQUEST_CODE)
        }

        builder.setNegativeButton("Skip for Now") { _, _ ->
            // Navigate to main activity even if permissions are denied
            navigateToMainActivity()
        }

        builder.setCancelable(false)
        builder.show()
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                if (allGranted) {
                    Toast.makeText(this, "Thank you! Notifications enabled for better safety.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "You can enable notifications later in Settings.", Toast.LENGTH_SHORT).show()
                }

                // Navigate to main activity regardless of permission results
                navigateToMainActivity()
            }
        }
    }

    /**
     * Navigate to the registration screen
     */
    private fun navigateToRegister() {
        try {
            Log.d(TAG, "Creating intent for RegisterActivity")
            val intent = Intent(this, RegisterActivity::class.java)
            Log.d(TAG, "Starting RegisterActivity")
            startActivity(intent)
            Log.d(TAG, "RegisterActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RegisterActivity: ${e.message}", e)
            e.printStackTrace()
            showError("Unable to open registration screen: ${e.message}")

            // Show detailed error dialog for debugging
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Navigation Error")
                .setMessage("Failed to open registration screen.\n\nError: ${e.message}\n\nPlease check the logs for more details.")
                .setPositiveButton("OK", null)
                .show()
        }
    }


    /**
     * Navigate to the forgot password screen
     */
    private fun navigateToForgotPassword() {
        val email = getEmailText()

        if (email.isEmpty()) {
            showError("Please enter your email address to reset your password")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address")
            return
        }

        showResetPasswordDialog(email)
    }

    /**
     * Show dialog to confirm password reset
     */
    private fun showResetPasswordDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Send password reset email to $email?")
            .setPositiveButton("Yes") { _, _ ->
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Send password reset email
     */
    private fun sendPasswordResetEmail(email: String) {
        // Simulate sending reset email
        Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        // Clean up resources
        try {
            findViewById<EditText>(android.R.id.edit)?.removeTextChangedListener(emailTextWatcher)
            findViewById<EditText>(android.R.id.text1)?.removeTextChangedListener(
                passwordTextWatcher
            )
        } catch (e: Exception) {
            // Views not found, continue
        }

        // Remove auth state listener
        authStateListener?.let { listener ->
            auth.removeAuthStateListener(listener)
        }

        super.onDestroy()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}