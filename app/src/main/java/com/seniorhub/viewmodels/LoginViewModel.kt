package com.seniorhub.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.seniorhub.models.User
import com.seniorhub.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * LoginViewModel - Authentication Management
 *
 * This ViewModel manages the authentication state and user login process,
 * including form validation and error handling for senior users.
 */
class LoginViewModel(
    private val auth: FirebaseAuth = Firebase.auth,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _loginState = MutableLiveData<Result<User>>()
    val loginState: LiveData<Result<User>> = _loginState

    private val _uiState = MutableLiveData<LoginUiState>()
    val uiState: LiveData<LoginUiState> = _uiState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        object Success : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_EMAIL_LENGTH = 254
        
        // Error messages
        private const val EMAIL_VALIDATION_ERROR = "Please enter a valid email address"
        private const val EMPTY_EMAIL_ERROR = "Email cannot be empty"
        private const val EMPTY_PASSWORD_ERROR = "Password cannot be empty"
        private const val SHORT_PASSWORD_ERROR = "Password must be at least $MIN_PASSWORD_LENGTH characters"
        private const val INVALID_CREDENTIALS = "Invalid email or password"
        private const val TOO_MANY_REQUESTS = "Too many login attempts. Please try again later."
    }

    /**
     * Attempt to log in with email and password
     *
     * @param email User's email address
     * @param password User's password
     */
    fun login(email: String, password: String) {
        if (!validateInput(email, password)) {
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                // Sign in with Firebase Authentication
                val authResult = withContext(dispatcher) {
                    auth.signInWithEmailAndPassword(email, password).await()
                }

                // Get user data
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    _uiState.value = LoginUiState.Error(INVALID_CREDENTIALS)
                    return@launch
                }

                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
                    lastName = firebaseUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified
                )

                _loginState.value = Result.Success(user)
                _uiState.value = LoginUiState.Success
                _errorMessage.value = null
                
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email"
                    is FirebaseAuthInvalidCredentialsException -> INVALID_CREDENTIALS
                    is FirebaseNetworkException -> "Network error. Please check your connection."
                    else -> "Login failed: ${e.message ?: "Unknown error"}"
                }
                _uiState.value = LoginUiState.Error(errorMessage)
                _errorMessage.value = errorMessage
                _loginState.value = Result.Error(e)
            }
        }
    }

    /**
     * Send password reset email
     *
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            if (email.isBlank()) {
                return Result.Error(IllegalArgumentException(EMPTY_EMAIL_ERROR))
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.Error(IllegalArgumentException(EMAIL_VALIDATION_ERROR))
            }
            
            withContext(dispatcher) {
                auth.sendPasswordResetEmail(email).await()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "No account found with this email"
                is FirebaseNetworkException -> "Network error. Please check your connection."
                else -> "Failed to send password reset email: ${e.message ?: "Unknown error"}"
            }
            Result.Error(Exception(errorMessage, e))
        }
    }

    /**
     * Check if user is already authenticated and email is verified
     *
     * @return Pair of (isAuthenticated, isEmailVerified)
     */
    fun checkAuthState(): Pair<Boolean, Boolean> {
        val user = auth.currentUser
        return (user != null) to (user?.isEmailVerified == true)
    }

    /**
     * Clear any error messages and reset UI state
     */
    fun resetState() {
        _errorMessage.value = null
        _uiState.value = LoginUiState.Idle
    }

    /**
     * Sign out current user
     */
    fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            _loginState.value = Result.Error(Exception("User signed out"))
            resetState()
            Result.Success(Unit)
        } catch (e: Exception) {
            val error = Exception("Failed to sign out: ${e.message}", e)
            _errorMessage.value = error.message
            Result.Error(error)
        }
    }

    /**
     * Validate login input
     * @return true if input is valid, false otherwise
     */
    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _errorMessage.value = EMPTY_EMAIL_ERROR
                false
            }
            email.length > MAX_EMAIL_LENGTH || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _errorMessage.value = EMAIL_VALIDATION_ERROR
                false
            }
            password.isBlank() -> {
                _errorMessage.value = EMPTY_PASSWORD_ERROR
                false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                _errorMessage.value = SHORT_PASSWORD_ERROR
                false
            }
            else -> true
        }
    }

    /**
     * Get user-friendly error message from exception
     */
    private fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found with this email"
            is FirebaseAuthInvalidCredentialsException -> INVALID_CREDENTIALS
            is FirebaseNetworkException -> "Network error. Please check your connection."
            else -> exception.message ?: "An unknown error occurred"
        }
    }
    
    /**
     * Clean up resources when ViewModel is no longer in use
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing coroutines if needed
    }
    }