// Sample Data Creation for Testing
class SampleDataManager {
    constructor() {
        this.init();
    }

    init() {
        // Add sample data creation methods to window for easy access
        window.createSampleEmergencyServices = () => this.createSampleEmergencyServices();
        window.createSampleEmergencyAlerts = () => this.createSampleEmergencyAlerts();
        window.createSampleHealthRecords = () => this.createSampleHealthRecords();
        window.createSampleBenefits = () => this.createSampleBenefits();
        window.createDavaoCityBenefits = () => this.createDavaoCityBenefits();
        window.createSampleSocialServices = () => this.createSampleSocialServices();
    }

    async createSampleEmergencyServices() {
        console.log('Creating sample emergency services...');
        const sampleServices = [
            {
                name: "Central 911 Davao",
                serviceType: "EMERGENCY",
                phoneNumber: "911",
                address: "Davao City Central 911",
                priority: 100,
                isActive: true,
                officeHours: "24/7",
                website: "",
                description: "Central emergency response system for Davao City",
                notes: "Primary emergency contact for all emergencies"
            },
            {
                name: "Davao Doctors Hospital",
                serviceType: "MEDICAL",
                phoneNumber: "(082) 222-8000",
                address: "Medical School Drive, Davao City",
                priority: 90,
                isActive: true,
                officeHours: "24/7",
                website: "https://davaodoctors.com",
                description: "Private hospital with emergency services",
                notes: "Full medical emergency services available"
            },
            {
                name: "Southern Philippines Medical Center",
                serviceType: "MEDICAL",
                phoneNumber: "(082) 227-2731",
                address: "J.P. Laurel Avenue, Davao City",
                priority: 85,
                isActive: true,
                officeHours: "24/7",
                website: "https://spmc.doh.gov.ph",
                description: "Government hospital with comprehensive medical services",
                notes: "Primary government hospital for Davao City"
            },
            {
                name: "Davao City Police Office",
                serviceType: "POLICE",
                phoneNumber: "(082) 227-1180",
                address: "San Pedro Street, Davao City",
                priority: 80,
                isActive: true,
                officeHours: "24/7",
                website: "",
                description: "Davao City Police Department",
                notes: "Law enforcement and public safety"
            },
            {
                name: "Davao City Central Fire Station",
                serviceType: "FIRE",
                phoneNumber: "(082) 224-3575",
                address: "Roxas Avenue, Davao City",
                priority: 75,
                isActive: true,
                officeHours: "24/7",
                website: "",
                description: "Fire and rescue services",
                notes: "Fire suppression and rescue operations"
            },
            {
                name: "Senior Citizens Hotline",
                serviceType: "SENIOR",
                phoneNumber: "09705416533",
                address: "Davao City Social Services",
                priority: 60,
                isActive: true,
                officeHours: "Mon-Fri 8AM-5PM",
                website: "",
                description: "Dedicated hotline for senior citizens",
                notes: "Specialized services for senior citizens"
            }
        ];

        try {
            for (const service of sampleServices) {
                await FirebaseUtils.addDoc(COLLECTIONS.EMERGENCY_SERVICES, {
                    ...service,
                    createdBy: currentUser().uid,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                });
            }
            console.log('Sample emergency services created successfully');
            alert('Sample emergency services created successfully!');
        } catch (e) {
            console.error('Error creating sample emergency services:', e);
            alert('Error creating sample emergency services: ' + e.message);
        }
    }

