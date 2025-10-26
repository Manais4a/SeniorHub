package com.seniorhub.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.firebase.FirebaseConfig
import com.seniorhub.models.User
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * EmergencyService - Firebase Cloud Functions + Semaphore SMS Integration
 * 
 * Key Features:
 * 1. Firebase Cloud Functions: Handles SMS sending via Semaphore API
 * 2. Automatic SMS: Triggers SMS when emergency alert is created in Firestore
 * 3. Location Sharing: Includes Google Maps links in SMS messages
 * 4. Emergency Contact Integration: Uses profile emergency contact information
 * 5. Admin Dashboard: Logs all emergency alerts for monitoring
 */
class EmergencyService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "EmergencyService"
        private const val COLLECTION_EMERGENCY_ALERTS = "emergency_alerts"
    }
    
    // Netlify Functions URL - Replace with your actual Netlify site URL
    private val API_BASE_URL = "https://your-site-name.netlify.app" // Replace with your actual Netlify URL
    
    /**
     * Send emergency alert via Netlify Functions API
     */
    suspend fun sendEmergencyAlert(
        emergencyType: String,
        seniorName: String,
        location: Location?,
        emergencyContactPhone: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Get user data for emergency contact
                val userResult = UserRepository.getInstance().getCurrentUser()
                val user = when (userResult) {
                    is Result.Success -> userResult.data?.user
                    else -> null
                }
                
                val contactPhone = emergencyContactPhone ?: user?.getPrimaryEmergencyContact()?.phoneNumber
                val finalSeniorName = seniorName.ifEmpty { user?.getFullName() ?: "Senior User" }
                
                if (contactPhone.isNullOrEmpty()) {
                    Log.w(TAG, "No emergency contact phone number available")
                    return@withContext Result.Error(Exception("No emergency contact phone number available"))
                }
                
                // Create emergency alert data
                val alertData = createEmergencyAlertData(
                    emergencyType = emergencyType,
                    seniorName = finalSeniorName,
                    location = location,
                    emergencyContactPhone = contactPhone,
                    emergencyContactName = user?.getPrimaryEmergencyContact()?.name
                )
                
                // Send SMS via Netlify Functions API
                val apiResult = sendSMSViaAPI(alertData)
                
                if (apiResult.success) {
                    Log.d(TAG, "SMS sent successfully via Netlify Functions API")
                    Result.Success(true)
                } else {
                    Log.e(TAG, "Failed to send SMS via Netlify Functions API: ${apiResult.error}")
                    Result.Error(Exception(apiResult.error))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending emergency alert: ${e.message}", e)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Send emergency alert for Emergency Services List via Netlify Functions API
     */
    suspend fun sendEmergencyServiceAlert(
        serviceName: String,
        servicePhone: String,
        seniorName: String,
        location: Location?
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Get user data for emergency contact
                val userResult = UserRepository.getInstance().getCurrentUser()
                val user = when (userResult) {
                    is Result.Success -> userResult.data?.user
                    else -> null
                }
                
                val finalSeniorName = seniorName.ifEmpty { user?.getFullName() ?: "Senior User" }
                val contactPhone = user?.getPrimaryEmergencyContact()?.phoneNumber
                
                if (contactPhone.isNullOrEmpty()) {
                    Log.w(TAG, "No emergency contact phone number available")
                    return@withContext Result.Error(Exception("No emergency contact phone number available"))
                }
                
                // Create emergency alert data
                val alertData = createEmergencyAlertData(
                    emergencyType = "Emergency Service Call: $serviceName",
                    seniorName = finalSeniorName,
                    location = location,
                    emergencyContactPhone = contactPhone,
                    emergencyContactName = user?.getPrimaryEmergencyContact()?.name,
                    serviceName = serviceName,
                    servicePhone = servicePhone
                )
                
                // Send SMS via Netlify Functions API
                val apiResult = sendSMSViaAPI(alertData)
                
                if (apiResult.success) {
                    Log.d(TAG, "Emergency service SMS sent successfully via Netlify Functions API")
                    Result.Success(true)
                } else {
                    Log.e(TAG, "Failed to send emergency service SMS via Netlify Functions API: ${apiResult.error}")
                    Result.Error(Exception(apiResult.error))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending emergency service alert: ${e.message}", e)
                Result.Error(e)
            }
        }
    }

    /**
     * Send SMS via Netlify Functions API
     */
    private suspend fun sendSMSViaAPI(alertData: Map<String, Any>): ApiResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("$API_BASE_URL/.netlify/functions/send-emergency-sms")
                val connection = url.openConnection() as java.net.HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                // Convert alert data to JSON
                val jsonData = org.json.JSONObject(alertData).toString()
                
                // Send request
                val outputStream = connection.outputStream
                val writer = java.io.OutputStreamWriter(outputStream)
                writer.write(jsonData)
                writer.flush()
                writer.close()
                
                // Read response
                val responseCode = connection.responseCode
                val inputStream = if (responseCode == 200) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                if (responseCode == 200) {
                    val jsonResponse = org.json.JSONObject(response.toString())
                    ApiResult(
                        success = true,
                        messageId = jsonResponse.optString("messageId"),
                        error = null
                    )
                } else {
                    val jsonResponse = org.json.JSONObject(response.toString())
                    ApiResult(
                        success = false,
                        messageId = null,
                        error = jsonResponse.optString("error", "Unknown API error")
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calling SMS API: ${e.message}", e)
                ApiResult(
                    success = false,
                    messageId = null,
                    error = e.message ?: "Network error"
                )
            }
        }
    }
    
    /**
     * Create emergency alert data for API
     */
    private suspend fun createEmergencyAlertData(
        emergencyType: String,
        seniorName: String,
        location: Location?,
        emergencyContactPhone: String,
        emergencyContactName: String? = null,
        serviceName: String? = null,
        servicePhone: String? = null
    ): Map<String, Any> {
        val user = FirebaseConfig.getAuth().currentUser
        val currentTime = Timestamp.now()
        
        // Get senior's information from user profile
        val seniorAddress = getSeniorAddress()
        val seniorPhone = getSeniorPhone()
        val seniorEmail = getSeniorEmail()
        
        // Create formatted SMS message
        val smsMessage = createSMSMessage(emergencyType, seniorName, location, serviceName)
        
        return mapOf(
            // Basic alert information
            "seniorId" to (user?.uid ?: ""),
            "seniorName" to seniorName,
            "emergencyType" to emergencyType,
            "timestamp" to currentTime,
            
            // Emergency contact information
            "emergencyContactPhone" to emergencyContactPhone,
            "emergencyContactName" to (emergencyContactName ?: "Emergency Contact"),
            
            // Location information
            "location" to if (location != null) {
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "address" to seniorAddress
                )
            } else null,
            
            // Service information (for emergency service calls)
            "serviceName" to (serviceName ?: "SOS Emergency"),
            "servicePhone" to (servicePhone ?: "N/A"),
            
            // SMS message content
            "smsMessage" to smsMessage,
            
            // Additional metadata
            "createdAt" to currentTime,
            "status" to "pending",
            "smsSent" to false
        )
    }

    /**
     * Create SMS message in the exact format requested
     */
    private fun createSMSMessage(
        emergencyType: String,
        seniorName: String,
        location: Location?,
        serviceName: String? = null
    ): String {
        val timestamp = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Determine emergency type for display
        val displayEmergencyType = when {
            serviceName != null -> serviceName
            emergencyType.contains("SOS") -> "SOS Button"
            emergencyType.contains("Davao Doctors") -> "Davao Doctors Hospital"
            emergencyType.contains("SPMC") -> "Southern Philippines Medical Center (SPMC)"
            emergencyType.contains("DCPO") -> "Davao City Police Office (DCPO)"
            emergencyType.contains("Fire Station") -> "Davao City Central Fire Station"
            emergencyType.contains("Emergency Response") -> "Central 911 Davao"
            emergencyType.contains("Ambulance") -> "Davao City Ambulance Service"
            else -> emergencyType
        }
        
        // Create location text
        val locationText = if (location != null) {
            "Lat: ${location.latitude}, Lng: ${location.longitude}"
        } else {
            "Current Location of the Senior User"
        }
        
        // Create Google Maps link if location is available
        val googleMapsLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            ""
        }
        
        return buildString {
            appendLine("🚨 SOS ALERT 🚨")
            appendLine("Emergency Alert: $seniorName may need immediate help. Please Try To Reach her/him.")
            appendLine("")
            appendLine("📍 Location: $locationText")
            appendLine("🩺 Emergency Type: $displayEmergencyType")
            appendLine("⏰ Timestamp: $timestamp")
            if (googleMapsLink.isNotEmpty()) {
                appendLine("")
                appendLine("🗺️ Click this Google Maps link for exact location:")
                appendLine(googleMapsLink)
            }
        }
    }


    
    /**
     * Get senior's address from user profile
     */
    private suspend fun getSeniorAddress(): String {
        return try {
            when (val result = UserRepository.getInstance().getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    user?.getFormattedAddress() ?: "Address not available"
                }
                else -> "Address not available"
            }
        } catch (e: Exception) {
            "Address not available"
        }
    }
    
    /**
     * Get senior's phone number
     */
    private suspend fun getSeniorPhone(): String {
        return try {
            when (val result = UserRepository.getInstance().getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    user?.phoneNumber ?: "N/A"
                }
                else -> "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }
    
    /**
     * Get senior's email
     */
    private suspend fun getSeniorEmail(): String {
        return try {
            when (val result = UserRepository.getInstance().getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    user?.email ?: "N/A"
                }
                else -> "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }
    
    /**
     * Get emergency contact name
     */
    private suspend fun getEmergencyContactName(): String {
        return try {
            when (val result = UserRepository.getInstance().getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    user?.getPrimaryEmergencyContact()?.name ?: "Emergency Contact"
                }
                else -> "Emergency Contact"
            }
        } catch (e: Exception) {
            "Emergency Contact"
        }
    }
    
    /**
     * Get emergency contact relationship
     */
    private suspend fun getEmergencyContactRelationship(): String {
        return try {
            when (val result = UserRepository.getInstance().getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    user?.getPrimaryEmergencyContact()?.relationship ?: "Family"
                }
                else -> "Family"
            }
        } catch (e: Exception) {
            "Family"
        }
    }
    
    /**
     * Extract latitude from location string
     */
    private fun extractLatitude(location: String): String {
        return try {
            val latMatch = Regex("Lat: ([0-9.-]+)").find(location)
            latMatch?.groupValues?.get(1) ?: "0.0"
        } catch (e: Exception) {
            "0.0"
        }
    }
    
    /**
     * Extract longitude from location string
     */
    private fun extractLongitude(location: String): String {
        return try {
            val lngMatch = Regex("Lng: ([0-9.-]+)").find(location)
            lngMatch?.groupValues?.get(1) ?: "0.0"
        } catch (e: Exception) {
            "0.0"
        }
    }
}

/**
 * Data class for API result
 */
data class ApiResult(
    val success: Boolean,
    val messageId: String?,
    val error: String?
)
