// Settings Management
class SettingsManager {
    constructor() {
        this.admins = [];
        this.init();
    }

    init() {
        this.setupRealtime();
        this.loadAdmins();
    }

    setupRealtime() {
        console.log('Setting up real-time listener for admin users...');
        try {
            FirebaseUtils.onSnapshot(COLLECTIONS.ADMIN_USERS, (snapshot) => {
                console.log('Real-time snapshot received:', snapshot.size, 'documents');
                console.log('Real-time snapshot empty:', snapshot.empty);
                const items = [];
                snapshot.forEach(doc => {
                    const data = { id: doc.id, ...doc.data() };
                    items.push(data);
                    console.log('Admin user from real-time:', data);
                });
                console.log('Real-time items count:', items.length);
                this.admins = items;
                this.render();
            }, 'email');
        } catch (e) {
            console.error('Failed to setup real-time listener:', e);
            console.error('Real-time listener error details:', e.message, e.code);
        }
    }

    async loadAdmins() {
        try {
            console.log('Loading admin users from collection:', COLLECTIONS.ADMIN_USERS);
            console.log('Current user:', window.mainApp?.currentUser);
            console.log('Current user role:', window.mainApp?.currentUserRole);
            
            // Try multiple approaches to load the data
            const items = await FirebaseUtils.getCollection(COLLECTIONS.ADMIN_USERS, 'email');
            console.log('Loaded admin users via getCollection:', items);
            console.log('getCollection result length:', items?.length || 0);
            
            // Also try direct Firebase query for debugging
            const directQuery = await db.collection(COLLECTIONS.ADMIN_USERS).get();
            const directItems = directQuery.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            console.log('Loaded admin users via direct query:', directItems);
            console.log('Direct query result length:', directItems?.length || 0);
            console.log('Direct query snapshot size:', directQuery.size);
            
            // Use whichever method returns data
            this.admins = items && items.length > 0 ? items : directItems;
            console.log('Final admin users array:', this.admins);
            console.log('Final admin users count:', this.admins?.length || 0);
            this.render();
        } catch (e) {
            console.error('Failed to load admins', e);
            console.error('Error details:', e.message, e.code);
            console.error('Error stack:', e.stack);
            this.admins = [];
            this.render();
        }
    }

    // Force refresh method with visual feedback
    async forceRefresh() {
        try {
            console.log('Force refreshing settings data...');
            
            // Show loading state on refresh button
            const refreshBtn = document.querySelector('button[onclick="settingsManager.forceRefresh()"]');
            if (refreshBtn) {
                const originalText = refreshBtn.innerHTML;
                refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Refreshing...';
                refreshBtn.disabled = true;
                
                // Re-enable button after a delay
                setTimeout(() => {
                    refreshBtn.innerHTML = originalText;
                    refreshBtn.disabled = false;
                }, 2000);
            }
            
            // Clear current data and show loading
            this.admins = [];
            this.render();
            
            // Load fresh data
            await this.loadAdmins();
            
            if (window.mainApp) {
                window.mainApp.showSuccess('Admin users refreshed successfully!');
            }
        } catch (error) {
            console.error('Error refreshing admin data:', error);
            if (window.mainApp) {
                window.mainApp.showError('Failed to refresh admin data');
            }
        }
    }

    // Debug method to check Firebase connectivity
    async checkFirebaseConnectivity() {
        try {
            console.log('=== Firebase Connectivity Check ===');
            console.log('Current user:', window.mainApp?.currentUser);
            console.log('Current user role:', window.mainApp?.currentUserRole);
            console.log('Firebase app:', firebase.app().name);
            console.log('Firestore instance:', db.app.name);
            console.log('Admin users collection name:', COLLECTIONS.ADMIN_USERS);
            
            // Try to read the admin_users collection
            console.log('Attempting to read admin_users collection...');
            const adminUsersQuery = await db.collection(COLLECTIONS.ADMIN_USERS).get();
            console.log('Admin users collection query successful');
            console.log('Admin users collection size:', adminUsersQuery.size);
            console.log('Admin users collection empty:', adminUsersQuery.empty);
            
            if (adminUsersQuery.empty) {
                console.log('⚠️  Admin users collection is empty - no admin users found');
            } else {
                console.log('✅ Admin users collection has data');
                adminUsersQuery.forEach(doc => {
                    console.log('Admin user document:', doc.id, doc.data());
                });
            }
            
            // Try to read a test document
            console.log('Testing general Firebase read access...');
            const testDoc = await db.collection('test').doc('test').get();
            console.log('Firebase read test successful');
            
        } catch (e) {
            console.error('Firebase connectivity check failed:', e);
            console.error('Error details:', e.message, e.code);
            console.error('Error stack:', e.stack);
        }
    }

