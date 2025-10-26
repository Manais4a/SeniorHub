package com.seniorhub.config

/**
 * Cloudinary Configuration for SeniorHub Android App
 * 
 * This configuration matches the admin web interface settings
 * to ensure consistent image handling across all platforms.
 */
object CloudinaryConfig {
    
    // Cloudinary credentials - Replace with your actual values
    const val CLOUD_NAME = "dftnlg6nh"
    const val API_KEY = "578778791638823"
    const val API_SECRET = "o9ZTgXlca2aqlMnX8t17Rj75D7M"
    
    // Upload settings
    const val UPLOAD_PRESET = "senior-hub-upload"
    const val FOLDER = "senior-hub/profiles"
    
    // Image transformation settings
    object Transformations {
        // Profile image transformations
        const val PROFILE = "w_400,h_400,c_fill,g_face,q_auto,f_auto"
        const val THUMBNAIL = "w_100,h_100,c_fill,g_face,q_auto,f_auto"
        const val FULL_SIZE = "w_800,h_800,c_fill,g_face,q_auto,f_auto"
        
        // Admin display transformations
        const val ADMIN_LIST = "w_60,h_60,c_fill,g_face,q_auto,f_auto"
        const val ADMIN_DETAIL = "w_200,h_200,c_fill,g_face,q_auto,f_auto"
    }
    
    // File validation settings
    object Validation {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        val ALLOWED_TYPES = listOf(
            "image/jpeg",
            "image/png", 
            "image/webp",
            "image/gif"
        )
    }
    
    // URL generation helpers
    fun getProfileImageUrl(publicId: String, transformation: String = Transformations.PROFILE): String {
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/$transformation/$publicId"
    }
    
    fun getAdminImageUrl(publicId: String, transformation: String = Transformations.ADMIN_LIST): String {
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/$transformation/$publicId"
    }
}

