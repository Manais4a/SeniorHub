package com.seniorhub.activities

import android.os.Bundle
import android.util.Log
// Removed MenuItem import since we're using custom back button
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.seniorhub.R
import com.seniorhub.adapters.SocialServicesAdapter
import com.seniorhub.models.SocialService

/**
 * SocialActivity - Davao City District 1 Social Services Directory
 * 
 * Features:
 * - Firebase Realtime Database integration
 * - Real-time updates of social services
 * - Call, Map, and Email functionality
 * - Accessibility support for senior citizens
 * - Service filtering and search capabilities
 */
class SocialActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SocialServicesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmptyState: View
    private lateinit var tvServiceCount: TextView
    private lateinit var fabRefresh: FloatingActionButton
    
    private val socialServicesList = mutableListOf<SocialService>()
    private val database by lazy { FirebaseDatabase.getInstance() }
    private val socialServicesRef by lazy { database.getReference("social_services") }
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

        try {
            setupToolbar()
            initializeViews()
            setupRecyclerView()
            setupRefreshButton()
            fetchSocialServicesFromFirebase()
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing social services: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar() {
        try {
            val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
            // Check if we can set the toolbar as action bar
            if (supportActionBar == null) {
                setSupportActionBar(toolbar)
                // Remove automatic back button - we'll use custom back button
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.title = "Social Services"
                supportActionBar?.subtitle = "District 1, Davao City"
            } else {
                // Action bar already exists, just configure it
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.title = "Social Services"
                supportActionBar?.subtitle = "District 1, Davao City"
            }
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error setting up toolbar: ${e.message}")
            // Continue without toolbar setup if there's an issue
        }
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerViewSocialServices)
            progressBar = findViewById(R.id.progressBar)
            layoutEmptyState = findViewById(R.id.layoutEmptyState)
            tvServiceCount = findViewById(R.id.tvServiceCount)
            fabRefresh = findViewById(R.id.fabRefresh)
            
            // Setup custom back button
            setupBackButton()
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error loading UI elements: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupBackButton() {
        try {
            val btnBack = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)
            btnBack?.setOnClickListener {
                finish() // Close the activity and return to previous screen
            }
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error setting up back button: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = SocialServicesAdapter(socialServicesList) { service ->
                // Handle service item click
                showServiceDetails(service)
            }
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Error setting up service list: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRefreshButton() {
        try {
            fabRefresh.setOnClickListener {
                try {
                    fetchSocialServicesFromFirebase()
                } catch (e: Exception) {
                    Log.e("SocialActivity", "Error in refresh button: ${e.message}", e)
                    Toast.makeText(this, "Error refreshing services. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Add long press for debug info
            fabRefresh.setOnLongClickListener {
                try {
                    showDebugInfo()
                    true
                } catch (e: Exception) {
                    Log.e("SocialActivity", "Error in debug info: ${e.message}", e)
                    Toast.makeText(this, "Error showing debug info. Please try again.", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error setting up refresh button: ${e.message}", e)
            Toast.makeText(this, "Error setting up refresh button. Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private fun showDebugInfo() {
        val debugInfo = buildString {
            append("Firebase Debug Info:\n")
            append("Database URL: ${database.app.options.databaseUrl}\n")
            append("Reference: social_services\n")
            append("Current Services: ${socialServicesList.size}\n")
            append("Services List: ${socialServicesList.map { it.name }}\n")
            append("\nTesting Firebase connection...\n")
        }
        
        // Test Firebase connection immediately
        socialServicesRef.get().addOnCompleteListener { task ->
            val result = if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot.exists()) {
                    "✅ Data exists (${snapshot.childrenCount} items)\nFirst item: ${snapshot.children.firstOrNull()?.key}"
                } else {
                    "❌ No data found at path"
                }
            } else {
                "❌ Connection failed: ${task.exception?.message}"   
            }
            
            // Show updated debug info
            val updatedDebugInfo = debugInfo + result
            try {
                if (!isFinishing && !isDestroyed) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Debug Information")
                        .setMessage(updatedDebugInfo)
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Test Connection") { _, _ ->
                            testFirebaseConnection()
                        }
                        .show()
                }
            } catch (e: Exception) {
                Log.e("SocialActivity", "Error showing debug dialog: ${e.message}")
            }
        }
    }

    private fun fetchSocialServicesFromFirebase() {
        showLoading(true)
        
        try {
            Log.d("SocialActivity", "Fetching social services from Firebase...")
            Log.d("SocialActivity", "Database URL: ${database.app.options.databaseUrl}")
            Log.d("SocialActivity", "Reference: social_services")
            
            // Remove existing listener to avoid duplicates
            valueEventListener?.let { listener ->
                socialServicesRef.removeEventListener(listener)
            }
            
            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("SocialActivity", "onDataChange called")
                    socialServicesList.clear()
                    
                    try {
                        if (snapshot.exists()) {
                            Log.d("SocialActivity", "Snapshot exists with ${snapshot.childrenCount} children")
                            
                            for (serviceSnapshot in snapshot.children) {
                                try {
                                    Log.d("SocialActivity", "Processing child: ${serviceSnapshot.key}")
                                    
                                    val service = serviceSnapshot.getValue(SocialService::class.java)
                                    if (service != null) {
                                        val serviceWithId = service.copy(id = serviceSnapshot.key ?: "")
                                        Log.d("SocialActivity", "Parsed service: ${serviceWithId.name}")
                                        
                                        // Only add active services
                                        if (serviceWithId.isActive) {
                                            socialServicesList.add(serviceWithId)
                                            Log.d("SocialActivity", "Added active service: ${serviceWithId.name}")
                                        } else {
                                            Log.d("SocialActivity", "Skipped inactive service: ${serviceWithId.name}")
                                        }
                                    } else {
                                        Log.w("SocialActivity", "Failed to parse service from snapshot: ${serviceSnapshot.key}")
                                        
                                        // Try manual parsing as fallback
                                        val rawData = serviceSnapshot.value as? Map<String, Any>
                                        if (rawData != null) {
                                            val isActive = rawData["isActive"]?.toString()?.toBoolean() ?: true
                                            if (isActive) {
                                                val manualService = SocialService(
                                                    id = serviceSnapshot.key ?: "",
                                                    name = rawData["name"]?.toString() ?: "Unknown Service",
                                                    address = rawData["address"]?.toString() ?: "",
                                                    contact = rawData["contact"]?.toString() ?: "",
                                                    phoneNumber = rawData["phoneNumber"]?.toString() ?: "",
                                                    email = rawData["email"]?.toString() ?: "",
                                                    servicesOffered = rawData["servicesOffered"]?.toString() ?: "",
                                                    serviceType = rawData["serviceType"]?.toString() ?: "GOVERNMENT",
                                                    officeHours = rawData["officeHours"]?.toString() ?: "",
                                                    notes = rawData["notes"]?.toString() ?: "",
                                                    website = rawData["website"]?.toString() ?: "",
                                                    isActive = isActive,
                                                    priority = (rawData["priority"]?.toString()?.toIntOrNull()) ?: 0
                                                )
                                                
                                                socialServicesList.add(manualService)
                                                Log.d("SocialActivity", "Manually added active service: ${manualService.name}")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("SocialActivity", "Error parsing service: ${e.message}", e)
                                }
                            }
                            
                            // Sort by priority (higher priority first)
                            socialServicesList.sortByDescending { it.priority }
                            
                            Log.d("SocialActivity", "Successfully loaded ${socialServicesList.size} active social services")
                            
                            if (socialServicesList.isNotEmpty()) {
                                Log.d("SocialActivity", "Active services loaded: ${socialServicesList.map { it.name }}")
                            }
                        } else {
                            Log.w("SocialActivity", "No data found at social_services path")
                        }
                    } catch (e: Exception) {
                        Log.e("SocialActivity", "Error processing data: ${e.message}", e)
                    }
                    
                    updateUI()
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("SocialActivity", "Failed to read value: ${error.message}", error.toException())
                    showError("Failed to load social services: ${error.message}")
                    showLoading(false)
                }
            }
            
            // Add the listener
            valueEventListener?.let { listener ->
                socialServicesRef.addValueEventListener(listener)
            }
            
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error accessing Firebase: ${e.message}", e)
            showError("Error connecting to database: ${e.message}")
            showLoading(false)
        }
    }

    private fun updateUI() {
        Log.d("SocialActivity", "updateUI called with ${socialServicesList.size} services")
        // Show only the top 8 services by current sorting (priority desc)
        val displayedServices = socialServicesList.take(8)
        adapter.updateServices(displayedServices)
        updateServiceCount(displayedServices.size)
        
        if (socialServicesList.isEmpty()) {
            Log.w("SocialActivity", "No services to display - showing empty state")
            showEmptyState(true)
        } else {
            Log.d("SocialActivity", "Services available - hiding empty state")
            showEmptyState(false)
        }
    }

    private fun updateServiceCount(countOverride: Int? = null) {
        val count = countOverride ?: socialServicesList.size
        tvServiceCount.text = "$count Service${if (count != 1) "s" else ""}"
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        layoutEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    
    /**
     * Test Firebase connection and data structure
     */
    private fun testFirebaseConnection() {
        Log.d("SocialActivity", "Testing Firebase connection...")
        Log.d("SocialActivity", "Database URL: ${database.app.options.databaseUrl}")
        Log.d("SocialActivity", "Reference: social_services")
        
        socialServicesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                Log.d("SocialActivity", "Firebase connection successful")
                Log.d("SocialActivity", "Data exists: ${snapshot.exists()}")
                if (snapshot.exists()) {
                    Log.d("SocialActivity", "Children count: ${snapshot.childrenCount}")
                    for (child in snapshot.children) {
                        Log.d("SocialActivity", "Child key: ${child.key}")
                        Log.d("SocialActivity", "Child value: ${child.value}")
                        
                        // Check if the child has the expected structure
                        val service = child.getValue(SocialService::class.java)
                        if (service != null) {
                            Log.d("SocialActivity", "Parsed service: ${service.name}, isActive: ${service.isActive}")
                        } else {
                            Log.w("SocialActivity", "Failed to parse service from child: ${child.key}")
                            Log.w("SocialActivity", "Child data type: ${child.value?.javaClass?.simpleName}")
                            Log.w("SocialActivity", "Child data: ${child.value}")
                            
                            // Try to parse as Map to see the actual structure
                            try {
                                val rawData = child.value as? Map<String, Any>
                                if (rawData != null) {
                                    Log.w("SocialActivity", "Raw data structure:")
                                    rawData.forEach { (key, value) ->
                                        Log.w("SocialActivity", "  $key: $value (${value?.javaClass?.simpleName})")
                                    }
                                    
                                    // Try to manually create a SocialService from raw data
                                    try {
                                        val manualService = SocialService(
                                            id = child.key ?: "",
                                            name = rawData["name"]?.toString() ?: "",
                                            address = rawData["address"]?.toString() ?: "",
                                            contact = rawData["contact"]?.toString() ?: "",
                                            phoneNumber = rawData["phoneNumber"]?.toString() ?: "",
                                            email = rawData["email"]?.toString() ?: "",
                                            servicesOffered = rawData["servicesOffered"]?.toString() ?: "",
                                            serviceType = rawData["serviceType"]?.toString() ?: "GOVERNMENT",
                                            officeHours = rawData["officeHours"]?.toString() ?: "",
                                            notes = rawData["notes"]?.toString() ?: "",
                                            website = rawData["website"]?.toString() ?: "",
                                            isActive = rawData["isActive"]?.toString()?.toBoolean() ?: true,
                                            priority = (rawData["priority"]?.toString()?.toIntOrNull()) ?: 0
                                        )
                                        
                                        if (manualService.isActive) {
                                            socialServicesList.add(manualService)
                                            Log.d("SocialActivity", "Manually added service: ${manualService.name}")
                                            Log.d("SocialActivity", "Service details: name=${manualService.name}, isActive=${manualService.isActive}, priority=${manualService.priority}")
                                        } else {
                                            Log.w("SocialActivity", "Manual service is inactive: ${manualService.name}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SocialActivity", "Error creating manual service: ${e.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SocialActivity", "Error analyzing raw data: ${e.message}")
                            }
                        }
                    }
                } else {
                    Log.w("SocialActivity", "No data found at social_services path")
                }
            } else {
                Log.e("SocialActivity", "Firebase connection failed: ${task.exception?.message}")
                task.exception?.printStackTrace()
            }
        }
    }

    private fun showServiceDetails(service: SocialService) {
        // Check if activity is still running
        if (isFinishing || isDestroyed) {
            return
        }
        
        val details = buildString {
            append("Service: ${service.name}\n\n")
            append("Address: ${service.address}\n\n")
            append("Contact: ${service.contact}\n\n")
            if (service.officeHours.isNotBlank()) {
                append("Office Hours: ${service.officeHours}\n\n")
            }
            if (service.servicesOffered.isNotBlank()) {
                append("Services Offered:\n${service.servicesOffered}\n\n")
            }
            if (service.notes.isNotBlank()) {
                append("Notes: ${service.notes}")
            }
        }

        try {
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
                        Toast.makeText(this, R.string.no_phone_number_available, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Map") { _, _ ->
                    if (service.hasAddress()) {
                        val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(service.address))
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                    } else {
                        Toast.makeText(this, R.string.no_address_available, Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("SocialActivity", "Error showing service details dialog: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { listener ->
            socialServicesRef.removeEventListener(listener)
        }
    }

    // Removed onOptionsItemSelected since we're using custom back button
}