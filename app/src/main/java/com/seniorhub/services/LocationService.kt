package com.seniorhub.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * LocationService - Simplified location handling for emergency alerts
 */
class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    companion object {
        private const val TAG = "LocationService"
    }
    
    /**
     * Get current location with simplified approach
     */
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Check location permission
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Location permission not granted")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // Get last known location first (faster)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        } else {
                            // If no last known location, request current location
                            requestCurrentLocation(continuation)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting last known location: ${exception.message}", exception)
                        // Try to get current location as fallback
                        requestCurrentLocation(continuation)
                    }
                    
            } catch (e: Exception) {
                Log.e(TAG, "Error in getCurrentLocation: ${e.message}", e)
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Request current location
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestCurrentLocation(continuation: CancellableContinuation<Location?>) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Got current location: ${location.latitude}, ${location.longitude}")
                        continuation.resume(location)
                    } else {
                        Log.w(TAG, "Current location is null")
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting current location: ${exception.message}", exception)
                    continuation.resume(null)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting current location: ${e.message}", e)
            continuation.resume(null)
        }
    }
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
