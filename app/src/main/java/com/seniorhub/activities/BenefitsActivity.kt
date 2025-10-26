package com.seniorhub.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.seniorhub.adapters.BenefitsAdapter
import com.seniorhub.adapters.BenefitsScheduleAdapter
import com.seniorhub.models.Benefit
import com.seniorhub.repositories.BenefitsRepositoryRTDB
import com.seniorhub.repositories.SimpleBenefitsRepository
import com.seniorhub.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import com.seniorhub.R

/**
 * BenefitsActivity - Senior Assistance Programs Management
 *
 * Features designed for 60+ users:
 * - Large, clear assistance program cards with high contrast
 * - Voice assistance for reading assistance information
 * - Simple one-tap actions to view or apply for assistance
 * - Easy-to-understand assistance categories
 * - Clear status indicators (Active, Pending, Available)
 *
 * Assistance programs include: Medical Assistance, OSCA Benefits, DSWD Programs,
 * Transportation Services, Housing Support, Utility Discounts, etc.
 */
class BenefitsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var preferenceManager: PreferenceManager
    private var textToSpeech: TextToSpeech? = null
    private var isVoiceAssistanceEnabled = false

    // Repository and adapters
    private lateinit var benefitsRepository: BenefitsRepositoryRTDB
    private lateinit var simpleBenefitsRepository: SimpleBenefitsRepository
    private lateinit var benefitsAdapter: BenefitsAdapter
    private lateinit var benefitsScheduleAdapter: BenefitsScheduleAdapter

    // UI Components
    private lateinit var tvAvailableBenefits: TextView
    private lateinit var tvNextDisbursement: TextView
    private lateinit var recyclerViewBenefits: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var cardAvailableBenefits: MaterialCardView
    private lateinit var cardNextSchedule: MaterialCardView
    private lateinit var tvBenefitsListTitle: TextView

    // Current user ID (in real app, get from authentication)
    private val currentUserId = "user123" // Demo user ID
    private var isAdmin = false
    
    // View state management
    private var currentViewMode = "available" // "available" or "schedule"
    private var allBenefits = emptyList<Benefit>()
    private var scheduledBenefits = emptyList<Benefit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benefits)

        // Initialize components
        initializeComponents()
        setupUI()
        checkAdminStatus()
        loadBenefits()
    }

    private fun initializeComponents() {
        try {
            // Initialize preferences and voice assistance
            preferenceManager = PreferenceManager.getInstance()
            textToSpeech = TextToSpeech(this, this)

            // Initialize repositories
            benefitsRepository = BenefitsRepositoryRTDB.getInstance()
            simpleBenefitsRepository = SimpleBenefitsRepository.getInstance()

            // Initialize UI components
            tvAvailableBenefits = findViewById(R.id.tvAvailableBenefits)
            tvNextDisbursement = findViewById(R.id.tvNextDisbursement)
            recyclerViewBenefits = findViewById(R.id.recyclerViewBenefits)
            btnBack = findViewById(R.id.btnBack)
            cardAvailableBenefits = findViewById(R.id.cardAvailableBenefits)
            cardNextSchedule = findViewById(R.id.cardNextSchedule)
            tvBenefitsListTitle = findViewById(R.id.tvBenefitsListTitle)

            // Initialize adapters
            benefitsAdapter = BenefitsAdapter()
            benefitsScheduleAdapter = BenefitsScheduleAdapter()

            // Setup RecyclerView
            recyclerViewBenefits.layoutManager = LinearLayoutManager(this)
            recyclerViewBenefits.adapter = benefitsAdapter
        } catch (e: Exception) {
            Log.e("BenefitsActivity", "Error initializing components: ${e.message}", e)
            Toast.makeText(this, "Error initializing benefits. Please restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.setLanguage(Locale.getDefault())
            isVoiceAssistanceEnabled = preferenceManager.isVoiceAssistanceEnabled

            if (isVoiceAssistanceEnabled) {
                speakText("Senior assistance screen loaded. Review your available and claimed assistance programs.")
            }
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Senior Assistance Programs"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupUI() {
        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up card click listeners
        cardAvailableBenefits.setOnClickListener {
            switchToAvailableView()
        }
        
        cardNextSchedule.setOnClickListener {
            switchToScheduleView()
        }

        // Admin controls removed - no longer needed

        // Apply accessibility settings
        applyAccessibilitySettings()
    }

    private fun checkAdminStatus() {
        // Admin functionality removed - no longer needed
        isAdmin = false
    }

    private fun loadBenefits() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("BenefitsActivity", "Starting to load benefits...")
                
                // Load available benefits first (this will also update the count)
                loadAvailableBenefits()
                
                // Then load benefits summary (which will calculate next disbursement)
                loadBenefitsSummary()
                
                Log.d("BenefitsActivity", "Benefits loading completed")
            } catch (e: Exception) {
                Log.e("BenefitsActivity", "Failed to load benefits: ${e.message}", e)
                Toast.makeText(this@BenefitsActivity, "Failed to load benefits: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Set default values on error
                tvAvailableBenefits.text = "0"
                tvNextDisbursement.text = "TBD"
                benefitsAdapter.updateBenefits(emptyList())
            }
        }
    }

    private suspend fun loadBenefitsSummary() {
        try {
            // Load available benefits to get the count
            when (val result = benefitsRepository.getAvailableBenefits()) {
                is com.seniorhub.utils.Result.Success -> {
                    val benefits = result.data ?: emptyList()
                    val availableCount = benefits.size
                    tvAvailableBenefits.text = availableCount.toString()
                    
                    // Find the next disbursement date from active benefits
                    val nextDisbursement = benefits
                        .filter { it.nextDisbursementDate != null }
                        .minByOrNull { it.nextDisbursementDate!! }?.nextDisbursementDate
                    
                    if (nextDisbursement != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = nextDisbursement
                        val formattedDate = "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
                        tvNextDisbursement.text = formattedDate
                    } else {
                        tvNextDisbursement.text = "TBD"
                    }
                }
                is com.seniorhub.utils.Result.Error -> {
                    tvAvailableBenefits.text = "0"
                    tvNextDisbursement.text = "TBD"
                    Toast.makeText(this, "Failed to load benefits summary", Toast.LENGTH_SHORT).show()
                }
                is com.seniorhub.utils.Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        } catch (e: Exception) {
            Log.e("BenefitsActivity", "Error loading benefits summary: ${e.message}", e)
            tvAvailableBenefits.text = "0"
            tvNextDisbursement.text = "TBD"
        }
    }


    private suspend fun loadAvailableBenefits() {
        try {
            when (val result = benefitsRepository.getAvailableBenefits()) {
                is com.seniorhub.utils.Result.Success -> {
                    val benefits = result.data ?: emptyList()
                    Log.d("BenefitsActivity", "Loaded ${benefits.size} available benefits")
                    
                    // Store all benefits and filter scheduled ones
                    allBenefits = benefits
                    scheduledBenefits = benefits.filter { it.nextDisbursementDate != null }
                    
                    // Update the adapter with the benefits data based on current view
                    updateRecyclerView()
                    
                    // Update benefits summary with actual count
                    tvAvailableBenefits.text = benefits.size.toString()
                    tvNextDisbursement.text = scheduledBenefits.size.toString()
                    
                    // Show success message if benefits were loaded
                    if (benefits.isNotEmpty()) {
                        Log.d("BenefitsActivity", "Benefits loaded successfully: ${benefits.map { it.title }}")
                        Toast.makeText(this, "Loaded ${benefits.size} assistance programs", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("BenefitsActivity", "No benefits found in database")
                        Toast.makeText(this, "No assistance programs available at the moment", Toast.LENGTH_SHORT).show()
                    }
                }
                is com.seniorhub.utils.Result.Error -> {
                    Log.e("BenefitsActivity", "Failed to load available benefits: ${result.exception.message}")
                    Toast.makeText(this, "Failed to load available benefits: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    tvAvailableBenefits.text = "0"
                    tvNextDisbursement.text = "0"
                    
                    // Clear the adapter on error
                    benefitsAdapter.updateBenefits(emptyList())
                }
                is com.seniorhub.utils.Result.Loading -> {
                    Log.d("BenefitsActivity", "Loading available benefits...")
                    // Handle loading state if needed
                }
            }
        } catch (e: Exception) {
            Log.e("BenefitsActivity", "Exception in loadAvailableBenefits: ${e.message}", e)
            Toast.makeText(this, "Error loading benefits: ${e.message}", Toast.LENGTH_SHORT).show()
            tvAvailableBenefits.text = "0"
            tvNextDisbursement.text = "0"
            benefitsAdapter.updateBenefits(emptyList())
        }
    }


    /**
     * Setup real-time benefits stream for automatic updates
     */
    private fun setupRealtimeBenefitsStream() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                simpleBenefitsRepository.getBenefitsStream().collect { result ->
                    when (result) {
                        is com.seniorhub.utils.Result.Success -> {
                            val benefits = result.data ?: emptyList()
                            Log.d("BenefitsActivity", "Real-time update: ${benefits.size} benefits")
                            
                            // Store all benefits and filter scheduled ones
                            allBenefits = benefits
                            scheduledBenefits = benefits.filter { it.nextDisbursementDate != null }
                            
                            // Update adapter with new data based on current view
                            updateRecyclerView()
                            
                            // Update benefits count
                            tvAvailableBenefits.text = benefits.size.toString()
                            tvNextDisbursement.text = scheduledBenefits.size.toString()
                            
                            // Show update notification
                            if (benefits.isNotEmpty()) {
                                Toast.makeText(this@BenefitsActivity, "Benefits updated automatically", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is com.seniorhub.utils.Result.Error -> {
                            Log.e("BenefitsActivity", "Real-time stream error: ${result.exception.message}")
                            // Fallback to cached data
                            val cachedBenefits = simpleBenefitsRepository.getCachedBenefits()
                            benefitsAdapter.updateBenefits(cachedBenefits)
                            tvAvailableBenefits.text = cachedBenefits.size.toString()
                        }
                        is com.seniorhub.utils.Result.Loading -> {
                            Log.d("BenefitsActivity", "Real-time stream loading...")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BenefitsActivity", "Error in real-time stream: ${e.message}", e)
            }
        }
    }

    // claimBenefit method removed - no longer needed

    // Admin dialog methods removed - no longer needed

    /**
     * Switch to available benefits view
     */
    private fun switchToAvailableView() {
        currentViewMode = "available"
        tvBenefitsListTitle.text = getString(R.string.available_assistance)
        updateRecyclerView()
        
        // Update card visual states
        updateCardStates()
        
        if (isVoiceAssistanceEnabled) {
            speakText("Showing available assistance programs")
        }
    }

    /**
     * Switch to next schedule view
     */
    private fun switchToScheduleView() {
        currentViewMode = "schedule"
        tvBenefitsListTitle.text = "Benefits Next Schedule"
        updateRecyclerView()
        
        // Update card visual states
        updateCardStates()
        
        if (isVoiceAssistanceEnabled) {
            speakText("Showing scheduled benefits")
        }
    }

    /**
     * Update RecyclerView based on current view mode
     */
    private fun updateRecyclerView() {
        val benefitsToShow = when (currentViewMode) {
            "available" -> allBenefits
            "schedule" -> scheduledBenefits
            else -> allBenefits
        }
        
        // Switch adapter based on view mode
        when (currentViewMode) {
            "available" -> {
                recyclerViewBenefits.adapter = benefitsAdapter
                benefitsAdapter.updateBenefits(benefitsToShow)
            }
            "schedule" -> {
                recyclerViewBenefits.adapter = benefitsScheduleAdapter
                benefitsScheduleAdapter.updateBenefits(benefitsToShow)
            }
        }
    }

    /**
     * Update card visual states to show which is selected
     */
    private fun updateCardStates() {
        when (currentViewMode) {
            "available" -> {
                cardAvailableBenefits.setCardBackgroundColor(getColor(R.color.senior_benefits))
                cardNextSchedule.setCardBackgroundColor(getColor(R.color.senior_profile_light))
            }
            "schedule" -> {
                cardAvailableBenefits.setCardBackgroundColor(getColor(R.color.senior_benefits_light))
                cardNextSchedule.setCardBackgroundColor(getColor(R.color.senior_profile))
            }
        }
    }

    private fun showHelpDialog() {
        val helpMessage = """
            Davao City Senior Assistance Help:
            
            • ACTIVE status = You currently receive this assistance
            • AVAILABLE status = You can apply for this assistance  
            • PENDING status = Your application is being processed
            
            Available assistance programs:
            • Medical Service Assistance - Free medical consultation and healthcare services
            • DSWD Social Pension - ₱500 monthly cash assistance for indigent seniors
            • DSWD Food Assistance Program - Monthly food packs and nutritional support
            • Davao City Housing Program for Seniors - Housing assistance and emergency shelter
            • Annual Financial Assistance - ₱3,000 yearly cash assistance during Christmas
            
            Main Contacts:
            • DCMC: (082) 227-2731, JP Laurel Ave, Bajada
            • DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada
            • City Housing Office: (082) 222-1234, City Hall
            • OSCA Davao: (082) 222-1234, City Hall Ground Floor
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Davao City Senior Assistance Guide")
            .setMessage(helpMessage)
            .setPositiveButton("OK", null)
            .show()

        if (isVoiceAssistanceEnabled) {
            speakText("Davao City senior assistance help: Active status means you currently receive the assistance. Available means you can apply. Pending means your application is being processed.")
        }
    }

    private fun applyAccessibilitySettings() {
        // Apply larger text sizes if enabled
        if (preferenceManager.fontSize > 20f) {
            try {
                findViewById<TextView>(android.R.id.text1)?.textSize =
                    preferenceManager.fontSize + 4f
                findViewById<TextView>(android.R.id.text2)?.textSize = preferenceManager.fontSize
            } catch (e: Exception) {
                // Views not found, continue
            }
        }
    }

    private fun speakText(text: String) {
        if (isVoiceAssistanceEnabled && textToSpeech != null) {
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tts_${System.currentTimeMillis()}"
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        textToSpeech?.shutdown()
        super.onDestroy()
    }

}