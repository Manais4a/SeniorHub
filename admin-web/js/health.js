// Health Records Management - Aligned with Android App
class HealthManager {
    constructor() {
        this.records = [];
        this.recordsFS = [];
        this.recordsRTDB = [];
        this.healthSummary = {};
        this.seniorsData = []; // Store seniors data for name lookup
        this.init();
    }

    init() {
        this.setupRealtimeUpdates();
        this.setupRealtimeRTDB();
        this.loadHealthRecords();
        this.loadHealthRecordsRTDB();
        this.loadSeniorsData(); // Load seniors data for name lookup
    }

    setupRealtimeUpdates() {
        FirebaseUtils.onSnapshot(COLLECTIONS.HEALTH_RECORDS, async (snapshot) => {
            const items = [];
            snapshot.forEach(doc => items.push({ id: doc.id, source: 'fs', ...doc.data() }));
            this.recordsFS = items.sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp));
            await this.combineAndRender();
        });
    }

    setupRealtimeRTDB() {
        try {
            rtdb.ref('health_records').on('value', async (snap) => {
                const val = snap.val() || {};
                const items = Object.keys(val).map(key => ({
                    id: `rtdb:${key}`,
                    source: 'rtdb',
                    ...val[key]
                }));
                this.recordsRTDB = items.sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp));
                await this.combineAndRender();
            });
        } catch (e) {
            console.error('Health RTDB realtime failed', e);
        }
    }

    async loadHealthRecords() {
        try {
            console.log('Loading health records from Firebase...');
            const items = await FirebaseUtils.getCollection(COLLECTIONS.HEALTH_RECORDS, 'timestamp');
            console.log('Loaded health records:', items);
            this.recordsFS = (items || []).map(x => ({ id: x.id, source: 'fs', ...x }))
                .sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp));
            this.combineAndRender();
        } catch (e) {
            console.error('Error loading health records', e);
            // Show error in UI
            this.recordsFS = [];
            this.combineAndRender();
        }
    }

    async loadHealthRecordsRTDB() {
        try {
            const snap = await rtdb.ref('health_records').get();
            const val = snap.val() || {};
            const items = Object.keys(val).map(key => ({ id: `rtdb:${key}`, source: 'rtdb', ...val[key] }));
            this.recordsRTDB = items.sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp));
            this.combineAndRender();
        } catch (e) {
            console.error('Error loading RTDB health records', e);
        }
    }

    async combineAndRender() {
        this.records = [...this.recordsFS, ...this.recordsRTDB]
            .sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp));
        await this.render();
    }

    async loadSeniorsData() {
        try {
            console.log('Loading seniors data for health records...');
            // Load seniors from the same source as the seniors manager
            const users = await FirebaseUtils.getCollection('users');
            const seniorCitizens = users.filter(user => {
                const role = (user.role || '').toLowerCase().trim();
                const age = Number(user.age || 0);
                return (role === 'senior_citizen' || role === 'senior' || role === 'citizen') || 
                       (age >= 60 && !role.includes('admin'));
            });
            
            this.seniorsData = seniorCitizens.map(senior => ({
                id: senior.id,
                firstName: senior.firstName || '',
                lastName: senior.lastName || '',
                fullName: `${senior.firstName || ''} ${senior.lastName || ''}`.trim(),
                email: senior.email || '',
                phoneNumber: senior.phoneNumber || ''
            }));
            
            console.log('Loaded seniors data:', this.seniorsData.length, 'seniors');
        } catch (e) {
            console.error('Error loading seniors data:', e);
            this.seniorsData = [];
        }
    }

    getSeniorName(seniorId) {
        if (!seniorId || seniorId === 'unknown') {
            return 'Unknown Senior';
        }
        
        const senior = this.seniorsData.find(s => s.id === seniorId);
        if (senior && senior.fullName) {
            return senior.fullName;
        }
        
        // Fallback: try to find by email or other identifiers
        const seniorByEmail = this.seniorsData.find(s => s.email === seniorId);
        if (seniorByEmail && seniorByEmail.fullName) {
            return seniorByEmail.fullName;
        }
        
        return 'Unknown Senior';
    }



    getTime(ts) {
        if (!ts) return 0;
        if (ts.toDate) return ts.toDate().getTime();
        if (typeof ts === 'number') return ts;
        return new Date(ts).getTime();
    }

    formatTime(ts) {
        if (!ts) return 'N/A';
        const d = ts.toDate ? ts.toDate() : new Date(ts);
        return d.toLocaleString();
    }

    async render() {
        // Ensure seniors data is loaded before rendering
        await this.ensureSeniorsDataLoaded();
        this.renderHealthRecordsTable();
    }


    renderHealthRecordsTable() {
        const tbody = document.getElementById('healthTableBody');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!this.records.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-heartbeat" style="font-size: 3rem; color: #ccc; margin-bottom: 1rem;"></i>
                            <p style="color: #666; font-size: 1.1rem;">No health records yet</p>
                            <p style="color: #999; font-size: 0.9rem;">Health records from the senior app will appear here</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        // Group records by senior to show latest values for each health metric
        const seniorGroups = this.groupRecordsBySenior(this.records);
        
        // Render one row per senior with their latest health values
        Object.keys(seniorGroups).forEach(seniorId => {
            const seniorRecords = seniorGroups[seniorId];
            const seniorName = this.getSeniorName(seniorId);
            
            const tr = document.createElement('tr');
            tr.style.backgroundColor = '#fafafa';
            tr.style.borderBottom = '1px solid #e0e0e0';
            tr.style.borderLeft = '4px solid #4caf50';
            
            // Get latest values for each health type
            const latestBloodPressure = seniorRecords.find(r => r.type === 'blood_pressure');
            const latestBloodSugar = seniorRecords.find(r => r.type === 'blood_sugar');
            const latestWeight = seniorRecords.find(r => r.type === 'weight');
            const latestHeartRate = seniorRecords.find(r => r.type === 'heart_rate');
            
            // Get the most recent record for timestamp
            const mostRecentRecord = seniorRecords.sort((a, b) => this.getTime(b.timestamp) - this.getTime(a.timestamp))[0];
            
            tr.innerHTML = `
                <td style="padding: 15px 10px; font-weight: 500; color: #333;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <i class="fas fa-user" style="color: #666;"></i>
                        <span>${seniorName}</span>
                    </div>
                    <div style="font-size: 0.8rem; color: #999; margin-top: 2px;">
                        ID: ${seniorId}
                    </div>
                </td>
                <td style="padding: 15px 10px; text-align: center;">
                    ${latestBloodPressure ? this.renderHealthValue(latestBloodPressure, 'blood_pressure') : 
                      '<span style="color: #ccc; font-style: italic;">-</span>'}
                </td>
                <td style="padding: 15px 10px; text-align: center;">
                    ${latestBloodSugar ? this.renderHealthValue(latestBloodSugar, 'blood_sugar') : 
                      '<span style="color: #ccc; font-style: italic;">-</span>'}
                </td>
                <td style="padding: 15px 10px; text-align: center;">
                    ${latestWeight ? this.renderHealthValue(latestWeight, 'weight') : 
                      '<span style="color: #ccc; font-style: italic;">-</span>'}
                </td>
                <td style="padding: 15px 10px; text-align: center;">
                    ${latestHeartRate ? this.renderHealthValue(latestHeartRate, 'heart_rate') : 
                      '<span style="color: #ccc; font-style: italic;">-</span>'}
                </td>
                <td style="padding: 15px 10px; color: #666; font-size: 0.9rem;">
                    ${this.formatDate(mostRecentRecord.timestamp)}
                </td>
                <td style="padding: 15px 10px; color: #666; font-size: 0.9rem;">
                    ${this.formatTime(mostRecentRecord.timestamp)}
                </td>
                <td style="padding: 15px 10px;">
                    <div class="action-buttons">
                        <button class="action-btn view-btn" onclick="healthManager.viewSeniorRecords('${seniorId}')" title="View All Records" style="background: #4caf50; color: white; border: none; padding: 8px 12px; border-radius: 6px; margin: 2px;">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="action-btn add-btn" onclick="healthManager.addRecordForSenior('${seniorId}', '${seniorName}')" title="Add Record" style="background: #ff9800; color: white; border: none; padding: 8px 12px; border-radius: 6px; margin: 2px;">
                            <i class="fas fa-plus"></i>
                        </button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    groupRecordsBySenior(records) {
        const groups = {};
        records.forEach(record => {
            const seniorId = record.seniorId || 'unknown';
            if (!groups[seniorId]) {
                groups[seniorId] = [];
            }
            groups[seniorId].push(record);
        });
        return groups;
    }

    viewSeniorRecords(seniorId) {
        const seniorRecords = this.records.filter(r => r.seniorId === seniorId);
        if (!seniorRecords.length || !window.mainApp) return;
        
        const seniorName = this.getSeniorName(seniorId);
        
        // Group by type and get latest for each
        const latestByType = {};
        seniorRecords.forEach(record => {
            if (!latestByType[record.type] || this.getTime(record.timestamp) > this.getTime(latestByType[record.type].timestamp)) {
                latestByType[record.type] = record;
            }
        });
        
        const body = `
            <div class="senior-health-summary">
                <div class="senior-header" style="display: flex; align-items: center; margin-bottom: 1.5rem; padding: 1rem; background: #f8f9fa; border-radius: 8px;">
                    <div style="font-size: 3rem; margin-right: 1rem;">👴</div>
                    <div>
                        <h3 style="margin: 0; color: #333;">${seniorName}</h3>
                        <p style="margin: 0.25rem 0 0 0; color: #666;">Senior ID: ${seniorId}</p>
                    </div>
                </div>
                
                <div class="health-metrics-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
                    ${Object.keys(latestByType).map(type => {
                        const record = latestByType[type];
                        const icon = this.getHealthIcon(type);
                        const color = this.getHealthStatusColor(type, record.value);
                        return `
                            <div style="background: white; padding: 1rem; border-radius: 8px; border-left: 4px solid ${color}; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                <div style="display: flex; align-items: center; margin-bottom: 0.5rem;">
                                    <span style="font-size: 1.5rem; margin-right: 0.5rem;">${icon}</span>
                                    <strong style="color: #333;">${this.formatHealthType(type)}</strong>
                                </div>
                                <div style="font-size: 1.2rem; font-weight: bold; color: ${color};">
                                    ${this.formatHealthValueForDisplay(record)}
                                </div>
                                <div style="font-size: 0.8rem; color: #666; margin-top: 0.25rem;">
                                    ${this.formatTime(record.timestamp)}
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
                
                <div class="recent-records">
                    <h4 style="margin-bottom: 1rem; color: #333;">Recent Health Records</h4>
                    <div style="max-height: 300px; overflow-y: auto;">
                        ${seniorRecords.slice(0, 10).map(record => `
                            <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; border-bottom: 1px solid #eee;">
                                <div style="display: flex; align-items: center; gap: 0.5rem;">
                                    <span style="font-size: 1.2rem;">${this.getHealthIcon(record.type)}</span>
                                    <span style="font-weight: 500;">${this.formatHealthType(record.type)}</span>
                                    <span style="color: #666;">${this.formatHealthValueForDisplay(record)}</span>
                                </div>
                                <div style="font-size: 0.8rem; color: #999;">
                                    ${this.formatTime(record.timestamp)}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
        mainApp.openModal(`${seniorName} - Health Records`, body);
    }

    addRecordForSenior(seniorId, seniorName) {
        const actualSeniorName = this.getSeniorName(seniorId);
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Senior ID</label>
                    <input id="healthSeniorId" type="text" value="${seniorId}" readonly />
                </div>
                <div class="form-group">
                    <label>Senior Name</label>
                    <input id="healthSeniorName" type="text" value="${actualSeniorName}" readonly />
                </div>
                <div class="form-group">
                    <label>Health Metric Type</label>
                    <select id="healthType" onchange="healthManager.updateValuePlaceholder()">
                        <option value="blood_pressure">Blood Pressure</option>
                        <option value="blood_sugar">Blood Sugar</option>
                        <option value="weight">Weight</option>
                        <option value="heart_rate">Heart Rate</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Value</label>
                    <input id="healthValue" type="text" placeholder="e.g., 120/80 for blood pressure" />
                </div>
                <div class="form-group">
                    <label>Unit</label>
                    <input id="healthUnit" type="text" placeholder="Auto-filled based on type" readonly />
                </div>
                <div class="form-group">
                    <label>Recorded By</label>
                    <select id="healthRecordedBy">
                        <option value="admin">Admin</option>
                        <option value="user">User</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Notes (Optional)</label>
                    <textarea id="healthNotes" placeholder="Additional health notes..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="healthManager.saveNew()"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal(`Add Health Record - ${actualSeniorName}`, body);
        this.updateValuePlaceholder();
    }


    renderHealthValue(record, type) {
        if (!record) {
            return '<span style="color: #ccc; font-style: italic;">N/A</span>';
        }
        
        const color = this.getHealthStatusColor(type, record.value);
        const formattedValue = this.formatHealthValueForDisplay(record);
        const icon = this.getHealthIcon(type);
        
        return `
            <div style="display: flex; align-items: center; justify-content: center; gap: 5px;">
                <span style="font-size: 1.2rem;">${icon}</span>
                <span style="font-weight: bold; color: ${color}; font-size: 1rem;">${formattedValue}</span>
            </div>
        `;
    }

    formatDate(timestamp) {
        if (!timestamp) return 'N/A';
        const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
        return date.toLocaleDateString();
    }


    openAddHealthModal() {
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Senior ID</label>
                    <input id="healthSeniorId" type="text" placeholder="Enter senior ID" />
                </div>
                <div class="form-group">
                    <label>Senior Name</label>
                    <input id="healthSeniorName" type="text" placeholder="Enter senior name" />
                </div>
                <div class="form-group">
                    <label>Health Metric Type</label>
                    <select id="healthType" onchange="healthManager.updateValuePlaceholder()">
                        <option value="blood_pressure">Blood Pressure</option>
                        <option value="blood_sugar">Blood Sugar</option>
                        <option value="weight">Weight</option>
                        <option value="heart_rate">Heart Rate</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Value</label>
                    <input id="healthValue" type="text" placeholder="e.g., 120/80 for blood pressure" />
                </div>
                <div class="form-group">
                    <label>Unit</label>
                    <input id="healthUnit" type="text" placeholder="Auto-filled based on type" readonly />
                </div>
                <div class="form-group">
                    <label>Recorded By</label>
                    <select id="healthRecordedBy">
                        <option value="admin">Admin</option>
                        <option value="user">User</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label>Notes (Optional)</label>
                    <textarea id="healthNotes" placeholder="Additional health notes..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="healthManager.saveNew()"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal('Add Health Record', body);
        // Set default unit based on selected type
        this.updateValuePlaceholder();
    }

    updateValuePlaceholder() {
        const typeSelect = document.getElementById('healthType');
        const valueInput = document.getElementById('healthValue');
        const unitInput = document.getElementById('healthUnit');
        
        if (!typeSelect || !valueInput || !unitInput) return;
        
        const type = typeSelect.value;
        const placeholders = {
            'blood_pressure': '120/80',
            'blood_sugar': '95',
            'weight': '65',
            'heart_rate': '72'
        };
        
        const units = {
            'blood_pressure': 'mmHg',
            'blood_sugar': 'mg/dL',
            'weight': 'kg',
            'heart_rate': 'bpm'
        };
        
        valueInput.placeholder = placeholders[type] || '';
        unitInput.value = units[type] || '';
    }

    async saveNew() {
        try {
            const seniorId = document.getElementById('healthSeniorId').value.trim();
            const seniorName = document.getElementById('healthSeniorName').value.trim();
            const type = document.getElementById('healthType').value.trim();
            const value = document.getElementById('healthValue').value.trim();
            const unit = document.getElementById('healthUnit').value.trim();
            const recordedBy = document.getElementById('healthRecordedBy').value.trim();
            const notes = document.getElementById('healthNotes').value.trim();
            
            if (!seniorId || !seniorName || !type || !value) {
                alert('Please fill in all required fields (Senior ID, Senior Name, Type, Value)');
                return;
            }
            
            // Validate value format based on type
            if (!this.validateHealthValue(type, value)) {
                alert('Please enter a valid value for the selected health metric type');
                return;
            }
            
            const healthRecord = {
                seniorId,
                seniorName,
                type,
                value,
                unit: unit || this.getDefaultUnit(type),
                recordedBy: recordedBy || 'admin',
                notes: notes || '',
                timestamp: FirebaseUtils.getTimestamp(),
                createdAt: FirebaseUtils.getTimestamp(),
                updatedAt: FirebaseUtils.getTimestamp()
            };
            
            await FirebaseUtils.addDoc(COLLECTIONS.HEALTH_RECORDS, healthRecord);
            
            
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to save health record', e);
            alert('Failed to save health record. Please try again.');
        }
    }

    validateHealthValue(type, value) {
        switch (type) {
            case 'blood_pressure':
                // Should be in format "systolic/diastolic" (e.g., "120/80")
                return /^\d+\/\d+$/.test(value);
            case 'blood_sugar':
            case 'weight':
            case 'heart_rate':
                // Should be numeric
                return /^\d+(\.\d+)?$/.test(value);
            default:
                return true;
        }
    }

    view(id) {
        const r = this.records.find(x => x.id === id);
        if (!r || !window.mainApp) return;
        
        const icon = this.getHealthIcon(r.type);
        const statusColor = this.getHealthStatusColor(r.type, r.value);
        
        const body = `
            <div class="health-record-detail">
                <div class="health-record-header" style="display: flex; align-items: center; margin-bottom: 1rem; padding: 1rem; background: #f8f9fa; border-radius: 8px;">
                    <div style="font-size: 3rem; margin-right: 1rem;">${icon}</div>
                    <div>
                        <h3 style="margin: 0; color: #333;">${this.formatHealthType(r.type)}</h3>
                        <p style="margin: 0.25rem 0 0 0; color: #666;">${r.seniorName || 'Unknown Senior'}</p>
                    </div>
                </div>
                
                <div class="detail-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
                    <div class="detail-item">
                        <strong>Senior ID:</strong><br>
                        <span style="color: #666;">${r.seniorId || 'Unknown'}</span>
                    </div>
                    <div class="detail-item">
                        <strong>Health Metric:</strong><br>
                        <span style="color: #666;">${this.formatHealthType(r.type)}</span>
                    </div>
                    <div class="detail-item">
                        <strong>Value:</strong><br>
                        <span style="font-size: 1.2rem; font-weight: bold; color: ${statusColor};">${r.value} ${r.unit || this.getHealthUnit(r.type)}</span>
                    </div>
                    <div class="detail-item">
                        <strong>Recorded By:</strong><br>
                        <span style="color: #666;">${this.formatRecordedBy(r.recordedBy)}</span>
                    </div>
                    <div class="detail-item">
                        <strong>Recorded:</strong><br>
                        <span style="color: #666;">${this.formatTime(r.timestamp)}</span>
                    </div>
                    <div class="detail-item">
                        <strong>Source:</strong><br>
                        <span style="color: #666;">${r.source === 'fs' ? 'Firestore' : 'Realtime DB'}</span>
                    </div>
                </div>
                
                ${r.notes ? `
                    <div class="notes-section" style="margin-top: 1rem;">
                        <strong>Notes:</strong><br>
                        <div style="background: #f8f9fa; padding: 0.75rem; border-radius: 4px; margin-top: 0.5rem; color: #666;">
                            ${r.notes}
                        </div>
                    </div>
                ` : ''}
            </div>
        `;
        mainApp.openModal('Health Record Details', body);
    }

    getHealthIcon(type) {
        const icons = {
            'blood_pressure': '🩸',
            'blood_sugar': '🍯',
            'weight': '⚖️',
            'heart_rate': '❤️'
        };
        return icons[type] || '📊';
    }

    getHealthStatusColor(type, value) {
        // Simple color coding based on health metric type
        const colors = {
            'blood_pressure': '#4caf50',
            'blood_sugar': '#ff9800',
            'weight': '#2196f3',
            'heart_rate': '#f44336'
        };
        return colors[type] || '#666';
    }

    formatRecordedBy(recordedBy) {
        const labels = {
            'admin': 'Administrator',
            'user': 'Senior User',
            'family': 'Family Member'
        };
        return labels[recordedBy] || recordedBy || 'Unknown';
    }

    formatHealthValueForDisplay(record) {
        // Format the value with unit for display, matching Android app format
        const value = record.value || '';
        const unit = record.unit || this.getHealthUnit(record.type);
        
        if (!value) return 'N/A';
        
        // For blood pressure, don't add unit since it's already in the value (e.g., "120/80")
        if (record.type === 'blood_pressure') {
            return `${value} mmHg`;
        }
        
        // For other types, add unit if available
        return unit ? `${value} ${unit}` : value;
    }
    
    formatHealthType(type) {
        const types = {
            'blood_pressure': 'Blood Pressure',
            'blood_sugar': 'Blood Sugar',
            'weight': 'Weight',
            'heart_rate': 'Heart Rate'
        };
        return types[type] || type;
    }
    
    getHealthUnit(type) {
        const units = {
            'blood_pressure': 'mmHg',
            'blood_sugar': 'mg/dL',
            'weight': 'kg',
            'heart_rate': 'bpm',
            'temperature': '°C'
        };
        return units[type] || '';
    }

    getDefaultUnit(type) {
        return this.getHealthUnit(type);
    }

    edit(id) {
        // Placeholder for editing flow
        this.view(id);
    }

    async delete(id) {
        if (!confirm('Delete this health record?')) return;
        try {
            if (id.startsWith('rtdb:')) {
                const key = id.replace('rtdb:', '');
                await rtdb.ref('health_records').child(key).remove();
            } else {
                await db.collection(COLLECTIONS.HEALTH_RECORDS).doc(id).delete();
            }
        } catch (e) {
            console.error('Failed to delete health record', e);
            alert('Failed to delete health record. Please try again.');
        }
    }

    // Public method to refresh health data
    async refreshHealthData() {
        try {
            await this.loadHealthRecords();
            await this.loadHealthRecordsRTDB();
            await this.loadSeniorsData(); // Refresh seniors data too
            this.render();
        } catch (e) {
            console.error('Failed to refresh health data:', e);
        }
    }

    // Method to ensure seniors data is loaded
    async ensureSeniorsDataLoaded() {
        if (this.seniorsData.length === 0) {
            await this.loadSeniorsData();
        }
    }

    // Method to get health statistics
    getHealthStatistics() {
        const stats = {
            totalRecords: this.records.length,
            bloodPressureRecords: this.records.filter(r => r.type === 'blood_pressure').length,
            bloodSugarRecords: this.records.filter(r => r.type === 'blood_sugar').length,
            weightRecords: this.records.filter(r => r.type === 'weight').length,
            heartRateRecords: this.records.filter(r => r.type === 'heart_rate').length,
            recentRecords: this.records.filter(r => {
                const recordTime = this.getTime(r.timestamp);
                const dayAgo = Date.now() - (24 * 60 * 60 * 1000);
                return recordTime > dayAgo;
            }).length
        };
        return stats;
    }
}

const healthManager = new HealthManager();
window.healthManager = healthManager;


