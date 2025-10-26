package com.seniorhub.repositories

import android.util.Log
import com.google.firebase.database.*
import com.seniorhub.models.Benefit
import com.seniorhub.utils.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * SimpleBenefitsRepository - Real-time Benefits Data Management
 * 
 * Features:
 * - Real-time data synchronization with Firebase Realtime Database
 * - Automatic updates when data changes
 * - Offline support with local caching
 * - No FCM dependencies - uses only Realtime Database
 */
class SimpleBenefitsRepository private constructor() {

    companion object {
        private const val TAG = "SimpleBenefitsRepository"
        private const val BENEFITS_PATH = "benefits"
        
        @Volatile
        private var INSTANCE: SimpleBenefitsRepository? = null

        fun getInstance(): SimpleBenefitsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimpleBenefitsRepository().also { INSTANCE = it }
            }
        }
    }

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val benefitsRef: DatabaseReference = database.getReference(BENEFITS_PATH)
    
    // Local cache for offline support
    private val benefitsCache = mutableListOf<Benefit>()

    /**
     * Get real-time stream of benefits data
     */
    fun getBenefitsStream(): Flow<Result<List<Benefit>>> = callbackFlow {
        Log.d(TAG, "Starting benefits stream")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val benefits = mutableListOf<Benefit>()
                    
                    for (child in snapshot.children) {
                        try {
                            val benefit = child.getValue(Benefit::class.java)
                            benefit?.let {
                                it.id = child.key ?: ""
                                benefits.add(it)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing benefit: ${e.message}")
                        }
                    }
                    
                    // Sort by title
                    benefits.sortBy { it.title }
                    
                    // Update cache
                    benefitsCache.clear()
                    benefitsCache.addAll(benefits)
                    
                    // Emit success result
                    trySend(Result.Success(benefits))
                    Log.d(TAG, "Benefits stream updated: ${benefits.size} benefits")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing benefits data: ${e.message}")
                    trySend(Result.Error(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Benefits stream cancelled: ${error.message}")
                trySend(Result.Error(Exception(error.message)))
            }
        }
        
        benefitsRef.addValueEventListener(listener)
        
        awaitClose {
            benefitsRef.removeEventListener(listener)
        }
    }

    /**
     * Get available benefits (active only)
     */
    suspend fun getAvailableBenefits(): Result<List<Benefit>> {
        return try {
            val snapshot = benefitsRef.get().await()
            val benefits = mutableListOf<Benefit>()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        it.id = child.key ?: ""
                        if (it.isActive) {
                            benefits.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            // Sort by title
            benefits.sortBy { it.title }
            
            // Update cache
            benefitsCache.clear()
            benefitsCache.addAll(benefits)
            
            Log.d(TAG, "Retrieved ${benefits.size} available benefits")
            Result.Success(benefits)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available benefits: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Get benefits by category
     */
    suspend fun getBenefitsByCategory(category: String): Result<List<Benefit>> {
        return try {
            val snapshot = benefitsRef.orderByChild("category").equalTo(category).get().await()
            val benefits = mutableListOf<Benefit>()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        it.id = child.key ?: ""
                        if (it.isActive) {
                            benefits.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            Log.d(TAG, "Retrieved ${benefits.size} benefits for category: $category")
            Result.Success(benefits)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting benefits by category: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Search benefits by title or description
     */
    suspend fun searchBenefits(query: String): Result<List<Benefit>> {
        return try {
            val snapshot = benefitsRef.get().await()
            val benefits = mutableListOf<Benefit>()
            val searchQuery = query.lowercase()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        it.id = child.key ?: ""
                        if (it.isActive && (
                            it.title.lowercase().contains(searchQuery) ||
                            it.description.lowercase().contains(searchQuery) ||
                            it.category.lowercase().contains(searchQuery)
                        )) {
                            benefits.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            Log.d(TAG, "Found ${benefits.size} benefits matching query: $query")
            Result.Success(benefits)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching benefits: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Get cached benefits (for offline use)
     */
    fun getCachedBenefits(): List<Benefit> {
        return benefitsCache.toList()
    }



    /**
     * Delete a benefit (for admin use)
     */
    suspend fun deleteBenefit(benefitId: String): Result<Unit> {
        return try {
            benefitsRef.child(benefitId).removeValue().await()
            
            Log.d(TAG, "Benefit deleted successfully: $benefitId")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting benefit: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Get all benefits (including inactive ones)
     */
    suspend fun getAllBenefits(): Result<List<Benefit>> {
        return try {
            val snapshot = benefitsRef.get().await()
            val benefits = mutableListOf<Benefit>()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        it.id = child.key ?: ""
                        benefits.add(it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            // Sort by title
            benefits.sortBy { it.title }
            
            Log.d(TAG, "Retrieved ${benefits.size} total benefits")
            Result.Success(benefits)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all benefits: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Get benefits by status
     */
    suspend fun getBenefitsByStatus(status: String): Result<List<Benefit>> {
        return try {
            val snapshot = benefitsRef.orderByChild("status").equalTo(status).get().await()
            val benefits = mutableListOf<Benefit>()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        it.id = child.key ?: ""
                        benefits.add(it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            Log.d(TAG, "Retrieved ${benefits.size} benefits with status: $status")
            Result.Success(benefits)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting benefits by status: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Toggle benefit active status
     */
    suspend fun toggleBenefitStatus(benefitId: String, isActive: Boolean): Result<Unit> {
        return try {
            val updates = mapOf("isActive" to isActive, "updatedAt" to Date())
            benefitsRef.child(benefitId).updateChildren(updates).await()
            
            Log.d(TAG, "Benefit status toggled: $benefitId -> $isActive")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling benefit status: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Get benefits count by category
     */
    suspend fun getBenefitsCountByCategory(): Result<Map<String, Int>> {
        return try {
            val snapshot = benefitsRef.get().await()
            val categoryCount = mutableMapOf<String, Int>()
            
            for (child in snapshot.children) {
                try {
                    val benefit = child.getValue(Benefit::class.java)
                    benefit?.let {
                        val category = it.category
                        categoryCount[category] = (categoryCount[category] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing benefit: ${e.message}")
                }
            }
            
            Log.d(TAG, "Retrieved benefits count by category: $categoryCount")
            Result.Success(categoryCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting benefits count by category: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        benefitsCache.clear()
        Log.d(TAG, "Benefits cache cleared")
    }

    /**
     * Get cache size
     */
    fun getCacheSize(): Int {
        return benefitsCache.size
    }
}