    async createSampleEmergencyAlerts() {
        console.log('Creating sample emergency alerts...');
        const sampleAlerts = [
            {
                id: "alert_001",
                userId: "senior_001",
                type: "FALL_DETECTED",
                severity: "HIGH",
                status: "ACTIVE",
                location: {
                    address: "123 Barangay Street, Davao City",
                    city: "Davao City",
                    province: "Davao del Sur",
                    latitude: 7.1907,
                    longitude: 125.4551
                },
                timestamp: Date.now() - 2 * 60 * 60 * 1000, // 2 hours ago
                description: "Fall detected by motion sensor in living room. Senior citizen may have fallen and needs immediate assistance.",
                notes: "Previous fall history noted. Check for injuries and assess mobility.",
                contactedServices: ["emergency_001", "emergency_002"],
                contactedFamily: ["contact_001", "contact_002"],
                responseTime: 0,
                resolvedAt: 0,
                resolvedBy: "",
                resolutionNotes: "",
                isAutoGenerated: true,
                triggeredBy: "health_monitor",
                deviceInfo: "Motion Sensor v2.1",
                batteryLevel: 85,
                signalStrength: 90,
                followUpRequired: true,
                followUpDate: Date.now() + 24 * 60 * 60 * 1000, // 24 hours from now
                attachments: []
            },
            {
                id: "alert_002",
                userId: "senior_002",
                type: "MEDICAL_EMERGENCY",
                severity: "CRITICAL",
                status: "IN_PROGRESS",
                location: {
                    address: "456 Senior Village, Davao City",
                    city: "Davao City",
                    province: "Davao del Sur",
                    latitude: 7.2000,
                    longitude: 125.4600
                },
                timestamp: Date.now() - 1 * 60 * 60 * 1000, // 1 hour ago
                description: "Senior citizen reported severe chest pain and difficulty breathing. Possible heart attack symptoms.",
                notes: "Patient has history of hypertension and diabetes. Administered first aid while waiting for ambulance.",
                contactedServices: ["emergency_001", "emergency_003"],
                contactedFamily: ["contact_003"],
                responseTime: 120000,
                resolvedAt: 0,
                resolvedBy: "",
                resolutionNotes: "",
                isAutoGenerated: false,
                triggeredBy: "user",
                deviceInfo: "Emergency Button v1.0",
                batteryLevel: 95,
                signalStrength: 88,
                followUpRequired: true,
                followUpDate: Date.now() + 12 * 60 * 60 * 1000, // 12 hours from now
                attachments: []
            },
            {
                id: "alert_003",
                userId: "senior_003",
                type: "DIABETIC_EMERGENCY",
                severity: "HIGH",
                status: "RESOLVED",
                location: {
                    address: "789 Health Center Road, Davao City",
                    city: "Davao City",
                    province: "Davao del Sur",
                    latitude: 7.1800,
                    longitude: 125.4500
                },
                timestamp: Date.now() - 4 * 60 * 60 * 1000, // 4 hours ago
                description: "Blood glucose level critically low. Senior citizen experiencing dizziness and confusion.",
                notes: "Administered glucose tablet. Patient stabilized after 15 minutes.",
                contactedServices: ["emergency_001"],
                contactedFamily: ["contact_004"],
                responseTime: 180000,
                resolvedAt: Date.now() - 3 * 60 * 60 * 1000, // 3 hours ago
                resolvedBy: "nurse_001",
                resolutionNotes: "Patient given glucose tablet and monitored for 30 minutes. Blood sugar normalized. Advised to eat regular meals.",
                isAutoGenerated: true,
                triggeredBy: "health_monitor",
                deviceInfo: "Glucose Monitor v3.0",
                batteryLevel: 70,
                signalStrength: 85,
                followUpRequired: true,
                followUpDate: Date.now() + 48 * 60 * 60 * 1000, // 48 hours from now
                attachments: []
            },
            {
                id: "alert_004",
                userId: "senior_004",
                type: "FIRE",
                severity: "LIFE_THREATENING",
                status: "RESOLVED",
                location: {
                    address: "321 Safety Street, Davao City",
                    city: "Davao City",
                    province: "Davao del Sur",
                    latitude: 7.2100,
                    longitude: 125.4700
                },
                timestamp: Date.now() - 6 * 60 * 60 * 1000, // 6 hours ago
                description: "Fire detected in kitchen area. Smoke alarm triggered. Immediate evacuation required.",
                notes: "Small kitchen fire caused by overheated cooking oil. Fire department responded quickly.",
                contactedServices: ["emergency_002", "emergency_004"],
                contactedFamily: ["contact_005", "contact_006"],
                responseTime: 300000,
                resolvedAt: Date.now() - 5 * 60 * 60 * 1000, // 5 hours ago
                resolvedBy: "firefighter_001",
                resolutionNotes: "Fire extinguished. Minor smoke damage to kitchen. Senior citizen evacuated safely. No injuries reported.",
                isAutoGenerated: true,
                triggeredBy: "system",
                deviceInfo: "Smoke Detector v2.5",
                batteryLevel: 100,
                signalStrength: 92,
                followUpRequired: true,
                followUpDate: Date.now() + 24 * 60 * 60 * 1000, // 24 hours from now
                attachments: []
            },
            {
                id: "alert_005",
                userId: "senior_005",
                type: "LOST_OR_CONFUSED",
                severity: "MEDIUM",
                status: "ACTIVE",
                location: {
                    address: "Unknown - GPS tracking enabled",
                    city: "Davao City",
                    province: "Davao del Sur",
                    latitude: 7.1950,
                    longitude: 125.4580
                },
                timestamp: Date.now() - 30 * 60 * 1000, // 30 minutes ago
                description: "Senior citizen reported feeling lost and confused while walking in the neighborhood. GPS tracking shows unusual movement pattern.",
                notes: "Patient has mild dementia. Family members notified and en route to location.",
                contactedServices: ["emergency_001"],
                contactedFamily: ["contact_007", "contact_008"],
                responseTime: 0,
                resolvedAt: 0,
                resolvedBy: "",
                resolutionNotes: "",
                isAutoGenerated: false,
                triggeredBy: "family",
                deviceInfo: "GPS Tracker v1.8",
                batteryLevel: 60,
                signalStrength: 75,
                followUpRequired: true,
                followUpDate: Date.now() + 12 * 60 * 60 * 1000, // 12 hours from now
                attachments: []
            }
        ];

        try {
            for (const alert of sampleAlerts) {
                await rtdb.ref(`emergency_alerts/${alert.id}`).set({
                    ...alert,
                    createdAt: Date.now(),
                    updatedAt: Date.now()
                });
            }
            console.log('Sample emergency alerts created successfully');
            alert('Sample emergency alerts created successfully!');
        } catch (e) {
            console.error('Error creating sample emergency alerts:', e);
            alert('Error creating sample emergency alerts: ' + e.message);
        }
    }

