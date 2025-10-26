package com.seniorhub.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.seniorhub.models.SocialService
import org.json.JSONObject
import java.io.IOException

/**
 * Utility class to upload social services data to Firebase Realtime Database
 * This class reads the firebase_social_services_sample.json file and uploads it to Firebase
 */
object SocialServicesDataUploader {
    
    private const val TAG = "SocialServicesDataUploader"
    
    /**
     * Upload social services data from JSON file to Firebase Realtime Database
     * 
     * @param context Application context
     * @param onComplete Callback when upload is complete
     * @param onError Callback when upload fails
     */
    fun uploadSocialServicesData(
        context: Context,
        onComplete: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d(TAG, "Starting social services data upload...")
            
            // Read JSON file from assets
            val jsonString = readJsonFromAssets(context, "firebase_social_services_sample.json")
            if (jsonString == null) {
                onError("Could not read firebase_social_services_sample.json from assets")
                return
            }
            
            // Parse JSON
            val jsonObject = JSONObject(jsonString)
            val socialServicesObject = jsonObject.getJSONObject("social_services")
            
            // Get Firebase database reference
            val database = FirebaseDatabase.getInstance()
            val socialServicesRef = database.getReference("social_services")
            
            // Clear existing data first
            socialServicesRef.removeValue().addOnCompleteListener { clearTask ->
                if (clearTask.isSuccessful) {
                    Log.d(TAG, "Cleared existing social services data")
                    uploadServices(socialServicesObject, socialServicesRef, onComplete, onError)
                } else {
                    Log.e(TAG, "Failed to clear existing data: ${clearTask.exception?.message}")
                    onError("Failed to clear existing data: ${clearTask.exception?.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading social services data: ${e.message}", e)
            onError("Error uploading data: ${e.message}")
        }
    }
    
    /**
     * Read JSON file from assets folder
     */
    private fun readJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading file from assets: ${e.message}")
            null
        }
    }
    
    /**
     * Upload individual services to Firebase
     */
    private fun uploadServices(
        socialServicesObject: JSONObject,
        socialServicesRef: com.google.firebase.database.DatabaseReference,
        onComplete: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        var uploadCount = 0
        val totalServices = socialServicesObject.length()
        var completedUploads = 0
        
        Log.d(TAG, "Uploading $totalServices social services...")
        
        socialServicesObject.keys().forEach { serviceKey ->
            try {
                val serviceObject = socialServicesObject.getJSONObject(serviceKey)
                
                // Convert JSON to SocialService object
                val socialService = SocialService(
                    id = serviceKey,
                    name = serviceObject.optString("name", ""),
                    address = serviceObject.optString("address", ""),
                    contact = serviceObject.optString("contact", ""),
                    phoneNumber = serviceObject.optString("phoneNumber", ""),
                    email = serviceObject.optString("email", ""),
                    servicesOffered = serviceObject.optString("servicesOffered", ""),
                    serviceType = serviceObject.optString("serviceType", "GOVERNMENT"),
                    officeHours = serviceObject.optString("officeHours", ""),
                    notes = serviceObject.optString("notes", ""),
                    website = serviceObject.optString("website", ""),
                    isActive = serviceObject.optBoolean("isActive", true),
                    priority = serviceObject.optInt("priority", 0)
                )
                
                // Upload to Firebase
                socialServicesRef.child(serviceKey).setValue(socialService)
                    .addOnCompleteListener { task ->
                        completedUploads++
                        
                        if (task.isSuccessful) {
                            uploadCount++
                            Log.d(TAG, "Uploaded service: ${socialService.name}")
                        } else {
                            Log.e(TAG, "Failed to upload service ${socialService.name}: ${task.exception?.message}")
                        }
                        
                        // Check if all uploads are complete
                        if (completedUploads == totalServices) {
                            if (uploadCount > 0) {
                                Log.i(TAG, "Successfully uploaded $uploadCount out of $totalServices services")
                                onComplete(uploadCount)
                            } else {
                                onError("Failed to upload any services")
                            }
                        }
                    }
                
            } catch (e: Exception) {
                completedUploads++
                Log.e(TAG, "Error processing service $serviceKey: ${e.message}")
                
                if (completedUploads == totalServices) {
                    if (uploadCount > 0) {
                        onComplete(uploadCount)
                    } else {
                        onError("Failed to upload any services")
                    }
                }
            }
        }
    }
    
    /**
     * Check if social services data exists in Firebase
     */
    fun checkDataExists(
        onExists: (Int) -> Unit,
        onNotExists: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val database = FirebaseDatabase.getInstance()
            val socialServicesRef = database.getReference("social_services")
            
            socialServicesRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        onExists(snapshot.childrenCount.toInt())
                    } else {
                        onNotExists()
                    }
                } else {
                    onError("Failed to check data: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            onError("Error checking data: ${e.message}")
        }
    }
}