    // Debug method to manually load data with detailed error handling
    async debugLoadAdmins() {
        try {
            console.log('=== Manual Admin Data Loading Debug ===');
            console.log('Starting manual data load...');
            
            // Clear existing data
            this.admins = [];
            this.render();
            
            // Try to load data step by step
            console.log('Step 1: Testing Firebase connection...');
            const testConnection = await db.collection(COLLECTIONS.ADMIN_USERS).limit(1).get();
            console.log('✅ Firebase connection successful');
            
            console.log('Step 2: Loading all admin users...');
            const allAdmins = await db.collection(COLLECTIONS.ADMIN_USERS).get();
            console.log('✅ Loaded admin users query, size:', allAdmins.size);
            
            console.log('Step 3: Processing documents...');
            const processedAdmins = [];
            allAdmins.forEach(doc => {
                const data = { id: doc.id, ...doc.data() };
                processedAdmins.push(data);
                console.log('Processed admin:', data);
            });
            
            console.log('Step 4: Setting admin data...');
            this.admins = processedAdmins;
            console.log('✅ Set admin data, count:', this.admins.length);
            
            console.log('Step 5: Rendering table...');
            this.render();
            console.log('✅ Render completed');
            
            if (window.mainApp) {
                window.mainApp.showSuccess(`Successfully loaded ${this.admins.length} admin users!`);
            }
            
        } catch (e) {
            console.error('❌ Manual data loading failed:', e);
            console.error('Error details:', e.message, e.code);
            console.error('Error stack:', e.stack);
            
            if (window.mainApp) {
                window.mainApp.showError('Failed to load admin data: ' + e.message);
            }
        }
    }

    // Debug method to test Firebase security rules
    async testFirebasePermissions() {
        try {
            console.log('=== Testing Firebase Permissions ===');
            
            // Test 1: Check if we can read the admin_users collection
            console.log('Test 1: Reading admin_users collection...');
            const adminUsersQuery = await db.collection(COLLECTIONS.ADMIN_USERS).get();
            console.log('✅ Successfully read admin_users collection');
            console.log('Collection size:', adminUsersQuery.size);
            
            // Test 2: Check if we can read individual documents
            console.log('Test 2: Reading individual documents...');
            for (const doc of adminUsersQuery.docs) {
                try {
                    const docData = await db.collection(COLLECTIONS.ADMIN_USERS).doc(doc.id).get();
                    console.log('✅ Successfully read document:', doc.id, docData.data());
                } catch (error) {
                    console.error('❌ Failed to read document:', doc.id, error);
                }
            }
            
            // Test 3: Check if we can write to the collection (for testing)
            console.log('Test 3: Testing write permissions...');
            try {
                const testDoc = {
                    name: 'Test User',
                    email: 'test@example.com',
                    role: 'admin',
                    isActive: true,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                };
                const docRef = await db.collection(COLLECTIONS.ADMIN_USERS).add(testDoc);
                console.log('✅ Successfully created test document:', docRef.id);
                
                // Clean up - delete the test document
                await db.collection(COLLECTIONS.ADMIN_USERS).doc(docRef.id).delete();
                console.log('✅ Successfully deleted test document');
            } catch (error) {
                console.error('❌ Failed to write to collection:', error);
            }
            
        } catch (e) {
            console.error('Firebase permissions test failed:', e);
            console.error('Error details:', e.message, e.code);
        }
    }

