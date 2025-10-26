// Main Application Controller
class MainApp {
    constructor() {
        this.currentSection = 'dashboard';
        this.currentUser = null;
        this.currentUserRole = null;
        this.roleLoadRetries = 0;
        this.init();
    }

    init() {
        this.setupEventListeners();
        // Dark mode removed
        this.initializeApp();
    }

    setupEventListeners() {
        // Navigation menu
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = e.currentTarget.getAttribute('href').substring(1);
                this.showSection(section);
            });
        });

        // Sidebar toggle
        document.getElementById('sidebarToggle').addEventListener('click', () => {
            this.toggleSidebar();
        });

        // Modal close
        document.getElementById('modalOverlay').addEventListener('click', (e) => {
            if (e.target === e.currentTarget) {
                this.closeModal();
            }
        });

        // Logout button
        document.getElementById('logoutBtn').addEventListener('click', () => {
            this.handleLogout();
        });

        // Dark mode toggle removed

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            this.handleKeyboardShortcuts(e);
        });

        // Window resize
        window.addEventListener('resize', () => {
            this.handleWindowResize();
        });

        // Handle browser navigation (back/forward) via hash
        window.addEventListener('hashchange', () => {
            const sectionFromHash = (window.location.hash || '').replace('#', '') || 'dashboard';
            this.showSection(sectionFromHash);
        });
    }

    initializeApp() {
        this.updateLoadingStatus('Checking authentication...');

        // Rely solely on the auth state listener to allow session restoration
        let authResolved = false;
        const restorationGraceMs = 800; // shorter wait for Firebase to restore session

        auth.onAuthStateChanged((user) => {
            authResolved = true;
            if (user) {
                console.log('Auth state - user authenticated:', user.email);
                this.updateLoadingStatus('Loading user data...');
                this.currentUser = user;
                this.loadUserRole(user.uid);
            } else {
                console.log('Auth state - no authenticated user');
                this.updateLoadingStatus('Redirecting to login...');
                setTimeout(() => {
                    // If still no user after grace period, redirect to login
                    if (!auth.currentUser) {
                        window.location.href = 'login.html';
                    }
                }, 500);
            }
        });

        // Fallback: if listener hasn't fired, give time for restoration
        setTimeout(() => {
            if (!authResolved && auth.currentUser) {
                // Session restored but callback delayed; proceed
                this.currentUser = auth.currentUser;
                this.loadUserRole(this.currentUser.uid);
            } else if (!authResolved && !auth.currentUser) {
                // No session after grace period, redirect
                this.updateLoadingStatus('Redirecting to login...');
                window.location.href = 'login.html';
            }
        }, restorationGraceMs);
    }

    updateLoadingStatus(message) {
        const statusElement = document.getElementById('loadingStatus');
        if (statusElement) {
            statusElement.textContent = message;
        }
    }

    hideLoadingScreen() {
        const loadingScreen = document.getElementById('loadingScreen');
        if (loadingScreen) {
            loadingScreen.classList.add('hidden');
        }
    }

    showRoleWelcomeMessage(role, userName) {
        const roleMessages = {
            'super_admin': {
                title: 'Welcome, Super Administrator!',
                message: `Hello ${userName}! You have full system access including admin management capabilities.`,
                icon: 'fas fa-crown',
                color: '#ff9800'
            },
            'facilitator': {
                title: 'Welcome, Facilitator!',
                message: `Hello ${userName}! You can manage seniors, health records, and appointments.`,
                icon: 'fas fa-user-tie',
                color: '#4caf50'
            },
            'admin': {
                title: 'Welcome, Administrator!',
                message: `Hello ${userName}! You have administrative access to the system.`,
                icon: 'fas fa-user-shield',
                color: '#2196f3'
            }
        };

        const message = roleMessages[role] || roleMessages['facilitator'];
        
        // Create welcome notification
        const notification = document.createElement('div');
        notification.className = 'welcome-notification';
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: white;
            border-left: 4px solid ${message.color};
            padding: 16px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 1000;
            max-width: 400px;
            animation: slideInRight 0.3s ease-out;
        `;
        
        notification.innerHTML = `
            <div style="display: flex; align-items: center; gap: 12px;">
                <i class="${message.icon}" style="color: ${message.color}; font-size: 24px;"></i>
                <div>
                    <h4 style="margin: 0 0 4px 0; color: #333; font-size: 16px;">${message.title}</h4>
                    <p style="margin: 0; color: #666; font-size: 14px;">${message.message}</p>
                </div>
                <button onclick="this.parentElement.parentElement.remove()" style="background: none; border: none; color: #999; cursor: pointer; font-size: 18px; margin-left: auto;">&times;</button>
            </div>
        `;
        
        document.body.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.style.animation = 'slideOutRight 0.3s ease-in';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    async createAdminUserRecord(uid) {
        try {
            const user = auth.currentUser;
            if (!user) {
                console.error('No authenticated user found');
                this.updateLoadingStatus('No authenticated user found. Please log in again.');
                return;
            }

            // Check if admin user already exists
            const existingDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).get();
            if (existingDoc.exists) {
                console.log('Admin user already exists, skipping creation');
                this.updateLoadingStatus('Admin profile found. Loading dashboard...');
                setTimeout(() => {
                    this.loadUserRole(uid);
                }, 1000);
                return;
            }

            console.log('Creating admin user record for:', user.email, 'UID:', uid);
            
            // All admin users get the same role
            const defaultRole = 'admin';
            console.log('Setting role to admin for all admin users');
            
            // Create admin user record with determined role
            const adminUserData = {
                uid: uid,
                email: user.email,
                name: user.displayName || user.email.split('@')[0],
                role: defaultRole,
                department: 'Administration',
                permissions: this.getRolePermissions(defaultRole),
                createdAt: FirebaseUtils.getTimestamp(),
                lastLogin: FirebaseUtils.getTimestamp(),
                isActive: true,
                createdBy: 'system',
                notes: `Auto-created ${defaultRole} account`
            };

            // Save to admin_users collection with retry mechanism
            let retryCount = 0;
            const maxRetries = 3;
            
            while (retryCount < maxRetries) {
                try {
                    await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).set(adminUserData);
                    console.log('Admin user record created successfully');
                    this.updateLoadingStatus(`${defaultRole} profile created successfully. Loading dashboard...`);
                    
                    // Retry loading the user role
                    setTimeout(() => {
                        this.loadUserRole(uid);
                    }, 1000);
                    return; // Success, exit the retry loop
                    
                } catch (createError) {
                    retryCount++;
                    console.warn(`Admin user creation attempt ${retryCount} failed:`, createError);
                    
                    if (retryCount < maxRetries) {
                        this.updateLoadingStatus(`Creating facilitator profile... (Attempt ${retryCount + 1}/${maxRetries})`);
                        // Wait before retrying
                        await new Promise(resolve => setTimeout(resolve, 1000 * retryCount));
                    } else {
                        throw createError; // Re-throw the error if all retries failed
                    }
                }
            }
            
        } catch (error) {
            console.error('Failed to create admin user record:', error);
            
            // Try fallback to Realtime Database
            try {
                console.log('Attempting fallback to Realtime Database...');
                this.updateLoadingStatus('Creating facilitator profile via fallback method...');
                
                const rtdb = firebase.database();
                const adminUserRef = rtdb.ref(`admin_users/${uid}`);
                
                await adminUserRef.set(adminUserData);
                
                console.log('Admin user record created in Realtime Database');
                this.updateLoadingStatus('Facilitator profile created successfully. Loading dashboard...');
                
                // Retry loading the user role
                setTimeout(() => {
                    this.loadUserRole(uid);
                }, 1000);
                
            } catch (fallbackError) {
                console.error('Fallback creation also failed:', fallbackError);
                this.updateLoadingStatus(`Failed to create admin profile: ${error.message}`);
                
                // Show retry button as final fallback
                const container = document.getElementById('loadingScreen');
                if (container && !container.querySelector('.retry-btn')) {
                    const btn = document.createElement('button');
                    btn.className = 'btn btn-primary retry-btn';
                    btn.style.marginTop = '16px';
                    btn.innerHTML = '<i class="fas fa-redo"></i> Retry';
                    btn.onclick = () => this.createAdminUserRecord(uid);
                    container.appendChild(btn);
                }
            }
        }
    }

    async loadUserRole(uid) {
        try {
            this.updateLoadingStatus('Verifying admin permissions...');
            
            // Check if role is stored in session (from login)
            const sessionRole = sessionStorage.getItem('userRole');
            const sessionEmail = sessionStorage.getItem('userEmail');
            const sessionName = sessionStorage.getItem('userName');
            
            if (sessionRole && sessionEmail && sessionName) {
                console.log('Using role from session storage:', sessionRole);
                this.currentUserRole = sessionRole;
                this.currentUserRoleLabel = this.formatRoleDisplay(sessionRole);
                
                // Apply role-based visibility immediately
                this.applyRoleBasedVisibility(sessionRole);
                
                // Update user display
                this.updateUserInfo();
                
                // Show role-specific welcome message
                this.showRoleWelcomeMessage(sessionRole, sessionName);
                
                // Clear session storage
                sessionStorage.removeItem('userRole');
                sessionStorage.removeItem('userEmail');
                sessionStorage.removeItem('userName');
                
                this.updateLoadingStatus('Loading dashboard...');
                setTimeout(() => {
                    this.hideLoadingScreen();
                }, 1000);
                return;
            }
            
            // Try Firestore first
            let doc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).get();
            
            // If not found in Firestore, try Realtime Database
            if (!doc.exists) {
                console.log('Admin user not found in Firestore, checking Realtime Database...');
                try {
                    const rtdb = firebase.database();
                    const rtdbSnapshot = await rtdb.ref(`admin_users/${uid}`).once('value');
                    
                    if (rtdbSnapshot.exists()) {
                        console.log('Found admin user in Realtime Database, migrating to Firestore...');
                        const rtdbData = rtdbSnapshot.val();
                        
                        // Migrate to Firestore
                        await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).set(rtdbData);
                        doc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).get();
                    }
                } catch (rtdbError) {
                    console.warn('Realtime Database check failed:', rtdbError);
                }
            }
            if (doc.exists) {
                const userData = doc.data();
                const rawRole = (userData.role || '').toString();
                // Normalize role to match ADMIN_ROLES keys (lowercase, spaces to underscores)
                this.currentUserRole = rawRole.trim().toLowerCase().replace(/\s+/g, '_');
                this.currentUserRoleLabel = rawRole || 'Admin';
                console.log('User role loaded:', {
                    rawRole: rawRole,
                    normalizedRole: this.currentUserRole,
                    roleLabel: this.currentUserRoleLabel,
                    userData: userData
                });
                const initialSection = (window.location.hash || '').replace('#', '') || 'dashboard';
                this.updateLoadingStatus('Loading...');
                this.setupRoleBasedUI();
                this.showSection(initialSection);
                this.hideLoading();
                this.showMainApp();
                this.roleLoadRetries = 0;
            } else {
                console.warn('Admin doc not found by UID. Attempting email fallback...');
                const email = this.currentUser?.email || '';
                const q = await db.collection(COLLECTIONS.ADMIN_USERS).where('email', '==', email).limit(1).get();
                if (!q.empty) {
                    const found = q.docs[0];
                    const data = found.data();
                    // Auto-migrate to UID-based document for future reads
                    await db.collection(COLLECTIONS.ADMIN_USERS).doc(uid).set(data, { merge: true });
                    console.log('Migrated admin profile to UID doc. Retrying load...');
                    return this.loadUserRole(uid);
                }
                console.error('Admin user not found by UID or email');
                this.updateLoadingStatus('Admin profile missing. Creating facilitator profile...');
                
                // Auto-create admin user record with facilitator role
                this.createAdminUserRecord(uid);
            }
        } catch (error) {
            console.error('Error loading user role:', error);
            if (this.roleLoadRetries < 1) {
                this.roleLoadRetries += 1;
                const cur = auth.currentUser;
                console.warn('Auth UID/email while retrying:', cur ? cur.uid : 'no-user', cur ? cur.email : '');
                this.updateLoadingStatus('Temporary error loading user data. Retrying...');
                setTimeout(() => {
                    if (this.currentUser) {
                        this.loadUserRole(this.currentUser.uid);
                    } else {
                        window.location.href = 'login.html';
                    }
                }, 800);
            } else {
                const cur = auth.currentUser;
                const uidInfo = cur ? ` UID: ${cur.uid}` : '';
                const emailInfo = cur ? ` Email: ${cur.email}` : '';
                const errInfo = error && (error.code || error.message) ? ` Error: ${error.code || error.message}` : '';
                this.updateLoadingStatus(`Unable to load admin data. Creating facilitator profile...${uidInfo}${emailInfo}${errInfo}`);
                
                // Try to auto-create admin user record
                this.createAdminUserRecord(uid);
            }
        }
    }

    showSection(sectionName) {
        // Check if user has access to this section
        if (!this.canAccessSection(sectionName)) {
            console.warn(`Access denied to section: ${sectionName} for role: ${this.currentUserRole}`);
            // Redirect to dashboard if access denied
            this.showSection('dashboard');
            return;
        }

        // Hide all sections
        document.querySelectorAll('.content-section').forEach(section => {
            section.classList.add('hidden');
        });

        // Show selected section
        const targetSection = document.getElementById(`${sectionName}Section`);
        console.log('Looking for section:', `${sectionName}Section`, 'Found:', targetSection);
        if (targetSection) {
            targetSection.classList.remove('hidden');
            
            // Refresh data for specific sections (but not dashboard or seniors to prevent constant reloading)
            if (sectionName === 'seniors' && window.seniorsManager && !window.seniorsManager.dataLoaded) {
                console.log('Seniors section shown, loading data...');
                window.seniorsManager.loadSeniors();
            }
            
            // Refresh health data when health section is shown
            if (sectionName === 'health' && window.healthManager) {
                console.log('Health section shown, refreshing data...');
                window.healthManager.refreshHealthData();
            }
            
            // Only refresh dashboard data if it hasn't been loaded yet
            if (sectionName === 'dashboard' && window.dashboardManager && !window.dashboardManager.dataLoaded) {
                console.log('Dashboard section shown, loading data...');
                window.dashboardManager.loadDashboardData();
            }
            
            // Load debug data counts
            if (sectionName === 'debug') {
                this.loadDebugDataCounts();
            }
        }

        // Update navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        
        const activeLink = document.querySelector(`[href="#${sectionName}"]`);
        if (activeLink) {
            activeLink.classList.add('active');
        }

        // Update page title
        this.updatePageTitle(sectionName);

        // Load section-specific data
        this.loadSectionData(sectionName);

        this.currentSection = sectionName;

        // Persist in URL without page reload
        if (window.location.hash !== `#${sectionName}`) {
            window.location.hash = `#${sectionName}`;
        }
    }

    async loadDebugDataCounts() {
        try {
            // Load emergency services count
            const emergencyServices = await FirebaseUtils.getCollection(COLLECTIONS.EMERGENCY_SERVICES);
            document.getElementById('emergencyCount').textContent = emergencyServices.length;

            // Load health records count
            const healthRecords = await FirebaseUtils.getCollection(COLLECTIONS.HEALTH_RECORDS);
            document.getElementById('healthCount').textContent = healthRecords.length;

            // Load benefits count from Realtime Database
            const benefitsSnapshot = await rtdb.ref('benefits').get();
            const benefits = benefitsSnapshot.val() || {};
            const benefitsCount = Object.keys(benefits).length;
            document.getElementById('benefitsCount').textContent = benefitsCount;

            // Load social services count
            const socialServicesSnapshot = await rtdb.ref('social_services').get();
            const socialServices = socialServicesSnapshot.val() || {};
            const socialServicesCount = Object.keys(socialServices).length;
            document.getElementById('socialCount').textContent = socialServicesCount;

        } catch (e) {
            console.error('Error loading debug data counts:', e);
            document.getElementById('emergencyCount').textContent = 'Error';
            document.getElementById('healthCount').textContent = 'Error';
            document.getElementById('benefitsCount').textContent = 'Error';
            document.getElementById('socialCount').textContent = 'Error';
        }
    }

    updatePageTitle(sectionName) {
        const titles = {
            'dashboard': 'Dashboard',
            'seniors': 'Senior Citizens',
            'data-collection': 'Data Collection',
            'health': 'Health',
            'social-services': 'Social Services',
            'debug': 'Debug & Sample Data',
            'benefits': 'Benefits Management',
            'appointments': 'Appointments',
            'analytics': 'Analytics',
            'reports': 'Reports',
            'settings': 'Settings'
        };

        const subtitles = {
            'dashboard': 'Welcome to SeniorHub Admin Portal',
            'seniors': 'Manage senior citizen information',
            'data-collection': 'Collect and update senior data',
            'health': 'View and manage health tracking entries',
            'social-services': 'Directory of government and community services',
            'benefits': 'Manage government benefits',
            'appointments': 'Schedule and manage appointments',
            'analytics': 'View data insights and trends',
            'reports': 'Generate and download reports',
            'settings': 'Configure application settings'
        };

        const pageTitleElement = document.getElementById('pageTitle');
        if (pageTitleElement) {
            pageTitleElement.textContent = titles[sectionName] || 'Dashboard';
        }
        
        const pageSubtitleElement = document.getElementById('pageSubtitle');
        if (pageSubtitleElement) {
            pageSubtitleElement.textContent = subtitles[sectionName] || 'Welcome to SeniorHub Admin Portal';
        }
    }

    loadSectionData(sectionName) {
        switch (sectionName) {
            case 'dashboard':
                if (window.dashboardManager) {
                    dashboardManager.loadDashboardData();
                }
                if (window.analyticsManager) {
                    analyticsManager.loadAnalyticsData();
                }
                break;
            case 'seniors':
                if (window.seniorsManager) {
                    seniorsManager.loadSeniors();
                }
                break;
            case 'benefits':
                if (window.benefitsManager) {
                    benefitsManager.loadBenefits();
                }
                break;
            case 'appointments':
                if (window.appointmentsManager) {
                    appointmentsManager.loadAppointments();
                }
                break;
            case 'analytics':
                if (window.analyticsManager) {
                    analyticsManager.loadAnalyticsData();
                }
                break;
            case 'emergency':
                if (window.emergencyManager) {
                    emergencyManager.loadServices();
                }
                if (window.emergencyAlertsManager) {
                    console.log('Loading emergency alerts data...');
                    emergencyAlertsManager.loadAlerts();
                }
                break;
            case 'health':
                if (window.healthManager) {
                    healthManager.loadHealthRecords();
                }
                break;
            case 'admin-management':
                if (window.adminManager) {
                    adminManager.loadAdmins();
                }
                break;
            case 'social-services':
                if (window.socialServicesManager) {
                    socialServicesManager.loadServices();
                }
                break;
            case 'data-collection':
                // Data collection forms are loaded on demand
                break;
            case 'settings':
                if (window.settingsManager) {
                    console.log('Loading settings data...');
                    console.log('Current user role:', this.currentUserRole);
                    console.log('ADMIN_ROLES.SUPER_ADMIN:', ADMIN_ROLES.SUPER_ADMIN);
                    console.log('Is super admin?', this.currentUserRole === ADMIN_ROLES.SUPER_ADMIN);
                    console.log('Role comparison details:', {
                        currentRole: this.currentUserRole,
                        superAdminRole: ADMIN_ROLES.SUPER_ADMIN,
                        isEqual: this.currentUserRole === ADMIN_ROLES.SUPER_ADMIN,
                        isSuperAdminString: this.currentUserRole === 'super_admin'
                    });
                    settingsManager.loadAdmins();
                } else {
                    console.warn('SettingsManager not found');
                }
                break;
            default:
                console.log(`Loading data for section: ${sectionName}`);
        }
    }

    setupRoleBasedUI() {
        const role = this.currentUserRole;
        console.log('Setting up role-based UI for role:', role);
        console.log('ADMIN_ROLES:', ADMIN_ROLES);
        console.log('ADMIN_ROLES.SUPER_ADMIN:', ADMIN_ROLES.SUPER_ADMIN);
        console.log('ADMIN_ROLES.FACILITATOR:', ADMIN_ROLES.FACILITATOR);
        
        // Show/hide role-specific navigation items
        document.querySelectorAll('.facilitator-only').forEach(el => {
            const shouldShow = role === ADMIN_ROLES.FACILITATOR || role === 'facilitator';
            el.style.display = shouldShow ? 'block' : 'none';
            console.log('Facilitator-only element:', el, 'should show:', shouldShow);
        });
        
        document.querySelectorAll('.super-only').forEach(el => {
            const shouldShow = role === ADMIN_ROLES.SUPER_ADMIN || role === 'super_admin';
            el.style.display = shouldShow ? 'block' : 'none';
            console.log('Super-only element:', el, 'should show:', shouldShow, 'role:', role, 'ADMIN_ROLES.SUPER_ADMIN:', ADMIN_ROLES.SUPER_ADMIN);
            console.log('Element details:', {
                element: el,
                className: el.className,
                id: el.id,
                shouldShow: shouldShow,
                role: role,
                superAdminRole: ADMIN_ROLES.SUPER_ADMIN,
                isEqual: role === ADMIN_ROLES.SUPER_ADMIN,
                isSuperAdminString: role === 'super_admin'
            });
        });

        // Update user info in header
        this.updateUserInfo();
    }

    async updateUserInfo() {
        if (this.currentUser) {
            try {
                // Get user data from admin_users collection
                const userDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(this.currentUser.uid).get();
                let userName = 'Admin User';
                let userRole = 'Admin';
                let userEmail = this.currentUser.email || 'admin@seniorhub.com';
                
                if (userDoc.exists) {
                    const userData = userDoc.data();
                    userName = userData.name || userData.displayName || this.currentUser.displayName || 'Admin User';
                    userRole = userData.role || 'Admin';
                    userEmail = userData.email || this.currentUser.email || 'admin@seniorhub.com';
                    
                    // Update role labels for display
                    this.currentUserRoleLabel = this.formatRoleDisplay(userRole);
                    
                    // Apply role-based visibility
                    this.applyRoleBasedVisibility(userRole);
                } else {
                    // Fallback to Firebase Auth data
                    userName = this.currentUser.displayName || 'Admin User';
                    userRole = this.currentUserRoleLabel || this.currentUserRole || 'Admin';
                }
                
                // Update sidebar user info with null checks
                const userNameElement = document.getElementById('userName');
                if (userNameElement) {
                    userNameElement.textContent = userName;
                }
                
                const userRoleElement = document.getElementById('userRole');
                if (userRoleElement) {
                    userRoleElement.textContent = this.formatRoleDisplay(userRole);
                }
                
                const userEmailElement = document.getElementById('userEmail');
                if (userEmailElement) {
                    userEmailElement.textContent = userEmail;
                }
                
                // Update header user info with null checks
                const headerUserNameElement = document.getElementById('headerUserName');
                if (headerUserNameElement) {
                    headerUserNameElement.textContent = userName;
                }
                
                // Update user avatars with dynamic URLs
                const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}&background=007bff&color=fff&size=`;
                
                // Sidebar avatar
                const sidebarAvatar = document.getElementById('userAvatar');
                if (sidebarAvatar) {
                    sidebarAvatar.src = avatarUrl + '40';
                    sidebarAvatar.alt = userName;
                }
                
                // Header avatar
                const headerAvatar = document.getElementById('headerUserAvatar');
                if (headerAvatar) {
                    headerAvatar.src = avatarUrl + '32';
                    headerAvatar.alt = userName;
                }
                
                // Dropdown avatar
                const dropdownAvatar = document.getElementById('dropdownUserAvatar');
                if (dropdownAvatar) {
                    dropdownAvatar.src = avatarUrl + '48';
                    dropdownAvatar.alt = userName;
                }
                
                // Update dropdown user info
                const dropdownUserName = document.getElementById('dropdownUserName');
                const dropdownUserRole = document.getElementById('dropdownUserRole');
                const dropdownUserEmail = document.getElementById('dropdownUserEmail');
                
                if (dropdownUserName) dropdownUserName.textContent = userName;
                if (dropdownUserRole) dropdownUserRole.textContent = this.formatRoleDisplay(userRole);
                if (dropdownUserEmail) dropdownUserEmail.textContent = userEmail;
                
            } catch (error) {
                console.error('Error loading user info:', error);
                // Fallback to basic info
                const userName = this.currentUser.displayName || 'Admin User';
                const userRole = this.currentUserRoleLabel || this.currentUserRole || 'Admin';
                const userEmail = this.currentUser.email || 'admin@seniorhub.com';
                
                document.getElementById('userName').textContent = userName;
                document.getElementById('userRole').textContent = userRole;
                document.getElementById('userEmail').textContent = userEmail;
                document.getElementById('headerUserName').textContent = userName;
            }
        }
    }
    
    formatRoleDisplay(role) {
        // Trim whitespace and normalize role
        const normalizedRole = (role || '').toString().trim().toLowerCase();
        const roleLabels = {
            'super_admin': 'Super Admin',
            'facilitator': 'Facilitator',
            'admin': 'Admin'
        };
        return roleLabels[normalizedRole] || 'Admin';
    }

    getRolePermissions(role) {
        const normalizedRole = (role || '').toString().trim().toLowerCase();
        const permissions = {
            'admin': {
                health_records: true,
                emergency_alerts: true,
                user_management: true,
                data_export: true,
                seniors_management: true,
                benefits_management: true,
                appointments_management: true,
                analytics_view: true,
                system_settings: true,
                admin_management: true,
                create_admin_users: true,
                delete_admin_users: true,
                edit_admin_users: true,
                view_all_admins: true
            }
        };
        return permissions[normalizedRole] || permissions['admin'];
    }

    getRoleDescription(role) {
        const normalizedRole = (role || '').toString().trim().toLowerCase();
        const descriptions = {
            'admin': 'Full system access with administrative privileges. Can create, edit, and delete all admin users and manage all system settings.'
        };
        return descriptions[normalizedRole] || 'System user with basic access';
    }

    // Check if current user has specific permission
    hasPermission(permission) {
        if (!this.currentUserRole) return false;
        const permissions = this.getRolePermissions(this.currentUserRole);
        return permissions[permission] === true;
    }

    // Check if current user is Admin
    isAdmin() {
        return this.currentUserRole === 'admin';
    }

    // Check if current user can manage admin users
    canManageAdmins() {
        return this.hasPermission('admin_management') && this.hasPermission('create_admin_users');
    }

    // Apply role-based visibility to UI elements
    applyRoleBasedVisibility(userRole) {
        const normalizedRole = (userRole || '').toString().trim().toLowerCase();
        
        // Prevent repeated calls with the same role
        if (this.lastAppliedRole === normalizedRole) {
            return;
        }
        this.lastAppliedRole = normalizedRole;
        
        console.log('Applying role-based visibility for:', normalizedRole);
        
        // Show/hide admin only elements (all admin users have full access)
        const adminElements = document.querySelectorAll('.super-admin-only, .facilitator-only');
        adminElements.forEach(element => {
            element.style.display = normalizedRole === 'admin' ? 'block' : 'none';
        });

        // Show/hide dashboard types based on role
        const superAdminDashboard = document.querySelector('.super-admin-dashboard');
        const facilitatorDashboard = document.querySelector('.facilitator-dashboard');
        
        if (superAdminDashboard && facilitatorDashboard) {
            if (normalizedRole === 'admin') {
                superAdminDashboard.style.display = 'block';
                facilitatorDashboard.style.display = 'none';
            }
        }

        // Apply role-based navigation restrictions
        this.applyNavigationRestrictions(normalizedRole);

        // Add role class to body for CSS targeting
        document.body.className = document.body.className.replace(/role-\w+/g, '');
        document.body.classList.add(`role-${normalizedRole}`);
        
        console.log('Role-based visibility applied successfully');
    }

    // Apply navigation restrictions based on role
    applyNavigationRestrictions(userRole) {
        const normalizedRole = (userRole || '').toString().trim().toLowerCase();
        
        // Get all navigation links
        const navLinks = document.querySelectorAll('.nav-link');
        
        navLinks.forEach(link => {
            const href = link.getAttribute('href');
            const section = href ? href.replace('#', '') : '';
            
            // Define allowed sections for admin role (single role with full access)
            const adminAllowedSections = [
                'dashboard', 'seniors', 'emergency', 'health', 'benefits', 
                'social-services', 'analytics', 'reports', 'admin-management'
            ];
            
            let isAllowed = false;
            
            if (normalizedRole === 'admin') {
                isAllowed = adminAllowedSections.includes(section);
            }
            
            // Show/hide navigation links based on role
            if (isAllowed) {
                link.style.display = 'flex';
                link.style.pointerEvents = 'auto';
                link.style.opacity = '1';
            } else {
                link.style.display = 'none';
                link.style.pointerEvents = 'none';
                link.style.opacity = '0.5';
            }
        });
        
        console.log(`Navigation restrictions applied for role: ${normalizedRole}`);
    }

    // Check if user can access a specific section
    canAccessSection(section) {
        const userRole = this.currentUserRole;
        const normalizedRole = (userRole || '').toString().trim().toLowerCase();
        
        // Define allowed sections for admin role (single role with full access)
        const adminAllowedSections = [
            'dashboard', 'seniors', 'emergency', 'health', 'benefits', 
            'social-services', 'analytics', 'reports', 'admin-management'
        ];
        
        if (normalizedRole === 'admin') {
            return adminAllowedSections.includes(section);
        }
        
        return false;
    }

    // Prevent unnecessary collection creation for super admin
    preventUnnecessaryCollectionCreation() {
        const userRole = this.currentUserRole;
        const normalizedRole = (userRole || '').toString().trim().toLowerCase();
        
        if (normalizedRole === 'super_admin') {
            console.log('Super admin detected - preventing unnecessary collection creation');
            // Super admin should use existing collections only
            // No new collections should be created for super admin
            return true;
        }
        
        return false;
    }

    toggleSidebar() {
        const sidebar = document.querySelector('.sidebar');
        const mainContent = document.querySelector('.main-content');
        
        if (window.innerWidth <= 1024) {
            sidebar.classList.toggle('open');
        } else {
            sidebar.classList.toggle('collapsed');
            mainContent.classList.toggle('expanded');
        }
    }

    handleKeyboardShortcuts(e) {
        // Ctrl/Cmd + K for search
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            this.focusSearch();
        }

        // Escape to close modal
        if (e.key === 'Escape') {
            this.closeModal();
        }

        // Number keys for quick navigation (only when not in input fields)
        if (e.key >= '1' && e.key <= '9' && !e.ctrlKey && !e.metaKey && !e.altKey) {
            // Check if the target is an input, textarea, or contenteditable element
            const target = e.target;
            const isInputField = target.tagName === 'INPUT' || 
                                target.tagName === 'TEXTAREA' || 
                                target.contentEditable === 'true' ||
                                target.closest('.modal') !== null;
            
            if (!isInputField) {
                const navItems = document.querySelectorAll('.nav-link:not(.hidden)');
                const index = parseInt(e.key) - 1;
                if (navItems[index]) {
                    e.preventDefault();
                    navItems[index].click();
                }
            }
        }
    }

    handleWindowResize() {
        const sidebar = document.querySelector('.sidebar');
        const mainContent = document.querySelector('.main-content');
        
        if (window.innerWidth <= 1024) {
            sidebar.classList.remove('collapsed');
            mainContent.classList.remove('expanded');
        }
    }

    focusSearch() {
        const searchInput = document.querySelector('.search-input');
        if (searchInput) {
            searchInput.focus();
        }
    }

    openModal(title, body) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalBody').innerHTML = body;
        document.getElementById('modalOverlay').classList.remove('hidden');
        
        // Focus first input in modal
        setTimeout(() => {
            const firstInput = document.querySelector('#modalOverlay input, #modalOverlay select, #modalOverlay textarea');
            if (firstInput) {
                firstInput.focus();
            }
        }, 100);
    }

    closeModal() {
        document.getElementById('modalOverlay').classList.add('hidden');
    }

    async handleLogout() {
        try {
            // Show loading state
            this.showNotification('Signing out...', 'info');
            
            // Sign out from Firebase
            await auth.signOut();
            
            // Clear user data
            this.currentUser = null;
            this.currentUserRole = null;
            this.currentUserRoleLabel = null;
            
            // Clear any stored data
            localStorage.removeItem('userRole');
            localStorage.removeItem('userData');
            
            // Show success message briefly before redirect
            this.showSuccess('Successfully signed out');
            
            // Redirect to login page after a short delay
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1000);
            
        } catch (error) {
            console.error('Logout error:', error);
            this.showError('Error signing out. Please try again.');
        }
    }

    showAuthScreen() {
        document.getElementById('loadingScreen').classList.add('hidden');
        document.getElementById('authScreen').classList.remove('hidden');
        document.getElementById('mainApp').classList.add('hidden');
    }

    showMainApp() {
        document.getElementById('loadingScreen').classList.add('hidden');
        document.getElementById('authScreen').classList.add('hidden');
        document.getElementById('mainApp').classList.remove('hidden');
        
        this.updateUserInfo();
        this.setupRoleBasedUI();
    }

    // Utility functions
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${this.getNotificationIcon(type)}"></i>
                <span>${message}</span>
            </div>
            <button class="notification-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Add to notification container
        let container = document.querySelector('.notification-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'notification-container';
            document.body.appendChild(container);
        }

        container.appendChild(notification);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }

    getNotificationIcon(type) {
        const icons = {
            'success': 'check-circle',
            'error': 'exclamation-circle',
            'warning': 'exclamation-triangle',
            'info': 'info-circle'
        };
        return icons[type] || 'info-circle';
    }

    showLoading(message = 'Loading...') {
        const loadingOverlay = document.createElement('div');
        loadingOverlay.className = 'loading-overlay';
        loadingOverlay.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <p>${message}</p>
            </div>
        `;
        document.body.appendChild(loadingOverlay);
    }

    hideLoading() {
        const loadingOverlay = document.querySelector('.loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.remove();
        }
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    // Dark mode removed

    // initDarkMode removed

    // Global functions for use in HTML
    openAddSeniorModal() {
        if (window.seniorsManager) {
            seniorsManager.openAddSeniorModal();
        }
    }

    exportSeniorsData() {
        if (window.seniorsManager) {
            seniorsManager.exportSeniorsData();
        }
    }
}

// Initialize Main Application
const mainApp = new MainApp();

// Global functions for use in HTML
window.showSection = (sectionName) => mainApp.showSection(sectionName);
window.closeModal = () => mainApp.closeModal();
window.openAddSeniorModal = () => {
    if (window.seniorsManager) {
        window.seniorsManager.openAddSeniorModal();
    } else {
        console.error('SeniorsManager not available');
    }
};
window.exportSeniorsData = () => {
    if (window.seniorsManager) {
        window.seniorsManager.exportSeniorsData();
    } else {
        console.error('SeniorsManager not available');
    }
};
// Data Collection helper for onclick in index.html
window.openDataForm = (formType) => {
    if (window.dataCollectionManager) {
        dataCollectionManager.openDataForm(formType);
    }
};
// Expose managers used by new sections
window.refreshBenefits = () => window.benefitsManager && benefitsManager.loadBenefits();
window.refreshAppointments = () => window.appointmentsManager && appointmentsManager.loadAppointments();
window.refreshAdmins = () => window.settingsManager && settingsManager.loadAdmins();

// Export for use in other files
window.mainApp = mainApp;

// User Menu Functions
function toggleUserMenu() {
    const dropdown = document.getElementById('userMenuDropdown');
    if (dropdown) {
        dropdown.classList.toggle('show');
    }
}

// Close user menu when clicking outside
document.addEventListener('click', (e) => {
    const userMenu = document.querySelector('.user-menu');
    const dropdown = document.getElementById('userMenuDropdown');
    
    if (userMenu && dropdown && !userMenu.contains(e.target)) {
        dropdown.classList.remove('show');
    }
});

// User Menu Functions
async function openProfileModal() {
    const user = window.mainApp?.currentUser;
    if (!user) return;
    
    try {
        // Get user data from admin_users collection
        const userDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(user.uid).get();
        let userName = user.displayName || 'Admin User';
        let userRole = 'Admin';
        let userEmail = user.email || 'admin@seniorhub.com';
        let userDepartment = '';
        let userCreatedAt = '';
        let userLastLogin = '';
        
        if (userDoc.exists) {
            const userData = userDoc.data();
            userName = userData.name || userData.displayName || user.displayName || 'Admin User';
            userRole = userData.role || 'Admin';
            userEmail = userData.email || user.email || 'admin@seniorhub.com';
            userDepartment = userData.department || '';
            userCreatedAt = userData.createdAt ? userData.createdAt.toDate().toLocaleDateString() : '';
            userLastLogin = userData.lastLogin ? userData.lastLogin.toDate().toLocaleString() : '';
        }
        
        const roleDisplay = window.mainApp?.formatRoleDisplay(userRole) || 'Admin';
        const roleDescription = getRoleDescription(userRole);
        const rolePermissions = getRolePermissions(userRole);
        
        const body = `
            <div class="profile-form">
                <div class="profile-header">
                    <div class="profile-avatar">
                        <img src="https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}&background=007bff&color=fff&size=100" alt="Profile Picture" />
                    </div>
                    <div class="profile-info">
                        <h3>${userName}</h3>
                        <span class="role-badge ${getRoleClass(userRole)}">${roleDisplay}</span>
                        <p class="role-description">${roleDescription}</p>
                    </div>
                </div>
                
                <div class="profile-details">
                    <div class="detail-item">
                        <label>Full Name</label>
                        <div class="detail-value">${userName}</div>
                    </div>
                    <div class="detail-item">
                        <label>Email Address</label>
                        <div class="detail-value">${userEmail}</div>
                    </div>
                    <div class="detail-item">
                        <label>Role</label>
                        <div class="detail-value">
                            <span class="role-badge ${getRoleClass(userRole)}">${roleDisplay}</span>
                        </div>
                    </div>
                    ${userDepartment ? `
                    <div class="detail-item">
                        <label>Department/Office</label>
                        <div class="detail-value">${userDepartment}</div>
                    </div>
                    ` : ''}
                    ${userCreatedAt ? `
                    <div class="detail-item">
                        <label>Account Created</label>
                        <div class="detail-value">${userCreatedAt}</div>
                    </div>
                    ` : ''}
                    ${userLastLogin ? `
                    <div class="detail-item">
                        <label>Last Login</label>
                        <div class="detail-value">${userLastLogin}</div>
                    </div>
                    ` : ''}
                </div>
                
                <div class="permissions-section">
                    <h4>Your Permissions</h4>
                    <div class="permissions-list">
                        ${rolePermissions.map(permission => `
                            <div class="permission-item">
                                <i class="fas fa-check-circle"></i>
                                <span>${permission}</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="closeModal()">
                    <i class="fas fa-times"></i> Close
                </button>
            </div>
        `;
        
        if (window.mainApp) {
            window.mainApp.openModal('My Profile', body);
        }
    } catch (error) {
        console.error('Error loading profile data:', error);
        if (window.mainApp) {
            window.mainApp.showError('Failed to load profile data');
        }
    }
}

function getRoleDescription(role) {
    const descriptions = {
        'super_admin': 'Full system access with ability to manage all users and settings',
        'facilitator': 'Can manage senior citizens, health records, and benefits',
        'admin': 'Basic administrative access to view and manage data'
    };
    return descriptions[role] || 'Administrative access to the system';
}

function getRolePermissions(role) {
    const permissions = {
        'super_admin': [
            'Manage all admin users',
            'View and edit all data',
            'Access analytics and reports',
            'System configuration',
            'User role management',
            'Data export and backup'
        ],
        'facilitator': [
            'Manage senior citizens',
            'View and edit health records',
            'Manage benefits and services',
            'Create appointments',
            'Access emergency services',
            'View social services directory'
        ],
        'admin': [
            'View senior citizen data',
            'View health records',
            'View benefits information',
            'Basic data management'
        ]
    };
    return permissions[role] || ['Basic system access'];
}

function getRoleClass(role) {
    const roleClasses = {
        'super_admin': 'super-admin',
        'facilitator': 'facilitator',
        'admin': 'admin'
    };
    return roleClasses[role] || 'admin';
}






// Placeholder functions for user menu actions
function saveProfile() {
    if (window.mainApp) {
        window.mainApp.showSuccess('Profile updated successfully!');
        window.mainApp.closeModal();
    }
}


function changeProfilePicture() {
    if (window.mainApp) {
        window.mainApp.showNotification('Profile picture upload feature coming soon!', 'info');
    }
}

// Global function for Sign Out button
function handleLogout() {
    // Show confirmation dialog
    if (confirm('Are you sure you want to sign out?')) {
        if (window.mainApp) {
            window.mainApp.handleLogout();
        } else {
            // Fallback if mainApp is not available
            window.location.href = 'login.html';
        }
    }
}

// Debug function to test email availability
async function testEmailAvailability() {
    const email = prompt('Enter email to test availability:');
    if (!email) return;
    
    try {
        if (window.seniorsManager) {
            const isAvailable = await window.seniorsManager.testEmailAvailability(email);
            alert(`Email ${email} is ${isAvailable ? 'AVAILABLE' : 'NOT AVAILABLE'} for use`);
        } else {
            alert('Seniors manager not available');
        }
    } catch (error) {
        console.error('Error testing email availability:', error);
        alert('Error testing email availability');
    }
}