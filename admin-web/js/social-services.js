// Social Services Management (from Realtime Database)
class SocialServicesManager {
    constructor() {
        this.services = [];
        this.init();
    }

    init() {
        this.loadServices();
        this.setupRealtime();
    }

    setupRealtime() {
        try {
            rtdb.ref('social_services').on('value', (snapshot) => {
                const val = snapshot.val() || {};
                this.services = Object.keys(val)
                    .map(key => ({ id: key, ...val[key] }))
                    .sort((a, b) => (b.priority || 0) - (a.priority || 0));
                this.render();
            });
        } catch (e) {
            console.error('Realtime DB listener failed', e);
        }
    }

    async loadServices() {
        try {
            console.log('Loading social services from Firebase Realtime Database...');
            const snapshot = await rtdb.ref('social_services').get();
            const val = snapshot.val() || {};
            console.log('Loaded social services:', val);
            this.services = Object.keys(val)
                .map(key => ({ id: key, ...val[key] }))
                .sort((a, b) => (b.priority || 0) - (a.priority || 0));
            this.render();
        } catch (e) {
            console.error('Error loading social services', e);
            // Show error in UI
            this.services = [];
            this.render();
        }
    }

    render() {
        const tbody = document.getElementById('socialServicesTableBody');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!this.services.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-hand-holding-heart"></i>
                            <p>No social services found</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.services.forEach(s => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${s.name || ''}</td>
                <td>${s.serviceType || ''}</td>
                <td>${s.phoneNumber || 'N/A'}</td>
                <td>${s.email || 'N/A'}</td>
                <td>${s.address || 'N/A'}</td>
                <td><span class="priority-badge priority-${this.getPriorityDisplay(s.priority).toLowerCase()}">${this.getPriorityDisplay(s.priority)}</span></td>
                <td><span class="status-badge ${s.isActive ? 'active' : 'inactive'}">${s.isActive ? 'Yes' : 'No'}</span></td>
                <td>
                    <div class="action-buttons">
                        <div class="action-group">
                            <button class="action-btn view-btn" onclick="socialServicesManager.view('${s.id}')" title="View Details">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="action-btn edit-btn" onclick="socialServicesManager.edit('${s.id}')" title="Edit Information">
                                <i class="fas fa-edit"></i>
                            </button>
                        </div>
                        <div class="action-group">
                            <button class="action-btn delete-btn" onclick="socialServicesManager.delete('${s.id}')" title="Delete Service">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    getPriorityDisplay(priority) {
        if (priority >= 100) return 'Critical';
        if (priority >= 50) return 'High';
        if (priority >= 10) return 'Medium';
        return 'Low';
    }

    openAddServiceModal() {
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Name</label>
                    <input id="svcName" type="text" placeholder="Service name" />
                </div>
                <div class="form-group">
                    <label>Service Type</label>
                    <select id="svcType">
                        <option value="GOVERNMENT">Government</option>
                        <option value="BARANGAY">Barangay</option>
                        <option value="NGO">NGO</option>
                        <option value="PRIVATE">Private</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number</label>
                    <input id="svcPhone" type="text" placeholder="e.g., (082) 222-8000" />
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input id="svcEmail" type="email" placeholder="email@example.com" />
                </div>
                <div class="form-group full-width">
                    <label>Contact</label>
                    <input id="svcContact" type="text" placeholder="Contact person or department" />
                </div>
                <div class="form-group full-width">
                    <label>Address</label>
                    <input id="svcAddress" type="text" placeholder="Full address" />
                </div>
                <div class="form-group">
                    <label>Office Hours</label>
                    <input id="svcHours" type="text" placeholder="e.g., Mon-Fri 8AM-5PM" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="svcWebsite" type="text" placeholder="https://..." />
                </div>
                <div class="form-group">
                    <label>Priority</label>
                    <select id="svcPriority">
                        <option value="100">Critical (100)</option>
                        <option value="50">High (50)</option>
                        <option value="10">Medium (10)</option>
                        <option value="0">Low (0)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Active</label>
                    <select id="svcActive">
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Services Offered</label>
                    <textarea id="svcOffered" placeholder="List services offered (one per line)"></textarea>
                </div>
                <div class="form-group full-width">
                    <label>Notes</label>
                    <textarea id="svcNotes" placeholder="Additional notes..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="socialServicesManager.saveNew()"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal('Add Social Service', body);
    }

    async saveNew() {
        try {
            const data = this.collectFormData();
            const newRef = rtdb.ref('social_services').push();
            await newRef.set(data);
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to add service', e);
        }
    }

    view(id) {
        const s = this.services.find(x => x.id === id);
        if (!s || !window.mainApp) return;
        const body = `
            <div class="detail-grid">
                <div><strong>Name:</strong> ${s.name || ''}</div>
                <div><strong>Service Type:</strong> ${s.serviceType || ''}</div>
                <div><strong>Phone Number:</strong> ${s.phoneNumber || 'N/A'}</div>
                <div><strong>Email:</strong> ${s.email || 'N/A'}</div>
                <div class="full-width"><strong>Contact:</strong> ${s.contact || 'N/A'}</div>
                <div class="full-width"><strong>Address:</strong> ${s.address || 'N/A'}</div>
                <div><strong>Office Hours:</strong> ${s.officeHours || 'N/A'}</div>
                <div><strong>Website:</strong> ${s.website || 'N/A'}</div>
                <div><strong>Priority:</strong> ${this.getPriorityDisplay(s.priority)}</div>
                <div><strong>Active:</strong> ${s.isActive ? 'Yes' : 'No'}</div>
                <div class="full-width"><strong>Services Offered:</strong><br>${(s.servicesOffered || '').replace(/\n/g,'<br>')}</div>
                <div class="full-width"><strong>Notes:</strong><br>${s.notes || ''}</div>
            </div>
        `;
        mainApp.openModal('Social Service Details', body);
    }

    edit(id) {
        const s = this.services.find(x => x.id === id);
        if (!s || !window.mainApp) return;
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Name</label>
                    <input id="svcName" type="text" value="${s.name || ''}" />
                </div>
                <div class="form-group">
                    <label>Service Type</label>
                    <select id="svcType">
                        <option value="GOVERNMENT" ${s.serviceType === 'GOVERNMENT' ? 'selected' : ''}>Government</option>
                        <option value="BARANGAY" ${s.serviceType === 'BARANGAY' ? 'selected' : ''}>Barangay</option>
                        <option value="NGO" ${s.serviceType === 'NGO' ? 'selected' : ''}>NGO</option>
                        <option value="PRIVATE" ${s.serviceType === 'PRIVATE' ? 'selected' : ''}>Private</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number</label>
                    <input id="svcPhone" type="text" value="${s.phoneNumber || ''}" />
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input id="svcEmail" type="email" value="${s.email || ''}" />
                </div>
                <div class="form-group full-width">
                    <label>Contact</label>
                    <input id="svcContact" type="text" value="${s.contact || ''}" />
                </div>
                <div class="form-group full-width">
                    <label>Address</label>
                    <input id="svcAddress" type="text" value="${s.address || ''}" />
                </div>
                <div class="form-group">
                    <label>Office Hours</label>
                    <input id="svcHours" type="text" value="${s.officeHours || ''}" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="svcWebsite" type="text" value="${s.website || ''}" />
                </div>
                <div class="form-group">
                    <label>Priority</label>
                    <select id="svcPriority">
                        <option value="100" ${s.priority >= 100 ? 'selected' : ''}>Critical (100)</option>
                        <option value="50" ${s.priority >= 50 && s.priority < 100 ? 'selected' : ''}>High (50)</option>
                        <option value="10" ${s.priority >= 10 && s.priority < 50 ? 'selected' : ''}>Medium (10)</option>
                        <option value="0" ${s.priority < 10 ? 'selected' : ''}>Low (0)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Active</label>
                    <select id="svcActive">
                        <option value="true" ${s.isActive ? 'selected' : ''}>Yes</option>
                        <option value="false" ${!s.isActive ? 'selected' : ''}>No</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Services Offered</label>
                    <textarea id="svcOffered">${s.servicesOffered || ''}</textarea>
                </div>
                <div class="form-group full-width">
                    <label>Notes</label>
                    <textarea id="svcNotes">${s.notes || ''}</textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="socialServicesManager.saveEdit('${s.id}')"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        mainApp.openModal('Edit Social Service', body);
    }

    async saveEdit(id) {
        try {
            const data = this.collectFormData();
            await rtdb.ref('social_services').child(id).update(data);
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to update service', e);
        }
    }

    async delete(id) {
        if (!confirm('Delete this social service?')) return;
        try {
            await rtdb.ref('social_services').child(id).remove();
        } catch (e) {
            console.error('Failed to delete service', e);
        }
    }

    collectFormData() {
        return {
            name: document.getElementById('svcName').value.trim(),
            serviceType: document.getElementById('svcType').value,
            phoneNumber: document.getElementById('svcPhone').value.trim(),
            email: document.getElementById('svcEmail').value.trim(),
            contact: document.getElementById('svcContact').value.trim(),
            address: document.getElementById('svcAddress').value.trim(),
            officeHours: document.getElementById('svcHours').value.trim(),
            website: document.getElementById('svcWebsite').value.trim(),
            priority: Number(document.getElementById('svcPriority').value) || 0,
            isActive: document.getElementById('svcActive').value === 'true',
            servicesOffered: document.getElementById('svcOffered').value.trim(),
            notes: document.getElementById('svcNotes').value.trim()
        };
    }
}

const socialServicesManager = new SocialServicesManager();
window.socialServicesManager = socialServicesManager;


