// Emergency Alerts Management
class EmergencyAlertsManager {
    constructor() {
        this.alerts = [];
        this.init();
    }

    init() {
        this.setupFirestoreListener();
    }

    setupFirestoreListener() {
        try {
            if (typeof db === 'undefined') {
                console.warn('Firestore not available yet, retrying in 1 second...');
                setTimeout(() => this.setupFirestoreListener(), 1000);
                return;
            }

            db.collection(COLLECTIONS.EMERGENCY_ALERTS)
                .orderBy('timestamp', 'desc')
                .onSnapshot(async (snapshot) => {
                    const alerts = [];
                    for (const doc of snapshot.docs) {
                        const data = doc.data();
                        const alert = { id: doc.id, ...data };
                        // Enrich with senior data from users/seniors collection
                        const userId = alert.seniorId || alert.userId || '';
                        if (userId) {
                            // Try seniors collection first, then users
                            const seniorDoc = await db.collection(COLLECTIONS.SENIORS).doc(userId).get();
                            const userDoc = seniorDoc.exists ? null : await db.collection(COLLECTIONS.USERS).doc(userId).get();
                            const profile = seniorDoc.exists ? seniorDoc.data() : (userDoc && userDoc.exists ? userDoc.data() : null);
                            if (profile) {
                                // Set senior name
                                alert.userName = profile.fullName || profile.name || profile.displayName || alert.seniorName || alert.userId;
                                
                                // Set senior address if not already set
                                if (!alert.location || !alert.location.address) {
                                    const addressParts = [profile.address, profile.barangay, profile.city, profile.province]
                                        .filter(Boolean)
                                        .join(', ');
                                    alert.location = alert.location || {};
                                    alert.location.address = addressParts || alert.location.address || 'N/A';
                                }
                                
                                // Add senior contact information
                                alert.seniorPhone = profile.phoneNumber || profile.phone || 'N/A';
                                alert.seniorEmail = profile.email || 'N/A';
                                alert.seniorAge = profile.age || 'N/A';
                                alert.seniorGender = profile.gender || 'N/A';
                                
                                // Add emergency contact information
                                if (profile.emergencyContacts && profile.emergencyContacts.length > 0) {
                                    const primaryContact = profile.emergencyContacts[0];
                                    alert.emergencyContactName = primaryContact.name || 'N/A';
                                    alert.emergencyContactPhone = primaryContact.phoneNumber || primaryContact.phone || 'N/A';
                                    alert.emergencyContactRelationship = primaryContact.relationship || 'N/A';
                                }
                            }
                        }
                        alerts.push(alert);
                    }
                    this.alerts = alerts;
                    this.renderAlerts();
                }, (err) => console.error('Alerts listener error:', err));
        } catch (e) {
            console.error('Error setting up Firestore listener for emergency alerts:', e);
        }
    }

    async loadAlerts() { /* no-op now; using Firestore realtime listener */ }

    formatTime(timestamp) {
        if (!timestamp) return 'N/A';
        const date = new Date(timestamp);
        return date.toLocaleString();
    }

    getSeverityBadge(severity) {
        const severityMap = {
            'LOW': 'severity-low',
            'MEDIUM': 'severity-medium', 
            'HIGH': 'severity-high',
            'CRITICAL': 'severity-critical',
            'LIFE_THREATENING': 'severity-life-threatening'
        };
        return severityMap[severity] || 'severity-medium';
    }

    getStatusBadge(status) {
        const statusMap = {
            'ACTIVE': 'status-active',
            'ACKNOWLEDGED': 'status-acknowledged',
            'IN_PROGRESS': 'status-in-progress',
            'RESOLVED': 'status-resolved',
            'CANCELLED': 'status-cancelled',
            'FALSE_ALARM': 'status-false-alarm'
        };
        return statusMap[status] || 'status-active';
    }

    getTypeDisplay(type) {
        const typeMap = {
            'MEDICAL_EMERGENCY': 'Medical Emergency',
            'FALL_DETECTED': 'Fall Detected',
            'HEART_ATTACK': 'Heart Attack',
            'STROKE': 'Stroke',
            'DIABETIC_EMERGENCY': 'Diabetic Emergency',
            'RESPIRATORY_DISTRESS': 'Respiratory Distress',
            'SEIZURE': 'Seizure',
            'UNCONSCIOUS': 'Unconscious',
            'SEVERE_PAIN': 'Severe Pain',
            'ACCIDENT': 'Accident',
            'FIRE': 'Fire',
            'INTRUDER': 'Intruder',
            'NATURAL_DISASTER': 'Natural Disaster',
            'TECHNICAL_ISSUE': 'Technical Issue',
            'LOST_OR_CONFUSED': 'Lost or Confused',
            'MEDICATION_OVERDOSE': 'Medication Overdose',
            'DEHYDRATION': 'Dehydration',
            'HYPOTHERMIA': 'Hypothermia',
            'HYPERTHERMIA': 'Hyperthermia',
            'OTHER': 'Other'
        };
        return typeMap[type] || type;
    }

