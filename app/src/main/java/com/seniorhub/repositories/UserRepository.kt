package com.seniorhub.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.models.User
import com.seniorhub.models.EmergencyContact
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Wrapper class for nullable User results
 */
data class UserResult(val user: User?)

/**
 * UserRepository - User Management Repository
 *
 * Handles all user-related database operations:
 * - User profile management (CRUD operations)
 * - Emergency contacts management
 * - User authentication state management
 */
class UserRepository private constructor() {

    companion object {
        private const val TAG = "UserRepository"
        private const val USERS_COLLECTION = "users"

        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository().also { INSTANCE = it }
            }
        }
    }

    // Firebase Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    
    // Simple in-memory storage for demo purposes (keeping for fallback)
    private val userCache = mutableMapOf<String, User>()

    /**
     * Parse emergency contacts from Firebase data
     */
    private fun parseEmergencyContacts(contactsData: List<Map<String, Any>>?): List<EmergencyContact> {
        return contactsData?.mapNotNull { contactMap ->
            try {
                EmergencyContact(
                    id = contactMap["id"] as? String ?: "",
                    name = contactMap["name"] as? String ?: "",
                    phoneNumber = contactMap["phoneNumber"] as? String ?: "",
                    relationship = contactMap["relationship"] as? String ?: "",
                    email = contactMap["email"] as? String ?: "",
                    address = contactMap["address"] as? String ?: "",
                    isPrimary = contactMap["isPrimary"] as? Boolean ?: false,
                    isActive = contactMap["isActive"] as? Boolean ?: true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing emergency contact: ${e.message}")
                null
            }
        } ?: emptyList()
    }

    /**
     * Save user profile
     *
     * @param user User object to save
     * @return Result indicating success or failure
     */
    suspend fun saveUser(user: User): Result<User> {
        return try {
            Log.d(TAG, "Saving user to Firebase: ${user.id}")

            // Update timestamp
            user.updatedAt = Timestamp.now()

            // Save to Firebase Firestore
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(user.toMap())
                .await()

            // Also save to cache for offline access
            userCache[user.id] = user

            Log.i(TAG, "User saved successfully to Firebase: ${user.id}")
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user to Firebase: ${e.message}", e)
            // Fallback to cache if Firebase fails
            try {
                userCache[user.id] = user
                Log.w(TAG, "Saved to cache as fallback")
                Result.Success(user)
            } catch (cacheError: Exception) {
                Log.e(TAG, "Error saving to cache: ${cacheError.message}", cacheError)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Get user by ID
     *
     * @param userId User ID to retrieve
     * @return Result with UserResult wrapper
     */
    suspend fun getUserById(userId: String): Result<UserResult> {
        return try {
            Log.d(TAG, "Getting user by ID from Firebase: $userId")

            // Try to get from cache first for performance
            val cachedUser = userCache[userId]
            if (cachedUser != null) {
                Log.d(TAG, "User retrieved from cache: ${cachedUser.firstName} ${cachedUser.lastName}")
                return Result.Success(UserResult(cachedUser))
            }

            // Fetch from Firebase Firestore
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data
                Log.d(TAG, "User document exists, parsing data...")
                
                val user = User(
                    id = documentSnapshot.id,
                    firstName = userData?.get("firstName") as? String ?: "",
                    lastName = userData?.get("lastName") as? String ?: "",
                    birthDate = (userData?.get("birthDate") as? Timestamp)?.toDate(),
                    gender = userData?.get("gender") as? String ?: "",
                    profileImageUrl = userData?.get("profileImageUrl") as? String ?: "",
                    username = userData?.get("username") as? String ?: "",
                    phoneNumber = userData?.get("phoneNumber") as? String ?: "",
                    email = userData?.get("email") as? String ?: "",
                    houseNumberAndStreet = userData?.get("houseNumberAndStreet") as? String ?: "",
                    barangay = userData?.get("barangay") as? String ?: "",
                    city = userData?.get("city") as? String ?: "Davao City",
                    province = userData?.get("province") as? String ?: "Davao Del Sur",
                    zipCode = userData?.get("zipCode") as? String ?: "8000",
                    maritalStatus = userData?.get("maritalStatus") as? String ?: "",
                    sssNumber = userData?.get("sssNumber") as? String ?: "",
                    gsisNumber = userData?.get("gsisNumber") as? String ?: "",
                    oscaNumber = userData?.get("oscaNumber") as? String ?: "",
                    philHealthNumber = userData?.get("philHealthNumber") as? String ?: "",
                    emergencyContacts = parseEmergencyContacts(userData?.get("emergencyContacts") as? List<Map<String, Any>>),
                    age = (userData?.get("age") as? Long)?.toInt() ?: 0
                )

                // Cache the user for future access
                userCache[userId] = user

                Log.d(TAG, "User retrieved from Firebase: ${user.firstName} ${user.lastName}")
                Log.d(TAG, "Profile image URL: ${user.profileImageUrl}")
                Result.Success(UserResult(user))
            } else {
                Log.w(TAG, "User document not found in Firebase: $userId")
                Result.Success(UserResult(null))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user from Firebase: ${e.message}", e)
            // Fallback to cache
            val cachedUser = userCache[userId]
            if (cachedUser != null) {
                Log.w(TAG, "Using cached user as fallback")
                Result.Success(UserResult(cachedUser))
            } else {
                Result.Error(e)
            }
        }
    }
    
    /**
     * Get current user profile
     *
     * @return Result with UserResult wrapper
     */
    suspend fun getCurrentUser(): Result<UserResult> {
        val currentUserId = FirebaseManager.getCurrentUserId()
        return if (currentUserId != null) {
            getUserById(currentUserId)
        } else {
            Log.w(TAG, "No current user logged in")
            Result.Success(UserResult(null))
        }
    }

    /**
     * Update user profile
     *
     * @param user Updated user object
     * @return Result indicating success or failure
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            Log.d(TAG, "Updating user in Firebase: ${user.id}")

            // Update timestamp
            user.updatedAt = Timestamp.now()

            // Update in Firebase Firestore
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(user.toMap())
                .await()

            // Also update cache
            userCache[user.id] = user

            Log.i(TAG, "User updated successfully in Firebase: ${user.id}")
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user in Firebase: ${e.message}", e)
            // Fallback to cache if Firebase fails
            try {
                userCache[user.id] = user
                Log.w(TAG, "Updated in cache as fallback")
                Result.Success(user)
            } catch (cacheError: Exception) {
                Log.e(TAG, "Error updating in cache: ${cacheError.message}", cacheError)
                Result.Error(e)
            }
        }
    }
    
    /**
     * Update specific user fields
     *
     * @param userId User ID to update
     * @param updates Map of field updates
     * @return Result indicating success or failure
     */
    suspend fun updateUserFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            Log.d(TAG, "Updating user fields: $userId")

            val user = userCache[userId]
            if (user != null) {
                // Apply updates to user object
                updates.forEach { (key, value) ->
                    when (key) {
                        "firstName" -> user.firstName = value as String
                        "lastName" -> user.lastName = value as String
                        "phoneNumber" -> user.phoneNumber = value as String
                        "email" -> user.email = value as String
                        "houseNumberAndStreet" -> user.houseNumberAndStreet = value as String
                        "barangay" -> user.barangay = value as String
                        "city" -> user.city = value as String
                        "province" -> user.province = value as String
                        "zipCode" -> user.zipCode = value as String
                        "maritalStatus" -> user.maritalStatus = value as String
                        "sssNumber" -> user.sssNumber = value as String
                        "gsisNumber" -> user.gsisNumber = value as String
                        "oscaNumber" -> user.oscaNumber = value as String
                        "philHealthNumber" -> user.philHealthNumber = value as String
                        "textSize" -> user.textSize = value as Float
                        "highContrastMode" -> user.highContrastMode = value as Boolean
                        "voiceAssistanceEnabled" -> user.voiceAssistanceEnabled = value as Boolean
                    }
                }

                user.updatedAt = Timestamp.now()
                userCache[userId] = user
            }

            Log.i(TAG, "User fields updated successfully: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user fields: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Delete user profile
     *
     * @param userId User ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting user: $userId")

            userCache.remove(userId)

            Log.i(TAG, "User deleted successfully: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Check if user exists
     *
     * @param userId User ID to check
     * @return Result with boolean indicating existence
     */
    suspend fun userExists(userId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Checking if user exists: $userId")

            val exists = userCache.containsKey(userId)

            Log.d(TAG, "User exists: $exists")
            Result.Success(exists)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Add emergency contact to user
     *
     * @param userId User ID
     * @param emergencyContact EmergencyContact to add
     * @return Result indicating success or failure
     */
    suspend fun addEmergencyContact(
        userId: String,
        emergencyContact: EmergencyContact
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Adding emergency contact for user: $userId")

            // Get user from Firebase or cache
            val userResult = getUserById(userId)
            if (userResult is Result.Success && userResult.data?.user != null) {
                val user = userResult.data.user!!
                val updatedContacts = user.emergencyContacts.toMutableList()

                // If this is primary, remove primary flag from others
                if (emergencyContact.isPrimary) {
                    updatedContacts.forEach { it.isPrimary = false }
                }

                updatedContacts.add(emergencyContact)
                user.emergencyContacts = updatedContacts

                // Update in Firebase
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(user.toMap())
                    .await()

                // Also update cache
                userCache[userId] = user

                Log.i(TAG, "Emergency contact added successfully")
                Result.Success(Unit)
            } else {
                Log.e(TAG, "User not found: $userId")
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding emergency contact: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Remove emergency contact from user
     *
     * @param userId User ID
     * @param contactId Emergency contact ID/phone to remove
     * @return Result indicating success or failure
     */
    suspend fun removeEmergencyContact(userId: String, contactId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Removing emergency contact for user: $userId")

            // Get user from Firebase or cache
            val userResult = getUserById(userId)
            if (userResult is Result.Success && userResult.data?.user != null) {
                val user = userResult.data.user!!
                val updatedContacts = user.emergencyContacts.filterNot {
                    it.phoneNumber == contactId || it.name == contactId
                }

                user.emergencyContacts = updatedContacts

                // Update in Firebase
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(user.toMap())
                    .await()

                // Also update cache
                userCache[userId] = user

                Log.i(TAG, "Emergency contact removed successfully")
                Result.Success(Unit)
            } else {
                Log.e(TAG, "User not found: $userId")
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing emergency contact: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Get user by email (for login purposes)
     *
     * @param email Email to search for
     * @return Result with UserResult wrapper
     */
    suspend fun getUserByEmail(email: String): Result<UserResult> {
        return try {
            Log.d(TAG, "Getting user by email: $email")

            val user = userCache.values.find { it.email == email }
            Log.d(TAG, "User found by email: ${user?.firstName}")
            Result.Success(UserResult(user))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by email: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Update user's last login timestamp
     *
     * @param userId User ID
     * @return Result indicating success or failure
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Updating last login for user: $userId")

            val user = userCache[userId]
            if (user != null) {
                user.lastLogin = Timestamp.now()
                userCache[userId] = user
            }

            Log.d(TAG, "Last login updated successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last login: ${e.message}", e)
            Result.Error(e)
        }
    }
}