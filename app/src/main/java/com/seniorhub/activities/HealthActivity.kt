package com.seniorhub.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seniorhub.R
import com.seniorhub.adapters.HealthLogAdapter
import com.seniorhub.models.HealthRecord
import com.seniorhub.repositories.HealthRepository
import com.seniorhub.utils.PreferenceManager
import com.seniorhub.utils.Result
import com.seniorhub.utils.AccessibilityManager
import com.seniorhub.utils.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

/**
 * HealthActivity - Comprehensive Health Tracking Dashboard
 *
 * Features:
 * - Health metrics tracking (blood pressure, heart rate, blood sugar, weight)
 * - Medication management with reminders
 * - Health log history with timestamps
 * - f appointment scheduling
 * - Medical records management
 * - Emergency health alerts
 * - Accessibility support for senior users
 */
class  HealthActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var healthRepository: HealthRepository
    private lateinit var healthLogAdapter: HealthLogAdapter

    // UI Components
    private lateinit var tvTitle: TextView
    private lateinit var tvBloodPressure: TextView
    private lateinit var tvBloodSugar: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var recyclerViewHealthRecords: RecyclerView
    private lateinit var btnBack: Button

    // Health action buttons
    private lateinit var btnLogBloodPressure: Button
    private lateinit var btnLogBloodSugar: Button
    private lateinit var btnLogWeight: Button
    private lateinit var btnLogMedication: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        initializeComponents()
        setupActionBar()
        setupUI()
        setupRecyclerView()
        loadHealthData()
        testFirebaseConnectivity() // Test connectivity for debugging
        applyAccessibilitySettings()
    }

    // onResume already defined below; keep a single definition to avoid overload conflicts

    /**
     * Initialize all components and dependencies
     */
    private fun initializeComponents() {
        preferenceManager = PreferenceManager.getInstance()
        healthRepository = HealthRepository.getInstance()

        // Initialize UI components
        tvTitle = findViewById(R.id.tvTitle)
        tvBloodPressure = findViewById(R.id.tvBloodPressure)
        tvBloodSugar = findViewById(R.id.tvBloodSugar)
        tvWeight = findViewById(R.id.tvWeight)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        recyclerViewHealthRecords = findViewById(R.id.recyclerViewHealthRecords)
        btnBack = findViewById(R.id.btnBack)

        btnLogBloodPressure = findViewById(R.id.btnLogBloodPressure)
        btnLogBloodSugar = findViewById(R.id.btnLogBloodSugar)
        btnLogWeight = findViewById(R.id.btnLogWeight)
        btnLogMedication = findViewById(R.id.btnLogMedication)

    }

    /**
     * Setup action bar with proper title and navigation
     */
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Health Tracking"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    /**
     * Setup all UI components with click listeners and accessibility
     */
    private fun setupUI() {
        try {
            // Set title
            tvTitle.text = "HEALTH TRACKING"

            // Setup back button
            btnBack.setOnClickListener {
                try {
                    finish()
                } catch (e: Exception) {
                    Log.e("HealthActivity", "Error in back button: ${e.message}", e)
                    Toast.makeText(this, "Error closing health tracking. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // Setup health logging buttons
            btnLogBloodPressure.setOnClickListener {
                try {
                    startBloodPressureLogging()
                } catch (e: Exception) {
                    Log.e("HealthActivity", "Error in blood pressure logging: ${e.message}", e)
                    Toast.makeText(this, "Error opening blood pressure logging. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            btnLogBloodSugar.setOnClickListener {
                try {
                    startBloodSugarLogging()
                } catch (e: Exception) {
                    Log.e("HealthActivity", "Error in blood sugar logging: ${e.message}", e)
                    Toast.makeText(this, "Error opening blood sugar logging. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            btnLogWeight.setOnClickListener {
                try {
                    startWeightLogging()
                } catch (e: Exception) {
                    Log.e("HealthActivity", "Error in weight logging: ${e.message}", e)
                    Toast.makeText(this, "Error opening weight logging. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            btnLogMedication.setOnClickListener {
                try {
                    startMedicationLogging()
                } catch (e: Exception) {
                    Log.e("HealthActivity", "Error in medication logging: ${e.message}", e)
                    Toast.makeText(this, "Error opening medication logging. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }


            // Apply accessibility settings
            try {
                if (AccessibilityManager.isVoiceAssistanceEnabled(this)) {
                    setupVoiceAssistance()
                }
            } catch (e: Exception) {
                Log.w("HealthActivity", "Error setting up voice assistance: ${e.message}")
                // Continue without voice assistance if it fails
            }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error setting up UI: ${e.message}", e)
            Toast.makeText(this, "Error setting up health tracking interface. Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Setup RecyclerView for health records
     */
    private fun setupRecyclerView() {
        healthLogAdapter = HealthLogAdapter { healthRecord ->
            showHealthRecordDetails(healthRecord)
        }

        recyclerViewHealthRecords.apply {
            layoutManager = LinearLayoutManager(this@HealthActivity)
            adapter = healthLogAdapter
        }
    }

    /**
     * Load health data from repository and display
     */
    private fun loadHealthData() {
        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            showError("User not logged in")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Try multiple data sources for better reliability
                var recordsLoaded = false
                
                // First try: Load from repository (local cache)
                try {
                    loadHealthRecords(userId)
                    recordsLoaded = true
                } catch (e: Exception) {
                    Log.w("HealthActivity", "Repository load failed: ${e.message}")
                }
                
                // Second try: Load from Firebase Firestore
                try {
                    loadHealthRecordsFromFirebase(userId)
                    recordsLoaded = true
                } catch (e: Exception) {
                    Log.w("HealthActivity", "Firestore load failed: ${e.message}")
                }
                
                // Third try: Load from Realtime Database as fallback
                if (!recordsLoaded) {
                    try {
                        loadHealthRecordsFromRealtimeDatabase(userId)
                        recordsLoaded = true
                    } catch (e: Exception) {
                        Log.w("HealthActivity", "Realtime Database load failed: ${e.message}")
                    }
                }
                
                // If all methods failed, show a more helpful error
                if (!recordsLoaded) {
                    runOnUiThread {
                        showError("Unable to load health records. Please check your internet connection and try again.")
                    }
                } else {
                    // Check if we have any records loaded
                    val totalRecords = healthLogAdapter.itemCount
                    if (totalRecords == 0) {
                        handleEmptyHealthRecords()
                    }
                }

            } catch (e: Exception) {
                Log.e("HealthActivity", "Error in loadHealthData: ${e.message}", e)
                showError("Failed to load health data: ${e.message}")
            }
        }
    }

    /**
     * Load and display health summary
     */
    private suspend fun loadHealthSummary(userId: String) {
        when (val result = healthRepository.getHealthSummary(userId)) {
            is Result.Success -> {
                val summary = result.data
                // Update health metrics display with proper formatting
                tvBloodPressure.text = summary?.bloodPressure ?: "N/A"
                tvBloodSugar.text = summary?.bloodSugar ?: "N/A"
                tvWeight.text = summary?.weight ?: "N/A"
                tvHeartRate.text = summary?.heartRate ?: "N/A"
            }
            is Result.Error -> {
                // Set default values on error
                tvBloodPressure.text = "N/A"
                tvBloodSugar.text = "N/A"
                tvWeight.text = "N/A"
                tvHeartRate.text = "N/A"
            }
            is Result.Loading -> {
                // Handle loading state if needed
            }
        }
    }

    /**
     * Update health metrics grid with latest values from health records
     * Values will never disappear - they persist until new values are added
     */
    private fun updateHealthMetricsGrid(healthRecords: List<HealthRecord>) {
        // Get the most recent record for each type
        val latestBloodPressure = healthRecords.filter { it.type == "blood_pressure" }.maxByOrNull { it.timestamp }
        val latestBloodSugar = healthRecords.filter { it.type == "blood_sugar" }.maxByOrNull { it.timestamp }
        val latestWeight = healthRecords.filter { it.type == "weight" }.maxByOrNull { it.timestamp }
        val latestHeartRate = healthRecords.filter { it.type == "heart_rate" }.maxByOrNull { it.timestamp }

        // Update the display with latest values - only update if we have new values
        // This ensures values never disappear from the grid
        if (latestBloodPressure != null) {
            tvBloodPressure.text = latestBloodPressure.getDisplayValue()
            Log.d("HealthActivity", "Updated Blood Pressure: ${latestBloodPressure.getDisplayValue()}")
        } else {
            // Only set to N/A if we have no records at all
            if (healthRecords.isEmpty()) {
                tvBloodPressure.text = "N/A"
            }
        }
        
        if (latestBloodSugar != null) {
            tvBloodSugar.text = latestBloodSugar.getDisplayValue()
            Log.d("HealthActivity", "Updated Blood Sugar: ${latestBloodSugar.getDisplayValue()}")
        } else {
            if (healthRecords.isEmpty()) {
                tvBloodSugar.text = "N/A"
            }
        }
        
        if (latestWeight != null) {
            tvWeight.text = latestWeight.getDisplayValue()
            Log.d("HealthActivity", "Updated Weight: ${latestWeight.getDisplayValue()}")
        } else {
            if (healthRecords.isEmpty()) {
                tvWeight.text = "N/A"
            }
        }
        
        if (latestHeartRate != null) {
            tvHeartRate.text = latestHeartRate.getDisplayValue()
            Log.d("HealthActivity", "Updated Heart Rate: ${latestHeartRate.getDisplayValue()}")
        } else {
            if (healthRecords.isEmpty()) {
                tvHeartRate.text = "N/A"
            }
        }
    }

    /**
     * Load health records from Firebase Firestore
     */
    private suspend fun loadHealthRecordsFromFirebase(userId: String) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")
            
            // Try with orderBy first, if that fails, try without orderBy
            val query = healthRecordsCollection
                .whereEqualTo("seniorId", userId)
                .limit(50) // Limit to recent 50 records
            
            query
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val healthRecords = mutableListOf<HealthRecord>()
                    for (document in querySnapshot) {
                        try {
                            val healthRecord = document.toObject(HealthRecord::class.java)
                            healthRecord.id = document.id
                            healthRecords.add(healthRecord)
                        } catch (e: Exception) {
                            Log.w("HealthActivity", "Failed to parse document ${document.id}: ${e.message}")
                            continue
                        }
                    }
                    
                    // Sort by timestamp if orderBy didn't work
                    healthRecords.sortByDescending { it.timestamp }
                    
                    runOnUiThread {
                        Log.d("HealthActivity", "Loaded ${healthRecords.size} health records from Firestore")
                        // Show ALL health records - never filter them out
                        healthLogAdapter.updateRecords(healthRecords)
                        // Update health metrics grid with all records
                        updateHealthMetricsGrid(healthRecords)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("HealthActivity", "Firestore query with orderBy failed: ${exception.message}")
                    
                    // Try without orderBy as fallback
                    query.get()
                        .addOnSuccessListener { querySnapshot ->
                            val healthRecords = mutableListOf<HealthRecord>()
                            for (document in querySnapshot) {
                                try {
                                    val healthRecord = document.toObject(HealthRecord::class.java)
                                    healthRecord.id = document.id
                                    healthRecords.add(healthRecord)
                                } catch (e: Exception) {
                                    Log.w("HealthActivity", "Failed to parse document ${document.id}: ${e.message}")
                                    continue
                                }
                            }
                            
                            // Sort by timestamp manually
                            healthRecords.sortByDescending { it.timestamp }
                            
                            runOnUiThread {
                                Log.d("HealthActivity", "Loaded ${healthRecords.size} health records from Firestore (no orderBy)")
                                healthLogAdapter.updateRecords(healthRecords)
                                updateHealthMetricsGrid(healthRecords)
                            }
                        }
                        .addOnFailureListener { fallbackException ->
                            Log.e("HealthActivity", "Firestore fallback query also failed: ${fallbackException.message}")
                            runOnUiThread {
                                val msg = fallbackException.message ?: ""
                                if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                                    // Fallback to Realtime Database if Firestore rules block access
                                    loadHealthRecordsFromRealtimeDatabase(userId)
                                } else {
                                    showError("Failed to load health records: ${fallbackException.message}")
                                }
                            }
                        }
                }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error in loadHealthRecordsFromFirebase: ${e.message}", e)
            runOnUiThread {
                showError("Error loading health records: ${e.message}")
            }
        }
    }

    /**
     * Fallback: Load health records from Firebase Realtime Database if Firestore is denied
     */
    private fun loadHealthRecordsFromRealtimeDatabase(userId: String) {
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("health_records").child(userId)

            ref.get().addOnSuccessListener { snapshot ->
                val records = mutableListOf<HealthRecord>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        try {
                            val rec = child.getValue(HealthRecord::class.java)
                            if (rec != null) {
                                rec.id = child.key ?: rec.id
                                records.add(rec)
                            }
                        } catch (e: Exception) {
                            Log.w("HealthActivity", "Failed to parse Realtime Database record ${child.key}: ${e.message}")
                        }
                    }
                }

                // Sort by timestamp desc
                records.sortByDescending { it.timestamp }

                runOnUiThread {
                    Log.d("HealthActivity", "Loaded ${records.size} health records from Realtime Database")
                    // Show ALL health records - never filter them out
                    healthLogAdapter.updateRecords(records)
                    // Update health metrics grid with all records
                    updateHealthMetricsGrid(records)
                }
            }.addOnFailureListener { e ->
                Log.e("HealthActivity", "Realtime Database load failed: ${e.message}")
                runOnUiThread {
                    showError("Failed to load health records: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error in loadHealthRecordsFromRealtimeDatabase: ${e.message}", e)
            runOnUiThread {
                showError("Error loading health records: ${e.message}")
            }
        }
    }

    /**
     * Load health records into RecyclerView
     */
    private suspend fun loadHealthRecords(userId: String) {
        try {
            when (val result = healthRepository.getHealthRecords(userId)) {
                is Result.Success -> {
                    val all = result.data ?: emptyList()
                    Log.d("HealthActivity", "Loaded ${all.size} health records from repository")
                    // Show ALL health records - never filter them out
                    healthLogAdapter.updateRecords(all)
                    // Update health metrics grid with all records
                    updateHealthMetricsGrid(all)
                }
                is Result.Error -> {
                    Log.w("HealthActivity", "Repository load failed: ${result.exception?.message}")
                    throw Exception("Repository load failed: ${result.exception?.message}")
                }
                is Result.Loading -> {
                    Log.d("HealthActivity", "Repository loading...")
                    // Handle loading state if needed
                }
            }
        } catch (e: Exception) {
            Log.w("HealthActivity", "Repository load error: ${e.message}")
            throw e
        }
    }

    /**
     * Get start and end Date of previous calendar month
     */
    private fun getPreviousMonthRange(): Pair<java.util.Date, java.util.Date> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        // end of previous month = start of this month - 1 ms
        val startOfThisMonth = cal.time
        cal.add(java.util.Calendar.MILLISECOND, -1)
        val endPrev = cal.time

        // move to first day of previous month
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val startPrev = cal.time

        return startPrev to endPrev
    }

    /**
     * Start blood pressure logging
     */
    private fun startBloodPressureLogging() {
        promptAndSaveRecord(
            type = "blood_pressure",
            title = "Blood Pressure",
            hintPrimary = "Systolic (e.g. 120)",
            hintSecondary = "Diastolic (e.g. 80)",
            unit = "mmHg",
            combineAsBp = true
        )
    }

    /**
     * Start blood sugar logging
     */
    private fun startBloodSugarLogging() {
        promptAndSaveRecord(
            type = "blood_sugar",
            title = "Blood Sugar",
            hintPrimary = "mg/dL (e.g. 95)",
            unit = "mg/dL"
        )
    }

    /**
     * Start weight logging
     */
    private fun startWeightLogging() {
        promptAndSaveRecord(
            type = "weight",
            title = "Weight",
            hintPrimary = "Weight (e.g. 65)",
            unit = "kg"
        )
    }

    /**
     * Start heart rate logging (repurposed Medication quick button)
     */
    private fun startMedicationLogging() {
        promptAndSaveRecord(
            type = "heart_rate",
            title = "Heart Rate",
            hintPrimary = "BPM (e.g. 72)",
            unit = "bpm"
        )
    }



    /**
     * Show health record details
     */
    private fun showHealthRecordDetails(healthRecord: HealthRecord) {
        Toast.makeText(this, "Viewing: ${healthRecord.type}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Prompt user for input and save a health record, then update UI
     */
    private fun promptAndSaveRecord(
        type: String,
        title: String,
        hintPrimary: String,
        unit: String,
        hintSecondary: String? = null,
        combineAsBp: Boolean = false
    ) {
        val context = this
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val input1 = android.widget.EditText(context).apply {
            hint = hintPrimary
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(input1)

        var input2: android.widget.EditText? = null
        if (hintSecondary != null) {
            input2 = android.widget.EditText(context).apply {
                hint = hintSecondary
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            container.addView(input2)
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Log $title")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val v1 = input1.text.toString().trim()
                val v2 = input2?.text?.toString()?.trim()

                if (v1.isEmpty() || (hintSecondary != null && v2.isNullOrEmpty())) {
                    showError("Please enter a value")
                    return@setPositiveButton
                }

                // Validate numeric inputs
                try {
                    v1.toDouble()
                    if (v2 != null) v2.toDouble()
                } catch (e: NumberFormatException) {
                    showError("Please enter valid numbers")
                    return@setPositiveButton
                }

                val value = if (combineAsBp && v2 != null) "$v1/$v2" else v1

                saveHealthRecord(type = type, value = value, unit = unit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Save record via repository and refresh summary and list
     */
    private fun saveHealthRecord(type: String, value: String, unit: String) {
        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            showError("User not logged in")
            return
        }

        val record = HealthRecord(
            type = type,
            value = value,
            unit = unit,
            seniorId = userId,
            seniorName = userId,
            timestamp = Date(),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        lifecycleScope.launch {
            try {
                when (val result = healthRepository.saveHealthRecord(userId, record)) {
                    is Result.Success -> {
                        // Update health metrics grid immediately with the new record
                        runOnUiThread {
                            when (record.type) {
                                "blood_pressure" -> tvBloodPressure.text = record.getDisplayValue()
                                "blood_sugar" -> tvBloodSugar.text = record.getDisplayValue()
                                "weight" -> tvWeight.text = record.getDisplayValue()
                                "heart_rate" -> tvHeartRate.text = record.getDisplayValue()
                            }
                        }
                        
                        // Add to adapter for immediate display in list
                        healthLogAdapter.addRecord(record)
                        
                        // Save to Firebase for persistence and admin visibility
                        saveHealthRecordToFirebase(record)
                        
                        Toast.makeText(this@HealthActivity, "Health record saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        val errorMessage = result.exception?.message ?: "Unknown error"
                        showError("Failed to save record: $errorMessage")
                    }
                    is Result.Loading -> {
                        Toast.makeText(this@HealthActivity, "Saving...", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                showError("Error saving health record: ${e.message}")
            }
        }
    }

    /**
     * Save health record to Firebase for persistence and admin visibility
     */
    private fun saveHealthRecordToFirebase(record: HealthRecord) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val healthRecordsCollection = db.collection("health_records")
            
            // Add the record to Firestore
            healthRecordsCollection.add(record)
                .addOnSuccessListener { documentReference ->
                    Log.d("HealthActivity", "Health record saved to Firestore with ID: ${documentReference.id}")
                    // Update the record with the Firestore ID
                    record.id = documentReference.id
                }
                .addOnFailureListener { exception ->
                    Log.e("HealthActivity", "Error saving to Firestore: ${exception.message}")
                    // Also try Realtime Database as fallback
                    saveHealthRecordToRealtimeDatabase(record)
                }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error saving to Firestore: ${e.message}")
            // Also try Realtime Database as fallback
            saveHealthRecordToRealtimeDatabase(record)
        }
    }

    /**
     * Save health record to Firebase Realtime Database as fallback
     */
    private fun saveHealthRecordToRealtimeDatabase(record: HealthRecord) {
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            
            // Get senior name for better navigation
            val seniorName = getSeniorName(record.seniorId)
            val ref = database.getReference("health_records").child(seniorName)
            
            val recordKey = ref.push().key
            if (recordKey != null) {
                record.id = recordKey
                // Add senior name to the record for easier identification
                val recordWithName = record.copy(seniorName = seniorName)
                ref.child(recordKey).setValue(recordWithName)
                    .addOnSuccessListener {
                        Log.d("HealthActivity", "Health record saved to Realtime Database with senior name: $seniorName")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("HealthActivity", "Error saving to Realtime Database: ${exception.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error saving to Realtime Database: ${e.message}")
        }
    }

    /**
     * Get senior name from user ID
     */
    private fun getSeniorName(userId: String): String {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Get user's display name or email as fallback
                val displayName = currentUser.displayName
                if (!displayName.isNullOrEmpty()) {
                    displayName
                } else {
                    // Extract name from email
                    val email = currentUser.email ?: ""
                    val nameFromEmail = email.substringBefore("@")
                    nameFromEmail.replace(".", " ").replace("_", " ").split(" ")
                        .joinToString(" ") { it.capitalize() }
                }
            } else {
                "Unknown Senior"
            }
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error getting senior name: ${e.message}")
            "Unknown Senior"
        }
    }

    /**
     * Setup voice assistance for accessibility
     */
    private fun setupVoiceAssistance() {
        // Voice assistance setup would go here
        // This is a placeholder for future implementation
    }

    /**
     * Apply accessibility settings for senior users
     */
    private fun applyAccessibilitySettings() {
        // Apply health-specific accessibility settings
        AccessibilityManager.applyHealthAccessibilitySettings(
            this,
            tvBloodPressure,
            tvBloodSugar,
            tvWeight,
            tvHeartRate
        )

        // Apply general accessibility settings
        val textSize = AccessibilityManager.getCurrentTextSize(this)
        val isHighContrast = AccessibilityManager.isHighContrastModeEnabled(this)

        // Apply font size adjustments
        if (textSize > 16f) {
            tvTitle.textSize = textSize + 4f
        }

        // Apply high contrast mode if enabled
        if (isHighContrast) {
            AccessibilityManager.applyTextAppearance(this, tvTitle)
        }
    }

    /**
     * Show error message to user
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Handle empty health records state
     */
    private fun handleEmptyHealthRecords() {
        runOnUiThread {
            // Update health metrics grid to show N/A
            tvBloodPressure.text = "N/A"
            tvBloodSugar.text = "N/A"
            tvWeight.text = "N/A"
            tvHeartRate.text = "N/A"
            
            // Clear the adapter
            healthLogAdapter.updateRecords(emptyList())
            
            // Show a helpful message
            Toast.makeText(this@HealthActivity, 
                "No health records found. Add your first health record using the buttons below!", 
                Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Test Firebase connectivity and permissions
     */
    private fun testFirebaseConnectivity() {
        lifecycleScope.launch {
            try {
                val userId = FirebaseManager.getCurrentUserId()
                if (userId == null) {
                    Log.e("HealthActivity", "No user ID available")
                    return@launch
                }
                
                Log.d("HealthActivity", "Testing Firebase connectivity for user: $userId")
                
                // Test Firestore
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("health_records")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { 
                        Log.d("HealthActivity", "Firestore connectivity: OK")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HealthActivity", "Firestore connectivity: FAILED - ${e.message}")
                    }
                
                // Test Realtime Database
                val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
                rtdb.getReference("health_records").child(userId).limitToFirst(1)
                    .get()
                    .addOnSuccessListener {
                        Log.d("HealthActivity", "Realtime Database connectivity: OK")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HealthActivity", "Realtime Database connectivity: FAILED - ${e.message}")
                    }
                    
            } catch (e: Exception) {
                Log.e("HealthActivity", "Firebase connectivity test failed: ${e.message}", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to activity to ensure persistence
        try {
            loadHealthData()
        } catch (e: Exception) {
            Log.e("HealthActivity", "Error in onResume: ${e.message}", e)
            showError("Failed to refresh health data: ${e.message}")
        }
    }
}