package com.seniorhub.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreUtils {
    private const val TAG = "FirestoreUtils"

    suspend fun ensureUserDocument(
        uid: String,
        email: String?,
        displayName: String?
    ) {
        try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(uid)

            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                // Ensure required fields exist
                val updates = mutableMapOf<String, Any>()
                if ((snapshot.getString("role") ?: "").isBlank()) {
                    updates["role"] = "senior_citizen"
                }
                if ((snapshot.get("updatedAt")) == null) {
                    updates["updatedAt"] = Timestamp.now()
                }
                if (updates.isNotEmpty()) {
                    docRef.set(updates, SetOptions.merge()).await()
                }
                Log.d(TAG, "User document exists for $uid")
                return
            }

            val names = (displayName ?: "").trim().split(" ")
            val firstName = names.firstOrNull() ?: ""
            val lastName = if (names.size > 1) names.drop(1).joinToString(" ") else ""

            val payload = hashMapOf(
                "id" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to (email ?: ""),
                "role" to "senior_citizen",
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "isActive" to true,
                "accountVerified" to false
            )

            docRef.set(payload).await()
            Log.d(TAG, "Created users/$uid document")
        } catch (e: Exception) {
            Log.e(TAG, "ensureUserDocument failed: ${e.message}", e)
        }
    }
}


