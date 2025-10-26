// FCM Service for Admin Web
class FCMService {
    constructor() {
        this.messaging = null;
        this.token = null;
        this.init();
    }
    
    async init() {
        try {
            // Check if Firebase is available
            if (typeof firebase === 'undefined') {
                console.error('Firebase not loaded');
                return;
            }
            
            // Initialize Firebase Messaging
            this.messaging = firebase.messaging();
            
            // Request permission for notifications
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                // Get FCM token
                this.token = await this.messaging.getToken({
                    vapidKey: 'YOUR_VAPID_KEY' // Get this from Firebase Console
                });
                console.log('FCM Token:', this.token);
            }
        } catch (error) {
            console.error('Error initializing FCM:', error);
        }
    }
    
    async sendEmergencyAlert(seniorData, emergencyData) {
        try {
            // Send to your backend server
            const response = await fetch('/api/send-emergency-alert', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    seniorData,
                    emergencyData,
                    fcmToken: this.token
                })
            });
            
            return await response.json();
        } catch (error) {
            console.error('Error Sending Emergency Alert:', error);
            throw error;
        }
    }
}

// Initialize FCM Service
const fcmService = new FCMService();