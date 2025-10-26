package com.seniorhub.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.seniorhub.config.CloudinaryConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CloudinaryManager - Handles image upload and management using Cloudinary
 * 
 * This class provides a comprehensive solution for:
 * - Image upload to Cloudinary cloud storage
 * - Image compression and optimization
 * - URL generation for uploaded images
 * - Error handling and retry logic
 * - Senior-friendly image processing
 */
object CloudinaryManager {
    
    private const val TAG = "CloudinaryManager"
    private var isInitialized = false
    
    // Cloudinary configuration - Using centralized config
    private val CLOUD_NAME = CloudinaryConfig.CLOUD_NAME
    private val API_KEY = CloudinaryConfig.API_KEY
    private val API_SECRET = CloudinaryConfig.API_SECRET
    
    /**
     * Initialize Cloudinary with configuration
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                Log.d(TAG, "Initializing Cloudinary with cloud name: $CLOUD_NAME")
                Log.d(TAG, "API Key: $API_KEY")
                Log.d(TAG, "Upload Preset: ${CloudinaryConfig.UPLOAD_PRESET}")
                
                // For unsigned uploads with upload presets, we need cloud_name and api_key
                // Note: api_secret is NOT needed for unsigned uploads with presets
                val config = mapOf(
                    "cloud_name" to CLOUD_NAME,
                    "api_key" to API_KEY,
                    "upload_preset" to CloudinaryConfig.UPLOAD_PRESET
                )

                MediaManager.init(context, config)
                isInitialized = true
                Log.d(TAG, "Cloudinary initialized successfully with config: $config")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Cloudinary: ${e.message}", e)
                throw e
            }
        } else {
            Log.d(TAG, "Cloudinary already initialized")
        }
    }
    
    /**
     * Upload image from URI to Cloudinary
     * @param imageUri URI of the image to upload
     * @param publicId Optional public ID for the image
     * @param folder Optional folder to organize images
     * @return URL of the uploaded image
     */
    suspend fun uploadImage(
        imageUri: Uri,
        publicId: String? = null,
        folder: String = CloudinaryConfig.FOLDER
    ): String = suspendCancellableCoroutine { continuation ->
        
        if (!isInitialized) {
            continuation.resumeWithException(Exception("Cloudinary not initialized"))
            return@suspendCancellableCoroutine
        }
        
        try {
            // Generate unique public ID if not provided
            val finalPublicId = publicId ?: "profile_${System.currentTimeMillis()}"
            Log.d(TAG, "Uploading image with public ID: $finalPublicId")
            Log.d(TAG, "Upload folder: $folder")
            Log.d(TAG, "Upload preset: ${CloudinaryConfig.UPLOAD_PRESET}")
            
            // Unsigned upload options using preset
            val uploadOptions = mapOf(
                "public_id" to finalPublicId,
                "folder" to folder,
                "upload_preset" to CloudinaryConfig.UPLOAD_PRESET,
                "resource_type" to "image"
            )
            
            Log.d(TAG, "Upload options: $uploadOptions")
            Log.d(TAG, "Cloud name from config: $CLOUD_NAME")
            Log.d(TAG, "API key from config: $API_KEY")
            
            MediaManager.get().upload(imageUri)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started with request ID: $requestId")
                        Log.d(TAG, "Uploading image URI: $imageUri")
                    }
                    
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d(TAG, "Upload progress: $progress%")
                    }
                    
                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        Log.d(TAG, "Upload success callback received")
                        Log.d(TAG, "Result data keys: ${resultData.keys}")
                        Log.d(TAG, "Full result data: $resultData")
                        
                        val secureUrl = resultData["secure_url"] as? String
                        val publicId = resultData["public_id"] as? String
                        val url = resultData["url"] as? String
                        
                        Log.d(TAG, "secure_url: $secureUrl")
                        Log.d(TAG, "public_id: $publicId")
                        Log.d(TAG, "url: $url")
                        
