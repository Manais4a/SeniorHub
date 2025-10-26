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

/**
 * Adapter for displaying available benefits in RecyclerView
 */
class BenefitsAdapter(
    private var benefits: List<Benefit> = emptyList()
) : RecyclerView.Adapter<BenefitsAdapter.BenefitViewHolder>() {

    class BenefitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header elements
        val ivBenefitIcon: ImageView = itemView.findViewById(R.id.ivBenefitIcon)
        val tvBenefitTitle: TextView = itemView.findViewById(R.id.tvBenefitTitle)
        val tvBenefitStatus: TextView = itemView.findViewById(R.id.tvBenefitStatus)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        
        // Expandable details elements
        val detailsContainer: LinearLayout = itemView.findViewById(R.id.detailsContainer)
        val tvBenefitAmount: TextView = itemView.findViewById(R.id.tvBenefitAmount)
        val tvBenefitDescription: TextView = itemView.findViewById(R.id.tvBenefitDescription)
        val tvBenefitRequirements: TextView = itemView.findViewById(R.id.tvBenefitRequirements)
        val tvBenefitApplicationProcess: TextView = itemView.findViewById(R.id.tvBenefitApplicationProcess)
        val tvBenefitContactInfo: TextView = itemView.findViewById(R.id.tvBenefitContactInfo)
        
        // Action buttons removed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BenefitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_benefit, parent, false)
        return BenefitViewHolder(view)
    }

    override fun onBindViewHolder(holder: BenefitViewHolder, position: Int) {
        val benefit = benefits[position]
        
        // Set basic benefit information
        holder.tvBenefitTitle.text = benefit.title
        holder.tvBenefitStatus.text = benefit.status
        
        // Set category icon based on benefit category
        setCategoryIcon(holder.ivBenefitIcon, benefit.category)
        
        // Set expandable details
        holder.tvBenefitAmount.text = benefit.getFormattedAmount()
        holder.tvBenefitDescription.text = benefit.description
        holder.tvBenefitRequirements.text = benefit.requirements
        holder.tvBenefitApplicationProcess.text = benefit.applicationProcess
        holder.tvBenefitContactInfo.text = benefit.contactInfo
        
        // Initialize details container as collapsed
        holder.detailsContainer.visibility = View.GONE
        holder.ivExpand.rotation = 0f
        
        // Set expand/collapse functionality
        val headerContainer = holder.itemView.findViewById<LinearLayout>(R.id.headerContainer)
        headerContainer.setOnClickListener {
            toggleDetails(holder)
        }
        
        // Button functionality removed - details can be toggled by clicking the header
    }
    
    /**
     * Toggle expand/collapse of benefit details
     */
    private fun toggleDetails(holder: BenefitViewHolder) {
        val isExpanded = holder.detailsContainer.visibility == View.VISIBLE
        
        if (isExpanded) {
            holder.detailsContainer.visibility = View.GONE
            holder.ivExpand.rotation = 0f
        } else {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.ivExpand.rotation = 180f
        }
    }
    
    /**
     * Set category icon based on benefit category
     */
    private fun setCategoryIcon(imageView: ImageView, category: String) {
        val iconRes = when (category) {
            "Medical Assistance" -> R.drawable.ic_health
            "Financial Support" -> R.drawable.ic_money
            "Food Assistance" -> R.drawable.ic_food
            "Housing Support" -> R.drawable.ic_home
            "Medicare" -> R.drawable.ic_medicare
            else -> R.drawable.ic_benefits
        }
        imageView.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = benefits.size

    /**
     * Update the benefits list
     */
    fun updateBenefits(newBenefits: List<Benefit>) {
        benefits = newBenefits
        notifyDataSetChanged()
    }

    /**
     * Add a new benefit to the list
     */
    fun addBenefit(benefit: Benefit) {
        val newList = benefits.toMutableList()
        newList.add(benefit)
        benefits = newList
        notifyItemInserted(benefits.size - 1)
    }

    /**
     * Update a specific benefit
     */
    fun updateBenefit(updatedBenefit: Benefit) {
        val index = benefits.indexOfFirst { it.id == updatedBenefit.id }
        if (index != -1) {
            val newList = benefits.toMutableList()
            newList[index] = updatedBenefit
            benefits = newList
            notifyItemChanged(index)
        }
    }
}
