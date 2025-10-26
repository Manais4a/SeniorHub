const axios = require('axios');

// Semaphore SMS API configuration
const SEMAPHORE_API_KEY = process.env.SEMAPHORE_API_KEY || 'your-semaphore-api-key';
const SEMAPHORE_SENDER_NAME = process.env.SEMAPHORE_SENDER_NAME || 'SeniorHub';

/**
 * Netlify Function to send emergency SMS via Semaphore API
 */
exports.handler = async (event, context) => {
    // Enable CORS
    const headers = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Allow-Methods': 'POST, OPTIONS'
    };

    // Handle preflight requests
    if (event.httpMethod === 'OPTIONS') {
        return {
            statusCode: 200,
            headers,
            body: ''
        };
    }

    // Only allow POST requests
    if (event.httpMethod !== 'POST') {
        return {
            statusCode: 405,
            headers,
            body: JSON.stringify({
                success: false,
                error: 'Method not allowed. Use POST.'
            })
        };
    }

    try {
        // Parse request body
        const body = JSON.parse(event.body);
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
        } = body;

        // Validate required fields
        if (!emergencyContactPhone || !smsMessage) {
            return {
                statusCode: 400,
                headers,
                body: JSON.stringify({
                    success: false,
                    error: 'Missing required fields: emergencyContactPhone and smsMessage'
                })
            };
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

            return {
                statusCode: 200,
                headers,
                body: JSON.stringify({
                    success: true,
                    messageId: smsResponse.messageId,
                    message: 'Emergency SMS sent successfully'
                })
            };
        } else {
            console.error(`Failed to send SMS to ${emergencyContactPhone}: ${smsResponse.error}`);
            return {
                statusCode: 500,
                headers,
                body: JSON.stringify({
                    success: false,
                    error: smsResponse.error || 'Failed to send SMS'
                })
            };
        }

    } catch (error) {
        console.error('Error in send-emergency-sms function:', error);
        return {
            statusCode: 500,
            headers,
            body: JSON.stringify({
                success: false,
                error: error.message || 'Internal server error'
            })
        };
    }
};

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
