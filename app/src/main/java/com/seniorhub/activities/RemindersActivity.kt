package com.seniorhub.activities

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.R
import com.seniorhub.adapters.RemindersAdapter
import com.google.firebase.auth.FirebaseAuth
import com.seniorhub.models.Reminder
import com.seniorhub.models.ReminderPriority
import com.seniorhub.models.ReminderType
import com.seniorhub.models.RecurrencePattern
import com.seniorhub.receivers.ReminderReceiver
import com.seniorhub.services.ReminderService
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

/**
 * RemindersActivity - Comprehensive Reminder Management
 *
 * Features:
 * - Add, edit, delete reminders
 * - Set custom repeat patterns (minute, hour, daily, weekly, monthly, yearly, weekdays)
 * - Choose between notification and alarm alerts
 * - Toggle reminders on/off
 * - Visual priority indicators
 * - Senior-friendly interface
 */
class RemindersActivity : AppCompatActivity() {
    private lateinit var recyclerViewReminders: RecyclerView
    private lateinit var remindersAdapter: RemindersAdapter
    private lateinit var remindersRef: DatabaseReference
    private lateinit var alarmManager: AlarmManager

    private val reminders = mutableListOf<Reminder>()
    private var currentUserId: String = ""
    
    companion object {
        private const val REQUEST_SCHEDULE_EXACT_ALARM = 1001
    }
    
    // Date and time formatters
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    // Selected date and time for each form
    private var selectedMedicineDate: Calendar? = null
    private var selectedMedicineTime: Calendar? = null
    private var selectedAppointmentDate: Calendar? = null
    private var selectedAppointmentTime: Calendar? = null
    private var selectedBirthdayDate: Calendar? = null
    private var selectedBirthdayTime: Calendar? = null
    private var selectedExerciseDate: Calendar? = null
    private var selectedExerciseTime: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        // Initialize Firebase database and get current user
        currentUserId = FirebaseManager.getCurrentUserId() ?: ""
        
        // Check if user is authenticated
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to access reminders", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // Use senior name for better navigation in Realtime Database
        val seniorName = getSeniorName(currentUserId)
        remindersRef = FirebaseDatabase.getInstance().getReference("reminders").child(seniorName)

        setupViews()
        setupRecyclerView()
        setupClickListeners()
        setupSpinners()
        setupReminderFormListeners()
        setupAlarmManager()
        checkAlarmPermissions()
        loadReminders()
        
