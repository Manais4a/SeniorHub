package com.seniorhub.services

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.seniorhub.utils.Result
import com.seniorhub.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * SMSService - Handles sending emergency SMS alerts
 * 
 * Key Features:
 * 1. Phone Number Based: Emergency Contact Number from user profile
 * 2. SMS Delivery: SMS fallback for non-app users
 * 3. No Registration Required: Emergency Contact don't need to install app
 * 4. Immediate Alerts: Works even if contact doesn't have the app
 * 5. Location Sharing: Google Maps links in SMS
 */
class SMSService(private val context: Context) {

    private val smsManager = SmsManager.getDefault()
    private val userRepository = UserRepository.getInstance()

    /**
     * Send emergency SMS alert to the senior's emergency contact
     * 
     * @param emergencyType The type of emergency (SOS Button, Hospital, etc.)
     * @param seniorName The name of the senior user
     * @param location Current location of the senior
     * @return Result indicating success or failure
     */
    suspend fun sendEmergencySMS(
        emergencyType: String,
        seniorName: String,
        location: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("SMSService", "Starting emergency SMS for: $seniorName")
            
            // Get emergency contact phone number from user profile
            val emergencyContactPhone = getEmergencyContactPhone()
            if (emergencyContactPhone.isNullOrEmpty()) {
                Log.e("SMSService", "No emergency contact phone number found")
                return@withContext Result.Error(Exception("No emergency contact phone number found"))
            }

            // Create the emergency message
            val message = createEmergencyMessage(emergencyType, seniorName, location)
            Log.d("SMSService", "Emergency message created: $message")

            // Send the SMS
            sendSMS(emergencyContactPhone, message)
            
            Log.d("SMSService", "Emergency SMS sent successfully to: $emergencyContactPhone")
            Result.Success(true)
            
        } catch (e: Exception) {
            Log.e("SMSService", "Error sending emergency SMS: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get emergency contact phone number from user profile
     */
    private suspend fun getEmergencyContactPhone(): String? {
        return try {
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> {
                    val user = result.data?.user
                    // Get primary emergency contact phone number
                    user?.getPrimaryEmergencyContact()?.phoneNumber
                }
                is Result.Error -> {
                    Log.e("SMSService", "Error getting user data: ${result.exception.message}")
                    null
                }
                is Result.Loading -> {
                    Log.d("SMSService", "Loading user data...")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SMSService", "Error getting emergency contact phone: ${e.message}", e)
            null
        }
    }

    /**
     * Create formatted emergency message with Google Maps link
     */
    private fun createEmergencyMessage(
        emergencyType: String,
        seniorName: String,
        location: String?
    ): String {
        val timestamp = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date())
        val locationText = location ?: "Current Location of the Senior User"
        
        // Create Google Maps link if location is available
        val googleMapsLink = if (location != null && location.contains("Lat:")) {
            // Extract coordinates from location string
            val latMatch = Regex("Lat: ([0-9.-]+)").find(location)
            val lngMatch = Regex("Lng: ([0-9.-]+)").find(location)
            
            if (latMatch != null && lngMatch != null) {
                val lat = latMatch.groupValues[1]
                val lng = lngMatch.groupValues[1]
                "https://maps.google.com/?q=$lat,$lng"
            } else {
                ""
            }
        } else {
            ""
        }
        
        return buildString {
            appendLine("🚨 SOS ALERT 🚨")
            appendLine("Emergency Alert: $seniorName may need immediate help. Please Try To Reach her/him.")
            appendLine("")
            appendLine("📍 Location: $locationText")
            appendLine("🩺 Emergency Type: $emergencyType")
            appendLine("⏰ Timestamp: $timestamp")
            if (googleMapsLink.isNotEmpty()) {
                appendLine("")
                appendLine("🗺️ Click this Google Maps link for exact location:")
                appendLine(googleMapsLink)
            }
        }
    }

    /**
     * Send SMS message
     */
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            // Clean phone number (remove spaces, dashes, etc.)
            val cleanPhoneNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            
            Log.d("SMSService", "Sending SMS to: $cleanPhoneNumber")
            Log.d("SMSService", "Message: $message")
            
            // Send SMS using SmsManager
            smsManager.sendTextMessage(cleanPhoneNumber, null, message, null, null)
            
        } catch (e: Exception) {
            Log.e("SMSService", "Error sending SMS: ${e.message}", e)
            throw e
        }
    }

    /**
     * Send SMS to multiple emergency contacts (if needed in the future)
     */
    suspend fun sendEmergencySMSToMultipleContacts(
        emergencyType: String,
        seniorName: String,
        location: String? = null,
        phoneNumbers: List<String>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = createEmergencyMessage(emergencyType, seniorName, location)
            var successCount = 0
            var errorCount = 0

            for (phoneNumber in phoneNumbers) {
                try {
                    sendSMS(phoneNumber, message)
                    successCount++
                    Log.d("SMSService", "SMS sent successfully to: $phoneNumber")
                } catch (e: Exception) {
                    errorCount++
                    Log.e("SMSService", "Failed to send SMS to $phoneNumber: ${e.message}")
                }
            }

            if (successCount > 0) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Failed to send SMS to any emergency contacts"))
            }
            
        } catch (e: Exception) {
            Log.e("SMSService", "Error sending emergency SMS to multiple contacts: ${e.message}", e)
            Result.Error(e)
        }
    }
}
