// Robust Cloudinary Upload with Multiple Preset Fallbacks
class RobustCloudinaryUploader {
    constructor() {
        this.config = window.CLOUDINARY_CONFIG;
        this.currentPresetIndex = 0;
    }

    async uploadImage(file, options = {}) {
        const presets = this.config.uploadPresets || [this.config.uploadPreset];
        
        for (let i = 0; i < presets.length; i++) {
            const preset = presets[i];
            console.log(`Trying upload with preset: ${preset} (attempt ${i + 1}/${presets.length})`);
            
            try {
                const result = await this.uploadWithPreset(file, preset, options);
                console.log(`✅ Upload successful with preset: ${preset}`);
                return result;
            } catch (error) {
                console.warn(`❌ Upload failed with preset ${preset}:`, error.message);
                
                // If this is the last preset, throw the error
                if (i === presets.length - 1) {
                    throw new Error(`All upload presets failed. Last error: ${error.message}`);
                }
            }
        }
    }

    async uploadWithPreset(file, preset, options = {}) {
        // Validate file
        this.validateFile(file);
        
        const formData = new FormData();
        formData.append('file', file);
        formData.append('upload_preset', preset);
        
        // Add optional parameters
        if (options.folder) {
            formData.append('folder', options.folder);
        }
        
        if (options.publicId) {
            formData.append('public_id', options.publicId);
        }
        
        // Add transformations if specified (only for signed uploads)
        // Note: Unsigned uploads don't support transformation parameters
        if (options.transformation && preset !== 'senior-hub-upload') {
            formData.append('transformation', options.transformation);
        }
        
        const uploadUrl = `https://api.cloudinary.com/v1_1/${this.config.cloudName}/image/upload`;
        
        console.log('Upload details:', {
            url: uploadUrl,
            preset: preset,
            file: file.name,
            size: file.size
        });
        
        const response = await fetch(uploadUrl, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(`HTTP ${response.status}: ${errorData.error?.message || response.statusText}`);
        }
        
        const data = await response.json();
        
        if (!data.secure_url) {
            throw new Error('No secure_url in response');
        }
        
        return {
            url: data.secure_url,
            publicId: data.public_id,
            width: data.width,
            height: data.height,
            format: data.format,
            bytes: data.bytes,
            preset: preset
        };
    }

    validateFile(file) {
        if (!file) {
            throw new Error('No file provided');
        }
        
        if (!file.type.startsWith('image/')) {
            throw new Error('File must be an image');
        }
        
        const maxSize = 5 * 1024 * 1024; // 5MB
        if (file.size > maxSize) {
            throw new Error(`File too large. Maximum size: ${maxSize / 1024 / 1024}MB`);
        }
    }

    // Test all presets to see which ones work
    async testPresets() {
        console.log('🧪 Testing all upload presets...');
        
        const results = [];
        const testFile = await this.createTestFile();
        
        for (const preset of this.config.uploadPresets) {
            try {
                console.log(`Testing preset: ${preset}`);
                const result = await this.uploadWithPreset(testFile, preset);
                results.push({ preset, status: 'success', result });
                console.log(`✅ ${preset}: SUCCESS`);
            } catch (error) {
                results.push({ preset, status: 'failed', error: error.message });
                console.log(`❌ ${preset}: FAILED - ${error.message}`);
            }
        }
        
        return results;
    }

    async createTestFile() {
        const canvas = document.createElement('canvas');
        canvas.width = 50;
        canvas.height = 50;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FF0000';
        ctx.fillRect(0, 0, 50, 50);
        
        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
        return new File([blob], 'test.png', { type: 'image/png' });
    }
}

// Create global instance
window.robustUploader = new RobustCloudinaryUploader();

// Export for use in other files
window.RobustCloudinaryUploader = RobustCloudinaryUploader;
