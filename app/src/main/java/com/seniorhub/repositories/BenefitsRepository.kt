package com.seniorhub.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.QuerySnapshot
import com.seniorhub.models.Benefit
import com.seniorhub.models.ClaimedBenefit
import com.seniorhub.utils.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * BenefitsRepository - Government Benefits Management
 *
 * Handles all benefits-related database operations:
 * - Available benefits management (admin only)
 * - Claimed benefits tracking
 * - Benefit disbursement management
 * - Admin controls for benefit creation and updates
 */
class BenefitsRepository private constructor() {

    companion object {
        private const val TAG = "BenefitsRepository"
        private const val BENEFITS_PATH = "benefits"
        private const val CLAIMED_BENEFITS_PATH = "claimed_benefits"

        @Volatile
        private var INSTANCE: BenefitsRepository? = null

        /**
         * Get singleton instance of BenefitsRepository
         */
        fun getInstance(): BenefitsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BenefitsRepository().also { INSTANCE = it }
            }
        }
    }

    // Simple in-memory cache for demo purposes
    private val benefitsCache = mutableMapOf<String, MutableList<Benefit>>()
    private val claimedBenefitsCache = mutableMapOf<String, MutableList<ClaimedBenefit>>()

    /**
     * Get all available benefits from Firebase Firestore
     */
    suspend fun getAvailableBenefits(): Result<List<Benefit>> {
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val benefitsCollection = db.collection("benefits")
            
            val querySnapshot = benefitsCollection
                .whereEqualTo("isActive", true)
                .orderBy("title")
                .get()
                .await()
            
            val benefits = mutableListOf<Benefit>()
            for (document in querySnapshot) {
                try {
                    val benefit = document.toObject(Benefit::class.java)
                    benefit.id = document.id
                    benefits.add(benefit)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit document ${document.id}: ${e.message}")
                    continue
                }
            }
            
            Result.Success(benefits)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available benefits from Firebase", e)
            Result.Error(Exception("Failed to load available benefits: ${e.message}"))
        }
    }

    /**
     * Get claimed benefits for a specific user
     */
    suspend fun getClaimedBenefits(userId: String): Result<List<ClaimedBenefit>> {
        return try {
            delay(300) // Simulate network delay
            
            // Selected Davao City claimed assistance programs
            val sampleClaimedBenefits = listOf(
                ClaimedBenefit(
                    id = "1",
                    userId = userId,
                    benefitId = "1",
                    benefitTitle = "Medical Service Assistance",
                    claimDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 15)), // 15 days ago
                    status = "Active",
                    amount = "0",
                    applicationNumber = "MED-DC-2024-003456",
                    nextDisbursementDate = null,
                    disbursementAmount = "0"
                ),
                ClaimedBenefit(
                    id = "2",
                    userId = userId,
                    benefitId = "2",
                    benefitTitle = "DSWD Social Pension",
                    claimDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 60)), // 60 days ago
                    status = "Active",
                    amount = "500",
                    applicationNumber = "DSWD-DC-2024-005678",
                    nextDisbursementDate = getNextMonthDate(15),
                    disbursementAmount = "500"
                ),
                ClaimedBenefit(
                    id = "3",
                    userId = userId,
                    benefitId = "3",
                    benefitTitle = "DSWD Food Assistance Program",
                    claimDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 20)), // 20 days ago
                    status = "Processing",
                    amount = "0",
                    applicationNumber = "FOOD-DC-2024-004567",
                    nextDisbursementDate = getNextMonthDate(20),
                    disbursementAmount = "0"
                ),
                ClaimedBenefit(
                    id = "4",
                    userId = userId,
                    benefitId = "4",
                    benefitTitle = "Davao City Housing Program for Seniors",
                    claimDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 30)), // 30 days ago
                    status = "Processing",
                    amount = "0",
                    applicationNumber = "HOUSING-DC-2024-002345",
                    nextDisbursementDate = null,
                    disbursementAmount = "0"
                ),
                ClaimedBenefit(
                    id = "5",
                    userId = userId,
                    benefitId = "5",
                    benefitTitle = "Annual Financial Assistance",
                    claimDate = Timestamp(Date(System.currentTimeMillis() - 86400000 * 45)), // 45 days ago
                    status = "Active",
                    amount = "3000",
                    applicationNumber = "ANNUAL-DC-2024-001234",
                    nextDisbursementDate = getNextMonthDate(25),
                    disbursementAmount = "3000"
                )
            )
            
            Result.Success(sampleClaimedBenefits)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting claimed benefits for user $userId", e)
            Result.Error(Exception("Failed to load claimed benefits: ${e.message}"))
        }
    }

    /**
     * Claim a benefit (for senior citizens)
     */
    suspend fun claimBenefit(userId: String, benefitId: String, benefitTitle: String): Result<ClaimedBenefit> {
        return try {
            delay(500) // Simulate network delay
            
            val claimedBenefit = ClaimedBenefit(
                id = UUID.randomUUID().toString(),
                userId = userId,
                benefitId = benefitId,
                benefitTitle = benefitTitle,
                claimDate = Timestamp.now(),
                status = "Processing",
                applicationNumber = "APP-${System.currentTimeMillis()}"
            )
            
            // In real implementation, save to database
            Log.d(TAG, "Benefit claimed: $benefitTitle by user $userId")
            
            Result.Success(claimedBenefit)
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming benefit", e)
            Result.Error(Exception("Failed to claim benefit: ${e.message}"))
        }
    }

    /**
     * Add or update a benefit (admin only)
     */
    suspend fun saveBenefit(benefit: Benefit, adminUserId: String): Result<Benefit> {
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val benefitsCollection = db.collection("benefits")
            
            val updatedBenefit = benefit.copy(
                id = if (benefit.id.isEmpty()) UUID.randomUUID().toString() else benefit.id,
                createdBy = adminUserId,
                updatedAt = Timestamp.now()
            )
            
            if (benefit.id.isEmpty()) {
                // Add new benefit
                val docRef = benefitsCollection.document(updatedBenefit.id)
                docRef.set(updatedBenefit).await()
                Log.d(TAG, "New benefit added by admin $adminUserId: ${updatedBenefit.title}")
            } else {
                // Update existing benefit
                val docRef = benefitsCollection.document(updatedBenefit.id)
                docRef.set(updatedBenefit).await()
                Log.d(TAG, "Benefit updated by admin $adminUserId: ${updatedBenefit.title}")
            }
            
            Result.Success(updatedBenefit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving benefit to Firebase", e)
            Result.Error(Exception("Failed to save benefit: ${e.message}"))
        }
    }

    /**
     * Update benefit disbursement info (admin only)
     */
    suspend fun updateBenefitDisbursement(
        benefitId: String, 
        nextDisbursementDate: Date, 
        disbursementAmount: String,
        adminUserId: String
    ): Result<Boolean> {
        return try {
            delay(300) // Simulate network delay
            
            // In real implementation, update in database
            Log.d(TAG, "Benefit disbursement updated by admin $adminUserId for benefit $benefitId")
            
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating benefit disbursement", e)
            Result.Error(Exception("Failed to update disbursement: ${e.message}"))
        }
    }

    /**
     * Get benefits summary for dashboard
     */
    suspend fun getBenefitsSummary(userId: String): Result<Pair<Int, String>> {
        return try {
            delay(200) // Simulate network delay
            
            val availableBenefitsResult = getAvailableBenefits()
            val claimedBenefitsResult = getClaimedBenefits(userId)
            
            if (availableBenefitsResult is Result.Success && claimedBenefitsResult is Result.Success) {
                val availableCount = availableBenefitsResult.data?.size ?: 0
                val nextDisbursement = claimedBenefitsResult.data
                    ?.filter { it.isApprovedAndActive() }
                    ?.minByOrNull { it.nextDisbursementDate ?: Date(Long.MAX_VALUE) }
                    ?.getFormattedNextDisbursement() ?: "TBD"
                
                Result.Success(Pair(availableCount, nextDisbursement))
            } else {
                Result.Success(Pair(0, "TBD"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting benefits summary", e)
            Result.Error(Exception("Failed to load benefits summary: ${e.message}"))
        }
    }

    /**
     * Check if user is admin
     */
    suspend fun isUserAdmin(userId: String): Result<Boolean> {
        return try {
            delay(100) // Simulate network delay
            
            // For demo, assume user with ID "admin" is admin
            val isAdmin = userId == "admin" || userId.contains("admin")
            Result.Success(isAdmin)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            Result.Error(Exception("Failed to check admin status: ${e.message}"))
        }
    }

    /**
     * Helper function to get next month's date
     */
    private fun getNextMonthDate(day: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        return calendar.time
    }
}
