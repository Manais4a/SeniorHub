package com.seniorhub.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * FirebaseManager - Centralized Firebase Operations Management
 *
 * This singleton class handles all Firebase-related operations for the Senior Hub application including:
 * - Authentication (login, registration, password reset)
 * - Realtime Database operations (user data, health records, reminders)
 * - Cloud Storage (profile images, documents, medical records)
 * - Cloud Messaging (push notifications, emergency alerts)
 * - Real-time listeners for live data updates
 * - Error handling and offline support
 * - Security and data validation
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Firebase service instances
    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    private var storage: FirebaseStorage? = null
    private var messaging: FirebaseMessaging? = null

    // Database references
    // Database references
    private var usersRef: DatabaseReference? = null
    private var healthRecordsRef: DatabaseReference? = null
    private var remindersRef: DatabaseReference? = null
    private var emergencyContactsRef: DatabaseReference? = null

    // Initialization state
    private var isInitialized = false

    /**
     * Initialize Firebase services with comprehensive error handling
     * Must be called before using any Firebase operations
     *
     * @param context Application context for Firebase initialization
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "Initializing Firebase services...")

            // Initialize Firebase App if not already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase App initialized")
            } else {
                Log.d(TAG, "Firebase App already initialized")
            }

            // Initialize Firebase services with error handling
            initializeFirebaseServices()

            // Configure Realtime Database settings for offline support
            configureDatabaseSettings()

            // Initialize Cloud Messaging for notifications
            initializeCloudMessaging()

            isInitialized = true
            Log.i(TAG, "Firebase services initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error initializing Firebase: ${e.message}", e)
            throw RuntimeException("Failed to initialize Firebase services", e)
        }
    }

    /**
     * Initialize core Firebase service instances
     */
    private fun initializeFirebaseServices() {
        try {
            // Initialize Firebase Authentication
            auth = FirebaseAuth.getInstance().also {
                Log.d(TAG, "Firebase Auth initialized")
            }

            // Initialize Realtime Database
            database = FirebaseDatabase.getInstance().also {
                Log.d(TAG, "Firebase Realtime Database initialized")
            }

            // Initialize Firebase Storage
            storage = FirebaseStorage.getInstance().also {
                Log.d(TAG, "Firebase Storage initialized")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase services: ${e.message}", e)
            throw e
        }
    }

    /**
     * Configure Realtime Database settings for optimal performance and offline support
     */
    private fun configureDatabaseSettings() {
        try {
            database?.let { db ->
                // Enable offline persistence
                db.setPersistenceEnabled(true)

                // Initialize database references
                usersRef = db.getReference("users")
                healthRecordsRef = db.getReference("healthRecords")
                remindersRef = db.getReference("reminders")
                emergencyContactsRef = db.getReference("emergencyContacts")

                Log.d(TAG, "Realtime Database settings configured successfully")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Warning: Could not configure Realtime Database settings: ${e.message}", e)
            // Don't throw - this is not critical for basic functionality
        }
    }

    /**
     * Initialize Firebase Cloud Messaging for push notifications
     */
    private fun initializeCloudMessaging() {
        try {
            messaging = FirebaseMessaging.getInstance().also {
                Log.d(TAG, "Firebase Messaging initialized")

                // Subscribe to general senior hub topics
                it.subscribeToTopic("senior_hub_updates")
                it.subscribeToTopic("health_reminders")
                it.subscribeToTopic("emergency_alerts")

                Log.d(TAG, "Subscribed to notification topics")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Warning: Could not initialize Firebase Messaging: ${e.message}", e)
            // Don't throw - messaging is not critical for core functionality
        }
    }

    /**
     * Get Realtime Database instance with initialization check
     *
     * @return FirebaseDatabase instance
     * @throws IllegalStateException if Firebase is not initialized
     */
    fun getDatabase(): FirebaseDatabase {
        checkInitialization()
        return database ?: throw IllegalStateException("Realtime Database not properly initialized")
    }

    /**
     * Get Firebase Auth instance with initialization check
     *
     * @return FirebaseAuth instance
     * @throws IllegalStateException if Firebase is not initialized
     */
    fun getAuth(): FirebaseAuth {
        checkInitialization()
        return auth ?: throw IllegalStateException("Firebase Auth not properly initialized")
    }

    /**
     * Get Firebase Storage instance with initialization check
     *
     * @return FirebaseStorage instance
     * @throws IllegalStateException if Firebase is not initialized
     */
    fun getStorage(): FirebaseStorage {
        checkInitialization()
        return storage ?: throw IllegalStateException("Firebase Storage not properly initialized")
    }

    /**
     * Get Firebase Messaging instance with initialization check
     *
     * @return FirebaseMessaging instance or null if not available
     */
    fun getMessaging(): FirebaseMessaging? {
        checkInitialization()
        return messaging
    }

    /**
     * Get users database reference
     */
    fun getUsersReference(): DatabaseReference {
        checkInitialization()
        return usersRef ?: throw IllegalStateException("Users reference not initialized")
    }

    /**
     * Get health records database reference
     */
    fun getHealthRecordsReference(): DatabaseReference {
        checkInitialization()
        return healthRecordsRef
            ?: throw IllegalStateException("Health records reference not initialized")
    }

    /**
     * Get reminders database reference
     */
    fun getRemindersReference(): DatabaseReference {
        checkInitialization()
        return remindersRef ?: throw IllegalStateException("Reminders reference not initialized")
    }

    /**
     * Get emergency contacts database reference
     */
    fun getEmergencyContactsReference(): DatabaseReference {
        checkInitialization()
        return emergencyContactsRef
            ?: throw IllegalStateException("Emergency contacts reference not initialized")
    }

    /**
     * Check if user is currently logged in
     *
     * @return Boolean indicating authentication status
     */
    fun isUserLoggedIn(): Boolean {
        return try {
            checkInitialization()
            auth?.currentUser != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status: ${e.message}", e)
            false
        }
    }

    /**
     * Get current authenticated user
     *
     * @return FirebaseUser if logged in, null otherwise
     */
    fun getCurrentUser(): FirebaseUser? {
        return try {
            checkInitialization()
            auth?.currentUser
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}", e)
            null
        }
    }

    /**
     * Get current user ID
     *
     * @return String user ID if logged in, null otherwise
     */
    fun getCurrentUserId(): String? {
        return getCurrentUser()?.uid
    }

    /**
     * Get current user email
     *
     * @return String user email if available, null otherwise
     */
    fun getCurrentUserEmail(): String? {
        return getCurrentUser()?.email
    }

    /**
     * Sign out the current user with proper cleanup
     */
    fun signOut() {
        try {
            checkInitialization()
            auth?.signOut()

            // Clear any cached user data
            clearUserCache()

            Log.i(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out: ${e.message}", e)
        }
    }

    /**
     * Sign in user with email and password
     *
     * @param email User email address
     * @param password User password
     * @return Result indicating success or failure
     */
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            checkInitialization()

            if (email.isBlank() || password.isBlank()) {
                return Result.Error(IllegalArgumentException("Email and password cannot be empty"))
            }

            // Validate email format before sending to Firebase
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                return Result.Error(IllegalArgumentException("Please enter a valid email address"))
            }

            val authResult = auth!!.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user

            if (user != null) {
                Log.i(TAG, "User signed in successfully: ${user.email}")
                Result.Success(user)
            } else {
                Log.e(TAG, "Sign in failed: User object is null")
                Result.Error(Exception("Authentication failed: User object is null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            // Provide more user-friendly error messages
            val errorMessage = when {
                e.message?.contains("badly formatted") == true -> "Please enter a valid email address"
                e.message?.contains("invalid") == true -> "Invalid email or password"
                e.message?.contains("network") == true -> "Please check your internet connection"
                else -> e.message ?: "Login failed"
            }
            Result.Error(Exception(errorMessage))
        }
    }

    /**
     * Create new user account with email and password
     *
     * @param email User email address
     * @param password User password
     * @return Result indicating success or failure
     */
    /**
     * Create new user account with email and password
     *
     * @param email User email address
     * @param password User password
     * @return Result indicating success or failure
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            checkInitialization()

            if (email.isBlank() || password.isBlank()) {
                return Result.Error(IllegalArgumentException("Email and password cannot be empty"))
            }

            // Validate email format before sending to Firebase
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                return Result.Error(IllegalArgumentException("Please enter a valid email address"))
            }

            if (password.length < 6) {
                return Result.Error(
                    IllegalArgumentException("Password must be at least 6 characters")
                )
            }

            // Check if email was previously deleted and can be reused
            val canReuseEmail = checkEmailReusability(email.trim())
            if (!canReuseEmail) {
                return Result.Error(Exception("This email is currently in use. Please use a different email address."))
            }

            val authResult = auth!!.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user

            if (user != null) {
                Log.i(TAG, "User account created successfully: ${user.email}")

                // Clean up deleted user record if this email was previously deleted
                cleanupDeletedUserRecord(email.trim())

                // Send email verification
                sendEmailVerification(user)

                Result.Success(user)
            } else {
                Log.e(TAG, "Account creation failed: User object is null")
                Result.Error(Exception("Account creation failed: User object is null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Account creation error: ${e.message}", e)
            // Provide more user-friendly error messages
            val errorMessage = when {
                e.message?.contains("badly formatted") == true -> "Please enter a valid email address"
                e.message?.contains("already in use") == true -> "This email is already registered"
                e.message?.contains("network") == true -> "Please check your internet connection"
                else -> e.message ?: "Registration failed"
            }
            Result.Error(Exception(errorMessage))
        }
    }

    /**
     * Send password reset email
     *
     * @param email User email address
     * @return Result indicating success or failure
     */
    /**
     * Send password reset email
     *
     * @param email User email address
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            checkInitialization()

            if (email.isBlank()) {
                return Result.Error(IllegalArgumentException("Email cannot be empty"))
            }

            auth!!.sendPasswordResetEmail(email).await()
            Log.i(TAG, "Password reset email sent to: $email")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Send email verification to current user
     *
     * @param user FirebaseUser to send verification to
     */
    /**
     * Send email verification to the specified user
     *
     * @param user FirebaseUser to send verification to
     */
    private suspend fun sendEmailVerification(user: FirebaseUser) {
        try {
            user.sendEmailVerification().await()
            Log.d(TAG, "Email verification sent to: ${user.email}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send email verification: ${e.message}", e)
        }
    }

    /**
     * Subscribe to topic for push notifications
     *
     * @param topic Topic name to subscribe to
     */
    suspend fun subscribeToTopic(topic: String): Result<Unit> {
        return try {
            messaging?.subscribeToTopic(topic)?.await()
            Log.d(TAG, "Subscribed to topic: $topic")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topic $topic: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Unsubscribe from topic for push notifications
     *
     * @param topic Topic name to unsubscribe from
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> {
        return try {
            messaging?.unsubscribeFromTopic(topic)?.await()
            Log.d(TAG, "Unsubscribed from topic: $topic")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from topic $topic: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get FCM token for push notifications
     *
     * @return Result with FCM token if successful
     */
    suspend fun getFCMToken(): Result<String> {
        return try {
            val token = messaging?.token?.await()
            if (token != null) {
                Log.d(TAG, "FCM token retrieved successfully")
                Result.Success(token)
            } else {
                Log.e(TAG, "FCM token is null")
                Result.Error(Exception("Failed to retrieve FCM token"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Check if Firebase is properly initialized
     *
     * @throws IllegalStateException if not initialized
     */
    private fun checkInitialization() {
        if (!isInitialized) {
            throw IllegalStateException("Firebase not initialized. Call initialize() first.")
        }
    }

    /**
     * Clear user-specific cached data
     */
    private fun clearUserCache() {
        try {
            // Clear any cached user preferences or data
            Log.d(TAG, "User cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing user cache: ${e.message}", e)
        }
    }

    /**
     * Check if Firebase services are available
     *
     * @return Boolean indicating availability
     */
    fun areServicesAvailable(): Boolean {
        return try {
            isInitialized &&
                    auth != null &&
                    database != null &&
                    storage != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service availability: ${e.message}", e)
            false
        }
    }

    /**
     * Check if an email can be reused (was previously deleted)
     *
     * @param email Email to check
     * @return Boolean indicating if email can be reused
     */
    private suspend fun checkEmailReusability(email: String): Boolean {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // Check if email exists in deleted_users collection
            val deletedUserQuery = firestore.collection("deleted_users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            // If found in deleted_users, email can be reused
            if (!deletedUserQuery.isEmpty) {
                Log.d(TAG, "Email $email found in deleted users - can be reused")
                return true
            }
            
            // Check if email exists in active users
            val activeUserQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("isDeleted", false)
                .limit(1)
                .get()
                .await()
            
            // If not found in active users, email can be reused
            if (activeUserQuery.isEmpty) {
                Log.d(TAG, "Email $email not found in active users - can be reused")
                return true
            }
            
            Log.d(TAG, "Email $email found in active users - cannot be reused")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email reusability: ${e.message}", e)
            // If we can't check, allow the attempt (Firebase Auth will handle it)
            true
        }
    }

    /**
     * Clean up deleted user record when email is reused
     *
     * @param email Email that was reused
     */
    private suspend fun cleanupDeletedUserRecord(email: String) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val deletedUserQuery = firestore.collection("deleted_users")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            // Delete all records for this email from deleted_users
            val batch = firestore.batch()
            deletedUserQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "Cleaned up deleted user records for email: $email")
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up deleted user record: ${e.message}", e)
        }
    }

    /**
     * Get Firebase initialization status
     *
     * @return Boolean indicating if Firebase is initialized
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Save health record to Firebase Firestore with proper error handling
     *
     * @param healthRecord HealthRecord to save
     * @return Result indicating success or failure
     */
    suspend fun saveHealthRecord(healthRecord: com.seniorhub.models.HealthRecord): com.seniorhub.utils.Result<String> {
        return try {
            checkInitialization()
            
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")
            
            // Generate document ID if not provided
            val recordId = if (healthRecord.id.isEmpty()) {
                java.util.UUID.randomUUID().toString()
            } else {
                healthRecord.id
            }
            
            // Create updated record with proper timestamps
            val updatedRecord = healthRecord.copy(
                id = recordId,
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )
            
            // Save to Firestore
            healthRecordsCollection.document(recordId).set(updatedRecord).await()
            
            Log.d(TAG, "Health record saved successfully: $recordId")
            com.seniorhub.utils.Result.Success(recordId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving health record: ${e.message}", e)
            com.seniorhub.utils.Result.Error(e)
        }
    }

    /**
     * Get health records for a user from Firebase Firestore
     *
     * @param userId String ID of the user
     * @return Result with list of health records
     */
    suspend fun getHealthRecords(userId: String): com.seniorhub.utils.Result<List<com.seniorhub.models.HealthRecord>> {
        return try {
            checkInitialization()
            
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")
            
            val querySnapshot = healthRecordsCollection
                .whereEqualTo("seniorId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val records = mutableListOf<com.seniorhub.models.HealthRecord>()
            for (document in querySnapshot) {
                try {
                    val healthRecord = document.toObject(com.seniorhub.models.HealthRecord::class.java)
                    healthRecord.id = document.id
                    records.add(healthRecord)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing health record document ${document.id}: ${e.message}")
                    continue
                }
            }
            
            Log.d(TAG, "Retrieved ${records.size} health records for user: $userId")
            com.seniorhub.utils.Result.Success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving health records: ${e.message}", e)
            com.seniorhub.utils.Result.Error(e)
        }
    }
}
