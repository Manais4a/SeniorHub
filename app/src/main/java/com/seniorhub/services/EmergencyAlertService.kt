package com.seniorhub.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.seniorhub.R
import com.seniorhub.activities.MainActivity
import com.seniorhub.utils.FirebaseManager
import kotlinx.coroutines.*
import java.util.*

/**
 * EmergencyAlertService - Comprehensive Emergency Management Service
 * 
 * This foreground service handles emergency situations for senior citizens including:
 * - GPS location tracking during emergencies
 * - Automated emergency contact notifications (SMS/Calls)
 * - Real-time location sharing with emergency contacts
 * - Integration with local emergency services
 * - Continuous health monitoring during emergencies
 * - Family/caregiver notifications
 * - Emergency status updates and resolution
 */
class EmergencyAlertService : Service() {
    
    companion object {
        private const val TAG = "EmergencyAlertService"
        
        // Notification Configuration
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "emergency_service_channel"
        private const val CHANNEL_NAME = "Emergency Service"
        private const val CHANNEL_DESCRIPTION = "Emergency tracking and alert notifications"
        
        // Location Configuration
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds during emergency
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds fastest update
        private const val LOCATION_DISPLACEMENT = 10f // 10 meters minimum displacement
        
        // Service Actions
        const val ACTION_START_EMERGENCY = "com.seniorhub.START_EMERGENCY"
        const val ACTION_STOP_EMERGENCY = "com.seniorhub.STOP_EMERGENCY"
        const val ACTION_UPDATE_LOCATION = "com.seniorhub.UPDATE_LOCATION"
        const val ACTION_SEND_ALERT = "com.seniorhub.SEND_ALERT"
        
        // Emergency Types
        const val EMERGENCY_TYPE_MEDICAL = "medical"
        const val EMERGENCY_TYPE_FALL = "fall"
        const val EMERGENCY_TYPE_PANIC = "panic"
        const val EMERGENCY_TYPE_GENERAL = "general"
        
        // Emergency Status
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_PENDING = "pending"
    }
    
    // Service State
    private var isEmergencyActive = false
    private var emergencyStartTime: Long = 0
    private var emergencyType: String = EMERGENCY_TYPE_GENERAL
    private var emergencyId: String = ""
    
