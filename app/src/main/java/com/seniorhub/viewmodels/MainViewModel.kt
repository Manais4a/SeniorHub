package com.seniorhub.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seniorhub.models.User
import com.seniorhub.repositories.UserRepository
import com.seniorhub.utils.FirebaseManager
import com.seniorhub.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SingleLiveEvent for one-time navigation events
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    fun call() {
        value = null
    }
}

/**
 * MainViewModel - Main Screen Business Logic
 *
 * Handles the business logic and data operations for the main screen,
 * including user data management and navigation state for senior users.
 */
class MainViewModel(
    private val userRepository: UserRepository = UserRepository.getInstance(),
    private val firebaseManager: FirebaseManager = FirebaseManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _navigationEvent = SingleLiveEvent<Screen>()
    val navigationEvent: LiveData<Screen> = _navigationEvent

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _navigateToScreen = MutableLiveData<Screen?>()
    val navigateToScreen: LiveData<Screen?> = _navigateToScreen

    sealed class MainUiState {
        object Loading : MainUiState()
        data class Success(val user: User) : MainUiState()
        data class Error(val message: String) : MainUiState()
        object Unauthenticated : MainUiState()
    }

    /**
     * Navigation destinations for the main screen
     */
    sealed class Screen(val route: String) {
        object Profile : Screen("profile")
        object Emergency : Screen("emergency")
        object Reminders : Screen("reminders")
        object Social : Screen("social")
        object Settings : Screen("settings")
        object Login : Screen("login")
        object Health : Screen("health")
        object Benefits : Screen("benefits")

        companion object {
            fun fromRoute(route: String?): Screen? {
                return when (route?.substringBefore("/")) {
                    Profile.route -> Profile
                    Emergency.route -> Emergency
                    Reminders.route -> Reminders
                    Social.route -> Social
                    Settings.route -> Settings
                    Login.route -> Login
                    Health.route -> Health
                    Benefits.route -> Benefits
                    else -> null
                }
            }
        }
    }

    companion object {
        // Accessibility constants
        private const val MIN_TEXT_SIZE = 12f
        private const val MAX_TEXT_SIZE = 24f
        private const val DEFAULT_TEXT_SIZE = 16f

        // Error messages
        private const val ERROR_LOADING_USER = "Error loading user data"
        private const val ERROR_UPDATE_VOICE = "Failed to update voice assistance setting"
        private const val ERROR_UPDATE_TEXT_SIZE = "Failed to update text size"
        private const val ERROR_UPDATE_CONTRAST = "Failed to update high contrast mode"
        private const val ERROR_USER_NOT_FOUND = "User not found"
        private const val ERROR_NETWORK = "Network error. Please check your connection."
    }

    init {
        loadCurrentUser()
    }

    /**
     * Load the current user's data from the repository
     */
    private fun loadCurrentUser() {
        val currentUserId = firebaseManager.getCurrentUserId()
        if (currentUserId == null) {
            _uiState.value = MainUiState.Unauthenticated
            _navigationEvent.value = Screen.Login
            return
        }

        _uiState.value = MainUiState.Loading

        viewModelScope.launch {
            try {
                when (val result = withContext(dispatcher) {
                    userRepository.getUserById(currentUserId)
                }) {
                    is Result.Success -> {
                        val user = result.data?.user ?: throw IllegalStateException(ERROR_USER_NOT_FOUND)
                        _user.value = user
                        applyUserPreferences(user)
                        _uiState.value = MainUiState.Success(user)
                    }
                    is Result.Error -> {
                        _uiState.value = MainUiState.Error(
                            result.exception.message ?: ERROR_LOADING_USER
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = MainUiState.Loading
                    }
                }
            } catch (e: Exception) {
                val errorMessage = if (e is java.net.UnknownHostException) {
                    ERROR_NETWORK
                } else {
                    e.message ?: ERROR_LOADING_USER
                }
                _uiState.value = MainUiState.Error(errorMessage)
            }
        }
    }

    /**
     * Apply user preferences (text size, contrast, etc.)
     *
     * @param user User object containing preferences
     */
    private fun applyUserPreferences(user: User) {
        // Use existing User fields
        val textSize = user.textSize ?: DEFAULT_TEXT_SIZE
        val highContrast = user.highContrastMode ?: false
        val voiceAssistance = user.voiceAssistanceEnabled ?: false

        // Apply these settings to the app
        // AccessibilityManager.applyAccessibilitySettings(
        //     textSize = textSize,
        //     highContrast = highContrast,
        //     voiceAssistance = voiceAssistance
        // )
    }

    /**
     * Handle navigation to different screens based on menu selection
     *
     * @param route The route to navigate to
     */
    fun onNavigationItemSelected(route: String) {
        val screen = Screen.fromRoute(route) ?: return
        _navigationEvent.value = screen
    }

    /**
     * Handle back navigation
     */
    fun onBackPressed(): Boolean {
        // Add any back press logic here
        // Return true if the back press was handled, false otherwise
        return false
    }

    /**
     * Handle emergency button click
     */
    fun onEmergencyClicked() {
        // In a real app, this would trigger emergency protocols
        _navigationEvent.value = Screen.Emergency

        // Additional emergency handling logic
        viewModelScope.launch {
            try {
                // Notify emergency contacts
                // userRepository.notifyEmergencyContacts()

                // Log emergency event
                // analytics.logEvent("emergency_button_pressed", bundleOf(
                //     "timestamp" to System.currentTimeMillis(),
                //     "user_id" to _user.value?.id
                // ))
            } catch (e: Exception) {
                // Handle error silently in emergency case
            }
        }
    }

    /**
     * Sign out the current user and navigate to login
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                // Clear any local data
                // userRepository.clearUserData()

                // Sign out from Firebase
                firebaseManager.signOut()

                // Update UI state
                _user.value = null
                _uiState.value = MainUiState.Unauthenticated
                _navigationEvent.value = Screen.Login

            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Failed to sign out: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Refresh user data from the repository
     */
    fun refreshUserData() {
        loadCurrentUser()
    }

    /**
     * Clean up resources when ViewModel is no longer in use
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing coroutines if needed
    }

    /**
     * Toggle voice assistance for the current user
     *
     * @param enabled Whether voice assistance should be enabled
     */
    fun toggleVoiceAssistance(enabled: Boolean) {
        val userId = _user.value?.id ?: return
        viewModelScope.launch {
            try {
                userRepository.updateUserFields(
                    userId,
                    mapOf("voiceAssistanceEnabled" to enabled)
                )
                // Update local user object
                _user.value = _user.value?.copy(voiceAssistanceEnabled = enabled)
            } catch (e: Exception) {
                _error.value = ERROR_UPDATE_VOICE
            }
        }
    }

    /**
     * Update user's text size preference
     *
     * @param textSize New text size value
     */
    fun updateTextSize(textSize: Float) {
        val userId = _user.value?.id ?: return
        val validatedTextSize = textSize.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)

        viewModelScope.launch {
            try {
                userRepository.updateUserFields(
                    userId,
                    mapOf("textSize" to validatedTextSize)
                )
                // Update local user object
                _user.value = _user.value?.copy(textSize = validatedTextSize)
            } catch (e: Exception) {
                _error.value = ERROR_UPDATE_TEXT_SIZE
            }
        }
    }

    /**
     * Toggle high contrast mode for the current user
     *
     * @param enabled Whether high contrast mode should be enabled
     */
    fun toggleHighContrastMode(enabled: Boolean) {
        val userId = _user.value?.id ?: return
        viewModelScope.launch {
            try {
                userRepository.updateUserFields(
                    userId,
                    mapOf("highContrastMode" to enabled)
                )
                // Update local user object
                _user.value = _user.value?.copy(highContrastMode = enabled)
            } catch (e: Exception) {
                _error.value = ERROR_UPDATE_CONTRAST
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = ""
    }

    /**
     * Handle specific screen navigation
     */
    fun navigateToScreen(screen: Screen) {
        _navigateToScreen.value = screen
    }

    /**
     * Clear navigation event after handling
     */
    fun onNavigationHandled() {
        _navigateToScreen.value = null
    }

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return FirebaseManager.isUserLoggedIn()
    }

    /**
     * Get current user's display name
     */
    fun getUserDisplayName(): String {
        val user = _user.value
        return when {
            user != null -> "${user.firstName} ${user.lastName}".trim()
            else -> "User"
        }
    }

    /**
     * Check if user data is loaded
     */
    fun isUserDataLoaded(): Boolean {
        return _user.value != null
    }
}