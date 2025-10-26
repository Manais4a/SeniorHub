package com.seniorhub.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Emergency Contact model for SeniorHub application
 * Represents an emergency contact for a senior citizen
 *
 * @author SeniorHub Team
 * @version 1.0.0
 */
@Parcelize
data class EmergencyContact(
    val id: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var relationship: String = "",
    var isPrimary: Boolean = true,
    val isActive: Boolean = true,
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val contactType: ContactType = ContactType.FAMILY,
    val priority: Int = 1, // 1 = highest priority
    val isAvailable24x7: Boolean = true,
    val preferredContactMethod: ContactMethod = ContactMethod.PHONE,
    val lastContacted: Long = 0L,
    val responseTime: Int = 0 // in minutes
) : Parcelable {

    /**
     * Get full contact information for display
     */
    fun getFullContactInfo(): String {
        return buildString {
            append(name)
            if (relationship.isNotEmpty()) {
                append(" ($relationship)")
            }
            append("\n$phoneNumber")
            if (email.isNotEmpty()) {
                append("\n$email")
            }
            if (address.isNotEmpty()) {
                append("\n$address")
            }
        }
    }

    /**
     * Check if this is a high priority contact
     */
    fun isHighPriority(): Boolean = priority <= 2

    /**
     * Get contact method display name
     */
    fun getContactMethodDisplayName(): String {
        return when (preferredContactMethod) {
            ContactMethod.PHONE -> "Phone"
            ContactMethod.SMS -> "SMS"
            ContactMethod.EMAIL -> "Email"
            ContactMethod.VIDEO_CALL -> "Video Call"
        }
    }

    /**
     * Get contact type display name
     */
    fun getContactTypeDisplayName(): String {
        return when (contactType) {
            ContactType.FAMILY -> "Family"
            ContactType.FRIEND -> "Friend"
            ContactType.DOCTOR -> "Doctor"
            ContactType.NURSE -> "Nurse"
            ContactType.CAREGIVER -> "Caregiver"
            ContactType.POLICE -> "Police"
            ContactType.HOSPITAL -> "Hospital"
            ContactType.FIRE_DEPARTMENT -> "Fire Department"
            ContactType.SOCIAL_WELFARE -> "Social Welfare"
            ContactType.OTHER -> "Other"
        }
    }
}

/**
 * Contact types for emergency contacts
 */
@Parcelize
enum class ContactType : Parcelable {
    FAMILY,
    FRIEND,
    DOCTOR,
    NURSE,
    CAREGIVER,
    POLICE,
    HOSPITAL,
    FIRE_DEPARTMENT,
    SOCIAL_WELFARE,
    OTHER
}

/**
 * Preferred contact methods
 */
@Parcelize
enum class ContactMethod : Parcelable {
    PHONE,
    SMS,
    EMAIL,
    VIDEO_CALL
}
