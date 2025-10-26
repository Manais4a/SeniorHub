package com.seniorhub.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.switchmaterial.SwitchMaterial
import com.seniorhub.R
import com.seniorhub.models.Reminder
import com.seniorhub.models.ReminderPriority
import com.seniorhub.models.ReminderType
import com.seniorhub.models.RecurrencePattern
import java.text.SimpleDateFormat
import java.util.*

/**
 * RemindersAdapter - RecyclerView adapter for displaying reminders
 * 
 * Features:
 * - Displays reminder title, description, time, and repeat pattern
 * - Toggle reminder on/off
 * - Click to edit reminder
 * - Long click to delete reminder
 * - Visual indicators for priority and type
 */
class RemindersAdapter(
    private val context: Context,
    private var reminders: MutableList<Reminder> = mutableListOf(),
    private val onReminderClick: (Reminder) -> Unit = {},
    private val onReminderToggle: (Reminder, Boolean) -> Unit = { _, _ -> },
    private val onReminderLongClick: (Reminder) -> Unit = {}
) : RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder>() {

    companion object {
        private const val TAG = "RemindersAdapter"
    }

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardReminder = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardReminder)
        val imageIcon = itemView.findViewById<ImageView>(R.id.imageIcon)
        val textTitle = itemView.findViewById<TextView>(R.id.textTitle)
        val textDescription = itemView.findViewById<TextView>(R.id.textDescription)
        val switchReminder = itemView.findViewById<SwitchMaterial>(R.id.switchReminder)
        val textTime = itemView.findViewById<TextView>(R.id.textTime)
        val textDate = itemView.findViewById<TextView>(R.id.textDate)
        val layoutTimeAndDays = itemView.findViewById<View>(R.id.layoutTimeAndDays)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        
        // Set reminder icon based on type
        holder.imageIcon.setImageResource(getReminderIcon(reminder.type))
        
        // Set reminder content
        holder.textTitle.text = reminder.title
        holder.textDescription.text = reminder.description
        
        // Set status indicator
        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val textStatus = holder.itemView.findViewById<TextView>(R.id.textStatus)
        
        if (reminder.isActive) {
            statusIndicator.setBackgroundResource(R.drawable.status_indicator)
            textStatus.text = "Active"
            textStatus.setTextColor(context.getColor(R.color.success_green))
        } else {
            statusIndicator.setBackgroundColor(context.getColor(R.color.medium_gray))
            textStatus.text = "Inactive"
            textStatus.setTextColor(context.getColor(R.color.medium_gray))
        }
        
        // Set reminder time
        holder.textTime.text = formatReminderTime(reminder.scheduledTime)
        
        // Set reminder date
        holder.textDate.text = formatReminderDate(reminder.scheduledTime)
        
        // Set toggle state
        holder.switchReminder.isChecked = reminder.isActive
        
        // Set priority color
        setPriorityColor(holder, reminder.priority)
        
        // Set click listeners
        holder.cardReminder.setOnClickListener {
            onReminderClick(reminder)
        }
        
        holder.cardReminder.setOnLongClickListener {
            onReminderLongClick(reminder)
            true
        }
        
        holder.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            onReminderToggle(reminder, isChecked)
        }
    }

    override fun getItemCount(): Int = reminders.size

    /**
     * Update the reminders list
     */
    fun updateReminders(newReminders: List<Reminder>) {
        android.util.Log.d("RemindersAdapter", "updateReminders called with ${newReminders.size} items")
        reminders.clear()
        reminders.addAll(newReminders)
        android.util.Log.d("RemindersAdapter", "Adapter now has ${reminders.size} items")
        notifyDataSetChanged()
    }

    /**
     * Add a new reminder
     */
    fun addReminder(reminder: Reminder) {
        reminders.add(reminder)
        notifyItemInserted(reminders.size - 1)
    }

    /**
     * Update an existing reminder
     */
    fun updateReminder(reminder: Reminder) {
        val index = reminders.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            reminders[index] = reminder
            notifyItemChanged(index)
        }
    }

    /**
     * Remove a reminder
     */
    fun removeReminder(reminderId: String) {
        val index = reminders.indexOfFirst { it.id == reminderId }
        if (index != -1) {
            reminders.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * Get reminder icon based on type
     */
    private fun getReminderIcon(type: ReminderType): Int {
        return when (type) {
            ReminderType.MEDICATION -> R.drawable.ic_medication
            ReminderType.APPOINTMENT -> R.drawable.ic_calendar
            ReminderType.EXERCISE -> R.drawable.ic_fitness
            ReminderType.MEAL -> R.drawable.ic_meal
            ReminderType.HYDRATION -> R.drawable.ic_water
            ReminderType.BLOOD_PRESSURE_CHECK -> R.drawable.ic_heart
            ReminderType.BLOOD_SUGAR_CHECK -> R.drawable.ic_blood_sugar
            ReminderType.WEIGHT_CHECK -> R.drawable.ic_weight
            ReminderType.VITAMIN -> R.drawable.ic_vitamin
            ReminderType.DOCTOR_VISIT -> R.drawable.ic_doctor
            ReminderType.PHARMACY_PICKUP -> R.drawable.ic_pharmacy
            ReminderType.BILL_PAYMENT -> R.drawable.ic_bill
            ReminderType.SOCIAL_ACTIVITY -> R.drawable.ic_social
            ReminderType.FAMILY_CALL -> R.drawable.ic_phone
            ReminderType.FRIEND_VISIT -> R.drawable.ic_friend
            ReminderType.RELIGIOUS_SERVICE -> R.drawable.ic_church
            ReminderType.COMMUNITY_EVENT -> R.drawable.ic_event
            ReminderType.BENEFIT_CLAIM -> R.drawable.ic_benefit
            ReminderType.ID_RENEWAL -> R.drawable.ic_id
            ReminderType.VACCINATION -> R.drawable.ic_vaccine
            ReminderType.HEALTH_CHECKUP -> R.drawable.ic_health_check
            ReminderType.EYE_EXAM -> R.drawable.ic_eye
            ReminderType.DENTAL_CHECKUP -> R.drawable.ic_dental
            ReminderType.HEARING_TEST -> R.drawable.ic_hearing
            ReminderType.PHYSICAL_THERAPY -> R.drawable.ic_physical_therapy
            ReminderType.OCCUPATIONAL_THERAPY -> R.drawable.ic_occupational_therapy
            ReminderType.COUNSELING -> R.drawable.ic_counseling
            ReminderType.SUPPORT_GROUP -> R.drawable.ic_support_group
            ReminderType.VOLUNTEER_WORK -> R.drawable.ic_volunteer
            ReminderType.HOBBY_TIME -> R.drawable.ic_hobby
            ReminderType.READING_TIME -> R.drawable.ic_reading
            ReminderType.GARDENING -> R.drawable.ic_gardening
            ReminderType.WALKING -> R.drawable.ic_walking
            else -> R.drawable.ic_reminder
        }
    }

    /**
     * Format reminder time for display
     */
    private fun formatReminderTime(timeInMillis: Long): String {
        val now = System.currentTimeMillis()
        val timeDiff = timeInMillis - now
        
        return when {
            timeDiff < 0 -> {
                // Past time
                val daysAgo = (-timeDiff / (24 * 60 * 60 * 1000)).toInt()
                when {
                    daysAgo == 0 -> context.getString(R.string.today)
                    daysAgo == 1 -> context.getString(R.string.yesterday)
                    daysAgo < 7 -> context.getString(R.string.days_ago, daysAgo)
                    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timeInMillis))
                }
            }
            timeDiff < 24 * 60 * 60 * 1000 -> {
                // Today
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeInMillis))
            }
            timeDiff < 7 * 24 * 60 * 60 * 1000 -> {
                // This week
                SimpleDateFormat("EEEE HH:mm", Locale.getDefault()).format(Date(timeInMillis))
            }
            else -> {
                // Future date
                SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date(timeInMillis))
            }
        }
    }

    /**
     * Format reminder date for display
     */
    private fun formatReminderDate(timeInMillis: Long): String {
        val now = System.currentTimeMillis()
        val timeDiff = timeInMillis - now
        
        return when {
            timeDiff < 0 -> {
                // Past date
                val daysAgo = (-timeDiff / (24 * 60 * 60 * 1000)).toInt()
                when {
                    daysAgo == 0 -> "Today"
                    daysAgo == 1 -> "Yesterday"
                    daysAgo < 7 -> "$daysAgo days ago"
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timeInMillis))
                }
            }
            timeDiff < 24 * 60 * 60 * 1000 -> {
                // Today
                "Today"
            }
            timeDiff < 7 * 24 * 60 * 60 * 1000 -> {
                // This week
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timeInMillis))
            }
            timeDiff < 30 * 24 * 60 * 60 * 1000 -> {
                // This month
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timeInMillis))
            }
            else -> {
                // Future date
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timeInMillis))
            }
        }
    }


    /**
     * Set priority color for the reminder card
     */
    private fun setPriorityColor(holder: ReminderViewHolder, priority: ReminderPriority) {
        val colorRes = when (priority) {
            ReminderPriority.CRITICAL -> R.color.critical_priority
            ReminderPriority.HIGH -> R.color.high_priority
            ReminderPriority.MEDIUM -> R.color.medium_priority
            ReminderPriority.LOW -> R.color.low_priority
        }
        
        // Set border color or background color based on priority
        holder.cardReminder.setCardBackgroundColor(context.getColor(colorRes))
    }
}
