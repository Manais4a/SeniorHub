// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyAVJToEKOhI1E7BobvJDD5v949sSpgYdLs",
    authDomain: "seniorhub-admin-508eb.firebaseapp.com",
    databaseURL: "https://seniorhub-admin-508eb-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "seniorhub-admin-508eb",
    storageBucket: "seniorhub-admin-508eb.firebasestorage.app",
    messagingSenderId: "895180010431",
    appId: "1:895180010431:web:05a8c957a66ca4e2b8a70a",
    measurementId: "G-4JYG7NVWV3"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

// Initialize Firebase services
const auth = firebase.auth();
const db = firebase.firestore();
const analytics = firebase.analytics();
const rtdb = firebase.database();

// Set auth persistence to LOCAL
auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL)
    .then(() => {
        console.log('Auth persistence set to LOCAL');
    })
    .catch((error) => {
        console.error('Error setting auth persistence:', error);
    });

// Firebase collections
const COLLECTIONS = {
    USERS: 'users',
    SENIORS: 'seniors',
    BENEFITS: 'benefits',
    APPOINTMENTS: 'appointments',
    EMERGENCY_ALERTS: 'emergency_alerts',
    EMERGENCY_SERVICES: 'emergency_services',
    HEALTH_RECORDS: 'health_records',
    ADMIN_USERS: 'admin_users',
    DATA_COLLECTION: 'data_collection',
    ACTIVITIES: 'activities'
};

// Admin roles
const ADMIN_ROLES = {
    FACILITATOR: 'facilitator',
    SUPER_ADMIN: 'super_admin'
};

// Current user state
let currentUser = null;
let currentUserRole = null;

// Single global auth state listener
let globalAuthListener = null;

function initializeGlobalAuthListener() {
    if (globalAuthListener) {
        return; // Don't create multiple listeners
    }
    
    globalAuthListener = auth.onAuthStateChanged((user) => {
        console.log('Global Auth state changed:', user ? `User: ${user.email}` : 'No user');
        
        if (user) {
            currentUser = user;
            console.log('User authenticated:', user.email, 'UID:', user.uid);
            // Don't automatically load user role here - let individual pages handle it
        } else {
            console.log('User logged out or not authenticated');
            currentUser = null;
            currentUserRole = null;
        }
    }, (error) => {
        console.error('Auth state change error:', error);
    });
}

// Initialize the global auth listener only once
initializeGlobalAuthListener();

