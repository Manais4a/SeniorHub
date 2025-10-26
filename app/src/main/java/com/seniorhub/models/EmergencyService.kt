package com.seniorhub.models

import com.google.firebase.Timestamp
import java.io.Serializable


/**
 * EmergencyService data model for emergency services and contacts
 */
data class EmergencyService(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var phoneNumber: String = "",
    var address: String = "",
    var serviceType: String = "EMERGENCY", // "EMERGENCY", "MEDICAL", "POLICE", "FIRE", "SENIOR"
    var priority: Int = 0, // Higher number = higher priority
    var isActive: Boolean = true,
    var officeHours: String = "",
    var website: String = "",
    var notes: String = "",
    var createdBy: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now()
) : Serializable {

    /**
     * Get formatted phone number for display
     */
    fun getFormattedPhoneNumber(): String {
        return when {
            phoneNumber.isEmpty() -> "Not available"
            phoneNumber == "911" -> "911"
            else -> phoneNumber
        }
    }

    /**
     * Check if service has a valid phone number
     */
    fun hasPhoneNumber(): Boolean {
        return phoneNumber.isNotEmpty() && phoneNumber != "Not available"
    }

    /**
     * Check if service has a valid address
     */
    fun hasAddress(): Boolean {
        return address.isNotEmpty()
    }

    /**
     * Get service type display name
     */
    fun getServiceTypeDisplay(): String {
        return when (serviceType) {
            "EMERGENCY" -> "Emergency Services"
            "MEDICAL" -> "Medical Services"
            "POLICE" -> "Police Services"
            "FIRE" -> "Fire Services"
            "SENIOR" -> "Senior Services"
            else -> serviceType
        }
    }

    /**
     * Get priority display text
     */
    fun getPriorityDisplay(): String {
        return when {
            priority >= 100 -> "Critical"
            priority >= 50 -> "High"
            priority >= 10 -> "Medium"
            else -> "Low"
        }
    }
}