    // Debug method to create test admin data
    async createTestAdminData() {
        try {
            console.log('=== Creating Test Admin Data ===');
            
            const testAdmins = [
                {
                    name: 'Super Admin',
                    email: 'superadmin@seniorhub.com',
                    role: 'super_admin',
                    department: 'System Administration',
                    isActive: true,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                },
                {
                    name: 'John Facilitator',
                    email: 'john.facilitator@seniorhub.com',
                    role: 'facilitator',
                    department: 'OSCA',
                    isActive: true,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                },
                {
                    name: 'Jane Admin',
                    email: 'jane.admin@seniorhub.com',
                    role: 'admin',
                    department: 'DSWD',
                    isActive: true,
                    createdAt: FirebaseUtils.getTimestamp(),
                    updatedAt: FirebaseUtils.getTimestamp()
                }
            ];

            console.log('Creating', testAdmins.length, 'test admin users...');
            
            for (const admin of testAdmins) {
                try {
                    const docRef = await db.collection(COLLECTIONS.ADMIN_USERS).add(admin);
                    console.log('✅ Created test admin:', admin.name, 'with ID:', docRef.id);
                } catch (error) {
                    console.error('❌ Failed to create test admin:', admin.name, error);
                }
            }

            console.log('Test admin data creation completed');
            console.log('Reloading admin users...');
            await this.loadAdmins();
            
            if (window.mainApp) {
                window.mainApp.showSuccess('Test admin data created successfully!');
            }
        } catch (e) {
            console.error('Failed to create test admin data:', e);
            console.error('Error details:', e.message, e.code);
            if (window.mainApp) {
                window.mainApp.showError('Failed to create test admin data: ' + e.message);
            }
        }
    }