// Utility functions for Firebase operations
const FirebaseUtils = {
    // Get current timestamp
    getTimestamp: () => firebase.firestore.Timestamp.now(),
    
    // Convert timestamp to readable date
    formatDate: (timestamp) => {
        if (!timestamp) return 'N/A';
        const date = timestamp.toDate();
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    },
    
    // Get document with error handling
    getDoc: async (collection, docId) => {
        try {
            const doc = await db.collection(collection).doc(docId).get();
            return doc.exists ? { id: doc.id, ...doc.data() } : null;
        } catch (error) {
            console.error(`Error getting document ${docId}:`, error);
            return null;
        }
    },
    
    // Get collection with error handling
    getCollection: async (collection, orderBy = null, limit = null) => {
        try {
            let query = db.collection(collection);
            if (orderBy) {
                query = query.orderBy(orderBy);
            }
            if (limit) {
                query = query.limit(limit);
            }
            const snapshot = await query.get();
            return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        } catch (error) {
            console.error(`Error getting collection ${collection}:`, error);
            return [];
        }
    },
    
    // Add document with error handling
    addDoc: async (collection, data) => {
        try {
            const docRef = await db.collection(collection).add({
                ...data,
                createdAt: FirebaseUtils.getTimestamp(),
                updatedAt: FirebaseUtils.getTimestamp()
            });
            return docRef.id;
        } catch (error) {
            console.error(`Error adding document to ${collection}:`, error);
            throw error;
        }
    },
    
    // Update document with error handling
    updateDoc: async (collection, docId, data) => {
        try {
            await db.collection(collection).doc(docId).update({
                ...data,
                updatedAt: FirebaseUtils.getTimestamp()
            });
            return true;
        } catch (error) {
            console.error(`Error updating document ${docId}:`, error);
            throw error;
        }
    },
    
    // Delete document with error handling
    deleteDoc: async (collection, docId) => {
        try {
            await db.collection(collection).doc(docId).delete();
            return true;
        } catch (error) {
            console.error(`Error deleting document ${docId}:`, error);
            throw error;
        }
    },
    
    // Real-time listener
    onSnapshot: (collection, callback, orderBy = null) => {
        let query = db.collection(collection);
        if (orderBy) {
            query = query.orderBy(orderBy);
        }
        return query.onSnapshot(callback, (error) => {
            console.error(`Error in snapshot listener for ${collection}:`, error);
        });
    },

    // Complete user deletion utility with email reuse support
    deleteUserCompletely: async (userId, userEmail = null) => {
        try {
            console.log(`Starting complete deletion for user: ${userId}`);
            console.log('Current user:', currentUser ? currentUser.uid : 'No current user');
            console.log('User email:', userEmail);
            
            // Verify admin status before proceeding
            if (!currentUser) {
                throw new Error('No authenticated user found');
            }
            
            // Check if current user is an admin
            const adminDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(currentUser.uid).get();
            if (!adminDoc.exists) {
                throw new Error('Current user is not an admin');
            }
            
            const adminData = adminDoc.data();
            const userRole = (adminData.role || '').toLowerCase().trim();
            
            // Verify the user has the correct role for deletion
            if (!['admin'].includes(userRole)) {
                throw new Error(`Insufficient permissions. Required role: admin. Current role: ${userRole}`);
            }
            
            console.log('Admin verification successful:', {
                uid: currentUser.uid,
                email: currentUser.email,
                role: adminData.role,
                isActive: adminData.isActive,
                authorized: true
            });
            
            // Delete from all related collections
            const relatedCollections = [
                { collection: COLLECTIONS.HEALTH_RECORDS, field: 'seniorId' },
                { collection: COLLECTIONS.EMERGENCY_ALERTS, field: 'seniorId' },
                { collection: COLLECTIONS.DATA_COLLECTION, field: 'seniorId' },
                { collection: COLLECTIONS.ACTIVITIES, field: 'seniorId' }
            ];

            // Delete related documents
            for (const { collection, field } of relatedCollections) {
                try {
                    console.log(`Deleting related docs from ${collection} where ${field} = ${userId}`);
                    const q = await db.collection(collection).where(field, '==', userId).get();
                    console.log(`Found ${q.size} documents in ${collection}`);
                    const deletes = [];
                    q.forEach(doc => deletes.push(db.collection(collection).doc(doc.id).delete()));
                    await Promise.all(deletes);
                    console.log(`Deleted related docs from ${collection}`);
                } catch (error) {
                    console.warn(`Failed to delete from ${collection}:`, error);
                }
            }

            // Instead of deleting the user document, mark it as deleted and store email for reuse
            if (userEmail) {
                console.log('Creating deleted user record...');
                // Create a deleted user record to track email reuse
                await db.collection('deleted_users').doc(userId).set({
                    originalUserId: userId,
                    email: userEmail,
                    deletedAt: firebase.firestore.Timestamp.now(),
                    deletedBy: currentUser.uid,
                    reason: 'Admin deletion'
                });
                console.log(`Email ${userEmail} marked for reuse in deleted_users collection`);
            }

            // Actually delete the user document (this should work with your rules)
            console.log('Deleting user document...');
            console.log('Attempting to delete document:', `${COLLECTIONS.USERS}/${userId}`);
            console.log('Current user context:', {
                uid: currentUser.uid,
                email: currentUser.email,
                role: adminData.role
            });
            
            // Test the delete permission first
            console.log('Testing delete permission...');
            const testDoc = db.collection(COLLECTIONS.USERS).doc(userId);
            
            try {
                // First, let's try to read the document to make sure it exists
                const docSnapshot = await testDoc.get();
                if (!docSnapshot.exists) {
                    throw new Error('User document does not exist');
                }
                console.log('User document exists, proceeding with delete...');
                
                // Now attempt the delete
                await testDoc.delete();
                console.log(`User ${userId} document deleted successfully`);
            } catch (deleteError) {
                console.error('Delete operation failed:', deleteError);
                console.error('Delete error details:', {
                    code: deleteError.code,
                    message: deleteError.message,
                    details: deleteError.details,
                    stack: deleteError.stack
                });
                
                // Check if it's a permission error specifically
                if (deleteError.code === 'permission-denied') {
                    console.error('PERMISSION DENIED ERROR - This suggests a Firestore rules issue');
                    console.error('Current user role:', adminData.role);
                    console.error('Expected roles for delete: facilitator, super_admin');
                    console.error('User UID:', currentUser.uid);
                    console.error('Target document:', `${COLLECTIONS.USERS}/${userId}`);
                }
                
                // If delete fails, try to mark as deleted instead
                console.log('Attempting fallback: marking user as deleted...');
                const updateData = {
                    isDeleted: true,
                    deletedAt: firebase.firestore.Timestamp.now(),
                    deletedBy: currentUser.uid,
                    originalEmail: userEmail,
                    deleteError: deleteError.message
                };
                
                try {
                    await db.collection(COLLECTIONS.USERS).doc(userId).update(updateData);
                    console.log(`User ${userId} marked as deleted (fallback method)`);
                } catch (updateError) {
                    console.error('Fallback update also failed:', updateError);
                    throw new Error(`Both delete and update failed. Delete error: ${deleteError.message}. Update error: ${updateError.message}`);
                }
            }

            return true;
        } catch (error) {
            console.error('Error in complete user deletion:', error);
            console.error('Error details:', {
                message: error.message,
                code: error.code,
                stack: error.stack
            });
            throw error;
        }
    }
};

// Export for use in other files
window.FirebaseUtils = FirebaseUtils;
window.COLLECTIONS = COLLECTIONS;
window.ADMIN_ROLES = ADMIN_ROLES;
window.currentUser = () => currentUser;
window.currentUserRole = () => currentUserRole;
window.rtdb = rtdb;

// Resident/Senior verification helpers
window.Verification = {
    // Simple heuristic verification: requires barangay + city in Davao, age >= 60 for senior
    isResident: (data) => {
        if (!data) return false;
        const city = (data.city || '').trim().toLowerCase();
        const barangay = (data.barangay || '').trim();
        return Boolean(barangay) && (city === 'davao city');
    },
    isSenior: (data) => {
        if (!data) return false;
        const age = Number(data.age || 0);
        return !Number.isNaN(age) && age >= 60;
    },
    // Combined check used by Facilitator Admin when creating accounts
    verifyResidentOrSenior: (data) => {
        const resident = window.Verification.isResident(data);
        const senior = window.Verification.isSenior(data);
        return { resident, senior, passed: resident || senior };
    }
};