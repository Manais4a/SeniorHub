package com.seniorhub.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Firebase Configuration Manager
 * Centralized configuration for all Firebase services
 */
object FirebaseConfig {
    
    // Firebase instances
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }
    
    // Database references
    val usersRef by lazy { database.getReference("users") }
    val healthRecordsRef by lazy { database.getReference("health_records") }
    val emergencyAlertsRef by lazy { database.getReference("emergency_alerts") }
    val remindersRef by lazy { database.getReference("reminders") }
    val socialFeaturesRef by lazy { database.getReference("social_features") }
    val benefitsRef by lazy { database.getReference("benefits") }
    val activitiesRef by lazy { database.getReference("activities") }
    val notificationsRef by lazy { database.getReference("notifications") }
    
    // Firestore collections
    val usersCollection by lazy { firestore.collection("users") }
    val healthRecordsCollection by lazy { firestore.collection("health_records") }
    val emergencyAlertsCollection by lazy { firestore.collection("emergency_alerts") }
    val remindersCollection by lazy { firestore.collection("reminders") }
    val socialFeaturesCollection by lazy { firestore.collection("social_features") }
    val benefitsCollection by lazy { firestore.collection("benefits") }
    val activitiesCollection by lazy { firestore.collection("activities") }
    val notificationsCollection by lazy { firestore.collection("notifications") }
    
    /**
     * Initialize Firebase with custom settings
     */
    fun initialize() {
        try {
            // Enable offline persistence for Firestore
            firestore.enableNetwork()
            
            // Set database persistence
            database.setPersistenceEnabled(true)
            
            // Configure database rules for better performance
            database.setLoggingEnabled() // Disable in production
            
        } catch (e: Exception) {
            // Handle initialization error
            e.printStackTrace()
        }
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get user reference by ID
     */
    fun getUserRef(userId: String) = usersRef.child(userId)
    
    /**
     * Get health records reference for user
     */
    fun getHealthRecordsRef(userId: String) = healthRecordsRef.child(userId)
    
    /**
     * Get emergency alerts reference for user
     */
    fun getEmergencyAlertsRef(userId: String) = emergencyAlertsRef.child(userId)
    
    /**
     * Get reminders reference for user
     */
    fun getRemindersRef(userId: String) = remindersRef.child(userId)
    
    /**
     * Get social features reference for user
     */
    fun getSocialFeaturesRef(userId: String) = socialFeaturesRef.child(userId)
    
    /**
     * Get benefits reference for user
     */
    fun getBenefitsRef(userId: String) = benefitsRef.child(userId)
    
    /**
     * Get activities reference for user
     */
    fun getActivitiesRef(userId: String) = activitiesRef.child(userId)
    
    /**
     * Get notifications reference for user
     */
    fun getNotificationsRef(userId: String) = notificationsRef.child(userId)
}

private fun FirebaseDatabase.setLoggingEnabled() {}

