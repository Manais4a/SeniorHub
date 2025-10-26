package com.seniorhub.models

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.*

/**
 * Benefit data model for government benefits available to senior citizens
 */
data class Benefit(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "", // "Social Security", "Medicare", "Housing", "Food", "Transportation", etc.
    var status: String = "Available", // "Available", "Active", "Pending", "Discontinued"
    var amount: String = "", // Monthly amount or benefit value
    var requirements: String = "", // Eligibility requirements
    var applicationProcess: String = "", // How to apply
    var contactInfo: String = "", // Office contact information
    var website: String = "", // Official website
    var isActive: Boolean = true,
    var createdBy: String = "", // Admin user ID who created this benefit
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),
    var nextDisbursementDate: Date? = null, // For active benefits
    var disbursementAmount: String = "" // Amount for next disbursement
) : Serializable {

    /**
     * Get formatted benefit amount
     */
    fun getFormattedAmount(): String {
        return when {
            amount.isEmpty() -> "Contact for details"
            amount == "0" -> "Free"
            else -> "$$amount"
        }
    }

    /**
     * Get formatted disbursement amount
     */
    fun getFormattedDisbursementAmount(): String {
        return when {
            disbursementAmount.isEmpty() -> "TBD"
            disbursementAmount == "0" -> "Free"
            else -> "$$disbursementAmount"
        }
    }

    /**
     * Get formatted next disbursement date
     */
    fun getFormattedNextDisbursement(): String {
        return nextDisbursementDate?.let { date ->
            val calendar = Calendar.getInstance()
            calendar.time = date
            "${calendar.get(Calendar.DAY_OF_MONTH)}"
        } ?: "TBD"
    }

    /**
     * Check if benefit is claimable
     */
    fun isClaimable(): Boolean {
        return status == "Available" && isActive
    }

    /**
     * Check if benefit is active (currently receiving)
     */
    fun isCurrentlyActive(): Boolean {
        return status == "Active" && isActive
    }
}
