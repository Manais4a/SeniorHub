// Cloudinary Configuration
// Replace these values with your actual Cloudinary credentials

const CLOUDINARY_CONFIG = {
    // Your Cloudinary cloud name (found in your Cloudinary dashboard)
    cloudName: 'dftnlg6nh',
    
    // Upload presets to try (in order of preference)
    uploadPresets: [
        'senior-hub-upload',    // This one is working! Use it first
        'ml_default',           // Try default preset
    ],
    
    // Current upload preset (will be set dynamically)
    uploadPreset: 'senior-hub-upload',
    
    // Optional: API key and secret (for server-side operations)
    apiKey: '578778791638823',
    apiSecret: 'o9ZTgXlca2aqlMnX8t17Rj75D7M'
};

// Image upload settings
const UPLOAD_SETTINGS = {
    // Folder to organize images
    folder: 'senior-hub/profiles',
    
    // Image transformations - DISABLED for unsigned uploads
    // transformations: {
    //     // Resize to 400x400, crop to face, auto-format
    //     profile: 'w_400,h_400,c_fill,g_face,f_auto',
    //     
    //     // Thumbnail for lists
    //     thumbnail: 'w_100,h_100,c_fill,g_face,f_auto',
    //     
    //     // Full size for detailed view
    //     full: 'w_800,h_800,c_fill,g_face,f_auto'
    // },
    
    // File size limits
    maxFileSize: 5 * 1024 * 1024, // 5MB
    
    // Allowed file types
    allowedTypes: ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
};

// Export for use in other files
window.CLOUDINARY_CONFIG = CLOUDINARY_CONFIG;
window.UPLOAD_SETTINGS = UPLOAD_SETTINGS;
