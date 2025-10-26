package com.seniorhub.models

import java.util.Date

/**
 * SocialFeature - Represents a social connectivity feature for senior citizens
 *
 * Designed for senior-friendly social interactions including:
 * - Family messaging and video calls
 * - Community group participation
 * - Photo sharing with loved ones
 * - Event notifications and reminders
 */
data class SocialFeature(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconResId: Int = 0,
    val type: SocialFeatureType = SocialFeatureType.MESSAGES,
    val lastInteraction: Long = 0L, // Using timestamp instead of Firebase Timestamp
    val unreadCount: Int = 0,
    val isEnabled: Boolean = true,
    val participants: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {

    /**
     * Get the number of participants in this social feature
     */
    fun getParticipantCount(): Int = participants.size

    /**
     * Check if there are unread messages or notifications
     */
    fun hasUnreadMessages(): Boolean = unreadCount > 0

    /**
     * Get formatted last interaction time
     */
    fun getLastInteractionDate(): Date = Date(lastInteraction)

    /**
     * Get formatted creation date
     */
    fun getCreatedDate(): Date = Date(createdAt)

    /**
     * Check if this feature has been recently active (within 24 hours)
     */
    fun isRecentlyActive(): Boolean {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return lastInteraction > twentyFourHoursAgo
    }

    /**
     * Create a copy with updated interaction time
     */
    fun withUpdatedInteraction(): SocialFeature {
        return copy(
            lastInteraction = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Create a copy with updated unread count
     */
    fun withUnreadCount(count: Int): SocialFeature {
        return copy(
            unreadCount = count,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Convert to map for Firebase storage
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "iconResId" to iconResId,
            "type" to type.name,
            "lastInteraction" to lastInteraction,
            "unreadCount" to unreadCount,
            "isEnabled" to isEnabled,
            "participants" to participants,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        /**
         * Create SocialFeature from map (for Firebase)
         */
        fun fromMap(map: Map<String, Any>): SocialFeature {
            return SocialFeature(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                iconResId = (map["iconResId"] as? Long)?.toInt() ?: 0,
                type = SocialFeatureType.fromString(map["type"] as? String ?: "MESSAGES"),
                lastInteraction = map["lastInteraction"] as? Long ?: 0L,
                unreadCount = (map["unreadCount"] as? Long)?.toInt() ?: 0,
                isEnabled = map["isEnabled"] as? Boolean ?: true,
                participants = (map["participants"] as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList(),
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Types of social features available for senior citizens
 */
enum class SocialFeatureType {
    MESSAGES,       // Text messaging with family and friends
    VIDEO_CALL,     // Video calling functionality
    EVENTS,         // Community events and activities
    GROUPS,         // Senior citizen groups and communities
    PHOTOS,         // Photo sharing with family
    MEMORIES,       // Memory sharing and reminiscence
    EMERGENCY,      // Emergency family notifications
    SUPPORT;        // Peer support groups
    
    companion object {
        /**
         * Convert string to SocialFeatureType with fallback
         */
        fun fromString(value: String): SocialFeatureType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                MESSAGES // Default fallback for unknown types
            }
        }

        /**
         * Get all available social feature types
         */
        fun getAllTypes(): List<SocialFeatureType> = values().toList()
    }

    /**
     * Get user-friendly display name for this feature type
     */
    fun getDisplayName(): String {
        return when (this) {
            MESSAGES -> "Messages"
            VIDEO_CALL -> "Video Calls"
            EVENTS -> "Community Events"
            GROUPS -> "Groups"
            PHOTOS -> "Photo Sharing"
            MEMORIES -> "Memories"
            EMERGENCY -> "Emergency Contacts"
            SUPPORT -> "Support Groups"
        }
    }

    /**
     * Get description for this feature type
     */
    fun getDescription(): String {
        return when (this) {
            MESSAGES -> "Stay in touch with family and friends through simple messaging"
            VIDEO_CALL -> "See and talk to your loved ones face-to-face"
            EVENTS -> "Find and join community activities and events"
            GROUPS -> "Connect with other seniors in your area"
            PHOTOS -> "Share and view photos with your family"
            MEMORIES -> "Share stories and memories with loved ones"
            EMERGENCY -> "Quick access to emergency contacts and family"
            SUPPORT -> "Get support from peers and community members"
        }
    }

    /**
     * Check if this feature type requires internet connection
     */
    fun requiresInternet(): Boolean {
        return when (this) {
            VIDEO_CALL, EVENTS, GROUPS, PHOTOS, MEMORIES -> true
            MESSAGES, EMERGENCY, SUPPORT -> false
        }
    }
}
