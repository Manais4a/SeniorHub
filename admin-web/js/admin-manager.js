// Admin Management
class AdminManager {
    constructor() {
        this.admins = [];
        this.filteredAdmins = [];
        this.currentFilter = '';
        this.currentSearch = '';
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadAdminData();
    }

    setupEventListeners() {
        // Search functionality
        const searchInput = document.getElementById('adminSearch');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.currentSearch = e.target.value.toLowerCase();
                this.filterAdmins();
            });
        }

        // Filter functionality
        const filterSelect = document.getElementById('adminFilter');
        if (filterSelect) {
            filterSelect.addEventListener('change', (e) => {
                this.currentFilter = e.target.value;
                this.filterAdmins();
            });
        }
    }

    async loadAdminData() {
        try {
            this.showLoading();
            
            // Load admin users from Firestore
            const adminsSnapshot = await db.collection('admin_users').get();
            this.admins = [];
            
            adminsSnapshot.forEach(doc => {
                const data = doc.data();
                this.admins.push({
                    id: doc.id,
                    ...data,
                    createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
                    lastLogin: data.lastLogin?.toDate?.() || new Date(data.lastLogin)
                });
            });

            this.filterAdmins();
            this.updateAdminTable();
        } catch (error) {
            console.error('Error loading admin data:', error);
            this.showError('Failed to load admin data');
        } finally {
            this.hideLoading();
        }
    }

    filterAdmins() {
        this.filteredAdmins = this.admins.filter(admin => {
            const matchesSearch = !this.currentSearch || 
                admin.name?.toLowerCase().includes(this.currentSearch) ||
                admin.email?.toLowerCase().includes(this.currentSearch) ||
                admin.role?.toLowerCase().includes(this.currentSearch);
            
            const matchesFilter = !this.currentFilter || 
                admin.role === this.currentFilter ||
                (this.currentFilter === 'active' && admin.isActive) ||
                (this.currentFilter === 'inactive' && !admin.isActive);
            
            return matchesSearch && matchesFilter;
        });
        
        this.updateAdminTable();
    }

    updateAdminTable() {
        const tbody = document.getElementById('adminTableBody');
        if (!tbody) return;

        if (this.filteredAdmins.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No admin users found</td></tr>';
            return;
        }

        tbody.innerHTML = this.filteredAdmins.map(admin => `
            <tr>
                <td>
                    <div class="user-info">
                        <div class="user-avatar">
                            <i class="fas fa-user-shield"></i>
                        </div>
                        <div class="user-details">
                            <strong>${admin.name || 'Unknown'}</strong>
                        </div>
                    </div>
                </td>
                <td>${admin.email || 'N/A'}</td>
                <td>
                    <span class="role-badge role-${admin.role || 'unknown'}">
                        ${this.getRoleDisplayName(admin.role)}
                    </span>
                </td>
                <td>${admin.department || 'N/A'}</td>
                <td>
                    <span class="status-badge ${admin.isActive ? 'active' : 'inactive'}">
                        ${admin.isActive ? 'Active' : 'Inactive'}
                    </span>
                </td>
                <td>${this.formatDate(admin.lastLogin)}</td>
                <td>${this.formatDate(admin.createdAt)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-outline" onclick="adminManager.viewAdmin('${admin.id}')" title="View Details">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline" onclick="adminManager.editAdmin('${admin.id}')" title="Edit Admin">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="adminManager.deleteAdmin('${admin.id}')" title="Delete Admin">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    getRoleDisplayName(role) {
        const roleMap = {
            'super_admin': 'Super Admin',
            'facilitator': 'Facilitator',
            'admin': 'Admin'
        };
        return roleMap[role] || role || 'Unknown';
    }

    formatDate(date) {
        if (!date) return 'N/A';
        return new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    openAddAdminModal() {
        // TODO: Implement add admin modal
        console.log('Opening add admin modal');
        alert('Add Admin functionality will be implemented');
    }

    viewAdmin(adminId) {
        const admin = this.admins.find(a => a.id === adminId);
        if (admin) {
            console.log('Viewing admin:', admin);
            alert(`Viewing admin: ${admin.name}`);
        }
    }

    editAdmin(adminId) {
        const admin = this.admins.find(a => a.id === adminId);
        if (admin) {
            console.log('Editing admin:', admin);
            alert(`Editing admin: ${admin.name}`);
        }
    }

    deleteAdmin(adminId) {
        const admin = this.admins.find(a => a.id === adminId);
        if (admin && confirm(`Are you sure you want to delete admin: ${admin.name}?`)) {
            console.log('Deleting admin:', admin);
            alert(`Deleting admin: ${admin.name}`);
        }
    }

    async refreshAdminData() {
        console.log('Refreshing admin data...');
        await this.loadAdminData();
    }

    showLoading() {
        const tbody = document.getElementById('adminTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">Loading...</td></tr>';
        }
    }

    hideLoading() {
        // Loading state is handled by updateAdminTable
    }

    showError(message) {
        console.error(message);
        const tbody = document.getElementById('adminTableBody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger">${message}</td></tr>`;
        }
    }
}

// Initialize Admin Manager
const adminManager = new AdminManager();