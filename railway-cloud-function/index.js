const express = require('express');
const cors = require('cors');
const axios = require('axios');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Semaphore SMS API configuration
const SEMAPHORE_API_KEY = process.env.SEMAPHORE_API_KEY || 'your-semaphore-api-key';
const SEMAPHORE_SENDER_NAME = process.env.SEMAPHORE_SENDER_NAME || 'SeniorHub';

/**
 * Send emergency SMS via Semaphore API
 */
app.post('/send-emergency-sms', async (req, res) => {
    try {
        const {
            seniorName,
            emergencyType,
            emergencyContactPhone,
            emergencyContactName,
            smsMessage,
            location,
            serviceName,
            servicePhone,
            timestamp
        } = req.body;

        // Validate required fields
        if (!emergencyContactPhone || !smsMessage) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields: emergencyContactPhone and smsMessage'
            });
        }

        console.log(`Sending emergency SMS to ${emergencyContactPhone} for ${seniorName}`);

        // Send SMS via Semaphore API
        const smsResponse = await sendSMSViaSemaphore(emergencyContactPhone, smsMessage);

        if (smsResponse.success) {
            console.log(`SMS sent successfully to ${emergencyContactPhone}`);
            
            // Log the emergency alert
            logEmergencyAlert({
                seniorName,
                emergencyType,
                emergencyContactPhone,
                emergencyContactName,
                location,
                serviceName,
                servicePhone,
                timestamp,
                smsMessageId: smsResponse.messageId
            });

            res.json({
                success: true,
                messageId: smsResponse.messageId,
                message: 'Emergency SMS sent successfully'
            });
        } else {
            console.error(`Failed to send SMS to ${emergencyContactPhone}: ${smsResponse.error}`);
            res.status(500).json({
                success: false,
                error: smsResponse.error || 'Failed to send SMS'
            });
        }

    } catch (error) {
        console.error('Error in send-emergency-sms endpoint:', error);
        res.status(500).json({
            success: false,
            error: error.message || 'Internal server error'
        });
    }
});

/**
 * Send SMS via Semaphore API
 */
async function sendSMSViaSemaphore(phoneNumber, message) {
    try {
        // Clean phone number (remove spaces, dashes, etc.)
        const cleanPhoneNumber = phoneNumber.replace(/[^0-9+]/g, '');
        
        // Ensure phone number has country code
        const formattedPhoneNumber = cleanPhoneNumber.startsWith('+') 
            ? cleanPhoneNumber 
            : `+63${cleanPhoneNumber.replace(/^0/, '')}`;

        const response = await axios.post('https://api.semaphore.co/api/v4/messages', {
            apikey: SEMAPHORE_API_KEY,
            number: formattedPhoneNumber,
            message: message,
            sendername: SEMAPHORE_SENDER_NAME
        }, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000 // 10 second timeout
        });

        if (response.data && response.data[0]) {
            return {
                success: true,
                messageId: response.data[0].message_id || 'unknown'
            };
        } else {
            return {
                success: false,
                error: 'Invalid response from Semaphore API'
            };
        }

    } catch (error) {
        console.error('Semaphore API error:', error.response?.data || error.message);
        return {
            success: false,
            error: error.response?.data?.message || error.message || 'SMS API error'
        };
    }
}

/**
 * Log emergency alert for monitoring
 */
function logEmergencyAlert(alertData) {
    const logEntry = {
        timestamp: new Date().toISOString(),
        type: 'EMERGENCY_ALERT',
        data: alertData
    };
    
    console.log('EMERGENCY_ALERT_LOG:', JSON.stringify(logEntry, null, 2));
    
    // In production, you might want to save this to a database
    // For now, we'll just log it to console
}

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        service: 'SeniorHub SMS Service'
    });
});

/**
 * Root endpoint
 */
app.get('/', (req, res) => {
    res.json({
        message: 'SeniorHub Emergency SMS Service',
        version: '1.0.0',
        endpoints: {
            'POST /send-emergency-sms': 'Send emergency SMS alert',
            'GET /health': 'Health check'
        }
    });
});

// Error handling middleware
app.use((error, req, res, next) => {
    console.error('Unhandled error:', error);
    res.status(500).json({
        success: false,
        error: 'Internal server error'
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`SeniorHub SMS Service running on port ${PORT}`);
    console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
    console.log(`Semaphore API Key configured: ${!!SEMAPHORE_API_KEY}`);
});

module.exports = app;
