package com.seniorhub.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.TextPaint
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.seniorhub.R
import com.seniorhub.config.CloudinaryConfig
import com.seniorhub.models.EmergencyContact
import com.seniorhub.models.User
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.CloudinaryManager
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.Result
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    // Step containers
    private lateinit var step1: View
    private lateinit var step2: View
    private lateinit var step3: View
    private lateinit var step4: View

    // Header and progress
    private lateinit var btnBackToLogin: ImageButton
    private lateinit var tvStep: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var progressBar: CircularProgressIndicator

    // Step 1 fields
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etBirthday: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etGender: AutoCompleteTextView
    private lateinit var etMaritalStatus: AutoCompleteTextView
    private lateinit var etCellphoneNumber: TextInputEditText
    private lateinit var cbSSSMember: MaterialCheckBox
    private lateinit var etSSSNumber: TextInputEditText
    private lateinit var cbGSISMember: MaterialCheckBox
    private lateinit var etGSISNumber: TextInputEditText
    private lateinit var cbOSCAMember: MaterialCheckBox
    private lateinit var etOSCANumber: TextInputEditText
    private lateinit var cbPhilHealthMember: MaterialCheckBox
    private lateinit var etPhilHealthNumber: TextInputEditText

    // Step 2 fields
    private lateinit var etHouseStreet: TextInputEditText
    private lateinit var etBarangay: AutoCompleteTextView

    // Step 3 fields
    private lateinit var etEmergencyFirstName: TextInputEditText
    private lateinit var etEmergencyLastName: TextInputEditText
    private lateinit var etEmergencyContactPhone: TextInputEditText
    private lateinit var etRelationship: AutoCompleteTextView

    // Step 4 fields
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnSelectProfilePicture: MaterialButton
    private lateinit var cbTerms: MaterialCheckBox
    

    // Nav buttons
    private lateinit var btnPrevious: MaterialButton
    private lateinit var btnNext: MaterialButton

    // Helpers
    private var currentStep = 1
    private var selectedImageUri: Uri? = null
    private var isNavigating = false // Prevent multiple navigation attempts
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uri = data?.data
            if (uri != null) {
                selectedImageUri = uri
                ivProfilePicture.setImageURI(uri)
            }
        }
    }

    private fun setupDropdowns() {
        try {
            // Gender
            resources.getStringArray(R.array.gender_options).let { arr ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etGender.setAdapter(adapter)
                etGender.setOnClickListener { etGender.showDropDown() }
            }

            // Marital Status
            resources.getStringArray(R.array.marital_status_options).let { arr ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etMaritalStatus.setAdapter(adapter)
                etMaritalStatus.setOnClickListener { etMaritalStatus.showDropDown() }
            }

            // Barangay
            resources.getStringArray(R.array.davao_barangays).let { arr ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etBarangay.setAdapter(adapter)
                etBarangay.setOnClickListener { etBarangay.showDropDown() }
            }

            // Relationship
            resources.getStringArray(R.array.relationship_options).let { arr ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etRelationship.setAdapter(adapter)
                etRelationship.setOnClickListener { etRelationship.showDropDown() }
            }
        } catch (_: Exception) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize services
        try { FirebaseManager.initialize(this) } catch (_: Exception) {}
        try { CloudinaryManager.initialize(this) } catch (_: Exception) {}

        bindViews()
        setupDropdowns()
        setupInteractions()
        updateStepUi()
    }

    private fun bindViews() {
        step1 = findViewById(R.id.step1Layout)
        step2 = findViewById(R.id.step2Layout)
        step3 = findViewById(R.id.step3Layout)
        step4 = findViewById(R.id.step4Layout)

        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvStep = findViewById(R.id.tvStepIndicator)
        progressIndicator = findViewById(R.id.progressIndicator)
        progressBar = findViewById(R.id.progressBar)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etBirthday = findViewById(R.id.etBirthday)
        etAge = findViewById(R.id.etAge)
        etGender = findViewById(R.id.etGender)
        etMaritalStatus = findViewById(R.id.etMaritalStatus)
        etCellphoneNumber = findViewById(R.id.etCellphoneNumber)
        cbSSSMember = findViewById(R.id.cbSSSMember)
        etSSSNumber = findViewById(R.id.etSSSNumber)
        cbGSISMember = findViewById(R.id.cbGSISMember)
        etGSISNumber = findViewById(R.id.etGSISNumber)
        cbOSCAMember = findViewById(R.id.cbOSCAMember)
        etOSCANumber = findViewById(R.id.etOSCANumber)
        cbPhilHealthMember = findViewById(R.id.cbPhilHealthMember)
        etPhilHealthNumber = findViewById(R.id.etPhilHealthNumber)

        etHouseStreet = findViewById(R.id.etHouseStreet)
        etBarangay = findViewById(R.id.etBarangay)

        etEmergencyFirstName = findViewById(R.id.etEmergencyFirstName)
        etEmergencyLastName = findViewById(R.id.etEmergencyLastName)
        etEmergencyContactPhone = findViewById(R.id.etEmergencyContactPhone)
        etRelationship = findViewById(R.id.etRelationship)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        btnSelectProfilePicture = findViewById(R.id.btnSelectProfilePicture)
        cbTerms = findViewById(R.id.cbTermsAndConditions)
        // Render clickable HTML in checkbox label
        try {
            val htmlText = HtmlCompat.fromHtml(
                getString(R.string.i_agree_terms_bold),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            cbTerms.text = htmlText
            makeTermsAndConditionsClickable()
        } catch (_: Exception) { }

        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
    }

    private fun setupInteractions() {
        btnBackToLogin.setOnClickListener { finish() }

        // Show Material calendar date picker for birthday
        etBirthday.setOnClickListener {
            try {
                val today = Calendar.getInstance()
                val sixtyYearsAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -60) }

                val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.birthday))
                    .setSelection(sixtyYearsAgo.timeInMillis)

                val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
                    .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
                    .build()
                builder.setCalendarConstraints(constraints)

                val picker = builder.build()
                picker.addOnPositiveButtonClickListener { selection ->
                    if (selection != null) {
                        val picked = Calendar.getInstance().apply { timeInMillis = selection }
                        etBirthday.setText(dateFormat.format(picked.time))
                        etAge.setText(calculateAge(picked.time).toString())
                    }
                }
                picker.show(supportFragmentManager, "birthday_picker")
            } catch (_: Exception) { }
        }

        // Toggle ID inputs
        cbSSSMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<TextInputLayout>(R.id.tilSSSNumber).visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        cbGSISMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<TextInputLayout>(R.id.tilGSISNumber).visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        cbOSCAMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<TextInputLayout>(R.id.tilOSCANumber).visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        cbPhilHealthMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<TextInputLayout>(R.id.tilPhilHealthNumber).visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnSelectProfilePicture.setOnClickListener { pickImageFromGallery() }

        // Checkbox remains; dialogs open via clickable spans in text
        cbTerms.setOnClickListener { /* no-op */ }

        btnPrevious.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUi()
            }
        }

        btnNext.setOnClickListener {
            if (currentStep < 4) {
                if (validateCurrentStep()) {
                    currentStep++
                    updateStepUi()
                }
            } else {
                if (validateAll()) {
                    register()
                }
            }
        }
    }

    private fun updateStepUi() {
        step1.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        step2.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        step3.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        step4.visibility = if (currentStep == 4) View.VISIBLE else View.GONE

        btnPrevious.isVisible = currentStep > 1
        btnNext.text = if (currentStep == 4) getString(R.string.create_account) else getString(R.string.next)

        tvStep.text = "Step ${currentStep} of 4"
        progressIndicator.setProgressCompat(currentStep * 25, true)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun calculateAge(birthDate: Date): Int {
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = birthDate }
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                when {
                    etFirstName.text.isNullOrBlank() -> { toast("First name is required"); false }
                    etLastName.text.isNullOrBlank() -> { toast("Last name is required"); false }
                    etGender.text.isNullOrBlank() -> { toast("Gender is required"); false }
                    else -> true
                }
            }
            2 -> {
                if (etHouseStreet.text.isNullOrBlank()) { toast("House and street is required"); false } else true
            }
            3 -> {
                val hasName = !etEmergencyFirstName.text.isNullOrBlank() || !etEmergencyLastName.text.isNullOrBlank()
                if (!hasName || etEmergencyContactPhone.text.isNullOrBlank()) {
                    toast("Emergency contact name and phone are required"); false
                } else true
            }
            else -> true
        }
    }

    private fun validateAll(): Boolean {
        if (!validateCurrentStep()) return false
        if (etEmail.text.isNullOrBlank()) { toast("Email is required"); return false }
        if (!isValidEmail(etEmail.text.toString())) { toast("Please enter a valid email address"); return false }
        if (etPassword.text.isNullOrBlank()) { toast("Password is required"); return false }
        if (etPassword.text.toString() != etConfirmPassword.text.toString()) { toast("Passwords do not match"); return false }
        if (!cbTerms.isChecked) { toast("Please agree to the terms"); return false }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    private fun register() {
        setLoading(true)

        val email = etEmail.text!!.toString().trim()
        val password = etPassword.text!!.toString()

        lifecycleScope.launch {
            when (val authResult = FirebaseManager.createUserWithEmailAndPassword(email, password)) {
                is Result.Success -> {
                    val firebaseUser = authResult.data
                    if (firebaseUser == null) {
                        setLoading(false); toast("Authentication failed"); return@launch
                    }

                    // Optionally upload image first
                    var profileUrl = ""
                    if (selectedImageUri != null) {
                        try {
                            // Ensure Cloudinary is initialized at runtime
                            if (!CloudinaryManager.isReady()) {
                                try { CloudinaryManager.initialize(this@RegisterActivity.applicationContext) } catch (_: Exception) { }
                            }

                            profileUrl = CloudinaryManager.uploadImage(
                                selectedImageUri!!,
                                "profile_${firebaseUser.uid}",
                                CloudinaryConfig.FOLDER
                            )
                            android.util.Log.d("RegisterActivity", "Profile image uploaded successfully: $profileUrl")
                        } catch (e: Exception) {
                            // If upload fails, proceed but keep URL empty; log and inform user for later retry
                            android.util.Log.e("RegisterActivity", "Profile image upload failed: ${e.message}", e)
                            Toast.makeText(this@RegisterActivity, "Couldn't upload profile image now. You can add it later in Profile.", Toast.LENGTH_LONG).show()
                        }
                    }

                    val user = buildUser(firebaseUser.uid, profileUrl)

                    when (val save = UserRepository.getInstance().saveUser(user)) {
                        is Result.Success -> {
                            // Update Firebase Auth display name and photo for immediate header binding
                            try {
                                val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName("${user.firstName} ${user.lastName}".trim())
                                    .apply {
                                        if (user.profileImageUrl.isNotEmpty()) {
                                            setPhotoUri(Uri.parse(user.profileImageUrl))
                                        }
                                    }
                                    .build()
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdate)
                            } catch (_: Exception) { }

                            toast("Welcome To SeniorHub")
                            // Navigate to main dashboard - ensure single navigation
                            if (!isFinishing && !isDestroyed && !isNavigating) {
                                isNavigating = true
                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                        is Result.Error -> {
                            setLoading(false)
                            toast("Failed to save profile: ${save.exception.message}")
                        }
                        else -> setLoading(false)
                    }
                }
                is Result.Error -> {
                    setLoading(false)
                    toast(authResult.exception.message ?: "Registration failed")
                }
                else -> setLoading(false)
            }
        }
    }

    private fun buildUser(userId: String, imageUrl: String): User {
        val user = User()
        user.id = userId
        user.firstName = etFirstName.text?.toString() ?: ""
        user.lastName = etLastName.text?.toString() ?: ""
        user.gender = etGender.text?.toString() ?: ""
        user.maritalStatus = etMaritalStatus.text?.toString() ?: ""
        user.phoneNumber = etCellphoneNumber.text?.toString() ?: ""
        user.email = etEmail.text?.toString() ?: ""
        user.houseNumberAndStreet = etHouseStreet.text?.toString() ?: ""
        user.barangay = etBarangay.text?.toString() ?: ""
        user.profileImageUrl = imageUrl
        user.sssNumber = if (cbSSSMember.isChecked) etSSSNumber.text?.toString() ?: "" else ""
        user.gsisNumber = if (cbGSISMember.isChecked) etGSISNumber.text?.toString() ?: "" else ""
        user.oscaNumber = if (cbOSCAMember.isChecked) etOSCANumber.text?.toString() ?: "" else ""
        user.philHealthNumber = if (cbPhilHealthMember.isChecked) etPhilHealthNumber.text?.toString() ?: "" else ""

        // Birthday/age
        // Birthday/age from text field after picker selection
        val birthdayText = etBirthday.text?.toString()
        if (!birthdayText.isNullOrBlank()) {
            try {
                val date = dateFormat.parse(birthdayText)
                if (date != null) {
                    user.birthDate = date
                    user.age = calculateAge(date)
                }
            } catch (_: Exception) {}
        } else if (!etAge.text.isNullOrBlank()) {
            try { user.age = etAge.text.toString().toInt() } catch (_: Exception) {}
        }

        // Emergency contact
        val contactName = listOf(
            etEmergencyFirstName.text?.toString()?.trim().orEmpty(),
            etEmergencyLastName.text?.toString()?.trim().orEmpty()
        ).filter { it.isNotEmpty() }.joinToString(" ")

        val contact = EmergencyContact(
            name = contactName,
            phoneNumber = etEmergencyContactPhone.text?.toString() ?: "",
            relationship = etRelationship.text?.toString() ?: "",
            isPrimary = true
        )
        user.emergencyContacts = if (contact.name.isNotBlank() || contact.phoneNumber.isNotBlank()) listOf(contact) else emptyList()

        return user
    }

    private fun setLoading(loading: Boolean) {
        progressBar.isVisible = loading
        btnNext.isEnabled = !loading
        btnPrevious.isEnabled = !loading
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Toggle Terms and Conditions cards visibility
     */
    private fun toggleTermsAndConditionsCards() { /* removed cards */ }

    /**
     * Show Terms dialog
     */
    private fun showTermsCard() {
        showTermsDialog()
    }

    /**
     * Show Conditions dialog
     */
    private fun showConditionsCard() {
        showConditionsDialog()
    }

    /**
     * Fade out a card with animation
     */
    private fun fadeOutCard(card: com.google.android.material.card.MaterialCardView) { /* removed */ }

    /**
     * Make Terms and Conditions text clickable
     */
    private fun makeTermsAndConditionsClickable() {
        val text = cbTerms.text
        val spannableString = SpannableString(text)
        
        // Find "Terms" text and make it clickable
        val termsStart = text.toString().indexOf("Terms")
        val termsEnd = termsStart + "Terms".length
        if (termsStart >= 0) {
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    showTermsCard()
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = cbTerms.currentTextColor
                    ds.isFakeBoldText = true
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        // Find "Conditions" text and make it clickable
        val conditionsStart = text.toString().indexOf("Conditions")
        val conditionsEnd = conditionsStart + "Conditions".length
        if (conditionsStart >= 0) {
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    showConditionsCard()
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = cbTerms.currentTextColor
                    ds.isFakeBoldText = true
                }
            }, conditionsStart, conditionsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        cbTerms.text = spannableString
        cbTerms.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Show Terms dialog
     */
    private fun showTermsDialog() {
        // Check if activity is still running
        if (isFinishing || isDestroyed) {
            return
        }

        try {
            val dialog = android.app.AlertDialog.Builder(this, R.style.Theme_SeniorHub_AlertDialog)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_terms, null)

            dialog.setView(dialogView)
            val alertDialog = dialog.create()

            // Set up Yes button
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTermsDialogYes).setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error showing terms dialog: ${e.message}")
        }
    }

    /**
     * Show Conditions dialog
     */
    private fun showConditionsDialog() {
        // Check if activity is still running
        if (isFinishing || isDestroyed) {
            return
        }

        try {
            val dialog = android.app.AlertDialog.Builder(this, R.style.Theme_SeniorHub_AlertDialog)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_conditions, null)

            dialog.setView(dialogView)
            val alertDialog = dialog.create()

            // Set up Yes button
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConditionsDialogYes).setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error showing conditions dialog: ${e.message}")
        }
    }
}