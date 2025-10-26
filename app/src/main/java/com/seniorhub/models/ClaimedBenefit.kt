package com.seniorhub.models

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.*

/**
 * ClaimedBenefit data model for benefits that seniors have claimed
 */
data class ClaimedBenefit(
    var id: String = "",
    var userId: String = "", // Senior citizen who claimed the benefit
    var benefitId: String = "", // Reference to the Benefit
    var benefitTitle: String = "", // Cached benefit title for display
    var claimDate: Timestamp = Timestamp.now(),
    var status: String = "Claimed", // "Claimed", "Processing", "Approved", "Denied", "Active"
    var amount: String = "", // Amount claimed or received
    var notes: String = "", // Additional notes about the claim
    var applicationNumber: String = "", // Government application reference number
    var nextDisbursementDate: Date? = null, // For approved benefits
    var disbursementAmount: String = "", // Amount for next disbursement
    var isActive: Boolean = true,
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now()
) : Serializable {

    /**
     * Get formatted claim date
     */
    fun getFormattedClaimDate(): String {
        val calendar = Calendar.getInstance()
        calendar.time = claimDate.toDate()
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
    }

    /**
     * Get formatted amount
     */
    fun getFormattedAmount(): String {
        return when {
            amount.isEmpty() -> "TBD"
            amount == "0" -> "Free"
            else -> "$$amount"
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
     * Check if claim is approved and active
     */
    fun isApprovedAndActive(): Boolean {
        return status == "Approved" || status == "Active"
    }

    /**
     * Check if claim is still processing
     */
    fun isProcessing(): Boolean {
        return status == "Processing" || status == "Claimed"
    }

    /**
     * Get status color for UI display
     */
    fun getStatusColor(): String {
        return when (status) {
            "Approved", "Active" -> "green"
            "Processing", "Claimed" -> "orange"
            "Denied" -> "red"
            else -> "gray"
        }
    }
}
