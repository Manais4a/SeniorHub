// Senior Citizens Management
class SeniorsManager {
    constructor() {
        this.seniors = [];
        this.filteredSeniors = [];
        this.currentFilter = '';
        this.currentSearch = '';
        this.isLoading = false;
        this.dataLoaded = false;
        this.realtimeSetupComplete = false;
        
        // Cloudinary Configuration (loaded from cloudinary-config.js)
        this.cloudinaryConfig = window.CLOUDINARY_CONFIG;
        
        if (!this.cloudinaryConfig) {
            console.error('Cloudinary configuration not found! Make sure cloudinary-config.js is loaded before seniors.js');
            this.cloudinaryConfig = {
                cloudName: 'dftnlg6nh',
                uploadPreset: 'senior-hub-upload'
            };
        }
        
        console.log('Cloudinary config loaded:', this.cloudinaryConfig);
        
        this.init();
    }

    /**
     * Validate if URL is a valid Cloudinary URL
     * @param {string} url URL to validate
     * @returns {boolean} True if valid Cloudinary URL
     */
    isValidCloudinaryUrl(url) {
        return url && 
               url.startsWith('https://res.cloudinary.com/') && 
               url.includes('/image/upload/');
    }

    /**
     * Get optimized Cloudinary URL with transformations
     * @param {string} originalUrl Original Cloudinary URL
     * @param {number} width Desired width
     * @param {number} height Desired height
     * @returns {string} Optimized URL
     */
    getOptimizedCloudinaryUrl(originalUrl, width = 400, height = 400) {
        if (!this.isValidCloudinaryUrl(originalUrl)) return originalUrl;

        try {
            const marker = '/image/upload/';
            const idx = originalUrl.indexOf(marker);
            if (idx === -1) return originalUrl;

            const prefix = originalUrl.substring(0, idx + marker.length);
            // Preserve folders/version/publicId + extension
            const restPath = originalUrl.substring(idx + marker.length);

            return `${prefix}w_${width},h_${height},c_fill,g_face,q_auto,f_auto/${restPath}`;
        } catch (e) {
            console.warn('Error optimizing Cloudinary URL:', e);
            return originalUrl;
        }
    }

    init() {
        this.setupEventListeners();
        this.loadSeniors();
    }

