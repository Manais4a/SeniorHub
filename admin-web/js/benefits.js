// Benefits Management (CRUD)
class BenefitsManager {
    constructor() {
        this.benefits = [];
        this.init();
    }

    init() {
        this.setupRealtime();
        this.loadBenefits();
    }

    setupRealtime() {
        try {
            rtdb.ref('benefits').on('value', (snapshot) => {
                const val = snapshot.val() || {};
                this.benefits = Object.keys(val)
                    .map(key => ({ id: key, ...val[key] }))
                    .sort((a, b) => (a.title || '').localeCompare(b.title || ''));
                this.render();
            });
        } catch (e) {
            console.error('Realtime DB listener failed', e);
        }
    }

    async loadBenefits() {
        try {
            console.log('Loading benefits from Firebase Realtime Database...');
            const snapshot = await rtdb.ref('benefits').get();
            const val = snapshot.val() || {};
            console.log('Loaded benefits:', val);
            this.benefits = Object.keys(val)
                .map(key => ({ id: key, ...val[key] }))
                .sort((a, b) => (a.title || '').localeCompare(b.title || ''));
            this.render();
        } catch (e) {
            console.error('Failed to load benefits from Realtime Database', e);
            // Show error in UI
            this.benefits = [];
            this.render();
        }
    }

