package com.seniorhub.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.seniorhub.R

/**
 * ImageLoader - Utility class for loading images with Glide
 * 
 * Provides optimized image loading with:
 * - Cloudinary URL support
 * - Placeholder and error handling
 * - Caching strategies
 * - Senior-friendly image processing
 */
object ImageLoader {
    
    
    /**
     * Load profile image from URL with optimized settings
     * @param context Application context
     * @param imageView Target ImageView
     * @param imageUrl URL of the image to load
     * @param placeholderId Resource ID for placeholder image
     * @param errorId Resource ID for error image
     */
    fun loadProfileImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        placeholderId: Int = R.drawable.ic_profile,
        errorId: Int = R.drawable.ic_profile
    ) {
        android.util.Log.d("ImageLoader", "Loading profile image: $imageUrl")
        android.util.Log.d("ImageLoader", "ImageView: ${imageView != null}")
        android.util.Log.d("ImageLoader", "Context: ${context != null}")
        
        // Clear any existing image first
        imageView.setImageDrawable(null)
        
        // Validate image URL
        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) {
            android.util.Log.w("ImageLoader", "Invalid image URL: $imageUrl")
            imageView.setImageResource(errorId)
            return
        }
        
        android.util.Log.d("ImageLoader", "Starting Glide load for: $imageUrl")
        
        val requestOptions = RequestOptions()
            .placeholder(placeholderId)
            .error(errorId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .circleCrop()
            .timeout(15000) // 15 second timeout
        
        Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(imageView)
    }
    
    /**
     * Load regular image from URL
     * @param context Application context
     * @param imageView Target ImageView
     * @param imageUrl URL of the image to load
     * @param placeholderId Resource ID for placeholder image
     * @param errorId Resource ID for error image
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        placeholderId: Int = R.drawable.ic_profile,
        errorId: Int = R.drawable.ic_profile
    ) {
        val requestOptions = RequestOptions()
            .placeholder(placeholderId)
            .error(errorId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
        
        Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(imageView)
    }
    
    /**
     * Load image with custom transformations
     * @param context Application context
     * @param imageView Target ImageView
     * @param imageUrl URL of the image to load
     * @param width Desired width
     * @param height Desired height
     * @param placeholderId Resource ID for placeholder image
     * @param errorId Resource ID for error image
     */
    fun loadImageWithSize(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        width: Int,
        height: Int,
        placeholderId: Int = R.drawable.ic_profile,
        errorId: Int = R.drawable.ic_profile
    ) {
        val requestOptions = RequestOptions()
            .placeholder(placeholderId)
            .error(errorId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .override(width, height)
        
        Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(imageView)
    }
    
    /**
     * Clear image cache
     * @param context Application context
     */
    fun clearCache(context: Context) {
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
    
    /**
     * Validate if URL is a valid Cloudinary URL
     * @param url URL to validate
     * @return True if valid Cloudinary URL
     */
    fun isValidCloudinaryUrl(url: String?): Boolean {
        return !url.isNullOrBlank() && 
               url.startsWith("https://res.cloudinary.com/") && 
               url.contains("/image/upload/")
    }
    
    /**
     * Get optimized Cloudinary URL with transformations
     * @param originalUrl Original Cloudinary URL
     * @param width Desired width
     * @param height Desired height
     * @return Optimized URL
     */
    fun getOptimizedCloudinaryUrl(originalUrl: String?, width: Int = 400, height: Int = 400): String? {
        if (!isValidCloudinaryUrl(originalUrl)) return originalUrl
        
        return try {
            val baseUrl = originalUrl!!.substringBefore("/image/upload/")
            val publicId = originalUrl.substringAfterLast("/")
            "$baseUrl/image/upload/w_$width,h_$height,c_fill,g_face,q_auto,f_auto/$publicId"
        } catch (e: Exception) {
            originalUrl
        }
    }
}

