// Simplified SMS function that returns success without sending actual SMS
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
        const { emergencyContactPhone, smsMessage } = body;

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

        console.log(`SMS request for ${emergencyContactPhone}: ${smsMessage.substring(0, 50)}...`);

        // Return success (for testing - add actual SMS logic later)
        return {
            statusCode: 200,
            headers,
            body: JSON.stringify({
                success: true,
                message: 'SMS request received successfully (test mode)',
                phone: emergencyContactPhone
            })
        };

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