    render() {
        const tbody = document.getElementById('adminsTableBody');
        if (!tbody) {
            console.warn('adminsTableBody not found');
            return;
        }
        
        console.log('Rendering admin users:', this.admins);
        console.log('Admin users count:', this.admins.length);
        console.log('Admin users details:', this.admins.map(admin => ({
            id: admin.id,
            name: admin.name,
            email: admin.email,
            role: admin.role,
            isActive: admin.isActive
        })));
        tbody.innerHTML = '';

        if (!this.admins.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-user-shield"></i>
                            <p>No admin users found</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.admins.forEach(admin => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div style="font-weight: 500; color: var(--text-gray);">
                        ${admin.name || admin.displayName || 'Unknown'}
                    </div>
                </td>
                <td>
                    <div style="color: var(--text-gray); font-size: 0.9rem;">
                        ${admin.email || ''}
                    </div>
                </td>
                <td><span class="role-badge ${this.getRoleClass(admin.role)}">${this.formatRoleDisplay(admin.role)}</span></td>
                <td><span class="status-badge ${admin.isActive !== false ? 'active' : 'inactive'}">${admin.isActive !== false ? 'Active' : 'Inactive'}</span></td>
                <td>
                    <div class="action-buttons">
                        <div class="action-group">
                            <button class="action-btn view-btn" onclick="settingsManager.view('${admin.id}')" title="View Details">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="action-btn edit-btn" onclick="settingsManager.edit('${admin.id}')" title="Edit Information">
                                <i class="fas fa-edit"></i>
                            </button>
                        </div>
                        <div class="action-group">
                            <button class="action-btn status-btn ${admin.isActive !== false ? 'active' : 'inactive'}" onclick="settingsManager.toggleStatus('${admin.id}')" title="${admin.isActive !== false ? 'Deactivate' : 'Activate'} Admin">
                                <i class="fas fa-${admin.isActive !== false ? 'ban' : 'check'}"></i>
                            </button>
                            ${(admin.role || '').toString().trim().toLowerCase() !== 'super_admin' ? `
                            <button class="action-btn delete-btn" onclick="settingsManager.delete('${admin.id}')" title="Delete Admin">
                                <i class="fas fa-trash"></i>
                            </button>` : ''}
                        </div>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    getRoleClass(role) {
        // Trim whitespace and normalize role
        const normalizedRole = (role || '').toString().trim().toLowerCase();
        const roleClasses = {
            'super_admin': 'super-admin',
            'facilitator': 'facilitator',
            'admin': 'admin'
        };
        return roleClasses[normalizedRole] || 'admin';
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

    openInviteAdminModal() {
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Full Name</label>
                    <input id="adminName" type="text" placeholder="Enter full name" />
                </div>
                <div class="form-group">
                    <label>Email Address</label>
                    <input id="adminEmail" type="email" placeholder="Enter email address" />
                </div>
                <div class="form-group">
                    <label>Role</label>
                    <select id="adminRole">
                        <option value="facilitator">Facilitator</option>
                        <option value="admin">Admin</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Department/Office</label>
                    <input id="adminDepartment" type="text" placeholder="e.g., OSCA, DSWD, City Hall" />
                </div>
                <div class="form-group full-width">
                    <label>Notes (Optional)</label>
                    <textarea id="adminNotes" placeholder="Additional notes about this admin user..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="settingsManager.inviteAdmin()"><i class="fas fa-paper-plane"></i> Send Invitation</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal('Invite New Admin', body);
    }

    async inviteAdmin() {
        try {
            const name = document.getElementById('adminName').value.trim();
            const email = document.getElementById('adminEmail').value.trim();
            const role = document.getElementById('adminRole').value;
            const department = document.getElementById('adminDepartment').value.trim();
            const notes = document.getElementById('adminNotes').value.trim();

            if (!name || !email || !role) {
                alert('Please fill in all required fields (Name, Email, Role)');
                return;
            }

            // Validate email format
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                alert('Please enter a valid email address');
                return;
            }

            await FirebaseUtils.addDoc(COLLECTIONS.ADMIN_USERS, {
                name,
                email,
                role: role.trim(), // Ensure no trailing spaces
                department: department || '',
                notes: notes || '',
                isActive: true,
                invitedAt: FirebaseUtils.getTimestamp(),
                invitedBy: window.mainApp?.currentUser?.email || 'System'
            });

            if (window.mainApp) {
                mainApp.closeModal();
                mainApp.showSuccess('Admin invitation sent successfully!');
            }
        } catch (e) {
            console.error('Failed to invite admin', e);
            if (window.mainApp) mainApp.showError('Failed to send invitation');
        }
    }

    view(id) {
        const admin = this.admins.find(x => x.id === id);
        if (!admin || !window.mainApp) return;
        const body = `
            <div class="detail-grid">
                <div><strong>Name:</strong> ${admin.name || admin.displayName || 'Unknown'}</div>
                <div><strong>Email:</strong> ${admin.email || ''}</div>
                <div><strong>Role:</strong> <span class="role-badge ${this.getRoleClass(admin.role)}">${this.formatRoleDisplay(admin.role)}</span></div>
                <div><strong>Department:</strong> ${admin.department || 'Not specified'}</div>
                <div><strong>Status:</strong> <span class="status-badge ${admin.isActive !== false ? 'active' : 'inactive'}">${admin.isActive !== false ? 'Active' : 'Inactive'}</span></div>
                <div><strong>Invited:</strong> ${this.formatTime(admin.invitedAt)}</div>
                <div><strong>Invited By:</strong> ${admin.invitedBy || 'System'}</div>
                ${admin.lastLogin ? `<div><strong>Last Login:</strong> ${this.formatTime(admin.lastLogin)}</div>` : ''}
                ${admin.notes ? `<div class="full-width"><strong>Notes:</strong><br>${admin.notes}</div>` : ''}
            </div>
        `;
        mainApp.openModal('Admin User Details', body);
    }

    edit(id) {
        const admin = this.admins.find(x => x.id === id);
        if (!admin || !window.mainApp) return;
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Full Name</label>
                    <input id="adminName" type="text" value="${admin.name || admin.displayName || ''}" />
                </div>
                <div class="form-group">
                    <label>Email Address</label>
                    <input id="adminEmail" type="email" value="${admin.email || ''}" />
                </div>
                <div class="form-group">
                    <label>Role</label>
                    <select id="adminRole">
                        <option value="facilitator" ${(admin.role || '').toString().trim().toLowerCase() === 'facilitator' ? 'selected' : ''}>Facilitator</option>
                        <option value="admin" ${(admin.role || '').toString().trim().toLowerCase() === 'admin' ? 'selected' : ''}>Admin</option>
                        ${(admin.role || '').toString().trim().toLowerCase() === 'super_admin' ? '<option value="super_admin" selected>Super Admin</option>' : ''}
                    </select>
                </div>
                <div class="form-group">
                    <label>Department/Office</label>
                    <input id="adminDepartment" type="text" value="${admin.department || ''}" />
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="adminStatus">
                        <option value="true" ${admin.isActive !== false ? 'selected' : ''}>Active</option>
                        <option value="false" ${admin.isActive === false ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Notes</label>
                    <textarea id="adminNotes">${admin.notes || ''}</textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="settingsManager.saveEdit('${admin.id}')"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        mainApp.openModal('Edit Admin User', body);
    }

    async saveEdit(id) {
        try {
            const name = document.getElementById('adminName').value.trim();
            const email = document.getElementById('adminEmail').value.trim();
            const role = document.getElementById('adminRole').value;
            const department = document.getElementById('adminDepartment').value.trim();
            const isActive = document.getElementById('adminStatus').value === 'true';
            const notes = document.getElementById('adminNotes').value.trim();

            if (!name || !email || !role) {
                alert('Please fill in all required fields (Name, Email, Role)');
                return;
            }

            // Validate email format
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                alert('Please enter a valid email address');
                return;
            }

            await FirebaseUtils.updateDoc(COLLECTIONS.ADMIN_USERS, id, {
                name,
                email,
                role: role.trim(), // Ensure no trailing spaces
                department: department || '',
                isActive,
                notes: notes || '',
                updatedAt: FirebaseUtils.getTimestamp(),
                updatedBy: window.mainApp?.currentUser?.email || 'System'
            });

            if (window.mainApp) {
                mainApp.closeModal();
                mainApp.showSuccess('Admin user updated successfully!');
            }
        } catch (e) {
            console.error('Failed to update admin', e);
            if (window.mainApp) mainApp.showError('Failed to update admin user');
        }
    }

    async toggleStatus(id) {
        const admin = this.admins.find(x => x.id === id);
        if (!admin) return;

        const newStatus = admin.isActive === false;
        const action = newStatus ? 'activate' : 'deactivate';
        
        if (!confirm(`Are you sure you want to ${action} this admin user?`)) return;

        try {
            await FirebaseUtils.updateDoc(COLLECTIONS.ADMIN_USERS, id, {
                isActive: newStatus,
                updatedAt: FirebaseUtils.getTimestamp(),
                updatedBy: window.mainApp?.currentUser?.email || 'System'
            });

            if (window.mainApp) {
                mainApp.showSuccess(`Admin user ${action}d successfully!`);
            }
        } catch (e) {
            console.error(`Failed to ${action} admin`, e);
            if (window.mainApp) mainApp.showError(`Failed to ${action} admin user`);
        }
    }

    async delete(id) {
        const admin = this.admins.find(x => x.id === id);
        if (!admin) return;

        if ((admin.role || '').toString().trim().toLowerCase() === 'super_admin') {
            alert('Cannot delete super admin user');
            return;
        }

        if (!confirm(`Are you sure you want to delete admin user "${admin.name || admin.email}"? This action cannot be undone.`)) return;

        try {
            await db.collection(COLLECTIONS.ADMIN_USERS).doc(id).delete();
            if (window.mainApp) mainApp.showSuccess('Admin user deleted successfully!');
        } catch (e) {
            console.error('Failed to delete admin', e);
            if (window.mainApp) mainApp.showError('Failed to delete admin user');
        }
    }

    formatTime(ts) {
        if (!ts) return 'N/A';
        const d = ts.toDate ? ts.toDate() : new Date(ts);
        return d.toLocaleString();
    }

}

const settingsManager = new SettingsManager();
window.settingsManager = settingsManager;