    getLocationDisplay(location) {
        if (!location) return 'N/A';
        if (typeof location === 'string') return location;
        if (location.address) return location.address;
        if (location.city && location.province) {
            return `${location.city}, ${location.province}`;
        }
        return 'Location not specified';
    }

    getSeniorName(userId) {
        // This would typically fetch from a seniors collection
        // For now, we'll use a simple mapping or return the userId
        const seniorNames = {
            'senior_001': 'Maria Santos',
            'senior_002': 'Juan Cruz',
            'senior_003': 'Ana Rodriguez',
            'senior_004': 'Pedro Martinez',
            'senior_005': 'Carmen Lopez',
            'senior_006': 'Roberto Garcia',
            'senior_007': 'Elena Fernandez',
            'senior_008': 'Miguel Torres'
        };
        return seniorNames[userId] || userId || 'Unknown Senior';
    }

    getEmergencyServiceName(alert) {
        // Get the first contacted service name
        if (alert.contactedServices && alert.contactedServices.length > 0) {
            const serviceId = alert.contactedServices[0];
            const serviceNames = {
                'emergency_001': 'Davao City Emergency Response Unit',
                'emergency_002': 'Davao City Police Station',
                'emergency_003': 'Davao Doctors Hospital',
                'emergency_004': 'Davao City Fire Department'
            };
            return serviceNames[serviceId] || serviceId;
        }
        return 'No Service Contacted';
    }

    getEmergencyServiceType(alert) {
        // Map alert type to service type
        const typeMapping = {
            'FALL_DETECTED': 'Medical Emergency',
            'MEDICAL_EMERGENCY': 'Medical Emergency',
            'HEART_ATTACK': 'Medical Emergency',
            'STROKE': 'Medical Emergency',
            'DIABETIC_EMERGENCY': 'Medical Emergency',
            'RESPIRATORY_DISTRESS': 'Medical Emergency',
            'SEIZURE': 'Medical Emergency',
            'UNCONSCIOUS': 'Medical Emergency',
            'SEVERE_PAIN': 'Medical Emergency',
            'ACCIDENT': 'Emergency Response',
            'FIRE': 'Fire Department',
            'INTRUDER': 'Police Services',
            'NATURAL_DISASTER': 'Disaster Response',
            'TECHNICAL_ISSUE': 'Technical Support',
            'LOST_OR_CONFUSED': 'Police Services',
            'MEDICATION_OVERDOSE': 'Medical Emergency',
            'DEHYDRATION': 'Medical Emergency',
            'HYPOTHERMIA': 'Medical Emergency',
            'HYPERTHERMIA': 'Medical Emergency',
            'OTHER': 'General Emergency'
        };
        return typeMapping[alert.type] || 'General Emergency';
    }

