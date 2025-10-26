package com.seniorhub.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.database.*
import com.seniorhub.models.Benefit
import com.seniorhub.utils.Result
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * BenefitsRepositoryRTDB - Government Benefits Management using Firebase Realtime Database
 *
 * Handles all benefits-related database operations:
 * - Available benefits management (admin only)
 * - Benefit disbursement management
 * - Admin controls for benefit creation and updates
 */
class BenefitsRepositoryRTDB private constructor() {

    companion object {
        private const val TAG = "BenefitsRepositoryRTDB"
        private const val BENEFITS_PATH = "benefits"

        @Volatile
        private var INSTANCE: BenefitsRepositoryRTDB? = null

        /**
         * Get singleton instance of BenefitsRepositoryRTDB
         */
        fun getInstance(): BenefitsRepositoryRTDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BenefitsRepositoryRTDB().also { INSTANCE = it }
            }
        }
    }

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    /**
     * Get all available benefits from Firebase Realtime Database
     */
    suspend fun getAvailableBenefits(): Result<List<Benefit>> {
        return try {
            val benefitsRef = database.child(BENEFITS_PATH)
            val dataSnapshot = benefitsRef.get().await()
            
            val benefits = mutableListOf<Benefit>()
            if (dataSnapshot.exists()) {
                for (snapshot in dataSnapshot.children) {
                    try {
                        // Try to parse as Benefit object first
                        val benefitData = snapshot.getValue(Benefit::class.java)
                        benefitData?.let {
                            it.id = snapshot.key ?: ""
                            if (it.isActive) {
                                benefits.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        // If direct parsing fails, try manual parsing
                        try {
                            val data = snapshot.value as? Map<String, Any>
                            if (data != null) {
                                val benefit = Benefit(
                                    id = snapshot.key ?: "",
                                    title = data["title"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    category = data["category"] as? String ?: "",
                                    status = data["status"] as? String ?: "Available",
                                    amount = data["amount"] as? String ?: "",
                                    requirements = data["requirements"] as? String ?: "",
                                    applicationProcess = data["applicationProcess"] as? String ?: "",
                                    contactInfo = data["contactInfo"] as? String ?: "",
                                    website = data["website"] as? String ?: "",
                                    isActive = data["isActive"] as? Boolean ?: true,
                                    createdBy = data["createdBy"] as? String ?: "",
                                    disbursementAmount = data["disbursementAmount"] as? String ?: ""
                                )
                                
                                // Handle timestamps - fix type conversion
                                val createdAt = data["createdAt"] as? Map<String, Any>
                                if (createdAt != null) {
                                    val seconds = createdAt["seconds"] as? Long ?: 0L
                                    val nanoseconds = createdAt["nanoseconds"] as? Long ?: 0L
                                    benefit.createdAt = Timestamp(seconds, (nanoseconds / 1000000).toInt())
                                }
                                
                                val updatedAt = data["updatedAt"] as? Map<String, Any>
                                if (updatedAt != null) {
                                    val seconds = updatedAt["seconds"] as? Long ?: 0L
                                    val nanoseconds = updatedAt["nanoseconds"] as? Long ?: 0L
                                    benefit.updatedAt = Timestamp(seconds, (nanoseconds / 1000000).toInt())
                                }
                                
                                // Handle next disbursement date
                                val nextDisbursement = data["nextDisbursementDate"] as? Long
                                if (nextDisbursement != null) {
                                    benefit.nextDisbursementDate = Date(nextDisbursement)
                                }
                                
                                if (benefit.isActive) {
                                    benefits.add(benefit)
                                }
                            }
                        } catch (parseException: Exception) {
                            Log.w(TAG, "Error parsing benefit ${snapshot.key}: ${parseException.message}")
                            continue
                        }
                    }
                }
            }
            
            // If no benefits found, try to add default Davao City benefits
            if (benefits.isEmpty()) {
                Log.d(TAG, "No benefits found, adding default Davao City benefits")
                addDavaoCityBenefits("admin")
                // Try to load again after adding defaults
                return getAvailableBenefits()
            }
            
            // Sort by title
            benefits.sortBy { it.title }
            
            Log.d(TAG, "Loaded ${benefits.size} available benefits from Realtime Database")
            Result.Success(benefits)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available benefits from Realtime Database", e)
            Result.Error(Exception("Failed to load available benefits: ${e.message}"))
        }
    }

    /**
     * Add or update a benefit (admin only)
     */
    suspend fun saveBenefit(benefit: Benefit, adminUserId: String): Result<Benefit> {
        return try {
            val benefitsRef = database.child(BENEFITS_PATH)
            
            val updatedBenefit = benefit.copy(
                id = if (benefit.id.isEmpty()) UUID.randomUUID().toString() else benefit.id,
                createdBy = adminUserId,
                updatedAt = Timestamp.now()
            )
            
            if (benefit.id.isEmpty()) {
                // Add new benefit
                val newBenefitRef = benefitsRef.child(updatedBenefit.id)
                newBenefitRef.setValue(updatedBenefit).await()
                Log.d(TAG, "New benefit added by admin $adminUserId: ${updatedBenefit.title}")
            } else {
                // Update existing benefit
                val benefitRef = benefitsRef.child(updatedBenefit.id)
                benefitRef.setValue(updatedBenefit).await()
                Log.d(TAG, "Benefit updated by admin $adminUserId: ${updatedBenefit.title}")
            }
            
            Result.Success(updatedBenefit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving benefit to Realtime Database", e)
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
            val benefitRef = database.child(BENEFITS_PATH).child(benefitId)
            
            val updates = mapOf(
                "nextDisbursementDate" to nextDisbursementDate.time,
                "disbursementAmount" to disbursementAmount,
                "updatedAt" to Date().time
            )
            
            benefitRef.updateChildren(updates).await()
            
            Log.d(TAG, "Benefit disbursement updated by admin $adminUserId for benefit $benefitId")
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating benefit disbursement", e)
            Result.Error(Exception("Failed to update disbursement: ${e.message}"))
        }
    }

    /**
     * Delete a benefit (admin only)
     */
    suspend fun deleteBenefit(benefitId: String, adminUserId: String): Result<Boolean> {
        return try {
            val benefitRef = database.child(BENEFITS_PATH).child(benefitId)
            benefitRef.removeValue().await()
            
            Log.d(TAG, "Benefit deleted by admin $adminUserId: $benefitId")
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting benefit", e)
            Result.Error(Exception("Failed to delete benefit: ${e.message}"))
        }
    }

    /**
     * Get benefits summary for dashboard
     */
    suspend fun getBenefitsSummary(userId: String): Result<Pair<Int, String>> {
        return try {
            val availableBenefitsResult = getAvailableBenefits()
            
            if (availableBenefitsResult is Result.Success) {
                val availableCount = availableBenefitsResult.data?.size ?: 0
                val nextDisbursement = "TBD" // Simplified since we removed claimed benefits
                
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
            // For demo, assume user with ID "admin" is admin
            val isAdmin = userId == "admin" || userId.contains("admin")
            Result.Success(isAdmin)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            Result.Error(Exception("Failed to check admin status: ${e.message}"))
        }
    }

    /**
     * Add Davao City benefits to Realtime Database
     */
    suspend fun addDavaoCityBenefits(adminUserId: String): Result<Boolean> {
        return try {
            val davaoCityBenefits = mapOf(
                "medical_service_assistance" to Benefit(
                    id = "medical_service_assistance",
                    title = "Medical Service Assistance",
                    category = "Medical Assistance",
                    amount = "Free",
                    status = "Available",
                    description = "Free medical consultation and healthcare services for senior citizens in Davao City",
                    requirements = "Senior Citizen ID, valid ID, proof of residence in Davao City",
                    applicationProcess = "Visit DCMC or any participating health center with required documents",
                    contactInfo = "DCMC: (082) 227-2731, JP Laurel Ave, Bajada",
                    website = "",
                    isActive = true,
                    nextDisbursementDate = null,
                    disbursementAmount = "0",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                "dswd_social_pension" to Benefit(
                    id = "dswd_social_pension",
                    title = "DSWD Social Pension",
                    category = "Financial Support",
                    amount = "500",
                    status = "Available",
                    description = "₱500 monthly cash assistance for indigent senior citizens",
                    requirements = "Senior Citizen ID, valid ID, proof of indigency, barangay certification",
                    applicationProcess = "Submit application at DSWD Field Office XI with required documents",
                    contactInfo = "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                    website = "https://www.dswd.gov.ph",
                    isActive = true,
                    nextDisbursementDate = Date(System.currentTimeMillis() + 15 * 24 * 60 * 60 * 1000),
                    disbursementAmount = "500",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                "dswd_food_assistance" to Benefit(
                    id = "dswd_food_assistance",
                    title = "DSWD Food Assistance Program",
                    category = "Food Assistance",
                    amount = "Monthly Food Pack",
                    status = "Available",
                    description = "Monthly food packs and nutritional support for senior citizens",
                    requirements = "Senior Citizen ID, valid ID, proof of need, barangay certification",
                    applicationProcess = "Register at DSWD Field Office XI or through barangay",
                    contactInfo = "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                    website = "https://www.dswd.gov.ph",
                    isActive = true,
                    nextDisbursementDate = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
                    disbursementAmount = "0",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                "davao_housing_program" to Benefit(
                    id = "davao_housing_program",
                    title = "Davao City Housing Program for Seniors",
                    category = "Housing Support",
                    amount = "Varies",
                    status = "Available",
                    description = "Housing assistance and emergency shelter for senior citizens",
                    requirements = "Senior Citizen ID, valid ID, proof of housing need, income certificate",
                    applicationProcess = "Submit application at City Housing Office with required documents",
                    contactInfo = "City Housing Office: (082) 222-1234, City Hall",
                    website = "",
                    isActive = true,
                    nextDisbursementDate = null,
                    disbursementAmount = "0",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                "annual_financial_assistance" to Benefit(
                    id = "annual_financial_assistance",
                    title = "Annual Financial Assistance",
                    category = "Financial Support",
                    amount = "3000",
                    status = "Available",
                    description = "₱3,000 yearly cash assistance during Christmas season",
                    requirements = "Senior Citizen ID, valid ID, proof of residence in Davao City",
                    applicationProcess = "Register at OSCA Davao during application period",
                    contactInfo = "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                    website = "",
                    isActive = true,
                    nextDisbursementDate = Date(Date().year, 11, 15),
                    disbursementAmount = "3000",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                ),
                "senior_citizen_discount" to Benefit(
                    id = "senior_citizen_discount",
                    title = "Senior Citizen Discount",
                    category = "Utility Discounts",
                    amount = "20%",
                    status = "Available",
                    description = "20% discount on utilities, medicines, and transportation for senior citizens",
                    requirements = "Senior Citizen ID, valid ID",
                    applicationProcess = "Present Senior Citizen ID at participating establishments",
                    contactInfo = "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                    website = "",
                    isActive = true,
                    nextDisbursementDate = null,
                    disbursementAmount = "0",
                    createdBy = adminUserId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )

            val benefitsRef = database.child(BENEFITS_PATH)
            benefitsRef.setValue(davaoCityBenefits).await()
            
            Log.d(TAG, "Davao City benefits added to Realtime Database by admin $adminUserId")
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding Davao City benefits to Realtime Database", e)
            Result.Error(Exception("Failed to add Davao City benefits: ${e.message}"))
        }
    }
}