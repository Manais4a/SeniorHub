# SeniorHub Emergency SOS System Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing the enhanced emergency SOS functionality in the SeniorHub Android application. The system allows senior users to trigger emergency alerts that simultaneously call emergency services and send SMS notifications to their emergency contacts.

## System Architecture

```
[Android App] → [Senior Clicks Emergency Button] → [Firebase Database] → [Railway Cloud Function] → [Semaphore API] → [SMS Delivery]
```

## Features Implemented

### 1. SOS Button (MainActivity)
- **Location**: `activity_main.xml` - `btnSOS` button
- **Functionality**: 
  - Sends SMS to emergency contact
  - Calls emergency services (911)
  - Includes location information
  - Provides voice feedback

### 2. Emergency Service Buttons (EmergencyServicesActivity)
- **Buttons Implemented**:
  - `btnCall911Central` - Central 911 Davao
  - `btnCallDavaoDoctors` - Davao Doctors Hospital
  - `btnCallSPMC` - Southern Philippines Medical Center
  - `btnCallDCPO` - Davao City Police Office
  - `btnCallFireStation` - Davao City Central Fire Station
  - `btnCallEmergencyResponseUnit` - Emergency Response Unit
  - `btnCallAmbulanceService` - Ambulance Service

### 3. SMS Message Format
The system sends SMS messages in the exact format requested:

```
🚨 SOS ALERT 🚨
Emergency Alert: [Senior's Full Name] may need immediate help. Please Try To Reach her/him.

📍 Location: Current Location of the Senior User
🩺 Emergency Type: [Emergency Type]
⏰ Timestamp: [Date and Time when the senior user click the buttons]

🗺️ Click this Google Maps link for exact location:
https://maps.google.com/?q=lat,lng
```

## Step-by-Step Implementation Guide

### Step 1: Android App Configuration

#### 1.1 Update MainActivity.kt
The SOS button functionality has been enhanced in `MainActivity.kt`:

```kotlin
private fun activateEmergencySOS() {
    // Get current user name for emergency alert
    val currentUser = FirebaseManager.getCurrentUser()
    val seniorName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Senior User"

    // Launch coroutine to handle emergency alert
    lifecycleScope.launch {
        try {
            // Get current location
            val location = locationService.getCurrentLocation()

            // Send emergency alert with location to emergency contacts only
            when (val result = emergencyService.sendEmergencyAlert(
                emergencyType = "SOS Button",
                seniorName = seniorName,
                location = location
            )) {
                is Result.Success -> {
                    // Also call emergency services (911)
                    callEmergencyServices("911")
                    
                    // Provide feedback - alert sent successfully
                    speakText("Emergency Alert Sent to your Emergency Contact. Calling emergency services. Help is on the way.")
                    Toast.makeText(this@MainActivity, "Emergency alert sent to your emergency contact with location. Calling emergency services. Help is on the way.", Toast.LENGTH_LONG).show()
                }
                is Result.Error -> {
                    // Still call emergency services even if SMS fails
                    callEmergencyServices("911")
                    
                    // Alert failed to send
                    speakText("Failed to send SMS alert. Calling emergency services directly. Please contact your emergency contact manually.")
                    Toast.makeText(this@MainActivity, "Failed to send SMS alert. Calling emergency services directly. Please contact your emergency contact manually.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in emergency SOS: ${e.message}", e)
            // Still call emergency services as fallback
            callEmergencyServices("911")
        }
    }
}
```

#### 1.2 Update EmergencyServicesActivity.kt
All emergency service buttons have been enhanced to:
- Call the specific emergency service
- Send SMS to emergency contact with location
- Provide appropriate feedback

#### 1.3 Update EmergencyService.kt
Enhanced the `EmergencyService` class to:
- Create properly formatted SMS messages
- Integrate with Railway Cloud Function
- Handle location data properly

### Step 2: Railway Cloud Function Setup

