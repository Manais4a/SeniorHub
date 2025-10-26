package com.seniorhub.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.seniorhub.services.HealthMonitoringService
import com.seniorhub.utils.PreferenceManager

/**
 * BootReceiver - Handles device boot events
 *
 * This receiver restarts necessary services when the device boots up
 * to ensure continuous health monitoring and reminder functionality
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handleBootCompleted(context)
            }
        }
    }

    /**
     * Handle boot completed event
     */
    private fun handleBootCompleted(context: Context) {
        try {
            val preferenceManager = PreferenceManager.getInstance()

            // Only restart services if user is logged in and services were previously enabled
            if (preferenceManager.isUserLoggedIn && preferenceManager.areServicesEnabled) {

                // Restart health monitoring service
                val healthServiceIntent = Intent(context, HealthMonitoringService::class.java)
                context.startService(healthServiceIntent)

                Log.i(TAG, "Services restarted after boot")

                // TODO: Restore scheduled reminders from Firebase
                // This would involve reading all active reminders and rescheduling them

            } else {
                Log.d(TAG, "Services not restarted - user not logged in or services disabled")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling boot completed: ${e.message}", e)
        }
    }
}