    /**
     * Map Android app data format to admin dashboard format
     */
    mapSeniorData(senior) {
        console.log('Mapping senior data:', senior);
        console.log('Available fields:', Object.keys(senior));
        console.log('Birthday fields:', {
            birthday: senior.birthday,
            birthDate: senior.birthDate,
            birthdate: senior.birthdate
        });
        
        // Extract emergency contact information from array format
        let emergencyContactName = 'N/A';
        let emergencyContactPhone = 'N/A';
        let relationship = 'N/A';
        
        console.log('Emergency contacts:', senior.emergencyContacts);
        if (senior.emergencyContacts && Array.isArray(senior.emergencyContacts) && senior.emergencyContacts.length > 0) {
            const primaryContact = senior.emergencyContacts.find(contact => contact.isPrimary) || senior.emergencyContacts[0];
            emergencyContactName = primaryContact.name || 'N/A';
            emergencyContactPhone = primaryContact.phoneNumber || 'N/A';
            relationship = primaryContact.relationship || 'N/A';
            console.log('Mapped emergency contact:', { emergencyContactName, emergencyContactPhone, relationship });
        }
        
        // Map membership information - check if numbers exist to determine membership status
        const sssMember = !!(senior.sssNumber && senior.sssNumber.trim() !== '');
        const gsisMember = !!(senior.gsisNumber && senior.gsisNumber.trim() !== '');
        const oscaMember = !!(senior.oscaNumber && senior.oscaNumber.trim() !== '');
        const philHealthMember = !!(senior.philHealthNumber && senior.philHealthNumber.trim() !== '');
        
        console.log('Membership data:', {
            sssNumber: senior.sssNumber,
            gsisNumber: senior.gsisNumber,
            oscaNumber: senior.oscaNumber,
            philHealthNumber: senior.philHealthNumber,
            sssMember,
            gsisMember,
            oscaMember,
            philHealthMember
        });
        
        // Handle birthday formatting - check both 'birthday' and 'birthDate' fields
        let formattedBirthday = 'N/A';
        const birthdayField = senior.birthday || senior.birthDate;
        
        console.log('Birthday field found:', birthdayField);
        
        if (birthdayField) {
            try {
                // Handle both Firestore Timestamp and regular date formats
                let birthdayDate;
                if (birthdayField.toDate) {
                    birthdayDate = birthdayField.toDate();
                } else if (typeof birthdayField === 'string') {
                    birthdayDate = new Date(birthdayField);
                } else {
                    birthdayDate = new Date(birthdayField);
                }
                
                // Check if date is valid
                if (isNaN(birthdayDate.getTime())) {
                    console.warn('Invalid date:', birthdayField);
                    formattedBirthday = 'Invalid Date';
                } else {
                    formattedBirthday = birthdayDate.toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric'
                    });
                }
                console.log('Formatted birthday:', formattedBirthday);
            } catch (e) {
                console.warn('Error formatting birthday:', e);
                formattedBirthday = `Raw: ${birthdayField}`;
            }
        } else {
            console.log('No birthday field found in senior data');
        }
        
        const mappedData = {
            ...senior,
            // Emergency contact mapping
            emergencyContactName,
            emergencyContactPhone,
            relationship,
            
            // Membership mapping
            sssMember,
            gsisMember,
            oscaMember,
            philHealthMember,
            
            // Birthday formatting
            birthday: formattedBirthday,
            originalBirthday: birthdayField, // Keep original for editing
            
            // Ensure other fields exist
            phoneNumber: senior.phoneNumber || 'N/A',
            age: senior.age || 'N/A',
            houseNumberAndStreet: senior.houseNumberAndStreet || 'N/A',
            barangay: senior.barangay || 'N/A',
            city: senior.city || 'Davao City',
            province: senior.province || 'Davao Del Sur',
            gender: senior.gender || 'N/A',
            maritalStatus: senior.maritalStatus || 'N/A'
        };
        
        console.log('Final mapped data:', mappedData);
        return mappedData;
    }

    setupEventListeners() {
        // Search functionality
        document.getElementById('seniorSearch').addEventListener('input', (e) => {
            this.currentSearch = e.target.value.toLowerCase();
            this.filterSeniors();
        });

        // Filter functionality
        document.getElementById('seniorFilter').addEventListener('change', (e) => {
            this.currentFilter = e.target.value;
            this.filterSeniors();
        });

        // Real-time updates
        this.setupRealtimeUpdates();
    }

    setupRealtimeUpdates() {
        // Only set up real-time updates if not already set up
        if (this.realtimeSetupComplete) {
            console.log('Real-time updates already set up for seniors, skipping...');
            return;
        }
        
        console.log('Setting up real-time updates for seniors...');
        
        // Listen for real-time updates from Firebase
        FirebaseUtils.onSnapshot(COLLECTIONS.USERS, async (snapshot) => {
            console.log('Real-time update received, processing', snapshot.size, 'documents');
            this.seniors = [];
            let deletedCount = 0;
            let seniorCount = 0;
            let excludedByRole = 0;
            let excludedByAge = 0;
            
            // Get deleted user IDs from user_deletions collection as fallback
            let deletedUserIds = new Set();
            try {
                const deletionsSnapshot = await db.collection('user_deletions').get();
                deletionsSnapshot.forEach(doc => {
                    const data = doc.data();
                    if (data.isDeleted && data.userId) {
                        deletedUserIds.add(data.userId);
                    }
                });
            } catch (error) {
                console.warn('Could not check user_deletions collection in real-time:', error);
            }
            
            snapshot.forEach(doc => {
                const data = doc.data();
                const role = (data.role || '').toLowerCase().trim();
                const age = Number(data.age || 0);
                
                console.log('Processing user:', {
                    id: doc.id,
                    name: `${data.firstName || ''} ${data.lastName || ''}`,
                    role: role,
                    age: age,
                    isDeleted: data.isDeleted,
                    isActive: data.isActive
                });
                
                // Exclude deleted users (check both isDeleted flag and user_deletions collection)
                if (data.isDeleted || deletedUserIds.has(doc.id)) {
                    console.log('Excluding deleted user:', doc.id, data.firstName, data.lastName);
                    deletedCount++;
                    return;
                }
                
                // Check role-based filtering
                const hasSeniorRole = role === 'senior_citizen' || role === 'senior' || role === 'citizen';
                const isSeniorAge = age >= 60;
                
                if (hasSeniorRole) {
                    // Map Android app data format to admin dashboard format
                    const mappedData = this.mapSeniorData({ id: doc.id, ...data });
                    this.seniors.push(mappedData);
                    seniorCount++;
                    console.log('Added senior by role:', doc.id, data.firstName, data.lastName);
                } else if (isSeniorAge && !role.includes('admin')) {
                    // Map Android app data format to admin dashboard format
                    const mappedData = this.mapSeniorData({ id: doc.id, ...data });
                    this.seniors.push(mappedData);
                    seniorCount++;
                    console.log('Added senior by age:', doc.id, data.firstName, data.lastName);
                } else {
                    console.log('Excluded user - no senior role or age:', doc.id, { role, age });
                    if (!hasSeniorRole) excludedByRole++;
                    if (!isSeniorAge) excludedByAge++;
                }
            });
            console.log('Real-time update: Found', seniorCount, 'senior citizens, excluded', deletedCount, 'deleted users,', excludedByRole, 'by role,', excludedByAge, 'by age');
            this.filterSeniors();
            this.updateStats();
        });
        
        this.realtimeSetupComplete = true;
        console.log('Real-time updates setup completed for seniors');
    }

    async loadSeniors(retryCount = 0) {
        // Prevent multiple simultaneous loads
        if (this.isLoading) {
            console.log('Seniors data already loading, skipping...');
            return;
        }
        
        // If data is already loaded, don't reload unless forced
        if (this.dataLoaded && !this.forceReload) {
            console.log('Seniors data already loaded, skipping...');
            return;
        }
        
        this.isLoading = true;
        
        try {
            this.showLoading();
            console.log('Loading senior citizens data... (attempt:', retryCount + 1, ')');
            
            // Check if user is authenticated
            const currentUser = firebase.auth().currentUser;
            if (!currentUser) {
                throw new Error('User not authenticated');
            }
            
            console.log('Current user:', currentUser.email);
            
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            console.log('Users loaded:', users.length);
            
            // Debug: Log all users and their roles
            console.log('All users with roles:', users.map(user => ({
                id: user.id,
                name: `${user.firstName || ''} ${user.lastName || ''}`,
                role: user.role,
                email: user.email,
                age: user.age,
                accountVerified: user.accountVerified
            })));
            
            // Get deleted user IDs from user_deletions collection as fallback
            let deletedUserIds = new Set();
            try {
                const deletionsSnapshot = await db.collection('user_deletions').get();
                deletionsSnapshot.forEach(doc => {
                    const data = doc.data();
                    if (data.isDeleted && data.userId) {
                        deletedUserIds.add(data.userId);
                    }
                });
                console.log('Found deleted user IDs from user_deletions:', Array.from(deletedUserIds));
            } catch (error) {
                console.warn('Could not check user_deletions collection:', error);
            }

            // Enhanced filtering with multiple criteria
            const seniorCitizens = users.filter(user => {
                const role = (user.role || '').toLowerCase().trim();
                const age = Number(user.age || 0);
                
                // Exclude deleted users (check both isDeleted flag and user_deletions collection)
                if (user.isDeleted || deletedUserIds.has(user.id)) {
                    console.log('Excluding deleted user:', user.firstName, user.lastName, 'ID:', user.id);
                    return false;
                }
                
                // Primary criteria: role-based filtering
                const hasSeniorRole = role === 'senior_citizen' || role === 'senior' || role === 'citizen';
                
                // Secondary criteria: age-based filtering for users without proper role
                const isSeniorAge = age >= 60;
                
                // Include users with senior role OR users with senior age (as fallback)
                return hasSeniorRole || (isSeniorAge && !role.includes('admin'));
            });
            
            console.log('Senior citizens filtered (role-based):', users.filter(user => {
                const role = (user.role || '').toLowerCase().trim();
                return role === 'senior_citizen' || role === 'senior' || role === 'citizen';
            }).length);
            
            console.log('Senior citizens filtered (age-based fallback):', users.filter(user => {
                const age = Number(user.age || 0);
                const role = (user.role || '').toLowerCase().trim();
                return age >= 60 && !role.includes('admin');
            }).length);
            
            console.log('Final senior citizens count:', seniorCitizens.length);
            
            // Map Android app data format to admin dashboard format
            this.seniors = seniorCitizens.map(senior => this.mapSeniorData(senior));
            
            // Enhanced debugging for no seniors found
            if (this.seniors.length === 0) {
                console.log('=== NO SENIORS FOUND - DETAILED DEBUG ===');
                console.log('Total users in database:', users.length);
                console.log('All available roles:', [...new Set(users.map(u => u.role))]);
                console.log('Users by role:');
                const roleCounts = {};
                users.forEach(u => {
                    const role = u.role || 'no_role';
                    roleCounts[role] = (roleCounts[role] || 0) + 1;
                });
                console.log(roleCounts);
                
                // Check if there are users without role field
                const usersWithoutRole = users.filter(u => !u.role);
                if (usersWithoutRole.length > 0) {
                    console.log('Users without role field:', usersWithoutRole.length);
                    console.log('Sample user without role:', usersWithoutRole[0]);
                }
                
                // Check if there are users with age >= 60 but wrong role
                const potentialSeniors = users.filter(u => {
                    const age = Number(u.age || 0);
                    const role = (u.role || '').toLowerCase().trim();
                    return age >= 60 && !role.includes('admin');
                });
                if (potentialSeniors.length > 0) {
                    console.log('Potential seniors with wrong role:', potentialSeniors.length);
                    console.log('Sample potential senior:', potentialSeniors[0]);
                }
                
                // Show all users for debugging
                console.log('All users in database:');
                users.forEach((user, index) => {
                    console.log(`User ${index + 1}:`, {
                        id: user.id,
                        name: `${user.firstName || ''} ${user.lastName || ''}`,
                        email: user.email,
                        role: user.role,
                        age: user.age,
                        accountVerified: user.accountVerified,
                        createdAt: user.createdAt
                    });
                });
                console.log('=== END DEBUG ===');
            }
            
            this.filterSeniors();
            this.updateStats();
        } catch (error) {
            console.error('Error loading seniors:', error);
            console.error('Error details:', {
                message: error.message,
                code: error.code,
                stack: error.stack
            });
            
            // Retry logic for temporary failures
            if (retryCount < 2 && (error.code === 'unavailable' || error.code === 'deadline-exceeded')) {
                console.log('Retrying in 2 seconds...');
                setTimeout(() => {
                    this.loadSeniors(retryCount + 1);
                }, 2000);
                return;
            }
            
            // Provide more specific error messages
            let errorMessage = 'Failed to load senior citizens data';
            if (error.code === 'permission-denied') {
                errorMessage = 'Permission denied. Please check your admin privileges.';
            } else if (error.code === 'unavailable') {
                errorMessage = 'Service temporarily unavailable. Please try again.';
            } else if (error.message) {
                errorMessage = `Failed to load senior citizens data: ${error.message}`;
            }
            
            this.showError(errorMessage);
        } finally {
            this.hideLoading();
            this.isLoading = false;
            this.dataLoaded = true;
            this.forceReload = false;
        }
    }

    // Manual refresh method
    async refreshSeniors() {
        console.log('Manual refresh triggered');
        this.forceReload = true;
        this.dataLoaded = false;
        await this.loadSeniors();
    }

    // Debug method to check database directly
    async debugDatabase() {
        try {
            console.log('=== DATABASE DEBUG ===');
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            console.log('Total users in database:', users.length);
            
            users.forEach((user, index) => {
                console.log(`User ${index + 1}:`, {
                    id: user.id,
                    name: `${user.firstName || ''} ${user.lastName || ''}`,
                    email: user.email,
                    role: user.role,
                    age: user.age,
                    birthday: user.birthday,
                    birthDate: user.birthDate,
                    accountVerified: user.accountVerified,
                    isDeleted: user.isDeleted,
                    isActive: user.isActive
                });
            });
            
            const roles = [...new Set(users.map(u => u.role))];
            console.log('All roles found:', roles);
            
            // Test different filtering criteria
            const seniorRoles = users.filter(u => {
                const role = (u.role || '').toLowerCase().trim();
                return role === 'senior_citizen' || role === 'senior' || role === 'citizen';
            });
            console.log('Users matching senior role criteria:', seniorRoles.length);
            
            const seniorAge = users.filter(u => {
                const age = Number(u.age || 0);
                return age >= 60;
            });
            console.log('Users with age >= 60:', seniorAge.length);
            
            const notDeleted = users.filter(u => !u.isDeleted);
            console.log('Users not deleted:', notDeleted.length);
            
            const activeUsers = users.filter(u => u.isActive !== false);
            console.log('Active users:', activeUsers.length);
            
            // Show what the current filtering logic would select
            const currentFilter = users.filter(user => {
                const role = (user.role || '').toLowerCase().trim();
                const age = Number(user.age || 0);
                
                // Exclude deleted users
                if (user.isDeleted) {
                    return false;
                }
                
                // Primary criteria: role-based filtering
                const hasSeniorRole = role === 'senior_citizen' || role === 'senior' || role === 'citizen';
                
                // Secondary criteria: age-based filtering for users without proper role
                const isSeniorAge = age >= 60;
                
                // Include users with senior role OR users with senior age (as fallback)
                return hasSeniorRole || (isSeniorAge && !role.includes('admin'));
            });
            console.log('Users that would be shown in admin dashboard:', currentFilter.length);
            
            console.log('=== END DEBUG ===');
        } catch (error) {
            console.error('Debug database error:', error);
        }
    }

    // Debug method to check a specific user document
    async debugSpecificUser(userId) {
        try {
            console.log('=== DEBUGGING SPECIFIC USER ===');
            console.log('User ID:', userId);
            
            const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
            if (userDoc.exists) {
                const userData = userDoc.data();
                console.log('User data:', userData);
                
                // Test filtering logic
                const role = (userData.role || '').toLowerCase().trim();
                const age = Number(userData.age || 0);
                const isDeleted = userData.isDeleted || false;
                
                console.log('Filtering analysis:', {
                    role: role,
                    age: age,
                    isDeleted: isDeleted,
                    hasSeniorRole: role === 'senior_citizen' || role === 'senior' || role === 'citizen',
                    isSeniorAge: age >= 60,
                    wouldBeIncluded: !isDeleted && (role === 'senior_citizen' || role === 'senior' || role === 'citizen' || (age >= 60 && !role.includes('admin')))
                });
            } else {
                console.log('User document not found');
            }
            
            console.log('=== END USER DEBUG ===');
        } catch (error) {
            console.error('Error debugging user:', error);
        }
    }

    // Debug method to test admin permissions
    async debugAdminPermissions() {
        try {
            console.log('=== ADMIN PERMISSIONS DEBUG ===');
            
            // Check current user
            const currentUser = firebase.auth().currentUser;
            console.log('Current user:', currentUser ? {
                uid: currentUser.uid,
                email: currentUser.email,
                emailVerified: currentUser.emailVerified
            } : 'No user');
            
            if (!currentUser) {
                console.error('❌ No authenticated user');
                return;
            }
            
            // Check admin status
            console.log('Checking admin status...');
            const adminDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(currentUser.uid).get();
            console.log('Admin document exists:', adminDoc.exists);
            
            if (adminDoc.exists) {
                const adminData = adminDoc.data();
                const userRole = (adminData.role || '').toLowerCase().trim();
                // With the current rules, any user in admin_users collection can manage seniors
                const isAuthorized = true; // Rules handle the authorization
                
                console.log('Admin data:', adminData);
                console.log('User role:', userRole);
                console.log('Authorized for senior management:', isAuthorized ? '✅ YES' : '❌ NO');
                
                if (!isAuthorized) {
                    console.error('❌ User role not authorized for senior management');
                    return;
                }
            } else {
                console.error('❌ Admin document not found');
                return;
            }
            
            // Test reading users collection
            console.log('Testing read access to users collection...');
            try {
                const usersSnapshot = await db.collection(COLLECTIONS.USERS).limit(1).get();
                console.log('✅ Successfully read users collection');
                console.log('Sample user:', usersSnapshot.docs[0]?.data());
            } catch (error) {
                console.error('❌ Failed to read users collection:', error);
            }
            
            // Test write permissions (dry run)
            console.log('Testing write access to users collection...');
            try {
                const testUserId = 'test-permission-check';
                const testData = {
                    testField: 'permission-test',
                    testTimestamp: firebase.firestore.Timestamp.now()
                };
                
                // Try to update a non-existent document (this will fail but show if we have write permissions)
                await db.collection(COLLECTIONS.USERS).doc(testUserId).update(testData);
                console.log('✅ Successfully updated users collection');
            } catch (error) {
                if (error.code === 'not-found') {
                    console.log('✅ Write permission confirmed (document not found error is expected)');
                } else if (error.code === 'permission-denied') {
                    console.error('❌ Write permission denied:', error.message);
                } else {
                    console.error('❌ Failed to test write access:', error);
                }
            }
            
            console.log('=== END ADMIN PERMISSIONS DEBUG ===');
        } catch (error) {
            console.error('Error in admin permissions debug:', error);
        }
    }

    // Fix user roles for users without proper role assignment
    async fixUserRoles() {
        try {
            console.log('=== FIXING USER ROLES ===');
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            let fixedCount = 0;
            
            for (const user of users) {
                const role = user.role || '';
                const age = Number(user.age || 0);
                
                // Fix users without role or with incorrect role
                if (!role || role === '' || role === 'null' || role === 'undefined') {
                    if (age >= 60) {
                        // Set as senior citizen if age >= 60
                        await FirebaseUtils.updateDoc(COLLECTIONS.USERS, user.id, { role: 'senior_citizen' });
                        console.log(`Fixed role for user ${user.firstName} ${user.lastName} (age: ${age})`);
                        fixedCount++;
                    } else {
                        // Set as regular user if age < 60
                        await FirebaseUtils.updateDoc(COLLECTIONS.USERS, user.id, { role: 'user' });
                        console.log(`Set role as 'user' for ${user.firstName} ${user.lastName} (age: ${age})`);
                        fixedCount++;
                    }
                }
            }
            
            console.log(`Fixed roles for ${fixedCount} users`);
            console.log('=== ROLE FIX COMPLETE ===');
            
            // Refresh the seniors list
            await this.loadSeniors();
            
        } catch (error) {
            console.error('Error fixing user roles:', error);
        }
    }

    // Create a test senior user for debugging
    async createTestSenior() {
        try {
            console.log('=== CREATING TEST SENIOR ===');
            const testSenior = {
                firstName: 'Test',
                lastName: 'Senior',
                email: 'test.senior@example.com',
                phoneNumber: '+639123456789',
                age: 65,
                gender: 'Male',
                city: 'Davao City',
                province: 'Davao Del Sur',
                barangay: 'Poblacion',
                houseNumberAndStreet: '123 Test Street',
                zipCode: '8000',
                maritalStatus: 'Married',
                role: 'senior_citizen',
                accountVerified: true,
                isActive: true,
                createdAt: FirebaseUtils.getTimestamp(),
                updatedAt: FirebaseUtils.getTimestamp()
            };
            
            const docId = await FirebaseUtils.addDoc(COLLECTIONS.USERS, testSenior);
            console.log(`Test senior created with ID: ${docId}`);
            console.log('=== TEST SENIOR CREATED ===');
            
            // Refresh the seniors list
            await this.loadSeniors();
            
        } catch (error) {
            console.error('Error creating test senior:', error);
        }
    }

    filterSeniors() {
        this.filteredSeniors = this.seniors.filter(senior => {
            const matchesSearch = !this.currentSearch || 
                senior.firstName.toLowerCase().includes(this.currentSearch) ||
                senior.lastName.toLowerCase().includes(this.currentSearch) ||
                senior.email.toLowerCase().includes(this.currentSearch) ||
                senior.phoneNumber.includes(this.currentSearch);

            const matchesFilter = !this.currentFilter || 
                (this.currentFilter === 'active' && senior.isActive) ||
                (this.currentFilter === 'inactive' && !senior.isActive) ||
                (this.currentFilter === 'verified' && senior.accountVerified) ||
                (this.currentFilter === 'unverified' && !senior.accountVerified) ||
                (this.currentFilter === 'new' && this.isNewThisMonth(senior.createdAt));

            return matchesSearch && matchesFilter;
        });

        this.renderSeniorsTable();
    }

    isNewThisMonth(timestamp) {
        if (!timestamp) return false;
        const now = new Date();
        const createdDate = timestamp.toDate();
        return createdDate.getMonth() === now.getMonth() && 
               createdDate.getFullYear() === now.getFullYear();
    }

    renderSeniorsTable() {
        const tbody = document.getElementById('seniorsTableBody');
        tbody.innerHTML = '';

        if (this.filteredSeniors.length === 0) {
            let message = 'No senior citizens found';
            let icon = 'fas fa-users';
            let subMessage = '';
            
            if (this.seniors.length === 0) {
                message = 'No senior citizens in database';
                icon = 'fas fa-database';
                subMessage = '';
            } else if (this.currentSearch || this.currentFilter) {
                message = 'No senior citizens match your search/filter';
                icon = 'fas fa-search';
                subMessage = 'Try adjusting your search terms or filter options';
            }
            
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">
                        <div class="empty-state">
                            <i class="${icon}"></i>
                            <p>${message}</p>
                            ${subMessage ? `<small class="text-muted">${subMessage}</small>` : ''}
                            
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.filteredSeniors.forEach(senior => {
            const row = this.createSeniorRow(senior);
            tbody.appendChild(row);
        });
    }

    createSeniorRow(senior) {
        const row = document.createElement('tr');
        row.className = 'senior-row';
        
        // Debug logging for profile image
        console.log('Creating row for senior:', senior.firstName, 'Profile image URL:', senior.profileImageUrl);
        
        row.innerHTML = `
            <td>
                <div class="user-info">
                    <div class="user-avatar">
                        ${senior.profileImageUrl ? 
                            `<img src="${this.getOptimizedCloudinaryUrl(senior.profileImageUrl, 60, 60)}" alt="${senior.firstName}" onerror="console.log('Image failed to load:', this.src); this.style.display='none'; this.nextElementSibling.style.display='flex';">
                             <div class="avatar-placeholder" style="display: none;"><i class="fas fa-user"></i></div>` :
                            `<div class="avatar-placeholder"><i class="fas fa-user"></i></div>`
                        }
                    </div>
                    <div class="user-details">
                        <span class="user-name">${senior.firstName} ${senior.lastName}</span>
                        <span class="user-email">${senior.email}</span>
                    </div>
                </div>
            </td>
            <td>
                <span class="age-badge">${senior.age || 'N/A'}</span>
            </td>
            <td>
                <div class="contact-info">
                    <span class="phone">${senior.phoneNumber || 'N/A'}</span>
                </div>
            </td>
            <td>
                <div class="address-info">
                    <span class="address-line">${senior.houseNumberAndStreet || 'N/A'}</span>
                    <span class="address-details">${senior.barangay || ''}${senior.barangay && senior.city ? ', ' : ''}${senior.city || ''}</span>
                </div>
            </td>
            <td>
                <span class="status-badge ${senior.isActive ? 'active' : 'inactive'}">
                    ${senior.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td>
                <span class="status-badge ${senior.accountVerified ? 'active' : 'pending'}" title="Account Verification Status">
                    ${senior.accountVerified ? 'Verified' : 'Unverified'}
                </span>
            </td>
            <td>
                <div class="action-buttons">
                    <div class="action-group">
                        <button class="action-btn view-btn" onclick="seniorsManager.viewSenior('${senior.id}')" title="View Details">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="action-btn edit-btn" onclick="seniorsManager.editSenior('${senior.id}')" title="Edit Information">
                            <i class="fas fa-edit"></i>
                        </button>
                    </div>
                    <div class="action-group">
                        ${senior.accountVerified ? `
                        <button class="action-btn verified-btn" title="Account Verified" disabled>
                            <i class="fas fa-check-circle"></i>
                        </button>` : `
                        <button class="action-btn verify-btn" title="Verify Account" onclick="seniorsManager.verifyAccount('${senior.id}')">
                            <i class="fas fa-user-check"></i>
                        </button>`}
                    </div>
                    <div class="action-group">
                        <button class="action-btn delete-btn" onclick="seniorsManager.deleteSenior('${senior.id}', '${senior.firstName} ${senior.lastName}')" title="Delete Account">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </td>
        `;
        return row;
    }


    // Test function to check delete permissions
    async testDeletePermission(seniorId) {
        try {
            console.log('Testing delete permission for user:', seniorId);
            const currentUser = firebase.auth().currentUser;
            
            if (!currentUser) {
                throw new Error('No authenticated user');
            }
            
            // Check admin role
            const adminDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(currentUser.uid).get();
            if (!adminDoc.exists) {
                throw new Error('No admin document found');
            }
            
            const adminData = adminDoc.data();
            const role = adminData.role;
            console.log('Current user role:', role);
            
            // Test if we can read the user document
            const userDoc = await db.collection(COLLECTIONS.USERS).doc(seniorId).get();
            if (!userDoc.exists) {
                throw new Error('User document does not exist');
            }
            
            console.log('User document exists and can be read');
            console.log('Role check:', role, 'in', ['facilitator', 'super_admin'], '=', ['facilitator', 'super_admin'].includes(role));
            
            return {
                canDelete: ['admin'].includes(role),
                role: role,
                userExists: true
            };
            
        } catch (error) {
            console.error('Permission test failed:', error);
            return {
                canDelete: false,
                error: error.message
            };
        }
    }

    async deleteSenior(seniorId, displayName = 'this user') {
        console.log('Delete senior called:', { seniorId, displayName });
        
        const confirmed = confirm(`Permanently delete ${displayName}? This will remove their record and related data. This action cannot be undone.`);
        if (!confirmed) {
            console.log('Delete cancelled by user');
            return;
        }

        try {
            this.showLoading('Deleting senior user and all related data...');
            console.log('Starting deletion process for user:', seniorId);
            
            // Test delete permissions first
            const permissionTest = await this.testDeletePermission(seniorId);
            console.log('Permission test result:', permissionTest);
            
            if (!permissionTest.canDelete) {
                throw new Error(`Insufficient permissions. Role: ${permissionTest.role || 'unknown'}. Required: admin`);
            }

            // Get user data before deletion
            const userDoc = await db.collection(COLLECTIONS.USERS).doc(seniorId).get();
            const userData = userDoc.exists ? userDoc.data() : null;
            const userEmail = userData ? userData.email : null;
            
            console.log('User data before deletion:', { userData, userEmail });

            // Use the complete deletion utility
            console.log('Calling FirebaseUtils.deleteUserCompletely...');
            await FirebaseUtils.deleteUserCompletely(seniorId, userEmail);
            console.log('FirebaseUtils.deleteUserCompletely completed');

            this.hideLoading();
            this.showSuccess('Senior user and all related data deleted successfully. Email is now available for reuse.');
            
            // Remove from local array immediately for better UX
            console.log('Removing from local array. Current seniors count:', this.seniors.length);
            this.seniors = this.seniors.filter(senior => senior.id !== seniorId);
            console.log('After filtering. New seniors count:', this.seniors.length);
            
            this.filterSeniors();
            this.updateStats();
            
            // Also refresh from database to ensure consistency
            console.log('Scheduling database refresh...');
            setTimeout(() => {
                console.log('Executing database refresh...');
                this.loadSeniors();
            }, 1000);
        } catch (e) {
            this.hideLoading();
            console.error('Delete senior failed', e);
            console.error('Error details:', {
                message: e.message,
                code: e.code,
                stack: e.stack
            });
            this.showError(`Failed to delete senior user: ${e.message}`);
        }
    }

    async deleteCollectionDocs(collection, field, value) {
        try {
            const q = await db.collection(collection).where(field, '==', value).get();
            const deletes = [];
            q.forEach(doc => deletes.push(db.collection(collection).doc(doc.id).delete()));
            await Promise.all(deletes);
        } catch (e) {
            console.warn(`Failed deleting related docs in ${collection}`, e);
        }
    }

    async checkEmailExists(email) {
        try {
            // Check in users collection
            const usersQuery = await db.collection(COLLECTIONS.USERS).where('email', '==', email).get();
            if (!usersQuery.empty) {
                return true;
            }

            // Check in admin_users collection
            const adminQuery = await db.collection(COLLECTIONS.ADMIN_USERS).where('email', '==', email).get();
            if (!adminQuery.empty) {
                return true;
            }

            // Check in Firebase Auth
            try {
                const signInMethods = await auth.fetchSignInMethodsForEmail(email);
                if (signInMethods && signInMethods.length > 0) {
                    return true;
                }
            } catch (authError) {
                console.warn('Error checking Firebase Auth for email:', authError);
                // Continue with other checks even if Firebase Auth check fails
            }

            return false;
        } catch (error) {
            console.error('Error checking email existence:', error);
            // If there's an error checking, assume email doesn't exist to avoid blocking valid registrations
            return false;
        }
    }

    // Debug function to test email availability
    async testEmailAvailability(email) {
        try {
            const exists = await this.checkEmailExists(email);
            console.log(`Email ${email} is ${exists ? 'NOT available' : 'available'} for use`);
            return !exists;
        } catch (error) {
            console.error('Error testing email availability:', error);
            return false;
        }
    }

    async viewSenior(seniorId) {
        try {
            const senior = await FirebaseUtils.getDoc(COLLECTIONS.USERS, seniorId);
            if (senior) {
                console.log('Viewing senior data:', senior);
                // Map the data to ensure proper formatting
                const mappedSenior = this.mapSeniorData(senior);
                console.log('Mapped senior data:', mappedSenior);
                this.showSeniorModal(mappedSenior, 'view');
            }
        } catch (error) {
            console.error('Error viewing senior:', error);
            this.showError('Failed to load senior information');
        }
    }

    async editSenior(seniorId) {
        try {
            const senior = await FirebaseUtils.getDoc(COLLECTIONS.USERS, seniorId);
            if (senior) {
                // Map the data to ensure proper formatting
                const mappedSenior = this.mapSeniorData(senior);
                this.showSeniorModal(mappedSenior, 'edit');
            }
        } catch (error) {
            console.error('Error editing senior:', error);
            this.showError('Failed to load senior information');
        }
    }

    async toggleSeniorStatus(seniorId, currentStatus) {
        const newStatus = !currentStatus;
        const action = newStatus ? 'activate' : 'deactivate';
        
        if (!confirm(`Are you sure you want to ${action} this senior citizen?`)) {
            return;
        }

        try {
            await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, {
                isActive: newStatus
            });
            
            this.showSuccess(`Senior citizen ${action}d successfully`);
        } catch (error) {
            console.error('Error toggling senior status:', error);
            this.showError('Failed to update senior status');
        }
    }

    showSeniorModal(senior, mode) {
        const modalTitle = mode === 'view' ? 'View Senior Citizen' : 'Edit Senior Citizen';
        const modalBody = this.createSeniorModalContent(senior, mode);
        
        this.openModal(modalTitle, modalBody);
        
        // Setup event listeners for edit mode
        if (mode === 'edit') {
            this.setupEditSeniorEventListeners();
        }
    }

    setupEditSeniorEventListeners() {
        // Birthday to age calculation
        const birthdayInput = document.getElementById('editBirthday');
        const ageInput = document.getElementById('editAge');
        
        if (birthdayInput && ageInput) {
            birthdayInput.addEventListener('change', () => {
                const birthday = new Date(birthdayInput.value);
                const today = new Date();
                let age = today.getFullYear() - birthday.getFullYear();
                const monthDiff = today.getMonth() - birthday.getMonth();
                
                if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthday.getDate())) {
                    age--;
                }
                
                ageInput.value = age >= 0 ? age : '';
            });
        }

        // Membership checkboxes
        const membershipCheckboxes = [
            { checkbox: 'editSSSMember', group: 'editSSSNumberGroup' },
            { checkbox: 'editGSISMember', group: 'editGSISNumberGroup' },
            { checkbox: 'editOSCAMember', group: 'editOSCANumberGroup' },
            { checkbox: 'editPhilHealthMember', group: 'editPhilHealthNumberGroup' }
        ];

        membershipCheckboxes.forEach(({ checkbox, group }) => {
            const checkboxElement = document.getElementById(checkbox);
            const groupElement = document.getElementById(group);
            
            if (checkboxElement && groupElement) {
                checkboxElement.addEventListener('change', () => {
                    groupElement.style.display = checkboxElement.checked ? 'block' : 'none';
                });
            }
        });
    }

    createSeniorModalContent(senior, mode) {
        const isViewMode = mode === 'view';
        
        if (isViewMode) {
            return this.createViewSeniorModal(senior);
        } else {
            return this.createEditSeniorModal(senior);
        }
    }

    createViewSeniorModal(senior) {
        return `
            <div class="senior-profile-container">
                <!-- Senior Header -->
                <div class="senior-profile-header">
                    <div class="senior-avatar-large">
                        ${senior.profileImageUrl ? 
                            `<img src="${this.getOptimizedCloudinaryUrl(senior.profileImageUrl, 200, 200)}" alt="${senior.firstName}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                             <div class="avatar-placeholder-large" style="display: none;"><i class="fas fa-user"></i></div>` :
                            `<div class="avatar-placeholder-large"><i class="fas fa-user"></i></div>`
                        }
                    </div>
                    <div class="senior-basic-info">
                        <h2 class="senior-name">${senior.firstName} ${senior.lastName}</h2>
                        <p class="senior-email">${senior.email || 'No email provided'}</p>
                        <div class="senior-status-badges">
                            <span class="status-badge ${senior.isActive ? 'active' : 'inactive'}">
                                <i class="fas fa-circle"></i>
                                ${senior.isActive ? 'Active' : 'Inactive'}
                            </span>
                            <span class="status-badge ${senior.accountVerified ? 'verified' : 'unverified'}">
                                <i class="fas fa-${senior.accountVerified ? 'check-circle' : 'clock'}"></i>
                                ${senior.accountVerified ? 'Verified' : 'Unverified'}
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Senior Profile Details -->
                <div class="senior-profile-details">
                    <!-- Basic Information Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-user"></i> Basic Information
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>First Name</label>
                                <div class="profile-value">${senior.firstName || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Last Name</label>
                                <div class="profile-value">${senior.lastName || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Birthday</label>
                                <div class="profile-value">${senior.birthday || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Age</label>
                                <div class="profile-value">${senior.age || 'N/A'} years old</div>
                            </div>
                            <div class="profile-item">
                                <label>Gender</label>
                                <div class="profile-value">${senior.gender || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Marital Status</label>
                                <div class="profile-value">${senior.maritalStatus || 'N/A'}</div>
                            </div>
                        </div>
                    </div>

                    <!-- Contact Information Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-phone"></i> Contact Information
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>Phone Number</label>
                                <div class="profile-value">${senior.phoneNumber || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Email Address</label>
                                <div class="profile-value">${senior.email || 'N/A'}</div>
                            </div>
                        </div>
                    </div>

                    <!-- Address Information Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-map-marker-alt"></i> Address Information
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item full-width">
                                <label>House Number and Street</label>
                                <div class="profile-value">${senior.houseNumberAndStreet || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Barangay</label>
                                <div class="profile-value">${senior.barangay || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>City</label>
                                <div class="profile-value">${senior.city || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Province</label>
                                <div class="profile-value">${senior.province || 'N/A'}</div>
                            </div>
                        </div>
                    </div>

                    <!-- Membership Information Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-id-card"></i> Membership Information
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>SSS Member</label>
                                <div class="profile-value">
                                    <span class="membership-badge ${senior.sssMember ? 'active' : 'inactive'}">
                                        <i class="fas fa-${senior.sssMember ? 'check-circle' : 'times-circle'}"></i>
                                        ${senior.sssMember ? 'Yes' : 'No'}
                                    </span>
                                </div>
                            </div>
                            ${senior.sssMember && senior.sssNumber ? `
                            <div class="profile-item">
                                <label>SSS Number</label>
                                <div class="profile-value">${senior.sssNumber}</div>
                            </div>` : ''}
                            <div class="profile-item">
                                <label>GSIS Member</label>
                                <div class="profile-value">
                                    <span class="membership-badge ${senior.gsisMember ? 'active' : 'inactive'}">
                                        <i class="fas fa-${senior.gsisMember ? 'check-circle' : 'times-circle'}"></i>
                                        ${senior.gsisMember ? 'Yes' : 'No'}
                                    </span>
                                </div>
                            </div>
                            ${senior.gsisMember && senior.gsisNumber ? `
                            <div class="profile-item">
                                <label>GSIS Number</label>
                                <div class="profile-value">${senior.gsisNumber}</div>
                            </div>` : ''}
                            <div class="profile-item">
                                <label>OSCA Member</label>
                                <div class="profile-value">
                                    <span class="membership-badge ${senior.oscaMember ? 'active' : 'inactive'}">
                                        <i class="fas fa-${senior.oscaMember ? 'check-circle' : 'times-circle'}"></i>
                                        ${senior.oscaMember ? 'Yes' : 'No'}
                                    </span>
                                </div>
                            </div>
                            ${senior.oscaMember && senior.oscaNumber ? `
                            <div class="profile-item">
                                <label>OSCA Number</label>
                                <div class="profile-value">${senior.oscaNumber}</div>
                            </div>` : ''}
                            <div class="profile-item">
                                <label>PhilHealth Member</label>
                                <div class="profile-value">
                                    <span class="membership-badge ${senior.philHealthMember ? 'active' : 'inactive'}">
                                        <i class="fas fa-${senior.philHealthMember ? 'check-circle' : 'times-circle'}"></i>
                                        ${senior.philHealthMember ? 'Yes' : 'No'}
                                    </span>
                                </div>
                            </div>
                            ${senior.philHealthMember && senior.philHealthNumber ? `
                            <div class="profile-item">
                                <label>PhilHealth Number</label>
                                <div class="profile-value">${senior.philHealthNumber}</div>
                            </div>` : ''}
                        </div>
                    </div>

                    <!-- Emergency Contact Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-exclamation-triangle"></i> Emergency Contact
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>Emergency Contact Name</label>
                                <div class="profile-value">${senior.emergencyContactName || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Emergency Contact Phone</label>
                                <div class="profile-value">${senior.emergencyContactPhone || 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Relationship</label>
                                <div class="profile-value">${senior.relationship || 'N/A'}</div>
                            </div>
                        </div>
                    </div>

                    <!-- Account Information Section -->
                    <div class="profile-section">
                        <h4 class="profile-section-title">
                            <i class="fas fa-user-shield"></i> Account Information
                        </h4>
                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>Account Status</label>
                                <div class="profile-value">
                                    <span class="status-badge ${senior.isActive ? 'active' : 'inactive'}">
                                        <i class="fas fa-${senior.isActive ? 'check-circle' : 'times-circle'}"></i>
                                        ${senior.isActive ? 'Active' : 'Inactive'}
                                    </span>
                                </div>
                            </div>
                            <div class="profile-item">
                                <label>Verification Status</label>
                                <div class="profile-value">
                                    <span class="status-badge ${senior.accountVerified ? 'verified' : 'unverified'}">
                                        <i class="fas fa-${senior.accountVerified ? 'check-circle' : 'clock'}"></i>
                                        ${senior.accountVerified ? 'Verified' : 'Unverified'}
                                    </span>
                                </div>
                            </div>
                            <div class="profile-item">
                                <label>Member Since</label>
                                <div class="profile-value">${senior.createdAt ? this.formatDate(senior.createdAt.toDate()) : 'N/A'}</div>
                            </div>
                            <div class="profile-item">
                                <label>Last Updated</label>
                                <div class="profile-value">${senior.updatedAt ? this.formatDate(senior.updatedAt.toDate()) : 'N/A'}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-success" onclick="generateSeniorReport('${senior.id}')">
                    <i class="fas fa-file-pdf"></i> Generate Report
                </button>
                <button class="btn btn-secondary" onclick="closeModal()">
                    <i class="fas fa-times"></i> Close
                </button>
            </div>
        `;
    }

    createEditSeniorModal(senior) {
        return `
            <div class="senior-edit-container">
                <!-- Senior Header -->
                <div class="senior-edit-header">
                    <div class="senior-avatar-section">
                        <div class="senior-avatar-large">
                            ${senior.profileImageUrl ? 
                                `<img src="${senior.profileImageUrl}" alt="${senior.firstName}">` :
                                `<div class="avatar-placeholder-large"><i class="fas fa-user"></i></div>`
                            }
                        </div>
                        <button type="button" class="btn btn-outline btn-sm" onclick="seniorsManager.changeProfileImage('${senior.id}')">
                            <i class="fas fa-camera"></i> Change Photo
                        </button>
                    </div>
                    <div class="senior-basic-info">
                        <h2 class="senior-name">${senior.firstName} ${senior.lastName}</h2>
                        <p class="senior-email">${senior.email || 'No email provided'}</p>
                        <div class="senior-status-badges">
                            <span class="status-badge ${senior.isActive ? 'active' : 'inactive'}">
                                <i class="fas fa-circle"></i>
                                ${senior.isActive ? 'Active' : 'Inactive'}
                            </span>
                            <span class="status-badge ${senior.accountVerified ? 'verified' : 'unverified'}">
                                <i class="fas fa-${senior.accountVerified ? 'check-circle' : 'clock'}"></i>
                                ${senior.accountVerified ? 'Verified' : 'Unverified'}
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Edit Form -->
                <form class="senior-edit-form" id="editSeniorForm">
                    <!-- Basic Information Section -->
                    <div class="form-section">
                        <h4 class="section-title">
                            <i class="fas fa-user"></i> Basic Information
                        </h4>
                        <div class="form-grid">
                            <div class="form-group">
                                <label>First Name *</label>
                                <input type="text" id="editFirstName" value="${senior.firstName || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Last Name *</label>
                                <input type="text" id="editLastName" value="${senior.lastName || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Birthday *</label>
                                <input type="date" id="editBirthday" value="${this.formatDateForInput(senior.originalBirthday || senior.birthday || senior.birthDate)}" required />
                            </div>
                            <div class="form-group">
                                <label>Age (Auto-calculated)</label>
                                <input type="number" id="editAge" value="${senior.age || ''}" readonly />
                            </div>
                            <div class="form-group">
                                <label>Gender *</label>
                                <select id="editGender" required>
                                    <option value="">Select Gender</option>
                                    <option value="Male" ${senior.gender === 'Male' ? 'selected' : ''}>Male</option>
                                    <option value="Female" ${senior.gender === 'Female' ? 'selected' : ''}>Female</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>Marital Status</label>
                                <select id="editMaritalStatus">
                                    <option value="">Select Status</option>
                                    <option value="Single" ${senior.maritalStatus === 'Single' ? 'selected' : ''}>Single</option>
                                    <option value="Married" ${senior.maritalStatus === 'Married' ? 'selected' : ''}>Married</option>
                                    <option value="Widowed" ${senior.maritalStatus === 'Widowed' ? 'selected' : ''}>Widowed</option>
                                    <option value="Divorced" ${senior.maritalStatus === 'Divorced' ? 'selected' : ''}>Divorced</option>
                                    <option value="Separated" ${senior.maritalStatus === 'Separated' ? 'selected' : ''}>Separated</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>Phone Number *</label>
                                <input type="tel" id="editPhone" value="${senior.phoneNumber || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Email Address *</label>
                                <input type="email" id="editEmail" value="${senior.email || ''}" required />
                            </div>
                        </div>
                    </div>

                    <!-- Membership Information Section -->
                    <div class="form-section">
                        <h4 class="section-title">
                            <i class="fas fa-id-card"></i> Membership Information
                        </h4>
                        <div class="form-grid">
                            <div class="form-group checkbox-group">
                                <label class="checkbox-label">
                                    <input type="checkbox" id="editSSSMember" ${senior.sssMember ? 'checked' : ''} />
                                    <span class="checkmark"></span>
                                    SSS Member
                                </label>
                            </div>
                            <div class="form-group" id="editSSSNumberGroup" style="display: ${senior.sssMember ? 'block' : 'none'};">
                                <label>SSS Number</label>
                                <input type="text" id="editSSSNumber" value="${senior.sssNumber || ''}" />
                            </div>
                            <div class="form-group checkbox-group">
                                <label class="checkbox-label">
                                    <input type="checkbox" id="editGSISMember" ${senior.gsisMember ? 'checked' : ''} />
                                    <span class="checkmark"></span>
                                    GSIS Member
                                </label>
                            </div>
                            <div class="form-group" id="editGSISNumberGroup" style="display: ${senior.gsisMember ? 'block' : 'none'};">
                                <label>GSIS Number</label>
                                <input type="text" id="editGSISNumber" value="${senior.gsisNumber || ''}" />
                            </div>
                            <div class="form-group checkbox-group">
                                <label class="checkbox-label">
                                    <input type="checkbox" id="editOSCAMember" ${senior.oscaMember ? 'checked' : ''} />
                                    <span class="checkmark"></span>
                                    OSCA Member
                                </label>
                            </div>
                            <div class="form-group" id="editOSCANumberGroup" style="display: ${senior.oscaMember ? 'block' : 'none'};">
                                <label>OSCA Number</label>
                                <input type="text" id="editOSCANumber" value="${senior.oscaNumber || ''}" />
                            </div>
                            <div class="form-group checkbox-group">
                                <label class="checkbox-label">
                                    <input type="checkbox" id="editPhilHealthMember" ${senior.philHealthMember ? 'checked' : ''} />
                                    <span class="checkmark"></span>
                                    PhilHealth Member
                                </label>
                            </div>
                            <div class="form-group" id="editPhilHealthNumberGroup" style="display: ${senior.philHealthMember ? 'block' : 'none'};">
                                <label>PhilHealth Number</label>
                                <input type="text" id="editPhilHealthNumber" value="${senior.philHealthNumber || ''}" />
                            </div>
                        </div>
                    </div>

                    <!-- Address Information Section -->
                    <div class="form-section">
                        <h4 class="section-title">
                            <i class="fas fa-map-marker-alt"></i> Address Information
                        </h4>
                        <div class="form-grid">
                            <div class="form-group full-width">
                                <label>House Number and Street *</label>
                                <input type="text" id="editStreet" value="${senior.houseNumberAndStreet || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Barangay *</label>
                                <select id="editBarangay" required>
                                    <option value="">Select Barangay</option>
                                    <option value="1-A Poblacion" ${senior.barangay === '1-A Poblacion' ? 'selected' : ''}>1-A Poblacion</option>
                                    <option value="2-A Poblacion" ${senior.barangay === '2-A Poblacion' ? 'selected' : ''}>2-A Poblacion</option>
                                    <option value="3-A Poblacion" ${senior.barangay === '3-A Poblacion' ? 'selected' : ''}>3-A Poblacion</option>
                                    <option value="4-A Poblacion" ${senior.barangay === '4-A Poblacion' ? 'selected' : ''}>4-A Poblacion</option>
                                    <option value="5-A Poblacion" ${senior.barangay === '5-A Poblacion' ? 'selected' : ''}>5-A Poblacion</option>
                                    <option value="6-A Poblacion" ${senior.barangay === '6-A Poblacion' ? 'selected' : ''}>6-A Poblacion</option>
                                    <option value="7-A Poblacion" ${senior.barangay === '7-A Poblacion' ? 'selected' : ''}>7-A Poblacion</option>
                                    <option value="8-A Poblacion" ${senior.barangay === '8-A Poblacion' ? 'selected' : ''}>8-A Poblacion</option>
                                    <option value="9-A Poblacion" ${senior.barangay === '9-A Poblacion' ? 'selected' : ''}>9-A Poblacion</option>
                                    <option value="10-A Poblacion" ${senior.barangay === '10-A Poblacion' ? 'selected' : ''}>10-A Poblacion</option>
                                    <option value="11-B Poblacion" ${senior.barangay === '11-B Poblacion' ? 'selected' : ''}>11-B Poblacion</option>
                                    <option value="12-B Poblacion" ${senior.barangay === '12-B Poblacion' ? 'selected' : ''}>12-B Poblacion</option>
                                    <option value="13-B Poblacion" ${senior.barangay === '13-B Poblacion' ? 'selected' : ''}>13-B Poblacion</option>
                                    <option value="14-B Poblacion" ${senior.barangay === '14-B Poblacion' ? 'selected' : ''}>14-B Poblacion</option>
                                    <option value="15-B Poblacion" ${senior.barangay === '15-B Poblacion' ? 'selected' : ''}>15-B Poblacion</option>
                                    <option value="16-B Poblacion" ${senior.barangay === '16-B Poblacion' ? 'selected' : ''}>16-B Poblacion</option>
                                    <option value="17-B Poblacion" ${senior.barangay === '17-B Poblacion' ? 'selected' : ''}>17-B Poblacion</option>
                                    <option value="18-B Poblacion" ${senior.barangay === '18-B Poblacion' ? 'selected' : ''}>18-B Poblacion</option>
                                    <option value="19-B Poblacion" ${senior.barangay === '19-B Poblacion' ? 'selected' : ''}>19-B Poblacion</option>
                                    <option value="20-B Poblacion" ${senior.barangay === '20-B Poblacion' ? 'selected' : ''}>20-B Poblacion</option>
                                    <option value="21-C Poblacion" ${senior.barangay === '21-C Poblacion' ? 'selected' : ''}>21-C Poblacion</option>
                                    <option value="22-C Poblacion" ${senior.barangay === '22-C Poblacion' ? 'selected' : ''}>22-C Poblacion</option>
                                    <option value="23-C Poblacion" ${senior.barangay === '23-C Poblacion' ? 'selected' : ''}>23-C Poblacion</option>
                                    <option value="24-C Poblacion" ${senior.barangay === '24-C Poblacion' ? 'selected' : ''}>24-C Poblacion</option>
                                    <option value="25-C Poblacion" ${senior.barangay === '25-C Poblacion' ? 'selected' : ''}>25-C Poblacion</option>
                                    <option value="26-C Poblacion" ${senior.barangay === '26-C Poblacion' ? 'selected' : ''}>26-C Poblacion</option>
                                    <option value="27-C Poblacion" ${senior.barangay === '27-C Poblacion' ? 'selected' : ''}>27-C Poblacion</option>
                                    <option value="28-C Poblacion" ${senior.barangay === '28-C Poblacion' ? 'selected' : ''}>28-C Poblacion</option>
                                    <option value="29-C Poblacion" ${senior.barangay === '29-C Poblacion' ? 'selected' : ''}>29-C Poblacion</option>
                                    <option value="30-C Poblacion" ${senior.barangay === '30-C Poblacion' ? 'selected' : ''}>30-C Poblacion</option>
                                    <option value="31-D Poblacion" ${senior.barangay === '31-D Poblacion' ? 'selected' : ''}>31-D Poblacion</option>
                                    <option value="32-D Poblacion" ${senior.barangay === '32-D Poblacion' ? 'selected' : ''}>32-D Poblacion</option>
                                    <option value="33-D Poblacion" ${senior.barangay === '33-D Poblacion' ? 'selected' : ''}>33-D Poblacion</option>
                                    <option value="34-D Poblacion" ${senior.barangay === '34-D Poblacion' ? 'selected' : ''}>34-D Poblacion</option>
                                    <option value="35-D Poblacion" ${senior.barangay === '35-D Poblacion' ? 'selected' : ''}>35-D Poblacion</option>
                                    <option value="36-D Poblacion" ${senior.barangay === '36-D Poblacion' ? 'selected' : ''}>36-D Poblacion</option>
                                    <option value="37-D Poblacion" ${senior.barangay === '37-D Poblacion' ? 'selected' : ''}>37-D Poblacion</option>
                                    <option value="38-D Poblacion" ${senior.barangay === '38-D Poblacion' ? 'selected' : ''}>38-D Poblacion</option>
                                    <option value="39-D Poblacion" ${senior.barangay === '39-D Poblacion' ? 'selected' : ''}>39-D Poblacion</option>
                                    <option value="40-D Poblacion" ${senior.barangay === '40-D Poblacion' ? 'selected' : ''}>40-D Poblacion</option>
                                    <option value="Acacia" ${senior.barangay === 'Acacia' ? 'selected' : ''}>Acacia</option>
                                    <option value="Agdao" ${senior.barangay === 'Agdao' ? 'selected' : ''}>Agdao</option>
                                    <option value="Alfonso Angliongto Sr." ${senior.barangay === 'Alfonso Angliongto Sr.' ? 'selected' : ''}>Alfonso Angliongto Sr.</option>
                                    <option value="Angalan" ${senior.barangay === 'Angalan' ? 'selected' : ''}>Angalan</option>
                                    <option value="Atan-Awe" ${senior.barangay === 'Atan-Awe' ? 'selected' : ''}>Atan-Awe</option>
                                    <option value="Bagong Silang" ${senior.barangay === 'Bagong Silang' ? 'selected' : ''}>Bagong Silang</option>
                                    <option value="Bago Aplaya" ${senior.barangay === 'Bago Aplaya' ? 'selected' : ''}>Bago Aplaya</option>
                                    <option value="Bago Gallera" ${senior.barangay === 'Bago Gallera' ? 'selected' : ''}>Bago Gallera</option>
                                    <option value="Bago Oshiro" ${senior.barangay === 'Bago Oshiro' ? 'selected' : ''}>Bago Oshiro</option>
                                    <option value="Bajada" ${senior.barangay === 'Bajada' ? 'selected' : ''}>Bajada</option>
                                    <option value="Balusong" ${senior.barangay === 'Balusong' ? 'selected' : ''}>Balusong</option>
                                    <option value="Bangkas Heights" ${senior.barangay === 'Bangkas Heights' ? 'selected' : ''}>Bangkas Heights</option>
                                    <option value="Barangay 1-A" ${senior.barangay === 'Barangay 1-A' ? 'selected' : ''}>Barangay 1-A</option>
                                    <option value="Barangay 2-A" ${senior.barangay === 'Barangay 2-A' ? 'selected' : ''}>Barangay 2-A</option>
                                    <option value="Barangay 3-A" ${senior.barangay === 'Barangay 3-A' ? 'selected' : ''}>Barangay 3-A</option>
                                    <option value="Barangay 4-A" ${senior.barangay === 'Barangay 4-A' ? 'selected' : ''}>Barangay 4-A</option>
                                    <option value="Barangay 5-A" ${senior.barangay === 'Barangay 5-A' ? 'selected' : ''}>Barangay 5-A</option>
                                    <option value="Barangay 6-A" ${senior.barangay === 'Barangay 6-A' ? 'selected' : ''}>Barangay 6-A</option>
                                    <option value="Barangay 7-A" ${senior.barangay === 'Barangay 7-A' ? 'selected' : ''}>Barangay 7-A</option>
                                    <option value="Barangay 8-A" ${senior.barangay === 'Barangay 8-A' ? 'selected' : ''}>Barangay 8-A</option>
                                    <option value="Barangay 9-A" ${senior.barangay === 'Barangay 9-A' ? 'selected' : ''}>Barangay 9-A</option>
                                    <option value="Barangay 10-A" ${senior.barangay === 'Barangay 10-A' ? 'selected' : ''}>Barangay 10-A</option>
                                    <option value="Barangay 11-B" ${senior.barangay === 'Barangay 11-B' ? 'selected' : ''}>Barangay 11-B</option>
                                    <option value="Barangay 12-B" ${senior.barangay === 'Barangay 12-B' ? 'selected' : ''}>Barangay 12-B</option>
                                    <option value="Barangay 13-B" ${senior.barangay === 'Barangay 13-B' ? 'selected' : ''}>Barangay 13-B</option>
                                    <option value="Barangay 14-B" ${senior.barangay === 'Barangay 14-B' ? 'selected' : ''}>Barangay 14-B</option>
                                    <option value="Barangay 15-B" ${senior.barangay === 'Barangay 15-B' ? 'selected' : ''}>Barangay 15-B</option>
                                    <option value="Barangay 16-B" ${senior.barangay === 'Barangay 16-B' ? 'selected' : ''}>Barangay 16-B</option>
                                    <option value="Barangay 17-B" ${senior.barangay === 'Barangay 17-B' ? 'selected' : ''}>Barangay 17-B</option>
                                    <option value="Barangay 18-B" ${senior.barangay === 'Barangay 18-B' ? 'selected' : ''}>Barangay 18-B</option>
                                    <option value="Barangay 19-B" ${senior.barangay === 'Barangay 19-B' ? 'selected' : ''}>Barangay 19-B</option>
                                    <option value="Barangay 20-B" ${senior.barangay === 'Barangay 20-B' ? 'selected' : ''}>Barangay 20-B</option>
                                    <option value="Barangay 21-C" ${senior.barangay === 'Barangay 21-C' ? 'selected' : ''}>Barangay 21-C</option>
                                    <option value="Barangay 22-C" ${senior.barangay === 'Barangay 22-C' ? 'selected' : ''}>Barangay 22-C</option>
                                    <option value="Barangay 23-C" ${senior.barangay === 'Barangay 23-C' ? 'selected' : ''}>Barangay 23-C</option>
                                    <option value="Barangay 24-C" ${senior.barangay === 'Barangay 24-C' ? 'selected' : ''}>Barangay 24-C</option>
                                    <option value="Barangay 25-C" ${senior.barangay === 'Barangay 25-C' ? 'selected' : ''}>Barangay 25-C</option>
                                    <option value="Barangay 26-C" ${senior.barangay === 'Barangay 26-C' ? 'selected' : ''}>Barangay 26-C</option>
                                    <option value="Barangay 27-C" ${senior.barangay === 'Barangay 27-C' ? 'selected' : ''}>Barangay 27-C</option>
                                    <option value="Barangay 28-C" ${senior.barangay === 'Barangay 28-C' ? 'selected' : ''}>Barangay 28-C</option>
                                    <option value="Barangay 29-C" ${senior.barangay === 'Barangay 29-C' ? 'selected' : ''}>Barangay 29-C</option>
                                    <option value="Barangay 30-C" ${senior.barangay === 'Barangay 30-C' ? 'selected' : ''}>Barangay 30-C</option>
                                    <option value="Barangay 31-D" ${senior.barangay === 'Barangay 31-D' ? 'selected' : ''}>Barangay 31-D</option>
                                    <option value="Barangay 32-D" ${senior.barangay === 'Barangay 32-D' ? 'selected' : ''}>Barangay 32-D</option>
                                    <option value="Barangay 33-D" ${senior.barangay === 'Barangay 33-D' ? 'selected' : ''}>Barangay 33-D</option>
                                    <option value="Barangay 34-D" ${senior.barangay === 'Barangay 34-D' ? 'selected' : ''}>Barangay 34-D</option>
                                    <option value="Barangay 35-D" ${senior.barangay === 'Barangay 35-D' ? 'selected' : ''}>Barangay 35-D</option>
                                    <option value="Barangay 36-D" ${senior.barangay === 'Barangay 36-D' ? 'selected' : ''}>Barangay 36-D</option>
                                    <option value="Barangay 37-D" ${senior.barangay === 'Barangay 37-D' ? 'selected' : ''}>Barangay 37-D</option>
                                    <option value="Barangay 38-D" ${senior.barangay === 'Barangay 38-D' ? 'selected' : ''}>Barangay 38-D</option>
                                    <option value="Barangay 39-D" ${senior.barangay === 'Barangay 39-D' ? 'selected' : ''}>Barangay 39-D</option>
                                    <option value="Barangay 40-D" ${senior.barangay === 'Barangay 40-D' ? 'selected' : ''}>Barangay 40-D</option>
                                    <option value="Bucana" ${senior.barangay === 'Bucana' ? 'selected' : ''}>Bucana</option>
                                    <option value="Buhangin" ${senior.barangay === 'Buhangin' ? 'selected' : ''}>Buhangin</option>
                                    <option value="Bunawan" ${senior.barangay === 'Bunawan' ? 'selected' : ''}>Bunawan</option>
                                    <option value="Cabantian" ${senior.barangay === 'Cabantian' ? 'selected' : ''}>Cabantian</option>
                                    <option value="Calinan" ${senior.barangay === 'Calinan' ? 'selected' : ''}>Calinan</option>
                                    <option value="Callawa" ${senior.barangay === 'Callawa' ? 'selected' : ''}>Callawa</option>
                                    <option value="Camansi" ${senior.barangay === 'Camansi' ? 'selected' : ''}>Camansi</option>
                                    <option value="Catalunan Grande" ${senior.barangay === 'Catalunan Grande' ? 'selected' : ''}>Catalunan Grande</option>
                                    <option value="Catalunan Pequeño" ${senior.barangay === 'Catalunan Pequeño' ? 'selected' : ''}>Catalunan Pequeño</option>
                                    <option value="Central Park" ${senior.barangay === 'Central Park' ? 'selected' : ''}>Central Park</option>
                                    <option value="Centro" ${senior.barangay === 'Centro' ? 'selected' : ''}>Centro</option>
                                    <option value="Communal" ${senior.barangay === 'Communal' ? 'selected' : ''}>Communal</option>
                                    <option value="Croton" ${senior.barangay === 'Croton' ? 'selected' : ''}>Croton</option>
                                    <option value="Dacudao" ${senior.barangay === 'Dacudao' ? 'selected' : ''}>Dacudao</option>
                                    <option value="Daliao" ${senior.barangay === 'Daliao' ? 'selected' : ''}>Daliao</option>
                                    <option value="Datu Salumay" ${senior.barangay === 'Datu Salumay' ? 'selected' : ''}>Datu Salumay</option>
                                    <option value="Deca Homes" ${senior.barangay === 'Deca Homes' ? 'selected' : ''}>Deca Homes</option>
                                    <option value="Dumoy" ${senior.barangay === 'Dumoy' ? 'selected' : ''}>Dumoy</option>
                                    <option value="Ecoland" ${senior.barangay === 'Ecoland' ? 'selected' : ''}>Ecoland</option>
                                    <option value="Eden" ${senior.barangay === 'Eden' ? 'selected' : ''}>Eden</option>
                                    <option value="Fatima" ${senior.barangay === 'Fatima' ? 'selected' : ''}>Fatima</option>
                                    <option value="Gatungan" ${senior.barangay === 'Gatungan' ? 'selected' : ''}>Gatungan</option>
                                    <option value="Gov. Paciano Bangoy" ${senior.barangay === 'Gov. Paciano Bangoy' ? 'selected' : ''}>Gov. Paciano Bangoy</option>
                                    <option value="Gov. Vicente Duterte" ${senior.barangay === 'Gov. Vicente Duterte' ? 'selected' : ''}>Gov. Vicente Duterte</option>
                                    <option value="Guadalupe" ${senior.barangay === 'Guadalupe' ? 'selected' : ''}>Guadalupe</option>
                                    <option value="Gumalang" ${senior.barangay === 'Gumalang' ? 'selected' : ''}>Gumalang</option>
                                    <option value="Hizon" ${senior.barangay === 'Hizon' ? 'selected' : ''}>Hizon</option>
                                    <option value="Indangan" ${senior.barangay === 'Indangan' ? 'selected' : ''}>Indangan</option>
                                    <option value="Kabacan" ${senior.barangay === 'Kabacan' ? 'selected' : ''}>Kabacan</option>
                                    <option value="Kaligutan" ${senior.barangay === 'Kaligutan' ? 'selected' : ''}>Kaligutan</option>
                                    <option value="Lacson" ${senior.barangay === 'Lacson' ? 'selected' : ''}>Lacson</option>
                                    <option value="Lamanan" ${senior.barangay === 'Lamanan' ? 'selected' : ''}>Lamanan</option>
                                    <option value="Lampianao" ${senior.barangay === 'Lampianao' ? 'selected' : ''}>Lampianao</option>
                                    <option value="Lasang" ${senior.barangay === 'Lasang' ? 'selected' : ''}>Lasang</option>
                                    <option value="Leon Garcia Sr." ${senior.barangay === 'Leon Garcia Sr.' ? 'selected' : ''}>Leon Garcia Sr.</option>
                                    <option value="Lizada" ${senior.barangay === 'Lizada' ? 'selected' : ''}>Lizada</option>
                                    <option value="Los Amigos" ${senior.barangay === 'Los Amigos' ? 'selected' : ''}>Los Amigos</option>
                                    <option value="Lubogan" ${senior.barangay === 'Lubogan' ? 'selected' : ''}>Lubogan</option>
                                    <option value="Lungaog" ${senior.barangay === 'Lungaog' ? 'selected' : ''}>Lungaog</option>
                                    <option value="Maa" ${senior.barangay === 'Maa' ? 'selected' : ''}>Maa</option>
                                    <option value="Magtuod" ${senior.barangay === 'Magtuod' ? 'selected' : ''}>Magtuod</option>
                                    <option value="Mahayag" ${senior.barangay === 'Mahayag' ? 'selected' : ''}>Mahayag</option>
                                    <option value="Malabog" ${senior.barangay === 'Malabog' ? 'selected' : ''}>Malabog</option>
                                    <option value="Malagos" ${senior.barangay === 'Malagos' ? 'selected' : ''}>Malagos</option>
                                    <option value="Malibago" ${senior.barangay === 'Malibago' ? 'selected' : ''}>Malibago</option>
                                    <option value="Mamay" ${senior.barangay === 'Mamay' ? 'selected' : ''}>Mamay</option>
                                    <option value="Mandug" ${senior.barangay === 'Mandug' ? 'selected' : ''}>Mandug</option>
                                    <option value="Manuel Guianga" ${senior.barangay === 'Manuel Guianga' ? 'selected' : ''}>Manuel Guianga</option>
                                    <option value="Mapula" ${senior.barangay === 'Mapula' ? 'selected' : ''}>Mapula</option>
                                    <option value="Marapangi" ${senior.barangay === 'Marapangi' ? 'selected' : ''}>Marapangi</option>
                                    <option value="Marilog" ${senior.barangay === 'Marilog' ? 'selected' : ''}>Marilog</option>
                                    <option value="Matina Aplaya" ${senior.barangay === 'Matina Aplaya' ? 'selected' : ''}>Matina Aplaya</option>
                                    <option value="Matina Crossing" ${senior.barangay === 'Matina Crossing' ? 'selected' : ''}>Matina Crossing</option>
                                    <option value="Matina Pangi" ${senior.barangay === 'Matina Pangi' ? 'selected' : ''}>Matina Pangi</option>
                                    <option value="Mintal" ${senior.barangay === 'Mintal' ? 'selected' : ''}>Mintal</option>
                                    <option value="Mudiang" ${senior.barangay === 'Mudiang' ? 'selected' : ''}>Mudiang</option>
                                    <option value="Mulig" ${senior.barangay === 'Mulig' ? 'selected' : ''}>Mulig</option>
                                    <option value="New Valencia" ${senior.barangay === 'New Valencia' ? 'selected' : ''}>New Valencia</option>
                                    <option value="Pampanga" ${senior.barangay === 'Pampanga' ? 'selected' : ''}>Pampanga</option>
                                    <option value="Panacan" ${senior.barangay === 'Panacan' ? 'selected' : ''}>Panacan</option>
                                    <option value="Pandaitan" ${senior.barangay === 'Pandaitan' ? 'selected' : ''}>Pandaitan</option>
                                    <option value="Panorama" ${senior.barangay === 'Panorama' ? 'selected' : ''}>Panorama</option>
                                    <option value="Paquibato" ${senior.barangay === 'Paquibato' ? 'selected' : ''}>Paquibato</option>
                                    <option value="Paradise Embac" ${senior.barangay === 'Paradise Embac' ? 'selected' : ''}>Paradise Embac</option>
                                    <option value="Pasian" ${senior.barangay === 'Pasian' ? 'selected' : ''}>Pasian</option>
                                    <option value="Poblacion" ${senior.barangay === 'Poblacion' ? 'selected' : ''}>Poblacion</option>
                                    <option value="Salapawan" ${senior.barangay === 'Salapawan' ? 'selected' : ''}>Salapawan</option>
                                    <option value="Saloy" ${senior.barangay === 'Saloy' ? 'selected' : ''}>Saloy</option>
                                    <option value="San Antonio" ${senior.barangay === 'San Antonio' ? 'selected' : ''}>San Antonio</option>
                                    <option value="San Isidro" ${senior.barangay === 'San Isidro' ? 'selected' : ''}>San Isidro</option>
                                    <option value="San Jose" ${senior.barangay === 'San Jose' ? 'selected' : ''}>San Jose</option>
                                    <option value="San Rafael" ${senior.barangay === 'San Rafael' ? 'selected' : ''}>San Rafael</option>
                                    <option value="Sasa" ${senior.barangay === 'Sasa' ? 'selected' : ''}>Sasa</option>
                                    <option value="Sirawan" ${senior.barangay === 'Sirawan' ? 'selected' : ''}>Sirawan</option>
                                    <option value="Sto. Niño" ${senior.barangay === 'Sto. Niño' ? 'selected' : ''}>Sto. Niño</option>
                                    <option value="Suawan" ${senior.barangay === 'Suawan' ? 'selected' : ''}>Suawan</option>
                                    <option value="Subasta" ${senior.barangay === 'Subasta' ? 'selected' : ''}>Subasta</option>
                                    <option value="Talomo" ${senior.barangay === 'Talomo' ? 'selected' : ''}>Talomo</option>
                                    <option value="Talomo River" ${senior.barangay === 'Talomo River' ? 'selected' : ''}>Talomo River</option>
                                    <option value="Talomo Proper" ${senior.barangay === 'Talomo Proper' ? 'selected' : ''}>Talomo Proper</option>
                                    <option value="Tamugan" ${senior.barangay === 'Tamugan' ? 'selected' : ''}>Tamugan</option>
                                    <option value="Tibungco" ${senior.barangay === 'Tibungco' ? 'selected' : ''}>Tibungco</option>
                                    <option value="Tigatto" ${senior.barangay === 'Tigatto' ? 'selected' : ''}>Tigatto</option>
                                    <option value="Tugbok" ${senior.barangay === 'Tugbok' ? 'selected' : ''}>Tugbok</option>
                                    <option value="Ula" ${senior.barangay === 'Ula' ? 'selected' : ''}>Ula</option>
                                    <option value="Vicente Hizon Sr." ${senior.barangay === 'Vicente Hizon Sr.' ? 'selected' : ''}>Vicente Hizon Sr.</option>
                                    <option value="Waan" ${senior.barangay === 'Waan' ? 'selected' : ''}>Waan</option>
                                    <option value="Wangan" ${senior.barangay === 'Wangan' ? 'selected' : ''}>Wangan</option>
                                    <option value="Wilfredo Aquino" ${senior.barangay === 'Wilfredo Aquino' ? 'selected' : ''}>Wilfredo Aquino</option>
                                    <option value="Wines" ${senior.barangay === 'Wines' ? 'selected' : ''}>Wines</option>
                                    <option value="Yakal" ${senior.barangay === 'Yakal' ? 'selected' : ''}>Yakal</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>City</label>
                                <input type="text" id="editCity" value="Davao City" readonly />
                            </div>
                            <div class="form-group">
                                <label>Province</label>
                                <input type="text" id="editProvince" value="Davao Del Sur" readonly />
                            </div>
                        </div>
                    </div>

                    <!-- Emergency Contact Section -->
                    <div class="form-section">
                        <h4 class="section-title">
                            <i class="fas fa-exclamation-triangle"></i> Emergency Contact
                        </h4>
                        <div class="form-grid">
                            <div class="form-group">
                                <label>Emergency Contact Name *</label>
                                <input type="text" id="editEmergencyContactName" value="${senior.emergencyContactName || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Emergency Contact Phone *</label>
                                <input type="tel" id="editEmergencyContactPhone" value="${senior.emergencyContactPhone || ''}" required />
                            </div>
                            <div class="form-group">
                                <label>Relationship *</label>
                                <select id="editRelationship" required>
                                    <option value="">Select Relationship</option>
                                    <option value="Spouse" ${senior.relationship === 'Spouse' ? 'selected' : ''}>Spouse</option>
                                    <option value="Son" ${senior.relationship === 'Son' ? 'selected' : ''}>Son</option>
                                    <option value="Daughter" ${senior.relationship === 'Daughter' ? 'selected' : ''}>Daughter</option>
                                    <option value="Son-in-law" ${senior.relationship === 'Son-in-law' ? 'selected' : ''}>Son-in-law</option>
                                    <option value="Daughter-in-law" ${senior.relationship === 'Daughter-in-law' ? 'selected' : ''}>Daughter-in-law</option>
                                    <option value="Grandson" ${senior.relationship === 'Grandson' ? 'selected' : ''}>Grandson</option>
                                    <option value="Granddaughter" ${senior.relationship === 'Granddaughter' ? 'selected' : ''}>Granddaughter</option>
                                    <option value="Brother" ${senior.relationship === 'Brother' ? 'selected' : ''}>Brother</option>
                                    <option value="Sister" ${senior.relationship === 'Sister' ? 'selected' : ''}>Sister</option>
                                    <option value="Friend" ${senior.relationship === 'Friend' ? 'selected' : ''}>Friend</option>
                                    <option value="Neighbor" ${senior.relationship === 'Neighbor' ? 'selected' : ''}>Neighbor</option>
                                    <option value="Caregiver" ${senior.relationship === 'Caregiver' ? 'selected' : ''}>Caregiver</option>
                                    <option value="Other" ${senior.relationship === 'Other' ? 'selected' : ''}>Other</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- Account Information Section -->
                    <div class="form-section">
                        <h4 class="section-title">
                            <i class="fas fa-user-shield"></i> Account Information
                        </h4>
                        <div class="form-grid">
                            <div class="form-group">
                                <label>Account Status</label>
                                <select id="editAccountStatus">
                                    <option value="true" ${senior.isActive ? 'selected' : ''}>Active</option>
                                    <option value="false" ${!senior.isActive ? 'selected' : ''}>Inactive</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>Verification Status</label>
                                <select id="editVerificationStatus">
                                    <option value="true" ${senior.accountVerified ? 'selected' : ''}>Verified</option>
                                    <option value="false" ${!senior.accountVerified ? 'selected' : ''}>Unverified</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="seniorsManager.saveSeniorChanges('${senior.id}')">
                    <i class="fas fa-save"></i> Save Changes
                </button>
                <button class="btn btn-secondary" onclick="closeModal()">
                    <i class="fas fa-times"></i> Cancel
                </button>
            </div>
        `;
    }

    formatDate(date) {
        if (!date) return 'N/A';
        const d = new Date(date);
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }


    renderEmergencyContacts(contacts, isViewMode) {
        if (!contacts || contacts.length === 0) {
            return `
                <div class="empty-state">
                    <i class="fas fa-phone-slash"></i>
                    <p>No emergency contacts added</p>
                </div>
            `;
        }

        return contacts.map((contact, index) => `
            <div class="emergency-contact-card">
                <div class="contact-avatar">
                    <i class="fas fa-user"></i>
                </div>
                <div class="contact-details">
                    <h4 class="contact-name">${contact.name}</h4>
                    <p class="contact-relationship">${contact.relationship}</p>
                    <p class="contact-phone">
                        <i class="fas fa-phone"></i>
                        ${contact.phoneNumber}
                    </p>
                    ${contact.email ? `
                        <p class="contact-email">
                            <i class="fas fa-envelope"></i>
                            ${contact.email}
                        </p>
                    ` : ''}
                </div>
                ${!isViewMode ? `
                    <button type="button" class="btn btn-sm btn-error" onclick="seniorsManager.removeEmergencyContact(${index})" title="Remove Contact">
                        <i class="fas fa-trash"></i>
                    </button>
                ` : ''}
            </div>
        `).join('');
    }

    formatDateForInput(date) {
        if (!date) return '';
        if (date.toDate) {
            return date.toDate().toISOString().split('T')[0];
        }
        return new Date(date).toISOString().split('T')[0];
    }

    async saveSenior(seniorId) {
        try {
            const formData = this.collectFormData();
            await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, formData);
            this.showSuccess('Senior citizen information updated successfully');
            this.closeModal();
        } catch (error) {
            console.error('Error saving senior:', error);
            this.showError('Failed to save changes');
        }
    }

    async saveSeniorChanges(seniorId) {
        try {
            // Validate required fields
            const requiredFields = [
                'editFirstName', 'editLastName', 'editBirthday', 'editGender', 
                'editPhone', 'editStreet', 'editBarangay', 'editEmergencyContactName',
                'editEmergencyContactPhone', 'editRelationship', 'editEmail'
            ];

            for (const fieldId of requiredFields) {
                const field = document.getElementById(fieldId);
                if (!field || !field.value.trim()) {
                    this.showError(`Please fill in all required fields. Missing: ${fieldId.replace('edit', '')}`);
                    return;
                }
            }

            // Collect all form data
            const data = {
                // Basic Information
                firstName: document.getElementById('editFirstName').value.trim(),
                lastName: document.getElementById('editLastName').value.trim(),
                birthDate: document.getElementById('editBirthday').value, // Use birthDate to match Android app
                age: Number(document.getElementById('editAge').value) || null,
                gender: document.getElementById('editGender').value,
                maritalStatus: document.getElementById('editMaritalStatus').value || null,
                phoneNumber: document.getElementById('editPhone').value.trim(),
                email: document.getElementById('editEmail').value.trim(),
                
                // Membership Information
                sssMember: document.getElementById('editSSSMember').checked,
                sssNumber: document.getElementById('editSSSNumber').value.trim() || null,
                gsisMember: document.getElementById('editGSISMember').checked,
                gsisNumber: document.getElementById('editGSISNumber').value.trim() || null,
                oscaMember: document.getElementById('editOSCAMember').checked,
                oscaNumber: document.getElementById('editOSCANumber').value.trim() || null,
                philHealthMember: document.getElementById('editPhilHealthMember').checked,
                philHealthNumber: document.getElementById('editPhilHealthNumber').value.trim() || null,
                
                // Address Information
                houseNumberAndStreet: document.getElementById('editStreet').value.trim(),
                barangay: document.getElementById('editBarangay').value,
                city: document.getElementById('editCity').value,
                province: document.getElementById('editProvince').value,
                zipCode: "8000", // Fixed postal code for Davao City
                
                // Emergency Contact - Update the primary emergency contact
                emergencyContacts: [{
                    name: document.getElementById('editEmergencyContactName').value.trim(),
                    phoneNumber: document.getElementById('editEmergencyContactPhone').value.trim(),
                    relationship: document.getElementById('editRelationship').value,
                    isPrimary: true
                }],
                
                // Account Information
                isActive: document.getElementById('editAccountStatus').value === 'true',
                accountVerified: document.getElementById('editVerificationStatus').value === 'true',
                
                // System fields
                updatedAt: FirebaseUtils.getTimestamp(),
                lastUpdatedBy: 'admin' // Track that this was updated by admin
            };

            // Update in Firebase
            await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, data);

            this.showSuccess('Senior citizen information updated successfully!');
            this.closeModal();
            this.loadSeniors();
            
        } catch (error) {
            console.error('Error saving senior changes:', error);
            this.showError('Failed to save changes. Please try again.');
        }
    }

    collectFormData() {
        const form = document.querySelector('.senior-modal');
        const data = {};
        
        // Collect all form inputs
        form.querySelectorAll('input, select, textarea').forEach(input => {
            if (input.name || input.id) {
                const key = input.name || input.id;
                data[key] = input.value;
            }
        });
        
        return data;
    }

    async exportSeniorsData() {
        try {
            this.showLoading('Exporting data...');
            
            const seniorsData = this.filteredSeniors.map(senior => ({
                'Full Name': `${senior.firstName} ${senior.lastName}`,
                'Email': senior.email,
                'Phone': senior.phoneNumber,
                'Age': senior.age,
                'Address': `${senior.houseNumberAndStreet}, ${senior.barangay}, ${senior.city}`,
                'OSCA Number': senior.oscaNumber,
                'Status': senior.isActive ? 'Active' : 'Inactive',
                'Verification': senior.accountVerified ? 'Verified' : 'Unverified',
                'Created': FirebaseUtils.formatDate(senior.createdAt)
            }));

            this.downloadCSV(seniorsData, 'senior_citizens_data.csv');
            this.showSuccess('Data exported successfully');
        } catch (error) {
            console.error('Error exporting data:', error);
            this.showError('Failed to export data');
        } finally {
            this.hideLoading();
        }
    }

    openAddSeniorModal() {
        const body = `
            <div class="senior-form-container">
                <!-- Basic Information Section -->
                <div class="form-section">
                    <h4 class="section-title">Basic Information</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>First Name *</label>
                            <input id="newFirstName" type="text" required />
                        </div>
                        <div class="form-group">
                            <label>Last Name *</label>
                            <input id="newLastName" type="text" required />
                        </div>
                        <div class="form-group">
                            <label>Birthday *</label>
                            <input id="newBirthday" type="date" required />
                        </div>
                        <div class="form-group">
                            <label>Age (Auto-calculated)</label>
                            <input id="newAge" type="number" readonly />
                        </div>
                        <div class="form-group">
                            <label>Gender *</label>
                            <select id="newGender" required>
                                <option value="">Select Gender</option>
                                <option value="Male">Male</option>
                                <option value="Female">Female</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Marital Status</label>
                            <select id="newMaritalStatus">
                                <option value="">Select Status</option>
                                <option value="Single">Single</option>
                                <option value="Married">Married</option>
                                <option value="Widowed">Widowed</option>
                                <option value="Divorced">Divorced</option>
                                <option value="Separated">Separated</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Phone Number *</label>
                            <input id="newPhone" type="tel" required />
                        </div>
                    </div>
                </div>

                <!-- Membership Information Section -->
                <div class="form-section">
                    <h4 class="section-title">Membership Information</h4>
                    <div class="form-grid">
                        <div class="form-group checkbox-group">
                            <label class="checkbox-label">
                                <input id="newSSSMember" type="checkbox" />
                                <span class="checkmark"></span>
                                SSS Member
                            </label>
                        </div>
                        <div class="form-group" id="sssNumberGroup" style="display: none;">
                            <label>SSS Number</label>
                            <input id="newSSSNumber" type="text" />
                        </div>
                        <div class="form-group checkbox-group">
                            <label class="checkbox-label">
                                <input id="newGSISMember" type="checkbox" />
                                <span class="checkmark"></span>
                                GSIS Member
                            </label>
                        </div>
                        <div class="form-group" id="gsisNumberGroup" style="display: none;">
                            <label>GSIS Number</label>
                            <input id="newGSISNumber" type="text" />
                        </div>
                        <div class="form-group checkbox-group">
                            <label class="checkbox-label">
                                <input id="newOSCAMember" type="checkbox" />
                                <span class="checkmark"></span>
                                OSCA Member
                            </label>
                        </div>
                        <div class="form-group" id="oscaNumberGroup" style="display: none;">
                            <label>OSCA Number</label>
                            <input id="newOSCANumber" type="text" />
                        </div>
                        <div class="form-group checkbox-group">
                            <label class="checkbox-label">
                                <input id="newPhilHealthMember" type="checkbox" />
                                <span class="checkmark"></span>
                                PhilHealth Member
                            </label>
                        </div>
                        <div class="form-group" id="philHealthNumberGroup" style="display: none;">
                            <label>PhilHealth Number</label>
                            <input id="newPhilHealthNumber" type="text" />
                        </div>
                    </div>
                </div>

                <!-- Address Information Section -->
                <div class="form-section">
                    <h4 class="section-title">Address Information</h4>
                    <div class="form-grid">
                        <div class="form-group full-width">
                            <label>House Number and Street *</label>
                            <input id="newStreet" type="text" required />
                        </div>
                        <div class="form-group">
                            <label>Barangay *</label>
                            <select id="newBarangay" required>
                                <option value="">Select Barangay</option>
                                <option value="1-A Poblacion">1-A Poblacion</option>
                                <option value="2-A Poblacion">2-A Poblacion</option>
                                <option value="3-A Poblacion">3-A Poblacion</option>
                                <option value="4-A Poblacion">4-A Poblacion</option>
                                <option value="5-A Poblacion">5-A Poblacion</option>
                                <option value="6-A Poblacion">6-A Poblacion</option>
                                <option value="7-A Poblacion">7-A Poblacion</option>
                                <option value="8-A Poblacion">8-A Poblacion</option>
                                <option value="9-A Poblacion">9-A Poblacion</option>
                                <option value="10-A Poblacion">10-A Poblacion</option>
                                <option value="11-B Poblacion">11-B Poblacion</option>
                                <option value="12-B Poblacion">12-B Poblacion</option>
                                <option value="13-B Poblacion">13-B Poblacion</option>
                                <option value="14-B Poblacion">14-B Poblacion</option>
                                <option value="15-B Poblacion">15-B Poblacion</option>
                                <option value="16-B Poblacion">16-B Poblacion</option>
                                <option value="17-B Poblacion">17-B Poblacion</option>
                                <option value="18-B Poblacion">18-B Poblacion</option>
                                <option value="19-B Poblacion">19-B Poblacion</option>
                                <option value="20-B Poblacion">20-B Poblacion</option>
                                <option value="21-C Poblacion">21-C Poblacion</option>
                                <option value="22-C Poblacion">22-C Poblacion</option>
                                <option value="23-C Poblacion">23-C Poblacion</option>
                                <option value="24-C Poblacion">24-C Poblacion</option>
                                <option value="25-C Poblacion">25-C Poblacion</option>
                                <option value="26-C Poblacion">26-C Poblacion</option>
                                <option value="27-C Poblacion">27-C Poblacion</option>
                                <option value="28-C Poblacion">28-C Poblacion</option>
                                <option value="29-C Poblacion">29-C Poblacion</option>
                                <option value="30-C Poblacion">30-C Poblacion</option>
                                <option value="31-D Poblacion">31-D Poblacion</option>
                                <option value="32-D Poblacion">32-D Poblacion</option>
                                <option value="33-D Poblacion">33-D Poblacion</option>
                                <option value="34-D Poblacion">34-D Poblacion</option>
                                <option value="35-D Poblacion">35-D Poblacion</option>
                                <option value="36-D Poblacion">36-D Poblacion</option>
                                <option value="37-D Poblacion">37-D Poblacion</option>
                                <option value="38-D Poblacion">38-D Poblacion</option>
                                <option value="39-D Poblacion">39-D Poblacion</option>
                                <option value="40-D Poblacion">40-D Poblacion</option>
                                <option value="Acacia">Acacia</option>
                                <option value="Agdao">Agdao</option>
                                <option value="Alfonso Angliongto Sr.">Alfonso Angliongto Sr.</option>
                                <option value="Angalan">Angalan</option>
                                <option value="Atan-Awe">Atan-Awe</option>
                                <option value="Bagong Silang">Bagong Silang</option>
                                <option value="Bago Aplaya">Bago Aplaya</option>
                                <option value="Bago Gallera">Bago Gallera</option>
                                <option value="Bago Oshiro">Bago Oshiro</option>
                                <option value="Bajada">Bajada</option>
                                <option value="Balusong">Balusong</option>
                                <option value="Bangkas Heights">Bangkas Heights</option>
                                <option value="Barangay 1-A">Barangay 1-A</option>
                                <option value="Barangay 2-A">Barangay 2-A</option>
                                <option value="Barangay 3-A">Barangay 3-A</option>
                                <option value="Barangay 4-A">Barangay 4-A</option>
                                <option value="Barangay 5-A">Barangay 5-A</option>
                                <option value="Barangay 6-A">Barangay 6-A</option>
                                <option value="Barangay 7-A">Barangay 7-A</option>
                                <option value="Barangay 8-A">Barangay 8-A</option>
                                <option value="Barangay 9-A">Barangay 9-A</option>
                                <option value="Barangay 10-A">Barangay 10-A</option>
                                <option value="Barangay 11-B">Barangay 11-B</option>
                                <option value="Barangay 12-B">Barangay 12-B</option>
                                <option value="Barangay 13-B">Barangay 13-B</option>
                                <option value="Barangay 14-B">Barangay 14-B</option>
                                <option value="Barangay 15-B">Barangay 15-B</option>
                                <option value="Barangay 16-B">Barangay 16-B</option>
                                <option value="Barangay 17-B">Barangay 17-B</option>
                                <option value="Barangay 18-B">Barangay 18-B</option>
                                <option value="Barangay 19-B">Barangay 19-B</option>
                                <option value="Barangay 20-B">Barangay 20-B</option>
                                <option value="Barangay 21-C">Barangay 21-C</option>
                                <option value="Barangay 22-C">Barangay 22-C</option>
                                <option value="Barangay 23-C">Barangay 23-C</option>
                                <option value="Barangay 24-C">Barangay 24-C</option>
                                <option value="Barangay 25-C">Barangay 25-C</option>
                                <option value="Barangay 26-C">Barangay 26-C</option>
                                <option value="Barangay 27-C">Barangay 27-C</option>
                                <option value="Barangay 28-C">Barangay 28-C</option>
                                <option value="Barangay 29-C">Barangay 29-C</option>
                                <option value="Barangay 30-C">Barangay 30-C</option>
                                <option value="Barangay 31-D">Barangay 31-D</option>
                                <option value="Barangay 32-D">Barangay 32-D</option>
                                <option value="Barangay 33-D">Barangay 33-D</option>
                                <option value="Barangay 34-D">Barangay 34-D</option>
                                <option value="Barangay 35-D">Barangay 35-D</option>
                                <option value="Barangay 36-D">Barangay 36-D</option>
                                <option value="Barangay 37-D">Barangay 37-D</option>
                                <option value="Barangay 38-D">Barangay 38-D</option>
                                <option value="Barangay 39-D">Barangay 39-D</option>
                                <option value="Barangay 40-D">Barangay 40-D</option>
                                <option value="Bucana">Bucana</option>
                                <option value="Buhangin">Buhangin</option>
                                <option value="Bunawan">Bunawan</option>
                                <option value="Cabantian">Cabantian</option>
                                <option value="Calinan">Calinan</option>
                                <option value="Callawa">Callawa</option>
                                <option value="Camansi">Camansi</option>
                                <option value="Catalunan Grande">Catalunan Grande</option>
                                <option value="Catalunan Pequeño">Catalunan Pequeño</option>
                                <option value="Central Park">Central Park</option>
                                <option value="Centro">Centro</option>
                                <option value="Communal">Communal</option>
                                <option value="Croton">Croton</option>
                                <option value="Dacudao">Dacudao</option>
                                <option value="Daliao">Daliao</option>
                                <option value="Datu Salumay">Datu Salumay</option>
                                <option value="Deca Homes">Deca Homes</option>
                                <option value="Dumoy">Dumoy</option>
                                <option value="Ecoland">Ecoland</option>
                                <option value="Eden">Eden</option>
                                <option value="Fatima">Fatima</option>
                                <option value="Gatungan">Gatungan</option>
                                <option value="Gov. Paciano Bangoy">Gov. Paciano Bangoy</option>
                                <option value="Gov. Vicente Duterte">Gov. Vicente Duterte</option>
                                <option value="Guadalupe">Guadalupe</option>
                                <option value="Gumalang">Gumalang</option>
                                <option value="Hizon">Hizon</option>
                                <option value="Indangan">Indangan</option>
                                <option value="Kabacan">Kabacan</option>
                                <option value="Kaligutan">Kaligutan</option>
                                <option value="Lacson">Lacson</option>
                                <option value="Lamanan">Lamanan</option>
                                <option value="Lampianao">Lampianao</option>
                                <option value="Lasang">Lasang</option>
                                <option value="Leon Garcia Sr.">Leon Garcia Sr.</option>
                                <option value="Lizada">Lizada</option>
                                <option value="Los Amigos">Los Amigos</option>
                                <option value="Lubogan">Lubogan</option>
                                <option value="Lungaog">Lungaog</option>
                                <option value="Maa">Maa</option>
                                <option value="Magtuod">Magtuod</option>
                                <option value="Mahayag">Mahayag</option>
                                <option value="Malabog">Malabog</option>
                                <option value="Malagos">Malagos</option>
                                <option value="Malibago">Malibago</option>
                                <option value="Mamay">Mamay</option>
                                <option value="Mandug">Mandug</option>
                                <option value="Manuel Guianga">Manuel Guianga</option>
                                <option value="Mapula">Mapula</option>
                                <option value="Marapangi">Marapangi</option>
                                <option value="Marilog">Marilog</option>
                                <option value="Matina Aplaya">Matina Aplaya</option>
                                <option value="Matina Crossing">Matina Crossing</option>
                                <option value="Matina Pangi">Matina Pangi</option>
                                <option value="Mintal">Mintal</option>
                                <option value="Mudiang">Mudiang</option>
                                <option value="Mulig">Mulig</option>
                                <option value="New Valencia">New Valencia</option>
                                <option value="Pampanga">Pampanga</option>
                                <option value="Panacan">Panacan</option>
                                <option value="Pandaitan">Pandaitan</option>
                                <option value="Panorama">Panorama</option>
                                <option value="Paquibato">Paquibato</option>
                                <option value="Paradise Embac">Paradise Embac</option>
                                <option value="Pasian">Pasian</option>
                                <option value="Poblacion">Poblacion</option>
                                <option value="Salapawan">Salapawan</option>
                                <option value="Saloy">Saloy</option>
                                <option value="San Antonio">San Antonio</option>
                                <option value="San Isidro">San Isidro</option>
                                <option value="San Jose">San Jose</option>
                                <option value="San Rafael">San Rafael</option>
                                <option value="Sasa">Sasa</option>
                                <option value="Sirawan">Sirawan</option>
                                <option value="Sto. Niño">Sto. Niño</option>
                                <option value="Suawan">Suawan</option>
                                <option value="Subasta">Subasta</option>
                                <option value="Talomo">Talomo</option>
                                <option value="Talomo River">Talomo River</option>
                                <option value="Talomo Proper">Talomo Proper</option>
                                <option value="Tamugan">Tamugan</option>
                                <option value="Tibungco">Tibungco</option>
                                <option value="Tigatto">Tigatto</option>
                                <option value="Tugbok">Tugbok</option>
                                <option value="Ula">Ula</option>
                                <option value="Vicente Hizon Sr.">Vicente Hizon Sr.</option>
                                <option value="Waan">Waan</option>
                                <option value="Wangan">Wangan</option>
                                <option value="Wilfredo Aquino">Wilfredo Aquino</option>
                                <option value="Wines">Wines</option>
                                <option value="Yakal">Yakal</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>City</label>
                            <input id="newCity" type="text" value="Davao City" readonly />
                        </div>
                        <div class="form-group">
                            <label>Province</label>
                            <input id="newProvince" type="text" value="Davao Del Sur" readonly />
                        </div>
                    </div>
                </div>

                <!-- Emergency Contact Section -->
                <div class="form-section">
                    <h4 class="section-title">Emergency Contact</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Emergency Contact Name *</label>
                            <input id="newEmergencyContactName" type="text" required />
                        </div>
                        <div class="form-group">
                            <label>Emergency Contact Phone *</label>
                            <input id="newEmergencyContactPhone" type="tel" required />
                        </div>
                        <div class="form-group">
                            <label>Relationship *</label>
                            <select id="newRelationship" required>
                                <option value="">Select Relationship</option>
                                <option value="Spouse">Spouse</option>
                                <option value="Son">Son</option>
                                <option value="Daughter">Daughter</option>
                                <option value="Son-in-law">Son-in-law</option>
                                <option value="Daughter-in-law">Daughter-in-law</option>
                                <option value="Grandson">Grandson</option>
                                <option value="Granddaughter">Granddaughter</option>
                                <option value="Brother">Brother</option>
                                <option value="Sister">Sister</option>
                                <option value="Friend">Friend</option>
                                <option value="Neighbor">Neighbor</option>
                                <option value="Caregiver">Caregiver</option>
                                <option value="Other">Other</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- Account Information Section -->
                <div class="form-section">
                    <h4 class="section-title">Account Information</h4>
                    <div class="form-grid">
                        <div class="form-group full-width">
                            <label>Profile Image</label>
                            <div class="image-upload-container">
                                <div class="image-preview" id="profileImagePreview">
                                    <i class="fas fa-user-circle"></i>
                                    <span>No image selected</span>
                                </div>
                                <input type="file" id="newProfileImage" accept="image/*" style="display: none;" />
                                <button type="button" class="btn btn-outline btn-sm" onclick="document.getElementById('newProfileImage').click()">
                                    <i class="fas fa-upload"></i> Choose Image
                                </button>
                                <button type="button" class="btn btn-outline btn-sm" id="removeImageBtn" onclick="seniorsManager.removeProfileImage()" style="display: none;">
                                    <i class="fas fa-trash"></i> Remove
                                </button>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Email Address *</label>
                            <input id="newEmail" type="email" required />
                        </div>
                        <div class="form-group">
                            <label>Password *</label>
                            <input id="newPassword" type="password" required />
                        </div>
                        <div class="form-group">
                            <label>Confirm Password *</label>
                            <input id="newConfirmPassword" type="password" required />
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="seniorsManager.saveNewSenior()">
                    <i class="fas fa-save"></i> Create Senior Account
                </button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        this.openModal('Add Senior Citizen', body);
        
        // Add event listeners for conditional fields
        this.setupAddSeniorEventListeners();
        
        // Add image upload event listener
        this.setupImageUploadListener();
    }

    setupAddSeniorEventListeners() {
        // Birthday to age calculation
        const birthdayInput = document.getElementById('newBirthday');
        const ageInput = document.getElementById('newAge');
        
        if (birthdayInput && ageInput) {
            birthdayInput.addEventListener('change', () => {
                const birthday = new Date(birthdayInput.value);
                const today = new Date();
                let age = today.getFullYear() - birthday.getFullYear();
                const monthDiff = today.getMonth() - birthday.getMonth();
                
                if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthday.getDate())) {
                    age--;
                }
                
                ageInput.value = age >= 0 ? age : '';
            });
        }

        // Membership checkboxes
        const membershipCheckboxes = [
            { checkbox: 'newSSSMember', group: 'sssNumberGroup' },
            { checkbox: 'newGSISMember', group: 'gsisNumberGroup' },
            { checkbox: 'newOSCAMember', group: 'oscaNumberGroup' },
            { checkbox: 'newPhilHealthMember', group: 'philHealthNumberGroup' }
        ];

        membershipCheckboxes.forEach(({ checkbox, group }) => {
            const checkboxElement = document.getElementById(checkbox);
            const groupElement = document.getElementById(group);
            
            if (checkboxElement && groupElement) {
                checkboxElement.addEventListener('change', () => {
                    groupElement.style.display = checkboxElement.checked ? 'block' : 'none';
                });
            }
        });
    }

    setupImageUploadListener() {
        const imageInput = document.getElementById('newProfileImage');
        if (imageInput) {
            imageInput.addEventListener('change', (e) => {
                this.handleImageUpload(e);
            });
        }
    }

    handleImageUpload(event) {
        const file = event.target.files[0];
        if (file) {
            // Validate file type
            if (!file.type.startsWith('image/')) {
                this.showError('Please select a valid image file.');
                return;
            }

            // Validate file size (max 5MB)
            if (file.size > 5 * 1024 * 1024) {
                this.showError('Image size must be less than 5MB.');
                return;
            }

            // Create preview
            const reader = new FileReader();
            reader.onload = (e) => {
                this.showImagePreview(e.target.result);
            };
            reader.readAsDataURL(file);
        }
    }

    showImagePreview(imageUrl) {
        const preview = document.getElementById('profileImagePreview');
        const removeBtn = document.getElementById('removeImageBtn');
        
        if (preview) {
            preview.innerHTML = `
                <img src="${imageUrl}" alt="Profile Preview" style="width: 100px; height: 100px; border-radius: 50%; object-fit: cover;">
            `;
        }
        
        if (removeBtn) {
            removeBtn.style.display = 'inline-block';
        }
    }

    removeProfileImage() {
        const imageInput = document.getElementById('newProfileImage');
        const preview = document.getElementById('profileImagePreview');
        const removeBtn = document.getElementById('removeImageBtn');
        
        if (imageInput) {
            imageInput.value = '';
        }
        
        if (preview) {
            preview.innerHTML = `
                <i class="fas fa-user-circle"></i>
                <span>No image selected</span>
            `;
        }
        
        if (removeBtn) {
            removeBtn.style.display = 'none';
        }
    }

    async uploadProfileImage(file) {
        try {
            console.log('=== CLOUDINARY UPLOAD DEBUG ===');
            console.log('File details:', {
                name: file.name,
                size: file.size,
                type: file.type
            });
            console.log('Cloudinary config:', this.cloudinaryConfig);

            // Basic file validation
            if (!file.type.startsWith('image/')) {
                throw new Error('Please select an image file (JPEG, PNG, WebP, or GIF)');
            }
            
            if (file.size > 5 * 1024 * 1024) {
                throw new Error('File too large. Please select an image smaller than 5MB.');
            }
            
            // Create minimal FormData - no transformations, no folder
            const formData = new FormData();
            formData.append('file', file);
            formData.append('upload_preset', this.cloudinaryConfig.uploadPreset);
            
            console.log('FormData contents:');
            for (let [key, value] of formData.entries()) {
                console.log(`${key}:`, value);
            }
            
            const uploadUrl = `https://api.cloudinary.com/v1_1/${this.cloudinaryConfig.cloudName}/image/upload`;
            console.log('Upload URL:', uploadUrl);
            
            // Upload to Cloudinary
            const response = await fetch(uploadUrl, {
                method: 'POST',
                body: formData
            });
            
            console.log('Response status:', response.status);
            console.log('Response headers:', Object.fromEntries(response.headers.entries()));
            
            const responseText = await response.text();
            console.log('Raw response:', responseText);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}\nResponse: ${responseText}`);
            }
            
            let data;
            try {
                data = JSON.parse(responseText);
            } catch (parseError) {
                console.error('Failed to parse JSON response:', parseError);
                throw new Error(`Invalid response format: ${responseText}`);
            }
            
            console.log('Parsed response:', data);
            
            if (data.secure_url) {
                console.log('✅ SUCCESS: Image uploaded successfully!');
                console.log('Image URL:', data.secure_url);
                return data.secure_url;
            } else {
                console.error('❌ No secure_url in response');
                throw new Error(`No image URL returned. Response: ${JSON.stringify(data)}`);
            }
            
        } catch (error) {
            console.error('❌ UPLOAD ERROR:', error);
            console.error('Error stack:', error.stack);
            throw new Error(`Upload failed: ${error.message}`);
        }
    }

    // Debug function to test Cloudinary connection
    async testCloudinaryConnection() {
        try {
            console.log('=== CLOUDINARY CONNECTION TEST ===');
            console.log('Configuration:', this.cloudinaryConfig);
            
            // Test with a simple image file
            const canvas = document.createElement('canvas');
            canvas.width = 100;
            canvas.height = 100;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#FF0000';
            ctx.fillRect(0, 0, 100, 100);
            
            const testFile = new Promise(resolve => {
                canvas.toBlob(resolve, 'image/png');
            });
            
            const blob = await testFile;
            const file = new File([blob], 'test.png', { type: 'image/png' });
            
            console.log('Test file created:', file);
            
            const formData = new FormData();
            formData.append('file', file);
            formData.append('upload_preset', this.cloudinaryConfig.uploadPreset);
            
            const uploadUrl = `https://api.cloudinary.com/v1_1/${this.cloudinaryConfig.cloudName}/image/upload`;
            console.log('Testing upload to:', uploadUrl);
            
            const response = await fetch(uploadUrl, {
                method: 'POST',
                body: formData
            });
            
            console.log('Test response status:', response.status);
            const responseText = await response.text();
            console.log('Test response:', responseText);
            
            if (response.ok) {
                const data = JSON.parse(responseText);
                if (data.secure_url) {
                    console.log('✅ Cloudinary connection successful!');
                    console.log('Test image URL:', data.secure_url);
                    return true;
                } else {
                    console.error('❌ No secure_url in test response');
                    return false;
                }
            } else {
                console.error('❌ Cloudinary connection failed:', responseText);
                return false;
            }
        } catch (error) {
            console.error('❌ Cloudinary test error:', error);
            return false;
        }
    }

    // Simple test function for manual testing
    async testSimpleUpload() {
        console.log('=== SIMPLE UPLOAD TEST ===');
        try {
            // Create a simple test image
            const canvas = document.createElement('canvas');
            canvas.width = 50;
            canvas.height = 50;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#00FF00';
            ctx.fillRect(0, 0, 50, 50);
            
            const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
            const file = new File([blob], 'test-simple.png', { type: 'image/png' });
            
            console.log('Testing with file:', file);
            
            const result = await this.uploadProfileImage(file);
            console.log('✅ Upload successful! URL:', result);
            return result;
        } catch (error) {
            console.error('❌ Upload failed:', error);
            return null;
        }
    }

    async saveNewSenior() {
        try {
            // Validate required fields
            const requiredFields = [
                'newFirstName', 'newLastName', 'newBirthday', 'newGender', 
                'newPhone', 'newStreet', 'newBarangay', 'newEmergencyContactName',
                'newEmergencyContactPhone', 'newRelationship', 'newEmail', 
                'newPassword', 'newConfirmPassword'
            ];

            for (const fieldId of requiredFields) {
                const field = document.getElementById(fieldId);
                if (!field || !field.value.trim()) {
                    this.showError(`Please fill in all required fields. Missing: ${fieldId.replace('new', '')}`);
                    return;
                }
            }

            // Validate password confirmation
            const password = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('newConfirmPassword').value;
            if (password !== confirmPassword) {
                this.showError('Passwords do not match');
                return;
            }

            // Handle profile image upload
            let profileImageUrl = null;
            const imageFile = document.getElementById('newProfileImage').files[0];
            if (imageFile) {
                try {
                    this.showLoading('Uploading profile image...');
                    profileImageUrl = await this.uploadProfileImage(imageFile);
                } catch (error) {
                    console.error('Error uploading profile image:', error);
                    this.showError('Failed to upload profile image. Please try again.');
                    return;
                } finally {
                    this.hideLoading();
                }
            }

            // Collect all form data
            const data = {
                // Basic Information
                firstName: document.getElementById('newFirstName').value.trim(),
                lastName: document.getElementById('newLastName').value.trim(),
                birthday: document.getElementById('newBirthday').value,
                age: Number(document.getElementById('newAge').value) || null,
                gender: document.getElementById('newGender').value,
                maritalStatus: document.getElementById('newMaritalStatus').value || null,
                phoneNumber: document.getElementById('newPhone').value.trim(),
                
                // Membership Information
                sssMember: document.getElementById('newSSSMember').checked,
                sssNumber: document.getElementById('newSSSNumber').value.trim() || null,
                gsisMember: document.getElementById('newGSISMember').checked,
                gsisNumber: document.getElementById('newGSISNumber').value.trim() || null,
                oscaMember: document.getElementById('newOSCAMember').checked,
                oscaNumber: document.getElementById('newOSCANumber').value.trim() || null,
                philHealthMember: document.getElementById('newPhilHealthMember').checked,
                philHealthNumber: document.getElementById('newPhilHealthNumber').value.trim() || null,
                
                // Address Information
                houseNumberAndStreet: document.getElementById('newStreet').value.trim(),
                barangay: document.getElementById('newBarangay').value,
                city: document.getElementById('newCity').value,
                province: document.getElementById('newProvince').value,
                
                // Emergency Contact
                emergencyContactName: document.getElementById('newEmergencyContactName').value.trim(),
                emergencyContactPhone: document.getElementById('newEmergencyContactPhone').value.trim(),
                relationship: document.getElementById('newRelationship').value,
                
                // Account Information
                email: document.getElementById('newEmail').value.trim(),
                password: password, // Note: In production, this should be hashed
                profileImageUrl: profileImageUrl,
                
                // System fields
                role: 'senior_citizen',
                isActive: true,
                accountVerified: false,
                createdAt: FirebaseUtils.getTimestamp()
            };

            // Check if email already exists
            const emailExists = await this.checkEmailExists(data.email);
            if (emailExists) {
                this.showError('An account with this email already exists. Please use a different email address.');
                return;
            }

            // Verify resident/senior status
            const verdict = window.Verification.verifyResidentOrSenior(data);
            if (!verdict.passed) {
                this.showError('Verification failed: Not a Davao City resident or not a senior (age 60+).');
                return;
            }

            // Create Firebase Auth user first
            try {
                const userCredential = await auth.createUserWithEmailAndPassword(data.email, data.password);
                const firebaseUser = userCredential.user;
                
                // Update the user's display name
                await firebaseUser.updateProfile({
                    displayName: `${data.firstName} ${data.lastName}`.trim()
                });
                
                // Remove password from data before saving to Firestore for security
                const { password, ...userData } = data;
                
                // Add to Firestore with the Firebase Auth UID
                await db.collection(COLLECTIONS.USERS).doc(firebaseUser.uid).set({
                    ...userData,
                    isVerifiedResident: verdict.resident,
                    verifiedAt: FirebaseUtils.getTimestamp()
                });

                this.showSuccess('Senior citizen account created successfully!');
                this.closeModal();
                this.loadSeniors();
                
                // Update dashboard if needed
                this.updateDashboardVerifiedCount();
                
            } catch (authError) {
                console.error('Firebase Auth creation failed:', authError);
                if (authError.code === 'auth/email-already-in-use') {
                    this.showError('An account with this email already exists in Firebase Auth.');
                } else {
                    this.showError(`Failed to create authentication account: ${authError.message}`);
                }
                return;
            }
            
        } catch (e) {
            console.error('Failed to add senior', e);
            this.showError(`Failed to create senior account: ${e.message}`);
        }
    }

    addEmergencyContact() {
        // This method would add a new emergency contact to the current senior being edited
        // For now, we'll show a simple prompt - in a real implementation, this would open a modal
        const name = prompt('Enter emergency contact name:');
        if (name) {
            const phone = prompt('Enter emergency contact phone:');
            if (phone) {
                const relationship = prompt('Enter relationship:');
                if (relationship) {
                    // Add the contact to the current senior's emergency contacts
                    // This would need to be implemented based on the current editing context
                    console.log('Adding emergency contact:', { name, phone, relationship });
                }
            }
        }
    }

    removeEmergencyContact(index) {
        if (confirm('Are you sure you want to remove this emergency contact?')) {
            // Remove the contact at the specified index
            // This would need to be implemented based on the current editing context
            console.log('Removing emergency contact at index:', index);
        }
    }

    downloadCSV(data, filename) {
        const csv = this.convertToCSV(data);
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    convertToCSV(data) {
        if (data.length === 0) return '';
        
        const headers = Object.keys(data[0]);
        const csvContent = [
            headers.join(','),
            ...data.map(row => headers.map(header => `"${row[header] || ''}"`).join(','))
        ].join('\n');
        
        return csvContent;
    }

    updateStats() {
        // Update total seniors count
        const totalSeniorsElement = document.getElementById('totalSeniors');
        if (totalSeniorsElement) {
            totalSeniorsElement.textContent = this.seniors.length;
        }
        
        // Update role-specific stats
        const currentUserRole = window.mainApp?.currentUserRole;
        // With the current rules, any admin can see these stats
        if (currentUserRole) {
            // For facilitator admin: update verified accounts
            const verifiedElement = document.getElementById('verifiedAccountsFacilitator');
            if (verifiedElement) {
                const verifiedCount = this.seniors.filter(s => s.accountVerified || s.isVerified).length;
                verifiedElement.textContent = verifiedCount;
            }
        } else {
            // For other admin types: update active seniors
            const activeElement = document.getElementById('activeSeniors');
            if (activeElement) {
                activeElement.textContent = this.seniors.filter(s => s.isActive).length;
            }
        }
    }

    showLoading(message = 'Loading...') {
        if (window.mainApp) {
            window.mainApp.showLoading(message);
        }
    }

    hideLoading() {
        if (window.mainApp) {
            window.mainApp.hideLoading();
        }
    }

    showError(message) {
        if (window.mainApp) {
            window.mainApp.showError(message);
        } else {
            console.error(message);
            alert(message);
        }
    }

    showSuccess(message) {
        if (window.mainApp) {
            window.mainApp.showSuccess(message);
        } else {
            console.log(message);
            alert(message);
        }
    }

    openModal(title, body) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalBody').innerHTML = body;
        document.getElementById('modalOverlay').classList.remove('hidden');
    }

    closeModal() {
        document.getElementById('modalOverlay').classList.add('hidden');
    }

    async verifyResident(seniorId) {
        try {
            const senior = await FirebaseUtils.getDoc(COLLECTIONS.USERS, seniorId);
            if (!senior) return;
            const verdict = window.Verification.verifyResidentOrSenior(senior);
            if (!verdict.passed) {
                this.showError('Verification failed: Not a Davao City resident or not a senior.');
                return;
            }
            await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, {
                isVerifiedResident: true,
                verifiedAt: FirebaseUtils.getTimestamp()
            });
            this.showSuccess('Resident verification successful');
        } catch (e) {
            console.error('Verification error', e);
            this.showError('Failed to verify resident');
        }
    }

    async verifyAccount(seniorId) {
        try {
            if (!confirm('Verify this senior citizen account? The user will be marked as verified and can access all features.')) {
                return;
            }

            // Get current admin user info
            const currentAdmin = window.mainApp?.currentUser;
            const adminEmail = currentAdmin?.email || 'Unknown Admin';

            await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, {
                accountVerified: true,
                accountVerifiedAt: FirebaseUtils.getTimestamp(),
                accountVerifiedBy: adminEmail,
                verificationStatus: 'verified'
            });

            this.showSuccess('Senior citizen account verified successfully!');
            
            // Refresh the seniors list to show updated status
            this.loadSeniors();
            
            // Update modal UI if open
            this.updateModalAfterVerification();
            
            // Update dashboard verified accounts count
            this.updateDashboardVerifiedCount();
            
        } catch (e) {
            console.error('Account verification failed:', e);
            this.showError('Failed to verify account. Please try again.');
        }
    }

    updateModalAfterVerification() {
        const modal = document.getElementById('modalOverlay');
        if (modal && !modal.classList.contains('hidden')) {
            // Replace Verify Account button with a disabled Verified indicator
            const actions = modal.querySelector('.modal-actions');
            if (actions) {
                const verifyBtn = Array.from(actions.querySelectorAll('button')).find(b => 
                    b.textContent.includes('Verify Account') || b.onclick?.toString().includes('verifyAccount')
                );
                if (verifyBtn) {
                    const verifiedBtn = document.createElement('button');
                    verifiedBtn.className = 'btn btn-success';
                    verifiedBtn.disabled = true;
                    verifiedBtn.innerHTML = '<i class="fas fa-check-circle"></i> Account Verified';
                    verifyBtn.replaceWith(verifiedBtn);
                }
            }
            
            // Update Account Verified field if present
            const verifiedField = modal.querySelector('input[readonly]');
            if (verifiedField && verifiedField.value === 'No') {
                verifiedField.value = 'Yes';
            }
        }
    }

    async updateDashboardVerifiedCount() {
        try {
            // Get current verified accounts count
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            const verifiedCount = users.filter(user => user.isVerified || user.accountVerified).length;
            
            // Update the dashboard stat card
            const verifiedElement = document.getElementById('verifiedAccountsFacilitator');
            if (verifiedElement) {
                verifiedElement.textContent = verifiedCount;
                
                // Add a subtle animation to highlight the update
                verifiedElement.style.transform = 'scale(1.1)';
                verifiedElement.style.color = '#28a745';
                setTimeout(() => {
                    verifiedElement.style.transform = 'scale(1)';
                    verifiedElement.style.color = '';
                }, 500);
            }
        } catch (error) {
            console.error('Error updating dashboard verified count:', error);
        }
    }

    async changeProfileImage(seniorId) {
        try {
            // Create file input for image selection
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/*';
            input.style.display = 'none';
            
            input.onchange = async (e) => {
                const file = e.target.files[0];
                if (file) {
                    try {
                        this.showLoading('Uploading new profile image...');
                        
                        // Upload the new image
                        const imageUrl = await this.uploadProfileImage(file);
                        
                        // Update the senior's profile image
                        await FirebaseUtils.updateDoc(COLLECTIONS.USERS, seniorId, {
                            profileImageUrl: imageUrl,
                            updatedAt: FirebaseUtils.getTimestamp()
                        });
                        
                        this.showSuccess('Profile image updated successfully!');
                        
                        // Update the modal display
                        const avatarImg = document.querySelector('.senior-avatar-large img');
                        if (avatarImg) {
                            avatarImg.src = imageUrl;
                        }
                        
                        // Refresh the seniors list
                        this.loadSeniors();
                        
                    } catch (error) {
                        console.error('Error updating profile image:', error);
                        this.showError('Failed to update profile image. Please try again.');
                    } finally {
                        this.hideLoading();
                    }
                }
            };
            
            // Trigger file selection
            document.body.appendChild(input);
            input.click();
            document.body.removeChild(input);
            
        } catch (error) {
            console.error('Error changing profile image:', error);
            this.showError('Failed to change profile image. Please try again.');
        }
    }

    /**
     * Generate PDF report for a specific senior
     */
    generateSeniorReport(seniorId) {
        const senior = this.seniors.find(s => s.id === seniorId);
        if (!senior) {
            showNotification('Senior not found', 'error');
            return;
        }

        // Show report generation modal with options
        this.openReportOptionsModal(senior);
    }

    /**
     * Open report generation options modal
     */
    openReportOptionsModal(senior) {
        const modalTitle = `Generate Report - ${senior.firstName} ${senior.lastName}`;
        const modalBody = this.createReportOptionsForm(senior);
        
        openModal(modalTitle, modalBody, () => {
            this.initializeReportOptions(senior);
        });
    }

    /**
     * Create report options form
     */
    createReportOptionsForm(senior) {
        return `
            <div class="report-options-form">
                <div class="senior-info-header">
                    <div class="senior-avatar-section">
                        <div class="senior-avatar-small">
                            ${senior.profileImageUrl ? 
                                `<img src="${senior.profileImageUrl}" alt="${senior.firstName}">` :
                                `<div class="avatar-placeholder-small">
                                    <i class="fas fa-user"></i>
                                </div>`
                            }
                        </div>
                        <div class="senior-info-text">
                            <h4>${senior.firstName} ${senior.lastName}</h4>
                            <p>Age: ${this.calculateAge(senior.dateOfBirth)} years • ${senior.status || 'Active'}</p>
                        </div>
                    </div>
                </div>

                <div class="report-options-grid">
                    <div class="option-card" onclick="seniorsManager.selectReportType('comprehensive', '${senior.id}')">
                        <div class="option-icon comprehensive">
                            <i class="fas fa-file-alt"></i>
                        </div>
                        <h4>Comprehensive Report</h4>
                        <p>Complete profile with all information including health, benefits, and emergency contacts</p>
                        <div class="option-features">
                            <span class="feature-tag">Personal Info</span>
                            <span class="feature-tag">Health Records</span>
                            <span class="feature-tag">Benefits</span>
                            <span class="feature-tag">Emergency Contacts</span>
                        </div>
                    </div>

                    <div class="option-card" onclick="seniorsManager.selectReportType('summary', '${senior.id}')">
                        <div class="option-icon summary">
                            <i class="fas fa-chart-pie"></i>
                        </div>
                        <h4>Summary Report</h4>
                        <p>Brief overview with key information and statistics</p>
                        <div class="option-features">
                            <span class="feature-tag">Basic Info</span>
                            <span class="feature-tag">Contact Details</span>
                            <span class="feature-tag">Status Summary</span>
                        </div>
                    </div>

                    <div class="option-card" onclick="seniorsManager.selectReportType('health', '${senior.id}')">
                        <div class="option-icon health">
                            <i class="fas fa-heartbeat"></i>
                        </div>
                        <h4>Health Report</h4>
                        <p>Medical information, health conditions, and health-related data</p>
                        <div class="option-features">
                            <span class="feature-tag">Medical Conditions</span>
                            <span class="feature-tag">Medications</span>
                            <span class="feature-tag">Allergies</span>
                            <span class="feature-tag">Blood Type</span>
                        </div>
                    </div>

                    <div class="option-card" onclick="seniorsManager.selectReportType('benefits', '${senior.id}')">
                        <div class="option-icon benefits">
                            <i class="fas fa-gift"></i>
                        </div>
                        <h4>Benefits Report</h4>
                        <p>Government benefits, assistance programs, and financial support information</p>
                        <div class="option-features">
                            <span class="feature-tag">Government Benefits</span>
                            <span class="feature-tag">Pension Info</span>
                            <span class="feature-tag">Assistance Programs</span>
                        </div>
                    </div>
                </div>

                <div class="report-settings">
                    <h4><i class="fas fa-cog"></i> Report Settings</h4>
                    <div class="settings-grid">
                        <div class="setting-item">
                            <label>
                                <input type="checkbox" id="includePhoto" checked>
                                <span class="checkmark"></span>
                                Include Profile Photo
                            </label>
                        </div>
                        <div class="setting-item">
                            <label>
                                <input type="checkbox" id="includeCharts" checked>
                                <span class="checkmark"></span>
                                Include Charts & Graphs
                            </label>
                        </div>
                        <div class="setting-item">
                            <label>
                                <input type="checkbox" id="includeTimestamps" checked>
                                <span class="checkmark"></span>
                                Include Timestamps
                            </label>
                        </div>
                        <div class="setting-item">
                            <label>
                                <input type="checkbox" id="includeEmergencyContacts" checked>
                                <span class="checkmark"></span>
                                Include Emergency Contacts
                            </label>
                        </div>
                    </div>
                </div>

                <div class="report-preview-section">
                    <h4><i class="fas fa-eye"></i> Report Preview</h4>
                    <div id="reportPreviewContent" class="preview-content">
                        <div class="preview-placeholder">
                            <i class="fas fa-file-pdf"></i>
                            <p>Select a report type to see preview</p>
                        </div>
                    </div>
                </div>

                <div class="report-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">
                        <i class="fas fa-times"></i> Cancel
                    </button>
                    <button class="btn btn-primary" id="generateReportBtn" onclick="seniorsManager.generateSelectedReport('${senior.id}')" disabled>
                        <i class="fas fa-file-pdf"></i> Generate Report
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Initialize report options
     */
    initializeReportOptions(senior) {
        this.selectedReportType = null;
        this.currentSenior = senior;
        
        // Add event listeners for settings
        const settings = ['includePhoto', 'includeCharts', 'includeTimestamps', 'includeEmergencyContacts'];
        settings.forEach(setting => {
            const element = document.getElementById(setting);
            if (element) {
                element.addEventListener('change', () => {
                    this.updateReportPreview();
                });
            }
        });
    }

    /**
     * Select report type
     */
    selectReportType(type, seniorId) {
        this.selectedReportType = type;
        this.currentSenior = this.seniors.find(s => s.id === seniorId);
        
        // Update UI
        document.querySelectorAll('.option-card').forEach(card => {
            card.classList.remove('selected');
        });
        event.currentTarget.classList.add('selected');
        
        // Enable generate button
        document.getElementById('generateReportBtn').disabled = false;
        
        // Update preview
        this.updateReportPreview();
    }

    /**
     * Update report preview
     */
    updateReportPreview() {
        const previewContainer = document.getElementById('reportPreviewContent');
        if (!previewContainer || !this.selectedReportType || !this.currentSenior) return;

        const senior = this.currentSenior;
        const includePhoto = document.getElementById('includePhoto')?.checked || false;
        const includeCharts = document.getElementById('includeCharts')?.checked || false;
        const includeTimestamps = document.getElementById('includeTimestamps')?.checked || false;
        const includeEmergencyContacts = document.getElementById('includeEmergencyContacts')?.checked || false;

        let previewContent = '';
        
        switch (this.selectedReportType) {
            case 'comprehensive':
                previewContent = this.generateComprehensivePreview(senior, includePhoto, includeCharts, includeTimestamps, includeEmergencyContacts);
                break;
            case 'summary':
                previewContent = this.generateSummaryPreview(senior, includePhoto, includeCharts, includeTimestamps);
                break;
            case 'health':
                previewContent = this.generateHealthPreview(senior, includePhoto, includeCharts, includeTimestamps);
                break;
            case 'benefits':
                previewContent = this.generateBenefitsPreview(senior, includePhoto, includeCharts, includeTimestamps);
                break;
        }

        previewContainer.innerHTML = previewContent;
    }

    /**
     * Generate comprehensive report preview
     */
    generateComprehensivePreview(senior, includePhoto, includeCharts, includeTimestamps, includeEmergencyContacts) {
        return `
            <div class="preview-report">
                <div class="preview-header">
                    <h5>Comprehensive Senior Report</h5>
                    <p>Complete profile for ${senior.firstName} ${senior.lastName}</p>
                </div>
                <div class="preview-sections">
                    <div class="preview-section">
                        <h6><i class="fas fa-user"></i> Personal Information</h6>
                        <ul>
                            <li>Name: ${senior.firstName} ${senior.lastName}</li>
                            <li>Age: ${this.calculateAge(senior.dateOfBirth)} years</li>
                            <li>Gender: ${senior.gender || 'N/A'}</li>
                            <li>Status: ${senior.status || 'Active'}</li>
                        </ul>
                    </div>
                    <div class="preview-section">
                        <h6><i class="fas fa-phone"></i> Contact Information</h6>
                        <ul>
                            <li>Phone: ${senior.phoneNumber || 'N/A'}</li>
                            <li>Email: ${senior.email || 'N/A'}</li>
                            <li>Address: ${senior.address || 'N/A'}</li>
                        </ul>
                    </div>
                    ${includeEmergencyContacts ? `
                    <div class="preview-section">
                        <h6><i class="fas fa-exclamation-triangle"></i> Emergency Contacts</h6>
                        <ul>
                            <li>${senior.emergencyContacts?.length || 0} emergency contacts</li>
                        </ul>
                    </div>
                    ` : ''}
                    <div class="preview-section">
                        <h6><i class="fas fa-heartbeat"></i> Health Information</h6>
                        <ul>
                            <li>Medical Conditions: ${senior.medicalConditions || 'None'}</li>
                            <li>Medications: ${senior.medications || 'None'}</li>
                            <li>Allergies: ${senior.allergies || 'None'}</li>
                        </ul>
                    </div>
                    <div class="preview-section">
                        <h6><i class="fas fa-gift"></i> Benefits & Assistance</h6>
                        <ul>
                            <li>Government Benefits: ${senior.governmentBenefits || 'None'}</li>
                            <li>Pension: ${senior.pension || 'N/A'}</li>
                        </ul>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Generate summary report preview
     */
    generateSummaryPreview(senior, includePhoto, includeCharts, includeTimestamps) {
        return `
            <div class="preview-report">
                <div class="preview-header">
                    <h5>Summary Report</h5>
                    <p>Brief overview for ${senior.firstName} ${senior.lastName}</p>
                </div>
                <div class="preview-sections">
                    <div class="preview-section">
                        <h6><i class="fas fa-info-circle"></i> Key Information</h6>
                        <ul>
                            <li>Name: ${senior.firstName} ${senior.lastName}</li>
                            <li>Age: ${this.calculateAge(senior.dateOfBirth)} years</li>
                            <li>Status: ${senior.status || 'Active'}</li>
                            <li>Verified: ${senior.isVerified ? 'Yes' : 'No'}</li>
                        </ul>
                    </div>
                    <div class="preview-section">
                        <h6><i class="fas fa-phone"></i> Contact Details</h6>
                        <ul>
                            <li>Phone: ${senior.phoneNumber || 'N/A'}</li>
                            <li>Address: ${senior.address || 'N/A'}</li>
                        </ul>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Generate health report preview
     */
    generateHealthPreview(senior, includePhoto, includeCharts, includeTimestamps) {
        return `
            <div class="preview-report">
                <div class="preview-header">
                    <h5>Health Report</h5>
                    <p>Medical information for ${senior.firstName} ${senior.lastName}</p>
                </div>
                <div class="preview-sections">
                    <div class="preview-section">
                        <h6><i class="fas fa-heartbeat"></i> Health Status</h6>
                        <ul>
                            <li>Medical Conditions: ${senior.medicalConditions || 'None reported'}</li>
                            <li>Medications: ${senior.medications || 'None reported'}</li>
                            <li>Allergies: ${senior.allergies || 'None reported'}</li>
                            <li>Blood Type: ${senior.bloodType || 'N/A'}</li>
                        </ul>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Generate benefits report preview
     */
    generateBenefitsPreview(senior, includePhoto, includeCharts, includeTimestamps) {
        return `
            <div class="preview-report">
                <div class="preview-header">
                    <h5>Benefits Report</h5>
                    <p>Benefits and assistance for ${senior.firstName} ${senior.lastName}</p>
                </div>
                <div class="preview-sections">
                    <div class="preview-section">
                        <h6><i class="fas fa-gift"></i> Benefits Information</h6>
                        <ul>
                            <li>Government Benefits: ${senior.governmentBenefits || 'None reported'}</li>
                            <li>Pension: ${senior.pension || 'N/A'}</li>
                            <li>Other Assistance: ${senior.otherAssistance || 'None reported'}</li>
                        </ul>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Generate the selected report
     */
    generateSelectedReport(seniorId) {
        if (!this.selectedReportType || !this.currentSenior) {
            showNotification('Please select a report type', 'warning');
            return;
        }

        const senior = this.currentSenior;
        const includePhoto = document.getElementById('includePhoto')?.checked || false;
        const includeCharts = document.getElementById('includeCharts')?.checked || false;
        const includeTimestamps = document.getElementById('includeTimestamps')?.checked || false;
        const includeEmergencyContacts = document.getElementById('includeEmergencyContacts')?.checked || false;

        showNotification('Generating PDF report...', 'info');
        
        // Add a small delay to ensure UI updates
        setTimeout(() => {
            try {
                // Check if jsPDF is available
                if (typeof window.jspdf !== 'undefined') {
                    // Generate PDF using jsPDF
                    this.generatePDFReport(senior, this.selectedReportType, {
                        includePhoto,
                        includeCharts,
                        includeTimestamps,
                        includeEmergencyContacts
                    });
                    
                    showNotification('PDF report generated successfully!', 'success');
                } else {
                    // Fallback to text file if jsPDF is not available
                    console.warn('jsPDF not available, generating text file instead');
                    this.generateTextReport(senior, this.selectedReportType, {
                        includePhoto,
                        includeCharts,
                        includeTimestamps,
                        includeEmergencyContacts
                    });
                    
                    showNotification('Text report generated successfully!', 'success');
                }
                
                closeModal();
            } catch (error) {
                console.error('Error generating report:', error);
                showNotification('Error generating report. Please try again.', 'error');
            }
        }, 500);
    }

    /**
     * Generate PDF report using jsPDF
     */
    generatePDFReport(senior, reportType, options) {
        // Check if jsPDF is available
        if (typeof window.jspdf === 'undefined') {
            console.error('jsPDF library not loaded');
            showNotification('PDF library not loaded. Please refresh the page.', 'error');
            return;
        }

        try {
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF();
            
            // Set up the document
            const pageWidth = doc.internal.pageSize.getWidth();
            const pageHeight = doc.internal.pageSize.getHeight();
            let yPosition = 20;
            const lineHeight = 7;
            const margin = 20;
        
        // Helper function to add text with word wrapping
        const addText = (text, x = margin, y = yPosition, options = {}) => {
            const textOptions = {
                fontSize: options.fontSize || 10,
                fontStyle: options.fontStyle || 'normal',
                color: options.color || [0, 0, 0],
                align: options.align || 'left'
            };
            
            doc.setFontSize(textOptions.fontSize);
            doc.setTextColor(textOptions.color[0], textOptions.color[1], textOptions.color[2]);
            
            if (textOptions.fontStyle === 'bold') {
                doc.setFont(undefined, 'bold');
            } else {
                doc.setFont(undefined, 'normal');
            }
            
            const lines = doc.splitTextToSize(text, pageWidth - 2 * margin);
            doc.text(lines, x, y);
            
            return y + (lines.length * textOptions.fontSize * 0.4);
        };
        
        // Helper function to add a new page if needed
        const checkNewPage = (requiredSpace = 20) => {
            if (yPosition + requiredSpace > pageHeight - margin) {
                doc.addPage();
                yPosition = 20;
                return true;
            }
            return false;
        };
        
        // Add header
        addText('SENIOR CITIZEN REPORT', margin, yPosition, { 
            fontSize: 18, 
            fontStyle: 'bold', 
            color: [0, 123, 255] 
        });
        yPosition += 10;
        
        addText(`Report Type: ${reportType.toUpperCase()}`, margin, yPosition, { 
            fontSize: 12, 
            fontStyle: 'bold' 
        });
        yPosition += 5;
        
        addText(`Generated on: ${new Date().toLocaleDateString()}`, margin, yPosition, { 
            fontSize: 10 
        });
        yPosition += 15;
        
        // Add senior information based on report type
        switch (reportType) {
            case 'comprehensive':
                yPosition = this.addComprehensiveContent(doc, senior, addText, checkNewPage, yPosition, options);
                break;
            case 'summary':
                yPosition = this.addSummaryContent(doc, senior, addText, checkNewPage, yPosition, options);
                break;
            case 'health':
                yPosition = this.addHealthContent(doc, senior, addText, checkNewPage, yPosition, options);
                break;
            case 'benefits':
                yPosition = this.addBenefitsContent(doc, senior, addText, checkNewPage, yPosition, options);
                break;
        }
        
        // Add footer
        checkNewPage(30);
        yPosition += 10;
        addText('Report generated by SeniorHub Admin Dashboard', margin, yPosition, { 
            fontSize: 8, 
            color: [128, 128, 128] 
        });
        yPosition += 5;
        addText(`Date: ${new Date().toLocaleDateString()}`, margin, yPosition, { 
            fontSize: 8, 
            color: [128, 128, 128] 
        });
        
            // Save the PDF
            const fileName = `${reportType}_report_${senior.firstName}_${senior.lastName}_${new Date().toISOString().split('T')[0]}.pdf`;
            doc.save(fileName);
            
        } catch (error) {
            console.error('Error in PDF generation:', error);
            showNotification('Error generating PDF: ' + error.message, 'error');
        }
    }

    /**
     * Add comprehensive report content
     */
    addComprehensiveContent(doc, senior, addText, checkNewPage, yPosition, options) {
        const age = this.calculateAge(senior.dateOfBirth);
        
        // Personal Information
        checkNewPage(30);
        addText('PERSONAL INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Name: ${senior.firstName} ${senior.lastName}`, 20, yPosition);
        yPosition += 6;
        addText(`Age: ${age} years old`, 20, yPosition);
        yPosition += 6;
        addText(`Date of Birth: ${senior.dateOfBirth ? new Date(senior.dateOfBirth).toLocaleDateString() : 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Gender: ${senior.gender || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Status: ${senior.status || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Verified: ${senior.isVerified ? 'Yes' : 'No'}`, 20, yPosition);
        yPosition += 15;
        
        // Contact Information
        checkNewPage(30);
        addText('CONTACT INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Phone: ${senior.phoneNumber || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Email: ${senior.email || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Address: ${senior.address || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`City: ${senior.city || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Province: ${senior.province || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Postal Code: ${senior.postalCode || 'N/A'}`, 20, yPosition);
        yPosition += 15;
        
        // Emergency Contacts
        if (options.includeEmergencyContacts) {
            checkNewPage(30);
            addText('EMERGENCY CONTACTS', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
            yPosition += 10;
            
            if (senior.emergencyContacts && senior.emergencyContacts.length > 0) {
                senior.emergencyContacts.forEach((contact, index) => {
                    addText(`Contact ${index + 1}:`, 20, yPosition, { fontStyle: 'bold' });
                    yPosition += 6;
                    addText(`  Name: ${contact.name || 'N/A'}`, 20, yPosition);
                    yPosition += 6;
                    addText(`  Relationship: ${contact.relationship || 'N/A'}`, 20, yPosition);
                    yPosition += 6;
                    addText(`  Phone: ${contact.phone || 'N/A'}`, 20, yPosition);
                    yPosition += 6;
                    addText(`  Email: ${contact.email || 'N/A'}`, 20, yPosition);
                    yPosition += 10;
                });
            } else {
                addText('No emergency contacts on file', 20, yPosition);
                yPosition += 6;
            }
        }
        
        // Health Information
        checkNewPage(30);
        addText('HEALTH INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Medical Conditions: ${senior.medicalConditions || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Medications: ${senior.medications || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Allergies: ${senior.allergies || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Blood Type: ${senior.bloodType || 'N/A'}`, 20, yPosition);
        yPosition += 15;
        
        // Benefits & Assistance
        checkNewPage(30);
        addText('BENEFITS & ASSISTANCE', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Government Benefits: ${senior.governmentBenefits || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Pension: ${senior.pension || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Other Assistance: ${senior.otherAssistance || 'None reported'}`, 20, yPosition);
        yPosition += 15;
        
        // Additional Information
        if (options.includeTimestamps) {
            checkNewPage(30);
            addText('ADDITIONAL INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
            yPosition += 10;
            
            addText(`Member Since: ${senior.createdAt ? new Date(senior.createdAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 6;
            addText(`Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 6;
            addText(`Notes: ${senior.notes || 'No additional notes'}`, 20, yPosition);
            yPosition += 15;
        }
        
        return yPosition;
    }

    /**
     * Add summary report content
     */
    addSummaryContent(doc, senior, addText, checkNewPage, yPosition, options) {
        const age = this.calculateAge(senior.dateOfBirth);
        
        // Summary Information
        checkNewPage(30);
        addText('SUMMARY INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Name: ${senior.firstName} ${senior.lastName}`, 20, yPosition);
        yPosition += 6;
        addText(`Age: ${age} years old`, 20, yPosition);
        yPosition += 6;
        addText(`Status: ${senior.status || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Verified: ${senior.isVerified ? 'Yes' : 'No'}`, 20, yPosition);
        yPosition += 15;
        
        // Contact Details
        checkNewPage(30);
        addText('CONTACT DETAILS', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Phone: ${senior.phoneNumber || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Email: ${senior.email || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Address: ${senior.address || 'N/A'}`, 20, yPosition);
        yPosition += 15;
        
        // Membership Information
        if (options.includeTimestamps) {
            checkNewPage(30);
            addText('MEMBERSHIP INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
            yPosition += 10;
            
            addText(`Member Since: ${senior.createdAt ? new Date(senior.createdAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 6;
            addText(`Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 15;
        }
        
        return yPosition;
    }

    /**
     * Add health report content
     */
    addHealthContent(doc, senior, addText, checkNewPage, yPosition, options) {
        const age = this.calculateAge(senior.dateOfBirth);
        
        // Health Information
        checkNewPage(30);
        addText('HEALTH INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Name: ${senior.firstName} ${senior.lastName}`, 20, yPosition);
        yPosition += 6;
        addText(`Age: ${age} years old`, 20, yPosition);
        yPosition += 15;
        
        // Medical Conditions
        checkNewPage(30);
        addText('MEDICAL CONDITIONS', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Medical Conditions: ${senior.medicalConditions || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Medications: ${senior.medications || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Allergies: ${senior.allergies || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Blood Type: ${senior.bloodType || 'N/A'}`, 20, yPosition);
        yPosition += 15;
        
        // Record Information
        if (options.includeTimestamps) {
            checkNewPage(30);
            addText('RECORD INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
            yPosition += 10;
            
            addText(`Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 15;
        }
        
        return yPosition;
    }

    /**
     * Add benefits report content
     */
    addBenefitsContent(doc, senior, addText, checkNewPage, yPosition, options) {
        const age = this.calculateAge(senior.dateOfBirth);
        
        // Benefits Information
        checkNewPage(30);
        addText('BENEFITS INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Name: ${senior.firstName} ${senior.lastName}`, 20, yPosition);
        yPosition += 6;
        addText(`Age: ${age} years old`, 20, yPosition);
        yPosition += 15;
        
        // Government Benefits
        checkNewPage(30);
        addText('GOVERNMENT BENEFITS', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
        yPosition += 10;
        
        addText(`Government Benefits: ${senior.governmentBenefits || 'None reported'}`, 20, yPosition);
        yPosition += 6;
        addText(`Pension: ${senior.pension || 'N/A'}`, 20, yPosition);
        yPosition += 6;
        addText(`Other Assistance: ${senior.otherAssistance || 'None reported'}`, 20, yPosition);
        yPosition += 15;
        
        // Record Information
        if (options.includeTimestamps) {
            checkNewPage(30);
            addText('RECORD INFORMATION', 20, yPosition, { fontSize: 14, fontStyle: 'bold', color: [0, 123, 255] });
            yPosition += 10;
            
            addText(`Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}`, 20, yPosition);
            yPosition += 15;
        }
        
        return yPosition;
    }

    /**
     * Generate text report as fallback
     */
    generateTextReport(senior, reportType, options) {
        const reportContent = this.generateReportContent(senior, reportType, options);
        
        // Create and download the text file
        const blob = new Blob([reportContent], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${reportType}_report_${senior.firstName}_${senior.lastName}_${new Date().toISOString().split('T')[0]}.txt`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    /**
     * Generate report content based on type and options
     */
    generateReportContent(senior, reportType, options) {
        const reportDate = new Date().toLocaleDateString();
        const age = this.calculateAge(senior.dateOfBirth);
        
        let content = `
            SENIOR CITIZEN ${reportType.toUpperCase()} REPORT
            Generated on: ${reportDate}
            ===========================================
        `;

        switch (reportType) {
            case 'comprehensive':
                content += this.generateComprehensiveContent(senior, age, options);
                break;
            case 'summary':
                content += this.generateSummaryContent(senior, age, options);
                break;
            case 'health':
                content += this.generateHealthContent(senior, age, options);
                break;
            case 'benefits':
                content += this.generateBenefitsContent(senior, age, options);
                break;
        }

        return content;
    }

    /**
     * Generate comprehensive report content
     */
    generateComprehensiveContent(senior, age, options) {
        return `
            PERSONAL INFORMATION
            ===========================================
            Name: ${senior.firstName} ${senior.lastName}
            Age: ${age} years old
            Date of Birth: ${senior.dateOfBirth ? new Date(senior.dateOfBirth).toLocaleDateString() : 'N/A'}
            Gender: ${senior.gender || 'N/A'}
            Status: ${senior.status || 'N/A'}
            Verified: ${senior.isVerified ? 'Yes' : 'No'}
            
            CONTACT INFORMATION
            ===========================================
            Phone: ${senior.phoneNumber || 'N/A'}
            Email: ${senior.email || 'N/A'}
            Address: ${senior.address || 'N/A'}
            City: ${senior.city || 'N/A'}
            Province: ${senior.province || 'N/A'}
            Postal Code: ${senior.postalCode || 'N/A'}
            
            ${options.includeEmergencyContacts ? `
            EMERGENCY CONTACTS
            ===========================================
            ${senior.emergencyContacts && senior.emergencyContacts.length > 0 ? 
                senior.emergencyContacts.map((contact, index) => `
            Contact ${index + 1}:
            - Name: ${contact.name || 'N/A'}
            - Relationship: ${contact.relationship || 'N/A'}
            - Phone: ${contact.phone || 'N/A'}
            - Email: ${contact.email || 'N/A'}
                `).join('\n') : 
                'No emergency contacts on file'
            }
            ` : ''}
            
            HEALTH INFORMATION
            ===========================================
            Medical Conditions: ${senior.medicalConditions || 'None reported'}
            Medications: ${senior.medications || 'None reported'}
            Allergies: ${senior.allergies || 'None reported'}
            Blood Type: ${senior.bloodType || 'N/A'}
            
            BENEFITS & ASSISTANCE
            ===========================================
            Government Benefits: ${senior.governmentBenefits || 'None reported'}
            Pension: ${senior.pension || 'N/A'}
            Other Assistance: ${senior.otherAssistance || 'None reported'}
            
            ${options.includeTimestamps ? `
            ADDITIONAL INFORMATION
            ===========================================
            Member Since: ${senior.createdAt ? new Date(senior.createdAt.toDate()).toLocaleDateString() : 'N/A'}
            Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}
            Notes: ${senior.notes || 'No additional notes'}
            ` : ''}
            
            REPORT SUMMARY
            ===========================================
            This comprehensive report contains detailed information about ${senior.firstName} ${senior.lastName}, 
            a ${age}-year-old senior citizen. The report includes personal details, contact information, 
            ${options.includeEmergencyContacts ? 'emergency contacts, ' : ''}health information, and benefits status.
            
            Report generated by SeniorHub Admin Dashboard
            Date: ${new Date().toLocaleDateString()}
        `;
    }

    /**
     * Generate summary report content
     */
    generateSummaryContent(senior, age, options) {
        return `
            SUMMARY INFORMATION
            ===========================================
            Name: ${senior.firstName} ${senior.lastName}
            Age: ${age} years old
            Status: ${senior.status || 'N/A'}
            Verified: ${senior.isVerified ? 'Yes' : 'No'}
            
            CONTACT DETAILS
            ===========================================
            Phone: ${senior.phoneNumber || 'N/A'}
            Email: ${senior.email || 'N/A'}
            Address: ${senior.address || 'N/A'}
            
            ${options.includeTimestamps ? `
            MEMBERSHIP INFORMATION
            ===========================================
            Member Since: ${senior.createdAt ? new Date(senior.createdAt.toDate()).toLocaleDateString() : 'N/A'}
            Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}
            ` : ''}
            
            SUMMARY
            ===========================================
            This summary report provides key information about ${senior.firstName} ${senior.lastName}, 
            a ${age}-year-old senior citizen with ${senior.status || 'active'} status.
            
            Report generated by SeniorHub Admin Dashboard
            Date: ${new Date().toLocaleDateString()}
        `;
    }

    /**
     * Generate health report content
     */
    generateHealthContent(senior, age, options) {
        return `
            HEALTH INFORMATION
            ===========================================
            Name: ${senior.firstName} ${senior.lastName}
            Age: ${age} years old
            
            MEDICAL CONDITIONS
            ===========================================
            Medical Conditions: ${senior.medicalConditions || 'None reported'}
            Medications: ${senior.medications || 'None reported'}
            Allergies: ${senior.allergies || 'None reported'}
            Blood Type: ${senior.bloodType || 'N/A'}
            
            ${options.includeTimestamps ? `
            RECORD INFORMATION
            ===========================================
            Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}
            ` : ''}
            
            HEALTH SUMMARY
            ===========================================
            This health report contains medical information for ${senior.firstName} ${senior.lastName}, 
            a ${age}-year-old senior citizen.
            
            Report generated by SeniorHub Admin Dashboard
            Date: ${new Date().toLocaleDateString()}
        `;
    }

    /**
     * Generate benefits report content
     */
    generateBenefitsContent(senior, age, options) {
        return `
            BENEFITS INFORMATION
            ===========================================
            Name: ${senior.firstName} ${senior.lastName}
            Age: ${age} years old
            
            GOVERNMENT BENEFITS
            ===========================================
            Government Benefits: ${senior.governmentBenefits || 'None reported'}
            Pension: ${senior.pension || 'N/A'}
            Other Assistance: ${senior.otherAssistance || 'None reported'}
            
            ${options.includeTimestamps ? `
            RECORD INFORMATION
            ===========================================
            Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}
            ` : ''}
            
            BENEFITS SUMMARY
            ===========================================
            This benefits report contains assistance and benefits information for ${senior.firstName} ${senior.lastName}, 
            a ${age}-year-old senior citizen.
            
            Report generated by SeniorHub Admin Dashboard
            Date: ${new Date().toLocaleDateString()}
        `;
    }

    /**
     * Test PDF generation with a simple test
     */
    testPDFGeneration() {
        console.log('Testing PDF generation...');
        
        // Check if jsPDF is available
        if (typeof window.jspdf === 'undefined') {
            console.error('jsPDF library not loaded');
            showNotification('jsPDF library not loaded. Please check the console.', 'error');
            return;
        }
        
        try {
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF();
            
            // Add simple test content
            doc.setFontSize(20);
            doc.text('Test PDF Generation', 20, 20);
            doc.setFontSize(12);
            doc.text('This is a test PDF generated by SeniorHub Admin Dashboard', 20, 40);
            doc.text(`Generated on: ${new Date().toLocaleDateString()}`, 20, 60);
            
            // Save the test PDF
            doc.save('test_pdf_generation.pdf');
            
            console.log('PDF generation test successful!');
            showNotification('PDF generation test successful! Check your downloads.', 'success');
            
        } catch (error) {
            console.error('PDF generation test failed:', error);
            showNotification('PDF generation test failed: ' + error.message, 'error');
        }
    }

    /**
     * Generate report content for a specific senior
     */
    generateSeniorReportContent(senior) {
        const reportDate = new Date().toLocaleDateString();
        const age = this.calculateAge(senior.dateOfBirth);
        
        return `
            SENIOR CITIZEN REPORT
            Generated on: ${reportDate}
            
            ===========================================
            PERSONAL INFORMATION
            ===========================================
            
            Name: ${senior.firstName} ${senior.lastName}
            Age: ${age} years old
            Date of Birth: ${senior.dateOfBirth ? new Date(senior.dateOfBirth).toLocaleDateString() : 'N/A'}
            Gender: ${senior.gender || 'N/A'}
            Status: ${senior.status || 'N/A'}
            Verified: ${senior.isVerified ? 'Yes' : 'No'}
            
            ===========================================
            CONTACT INFORMATION
            ===========================================
            
            Phone: ${senior.phoneNumber || 'N/A'}
            Email: ${senior.email || 'N/A'}
            Address: ${senior.address || 'N/A'}
            City: ${senior.city || 'N/A'}
            Province: ${senior.province || 'N/A'}
            Postal Code: ${senior.postalCode || 'N/A'}
            
            ===========================================
            EMERGENCY CONTACTS
            ===========================================
            
            ${senior.emergencyContacts && senior.emergencyContacts.length > 0 ? 
                senior.emergencyContacts.map((contact, index) => `
            Contact ${index + 1}:
            - Name: ${contact.name || 'N/A'}
            - Relationship: ${contact.relationship || 'N/A'}
            - Phone: ${contact.phone || 'N/A'}
            - Email: ${contact.email || 'N/A'}
                `).join('\n') : 
                'No emergency contacts on file'
            }
            
            ===========================================
            HEALTH INFORMATION
            ===========================================
            
            Medical Conditions: ${senior.medicalConditions || 'None reported'}
            Medications: ${senior.medications || 'None reported'}
            Allergies: ${senior.allergies || 'None reported'}
            Blood Type: ${senior.bloodType || 'N/A'}
            
            ===========================================
            BENEFITS & ASSISTANCE
            ===========================================
            
            Government Benefits: ${senior.governmentBenefits || 'None reported'}
            Pension: ${senior.pension || 'N/A'}
            Other Assistance: ${senior.otherAssistance || 'None reported'}
            
            ===========================================
            ADDITIONAL INFORMATION
            ===========================================
            
            Member Since: ${senior.createdAt ? new Date(senior.createdAt.toDate()).toLocaleDateString() : 'N/A'}
            Last Updated: ${senior.updatedAt ? new Date(senior.updatedAt.toDate()).toLocaleDateString() : 'N/A'}
            Notes: ${senior.notes || 'No additional notes'}
            
            ===========================================
            REPORT SUMMARY
            ===========================================
            
            This report contains comprehensive information about ${senior.firstName} ${senior.lastName}, 
            a ${age}-year-old senior citizen. The report includes personal details, contact information, 
            emergency contacts, health information, and benefits status.
            
            Report generated by SeniorHub Admin Dashboard
            Date: ${reportDate}
        `;
    }
}

// Initialize Seniors Manager
const seniorsManager = new SeniorsManager();

// Debug function to test delete permissions
window.testDeletePermission = async function(seniorId) {
    if (!seniorId) {
        console.error('Please provide a senior ID');
        return;
    }
    
    console.log('Testing delete permission for senior ID:', seniorId);
    const result = await seniorsManager.testDeletePermission(seniorId);
    console.log('Permission test result:', result);
    return result;
};

// Export for use in other files
window.seniorsManager = seniorsManager;

// Export individual functions for global access
window.openAddSeniorModal = () => seniorsManager.openAddSeniorModal();
window.exportSeniorsData = () => seniorsManager.exportSeniorsData();
window.generateSeniorReport = (seniorId) => seniorsManager.generateSeniorReport(seniorId);
window.testPDFGeneration = () => seniorsManager.testPDFGeneration();
window.debugSeniorsDatabase = () => seniorsManager.debugDatabase();
window.debugSpecificUser = (userId) => seniorsManager.debugSpecificUser(userId);
window.debugAdminPermissions = () => seniorsManager.debugAdminPermissions();
window.fixUserRoles = () => seniorsManager.fixUserRoles();
window.createTestSenior = () => seniorsManager.createTestSenior();
window.testCloudinaryConnection = () => seniorsManager.testCloudinaryConnection();
window.testSimpleUpload = () => seniorsManager.testSimpleUpload();
