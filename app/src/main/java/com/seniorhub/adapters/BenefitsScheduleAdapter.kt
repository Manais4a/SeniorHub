package com.seniorhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seniorhub.R
import com.seniorhub.models.Benefit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying scheduled benefits in RecyclerView
 */
class BenefitsScheduleAdapter(
    private var benefits: List<Benefit> = emptyList()
) : RecyclerView.Adapter<BenefitsScheduleAdapter.BenefitScheduleViewHolder>() {

    class BenefitScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header elements
        val ivScheduleIcon: ImageView = itemView.findViewById(R.id.ivScheduleIcon)
        val tvBenefitTitle: TextView = itemView.findViewById(R.id.tvBenefitTitle)
        val tvScheduleDate: TextView = itemView.findViewById(R.id.tvScheduleDate)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        
        // Expandable details elements
        val detailsContainer: LinearLayout = itemView.findViewById(R.id.detailsContainer)
        val tvBenefitAmount: TextView = itemView.findViewById(R.id.tvBenefitAmount)
        val tvScheduleDetails: TextView = itemView.findViewById(R.id.tvScheduleDetails)
        val tvBenefitDescription: TextView = itemView.findViewById(R.id.tvBenefitDescription)
        val tvBenefitRequirements: TextView = itemView.findViewById(R.id.tvBenefitRequirements)
        val tvBenefitApplicationProcess: TextView = itemView.findViewById(R.id.tvBenefitApplicationProcess)
        val tvBenefitContactInfo: TextView = itemView.findViewById(R.id.tvBenefitContactInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BenefitScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_benefit_next_schedule, parent, false)
        return BenefitScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BenefitScheduleViewHolder, position: Int) {
        val benefit = benefits[position]
        
        // Set basic benefit information
        holder.tvBenefitTitle.text = benefit.title
        
        // Format and set schedule date
        val scheduleDate = benefit.nextDisbursementDate?.let { date ->
            val calendar = Calendar.getInstance()
            calendar.time = date
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val year = calendar.get(Calendar.YEAR)
            "$month/$day/$year"
        } ?: "TBD"
        
        holder.tvScheduleDate.text = scheduleDate
        
        // Set amount
        holder.tvBenefitAmount.text = formatAmount(benefit.amount)
        
        // Set description
        holder.tvBenefitDescription.text = benefit.description ?: "No description available"
        
        // Set requirements
        holder.tvBenefitRequirements.text = benefit.requirements ?: "No requirements specified"
        
        // Set application process
        holder.tvBenefitApplicationProcess.text = benefit.applicationProcess ?: "Contact for application process"
        
        // Set contact information
        holder.tvBenefitContactInfo.text = benefit.contactInfo ?: "Contact information not available"
        
        // Set schedule details
        val scheduleDetails = buildString {
            append("Next disbursement: $scheduleDate\n")
            append("Amount: ${formatAmount(benefit.amount)}\n")
            append("Status: ${benefit.status}")
        }
        holder.tvScheduleDetails.text = scheduleDetails
        
        // Set up expand/collapse functionality
        var isExpanded = false
        holder.ivExpand.setOnClickListener {
            isExpanded = !isExpanded
            holder.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.ivExpand.rotation = if (isExpanded) 180f else 0f
        }
        
        // Set up card click to expand/collapse
        holder.itemView.setOnClickListener {
            isExpanded = !isExpanded
            holder.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.ivExpand.rotation = if (isExpanded) 180f else 0f
        }
    }

    override fun getItemCount(): Int = benefits.size

    fun updateBenefits(newBenefits: List<Benefit>) {
        benefits = newBenefits
        notifyDataSetChanged()
    }

    private fun formatAmount(amount: String?): String {
        if (amount.isNullOrEmpty()) return "Contact for details"
        if (amount == "0") return "Free"
        if (amount == "Free") return "Free"
        if (amount.contains("%")) return amount // For percentage discounts
        if (amount.contains("Monthly Food Pack")) return amount // For non-monetary benefits
        if (amount.contains("Varies")) return amount // For variable amounts
        
        // Convert to number and format as peso
        return try {
            val numAmount = amount.toFloat()
            "₱${String.format("%.0f", numAmount)}"
        } catch (e: NumberFormatException) {
            amount // Return original if not a number
        }
    }
}

