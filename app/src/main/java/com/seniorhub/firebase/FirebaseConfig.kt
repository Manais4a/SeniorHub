package com.seniorhub.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

/**
 * Firebase Configuration and Initialization
 * Handles Firebase setup for the SeniorHub application
 * 
 * @author SeniorHub Team
 * @version 1.0.0
 */
object FirebaseConfig {
    
    private const val TAG = "FirebaseConfig"
    
    // Firebase instances
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var storage: FirebaseStorage? = null
    private var database: FirebaseDatabase? = null
    
    // Collection names
    const val COLLECTION_USERS = "users"
    const val COLLECTION_HEALTH_RECORDS = "healthRecords"
    const val COLLECTION_EMERGENCY_ALERTS = "emergencyAlerts"
    const val COLLECTION_BENEFITS = "benefits"
    const val COLLECTION_COMMUNITY_POSTS = "communityPosts"
    const val COLLECTION_MEDICATION_REMINDERS = "medicationReminders"
    const val COLLECTION_APPOINTMENTS = "appointments"
    const val COLLECTION_EMERGENCY_CONTACTS = "emergencyContacts"
    const val COLLECTION_SOCIAL_CONNECTIONS = "socialConnections"
    const val COLLECTION_ACTIVITY_LOGS = "activityLogs"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    
    /**
     * Initialize Firebase services
     * Call this method in Application class or MainActivity
     */
    fun initializeFirebase(context: Context) {
        try {
            // Initialize Firebase App
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase App initialized")
            }
            
            // Initialize Firestore with offline persistence
            firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore?.setFirestoreSettings(settings)
            Log.d(TAG, "Firestore initialized with offline persistence")
            
            // Initialize Auth
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase Auth initialized")
            
            // Initialize Storage
            storage = FirebaseStorage.getInstance()
            Log.d(TAG, "Firebase Storage initialized")
            
            // Initialize Realtime Database
            database = FirebaseDatabase.getInstance()
            database?.setPersistenceEnabled(true)
            Log.d(TAG, "Firebase Database initialized with persistence")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
            throw RuntimeException("Failed to initialize Firebase", e)
        }
    }
    
    /**
     * Get Firestore instance
     */
    fun getFirestore(): FirebaseFirestore {
        return firestore ?: throw IllegalStateException("Firebase not initialized. Call initializeFirebase() first.")
    }
    
    /**
     * Get Auth instance
     */
    fun getAuth(): FirebaseAuth {
        return auth ?: throw IllegalStateException("Firebase not initialized. Call initializeFirebase() first.")
    }
    
    /**
     * Get Storage instance
     */
    fun getStorage(): FirebaseStorage {
        return storage ?: throw IllegalStateException("Firebase not initialized. Call initializeFirebase() first.")
    }
    
    /**
     * Get Database instance
     */
    fun getDatabase(): FirebaseDatabase {
        return database ?: throw IllegalStateException("Firebase not initialized. Call initializeFirebase() first.")
    }
    
    /**
     * Check if Firebase is initialized
     */
    fun isInitialized(): Boolean {
        return firestore != null && auth != null && storage != null && database != null
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth?.currentUser?.uid
    }
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth?.currentUser != null
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth?.signOut()
        Log.d(TAG, "User signed out")
    }
}