                        if (secureUrl != null && secureUrl.isNotEmpty()) {
                            Log.d(TAG, "Upload successful: $secureUrl")
                            continuation.resume(secureUrl)
                        } else if (url != null && url.isNotEmpty()) {
                            Log.d(TAG, "Using fallback URL: $url")
                            continuation.resume(url)
                        } else {
                            Log.e(TAG, "Upload successful but no valid URL returned")
                            Log.e(TAG, "secure_url value: $secureUrl")
                            Log.e(TAG, "url value: $url")
                            Log.e(TAG, "Result data keys: ${resultData.keys}")
                            Log.e(TAG, "Result data: $resultData")
                            continuation.resumeWithException(Exception("No valid URL returned from upload"))
                        }
                    }
                    
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload failed: ${error.description}")
                        continuation.resumeWithException(Exception("Upload failed: ${error.description}"))
                    }
                    
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error starting upload: ${e.message}")
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload bitmap to Cloudinary
     * @param bitmap Bitmap to upload
     * @param publicId Optional public ID for the image
     * @param folder Optional folder to organize images
     * @return URL of the uploaded image
     */
    suspend fun uploadBitmap(
        bitmap: Bitmap,
        publicId: String? = null,
        folder: String = CloudinaryConfig.FOLDER
    ): String = suspendCancellableCoroutine { continuation ->
        
        if (!isInitialized) {
            continuation.resumeWithException(Exception("Cloudinary not initialized"))
            return@suspendCancellableCoroutine
        }
        
        try {
            // Compress bitmap to reduce file size
            val compressedBitmap = compressBitmap(bitmap, 80)
            val byteArray = bitmapToByteArray(compressedBitmap)
            
            // Generate unique public ID if not provided
            val finalPublicId = publicId ?: "profile_${System.currentTimeMillis()}"
            
            // Unsigned upload options using preset
            val uploadOptions = mapOf(
                "public_id" to finalPublicId,
                "folder" to folder,
                "upload_preset" to CloudinaryConfig.UPLOAD_PRESET,
                "resource_type" to "image"
            )
            
            MediaManager.get().upload(byteArray)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started: $requestId")
                    }
                    
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d(TAG, "Upload progress: $progress%")
                    }
                    
                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        Log.d(TAG, "Upload success callback received")
                        Log.d(TAG, "Result data keys: ${resultData.keys}")
                        Log.d(TAG, "Full result data: $resultData")
                        
                        val secureUrl = resultData["secure_url"] as? String
                        val publicId = resultData["public_id"] as? String
                        val url = resultData["url"] as? String
                        
                        Log.d(TAG, "secure_url: $secureUrl")
                        Log.d(TAG, "public_id: $publicId")
                        Log.d(TAG, "url: $url")
                        
                        if (secureUrl != null && secureUrl.isNotEmpty()) {
                            Log.d(TAG, "Upload successful: $secureUrl")
                            continuation.resume(secureUrl)
                        } else if (url != null && url.isNotEmpty()) {
                            Log.d(TAG, "Using fallback URL: $url")
                            continuation.resume(url)
                        } else {
                            Log.e(TAG, "Upload successful but no valid URL returned")
                            Log.e(TAG, "secure_url value: $secureUrl")
                            Log.e(TAG, "url value: $url")
                            Log.e(TAG, "Result data keys: ${resultData.keys}")
                            Log.e(TAG, "Result data: $resultData")
                            continuation.resumeWithException(Exception("No valid URL returned from upload"))
                        }
                    }
                    
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload failed: ${error.description}")
                        continuation.resumeWithException(Exception("Upload failed: ${error.description}"))
                    }
                    
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error starting upload: ${e.message}")
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Generate optimized image URL with transformations
     * @param publicId Public ID of the image
     * @param width Desired width
     * @param height Desired height
     * @param quality Image quality (auto, 80, 90, etc.)
     * @return Optimized image URL
     */
    fun getOptimizedImageUrl(
        publicId: String,
        width: Int = 400,
        height: Int = 400,
        quality: String = "auto"
    ): String {
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/w_$width,h_$height,c_fill,g_face,q_$quality,f_auto/$publicId"
    }
    
    /**
     * Extract public ID from Cloudinary URL
     * @param url Cloudinary URL
     * @return Public ID or null if invalid URL
     */
    fun extractPublicId(url: String): String? {
        return try {
            val parts = url.split("/")
            if (parts.size >= 2) {
                parts.last().split(".").first()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting public ID: ${e.message}")
            null
        }
    }
    
    /**
     * Compress bitmap to reduce file size
     * @param bitmap Original bitmap
     * @param quality Compression quality (0-100)
     * @return Compressed bitmap
     */
    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    
    /**
     * Convert bitmap to byte array
     * @param bitmap Bitmap to convert
     * @return Byte array representation
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Load image from URI and convert to bitmap
     * @param context Application context
     * @param uri Image URI
     * @return Bitmap or null if failed
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: ${e.message}")
            null
        }
    }
    
    /**
     * Check if Cloudinary is properly initialized
     * @return True if initialized, false otherwise
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Verify Cloudinary configuration
     * @return True if configuration is valid, false otherwise
     */
    fun verifyConfiguration(): Boolean {
        return try {
            val isValid = CLOUD_NAME.isNotEmpty() && 
                         API_KEY.isNotEmpty() && 
                         CloudinaryConfig.UPLOAD_PRESET.isNotEmpty()
            
            Log.d(TAG, "Configuration verification:")
            Log.d(TAG, "  Cloud name: $CLOUD_NAME (${CLOUD_NAME.isNotEmpty()})")
            Log.d(TAG, "  API key: $API_KEY (${API_KEY.isNotEmpty()})")
            Log.d(TAG, "  Upload preset: ${CloudinaryConfig.UPLOAD_PRESET} (${CloudinaryConfig.UPLOAD_PRESET.isNotEmpty()})")
            Log.d(TAG, "  Configuration valid: $isValid")
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying configuration: ${e.message}")
            false
        }
    }
}