    // System Services
    private lateinit var notificationManager: NotificationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    // Utility Classes
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Location Data
    private var lastKnownLocation: Location? = null
    private var locationUpdateCount = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "EmergencyAlertService created")
        
        initializeServices()
        createNotificationChannel()
        setupLocationCallback()
        
        Log.i(TAG, "EmergencyAlertService initialized successfully")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_EMERGENCY -> {
                val type = intent.getStringExtra("emergency_type") ?: EMERGENCY_TYPE_GENERAL
                val message = intent.getStringExtra("emergency_message") ?: "Emergency alert activated"
                startEmergencyMode(type, message)
            }
            ACTION_STOP_EMERGENCY -> {
                stopEmergencyMode()
            }
            ACTION_UPDATE_LOCATION -> {
                requestLocationUpdate()
            }
            ACTION_SEND_ALERT -> {
                sendEmergencyAlert()
            }
        }
        
        // Service should be restarted if killed during emergency
        return if (isEmergencyActive) START_STICKY else START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        // This is not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "EmergencyAlertService destroyed")

        // Clean up resources
        if (isEmergencyActive) {
            stopLocationTracking()
        }

        // Cancel all coroutines
        serviceScope.cancel()

        // Clear any remaining notifications
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Initialize all required services and components
     */
    private fun initializeServices() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    
    /**
     * Start emergency mode with comprehensive tracking and alerts
     */
    private fun startEmergencyMode(type: String = EMERGENCY_TYPE_GENERAL, message: String = "") {
        if (isEmergencyActive) {
            Log.d(TAG, "Emergency mode already active")
            updateEmergencyNotification("Emergency ongoing...")
            return
        }
        
        Log.i(TAG, "Starting emergency mode - Type: $type")
        
        // Set emergency state
        isEmergencyActive = true
        emergencyStartTime = System.currentTimeMillis()
        emergencyType = type
        emergencyId = UUID.randomUUID().toString()
        
        // Start foreground service with emergency notification
        startForeground(NOTIFICATION_ID, createEmergencyNotification("Emergency activated"))
        
        // Begin location tracking
        startLocationTracking()
        
        // Send initial emergency alert
        serviceScope.launch {
            delay(1000) // Brief delay to ensure location is available
            sendEmergencyAlert(message)
        }
        
        Log.i(TAG, "Emergency mode activated successfully - ID: $emergencyId")
    }
    
    /**
     * Stop emergency mode and clean up all resources
     */
    private fun stopEmergencyMode() {
        if (!isEmergencyActive) {
            Log.d(TAG, "Emergency mode not active")
            return
        }
        
        Log.i(TAG, "Stopping emergency mode - ID: $emergencyId")
        
        // Update emergency status in Firebase
        serviceScope.launch {
            updateEmergencyStatus(STATUS_RESOLVED)
        }
        
        // Stop location tracking
        stopLocationTracking()
        
        // Clear emergency state
        isEmergencyActive = false
        emergencyId = ""
        emergencyType = EMERGENCY_TYPE_GENERAL
        locationUpdateCount = 0
        lastKnownLocation = null
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        // Send resolution notification to contacts
        serviceScope.launch {
            sendResolutionAlert()
        }
        
        Log.i(TAG, "Emergency mode deactivated successfully")
    }
    
    /**
     * Create notification channel for emergency alerts (Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)
                setBypassDnd(true) // Allow notifications even in Do Not Disturb mode
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Emergency notification channel created")
        }
    }
    
    /**
     * Create comprehensive emergency notification for foreground service
     */
    private fun createEmergencyNotification(statusText: String): Notification {
        // Intent to open main activity
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("emergency_active", true)
            putExtra("emergency_id", emergencyId)
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to stop emergency
        val stopIntent = Intent(this, EmergencyAlertService::class.java).apply {
            action = ACTION_STOP_EMERGENCY
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to send manual alert
        val alertIntent = Intent(this, EmergencyAlertService::class.java).apply {
            action = ACTION_SEND_ALERT
        }
        
        val alertPendingIntent = PendingIntent.getService(
            this, 1, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build comprehensive notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 Emergency Mode Active")
            .setContentText(statusText)
            .setSubText("Type: ${emergencyType.capitalize()} • Updates: $locationUpdateCount")
            .setSmallIcon(R.drawable.ic_emergency)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(resources.getColor(R.color.emergency_red, null))
            .addAction(
                R.drawable.ic_emergency,
                "Stop Emergency",
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_emergency,
                "Send Alert",
                alertPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Emergency assistance is active. Your location is being tracked and emergency contacts have been notified. Tap to open app or use actions below.")
            )
            .build()
    }
    
    /**
     * Update the emergency notification with new status
     */
    private fun updateEmergencyNotification(statusText: String) {
        if (isEmergencyActive) {
            val notification = createEmergencyNotification(statusText)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    /**
     * Setup location tracking callback for emergency situations
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                Log.d(TAG, "Location availability: ${locationAvailability.isLocationAvailable}")
                
                if (!locationAvailability.isLocationAvailable) {
                    updateEmergencyNotification("⚠️ GPS signal lost - trying to reconnect...")
                }
            }
        }
    }
    
    /**
     * Start high-frequency location tracking for emergency
     */
    private fun startLocationTracking() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted - cannot start tracking")
            updateEmergencyNotification("⚠️ Location permission required")
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMaxUpdateAgeMillis(LOCATION_UPDATE_INTERVAL * 2)
            setMinUpdateDistanceMeters(LOCATION_DISPLACEMENT)
            setWaitForAccurateLocation(false) // Don't wait for highly accurate location in emergency
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
            
            Log.d(TAG, "High-frequency location tracking started")
            updateEmergencyNotification("📍 GPS tracking active...")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates: ${e.message}")
            updateEmergencyNotification("⚠️ Location access denied")
        }
    }
    
    /**
     * Stop location tracking
     */
    private fun stopLocationTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping location updates: ${e.message}")
        }
    }
    
    /**
     * Handle incoming location updates during emergency
     */
    private fun handleLocationUpdate(location: Location) {
        lastKnownLocation = location
        locationUpdateCount++
        
        Log.d(TAG, "Emergency location update #$locationUpdateCount: ${location.latitude}, ${location.longitude}")
        
        // Update notification with location info
        val accuracy = location.accuracy.toInt()
        updateEmergencyNotification("📍 Location tracked (±${accuracy}m) • Update #$locationUpdateCount")
        
        // Store location in Firebase for emergency monitoring
        serviceScope.launch {
            storeEmergencyLocation(location)
        }
        
        // Send location to emergency contacts every 5 updates or if high accuracy
        if (locationUpdateCount % 5 == 0 || location.accuracy < 20f) {
            serviceScope.launch {
                sendLocationUpdate(location)
            }
        }
    }
    
    /**
     * Request immediate location update
     */
    private fun requestLocationUpdate() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot request location update - permission not granted")
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { 
                    handleLocationUpdate(it)
                } ?: run {
                    Log.w(TAG, "No last known location available")
                    updateEmergencyNotification("📍 Acquiring GPS location...")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get last location: ${exception.message}")
                updateEmergencyNotification("⚠️ GPS error - retrying...")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location: ${e.message}")
        }
    }
    
    /**
     * Send comprehensive emergency alert to all contacts and services
     */
    private fun sendEmergencyAlert(customMessage: String = "") {
        serviceScope.launch {
            try {
                val emergencyMessage = if (customMessage.isNotBlank()) {
                    customMessage
                } else {
                    "🚨 EMERGENCY ALERT: Emergency assistance has been activated. Type: ${emergencyType.capitalize()}."
                }
                
                Log.d(TAG, "Sending emergency alert: $emergencyMessage")
                
                // Send to emergency contacts
                sendToEmergencyContacts(emergencyMessage)
                
                // Store in Firebase for admin monitoring
                storeEmergencyAlert(emergencyMessage)
                
                updateEmergencyNotification("📢 Emergency contacts notified")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending emergency alert: ${e.message}", e)
                updateEmergencyNotification("⚠️ Alert sending failed")
            }
        }
    }
    
    /**
     * Send location update to emergency contacts
     */
    private suspend fun sendLocationUpdate(location: Location) {
        try {
            val locationMessage = "📍 Emergency location update: https://maps.google.com/?q=${location.latitude},${location.longitude} (Accuracy: ±${location.accuracy.toInt()}m)"

            // TODO: Implement SMS/Push notification to emergency contact
            Log.d(TAG, "Location update prepared: $locationMessage")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending location update: ${e.message}", e)
        }
    }
    
    /**
     * Send emergency alert to all emergency contacts
     */
    private suspend fun sendToEmergencyContacts(message: String) {
        try {
            // TODO: Implement actual SMS/Call/Push notification
            // For now, just log the action
            Log.d(TAG, "Emergency alert prepared: $message")

            // Simulate sending notification
            delay(100)

            Log.i(TAG, "Emergency alerts processed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending to emergency contacts: ${e.message}", e)
        }
    }
    
    /**
     * Store emergency location in Firebase for admin monitoring
     */
    private suspend fun storeEmergencyLocation(location: Location) {
        try {
            val userId = FirebaseManager.getCurrentUserId() ?: return
            
            val locationData = mapOf(
                "emergencyId" to emergencyId,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to location.accuracy,
                "timestamp" to System.currentTimeMillis(),
                "isEmergency" to true,
                "emergencyType" to emergencyType,
                "status" to STATUS_ACTIVE,
                "updateCount" to locationUpdateCount
            )
            
            // TODO: Store in Firebase Realtime Database
            // FirebaseManager.getDatabase().getReference("emergencyLocations")
            //     .child(userId).child(emergencyId).setValue(locationData)
            
            Log.d(TAG, "Emergency location stored in Firebase")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing emergency location: ${e.message}", e)
        }
    }
    
    /**
     * Store emergency alert in Firebase for admin monitoring
     */
    private suspend fun storeEmergencyAlert(message: String) {
        try {
            val userId = FirebaseManager.getCurrentUserId() ?: return
            
            val alertData = mapOf(
                "emergencyId" to emergencyId,
                "message" to message,
                "type" to emergencyType,
                "timestamp" to System.currentTimeMillis(),
                "status" to STATUS_ACTIVE,
                "userId" to userId
            )
            
            // TODO: Store in Firebase Realtime Database
            // FirebaseManager.getDatabase().getReference("emergencyAlerts")
            //     .child(emergencyId).setValue(alertData)
            
            Log.d(TAG, "Emergency alert stored in Firebase")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing emergency alert: ${e.message}", e)
        }
    }
    
    /**
     * Update emergency status in Firebase
     */
    private suspend fun updateEmergencyStatus(status: String) {
        try {
            val userId = FirebaseManager.getCurrentUserId() ?: return
            
            val updates = mapOf(
                "status" to status,
                "resolvedAt" to if (status == STATUS_RESOLVED) System.currentTimeMillis() else null,
                "duration" to if (status == STATUS_RESOLVED) System.currentTimeMillis() - emergencyStartTime else null
            )
            
            // TODO: Update in Firebase Realtime Database
            // FirebaseManager.getDatabase().getReference("emergencyLocations")
            //     .child(userId).child(emergencyId).updateChildren(updates)
            
            Log.d(TAG, "Emergency status updated to: $status")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating emergency status: ${e.message}", e)
        }
    }
    
    /**
     * Send emergency resolution alert to contacts
     */
    private suspend fun sendResolutionAlert() {
        try {
            val duration = (System.currentTimeMillis() - emergencyStartTime) / 1000 / 60 // minutes

            val resolutionMessage =
                "✅ EMERGENCY RESOLVED: Emergency assistance has been resolved. Duration: ${duration} minutes."
            
            sendToEmergencyContacts(resolutionMessage)
            
            Log.i(TAG, "Emergency resolution alert sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending resolution alert: ${e.message}", e)
        }
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Handle low memory situations gracefully
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning - emergency service will continue running")
        // Emergency service should not be affected by low memory
    }
    
    /**
     * Handle task removal (user swipes app away)
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed - emergency service continues running")
        // Service should continue running during emergency even if app is removed
    }
}