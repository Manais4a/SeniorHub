package com.seniorhub.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.app.DatePickerDialog
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.seniorhub.R
import com.seniorhub.adapters.EmergencyContactAdapter
import com.seniorhub.models.EmergencyContact
import com.seniorhub.models.User
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.PreferenceManager
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.AccessibilityManager
import com.seniorhub.utils.Result
import com.seniorhub.utils.CloudinaryManager
import com.seniorhub.config.CloudinaryConfig
import com.seniorhub.utils.ImageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var userRepository: UserRepository
    private val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())

    // UI Components - Personal Information
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etBirthday: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etGender: android.widget.AutoCompleteTextView
    private lateinit var etMaritalStatus: android.widget.AutoCompleteTextView
    private lateinit var etCellphoneNumber: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etHouseStreet: TextInputEditText
    private lateinit var etBarangay: android.widget.AutoCompleteTextView
    private lateinit var etCity: TextInputEditText
    private lateinit var etProvince: TextInputEditText
    private lateinit var etPostalCode: TextInputEditText

    // UI Components - Membership Information
    private lateinit var cbSSSMember: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var etSSSNumber: TextInputEditText
    private lateinit var cbGSISMember: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var etGSISNumber: TextInputEditText
    private lateinit var cbOSCAMember: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var etOSCANumber: TextInputEditText
    private lateinit var cbPhilHealthMember: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var etPhilHealthNumber: TextInputEditText

    // UI Components - Emergency Contact Information
    private lateinit var etEmergencyContactName: TextInputEditText
    private lateinit var etEmergencyContactPhone: TextInputEditText
    private lateinit var etRelationship: android.widget.AutoCompleteTextView

    // UI Components - Action Buttons
    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSave: Button

    // UI Components - Verification Status
    private lateinit var verificationStatusCard: com.google.android.material.card.MaterialCardView
    private lateinit var tvVerificationStatus: TextView
    private lateinit var ivVerificationIcon: ImageView

    // Data
    private var currentUser: User? = null
    private var isEditMode = false

    // Image handling
    private var profileImageUri: Uri? = null
    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_LONG).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.data != null -> {
                    // Gallery image selected
                    profileImageUri = data.data
                    loadProfileImage()
                    uploadProfileImage()
                }
                data?.extras?.get("data") != null -> {
                    // Camera image captured
                    val bitmap = data.extras?.get("data") as? Bitmap
                    if (bitmap != null) {
                        profileImageUri = saveBitmapToUri(bitmap)
                        loadProfileImage()
                        uploadProfileImage()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Cloudinary
        try {
            CloudinaryManager.initialize(this)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Failed to initialize Cloudinary: ${e.message}")
        }

        initializeComponents()
        setupToolbar()
        setupDropdowns()
        setupUI()
        loadUserProfile()
    }

    private fun setupDropdowns() {
        try {
            // Gender dropdown
            resources.getStringArray(R.array.gender_options).let { arr ->
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etGender.setAdapter(adapter)
                etGender.setOnClickListener { etGender.showDropDown() }
            }

            // Marital Status dropdown
            resources.getStringArray(R.array.marital_status_options).let { arr ->
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etMaritalStatus.setAdapter(adapter)
                etMaritalStatus.setOnClickListener { etMaritalStatus.showDropDown() }
            }

            // Barangay dropdown
            resources.getStringArray(R.array.davao_barangays).let { arr ->
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etBarangay.setAdapter(adapter)
                etBarangay.setOnClickListener { etBarangay.showDropDown() }
            }

            // Relationship dropdown
            resources.getStringArray(R.array.relationship_options).let { arr ->
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, arr)
                etRelationship.setAdapter(adapter)
                etRelationship.setOnClickListener { etRelationship.showDropDown() }
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error setting up dropdowns: ${e.message}")
        }
    }

    private fun initializeComponents() {
        preferenceManager = PreferenceManager.getInstance()
        userRepository = UserRepository.getInstance()

        // Initialize UI components - Personal Information
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etBirthday = findViewById(R.id.etBirthday)
        etAge = findViewById(R.id.etAge)
        etGender = findViewById(R.id.etGender)
        etMaritalStatus = findViewById(R.id.etMaritalStatus)
        etCellphoneNumber = findViewById(R.id.etCellphoneNumber)
        etEmail = findViewById(R.id.etEmail)
        etHouseStreet = findViewById(R.id.etHouseStreet)
        etBarangay = findViewById(R.id.etBarangay)
        etCity = findViewById(R.id.etCity)
        etProvince = findViewById(R.id.etProvince)
        etPostalCode = findViewById(R.id.etPostalCode)

        // Initialize UI components - Membership Information
        cbSSSMember = findViewById(R.id.cbSSSMember)
        etSSSNumber = findViewById(R.id.etSSSNumber)
        cbGSISMember = findViewById(R.id.cbGSISMember)
        etGSISNumber = findViewById(R.id.etGSISNumber)
        cbOSCAMember = findViewById(R.id.cbOSCAMember)
        etOSCANumber = findViewById(R.id.etOSCANumber)
        cbPhilHealthMember = findViewById(R.id.cbPhilHealthMember)
        etPhilHealthNumber = findViewById(R.id.etPhilHealthNumber)

        // Initialize UI components - Emergency Contact Information
        etEmergencyContactName = findViewById(R.id.etEmergencyContactName)
        etEmergencyContactPhone = findViewById(R.id.etEmergencyContactPhone)
        etRelationship = findViewById(R.id.etRelationship)

        // Initialize UI components - Action Buttons
        try {
            btnBack = findViewById(R.id.btnBack)
            btnEdit = findViewById(R.id.btnEdit)
            btnChangePhoto = findViewById(R.id.btnChangePhoto)
            btnSave = findViewById(R.id.btnSave)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error initializing action buttons: ${e.message}", e)
            Toast.makeText(this, "Error loading UI components: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Verification status views
        verificationStatusCard = findViewById(R.id.verificationStatusCard)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)
        ivVerificationIcon = findViewById(R.id.ivVerificationIcon)
    }

    private fun setupToolbar() {
        try {
            val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            // Check if we can set the toolbar as action bar
            if (supportActionBar == null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            } else {
                // Action bar already exists, just configure it
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error setting up toolbar: ${e.message}")
        }
    }

    private fun setupUI() {
        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit button
        btnEdit.setOnClickListener {
            toggleEditMode(true)
        }

        // Set up save button
        btnSave?.setOnClickListener {
            saveProfile()
        }

        // Set up change photo button
        btnChangePhoto?.setOnClickListener {
            showImageSourceDialog()
        }

        // Set up profile image click
        ivProfilePhoto.setOnClickListener {
            if (isEditMode) {
                showImageSourceDialog()
            }
        }

        // Set up birthday date picker
        etBirthday.setOnClickListener {
            if (isEditMode) {
                showBirthdayDatePicker()
            }
        }

        // Set up membership checkbox listeners
        cbSSSMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSSSNumber)?.visibility = 
                if (isChecked) View.VISIBLE else View.GONE
        }
        cbGSISMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilGSISNumber)?.visibility = 
                if (isChecked) View.VISIBLE else View.GONE
        }
        cbOSCAMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilOSCANumber)?.visibility = 
                if (isChecked) View.VISIBLE else View.GONE
        }
        cbPhilHealthMember.setOnCheckedChangeListener { _, isChecked ->
            findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPhilHealthNumber)?.visibility = 
                if (isChecked) View.VISIBLE else View.GONE
        }

        // Initially disable editing
        setEditingEnabled(false)
    }

    private fun loadUserProfile() {
        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            showError("User not logged in")
            finish()
            return
        }

        Log.d("ProfileActivity", "Loading profile for user ID: $userId")

        lifecycleScope.launch {
            try {
                when (val result = userRepository.getUserById(userId)) {
                    is Result.Success -> {
                        val user = result.data?.user
                        if (user != null) {
                            Log.d("ProfileActivity", "User profile loaded successfully: ${user.firstName} ${user.lastName}")
                            currentUser = user
                            displayUserProfile(user)
                            updateVerificationCard(user.accountVerified)
                            observeVerificationStatus(user.id)
                        } else {
                            Log.w("ProfileActivity", "User profile not found in database")
                            showError("User profile not found. Please complete your registration.")
                        }
                    }

                    is Result.Error -> {
                        Log.e("ProfileActivity", "Error loading profile: ${result.exception.message}", result.exception)
                        showError("Failed to load profile: ${result.exception.message}")
                    }

                    is Result.Loading<*> -> {
                        Log.d("ProfileActivity", "Loading user profile...")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Exception in loadUserProfile: ${e.message}", e)
                showError("Error loading profile: ${e.message}")
            }
        }
    }

    private fun createDefaultUserProfile(userId: String) {
        lifecycleScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val defaultUser = User().apply {
                    id = userId
                    firstName = firebaseUser?.displayName?.split(" ")?.firstOrNull() ?: ""
                    lastName = firebaseUser?.displayName?.split(" ")?.lastOrNull() ?: ""
                    email = firebaseUser?.email ?: ""
                    phoneNumber = ""
                    profileImageUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    createdAt = com.google.firebase.Timestamp.now()
                    updatedAt = com.google.firebase.Timestamp.now()
                    isActive = true
                    role = "senior_citizen"
                }

                // Save the default user to Firestore
                when (val result = userRepository.updateUser(defaultUser)) {
                    is Result.Success -> {
                        currentUser = defaultUser
                        displayUserProfile(defaultUser)
                        Toast.makeText(this@ProfileActivity, "Profile created successfully", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        showError("Failed to create profile: ${result.exception.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error creating default profile: ${e.message}")
                showError("Failed to load or create profile")
            }
        }
    }

    private fun displayUserProfile(user: User) {
        // Personal Information
        etFirstName.setText(user.firstName)
        etLastName.setText(user.lastName)

        // Format and display birthday
        user.birthDate?.let { birthDate ->
            val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
            etBirthday.setText(dateFormat.format(birthDate))
        }

        etAge.setText(user.getAge()?.toString() ?: user.age.toString())
        etGender.setText(user.gender)
        etMaritalStatus.setText(user.maritalStatus)
        etCellphoneNumber.setText(user.phoneNumber)
        etEmail.setText(user.email)
        etHouseStreet.setText(user.houseNumberAndStreet)
        etBarangay.setText(user.barangay)
        etCity.setText(user.city)
        etProvince.setText(user.province)
        etPostalCode.setText(user.zipCode)

        // Membership Information
        cbSSSMember.isChecked = user.sssNumber.isNotEmpty()
        etSSSNumber.setText(user.sssNumber)
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSSSNumber)?.visibility = 
            if (user.sssNumber.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        cbGSISMember.isChecked = user.gsisNumber.isNotEmpty()
        etGSISNumber.setText(user.gsisNumber)
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilGSISNumber)?.visibility = 
            if (user.gsisNumber.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        cbOSCAMember.isChecked = user.oscaNumber.isNotEmpty()
        etOSCANumber.setText(user.oscaNumber)
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilOSCANumber)?.visibility = 
            if (user.oscaNumber.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        cbPhilHealthMember.isChecked = user.philHealthNumber.isNotEmpty()
        etPhilHealthNumber.setText(user.philHealthNumber)
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPhilHealthNumber)?.visibility = 
            if (user.philHealthNumber.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        // Emergency Contact Information
        val primaryContact = user.getPrimaryEmergencyContact()
        etEmergencyContactName.setText(primaryContact?.name ?: "")
        etEmergencyContactPhone.setText(primaryContact?.phoneNumber ?: "")
        etRelationship.setText(primaryContact?.relationship ?: "")

        // Load profile image
        val firestoreUrl = user.profileImageUrl
        val authUrl = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()

        val finalUrl = when {
            !firestoreUrl.isNullOrEmpty() && firestoreUrl.startsWith("http") -> firestoreUrl
            !authUrl.isNullOrEmpty() && authUrl.startsWith("http") -> authUrl
            else -> null
        }

        if (finalUrl != null) {
            val optimizedUrl = if (ImageLoader.isValidCloudinaryUrl(finalUrl)) {
                ImageLoader.getOptimizedCloudinaryUrl(finalUrl, 400, 400)
            } else {
                finalUrl
            }

            ImageLoader.loadProfileImage(
                this,
                ivProfilePhoto,
                optimizedUrl,
                R.drawable.ic_profile,
                R.drawable.ic_profile
            )
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_profile)
        }

        // Also update verification card on bind
        updateVerificationCard(user.accountVerified)
    }

    private fun updateVerificationCard(isVerified: Boolean) {
        try {
            if (isVerified) {
                // Verified: light blue background, white text/icon
                verificationStatusCard.setCardBackgroundColor(resources.getColor(R.color.primary_light, null))
                tvVerificationStatus.text = getString(R.string.verified_account)
                tvVerificationStatus.setTextColor(resources.getColor(R.color.focus_blue, null))
                ivVerificationIcon.setImageResource(R.drawable.ic_verified)
                ivVerificationIcon.setColorFilter(resources.getColor(R.color.focus_blue, null))

            } else {
                // Unverified: light red background, white text/icon
                verificationStatusCard.setCardBackgroundColor(resources.getColor(R.color.emergency_red_light, null))
                tvVerificationStatus.text = getString(R.string.unverified_account)
                tvVerificationStatus.setTextColor(resources.getColor(R.color.emergency_red, null))
                ivVerificationIcon.setImageResource(R.drawable.ic_unverified)
                ivVerificationIcon.setColorFilter(resources.getColor(R.color.emergency_red, null))

            }
        } catch (e: Exception) {
            Log.w("ProfileActivity", "Failed to style verification card: ${e.message}")
        }
    }

    private fun observeVerificationStatus(userId: String) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("users").document(userId)
                .addSnapshotListener(this) { snapshot, error ->
                    if (error != null) {
                        Log.w("ProfileActivity", "Profile listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        // Update verification status
                        val verified = snapshot.getBoolean("accountVerified") ?: false
                        updateVerificationCard(verified)
                        
                        // Check if this update was made by admin (not by the user themselves)
                        val lastUpdatedBy = snapshot.getString("lastUpdatedBy")
                        if (lastUpdatedBy == "admin") {
                            // Admin made changes, refresh the entire profile
                            Log.d("ProfileActivity", "Admin updated profile, refreshing data...")
                            refreshProfileFromSnapshot(snapshot)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("ProfileActivity", "Failed to observe profile changes: ${e.message}")
        }
    }
    
    private fun refreshProfileFromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                currentUser = user
                displayUserProfile(user)
                Log.d("ProfileActivity", "Profile refreshed from admin update")
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error refreshing profile from snapshot: ${e.message}")
        }
    }

    private fun setEditingEnabled(enabled: Boolean) {
        // Personal Information
        etFirstName.isEnabled = enabled
        etLastName.isEnabled = enabled
        etBirthday.isEnabled = enabled
        etAge.isEnabled = false // Age is auto-calculated, not editable
        etGender.isEnabled = enabled
        etMaritalStatus.isEnabled = enabled
        etCellphoneNumber.isEnabled = enabled
        etEmail.isEnabled = enabled
        etHouseStreet.isEnabled = enabled
        etBarangay.isEnabled = enabled
        etCity.isEnabled = false // City is fixed
        etProvince.isEnabled = false // Province is fixed
        etPostalCode.isEnabled = false // Postal code is fixed

        // Membership Information
        cbSSSMember.isEnabled = enabled
        etSSSNumber.isEnabled = enabled
        cbGSISMember.isEnabled = enabled
        etGSISNumber.isEnabled = enabled
        cbOSCAMember.isEnabled = enabled
        etOSCANumber.isEnabled = enabled
        cbPhilHealthMember.isEnabled = enabled
        etPhilHealthNumber.isEnabled = enabled

        // Emergency Contact Information
        etEmergencyContactName.isEnabled = enabled
        etEmergencyContactPhone.isEnabled = enabled
        etRelationship.isEnabled = enabled

        // Action buttons
        btnChangePhoto?.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleEditMode(editMode: Boolean) {
        isEditMode = editMode
        setEditingEnabled(editMode)

        if (editMode) {
            btnEdit.visibility = android.view.View.GONE
            btnSave?.visibility = android.view.View.VISIBLE
        } else {
            btnEdit.visibility = android.view.View.VISIBLE
            btnSave?.visibility = android.view.View.GONE
        }
    }

    private fun saveProfile() {
        val user = currentUser ?: return

        // Update user object with form data
        user.firstName = etFirstName.text?.toString() ?: ""
        user.lastName = etLastName.text?.toString() ?: ""
        user.gender = etGender.text?.toString() ?: ""
        user.maritalStatus = etMaritalStatus.text?.toString() ?: ""
        user.phoneNumber = etCellphoneNumber.text?.toString() ?: ""
        user.email = etEmail.text?.toString() ?: ""
        user.houseNumberAndStreet = etHouseStreet.text?.toString() ?: ""
        user.barangay = etBarangay.text?.toString() ?: ""
        user.city = etCity.text?.toString() ?: ""
        user.province = etProvince.text?.toString() ?: ""
        user.zipCode = etPostalCode.text?.toString() ?: ""
        user.sssNumber = etSSSNumber.text?.toString() ?: ""
        user.gsisNumber = etGSISNumber.text?.toString() ?: ""
        user.oscaNumber = etOSCANumber.text?.toString() ?: ""
        user.philHealthNumber = etPhilHealthNumber.text?.toString() ?: ""

        // Emergency Contact Information
        val primaryContact = user.getPrimaryEmergencyContact()
        if (primaryContact != null) {
            primaryContact.name = etEmergencyContactName.text?.toString() ?: ""
            primaryContact.phoneNumber = etEmergencyContactPhone.text?.toString() ?: ""
            primaryContact.relationship = etRelationship.text?.toString() ?: ""
        } else {
            val newContact = EmergencyContact(
                name = etEmergencyContactName.text?.toString() ?: "",
                phoneNumber = etEmergencyContactPhone.text?.toString() ?: "",
                relationship = etRelationship.text?.toString() ?: "",
                isPrimary = true
            )
            user.emergencyContacts = listOf(newContact)
        }

        // Parse age
        val ageText = etAge.text?.toString()
        if (!ageText.isNullOrBlank()) {
            try {
                val age = ageText.toInt()
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val birthYear = currentYear - age
                val birthCalendar = java.util.Calendar.getInstance()
                birthCalendar.set(birthYear, 0, 1)
                user.birthDate = birthCalendar.time
                user.age = age
            } catch (e: NumberFormatException) {
                showError("Invalid age format")
                return
            }
        }

        // Mark this update as made by user (not admin)
        user.lastUpdatedBy = "user"
        user.updatedAt = com.google.firebase.Timestamp.now()

        // Save to repository
        lifecycleScope.launch {
            when (val result = userRepository.updateUser(user)) {
                is Result.Success -> {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    toggleEditMode(false)
                    displayUserProfile(user)
                }

                is Result.Error -> {
                    showError("Failed to save profile: ${result.exception.message}")
                }

                is Result.Loading<*> -> {
                    // Show loading indicator
                }
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val click: (Int) -> Unit = { which ->
            when (which) {
                0 -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                        openCamera()
                    } else {
                        cameraPermissionRequest.launch(Manifest.permission.CAMERA)
                    }
                }
                1 -> openGallery()
                else -> {}
            }
        }
        click(1)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imagePickerLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun loadProfileImage() {
        profileImageUri?.let { uri ->
            try {
                ivProfilePhoto.setImageURI(uri)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage() {
        profileImageUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    // Ensure Cloudinary is initialized (defensive in case app init was skipped)
                    if (!CloudinaryManager.isReady()) {
                        CloudinaryManager.initialize(this@ProfileActivity.applicationContext)
                    }

                    // Verify configuration before attempting upload
                    if (!CloudinaryManager.verifyConfiguration()) {
                        Toast.makeText(this@ProfileActivity, "Image service not properly configured", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    if (CloudinaryManager.isReady()) {
                        val imageUrl = CloudinaryManager.uploadImage(
                            uri,
                            "profile_${currentUser?.id}",
                            CloudinaryConfig.FOLDER
                        )

                        Log.d("ProfileActivity", "Image uploaded successfully: $imageUrl")

                        currentUser?.let { user ->
                            user.profileImageUrl = imageUrl

                            when (val result = userRepository.updateUser(user)) {
                                is Result.Success -> {
                                    Log.d("ProfileActivity", "User profile updated successfully")
                                    Toast.makeText(this@ProfileActivity, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                                    val optimizedUrl = ImageLoader.getOptimizedCloudinaryUrl(imageUrl, 400, 400)
                                    ImageLoader.loadProfileImage(
                                        this@ProfileActivity,
                                        ivProfilePhoto,
                                        optimizedUrl,
                                        R.drawable.ic_profile,
                                        R.drawable.ic_profile
                                    )
                                    try {
                                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateProfile(
                                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                .setPhotoUri(android.net.Uri.parse(imageUrl))
                                                .build()
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ProfileActivity", "Error updating Firebase Auth profile photo URL: ${e.message}")
                                    }
                                }
                                is Result.Error -> {
                                    Toast.makeText(this@ProfileActivity, "Failed to update profile: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                                }
                                is Result.Loading<*> -> {
                                    // Show loading state
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "Image service not available", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Image upload failed: ${e.message}", e)
                    val errorMessage = if (e.message?.contains("api_secret") == true) {
                        "Upload service configuration error. Please try again later."
                    } else {
                        "Failed to upload image: ${e.message}"
                    }
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        return try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes)
            val path = MediaStore.Images.Media.insertImage(
                contentResolver, bitmap, "Profile_${System.currentTimeMillis()}", null
            )
            Uri.parse(path)
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile image when returning to ProfileActivity
        currentUser?.let { user ->
            val firestoreUrl = user.profileImageUrl
            val authUrl = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()

            val finalUrl = when {
                !firestoreUrl.isNullOrEmpty() && firestoreUrl.startsWith("http") -> firestoreUrl
                !authUrl.isNullOrEmpty() && authUrl.startsWith("http") -> authUrl
                else -> null
            }

            if (finalUrl != null) {
                val optimizedUrl = if (ImageLoader.isValidCloudinaryUrl(finalUrl)) {
                    ImageLoader.getOptimizedCloudinaryUrl(finalUrl, 400, 400)
                } else {
                    finalUrl
                }

                ImageLoader.loadProfileImage(
                    this,
                    ivProfilePhoto,
                    optimizedUrl,
                    R.drawable.ic_profile,
                    R.drawable.ic_profile
                )
            } else {
                ivProfilePhoto.setImageResource(R.drawable.ic_profile)
            }
        }
    }

    override fun onBackPressed() {
        if (isEditMode) {
            toggleEditMode(false)
            currentUser?.let { displayUserProfile(it) }
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Show birthday date picker with senior-appropriate date range
     */
    private fun showBirthdayDatePicker() {
        val today = java.util.Calendar.getInstance()
        val sixtyYearsAgo = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.YEAR, -60)
        }

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.Theme_SeniorHub_DatePickerDialog,
            { _, y, m, d ->
                val picked = java.util.Calendar.getInstance().apply { set(y, m, d) }
                etBirthday.setText(dateFormat.format(picked.time))
                val age = calculateAge(picked.time)
                etAge.setText(age.toString())
            },
            sixtyYearsAgo.get(java.util.Calendar.YEAR), // Start from 60 years ago
            sixtyYearsAgo.get(java.util.Calendar.MONTH),
            sixtyYearsAgo.get(java.util.Calendar.DAY_OF_MONTH)
        )

        // Set date picker limits
        datePickerDialog.datePicker.maxDate = today.timeInMillis // Maximum: today
        datePickerDialog.datePicker.minDate = sixtyYearsAgo.timeInMillis // Minimum: 60 years ago

        // Apply custom colors for buttons
        datePickerDialog.setOnShowListener {
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.black, null))
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.black, null))
        }

        datePickerDialog.show()
    }

    /**
     * Calculate age from birth date
     */
    private fun calculateAge(birthDate: java.util.Date): Int {
        val today = java.util.Calendar.getInstance()
        val birth = java.util.Calendar.getInstance().apply { time = birthDate }
        var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
        return age
    }
}