        // Add a test reminder for debugging
        addTestReminder()
    }

    private fun setupViews() {
        recyclerViewReminders = findViewById(R.id.recyclerViewReminders)
    }
    
    private fun setupAlarmManager() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    /**
     * Check if the app has permission to schedule exact alarms
     */
    private fun checkAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
            }
        }
    }
    
    /**
     * Request permission to schedule exact alarms
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivityForResult(intent, REQUEST_SCHEDULE_EXACT_ALARM)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCHEDULE_EXACT_ALARM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(this, "Exact alarm permission granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Exact alarm permission denied. Alarms may be less precise.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(
            context = this,
            reminders = reminders,
            onReminderClick = { reminder -> editReminder(reminder) },
            onReminderToggle = { reminder, isActive -> toggleReminder(reminder, isActive) },
            onReminderLongClick = { reminder -> showDeleteDialog(reminder) }
        )
        
        recyclerViewReminders.apply {
            layoutManager = LinearLayoutManager(this@RemindersActivity)
            adapter = remindersAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
        
        android.util.Log.d("RemindersActivity", "RecyclerView setup completed")
    }

    private fun setupClickListeners() {
        // Back button functionality
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupSpinners() {
        // Medicine frequency spinner
        val medicineFrequencySpinner = findViewById<Spinner>(R.id.spinnerMedicineFrequency)
        val medicineFrequencyOptions = arrayOf(
            "Once Daily", "Twice Daily", "Three Times Daily",
            "Every 4 Hours", "Every 6 Hours", "Every 8 Hours",
            "Before Meals", "After Meals", "As Needed"
        )
        val medicineAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicineFrequencyOptions)
        medicineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        medicineFrequencySpinner?.adapter = medicineAdapter
        
        // Birthday relationship spinner
        val birthdayRelationshipSpinner = findViewById<Spinner>(R.id.spinnerBirthdayRelationship)
        val relationshipOptions = arrayOf(
            "Son", "Daughter", "Grandchild", "Spouse",
            "Siblings", "Relatives", "Acquaintance",
            "Colleague", "Other"
        )
        val relationshipAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, relationshipOptions)
        relationshipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        birthdayRelationshipSpinner?.adapter = relationshipAdapter
        
        // Exercise activity spinner
        val exerciseActivitySpinner = findViewById<Spinner>(R.id.spinnerExerciseActivity)
        val exerciseOptions = arrayOf(
            "Walking", "Jogging", "Swimming", "Cycling", 
            "Yoga", "Tai Chi", "Strength Training", "Stretching",
            "Dancing", "Gardening", "Housework", "Other"
        )
        val exerciseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exerciseOptions)
        exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        exerciseActivitySpinner?.adapter = exerciseAdapter
    }

    private fun loadReminders() {
        lifecycleScope.launch {
            try {
                // Check if user is authenticated
                if (currentUserId.isEmpty()) {
                    Toast.makeText(this@RemindersActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Load reminders from Firebase
                remindersRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        android.util.Log.d("RemindersActivity", "onDataChange called with ${snapshot.childrenCount} children")
                        reminders.clear()
                        for (childSnapshot in snapshot.children) {
                            try {
                                val reminder = childSnapshot.getValue(Reminder::class.java)
                                reminder?.let {
                                    reminders.add(it)
                                    android.util.Log.d("RemindersActivity", "Added reminder: ${it.title}")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("RemindersActivity", "Error parsing reminder: ${e.message}")
                            }
                        }
                        android.util.Log.d("RemindersActivity", "About to update adapter with ${reminders.size} reminders")
                        remindersAdapter.updateReminders(reminders)
                        updateEmptyState()
                        android.util.Log.d("RemindersActivity", "Loaded ${reminders.size} reminders from Firebase")
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        android.util.Log.e("RemindersActivity", "Error loading reminders: ${error.message}")
                        when (error.code) {
                            com.google.firebase.database.DatabaseError.PERMISSION_DENIED -> {
                                Toast.makeText(this@RemindersActivity, "Permission denied. Please check your Firebase rules.", Toast.LENGTH_LONG).show()
                            }
                            com.google.firebase.database.DatabaseError.NETWORK_ERROR -> {
                                Toast.makeText(this@RemindersActivity, "Network error. Please check your internet connection.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@RemindersActivity, "Error loading reminders: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Toast.makeText(this@RemindersActivity, "Error loading reminders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun editReminder(reminder: Reminder) {
        // TODO: Implement edit reminder functionality
        Toast.makeText(this, "Edit reminder: ${reminder.title}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleReminder(reminder: Reminder, isActive: Boolean) {
        lifecycleScope.launch {
            try {
                // Update reminder in Firebase
                val updatedReminder = reminder.copy(isActive = isActive)
                remindersRef.child(reminder.id).setValue(updatedReminder)
                    .addOnSuccessListener {
                        // Update the local list and adapter
                        val index = reminders.indexOfFirst { it.id == reminder.id }
                        if (index != -1) {
                            reminders[index] = updatedReminder
                            // Use post to ensure RecyclerView is not in layout pass
                            recyclerViewReminders.post {
                                remindersAdapter.updateReminder(updatedReminder)
                            }
                        }
                        
                        if (isActive) {
                            scheduleReminder(updatedReminder)
                            Toast.makeText(this@RemindersActivity, "Reminder Activated", Toast.LENGTH_SHORT).show()
                        } else {
                            cancelReminder(reminder.id)
                            Toast.makeText(this@RemindersActivity, "Reminder Deactivated", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@RemindersActivity, "Error updating reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@RemindersActivity, "Error updating reminder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog(reminder: Reminder) {
        AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage(getString(R.string.confirm_delete_reminder))
            .setPositiveButton("Delete") { _, _ ->
                deleteReminder(reminder)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReminder(reminder: Reminder) {
        lifecycleScope.launch {
            try {
                // Remove from Firebase
                remindersRef.child(reminder.id).removeValue()
                    .addOnSuccessListener {
                        cancelReminder(reminder.id)
                        remindersAdapter.removeReminder(reminder.id)
                        updateEmptyState()
                        Toast.makeText(this@RemindersActivity, "Reminder deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@RemindersActivity, "Error deleting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@RemindersActivity, "Error deleting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleReminder(reminder: Reminder) {
        android.util.Log.d("RemindersActivity", "Scheduling alarm for reminder: ${reminder.id} at ${java.util.Date(reminder.scheduledTime)}")
        
        val reminderIntent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(ReminderService.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderService.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderService.EXTRA_REMINDER_MESSAGE, getAlarmMessage(reminder))
            putExtra(ReminderService.EXTRA_REMINDER_TYPE, reminder.type.name.lowercase())
            putExtra(ReminderService.EXTRA_REMINDER_IS_ALARM, reminder.soundEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (reminder.isRecurring) {
                scheduleRecurringAlarm(reminder, pendingIntent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.scheduledTime,
                            pendingIntent
                        )
                        android.util.Log.d("RemindersActivity", "Exact alarm scheduled: ${reminder.id}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.scheduledTime,
                            pendingIntent
                        )
                        android.util.Log.w("RemindersActivity", "Exact alarms not allowed, using inexact alarm: ${reminder.id}")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.scheduledTime,
                        pendingIntent
                    )
                    android.util.Log.d("RemindersActivity", "Exact alarm scheduled (legacy): ${reminder.id}")
                }
            }
            
            android.util.Log.d("RemindersActivity", "Alarm scheduled: ${reminder.id} at ${java.util.Date(reminder.scheduledTime)}")
        } catch (e: SecurityException) {
            android.util.Log.e("RemindersActivity", "SecurityException when scheduling alarm: ${e.message}", e)
            Toast.makeText(this, "Cannot schedule alarm: Permission denied. Please grant exact alarm permission.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("RemindersActivity", "Failed to schedule alarm: ${e.message}", e)
            Toast.makeText(this, "Failed to schedule alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminder(reminderId: String) {
        val reminderIntent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        android.util.Log.d("RemindersActivity", "Alarm cancelled: $reminderId")
    }

    private fun setupReminderFormListeners() {
        // Medicine form listeners
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMedicineReminder)?.setOnClickListener {
            showReminderForm("medicine")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAppointmentReminder)?.setOnClickListener {
            showReminderForm("appointment")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBirthdayReminder)?.setOnClickListener {
            showReminderForm("birthday")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExerciseReminder)?.setOnClickListener {
            showReminderForm("exercise")
        }
        
        // Date and time picker listeners
        setupDateTimePickers()
        
        // Form save and cancel buttons
        setupFormButtons()
    }
    
    private fun setupDateTimePickers() {
        // Medicine date and time pickers
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineDate)?.setOnClickListener {
            showDatePicker { calendar ->
                selectedMedicineDate = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineDate)?.text = "📅 ${dateFormat.format(calendar.time)}"
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineTime)?.setOnClickListener {
            showTimePicker { calendar ->
                selectedMedicineTime = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineTime)?.text = "🕐 ${timeFormat.format(calendar.time)}"
            }
        }
        
        // Appointment date and time pickers
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentDate)?.setOnClickListener {
            showDatePicker { calendar ->
                selectedAppointmentDate = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentDate)?.text = "📅 ${dateFormat.format(calendar.time)}"
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentTime)?.setOnClickListener {
            showTimePicker { calendar ->
                selectedAppointmentTime = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentTime)?.text = "🕐 ${timeFormat.format(calendar.time)}"
            }
        }
        
        // Birthday date and time pickers
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayDate)?.setOnClickListener {
            showDatePicker { calendar ->
                selectedBirthdayDate = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayDate)?.text = "📅 ${dateFormat.format(calendar.time)}"
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayTime)?.setOnClickListener {
            showTimePicker { calendar ->
                selectedBirthdayTime = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayTime)?.text = "🕐 ${timeFormat.format(calendar.time)}"
            }
        }
        
        // Exercise date and time pickers
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseDate)?.setOnClickListener {
            showDatePicker { calendar ->
                selectedExerciseDate = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseDate)?.text = "📅 ${dateFormat.format(calendar.time)}"
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseTime)?.setOnClickListener {
            showTimePicker { calendar ->
                selectedExerciseTime = calendar
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseTime)?.text = "🕐 ${timeFormat.format(calendar.time)}"
            }
        }
    }
    
    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerDialogTheme,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        
        // Apply custom styling
        datePickerDialog.datePicker.setBackgroundColor(getColor(R.color.white))
        datePickerDialog.datePicker.setCalendarViewShown(false)
        
        datePickerDialog.show()
    }
    
    private fun showTimePicker(onTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            R.style.TimePickerDialogTheme,
            { _, hourOfDay, minute ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                onTimeSelected(selectedCalendar)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // Use 12-hour format
        )
        
        // Apply custom styling
        timePickerDialog.window?.setBackgroundDrawableResource(R.color.white)
        
        timePickerDialog.show()
    }
    
    private fun setupFormButtons() {
        // Medicine form buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveMedicine)?.setOnClickListener {
            saveMedicineReminder()
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelMedicine)?.setOnClickListener {
            hideAllReminderForms()
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.VISIBLE
        }
        
        // Appointment form buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveAppointment)?.setOnClickListener {
            saveAppointmentReminder()
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelAppointment)?.setOnClickListener {
            hideAllReminderForms()
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.VISIBLE
        }
        
        // Birthday form buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveBirthday)?.setOnClickListener {
            saveBirthdayReminder()
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelBirthday)?.setOnClickListener {
            hideAllReminderForms()
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.VISIBLE
        }
        
        // Exercise form buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveExercise)?.setOnClickListener {
            saveExerciseReminder()
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelExercise)?.setOnClickListener {
            hideAllReminderForms()
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun showReminderForm(reminderType: String) {
        // Hide all forms first
        hideAllReminderForms()
        
        // Show the selected form
        when (reminderType) {
            "medicine" -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMedicineReminder)?.visibility = android.view.View.VISIBLE
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.GONE
            }
            "appointment" -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAppointmentReminder)?.visibility = android.view.View.VISIBLE
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.GONE
            }
            "birthday" -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardBirthdayReminder)?.visibility = android.view.View.VISIBLE
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.GONE
            }
            "exercise" -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardExerciseReminder)?.visibility = android.view.View.VISIBLE
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun hideAllReminderForms() {
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMedicineReminder)?.visibility = android.view.View.GONE
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAppointmentReminder)?.visibility = android.view.View.GONE
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardBirthdayReminder)?.visibility = android.view.View.GONE
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardExerciseReminder)?.visibility = android.view.View.GONE
    }
    
    private fun saveMedicineReminder() {
        val medicineName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etMedicineName)?.text?.toString() ?: ""
        val frequency = findViewById<android.widget.Spinner>(R.id.spinnerMedicineFrequency)?.selectedItem?.toString() ?: ""
        
        if (medicineName.isBlank()) {
            Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedMedicineDate == null || selectedMedicineTime == null) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Combine date and time
        val scheduledDateTime = Calendar.getInstance().apply {
            timeInMillis = selectedMedicineDate!!.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedMedicineTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedMedicineTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        
        val reminder = Reminder(
            id = java.util.UUID.randomUUID().toString(),
            userId = currentUserId,
            title = "Take $medicineName ($frequency)",
            description = "Medicine reminder: $medicineName",
            type = ReminderType.MEDICATION,
            priority = ReminderPriority.HIGH,
            scheduledTime = scheduledDateTime.timeInMillis,
            isRecurring = true,
            recurrencePattern = com.seniorhub.models.RecurrencePattern.DAILY,
            isActive = true,
            reminderData = mapOf(
                "medicineName" to medicineName,
                "frequency" to frequency
            )
        )
        
        saveReminder(reminder)
    }
    
    private fun saveAppointmentReminder() {
        val doctorName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDoctorName)?.text?.toString() ?: ""
        
        if (doctorName.isBlank()) {
            Toast.makeText(this, "Please enter doctor/service name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedAppointmentDate == null || selectedAppointmentTime == null) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Combine date and time
        val scheduledDateTime = Calendar.getInstance().apply {
            timeInMillis = selectedAppointmentDate!!.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedAppointmentTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedAppointmentTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        
        val reminder = Reminder(
            id = java.util.UUID.randomUUID().toString(),
            userId = currentUserId,
            title = "Appointment with $doctorName",
            description = "Medical appointment reminder",
            type = ReminderType.APPOINTMENT,
            priority = ReminderPriority.MEDIUM,
            scheduledTime = scheduledDateTime.timeInMillis,
            isRecurring = false,
            isActive = true,
            reminderData = mapOf(
                "doctorName" to doctorName
            )
        )
        
        saveReminder(reminder)
    }
    
    private fun saveBirthdayReminder() {
        val personName = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPersonName)?.text?.toString() ?: ""
        val relationship = findViewById<android.widget.Spinner>(R.id.spinnerBirthdayRelationship)?.selectedItem?.toString() ?: ""
        
        if (personName.isBlank()) {
            Toast.makeText(this, "Please enter person's name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedBirthdayDate == null || selectedBirthdayTime == null) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Combine date and time
        val scheduledDateTime = Calendar.getInstance().apply {
            timeInMillis = selectedBirthdayDate!!.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedBirthdayTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedBirthdayTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        
        val reminder = Reminder(
            id = java.util.UUID.randomUUID().toString(),
            userId = currentUserId,
            title = "It's $personName's ($relationship) birthday today!",
            description = "Birthday reminder for $personName",
            type = ReminderType.OTHER,
            priority = ReminderPriority.MEDIUM,
            scheduledTime = scheduledDateTime.timeInMillis,
            isRecurring = true,
            recurrencePattern = com.seniorhub.models.RecurrencePattern.YEARLY,
            isActive = true,
            reminderData = mapOf(
                "personName" to personName,
                "relationship" to relationship
            )
        )
        
        saveReminder(reminder)
    }
    
    private fun saveExerciseReminder() {
        val activity = findViewById<android.widget.Spinner>(R.id.spinnerExerciseActivity)?.selectedItem?.toString() ?: ""
        val duration = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExerciseDuration)?.text?.toString() ?: ""
        
        if (activity.isBlank()) {
            Toast.makeText(this, "Please select an activity", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedExerciseDate == null || selectedExerciseTime == null) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Combine date and time
        val scheduledDateTime = Calendar.getInstance().apply {
            timeInMillis = selectedExerciseDate!!.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedExerciseTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedExerciseTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        
        val reminder = Reminder(
            id = java.util.UUID.randomUUID().toString(),
            userId = currentUserId,
            title = "Time for $activity - $duration",
            description = "Exercise reminder: $activity",
            type = ReminderType.EXERCISE,
            priority = ReminderPriority.MEDIUM,
            scheduledTime = scheduledDateTime.timeInMillis,
            isRecurring = true,
            recurrencePattern = com.seniorhub.models.RecurrencePattern.DAILY,
            isActive = true,
            reminderData = mapOf(
                "activity" to activity,
                "duration" to duration
            )
        )
        
        saveReminder(reminder)
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
            android.util.Log.e("RemindersActivity", "Error getting senior name: ${e.message}")
            "Unknown Senior"
        }
    }

    private fun saveReminder(reminder: Reminder) {
        lifecycleScope.launch {
            try {
                // Check if user is authenticated
                if (currentUserId.isEmpty()) {
                    Toast.makeText(this@RemindersActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                android.util.Log.d("RemindersActivity", "Saving reminder: ${reminder.id} with title: ${reminder.title}")
                
                // Add senior name to the reminder for easier identification
                val seniorName = getSeniorName(currentUserId)
                val reminderWithName = reminder.copy(seniorUserName = seniorName)
                
                // Save to Firebase
                remindersRef.child(reminder.id).setValue(reminderWithName)
                    .addOnSuccessListener {
                        android.util.Log.d("RemindersActivity", "Reminder saved to Firebase successfully: ${reminder.id}")
                        scheduleReminder(reminder)
                        
                        // Hide forms and show type selection
                        hideAllReminderForms()
                        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminderTypeSelection)?.visibility = android.view.View.VISIBLE
                        
                        // Clear selected date/time for next use
                        clearSelectedDateTime()
                        
                        Toast.makeText(this@RemindersActivity, "Reminder saved successfully!", Toast.LENGTH_SHORT).show()
                        
                        android.util.Log.d("RemindersActivity", "Reminder saved to Firebase: ${reminder.id}")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("RemindersActivity", "Error saving reminder: ${e.message}")
                        when {
                            e.message?.contains("Permission denied") == true -> {
                                Toast.makeText(this@RemindersActivity, "Permission denied. Please check your Firebase rules.", Toast.LENGTH_LONG).show()
                            }
                            e.message?.contains("Network") == true -> {
                                Toast.makeText(this@RemindersActivity, "Network error. Please check your internet connection.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@RemindersActivity, "Error saving reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this@RemindersActivity, "Error saving reminder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun clearSelectedDateTime() {
        selectedMedicineDate = null
        selectedMedicineTime = null
        selectedAppointmentDate = null
        selectedAppointmentTime = null
        selectedBirthdayDate = null
        selectedBirthdayTime = null
        selectedExerciseDate = null
        selectedExerciseTime = null
        
        // Reset button texts
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineDate)?.text = "📅 Select Date"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectMedicineTime)?.text = "🕐 Select Time"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentDate)?.text = "📅 Select Date"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAppointmentTime)?.text = "🕐 Select Time"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayDate)?.text = "📅 Select Date"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectBirthdayTime)?.text = "🕐 Select Time"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseDate)?.text = "📅 Select Date"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectExerciseTime)?.text = "🕐 Select Time"
    }
    
    private fun updateEmptyState() {
        // Empty state handling removed - will be handled by RecyclerView adapter
        remindersAdapter.notifyDataSetChanged()
    }
    
    /**
     * Schedule recurring alarm based on reminder pattern
     */
    private fun scheduleRecurringAlarm(reminder: Reminder, pendingIntent: PendingIntent) {
        when (reminder.recurrencePattern) {
            com.seniorhub.models.RecurrencePattern.DAILY -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            com.seniorhub.models.RecurrencePattern.WEEKLY -> {
                if (reminder.recurrenceDays.isNotEmpty()) {
                    scheduleWeeklyAlarm(reminder, pendingIntent)
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        reminder.scheduledTime,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }
            }
            com.seniorhub.models.RecurrencePattern.MONTHLY -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledTime,
                    AlarmManager.INTERVAL_DAY * 30,
                    pendingIntent
                )
            }
            com.seniorhub.models.RecurrencePattern.YEARLY -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledTime,
                    AlarmManager.INTERVAL_DAY * 365,
                    pendingIntent
                )
            }
            else -> {
                // No repeat - schedule as one-time
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.scheduledTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.scheduledTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.scheduledTime,
                        pendingIntent
                    )
                }
            }
        }
    }
    
    /**
     * Schedule weekly alarm for specific days
     */
    private fun scheduleWeeklyAlarm(reminder: Reminder, pendingIntent: PendingIntent) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = reminder.scheduledTime
        }
        
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val nextDay = reminder.recurrenceDays.find { it >= currentDayOfWeek } ?: reminder.recurrenceDays.first()
        
        // Calculate next occurrence
        val daysUntilNext = if (nextDay >= currentDayOfWeek) {
            nextDay - currentDayOfWeek
        } else {
            (7 - currentDayOfWeek) + nextDay
        }
        
        val nextTime = reminder.scheduledTime + (daysUntilNext * 24 * 60 * 60 * 1000L)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
        }
    }
    


    private fun getAlarmMessage(reminder: Reminder): String {
        return when (reminder.type) {
            ReminderType.MEDICATION -> "💊 Time to Take Your Medication: ${reminder.title}"
            ReminderType.APPOINTMENT -> "📅 You Have an Appointment: ${reminder.title}"
            ReminderType.EXERCISE -> "🏃 Time for Exercise: ${reminder.title}"
            else -> "🎂 ${reminder.title}"
        }
    }
    
    /**
     * Add a test reminder for debugging purposes
     */
    private fun addTestReminder() {
        val testReminder = Reminder(
            id = "test_reminder_${System.currentTimeMillis()}",
            userId = currentUserId,
            title = "Test Reminder",
            description = "This is a test reminder to check if RecyclerView works",
            type = ReminderType.MEDICATION,
            priority = ReminderPriority.HIGH,
            scheduledTime = System.currentTimeMillis() + (5 * 60 * 1000), // 5 minutes from now
            isRecurring = false,
            isActive = true
        )
        
        android.util.Log.d("RemindersActivity", "Adding test reminder: ${testReminder.id}")
        reminders.add(testReminder)
        remindersAdapter.updateReminders(reminders)
        android.util.Log.d("RemindersActivity", "Test reminder added, adapter now has ${reminders.size} items")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}