    renderAlerts() {
        console.log('Rendering emergency alerts...', this.alerts.length, 'alerts found');
        const tbody = document.getElementById('emergencyAlertsTableBody');
        if (!tbody) {
            console.error('Emergency alerts table body not found!');
            return;
        }
        tbody.innerHTML = '';

        if (!this.alerts.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-bell-slash"></i>
                            <p>No emergency alerts found</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.alerts.forEach(alert => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div class="senior-info">
                        <strong>${alert.userName || this.getSeniorName(alert.userId)}</strong>
                        <br>
                        <small class="text-muted">ID: ${alert.userId || 'N/A'}</small>
                        ${alert.seniorPhone ? `<br><small class="text-muted">Phone: ${alert.seniorPhone}</small>` : ''}
                        ${alert.seniorAge ? `<br><small class="text-muted">Age: ${alert.seniorAge}</small>` : ''}
                    </div>
                </td>
                <td>
                    <div class="service-info">
                        <strong>${alert.serviceName || this.getEmergencyServiceName(alert)}</strong>
                        ${alert.servicePhone ? `<br><small class="text-muted">Phone: ${alert.servicePhone}</small>` : ''}
                    </div>
                </td>
                <td>
                    <div class="address-info">
                        ${this.getLocationDisplay(alert.location)}
                        ${alert.emergencyContactName ? `<br><small class="text-muted">Emergency Contact: ${alert.emergencyContactName} (${alert.emergencyContactPhone})</small>` : ''}
                    </div>
                </td>
                <td>
                    <div class="timestamp-info">
                        ${this.formatTime(alert.timestamp)}
                        <br>
                        <small class="text-muted">
                            <span class="status-badge ${this.getStatusBadge(alert.status)}">${alert.status || 'ACTIVE'}</span>
                        </small>
                    </div>
                </td>
                <td>
                    <div class="action-buttons">
                        <div class="action-group">
                            <button class="action-btn view-btn" onclick="emergencyAlertsManager.viewAlert('${alert.id}')" title="View Details">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="action-btn edit-btn" onclick="emergencyAlertsManager.editAlert('${alert.id}')" title="Update Status">
                                <i class="fas fa-edit"></i>
                            </button>
                        </div>
                        <div class="action-group">
                            <button class="action-btn resolve-btn" onclick="emergencyAlertsManager.resolveAlert('${alert.id}')" title="Mark as Resolved">
                                <i class="fas fa-check"></i>
                            </button>
                        </div>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    async viewAlert(alertId) {
        const alert = this.alerts.find(a => a.id === alertId);
        if (!alert) {
            showNotification('Alert not found', 'error');
            return;
        }

        const modalTitle = document.getElementById('modalTitle');
        const modalBody = document.getElementById('modalBody');
        
        modalTitle.textContent = `Emergency Alert Details - ${alert.id}`;
        modalBody.innerHTML = `
            <div class="alert-details">
                <h4>Emergency Alert Information</h4>
                <div class="detail-row">
                    <label>Alert ID:</label>
                    <span>${alert.id}</span>
                </div>
                <div class="detail-row">
                    <label>Type:</label>
                    <span>${this.getTypeDisplay(alert.type)}</span>
                </div>
                <div class="detail-row">
                    <label>Severity:</label>
                    <span class="severity-badge ${this.getSeverityBadge(alert.severity)}">${alert.severity}</span>
                </div>
                <div class="detail-row">
                    <label>Status:</label>
                    <span class="status-badge ${this.getStatusBadge(alert.status)}">${alert.status}</span>
                </div>
                <div class="detail-row">
                    <label>Timestamp:</label>
                    <span>${this.formatTime(alert.timestamp)}</span>
                </div>
                <div class="detail-row">
                    <label>Triggered By:</label>
                    <span>${alert.triggeredBy || 'Unknown'}</span>
                </div>
                
                <h4>Senior Citizen Information</h4>
                <div class="detail-row">
                    <label>Name:</label>
                    <span>${alert.userName || alert.seniorName || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>User ID:</label>
                    <span>${alert.userId || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Phone:</label>
                    <span>${alert.seniorPhone || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Email:</label>
                    <span>${alert.seniorEmail || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Age:</label>
                    <span>${alert.seniorAge || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Gender:</label>
                    <span>${alert.seniorGender || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Location:</label>
                    <span>${this.getLocationDisplay(alert.location)}</span>
                </div>
                
                <h4>Emergency Contact Information</h4>
                <div class="detail-row">
                    <label>Contact Name:</label>
                    <span>${alert.emergencyContactName || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Contact Phone:</label>
                    <span>${alert.emergencyContactPhone || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Relationship:</label>
                    <span>${alert.emergencyContactRelationship || 'N/A'}</span>
                </div>
                
                <h4>Emergency Service Information</h4>
                <div class="detail-row">
                    <label>Service Name:</label>
                    <span>${alert.serviceName || this.getEmergencyServiceName(alert)}</span>
                </div>
                <div class="detail-row">
                    <label>Service Phone:</label>
                    <span>${alert.servicePhone || 'N/A'}</span>
                </div>
                
                <h4>Alert Details</h4>
                <div class="detail-row">
                    <label>Description:</label>
                    <span>${alert.description || 'No description provided'}</span>
                </div>
                <div class="detail-row">
                    <label>Message Sent:</label>
                    <span>${alert.message || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Notes:</label>
                    <span>${alert.notes || 'No notes'}</span>
                </div>
                <div class="detail-row">
                    <label>Response Time:</label>
                    <span>${alert.responseTime ? `${alert.responseTime}ms` : 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Resolved At:</label>
                    <span>${alert.resolvedAt ? this.formatTime(alert.resolvedAt) : 'Not resolved'}</span>
                </div>
                <div class="detail-row">
                    <label>Resolved By:</label>
                    <span>${alert.resolvedBy || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Resolution Notes:</label>
                    <span>${alert.resolutionNotes || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <label>Follow-up Required:</label>
                    <span>${alert.followUpRequired ? 'Yes' : 'No'}</span>
                </div>
                ${alert.followUpDate ? `
                <div class="detail-row">
                    <label>Follow-up Date:</label>
                    <span>${this.formatTime(alert.followUpDate)}</span>
                </div>
                ` : ''}
            </div>
        `;
        
        showModal();
    }

    async editAlert(alertId) {
        const alert = this.alerts.find(a => a.id === alertId);
        if (!alert) {
            showNotification('Alert not found', 'error');
            return;
        }

        const modalTitle = document.getElementById('modalTitle');
        const modalBody = document.getElementById('modalBody');
        
        modalTitle.textContent = `Update Emergency Alert - ${alert.id}`;
        modalBody.innerHTML = `
            <form id="editAlertForm" class="form">
                <div class="form-group">
                    <label for="alertStatus">Status:</label>
                    <select id="alertStatus" name="status" required>
                        <option value="ACTIVE" ${alert.status === 'ACTIVE' ? 'selected' : ''}>Active</option>
                        <option value="ACKNOWLEDGED" ${alert.status === 'ACKNOWLEDGED' ? 'selected' : ''}>Acknowledged</option>
                        <option value="IN_PROGRESS" ${alert.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                        <option value="RESOLVED" ${alert.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
                        <option value="CANCELLED" ${alert.status === 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                        <option value="FALSE_ALARM" ${alert.status === 'FALSE_ALARM' ? 'selected' : ''}>False Alarm</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="alertNotes">Notes:</label>
                    <textarea id="alertNotes" name="notes" rows="3" placeholder="Add notes about this alert...">${alert.notes || ''}</textarea>
                </div>
                <div class="form-group">
                    <label for="resolutionNotes">Resolution Notes:</label>
                    <textarea id="resolutionNotes" name="resolutionNotes" rows="3" placeholder="Add resolution notes...">${alert.resolutionNotes || ''}</textarea>
                </div>
                <div class="form-group">
                    <label>
                        <input type="checkbox" id="followUpRequired" name="followUpRequired" ${alert.followUpRequired ? 'checked' : ''}>
                        Follow-up Required
                    </label>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Update Alert</button>
                </div>
            </form>
        `;
        
        showModal();
        
        // Handle form submission
        document.getElementById('editAlertForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.updateAlert(alertId);
        });
    }

    async updateAlert(alertId) {
        try {
            const form = document.getElementById('editAlertForm');
            const formData = new FormData(form);
            
            const updates = {
                status: formData.get('status'),
                notes: formData.get('notes'),
                resolutionNotes: formData.get('resolutionNotes'),
                followUpRequired: formData.get('followUpRequired') === 'on',
                updatedAt: Date.now()
            };

            if (updates.status === 'RESOLVED') {
                updates.resolvedAt = Date.now();
                updates.resolvedBy = getCurrentUser()?.uid || 'admin';
            }

            await rtdb.ref(`emergency_alerts/${alertId}`).update(updates);
            
            showNotification('Alert updated successfully', 'success');
            closeModal();
            this.loadAlerts();
        } catch (error) {
            console.error('Error updating alert:', error);
            showNotification('Failed to update alert', 'error');
        }
    }

    async resolveAlert(alertId) {
        try {
            const updates = {
                status: 'RESOLVED',
                resolvedAt: Date.now(),
                resolvedBy: getCurrentUser()?.uid || 'admin',
                updatedAt: Date.now()
            };

            await rtdb.ref(`emergency_alerts/${alertId}`).update(updates);
            
            showNotification('Alert marked as resolved', 'success');
            this.loadAlerts();
        } catch (error) {
            console.error('Error resolving alert:', error);
            showNotification('Failed to resolve alert', 'error');
        }
    }

    async openAddAlertModal() {
        const modalTitle = document.getElementById('modalTitle');
        const modalBody = document.getElementById('modalBody');
        
        modalTitle.textContent = 'Add New Emergency Alert';
        modalBody.innerHTML = `
            <form id="addAlertForm" class="form">
                <div class="form-group">
                    <label for="alertType">Alert Type:</label>
                    <select id="alertType" name="type" required>
                        <option value="">Select Alert Type</option>
                        <option value="MEDICAL_EMERGENCY">Medical Emergency</option>
                        <option value="FALL_DETECTED">Fall Detected</option>
                        <option value="HEART_ATTACK">Heart Attack</option>
                        <option value="STROKE">Stroke</option>
                        <option value="DIABETIC_EMERGENCY">Diabetic Emergency</option>
                        <option value="RESPIRATORY_DISTRESS">Respiratory Distress</option>
                        <option value="SEIZURE">Seizure</option>
                        <option value="UNCONSCIOUS">Unconscious</option>
                        <option value="SEVERE_PAIN">Severe Pain</option>
                        <option value="ACCIDENT">Accident</option>
                        <option value="FIRE">Fire</option>
                        <option value="INTRUDER">Intruder</option>
                        <option value="NATURAL_DISASTER">Natural Disaster</option>
                        <option value="TECHNICAL_ISSUE">Technical Issue</option>
                        <option value="LOST_OR_CONFUSED">Lost or Confused</option>
                        <option value="MEDICATION_OVERDOSE">Medication Overdose</option>
                        <option value="DEHYDRATION">Dehydration</option>
                        <option value="HYPOTHERMIA">Hypothermia</option>
                        <option value="HYPERTHERMIA">Hyperthermia</option>
                        <option value="OTHER">Other</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="alertSeverity">Severity:</label>
                    <select id="alertSeverity" name="severity" required>
                        <option value="LOW">Low</option>
                        <option value="MEDIUM" selected>Medium</option>
                        <option value="HIGH">High</option>
                        <option value="CRITICAL">Critical</option>
                        <option value="LIFE_THREATENING">Life Threatening</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="alertUserId">User ID:</label>
                    <input type="text" id="alertUserId" name="userId" required placeholder="Enter user ID">
                </div>
                <div class="form-group">
                    <label for="alertDescription">Description:</label>
                    <textarea id="alertDescription" name="description" rows="3" placeholder="Describe the emergency situation..."></textarea>
                </div>
                <div class="form-group">
                    <label for="alertLocation">Location:</label>
                    <input type="text" id="alertLocation" name="location" placeholder="Enter location address">
                </div>
                <div class="form-group">
                    <label for="alertTriggeredBy">Triggered By:</label>
                    <select id="alertTriggeredBy" name="triggeredBy">
                        <option value="user">User</option>
                        <option value="system">System</option>
                        <option value="family">Family</option>
                        <option value="health_monitor">Health Monitor</option>
                        <option value="admin">Admin</option>
                    </select>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Alert</button>
                </div>
            </form>
        `;
        
        showModal();
        
        // Handle form submission
        document.getElementById('addAlertForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.addAlert();
        });
    }

    async addAlert() {
        try {
            const form = document.getElementById('addAlertForm');
            const formData = new FormData(form);
            
            const alertData = {
                type: formData.get('type'),
                severity: formData.get('severity'),
                status: 'ACTIVE',
                userId: formData.get('userId'),
                description: formData.get('description'),
                location: formData.get('location'),
                triggeredBy: formData.get('triggeredBy'),
                timestamp: Date.now(),
                createdAt: Date.now(),
                updatedAt: Date.now()
            };

            const newAlertRef = rtdb.ref('emergency_alerts').push();
            await newAlertRef.set(alertData);
            
            showNotification('Emergency alert created successfully', 'success');
            closeModal();
            this.loadAlerts();
        } catch (error) {
            console.error('Error creating alert:', error);
            showNotification('Failed to create alert', 'error');
        }
    }

    async refreshAlerts() {
        showNotification('Refreshing alerts...', 'info');
        await this.loadAlerts();
    }

    // Test function to verify the section is working
    testEmergencyAlerts() {
        console.log('Testing Emergency Alerts section...');
        console.log('Manager available:', !!window.emergencyAlertsManager);
        console.log('Table body element:', document.getElementById('emergencyAlertsTableBody'));
        console.log('Current alerts:', this.alerts.length);
        this.renderAlerts();
    }
}

// Initialize Emergency Alerts Manager
const emergencyAlertsManager = new EmergencyAlertsManager();

// Make it globally available
window.emergencyAlertsManager = emergencyAlertsManager;

// Global test function
window.testEmergencyAlerts = () => emergencyAlertsManager.testEmergencyAlerts();