    render() {
        const tbody = document.getElementById('benefitsTableBody');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!this.benefits.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center">
                        <div class="empty-state">
                            <i class="fas fa-gift"></i>
                            <p>No benefits found</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        this.benefits.forEach(b => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${b.title || ''}</td>
                <td>${b.category || ''}</td>
                <td>${this.formatAmount(b.amount)}</td>
                <td><span class="status-badge ${b.status?.toLowerCase()}">${b.status || 'Available'}</span></td>
                <td><span class="status-badge ${b.isActive ? 'active' : 'inactive'}">${b.isActive ? 'Yes' : 'No'}</span></td>
                <td>${this.formatNextDisbursement(b.nextDisbursementDate, b.disbursementAmount)}</td>
                <td>
                    <div class="action-buttons">
                        <div class="action-group">
                            <button class="action-btn view-btn" onclick="benefitsManager.view('${b.id}')" title="View Details">
                                <i class="fas fa-eye"></i>
                            </button>
                            <button class="action-btn edit-btn" onclick="benefitsManager.edit('${b.id}')" title="Edit Information">
                                <i class="fas fa-edit"></i>
                            </button>
                        </div>
                        <div class="action-group">
                            <button class="action-btn delete-btn" onclick="benefitsManager.delete('${b.id}')" title="Delete Benefit">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    openAddBenefitModal() {
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Assistance Program Title</label>
                    <input id="benefitTitle" type="text" placeholder="e.g., Medical Service Assistance" />
                </div>
                <div class="form-group">
                    <label>Category</label>
                    <select id="benefitCategory">
                        <option value="Medical Assistance">Medical Assistance</option>
                        <option value="Financial Support">Financial Support</option>
                        <option value="Food Assistance">Food Assistance</option>
                        <option value="Housing Support">Housing Support</option>
                        <option value="Utility Discounts">Utility Discounts</option>
                        <option value="Transportation">Transportation</option>
                        <option value="Social Services">Social Services</option>
                        <option value="Social Security">Social Security</option>
                        <option value="Medicare">Medicare</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Amount/Value (₱)</label>
                    <input id="benefitAmount" type="text" placeholder="e.g., 500, Free, 20%, Monthly Food Pack" />
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="benefitStatus">
                        <option value="Available">Available</option>
                        <option value="Active">Active</option>
                        <option value="Pending">Pending</option>
                        <option value="Discontinued">Discontinued</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Benefits Next Schedule</label>
                    <input id="benefitNextDisbursement" type="date" />
                </div>
                <div class="form-group">
                    <label>Disbursement Amount (₱)</label>
                    <input id="benefitDisbursementAmount" type="text" placeholder="e.g., 500" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="benefitWebsite" type="text" placeholder="https://..." />
                </div>
                <div class="form-group full-width">
                    <label>Description</label>
                    <textarea id="benefitDescription" placeholder="Detailed description of the assistance program..."></textarea>
                </div>
                <div class="form-group full-width">
                    <label>Requirements</label>
                    <textarea id="benefitRequirements" placeholder="Eligibility requirements and documents needed..."></textarea>
                </div>
                <div class="form-group full-width">
                    <label>Application Process</label>
                    <textarea id="benefitApplicationProcess" placeholder="How to apply for this benefit..."></textarea>
                </div>
                <div class="form-group full-width">
                    <label>Contact Information</label>
                    <textarea id="benefitContactInfo" placeholder="Contact details, office location, phone numbers..."></textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="benefitsManager.saveNew()"><i class="fas fa-save"></i> Save</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        if (window.mainApp) mainApp.openModal('Add Assistance Program', body);
    }

    async saveNew() {
        try {
            // Check authentication
            if (!this.isUserAuthenticated()) {
                this.showError('You must be logged in to add benefits');
                return;
            }

            const title = document.getElementById('benefitTitle').value.trim();
            const category = document.getElementById('benefitCategory').value.trim();
            const amount = document.getElementById('benefitAmount').value.trim();
            const status = document.getElementById('benefitStatus').value;
            const nextDisbursementDate = document.getElementById('benefitNextDisbursement').value;
            const disbursementAmount = document.getElementById('benefitDisbursementAmount').value.trim();
            const website = document.getElementById('benefitWebsite').value.trim();
            const description = document.getElementById('benefitDescription').value.trim();
            const requirements = document.getElementById('benefitRequirements').value.trim();
            const applicationProcess = document.getElementById('benefitApplicationProcess').value.trim();
            const contactInfo = document.getElementById('benefitContactInfo').value.trim();
            
            if (!title || !category || !description) {
                alert('Please fill in all required fields (Title, Category, Description)');
                return;
            }
            
            const data = {
                title, 
                category, 
                amount: amount || '',
                status, 
                description,
                requirements: requirements || '',
                applicationProcess: applicationProcess || '',
                contactInfo: contactInfo || '',
                website: website || '',
                isActive: true,
                createdBy: currentUser().uid,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };

            if (nextDisbursementDate) {
                data.nextDisbursementDate = new Date(nextDisbursementDate).toISOString();
            }
            if (disbursementAmount) {
                data.disbursementAmount = disbursementAmount;
            }
            
            const newBenefitRef = rtdb.ref('benefits').push();
            await newBenefitRef.set(data);
            this.showSuccess('Benefit added successfully');
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to save benefit', e);
            this.showError('Failed to save benefit: ' + e.message);
        }
    }

    view(id) {
        console.log('View benefit clicked for ID:', id);
        const b = this.benefits.find(x => x.id === id);
        if (!b) {
            console.error('Benefit not found with ID:', id);
            this.showError('Benefit not found');
            return;
        }
        if (!window.mainApp) {
            console.error('MainApp not available');
            this.showError('Application not ready');
            return;
        }
        
        // Get category icon
        const categoryIcon = this.getCategoryIcon(b.category);
        
        const body = `
            <div class="simple-benefit-modal">
                <div class="benefit-header-simple">
                    <h2>${b.title || 'Untitled Program'}</h2>
                    <div class="benefit-meta">
                        <span class="category">${b.category || 'Uncategorized'}</span>
                        <span class="status ${b.status?.toLowerCase()}">${b.status || 'Available'}</span>
                        <span class="active ${b.isActive ? 'yes' : 'no'}">${b.isActive ? 'Active' : 'Inactive'}</span>
                    </div>
                </div>

                <div class="benefit-content">
                    <div class="info-row">
                        <label>Amount/Value:</label>
                        <span>${this.formatAmount(b.amount)}</span>
                    </div>
                    
                    ${b.nextDisbursementDate ? `
                    <div class="info-row">
                        <label>Benefits Next Schedule:</label>
                        <span>${this.formatNextDisbursement(b.nextDisbursementDate, b.disbursementAmount)}</span>
                    </div>
                    ` : ''}
                    
                    ${b.website ? `
                    <div class="info-row">
                        <label>Website:</label>
                        <a href="${b.website}" target="_blank">${b.website}</a>
                    </div>
                    ` : ''}

                    <div class="info-section">
                        <h4>Description</h4>
                        <p>${b.description || 'No description available.'}</p>
                    </div>

                    ${b.requirements ? `
                    <div class="info-section">
                        <h4>Requirements</h4>
                        <ul>
                            ${b.requirements.split('\n').map(req => req.trim()).filter(req => req).map(req => `<li>${req}</li>`).join('')}
                        </ul>
                    </div>
                    ` : ''}

                    ${b.applicationProcess ? `
                    <div class="info-section">
                        <h4>Application Process</h4>
                        <ol>
                            ${b.applicationProcess.split('\n').map(step => step.trim()).filter(step => step).map(step => `<li>${step}</li>`).join('')}
                        </ol>
                    </div>
                    ` : ''}

                    ${b.contactInfo ? `
                    <div class="info-section">
                        <h4>Contact Information</h4>
                        <p>${b.contactInfo}</p>
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
        mainApp.openModal('Assistance Program Details', body);
    }
    
    formatTime(ts) {
        if (!ts) return 'N/A';
        const d = ts.toDate ? ts.toDate() : new Date(ts);
        return d.toLocaleString();
    }

    getCategoryIcon(category) {
        const iconMap = {
            'Medical Assistance': 'fas fa-heartbeat',
            'Financial Support': 'fas fa-money-bill-wave',
            'Food Assistance': 'fas fa-utensils',
            'Housing Support': 'fas fa-home',
            'Utility Discounts': 'fas fa-bolt',
            'Transportation': 'fas fa-bus',
            'Social Services': 'fas fa-hands-helping',
            'Social Security': 'fas fa-shield-alt',
            'Medicare': 'fas fa-user-md'
        };
        return iconMap[category] || 'fas fa-gift';
    }

    formatAmount(amount) {
        if (!amount) return 'Contact for details';
        if (amount === '0') return '<span class="amount-display">Free</span>';
        if (amount === 'Free') return '<span class="amount-display">Free</span>';
        if (amount.includes('%')) return `<span class="amount-display">${amount}</span>`; // For percentage discounts
        if (amount.includes('Monthly Food Pack')) return `<span class="amount-display">${amount}</span>`; // For non-monetary benefits
        if (amount.includes('Varies')) return `<span class="amount-display">${amount}</span>`; // For variable amounts
        // Convert to number and format as peso
        const numAmount = parseFloat(amount);
        if (isNaN(numAmount)) return `<span class="amount-display">${amount}</span>`; // Return original if not a number
        return `<span class="amount-display currency-peso">₱${numAmount.toLocaleString('en-PH')}</span>`;
    }

    formatNextDisbursement(date, amount) {
        if (!date) return 'TBD';
        const d = date.toDate ? date.toDate() : new Date(date);
        let formattedAmount = '';
        if (amount) {
            if (amount === '0') {
                formattedAmount = '<span class="amount-display">Free</span>';
            } else {
                const numAmount = parseFloat(amount);
                if (!isNaN(numAmount)) {
                    formattedAmount = `<span class="amount-display currency-peso">₱${numAmount.toLocaleString('en-PH')}</span>`;
                } else {
                    formattedAmount = `<span class="amount-display">${amount}</span>`;
                }
            }
        }
        return `${d.toLocaleDateString()} ${formattedAmount}`;
    }

    edit(id) {
        console.log('Edit benefit clicked for ID:', id);
        const b = this.benefits.find(x => x.id === id);
        if (!b) {
            console.error('Benefit not found with ID:', id);
            this.showError('Benefit not found');
            return;
        }
        if (!window.mainApp) {
            console.error('MainApp not available');
            this.showError('Application not ready');
            return;
        }
        const body = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Assistance Program Title *</label>
                    <input id="benefitTitle" type="text" value="${b.title || ''}" placeholder="Enter program title" required />
                </div>
                <div class="form-group">
                    <label>Category *</label>
                    <select id="benefitCategory" required>
                        <option value="Medical Assistance" ${b.category === 'Medical Assistance' ? 'selected' : ''}>Medical Assistance</option>
                        <option value="Financial Support" ${b.category === 'Financial Support' ? 'selected' : ''}>Financial Support</option>
                        <option value="Food Assistance" ${b.category === 'Food Assistance' ? 'selected' : ''}>Food Assistance</option>
                        <option value="Housing Support" ${b.category === 'Housing Support' ? 'selected' : ''}>Housing Support</option>
                        <option value="Utility Discounts" ${b.category === 'Utility Discounts' ? 'selected' : ''}>Utility Discounts</option>
                        <option value="Transportation" ${b.category === 'Transportation' ? 'selected' : ''}>Transportation</option>
                        <option value="Social Services" ${b.category === 'Social Services' ? 'selected' : ''}>Social Services</option>
                        <option value="Social Security" ${b.category === 'Social Security' ? 'selected' : ''}>Social Security</option>
                        <option value="Medicare" ${b.category === 'Medicare' ? 'selected' : ''}>Medicare</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Amount/Value (₱)</label>
                    <input id="benefitAmount" type="text" value="${b.amount || ''}" placeholder="e.g., 500, Free, 20%, Monthly Food Pack" />
                </div>
                <div class="form-group">
                    <label>Status *</label>
                    <select id="benefitStatus" required>
                        <option value="Available" ${b.status === 'Available' ? 'selected' : ''}>Available</option>
                        <option value="Pending" ${b.status === 'Pending' ? 'selected' : ''}>Pending</option>
                        <option value="Active" ${b.status === 'Active' ? 'selected' : ''}>Active</option>
                        <option value="Discontinued" ${b.status === 'Discontinued' ? 'selected' : ''}>Discontinued</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Active Status</label>
                    <select id="benefitActive">
                        <option value="true" ${b.isActive ? 'selected' : ''}>Yes</option>
                        <option value="false" ${!b.isActive ? 'selected' : ''}>No</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Benefits Next Schedule</label>
                    <input id="benefitNextDisbursement" type="date" value="${b.nextDisbursementDate ? new Date(b.nextDisbursementDate).toISOString().split('T')[0] : ''}" />
                </div>
                <div class="form-group">
                    <label>Disbursement Amount (₱)</label>
                    <input id="benefitDisbursementAmount" type="text" value="${b.disbursementAmount || ''}" placeholder="e.g., 500" />
                </div>
                <div class="form-group">
                    <label>Website</label>
                    <input id="benefitWebsite" type="url" value="${b.website || ''}" placeholder="https://..." />
                </div>
                <div class="form-group full-width">
                    <label>Description *</label>
                    <textarea id="benefitDescription" rows="3" placeholder="Describe the assistance program..." required>${b.description || ''}</textarea>
                </div>
                <div class="form-group full-width">
                    <label>Requirements</label>
                    <textarea id="benefitRequirements" rows="2" placeholder="List the requirements...">${b.requirements || ''}</textarea>
                </div>
                <div class="form-group full-width">
                    <label>Application Process</label>
                    <textarea id="benefitApplicationProcess" rows="2" placeholder="Describe how to apply...">${b.applicationProcess || ''}</textarea>
                </div>
                <div class="form-group full-width">
                    <label>Contact Information</label>
                    <textarea id="benefitContactInfo" rows="2" placeholder="Provide contact details...">${b.contactInfo || ''}</textarea>
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-primary" onclick="benefitsManager.saveEdit('${b.id}')"><i class="fas fa-save"></i> Save Changes</button>
                <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        `;
        mainApp.openModal('Edit Assistance Program', body);
    }

    async saveEdit(id) {
        try {
            // Check authentication
            if (!this.isUserAuthenticated()) {
                this.showError('You must be logged in to edit benefits');
                return;
            }

            const title = document.getElementById('benefitTitle').value.trim();
            const category = document.getElementById('benefitCategory').value.trim();
            const amount = document.getElementById('benefitAmount').value.trim();
            const status = document.getElementById('benefitStatus').value;
            const isActive = document.getElementById('benefitActive').value === 'true';
            const description = document.getElementById('benefitDescription').value.trim();
            const requirements = document.getElementById('benefitRequirements').value.trim();
            const applicationProcess = document.getElementById('benefitApplicationProcess').value.trim();
            const contactInfo = document.getElementById('benefitContactInfo').value.trim();
            const website = document.getElementById('benefitWebsite').value.trim();
            const nextDisbursementDate = document.getElementById('benefitNextDisbursement').value;
            const disbursementAmount = document.getElementById('benefitDisbursementAmount').value.trim();
            
            if (!title || !category || !description) {
                alert('Please fill in all required fields (Title, Category, Description)');
                return;
            }
            
            const updateData = {
                title, 
                category, 
                amount: amount || '',
                status, 
                isActive, 
                description,
                requirements: requirements || '',
                applicationProcess: applicationProcess || '',
                contactInfo: contactInfo || '',
                website: website || '',
                updatedAt: new Date().toISOString()
            };

            // Add optional fields if they have values
            if (nextDisbursementDate) {
                updateData.nextDisbursementDate = new Date(nextDisbursementDate).toISOString();
            }
            if (disbursementAmount) {
                updateData.disbursementAmount = disbursementAmount;
            }
            
            await rtdb.ref(`benefits/${id}`).update(updateData);
            this.showSuccess('Benefit updated successfully');
            if (window.mainApp) mainApp.closeModal();
        } catch (e) {
            console.error('Failed to update benefit', e);
            this.showError('Failed to update benefit: ' + e.message);
        }
    }

    async delete(id) {
        console.log('Delete benefit clicked for ID:', id);
        if (!confirm('Are you sure you want to delete this benefit? This action cannot be undone.')) return;
        
        // Check if user is authenticated
        if (!this.isUserAuthenticated()) {
            this.showError('You must be logged in to delete benefits');
            return;
        }
        
        try {
            await rtdb.ref(`benefits/${id}`).remove();
            this.showSuccess('Benefit deleted successfully');
        } catch (e) {
            console.error('Failed to delete benefit', e);
            this.handleDeleteError(e);
        }
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

    isUserAuthenticated() {
        // Check if Firebase auth is available and user is logged in
        if (typeof auth !== 'undefined' && auth.currentUser) {
            console.log('User authenticated via Firebase Auth:', auth.currentUser.email);
            return true;
        }
        // Fallback check for mainApp authentication
        if (window.mainApp && window.mainApp.currentUser) {
            console.log('User authenticated via mainApp:', window.mainApp.currentUser);
            return true;
        }
        // Additional fallback - check if we're in development mode
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            console.log('Development mode detected, allowing operations');
            return true;
        }
        console.log('User not authenticated');
        return false;
    }

    handleDeleteError(error) {
        let errorMessage = 'Failed to delete benefit';
        
        if (error.code === 'PERMISSION_DENIED') {
            errorMessage = 'Permission denied. Please check your authentication and try again.';
        } else if (error.code === 'UNAVAILABLE') {
            errorMessage = 'Service unavailable. Please check your internet connection and try again.';
        } else if (error.code === 'NETWORK_ERROR') {
            errorMessage = 'Network error. Please check your connection and try again.';
        } else if (error.message) {
            errorMessage = `Failed to delete benefit: ${error.message}`;
        }
        
        this.showError(errorMessage);
    }
}

const benefitsManager = new BenefitsManager();
window.benefitsManager = benefitsManager;


