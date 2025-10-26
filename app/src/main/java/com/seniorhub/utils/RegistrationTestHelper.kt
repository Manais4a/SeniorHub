package com.seniorhub.utils

import android.content.Context
import android.content.Intent
import com.seniorhub.activities.RegisterActivity

/**
 * Helper class to easily test the registration activity
 */
object RegistrationTestHelper {
    
    /**
     * Open the registration activity for testing
     */
    fun openRegistrationActivity(context: Context) {
        val intent = Intent(context, RegisterActivity::class.java)
        context.startActivity(intent)
    }
    
    /**
     * Test data for registration form
     */
    fun getTestUserData(): Map<String, String> {
        return mapOf(
            "fullName" to "John Doe",
            "email" to "john.doe@example.com",
            "password" to "password123",
            "confirmPassword" to "password123",
            "age" to "75",
            "phoneNumber" to "+1234567890",
            "address" to "123 Main St, Anytown, USA",
            "emergencyName" to "Jane Doe",
            "emergencyPhone" to "+1234567891",
            "emergencyRelationship" to "Daughter"
        )
    }
}
