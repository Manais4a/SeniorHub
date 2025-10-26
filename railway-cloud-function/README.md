# SeniorHub SMS Service Environment Variables

## Required Environment Variables

### Semaphore SMS API Configuration
```
SEMAPHORE_API_KEY=your_semaphore_api_key_here
SEMAPHORE_SENDER_NAME=SeniorHub
```

### Server Configuration
```
PORT=3000
NODE_ENV=production
```

## How to Get Semaphore API Key

1. Go to [Semaphore SMS](https://semaphore.co/)
2. Sign up for an account
3. Navigate to API section in your dashboard
4. Generate an API key
5. Copy the API key and set it as `SEMAPHORE_API_KEY`

## Setting Environment Variables in Railway

1. Go to your Railway project dashboard
2. Click on your service
3. Go to the "Variables" tab
4. Add the following variables:
   - `SEMAPHORE_API_KEY`: Your Semaphore API key
   - `SEMAPHORE_SENDER_NAME`: SeniorHub (or your preferred sender name)
   - `NODE_ENV`: production

## Testing the Service

### Health Check
```bash
curl https://your-app-name.up.railway.app/health
```

### Send Test SMS
```bash
curl -X POST https://your-app-name.up.railway.app/send-emergency-sms \
  -H "Content-Type: application/json" \
  -d '{
    "seniorName": "Test Senior",
    "emergencyType": "SOS Button",
    "emergencyContactPhone": "+639123456789",
    "smsMessage": "🚨 SOS ALERT 🚨\nEmergency Alert: Test Senior may need immediate help. Please Try To Reach her/him.\n\n📍 Location: Test Location\n🩺 Emergency Type: SOS Button\n⏰ Timestamp: Dec 15, 2023 at 2:30 PM"
  }'
```

## SMS Message Format

The service sends SMS messages in the following format:

```
🚨 SOS ALERT 🚨
Emergency Alert: [Senior's Full Name] may need immediate help. Please Try To Reach her/him.

📍 Location: [Current Location of the Senior User]
🩺 Emergency Type: [Emergency Type]
⏰ Timestamp: [Date and Time when the senior user click the buttons]

🗺️ Click this Google Maps link for exact location:
https://maps.google.com/?q=lat,lng
```

## Emergency Types Supported

- SOS Button
- Davao Doctors Hospital
- Southern Philippines Medical Center (SPMC)
- Davao City Police Office (DCPO)
- Davao City Central Fire Station
- Central 911 Davao
- Davao City Ambulance Service
