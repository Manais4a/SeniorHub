package com.seniorhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seniorhub.R
import com.seniorhub.models.HealthRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * HealthLogAdapter - RecyclerView adapter for displaying health log records
 * with dynamic colors and icons that match the Health Metrics Grid design
 *
 * Features:
 * - Dynamic color theming based on health record type
 * - Matching icons for each health metric
 * - Senior-friendly UI with large text and clear formatting
 */
class HealthLogAdapter(
    private val onItemClick: (HealthRecord) -> Unit
) : RecyclerView.Adapter<HealthLogAdapter.HealthLogViewHolder>() {

    private var healthRecords = mutableListOf<HealthRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_log, parent, false)
        return HealthLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: HealthLogViewHolder, position: Int) {
        holder.bind(healthRecords[position])
    }

    override fun getItemCount(): Int = healthRecords.size

    /**
     * Update the list of health records with efficient change notifications
     * Ensures data persistence and proper display - records never disappear
     */
    fun updateRecords(newRecords: List<HealthRecord>?) {
        val oldSize = healthRecords.size
        val newRecordsList = newRecords ?: emptyList()
        
        // Merge new records with existing ones to ensure persistence
        val existingIds = healthRecords.map { it.id }.toSet()
        val newRecordsToAdd = newRecordsList.filter { it.id !in existingIds }
        
        // Add new records to the list
        healthRecords.addAll(newRecordsToAdd)
        
        // Sort records by timestamp (newest first) to ensure proper display order
        healthRecords.sortByDescending { it.timestamp }

        // Use more specific change notifications for better performance
        if (oldSize > 0 && healthRecords.isEmpty()) {
            notifyItemRangeRemoved(0, oldSize)
        } else if (healthRecords.isEmpty()) {
            // No items to notify about
        } else if (oldSize == 0) {
            notifyItemRangeInserted(0, healthRecords.size)
        } else if (newRecordsToAdd.isNotEmpty()) {
            // Only notify about new items added
            notifyItemRangeInserted(0, newRecordsToAdd.size)
        } else {
            // For simplicity, use notifyDataSetChanged when sizes are different
            // In a real app, you might want to implement DiffUtil for better performance
            notifyDataSetChanged()
        }
    }

    /**
     * Add a single record with efficient notification
     * Maintains sorted order by timestamp and ensures no duplicates
     */
    fun addRecord(healthRecord: HealthRecord) {
        // Check if record already exists to avoid duplicates
        val existingRecord = healthRecords.find { it.id == healthRecord.id }
        if (existingRecord != null) {
            // Update existing record
            val position = healthRecords.indexOf(existingRecord)
            healthRecords[position] = healthRecord
            notifyItemChanged(position)
            return
        }
        
        // Find the correct position to insert based on timestamp
        val insertPosition = healthRecords.indexOfFirst { it.timestamp < healthRecord.timestamp }
        val position = if (insertPosition == -1) healthRecords.size else insertPosition
        
        healthRecords.add(position, healthRecord)
        notifyItemInserted(position)
    }

    /**
     * Remove a record with efficient notification
     */
    fun removeRecord(position: Int) {
        if (position in 0 until healthRecords.size) {
            healthRecords.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class HealthLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHealthLogTitle: TextView = itemView.findViewById(R.id.tvHealthLogTitle)
        private val tvHealthLogValue: TextView = itemView.findViewById(R.id.tvHealthLogValue)
        private val tvHealthLogNotes: TextView = itemView.findViewById(R.id.tvHealthLogNotes)
        private val tvHealthLogTime: TextView = itemView.findViewById(R.id.tvHealthLogTime)
        private val ivHealthLogIcon: ImageView = itemView.findViewById(R.id.ivHealthLogIcon)
        private val btnViewHealthLogDetails: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnViewHealthLogDetails)
        private val cardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(healthRecord: HealthRecord) {
            // Set health record data
            tvHealthLogTitle.text = healthRecord.getTypeDisplay()
            tvHealthLogValue.text = formatHealthValue(healthRecord)
            tvHealthLogNotes.text = healthRecord.notes.ifEmpty { "No notes" }
            tvHealthLogTime.text = dateFormat.format(healthRecord.timestamp)

            // Set dynamic colors and icons based on health record type
            setHealthLogStyling(healthRecord)

            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(healthRecord)
            }

            btnViewHealthLogDetails.setOnClickListener {
                onItemClick(healthRecord)
            }
        }

        /**
         * Set colors and icons based on health record type to match Health Metrics Grid
         */
        private fun setHealthLogStyling(healthRecord: HealthRecord) {
            try {
                when (healthRecord.type.lowercase()) {
                    "blood_pressure" -> {
                        // Green theme for Blood Pressure
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_health_light))
                        cardView.strokeColor = itemView.context.getColor(R.color.senior_health)
                        ivHealthLogIcon.setImageResource(R.drawable.ic_blood_pressure)
                        ivHealthLogIcon.setColorFilter(itemView.context.getColor(R.color.senior_health))
                        tvHealthLogValue.setTextColor(itemView.context.getColor(R.color.senior_health))
                        btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(R.color.senior_health))
                    }
                    "blood_sugar" -> {
                        // Orange theme for Blood Sugar
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_reminder_light))
                        cardView.strokeColor = itemView.context.getColor(R.color.senior_reminder)
                        ivHealthLogIcon.setImageResource(R.drawable.ic_blood_sugar)
                        ivHealthLogIcon.setColorFilter(itemView.context.getColor(R.color.senior_reminder))
                        tvHealthLogValue.setTextColor(itemView.context.getColor(R.color.senior_reminder))
                        btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(R.color.senior_reminder))
                    }
                    "weight" -> {
                        // Blue theme for Weight
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_profile_light))
                        cardView.strokeColor = itemView.context.getColor(R.color.senior_profile)
                        ivHealthLogIcon.setImageResource(R.drawable.ic_weight)
                        ivHealthLogIcon.setColorFilter(itemView.context.getColor(R.color.senior_profile))
                        tvHealthLogValue.setTextColor(itemView.context.getColor(R.color.senior_profile))
                        btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(R.color.senior_profile))
                    }
                    "heart_rate" -> {
                        // Red theme for Heart Rate
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_emergency_light))
                        cardView.strokeColor = itemView.context.getColor(R.color.senior_emergency)
                        ivHealthLogIcon.setImageResource(R.drawable.ic_health)
                        ivHealthLogIcon.setColorFilter(itemView.context.getColor(R.color.senior_emergency))
                        tvHealthLogValue.setTextColor(itemView.context.getColor(R.color.senior_emergency))
                        btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(R.color.senior_emergency))
                    }
                    else -> {
                        // Default green theme
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_health_light))
                        cardView.strokeColor = itemView.context.getColor(R.color.senior_health)
                        ivHealthLogIcon.setImageResource(R.drawable.ic_health)
                        ivHealthLogIcon.setColorFilter(itemView.context.getColor(R.color.senior_health))
                        tvHealthLogValue.setTextColor(itemView.context.getColor(R.color.senior_health))
                        btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(R.color.senior_health))
                    }
                }
            } catch (e: Exception) {
                // Fallback to default styling if color resources are not available
                android.util.Log.e("HealthLogAdapter", "Error setting health log styling: ${e.message}")
                try {
                    cardView.setCardBackgroundColor(itemView.context.getColor(android.R.color.white))
                    cardView.strokeColor = itemView.context.getColor(android.R.color.darker_gray)
                    ivHealthLogIcon.setImageResource(R.drawable.ic_health)
                    ivHealthLogIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                    tvHealthLogValue.setTextColor(itemView.context.getColor(android.R.color.black))
                    btnViewHealthLogDetails.setBackgroundColor(itemView.context.getColor(android.R.color.darker_gray))
                } catch (fallbackException: Exception) {
                    android.util.Log.e("HealthLogAdapter", "Fallback styling also failed: ${fallbackException.message}")
                }
            }
        }

        /**
         * Format health value for display
         */
        private fun formatHealthValue(healthRecord: HealthRecord): String {
            return when (healthRecord.type.lowercase()) {
                "blood_pressure" -> {
                    // For blood pressure, we'll use the value field which should contain "systolic/diastolic"
                    "${healthRecord.value} mmHg"
                }
                "heart_rate" -> "${healthRecord.value} bpm"
                "blood_sugar" -> "${healthRecord.value} mg/dL"
                "weight" -> "${healthRecord.value} ${healthRecord.unit}"
                else -> if (healthRecord.unit.isNotBlank()) {
                    "${healthRecord.value} ${healthRecord.unit}"
                } else {
                    healthRecord.value
                }
            }
        }
    }
}