    async createSampleHealthRecords() {
        console.log('Creating sample health records...');
        const sampleRecords = [
            {
                seniorId: "senior001",
                seniorName: "Maria Santos",
                type: "blood_pressure",
                value: "120/80",
                unit: "mmHg",
                recordedBy: "admin",
                notes: "Regular checkup - normal reading",
                timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000) // 2 days ago
            },
            {
                seniorId: "senior001",
                seniorName: "Maria Santos",
                type: "blood_sugar",
                value: "95",
                unit: "mg/dL",
                recordedBy: "admin",
                notes: "Fasting blood sugar - normal",
                timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000) // 1 day ago
            },
            {
                seniorId: "senior002",
                seniorName: "Juan Cruz",
                type: "heart_rate",
                value: "72",
                unit: "bpm",
                recordedBy: "user",
                notes: "Resting heart rate - good",
                timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000) // 3 hours ago
            },
            {
                seniorId: "senior002",
                seniorName: "Juan Cruz",
                type: "weight",
                value: "65.5",
                unit: "kg",
                recordedBy: "admin",
                notes: "Monthly weight check",
                timestamp: new Date(Date.now() - 1 * 60 * 60 * 1000) // 1 hour ago
            },
            {
                seniorId: "senior003",
                seniorName: "Elena Rodriguez",
                type: "blood_pressure",
                value: "135/85",
                unit: "mmHg",
                recordedBy: "family",
                notes: "Slightly elevated - monitor closely",
                timestamp: new Date(Date.now() - 30 * 60 * 1000) // 30 minutes ago
            }
        ];

        try {
            for (const record of sampleRecords) {
                await FirebaseUtils.addDoc(COLLECTIONS.HEALTH_RECORDS, {
                    ...record,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                });
            }
            console.log('Sample health records created successfully');
            alert('Sample health records created successfully!');
        } catch (e) {
            console.error('Error creating sample health records:', e);
            alert('Error creating sample health records: ' + e.message);
        }
    }

    async createSampleBenefits() {
        console.log('Creating sample benefits...');
        const sampleBenefits = [
            {
                title: "Medical Service Assistance",
                category: "Medical Assistance",
                amount: "Free",
                status: "Available",
                description: "Free medical consultation and healthcare services for senior citizens in Davao City",
                requirements: "Senior Citizen ID, valid ID, proof of residence in Davao City",
                applicationProcess: "Visit DCMC or any participating health center with required documents",
                contactInfo: "DCMC: (082) 227-2731, JP Laurel Ave, Bajada",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            },
            {
                title: "DSWD Social Pension",
                category: "Financial Support",
                amount: "500",
                status: "Available",
                description: "₱500 monthly cash assistance for indigent senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of indigency, barangay certification",
                applicationProcess: "Submit application at DSWD Field Office XI with required documents",
                contactInfo: "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                website: "https://www.dswd.gov.ph",
                isActive: true,
                nextDisbursementDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000), // 15 days from now
                disbursementAmount: "500"
            },
            {
                title: "DSWD Food Assistance Program",
                category: "Food Assistance",
                amount: "Monthly Food Pack",
                status: "Available",
                description: "Monthly food packs and nutritional support for senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of need, barangay certification",
                applicationProcess: "Register at DSWD Field Office XI or through barangay",
                contactInfo: "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                website: "https://www.dswd.gov.ph",
                isActive: true,
                nextDisbursementDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 7 days from now
                disbursementAmount: "0"
            },
            {
                title: "Davao City Housing Program for Seniors",
                category: "Housing Support",
                amount: "Varies",
                status: "Available",
                description: "Housing assistance and emergency shelter for senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of housing need, income certificate",
                applicationProcess: "Submit application at City Housing Office with required documents",
                contactInfo: "City Housing Office: (082) 222-1234, City Hall",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            },
            {
                title: "Annual Financial Assistance",
                category: "Financial Support",
                amount: "3000",
                status: "Available",
                description: "₱3,000 yearly cash assistance during Christmas season",
                requirements: "Senior Citizen ID, valid ID, proof of residence in Davao City",
                applicationProcess: "Register at OSCA Davao during application period",
                contactInfo: "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                website: "",
                isActive: true,
                nextDisbursementDate: new Date(new Date().getFullYear(), 11, 15), // December 15th
                disbursementAmount: "3000"
            },
            {
                title: "Senior Citizen Discount",
                category: "Utility Discounts",
                amount: "20%",
                status: "Available",
                description: "20% discount on utilities, medicines, and transportation for senior citizens",
                requirements: "Senior Citizen ID, valid ID",
                applicationProcess: "Present Senior Citizen ID at participating establishments",
                contactInfo: "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            }
        ];

        try {
            for (const benefit of sampleBenefits) {
                const newBenefitRef = rtdb.ref('benefits').push();
                await newBenefitRef.set({
                    ...benefit,
                    createdBy: currentUser().uid,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString()
                });
            }
            console.log('Sample benefits created successfully');
            alert('Sample benefits created successfully!');
        } catch (e) {
            console.error('Error creating sample benefits:', e);
            alert('Error creating sample benefits: ' + e.message);
        }
    }

    async createDavaoCityBenefits() {
        console.log('Creating Davao City Senior Assistance benefits...');
        const davaoCityBenefits = [
            {
                title: "Medical Service Assistance",
                category: "Medical Assistance",
                amount: "Free",
                status: "Available",
                description: "Free medical consultation and healthcare services for senior citizens in Davao City",
                requirements: "Senior Citizen ID, valid ID, proof of residence in Davao City",
                applicationProcess: "Visit DCMC or any participating health center with required documents",
                contactInfo: "DCMC: (082) 227-2731, JP Laurel Ave, Bajada",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            },
            {
                title: "DSWD Social Pension",
                category: "Financial Support",
                amount: "500",
                status: "Available",
                description: "₱500 monthly cash assistance for indigent senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of indigency, barangay certification",
                applicationProcess: "Submit application at DSWD Field Office XI with required documents",
                contactInfo: "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                website: "https://www.dswd.gov.ph",
                isActive: true,
                nextDisbursementDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000), // 15 days from now
                disbursementAmount: "500"
            },
            {
                title: "DSWD Food Assistance Program",
                category: "Food Assistance",
                amount: "Monthly Food Pack",
                status: "Available",
                description: "Monthly food packs and nutritional support for senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of need, barangay certification",
                applicationProcess: "Register at DSWD Field Office XI or through barangay",
                contactInfo: "DSWD Field Office XI: (082) 224-1234, JP Laurel Ave, Bajada",
                website: "https://www.dswd.gov.ph",
                isActive: true,
                nextDisbursementDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 7 days from now
                disbursementAmount: "0"
            },
            {
                title: "Davao City Housing Program for Seniors",
                category: "Housing Support",
                amount: "Varies",
                status: "Available",
                description: "Housing assistance and emergency shelter for senior citizens",
                requirements: "Senior Citizen ID, valid ID, proof of housing need, income certificate",
                applicationProcess: "Submit application at City Housing Office with required documents",
                contactInfo: "City Housing Office: (082) 222-1234, City Hall",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            },
            {
                title: "Annual Financial Assistance",
                category: "Financial Support",
                amount: "3000",
                status: "Available",
                description: "₱3,000 yearly cash assistance during Christmas season",
                requirements: "Senior Citizen ID, valid ID, proof of residence in Davao City",
                applicationProcess: "Register at OSCA Davao during application period",
                contactInfo: "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                website: "",
                isActive: true,
                nextDisbursementDate: new Date(new Date().getFullYear(), 11, 15), // December 15th
                disbursementAmount: "3000"
            },
            {
                title: "Senior Citizen Discount",
                category: "Utility Discounts",
                amount: "20%",
                status: "Available",
                description: "20% discount on utilities, medicines, and transportation for senior citizens",
                requirements: "Senior Citizen ID, valid ID",
                applicationProcess: "Present Senior Citizen ID at participating establishments",
                contactInfo: "OSCA Davao: (082) 222-1234, City Hall Ground Floor",
                website: "",
                isActive: true,
                nextDisbursementDate: null,
                disbursementAmount: "0"
            }
        ];

        try {
            for (const benefit of davaoCityBenefits) {
                const newBenefitRef = rtdb.ref('benefits').push();
                await newBenefitRef.set({
                    ...benefit,
                    createdBy: currentUser().uid,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString()
                });
            }
            console.log('Davao City Senior Assistance benefits created successfully');
            alert('Davao City Senior Assistance benefits created successfully!');
        } catch (e) {
            console.error('Error creating Davao City benefits:', e);
            alert('Error creating Davao City benefits: ' + e.message);
        }
    }

    async createSampleSocialServices() {
        console.log('Creating sample social services...');
        const sampleServices = [
            {
                name: "DSWD Davao City Office",
                serviceType: "GOVERNMENT",
                phoneNumber: "(082) 222-9999",
                email: "davao@dswd.gov.ph",
                contact: "Department of Social Welfare and Development",
                address: "J.P. Laurel Avenue, Davao City",
                officeHours: "Mon-Fri 8AM-5PM",
                website: "https://www.dswd.gov.ph",
                priority: 100,
                isActive: true,
                servicesOffered: "Social pension\nCash assistance\nFood assistance\nDisaster relief",
                notes: "Primary government social services office"
            },
            {
                name: "Barangay 1-A Social Services",
                serviceType: "BARANGAY",
                phoneNumber: "(082) 222-1111",
                email: "brgy1a@davaocity.gov.ph",
                contact: "Barangay Captain Office",
                address: "Barangay 1-A, Davao City",
                officeHours: "Mon-Fri 8AM-5PM",
                website: "",
                priority: 80,
                isActive: true,
                servicesOffered: "Barangay ID\nCommunity assistance\nLocal programs",
                notes: "Local barangay social services"
            },
            {
                name: "Davao City Senior Citizens Association",
                serviceType: "NGO",
                phoneNumber: "(082) 222-2222",
                email: "seniors@davao.org",
                contact: "Association President",
                address: "Senior Citizens Center, Davao City",
                officeHours: "Mon-Fri 9AM-4PM",
                website: "https://davaoseniors.org",
                priority: 70,
                isActive: true,
                servicesOffered: "Social activities\nHealth programs\nEducational seminars\nRecreation",
                notes: "Non-profit organization for senior citizens"
            },
            {
                name: "Catholic Charities Davao",
                serviceType: "NGO",
                phoneNumber: "(082) 222-3333",
                email: "info@catholiccharitiesdavao.org",
                contact: "Program Director",
                address: "Catholic Center, Davao City",
                officeHours: "Mon-Fri 8AM-5PM",
                website: "https://catholiccharitiesdavao.org",
                priority: 60,
                isActive: true,
                servicesOffered: "Spiritual support\nCommunity outreach\nEmergency assistance\nCounseling",
                notes: "Religious-based social services"
            }
        ];

        try {
            for (const service of sampleServices) {
                const newRef = rtdb.ref('social_services').push();
                await newRef.set(service);
            }
            console.log('Sample social services created successfully');
            alert('Sample social services created successfully!');
        } catch (e) {
            console.error('Error creating sample social services:', e);
            alert('Error creating sample social services: ' + e.message);
        }
    }
}

// Initialize sample data manager
const sampleDataManager = new SampleDataManager();
window.sampleDataManager = sampleDataManager;
