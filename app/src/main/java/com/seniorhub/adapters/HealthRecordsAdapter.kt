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
 * HealthRecordsAdapter - RecyclerView adapter for displaying health records
 *
 * Features senior-friendly UI with large text and clear formatting
 */
class HealthRecordsAdapter(
    private val onItemClick: (HealthRecord) -> Unit
) : RecyclerView.Adapter<HealthRecordsAdapter.HealthRecordViewHolder>() {

    private var healthRecords = mutableListOf<HealthRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_record, parent, false)
        return HealthRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: HealthRecordViewHolder, position: Int) {
        holder.bind(healthRecords[position])
    }

    override fun getItemCount(): Int = healthRecords.size

    /**
     * Update the list of health records with efficient change notifications
     */
    fun updateRecords(newRecords: List<HealthRecord>?) {
        val oldSize = healthRecords.size
        healthRecords.clear()
        newRecords?.let { healthRecords.addAll(it) }

        // Use more specific change notifications for better performance
        if (oldSize > 0 && healthRecords.isEmpty()) {
            notifyItemRangeRemoved(0, oldSize)
        } else if (healthRecords.isEmpty()) {
            // No items to notify about
        } else if (oldSize == 0) {
            notifyItemRangeInserted(0, healthRecords.size)
        } else {
            // For simplicity, use notifyDataSetChanged when sizes are different
            // In a real app, you might want to implement DiffUtil for better performance
            notifyDataSetChanged()
        }
    }

    /**
     * Add a single record with efficient notification
     */
    fun addRecord(healthRecord: HealthRecord) {
        healthRecords.add(healthRecord)
        notifyItemInserted(healthRecords.size - 1)
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

    inner class HealthRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHealthType: TextView = itemView.findViewById(R.id.tvHealthType)
        private val tvHealthValue: TextView = itemView.findViewById(R.id.tvHealthValue)
        private val tvHealthDate: TextView = itemView.findViewById(R.id.tvHealthDate)
        private val ivHealthIcon: ImageView = itemView.findViewById(R.id.ivHealthIcon)
        private val cardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(healthRecord: HealthRecord) {
            // Fix deprecated capitalize() method
            tvHealthType.text = healthRecord.type.replace("_", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            // Create display value since getDisplayValue() doesn't exist
            tvHealthValue.text = formatHealthValue(healthRecord)
            tvHealthDate.text = dateFormat.format((healthRecord.timestamp))

            // Set dynamic colors and icons based on health record type
            setHealthRecordStyling(healthRecord)

            itemView.setOnClickListener {
                onItemClick(healthRecord)
            }
        }

        /**
         * Set colors and icons based on health record type to match Health Metrics Grid
         */
        private fun setHealthRecordStyling(healthRecord: HealthRecord) {
            when (healthRecord.type.lowercase()) {
                "blood_pressure" -> {
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_health_light))
                    cardView.strokeColor = itemView.context.getColor(R.color.senior_health)
                    ivHealthIcon.setImageResource(R.drawable.ic_blood_pressure)
                    ivHealthIcon.setColorFilter(itemView.context.getColor(R.color.senior_health))
                    tvHealthValue.setTextColor(itemView.context.getColor(R.color.senior_health))
                }
                "blood_sugar" -> {
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_reminder_light))
                    cardView.strokeColor = itemView.context.getColor(R.color.senior_reminder)
                    ivHealthIcon.setImageResource(R.drawable.ic_blood_sugar)
                    ivHealthIcon.setColorFilter(itemView.context.getColor(R.color.senior_reminder))
                    tvHealthValue.setTextColor(itemView.context.getColor(R.color.senior_reminder))
                }
                "weight" -> {
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_profile_light))
                    cardView.strokeColor = itemView.context.getColor(R.color.senior_profile)
                    ivHealthIcon.setImageResource(R.drawable.ic_weight)
                    ivHealthIcon.setColorFilter(itemView.context.getColor(R.color.senior_profile))
                    tvHealthValue.setTextColor(itemView.context.getColor(R.color.senior_profile))
                }
                "heart_rate" -> {
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_emergency_light))
                    cardView.strokeColor = itemView.context.getColor(R.color.senior_emergency)
                    ivHealthIcon.setImageResource(R.drawable.ic_health)
                    ivHealthIcon.setColorFilter(itemView.context.getColor(R.color.senior_emergency))
                    tvHealthValue.setTextColor(itemView.context.getColor(R.color.senior_emergency))
                }
                else -> {
                    // Default styling
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.senior_health_light))
                    cardView.strokeColor = itemView.context.getColor(R.color.senior_health)
                    ivHealthIcon.setImageResource(R.drawable.ic_health)
                    ivHealthIcon.setColorFilter(itemView.context.getColor(R.color.senior_health))
                    tvHealthValue.setTextColor(itemView.context.getColor(R.color.senior_health))
                }
            }
        }

        /**
         * Format health value for display since getDisplayValue() doesn't exist
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
                "temperature" -> "${healthRecord.value}°${healthRecord.unit}"
                else -> if (healthRecord.unit.isNotBlank()) {
                    "${healthRecord.value} ${healthRecord.unit}"
                } else {
                    healthRecord.value
                }
            }
        }
    }
}