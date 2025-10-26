package com.seniorhub.models

/**
 * SocialService data class representing social services in Davao City District 1
 * 
 * This model includes comprehensive information about each social service office
 * including contact details, services offered, and location information.
 */
data class SocialService(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val contact: String = "",
    val phoneNumber: String = "", // Extracted phone number for direct calling
    val email: String = "",      // Extracted email
    val servicesOffered: String = "", // Detailed list of services provided
    val serviceType: String = "GOVERNMENT", // Type: GOVERNMENT, NGO, PRIVATE, etc.
    val officeHours: String = "", // Operating hours
    val notes: String = "", // Additional notes
    val website: String = "", // Website URL if available
    val isActive: Boolean = true, // Whether the service is currently active
    val priority: Int = 0 // Priority level for sorting (higher = more important)
) {
    /**
     * Get formatted phone number for display
     */
    fun getFormattedPhoneNumber(): String {
        return if (phoneNumber.isNotBlank()) {
            phoneNumber
        } else {
            // Try to extract from contact string
            val phoneRegex = "\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}".toRegex()
            val match = phoneRegex.find(contact)
            match?.value ?: ""
        }
    }

    /**
     * Get formatted email for display
     */
    fun getFormattedEmail(): String {
        return if (email.isNotBlank()) {
            email
        } else {
            // Try to extract from contact string
            val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
            val match = emailRegex.find(contact)
            match?.value ?: ""
        }
    }

    /**
     * Check if service has phone number
     */
    fun hasPhoneNumber(): Boolean = getFormattedPhoneNumber().isNotBlank()

    /**
     * Check if service has email
     */
    fun hasEmail(): Boolean = getFormattedEmail().isNotBlank()

    /**
     * Check if service has address
     */
    fun hasAddress(): Boolean = address.isNotBlank()

    /**
     * Get services offered as a list
     */
    fun getServicesList(): List<String> {
        return if (servicesOffered.isNotBlank()) {
            servicesOffered.split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
}




