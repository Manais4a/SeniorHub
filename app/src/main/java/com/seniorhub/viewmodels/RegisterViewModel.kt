package com.seniorhub.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.seniorhub.models.User
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * RegisterViewModel - User Registration Management
 *
 * This ViewModel manages the registration process including:
 * - Form validation
 * - Firebase Authentication
 * - User profile creation in Firestore
 */
class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository: UserRepository = UserRepository.getInstance()

    private val _registrationState = MutableLiveData<Result<User>>()
    val registrationState: LiveData<Result<User>> = _registrationState

    companion object {
        // Default user settings for new registrations
        private const val DEFAULT_TEXT_SIZE = 16f
        private const val DEFAULT_HIGH_CONTRAST = false
        private const val DEFAULT_VOICE_ASSISTANCE = true
        private const val DEFAULT_LANGUAGE = "en"

        // Error messages
        private const val EMAIL_ALREADY_REGISTERED = "This email is already registered"
        private const val WEAK_PASSWORD_ERROR =
            "Password is too weak. Please choose a stronger password."
        private const val INVALID_EMAIL_ERROR = "Invalid email format. Please check and try again."
        private const val USER_CREATION_FAILED = "Failed to create user account"
        private const val DATABASE_SAVE_FAILED = "Failed to save user profile"
    }

    /**
     * Register a new user with email and password
     *
     * @param firstName User's first name
     * @param lastName User's last name
     * @param email User's email address
     * @param password User's password
     */
    fun register(firstName: String, lastName: String, email: String, password: String) {
        _registrationState.value = Result.Loading()

        viewModelScope.launch {
            try {
                // 1. Create user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                val userId = authResult.user?.uid ?: throw Exception(USER_CREATION_FAILED)

                // 2. Create user profile in database
                val user = createNewUser(userId, firstName, lastName, email)

                // 3. Save user profile to database
                when (val result = userRepository.saveUser(user)) {
                    is Result.Success -> {
                        // 4. Send email verification
                        sendEmailVerification()
                        _registrationState.value = Result.Success(user)
                    }
                    is Result.Error -> {
                        // If saving to database fails, delete the auth account to keep data consistent
                        deleteAuthAccount()
                        _registrationState.value = Result.Error(
                            Exception(DATABASE_SAVE_FAILED + ": ${result.exception.message}")
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                    }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _registrationState.value = Result.Error(Exception(EMAIL_ALREADY_REGISTERED))
            } catch (e: FirebaseAuthWeakPasswordException) {
                _registrationState.value = Result.Error(Exception(WEAK_PASSWORD_ERROR))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _registrationState.value = Result.Error(Exception(INVALID_EMAIL_ERROR))
            } catch (e: Exception) {
                _registrationState.value = Result.Error(e)
            }
        }
    }

    /**
     * Check if an email is already registered
     *
     * @param email Email address to check
     * @return Boolean indicating if email is already registered
     */
    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            // Try to fetch sign-in methods for the email
            val signInMethods = auth.fetchSignInMethodsForEmail(email).await()
            signInMethods.signInMethods?.isNotEmpty() ?: false
        } catch (e: Exception) {
            // If there's an error (e.g., email not found), return false
            false
        }
    }

    /**
     * Create a new User object with default settings
     */
    private fun createNewUser(
        userId: String,
        firstName: String,
        lastName: String,
        email: String
    ): User {
        return User(
            id = userId,
            firstName = firstName,
            lastName = lastName,
            email = email,
            // Set default values for new users
            preferredLanguage = DEFAULT_LANGUAGE,
            textSize = DEFAULT_TEXT_SIZE,
            highContrastMode = DEFAULT_HIGH_CONTRAST,
            // Initialize other required fields with default values
            voiceAssistanceEnabled = DEFAULT_VOICE_ASSISTANCE,
            isEmailVerified = false
        )
    }

    /**
     * Send email verification to the current user
     */
    private suspend fun sendEmailVerification() {
        try {
            auth.currentUser?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            // Log error but don't fail registration
            // In a real app, you might want to show a non-blocking message
        }
    }

    /**
     * Delete the Firebase auth account (used for cleanup on failure)
     */
    private suspend fun deleteAuthAccount() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (e: Exception) {
            // Log error but don't throw - this is cleanup
        }
    }

    /**
     * Clear registration state
     */
    fun clearRegistrationState() {
        _registrationState.value = Result.Success(null)
    }
}