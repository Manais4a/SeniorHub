package com.seniorhub.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.seniorhub.R
import com.seniorhub.models.SocialFeature
import com.seniorhub.utils.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.seniorhub.models.SocialFeatureType


/**
 * SocialViewModel - Social Features Management
 *
 * This ViewModel manages social features and settings for senior users,
 * including messaging, video calls, events, and community groups.
 */
class SocialViewModel : ViewModel() {

    private val _socialFeatures = MutableLiveData<List<SocialFeature>>()
    val socialFeatures: LiveData<List<SocialFeature>> = _socialFeatures

    private val _loadingState = MutableLiveData<Result<Unit>?>()
    val loadingState: LiveData<Result<Unit>?> = _loadingState

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val featuresCollection = db.collection("social_features")

    companion object {
        // Feature types
        private const val FEATURE_MESSAGES = "messages"
        private const val FEATURE_VIDEO_CALL = "video_call"
        private const val FEATURE_EVENTS = "events"
        private const val FEATURE_GROUPS = "groups"

        // Error messages
        private const val ERROR_USER_NOT_AUTHENTICATED = "User not authenticated"
        private const val ERROR_LOADING_FEATURES = "Failed to load social features"
        private const val ERROR_UPDATING_FEATURE = "Failed to update feature setting"

        // Sample user IDs for demonstration
        private const val SAMPLE_USER_1 = "user1"
        private const val SAMPLE_USER_2 = "user2"
        private const val SAMPLE_USER_3 = "user3"
        private const val SAMPLE_USER_4 = "user4"
        private const val SAMPLE_USER_5 = "user5"
        private const val SAMPLE_USER_6 = "user6"
        private const val SAMPLE_USER_7 = "user7"
        private const val SAMPLE_USER_8 = "user8"
    }

    init {
        loadSocialFeatures()
    }

    /**
     * Load available social features for the current user
     */
    fun loadSocialFeatures() {
        viewModelScope.launch {
            try {
                _loadingState.value = Result.Loading()

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _loadingState.value = Result.Error(Exception("User not authenticated"))
                    return@launch
                }

                // In a real app, we would fetch from database
                // For now, we'll use sample data
                val features = getSampleFeatures()
                _socialFeatures.value = features
                _loadingState.value = Result.Success(Unit)

            } catch (e: Exception) {
                _loadingState.value = Result.Error(e)
            }
        }
    }

    /**
     * Generate sample social features for demonstration
     *
     * @return List of sample social features
     */
    private fun getSampleFeatures(): List<SocialFeature> {
        return listOf(
            createMessagesFeature(),
            createVideoCallFeature(),
            createEventsFeature(),
            createGroupsFeature()
        )
    }

    /**
     * Create messages feature
     */
    private fun createMessagesFeature(): SocialFeature {
        return SocialFeature(
            id = FEATURE_MESSAGES,
            title = "Messages",
            description = "Chat with family and friends",
            type = SocialFeatureType.MESSAGES,
            iconResId = R.drawable.ic_message,
            unreadCount = 3,
            participants = listOf(SAMPLE_USER_1, SAMPLE_USER_2, SAMPLE_USER_3),
            isEnabled = true
        )
    }

    /**
     * Create video call feature
     */
    private fun createVideoCallFeature(): SocialFeature {
        return SocialFeature(
            id = FEATURE_VIDEO_CALL,
            title = "Video Calls",
            description = "Make video calls to your loved ones",
            type = SocialFeatureType.VIDEO_CALL,
            iconResId = R.drawable.ic_video_call,
            unreadCount = 0,
            participants = listOf(SAMPLE_USER_1, SAMPLE_USER_4),
            isEnabled = true
        )
    }

    /**
     * Create events feature
     */
    private fun createEventsFeature(): SocialFeature {
        return SocialFeature(
            id = FEATURE_EVENTS,
            title = "Events",
            description = "View and manage upcoming events",
            type = SocialFeatureType.EVENTS,
            iconResId = R.drawable.ic_event,
            unreadCount = 0,
            participants = listOf(SAMPLE_USER_1, SAMPLE_USER_2, SAMPLE_USER_5, SAMPLE_USER_6),
            isEnabled = true
        )
    }

    /**
     * Create groups feature
     */
    private fun createGroupsFeature(): SocialFeature {
        return SocialFeature(
            id = FEATURE_GROUPS,
            title = "Groups",
            description = "Join community groups",
            type = SocialFeatureType.GROUPS,
            iconResId = R.drawable.ic_group,
            unreadCount = 0,
            participants = listOf(SAMPLE_USER_1, SAMPLE_USER_7, SAMPLE_USER_8),
            isEnabled = true
        )
    }

    /**
     * Update the enabled status of a social feature
     *
     * @param featureId ID of the feature to update
     * @param isEnabled Whether the feature should be enabled
     */
    fun updateFeatureEnabled(featureId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // In a real app, update the setting in database
                    // featuresCollection.document(featureId)
                    //     .update("enabled", isEnabled)
                    //     .await()

                    // Update local state
                    updateLocalFeatureState(featureId, isEnabled)
                }
            } catch (e: Exception) {
                // Handle error appropriately
                _loadingState.value =
                    Result.Error(Exception(ERROR_UPDATING_FEATURE + ": ${e.message}"))
            }
        }
    }

    /**
     * Update local feature state
     */
    private fun updateLocalFeatureState(featureId: String, isEnabled: Boolean) {
        val currentFeatures = _socialFeatures.value?.toMutableList() ?: return
        val updatedFeatures = currentFeatures.map { feature ->
            if (feature.id == featureId) {
                feature.copy(isEnabled = isEnabled)
            } else {
                feature
            }
        }
        _socialFeatures.value = updatedFeatures
    }

    /**
     * Send password reset email (inherited functionality)
     *
     * @param email Email address to send reset link to
     * @return LiveData with result of password reset operation
     */
    fun sendPasswordResetEmail(email: String): LiveData<Result<Unit>> {
        val result = MutableLiveData<Result<Unit>>()

        if (email.isBlank()) {
            result.value = Result.Error(IllegalArgumentException("Email cannot be empty"))
            return result
        }

        viewModelScope.launch {
            try {
                result.value = Result.Loading()
                auth.sendPasswordResetEmail(email).await()
                result.value = Result.Success(Unit)
            } catch (e: Exception) {
                result.value = Result.Error(e)
            }
        }

        return result
    }

    /**
     * Refresh social features
     */
    fun refreshFeatures() {
        loadSocialFeatures()
    }

    /**
     * Clear loading state
     */
    fun clearLoadingState() {
        _loadingState.value = null
    }

    /**
     * Get feature by ID
     */
    fun getFeatureById(featureId: String): SocialFeature? {
        return _socialFeatures.value?.find { it.id == featureId }
    }

    /**
     * Get enabled features count
     */
    fun getEnabledFeaturesCount(): Int {
        return _socialFeatures.value?.count { it.isEnabled } ?: 0
    }
}
