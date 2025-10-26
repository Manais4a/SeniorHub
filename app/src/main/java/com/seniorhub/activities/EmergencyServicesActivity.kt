package com.seniorhub.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.seniorhub.R
import com.seniorhub.firebase.FirebaseConfig
import com.seniorhub.models.EmergencyService
import com.google.firebase.Timestamp
import com.seniorhub.services.EmergencyService as EmergencyServiceClass
import com.seniorhub.services.LocationService
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.Result
import android.location.Location
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

/**
 * EmergencyServicesActivity - Emergency contacts and services
 *
 * Provides emergency features for senior citizens including:
 * - Quick dial to 911
 * - Emergency contact management
 * - Medical information access
 * - Location sharing for emergencies
 */
class EmergencyServicesActivity : AppCompatActivity() {

    // Services
    private lateinit var emergencyService: EmergencyServiceClass
    private lateinit var locationService: LocationService

    // Permission request launcher for phone calls and SMS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val phoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (phoneGranted) {
            // Phone permission granted, can make emergency calls
            makeEmergencyCall(currentPhoneNumber)
        } else {
            // Permission denied, show explanation
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_services)

        // Initialize services
        emergencyService = EmergencyServiceClass(this)
        locationService = LocationService(this)

        setupActionBar()
        setupUI()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Emergency Services"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupUI() {
        // Set up back button functionality
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // Set up District 1 emergency contact buttons
        setupDistrict1Contacts()

        // Emergency contacts are now displayed as static cards in the layout
        // No RecyclerView setup needed
    }


    private fun setupDistrict1Contacts() {
        // Davao Doctors Hospital
        // Central 911
        try {
            findViewById<Button>(R.id.btnCall911Central)?.setOnClickListener {
                confirmEmergencyCall("Central 911 Davao")
            }
        } catch (_: Exception) {
            // View not found, continue
        }


        try {
            findViewById<Button>(R.id.btnCallDavaoDoctors)?.setOnClickListener {
                makeDirectCall("(082) 222-8000", "Davao Doctors Hospital")
            }
        } catch (_: Exception) {
            // View not found, continue
        }

        // SPMC
        try {
            findViewById<Button>(R.id.btnCallSPMC)?.setOnClickListener {
                makeDirectCall("(082) 227-2731", "Southern Philippines Medical Center")
            }
        } catch (_: Exception) {
            // View not found, continue
        }

        // Davao City Police Office
        try {
            findViewById<Button>(R.id.btnCallDCPO)?.setOnClickListener {
                makeDirectCall("(082) 227-1180", "Davao City Police Office")
            }
        } catch (_: Exception) {
            // View not found, continue
        }

        // Fire Station
        try {
            findViewById<Button>(R.id.btnCallFireStation)?.setOnClickListener {
                makeDirectCall("(082) 224-3575", "Davao City Central Fire Station")
            }
        } catch (_: Exception) {
            // View not found, continue
        }


        // Senior Hotline
        try {
            findViewById<Button>(R.id.btnCallSeniorHotline)?.setOnClickListener {
                makeDirectCall("09705416533", "Senior Hotline")
            }
        } catch (_: Exception) {
            // View not found, continue
        }

        // New: Davao City Emergency Response Unit
        try {
            findViewById<Button>(R.id.btnCallEmergencyResponseUnit)?.setOnClickListener {
                makeDirectCall("(082) 227-2731", "Davao City Emergency Response Unit")
            }
        } catch (_: Exception) {
            // View not found, continue
        }

        // New: Davao City Ambulance Service
        try {
            findViewById<Button>(R.id.btnCallAmbulanceService)?.setOnClickListener {
                makeDirectCall("(082) 227-2731", "Davao City Ambulance Service")
            }
        } catch (_: Exception) {
            // View not found, continue
        }
    }

    // Firebase-based dynamic loading removed; using static cards in layout
    
    private fun showEmergencyServiceDetails(service: com.seniorhub.models.EmergencyService) {
        val details = buildString {
            append("Service: ${service.name}\n\n")
            append("Description: ${service.description}\n\n")
            append("Phone: ${service.getFormattedPhoneNumber()}\n\n")
            append("Address: ${service.address}\n\n")
            if (service.officeHours.isNotBlank()) {
                append("Office Hours: ${service.officeHours}\n\n")
            }
            if (service.website.isNotBlank()) {
                append("Website: ${service.website}\n\n")
            }
            if (service.notes.isNotBlank()) {
                append("Notes: ${service.notes}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Service Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Call") { _, _ ->
                if (service.hasPhoneNumber()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:${service.getFormattedPhoneNumber()}")
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Map") { _, _ ->
                if (service.hasAddress()) {
                    val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(service.address))
                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "No address available", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun confirmEmergencyCall(serviceName: String = "911") {
        val phoneNumber = when (serviceName) {
            "911" -> "911"
            "Central 911 Davao" -> "911"
            "Senior Hotline" -> "09705416533" // Senior hotline number
            else -> "911"
        }

        AlertDialog.Builder(this)
            .setTitle("Emergency Call")
            .setMessage("Are you sure you want to call $serviceName for emergency services?")
            .setPositiveButton("Yes, Call $serviceName") { _, _ ->
                checkPermissionAndCall(phoneNumber)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private fun makeDirectCall(phoneNumber: String, serviceName: String) {
        AlertDialog.Builder(this)
            .setTitle("Call $serviceName")
            .setMessage("This will:\n• Call $serviceName\n• Send SMS to your Emergency Contact with location\n• Log the emergency alert")
            .setPositiveButton("Yes, Call") { _, _ ->
                checkPermissionAndCall(phoneNumber, serviceName)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private var currentPhoneNumber = "911"
    private var currentServiceName = "Emergency Service"

    private fun checkPermissionAndCall(phoneNumber: String = "911", serviceName: String = "Emergency Service") {
        currentPhoneNumber = phoneNumber
        currentServiceName = serviceName
        
        // Check required permissions
        val requiredPermissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CALL_PHONE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.SEND_SMS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (requiredPermissions.isNotEmpty()) {
            // Request all required permissions
            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            // All permissions granted
            makeEmergencyCall(phoneNumber, serviceName)
        }
    }

    private fun makeEmergencyCall(phoneNumber: String = "911", serviceName: String = "Emergency Service") {
        try {
            // Clean phone number (remove spaces, parentheses, etc.)
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

            // Get current user name for emergency alert
            val currentUser = FirebaseManager.getCurrentUser()
            val seniorName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Senior User"
            
            // Launch coroutine to handle emergency alert
            lifecycleScope.launch {
                try {
                    // Get current location
                    val location = locationService.getCurrentLocation()
                    
                    // Send emergency service alert with location
                    when (val result = emergencyService.sendEmergencyServiceAlert(
                        serviceName = serviceName,
                        servicePhone = cleanNumber,
                        seniorName = seniorName,
                        location = location
                    )) {
                        is Result.Success -> {
                            // Make emergency call
                            val emergencyIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$cleanNumber")
                            }
                            startActivity(emergencyIntent)
                            
                            // Show success message
                            Toast.makeText(this@EmergencyServicesActivity, "Calling $serviceName... Emergency alert sent with location.", Toast.LENGTH_LONG).show()
                        }
                        is Result.Error -> {
                            // Still make the call even if SMS fails
                            val emergencyIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$cleanNumber")
                            }
                            startActivity(emergencyIntent)
                            
                            Toast.makeText(this@EmergencyServicesActivity, "Calling $serviceName... SMS alert failed.", Toast.LENGTH_LONG).show()
                        }
                        is Result.Loading<*> -> {
                            // Handle loading state if needed
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EmergencyServicesActivity", "Error in emergency call: ${e.message}", e)
                    // Fallback to basic emergency call
                    val emergencyIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$cleanNumber")
                    }
                    startActivity(emergencyIntent)
                    Toast.makeText(this@EmergencyServicesActivity, "Calling $serviceName... Please contact your emergency contact manually.", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            // Handle error
            Toast.makeText(
                this,
                "Unable to make call. Please dial $phoneNumber manually.",
                Toast.LENGTH_LONG
            ).show()
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

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Phone permission is required for emergency calls. Please enable it in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}