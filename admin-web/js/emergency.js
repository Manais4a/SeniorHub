// Emergency Services Management
class EmergencyManager {
    constructor() {
        this.services = [];
        this.init();
    }

    init() {
        this.setupRealtimeUpdates();
        this.loadServices();
    }

    setupRealtimeUpdates() {
        try {
            rtdb.ref('emergency_services').on('value', (snapshot) => {
                const val = snapshot.val() || {};
                this.services = Object.keys(val)
                    .map(key => ({ id: key, ...val[key] }))
                    .sort((a, b) => (b.priority_level === 'High' ? 1 : 0) - (a.priority_level === 'High' ? 1 : 0));
                this.renderServices();
            });
        } catch (e) {
            console.error('Error setting up realtime updates for emergency services:', e);
        }
    }

    async loadServices() {
        try {
            console.log('Loading emergency services from Firebase Realtime Database...');
            const snapshot = await rtdb.ref('emergency_services').get();
            const val = snapshot.val() || {};
            this.services = Object.keys(val)
                .map(key => ({ id: key, ...val[key] }))
                .sort((a, b) => (b.priority_level === 'High' ? 1 : 0) - (a.priority_level === 'High' ? 1 : 0));
            console.log('Loaded emergency services:', this.services);
            this.renderServices();
        } catch (e) {
            console.error('Error loading emergency services from RTDB:', e);
            this.services = [];
            this.renderServices();
        }
    }

    formatTime(ts) {
        if (!ts) return 'N/A';
        const d = ts.toDate ? ts.toDate() : new Date(ts);
        return d.toLocaleString();
    }

    statusBadge(isActive) {
        const cls = isActive ? 'active' : 'inactive';
        return `<span class="status-badge ${cls}">${isActive ? 'Yes' : 'No'}</span>`;
    }

    getServiceTypeDisplay(serviceType) {
        const types = {
            'EMERGENCY': 'Emergency Services',
            'MEDICAL': 'Medical Services',
            'POLICE': 'Police Services',
            'FIRE': 'Fire Services',
            'SENIOR': 'Senior Services'
        };
        return types[serviceType] || serviceType;
    }

    getPriorityDisplay(priority) {
        if (priority >= 100) return 'Critical';
        if (priority >= 50) return 'High';
        if (priority >= 10) return 'Medium';
        return 'Low';
    }

