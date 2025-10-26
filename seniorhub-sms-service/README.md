# SeniorHub SMS Service

A serverless SMS service for the SeniorHub emergency system using Netlify Functions and Semaphore SMS API.

## 🚨 Overview

This service handles emergency alerts from the SeniorHub Android application and delivers SMS notifications to emergency contacts via the Semaphore API.

## 📱 System Architecture

```
[Android App] → [Firebase] → [Netlify Functions] → [Semaphore API] → [SMS Delivery]
```

## 🚀 Quick Start

### 1. Deploy to Netlify

1. Fork this repository
2. Connect to Netlify
3. Set environment variables
4. Deploy!

### 2. Set Environment Variables

In your Netlify site settings, add:

```
SEMAPHORE_API_KEY=your_semaphore_api_key_here
SEMAPHORE_SENDER_NAME=SeniorHub
```

### 3. Update Android App

Update your Android app's `EmergencyService.kt`:

```kotlin
private val API_BASE_URL = "https://your-site-name.netlify.app"
```

## 📋 Features

- 🚨 Emergency SOS alerts
- 📍 Location sharing with Google Maps links
- 📱 SMS delivery to emergency contacts
- 🔄 Automatic retry logic
- 📊 Comprehensive logging
- 🌐 Serverless architecture

## 🛠️ API Endpoint

**POST** `/.netlify/functions/send-emergency-sms`

### Request Body

```json
{
  "seniorName": "John Doe",
  "emergencyType": "SOS Button",
  "emergencyContactPhone": "+639123456789",
  "smsMessage": "🚨 SOS ALERT 🚨\nEmergency Alert: John Doe may need immediate help...",
  "location": {
    "latitude": 7.0731,
    "longitude": 125.6128
  }
}
```

### Response

```json
{
  "success": true,
  "messageId": "12345",
  "message": "Emergency SMS sent successfully"
}
```

## 📱 SMS Message Format

The service sends SMS messages in this format:

```
🚨 SOS ALERT 🚨
Emergency Alert: [Senior's Full Name] may need immediate help. Please Try To Reach her/him.

📍 Location: Lat: 7.0731, Lng: 125.6128
🩺 Emergency Type: SOS Button
⏰ Timestamp: Dec 15, 2023 at 2:30 PM

🗺️ Click this Google Maps link for exact location:
https://maps.google.com/?q=7.0731,125.6128
```

## 🆘 Emergency Types Supported

- SOS Button
- Davao Doctors Hospital
- Southern Philippines Medical Center (SPMC)
- Davao City Police Office (DCPO)
- Davao City Central Fire Station
- Central 911 Davao
- Davao City Ambulance Service

## 🧪 Testing

### Test the Function

```bash
curl -X POST https://your-site-name.netlify.app/.netlify/functions/send-emergency-sms \
  -H "Content-Type: application/json" \
  -d '{
    "seniorName": "Test Senior",
    "emergencyType": "SOS Button",
    "emergencyContactPhone": "+639123456789",
    "smsMessage": "🚨 SOS ALERT 🚨\nEmergency Alert: Test Senior may need immediate help. Please Try To Reach her/him.\n\n📍 Location: Test Location\n🩺 Emergency Type: SOS Button\n⏰ Timestamp: Dec 15, 2023 at 2:30 PM"
  }'
```

### Health Check

```bash
curl https://your-site-name.netlify.app/
```

## 🔧 Setup Semaphore SMS

1. Go to [Semaphore SMS](https://semaphore.co/)
2. Sign up for an account
3. Navigate to API section
4. Generate an API key
5. Add the key to Netlify environment variables

## 📊 Monitoring

- View function logs in Netlify dashboard
- Monitor Semaphore API usage
- Check SMS delivery status

## 🛡️ Security

- HTTPS only
- Input validation
- Rate limiting
- Environment variable protection

## 📈 Performance

- Serverless scaling
- Global CDN
- Automatic retries
- Optimized message length

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

MIT License - see LICENSE file for details

## 🆘 Support

For issues and questions:
- Check the troubleshooting section
- Review Netlify function logs
- Test with curl commands
- Verify environment variables

## 🔗 Links

- [Netlify Functions Documentation](https://docs.netlify.com/functions/overview/)
- [Semaphore SMS API](https://semaphore.co/docs/)
- [SeniorHub Android App](https://github.com/your-org/seniorhub-android)

---

**SeniorHub SMS Service v1.0.0**  
Powered by Netlify Functions & Semaphore SMS