#### 2.1 Create Railway Account
1. Go to [Railway.app](https://railway.app/)
2. Sign up for a free account
3. Connect your GitHub account

#### 2.2 Deploy Cloud Function
1. Create a new project in Railway
2. Connect your GitHub repository
3. Deploy the `railway-cloud-function` folder
4. Set environment variables (see Step 3)

#### 2.3 Update API URL
In `EmergencyService.kt`, update the API URL:
```kotlin
private val API_BASE_URL = "https://your-app-name.up.railway.app"
```

### Step 3: Semaphore SMS API Setup

#### 3.1 Get Semaphore API Key
1. Go to [Semaphore SMS](https://semaphore.co/)
2. Sign up for an account
3. Navigate to API section in your dashboard
4. Generate an API key
5. Copy the API key

#### 3.2 Configure Environment Variables
In your Railway project, set these environment variables:
```
SEMAPHORE_API_KEY=your_semaphore_api_key_here
SEMAPHORE_SENDER_NAME=SeniorHub
NODE_ENV=production
```

### Step 4: Firebase Configuration

#### 4.1 Update Firebase Rules
Ensure your Firebase Firestore rules allow emergency alert creation:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /emergency_alerts/{document} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### 4.2 Test Firebase Integration
Verify that the app can:
- Read user profile data
- Access emergency contact information
- Write emergency alerts to Firestore

### Step 5: Android Permissions

#### 5.1 Update AndroidManifest.xml
Ensure these permissions are included:

```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

#### 5.2 Runtime Permission Handling
The app already handles runtime permissions for:
- SMS sending
- Phone calls
- Location access

### Step 6: Testing the Implementation

#### 6.1 Test SOS Button
1. Open the app
2. Click the SOS button
3. Grant required permissions
4. Verify:
   - SMS is sent to emergency contact
   - Emergency services are called
   - Location is included in SMS

#### 6.2 Test Emergency Service Buttons
1. Navigate to Emergency Services
2. Click any emergency service button
3. Verify:
   - Specific service is called
   - SMS is sent to emergency contact
   - Location is included in SMS

#### 6.3 Test SMS Format
Verify SMS messages contain:
- 🚨 SOS ALERT 🚨 header
- Senior's full name
- Current location
- Emergency type
- Timestamp
- Google Maps link (if location available)

### Step 7: Emergency Contact Configuration

#### 7.1 Profile Setup
Ensure senior users have configured their emergency contact in `activity_profile.xml`:
- Emergency Contact Name (`etEmergencyContactName`)
- Emergency Contact Phone (`etEmergencyContactPhone`)
- Relationship (`etRelationship`)

#### 7.2 Data Validation
The system validates:
- Emergency contact phone number exists
- Phone number format is correct
- User profile is complete

## Troubleshooting

### Common Issues

#### 1. SMS Not Sending
- Check Semaphore API key configuration
- Verify phone number format
- Check Railway deployment logs

#### 2. Location Not Available
- Ensure location permissions are granted
- Check GPS is enabled
- Verify location services are working

#### 3. Emergency Calls Not Working
- Check phone permission
- Verify phone number format
- Test with different emergency numbers

#### 4. Firebase Connection Issues
- Verify Firebase configuration
- Check internet connectivity
- Review Firebase console logs

### Debug Steps

1. **Check Logs**: Review Android Studio logs for errors
2. **Test API**: Use curl to test Railway endpoint
3. **Verify Permissions**: Ensure all permissions are granted
4. **Check Network**: Verify internet connectivity
5. **Test SMS**: Send test SMS manually

## Security Considerations

### 1. API Security
- Use HTTPS for all API calls
- Implement rate limiting
- Validate input data

### 2. Data Privacy
- Encrypt sensitive data
- Follow GDPR guidelines
- Implement data retention policies

### 3. Emergency Contact Privacy
- Only send SMS to verified contacts
- Include opt-out instructions
- Respect contact preferences

## Performance Optimization

### 1. Location Services
- Use cached location when possible
- Implement location timeout
- Handle location errors gracefully

### 2. SMS Delivery
- Implement retry logic
- Use background processing
- Optimize message length

### 3. API Calls
- Implement timeout handling
- Use connection pooling
- Cache frequently used data

## Monitoring and Analytics

### 1. Emergency Alert Tracking
- Log all emergency alerts
- Track SMS delivery status
- Monitor response times

### 2. User Analytics
- Track emergency button usage
- Monitor location accuracy
- Analyze emergency patterns

### 3. System Health
- Monitor API uptime
- Track error rates
- Alert on system failures

## Future Enhancements

### 1. Additional Features
- Multiple emergency contacts
- Emergency contact verification
- Automated follow-up messages

### 2. Integration Options
- Integration with local emergency services
- GPS tracking during emergencies
- Medical information sharing

### 3. Accessibility Improvements
- Voice-activated emergency alerts
- Large button options
- Multi-language support

## Support and Maintenance

### 1. Regular Updates
- Keep dependencies updated
- Monitor API changes
- Update emergency contact lists

### 2. User Training
- Provide user guides
- Conduct training sessions
- Create video tutorials

### 3. Emergency Procedures
- Document emergency procedures
- Train support staff
- Create escalation processes

## Conclusion

This implementation provides a comprehensive emergency SOS system for senior citizens using the SeniorHub Android application. The system ensures that emergency alerts are sent to designated contacts while simultaneously calling appropriate emergency services, providing peace of mind for both seniors and their families.

The modular architecture allows for easy maintenance and future enhancements, while the cloud-based SMS service ensures reliable message delivery through the Semaphore API platform.