    renderServices() {
        const tbody = document.getElementById('emergencyTableBody');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!this.services.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-exclamation-triangle"></i>
                            <p>No emergency services found</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.services.forEach(service => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${service.name || 'Unknown'}</td>
                <td>${service.type || 'N/A'}</td>
                <td>${service.contact_number || 'N/A'}</td>
                <td>${service.address || 'N/A'}</td>
                <td><span class="priority-badge priority-${(service.priority_level || 'Medium').toLowerCase()}">${service.priority_level || 'Medium'}</span></td>
                <td>${this.statusBadge(service.isActive)}</td>
                <td>
                    <div class="action-buttons">
                        <div class="action-group">
                            <button class="action-btn view-btn" onclick="emergencyManager.viewService('${service.id}')" title="View Details">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="action-btn edit-btn" onclick="emergencyManager.editService('${service.id}')" title="Edit Service">
                                <i class="fas fa-edit"></i>
                            </button>
                        </div>
                        <div class="action-group">
                            <button class="action-btn delete-btn" onclick="emergencyManager.deleteService('${service.id}')" title="Delete Service">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    openAddServiceModal() {
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Service Name</label>
                    <input id="serviceName" type="text" placeholder="e.g., Central 911 Davao" />
                </div>
                <div class="form-group">
                    <label>Service Type</label>
                    <select id="serviceType">
                        <option value="EMERGENCY">Emergency Services</option>
                        <option value="MEDICAL">Medical Services</option>
                        <option value="POLICE">Police Services</option>
                        <option value="FIRE">Fire Services</option>
                        <option value="SENIOR">Senior Services</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number</label>
                    <input id="servicePhone" type="text" placeholder="e.g., (082) 222-8000" />
                </div>
                <div class="form-group">
                    <label>Address</label>
                    <input id="serviceAddress" type="text" placeholder="Full address" />
                </div>
                <div class="form-group">
                    <label>Priority Level</label>
                    <select id="servicePriority">
                        <option value="100">Critical (100)</option>
                        <option value="50">High (50)</option>
                        <option value="10">Medium (10)</option>
                        <option value="0">Low (0)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Office Hours</label>
                    <input id="serviceHours" type="text" placeholder="e.g., 24/7, Mon-Fri 8AM-5PM" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="serviceWebsite" type="text" placeholder="https://..." />
                </div>
                <div class="form-group">
                    <label>Active</label>
                    <select id="serviceActive">
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Description</label>
                    <textarea id="serviceDescription" placeholder="Service description..."></textarea>
                </div>
                <div class="form-group full-width">
                    <label>Notes</label>
                    <textarea id="serviceNotes" placeholder="Additional notes..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="emergencyManager.saveNew()"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal('Add Emergency Service', body);
    }

    async saveNew() {
        try {
            const data = this.collectFormData();
            await FirebaseUtils.addDoc(COLLECTIONS.EMERGENCY_SERVICES, data);
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to save emergency service', e);
        }
    }

    viewService(id) {
        const service = this.services.find(s => s.id === id);
        if (!service || !window.mainApp) return;
        const body = `
            <div class="detail-grid">
                <div><strong>Name:</strong> ${service.name || ''}</div>
                <div><strong>Type:</strong> ${service.type || 'N/A'}</div>
                <div><strong>Contact Number:</strong> ${service.contact_number || 'N/A'}</div>
                <div><strong>Alternate Number:</strong> ${service.alternate_number || 'N/A'}</div>
                <div><strong>Address:</strong> ${service.address || 'N/A'}</div>
                <div><strong>Operating Hours:</strong> ${service.operating_hours || 'N/A'}</div>
                <div><strong>Priority Level:</strong> <span class="priority-badge priority-${(service.priority_level || 'Medium').toLowerCase()}">${service.priority_level || 'Medium'}</span></div>
                <div><strong>Coverage Area:</strong> ${service.coverage_area || 'N/A'}</div>
                <div><strong>Active:</strong> ${service.isActive ? 'Yes' : 'No'}</div>
                <div class="full-width"><strong>Description:</strong><br>${service.description || 'No description'}</div>
                ${service.services ? `<div class="full-width"><strong>Services:</strong><br>${Array.isArray(service.services) ? service.services.join(', ') : service.services}</div>` : ''}
                ${service.createdAt ? `<div><strong>Created:</strong> ${this.formatTime(service.createdAt)}</div>` : ''}
                ${service.updatedAt ? `<div><strong>Last Updated:</strong> ${this.formatTime(service.updatedAt)}</div>` : ''}
            </div>
        `;
        mainApp.openModal('Emergency Service Details', body);
    }

    editService(id) {
        const service = this.services.find(s => s.id === id);
        if (!service || !window.mainApp) return;
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Service Name</label>
                    <input id="serviceName" type="text" value="${service.name || ''}" />
                </div>
                <div class="form-group">
                    <label>Service Type</label>
                    <select id="serviceType">
                        <option value="EMERGENCY" ${service.serviceType === 'EMERGENCY' ? 'selected' : ''}>Emergency Services</option>
                        <option value="MEDICAL" ${service.serviceType === 'MEDICAL' ? 'selected' : ''}>Medical Services</option>
                        <option value="POLICE" ${service.serviceType === 'POLICE' ? 'selected' : ''}>Police Services</option>
                        <option value="FIRE" ${service.serviceType === 'FIRE' ? 'selected' : ''}>Fire Services</option>
                        <option value="SENIOR" ${service.serviceType === 'SENIOR' ? 'selected' : ''}>Senior Services</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number</label>
                    <input id="servicePhone" type="text" value="${service.phoneNumber || ''}" />
                </div>
                <div class="form-group">
                    <label>Address</label>
                    <input id="serviceAddress" type="text" value="${service.address || ''}" />
                </div>
                <div class="form-group">
                    <label>Priority Level</label>
                    <select id="servicePriority">
                        <option value="100" ${service.priority >= 100 ? 'selected' : ''}>Critical (100)</option>
                        <option value="50" ${service.priority >= 50 && service.priority < 100 ? 'selected' : ''}>High (50)</option>
                        <option value="10" ${service.priority >= 10 && service.priority < 50 ? 'selected' : ''}>Medium (10)</option>
                        <option value="0" ${service.priority < 10 ? 'selected' : ''}>Low (0)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Office Hours</label>
                    <input id="serviceHours" type="text" value="${service.officeHours || ''}" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="serviceWebsite" type="text" value="${service.website || ''}" />
                </div>
                <div class="form-group">
                    <label>Active</label>
                    <select id="serviceActive">
                        <option value="true" ${service.isActive ? 'selected' : ''}>Yes</option>
                        <option value="false" ${!service.isActive ? 'selected' : ''}>No</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Description</label>
                    <textarea id="serviceDescription">${service.description || ''}</textarea>
                </div>
                <div class="form-group full-width">
                    <label>Notes</label>
                    <textarea id="serviceNotes">${service.notes || ''}</textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="emergencyManager.saveEdit('${service.id}')"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        mainApp.openModal('Edit Emergency Service', body);
    }

    async saveEdit(id) {
        try {
            const data = this.collectFormData();
            await rtdb.ref(`emergency_services/${id}`).update({
                ...data,
                updatedAt: new Date().toISOString()
            });
            this.showSuccess('Emergency service updated successfully');
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to update emergency service', e);
            this.showError('Failed to update emergency service: ' + e.message);
        }
    }

    async deleteService(id) {
        if (!confirm('Are you sure you want to delete this emergency service? This action cannot be undone.')) return;
        try {
            await rtdb.ref(`emergency_services/${id}`).remove();
            this.showSuccess('Emergency service deleted successfully');
        } catch (e) {
            console.error('Failed to delete emergency service', e);
            this.showError('Failed to delete emergency service: ' + e.message);
        }
    }

    collectFormData() {
        return {
            name: document.getElementById('serviceName').value.trim(),
            type: document.getElementById('serviceType').value,
            contact_number: document.getElementById('servicePhone').value.trim(),
            address: document.getElementById('serviceAddress').value.trim(),
            priority_level: document.getElementById('servicePriority').value,
            operating_hours: document.getElementById('serviceHours').value.trim(),
            website: document.getElementById('serviceWebsite').value.trim(),
            isActive: document.getElementById('serviceActive').value === 'true',
            description: document.getElementById('serviceDescription').value.trim(),
            coverage_area: document.getElementById('serviceCoverage').value.trim(),
            createdBy: currentUser().uid,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };
    }

    showSuccess(message) {
        if (window.mainApp && window.mainApp.showNotification) {
            window.mainApp.showNotification(message, 'success');
        } else {
            alert(message);
        }
    }

    showError(message) {
        if (window.mainApp && window.mainApp.showNotification) {
            window.mainApp.showNotification(message, 'error');
        } else {
            alert(message);
        }
    }
}

// Initialize and export
const emergencyManager = new EmergencyManager();
window.emergencyManager = emergencyManager;


