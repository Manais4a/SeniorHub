package com.seniorhub.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * Senior User Model
 * Represents a senior citizen user in the system
 */
@Parcelize
data class SeniorUser(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val birthDate: String = "",
    val age: Int = 0,
    val address: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isActive: Boolean = true,
    val lastLogin: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String = "",
    val seniorCitizenId: String = "",
    val philHealthId: String = "",
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val doctorInfo: DoctorInfo? = null,
    val familyMembers: List<FamilyMember> = emptyList()
) : Parcelable

@Parcelize
data class UserPreferences(
    val fontSize: String = "medium", // small, medium, large, extra_large
    val theme: String = "light", // light, dark, high_contrast
    val language: String = "en", // en, fil, ceb, etc.
    val voiceAssistance: Boolean = true,
    val notifications: Boolean = true,
    val locationSharing: Boolean = true,
    val emergencyAutoCall: Boolean = true,
    val reminderSound: Boolean = true,
    val vibrationEnabled: Boolean = true
) : Parcelable

@Parcelize
data class DoctorInfo(
    val id: String = "",
    val name: String = "",
    val specialization: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val clinicAddress: String = "",
    val clinicName: String = "",
    val isPrimary: Boolean = true
) : Parcelable

@Parcelize
data class FamilyMember(
    val id: String = "",
    val name: String = "",
    val relationship: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isGuardian: Boolean = false,
    val canViewHealthData: Boolean = false,
    val canReceiveAlerts: Boolean = true,
    val isActive: Boolean = true
) : Parcelable

