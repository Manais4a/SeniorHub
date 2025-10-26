// Data Collection Forms Management
class DataCollectionManager {
    constructor() {
        this.currentForm = null;
        this.currentSeniorId = null;
        this.formData = {};
        this.init();
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Form submission handlers will be set up dynamically
    }

    openDataForm(formType) {
        this.currentForm = formType;
        this.formData = {};
        
        const modalTitle = this.getFormTitle(formType);
        const modalBody = this.createFormContent(formType);
        
        this.openModal(modalTitle, modalBody);
    }

    getFormTitle(formType) {
        const titles = {
            'personal': 'Personal Information Collection',
            'health': 'Health Information Collection',
            'emergency': 'Emergency Contacts Collection',
            'benefits': 'Benefits Assessment',
            'social': 'Social Information Collection',
            'accessibility': 'Accessibility Needs Assessment'
        };
        return titles[formType] || 'Data Collection Form';
    }

    createFormContent(formType) {
        switch (formType) {
            case 'personal':
                return this.createPersonalInfoForm();
            case 'health':
                return this.createHealthInfoForm();
            case 'emergency':
                return this.createEmergencyContactsForm();
            case 'benefits':
                return this.createBenefitsAssessmentForm();
            case 'social':
                return this.createSocialInfoForm();
            case 'accessibility':
                return this.createAccessibilityForm();
            default:
                return '<p>Form not found</p>';
        }
    }

    createPersonalInfoForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Basic Information</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="firstName">First Name *</label>
                            <input type="text" id="firstName" name="firstName" required>
                        </div>
                        <div class="form-group">
                            <label for="lastName">Last Name *</label>
                            <input type="text" id="lastName" name="lastName" required>
                        </div>
                        <div class="form-group">
                            <label for="middleName">Middle Name</label>
                            <input type="text" id="middleName" name="middleName">
                        </div>
                        <div class="form-group">
                            <label for="birthDate">Birth Date *</label>
                            <input type="date" id="birthDate" name="birthDate" required>
                        </div>
                        <div class="form-group">
                            <label for="gender">Gender *</label>
                            <select id="gender" name="gender" required>
                                <option value="">Select Gender</option>
                                <option value="male">Male</option>
                                <option value="female">Female</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="maritalStatus">Marital Status</label>
                            <select id="maritalStatus" name="maritalStatus">
                                <option value="">Select Status</option>
                                <option value="single">Single</option>
                                <option value="married">Married</option>
                                <option value="widowed">Widowed</option>
                                <option value="divorced">Divorced</option>
                                <option value="separated">Separated</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h4>Contact Information</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="email">Email Address</label>
                            <input type="email" id="email" name="email">
                        </div>
                        <div class="form-group">
                            <label for="phoneNumber">Phone Number *</label>
                            <input type="tel" id="phoneNumber" name="phoneNumber" required>
                        </div>
                        <div class="form-group full-width">
                            <label for="houseNumberAndStreet">House Number and Street *</label>
                            <input type="text" id="houseNumberAndStreet" name="houseNumberAndStreet" required>
                        </div>
                        <div class="form-group">
                            <label for="barangay">Barangay *</label>
                            <input type="text" id="barangay" name="barangay" required>
                        </div>
                        <div class="form-group">
                            <label for="city">City *</label>
                            <input type="text" id="city" name="city" value="Davao City" required>
                        </div>
                        <div class="form-group">
                            <label for="province">Province *</label>
                            <input type="text" id="province" name="province" value="Davao Del Sur" required>
                        </div>
                        <div class="form-group">
                            <label for="zipCode">ZIP Code</label>
                            <input type="text" id="zipCode" name="zipCode" value="8000">
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h4>Government IDs</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="oscaNumber">OSCA Number</label>
                            <input type="text" id="oscaNumber" name="oscaNumber">
                        </div>
                        <div class="form-group">
                            <label for="sssNumber">SSS Number</label>
                            <input type="text" id="sssNumber" name="sssNumber">
                        </div>
                        <div class="form-group">
                            <label for="philHealthNumber">PhilHealth Number</label>
                            <input type="text" id="philHealthNumber" name="philHealthNumber">
                        </div>
                        <div class="form-group">
                            <label for="gsisNumber">GSIS Number</label>
                            <input type="text" id="gsisNumber" name="gsisNumber">
                        </div>
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('personal')">
                        <i class="fas fa-save"></i>
                        Save Personal Information
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    createHealthInfoForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Medical Information</h4>
                    <div class="form-group">
                        <label for="bloodType">Blood Type</label>
                        <select id="bloodType" name="bloodType">
                            <option value="">Select Blood Type</option>
                            <option value="A+">A+</option>
                            <option value="A-">A-</option>
                            <option value="B+">B+</option>
                            <option value="B-">B-</option>
                            <option value="AB+">AB+</option>
                            <option value="AB-">AB-</option>
                            <option value="O+">O+</option>
                            <option value="O-">O-</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label for="medicalConditions">Medical Conditions</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="diabetes">
                                <span>Diabetes</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="hypertension">
                                <span>Hypertension</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="heart_disease">
                                <span>Heart Disease</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="arthritis">
                                <span>Arthritis</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="osteoporosis">
                                <span>Osteoporosis</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="dementia">
                                <span>Dementia/Alzheimer's</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="medicalConditions" value="other">
                                <span>Other</span>
                            </label>
                        </div>
                        <input type="text" id="otherMedicalConditions" name="otherMedicalConditions" placeholder="Specify other conditions" class="mt-2">
                    </div>

                    <div class="form-group">
                        <label for="allergies">Allergies</label>
                        <textarea id="allergies" name="allergies" placeholder="List any known allergies"></textarea>
                    </div>

                    <div class="form-group">
                        <label for="medications">Current Medications</label>
                        <div id="medicationsList">
                            <div class="medication-item">
                                <input type="text" name="medicationName" placeholder="Medication name">
                                <input type="text" name="medicationDosage" placeholder="Dosage">
                                <input type="text" name="medicationFrequency" placeholder="Frequency">
                                <button type="button" class="btn btn-sm btn-error" onclick="this.parentElement.remove()">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </div>
                        <button type="button" class="btn btn-secondary" onclick="dataCollectionManager.addMedication()">
                            <i class="fas fa-plus"></i>
                            Add Medication
                        </button>
                    </div>
                </div>

                <div class="form-section">
                    <h4>Healthcare Provider</h4>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="doctorName">Primary Doctor Name</label>
                            <input type="text" id="doctorName" name="doctorName">
                        </div>
                        <div class="form-group">
                            <label for="doctorSpecialty">Doctor Specialty</label>
                            <input type="text" id="doctorSpecialty" name="doctorSpecialty">
                        </div>
                        <div class="form-group">
                            <label for="doctorPhone">Doctor Phone</label>
                            <input type="tel" id="doctorPhone" name="doctorPhone">
                        </div>
                        <div class="form-group">
                            <label for="hospitalName">Hospital/Clinic Name</label>
                            <input type="text" id="hospitalName" name="hospitalName">
                        </div>
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('health')">
                        <i class="fas fa-save"></i>
                        Save Health Information
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    createEmergencyContactsForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Emergency Contacts</h4>
                    <div id="emergencyContactsFormList">
                        <div class="emergency-contact-form">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>Contact Name *</label>
                                    <input type="text" name="contactName" required>
                                </div>
                                <div class="form-group">
                                    <label>Relationship *</label>
                                    <select name="contactRelationship" required>
                                        <option value="">Select Relationship</option>
                                        <option value="spouse">Spouse</option>
                                        <option value="child">Child</option>
                                        <option value="sibling">Sibling</option>
                                        <option value="parent">Parent</option>
                                        <option value="friend">Friend</option>
                                        <option value="neighbor">Neighbor</option>
                                        <option value="caregiver">Caregiver</option>
                                        <option value="other">Other</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>Phone Number *</label>
                                    <input type="tel" name="contactPhone" required>
                                </div>
                                <div class="form-group">
                                    <label>Email</label>
                                    <input type="email" name="contactEmail">
                                </div>
                                <div class="form-group full-width">
                                    <label>Address</label>
                                    <textarea name="contactAddress"></textarea>
                                </div>
                            </div>
                            <button type="button" class="btn btn-sm btn-error" onclick="this.closest('.emergency-contact-form').remove()">
                                <i class="fas fa-trash"></i>
                                Remove Contact
                            </button>
                        </div>
                    </div>
                    <button type="button" class="btn btn-secondary" onclick="dataCollectionManager.addEmergencyContactForm()">
                        <i class="fas fa-plus"></i>
                        Add Another Contact
                    </button>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('emergency')">
                        <i class="fas fa-save"></i>
                        Save Emergency Contacts
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    createBenefitsAssessmentForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Current Benefits</h4>
                    <div class="form-group">
                        <label>Government Benefits Currently Receiving</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="social_security">
                                <span>Social Security</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="medicare">
                                <span>Medicare</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="senior_citizen_discount">
                                <span>Senior Citizen Discount</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="pension">
                                <span>Pension</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="philhealth">
                                <span>PhilHealth</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="gsis">
                                <span>GSIS</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="currentBenefits" value="sss">
                                <span>SSS</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="benefitAmount">Monthly Benefit Amount (PHP)</label>
                        <input type="number" id="benefitAmount" name="benefitAmount" placeholder="0.00" step="0.01" min="0">
                    </div>
                </div>

                <div class="form-section">
                    <h4>Eligibility Assessment</h4>
                    <div class="form-group">
                        <label>Additional Benefits You May Be Eligible For</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="eligibleBenefits" value="food_assistance">
                                <span>Food Assistance Program</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="eligibleBenefits" value="transportation">
                                <span>Transportation Assistance</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="eligibleBenefits" value="housing">
                                <span>Housing Assistance</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="eligibleBenefits" value="healthcare">
                                <span>Additional Healthcare Benefits</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="eligibleBenefits" value="utilities">
                                <span>Utilities Discount</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="benefitNotes">Additional Notes</label>
                        <textarea id="benefitNotes" name="benefitNotes" placeholder="Any additional information about benefits or assistance needed"></textarea>
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('benefits')">
                        <i class="fas fa-save"></i>
                        Save Benefits Assessment
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    createSocialInfoForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Family Information</h4>
                    <div id="familyMembersList">
                        <div class="family-member-form">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>Family Member Name *</label>
                                    <input type="text" name="familyMemberName" required>
                                </div>
                                <div class="form-group">
                                    <label>Relationship *</label>
                                    <select name="familyMemberRelationship" required>
                                        <option value="">Select Relationship</option>
                                        <option value="spouse">Spouse</option>
                                        <option value="child">Child</option>
                                        <option value="grandchild">Grandchild</option>
                                        <option value="sibling">Sibling</option>
                                        <option value="parent">Parent</option>
                                        <option value="other">Other</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>Phone Number</label>
                                    <input type="tel" name="familyMemberPhone">
                                </div>
                                <div class="form-group">
                                    <label>Lives With You</label>
                                    <select name="familyMemberLivesWith">
                                        <option value="yes">Yes</option>
                                        <option value="no">No</option>
                                    </select>
                                </div>
                            </div>
                            <button type="button" class="btn btn-sm btn-error" onclick="this.closest('.family-member-form').remove()">
                                <i class="fas fa-trash"></i>
                                Remove Member
                            </button>
                        </div>
                    </div>
                    <button type="button" class="btn btn-secondary" onclick="dataCollectionManager.addFamilyMemberForm()">
                        <i class="fas fa-plus"></i>
                        Add Family Member
                    </button>
                </div>

                <div class="form-section">
                    <h4>Social Activities</h4>
                    <div class="form-group">
                        <label>Social Activities You Participate In</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="senior_center">
                                <span>Senior Center Activities</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="religious">
                                <span>Religious Activities</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="community">
                                <span>Community Events</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="hobbies">
                                <span>Hobbies and Crafts</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="exercise">
                                <span>Exercise Groups</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="socialActivities" value="volunteer">
                                <span>Volunteer Work</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="socialNotes">Additional Social Information</label>
                        <textarea id="socialNotes" name="socialNotes" placeholder="Any additional information about social activities or preferences"></textarea>
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('social')">
                        <i class="fas fa-save"></i>
                        Save Social Information
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    createAccessibilityForm() {
        return `
            <div class="data-collection-form">
                <div class="form-section">
                    <h4>Mobility and Physical Needs</h4>
                    <div class="form-group">
                        <label>Mobility Assistance Needed</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="mobilityNeeds" value="walker">
                                <span>Walker</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="mobilityNeeds" value="wheelchair">
                                <span>Wheelchair</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="mobilityNeeds" value="cane">
                                <span>Cane</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="mobilityNeeds" value="crutches">
                                <span>Crutches</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="mobilityNeeds" value="none">
                                <span>No assistance needed</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="mobilityNotes">Mobility Notes</label>
                        <textarea id="mobilityNotes" name="mobilityNotes" placeholder="Additional information about mobility needs"></textarea>
                    </div>
                </div>

                <div class="form-section">
                    <h4>Communication Needs</h4>
                    <div class="form-group">
                        <label>Communication Assistance</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationNeeds" value="hearing_aid">
                                <span>Hearing Aid</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationNeeds" value="sign_language">
                                <span>Sign Language</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationNeeds" value="large_text">
                                <span>Large Text</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationNeeds" value="voice_assistance">
                                <span>Voice Assistance</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationNeeds" value="none">
                                <span>No assistance needed</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="preferredLanguage">Preferred Language</label>
                        <select id="preferredLanguage" name="preferredLanguage">
                            <option value="en">English</option>
                            <option value="fil">Filipino</option>
                            <option value="ceb">Cebuano</option>
                            <option value="other">Other</option>
                        </select>
                    </div>
                </div>

                <div class="form-section">
                    <h4>Technology Preferences</h4>
                    <div class="form-group">
                        <label>Technology Comfort Level</label>
                        <div class="radio-group">
                            <label class="radio-item">
                                <input type="radio" name="techComfort" value="beginner">
                                <span>Beginner - Need help with basic functions</span>
                            </label>
                            <label class="radio-item">
                                <input type="radio" name="techComfort" value="intermediate">
                                <span>Intermediate - Comfortable with basic apps</span>
                            </label>
                            <label class="radio-item">
                                <input type="radio" name="techComfort" value="advanced">
                                <span>Advanced - Comfortable with most technology</span>
                            </label>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Preferred Communication Method</label>
                        <div class="checkbox-group">
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationMethod" value="phone">
                                <span>Phone Call</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationMethod" value="text">
                                <span>Text Message</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationMethod" value="email">
                                <span>Email</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationMethod" value="app">
                                <span>Mobile App</span>
                            </label>
                            <label class="checkbox-item">
                                <input type="checkbox" name="communicationMethod" value="in_person">
                                <span>In Person</span>
                            </label>
                        </div>
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-primary" onclick="dataCollectionManager.saveFormData('accessibility')">
                        <i class="fas fa-save"></i>
                        Save Accessibility Information
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">
                        Cancel
                    </button>
                </div>
            </div>
        `;
    }

    addMedication() {
        const medicationsList = document.getElementById('medicationsList');
        const medicationItem = document.createElement('div');
        medicationItem.className = 'medication-item';
        medicationItem.innerHTML = `
            <input type="text" name="medicationName" placeholder="Medication name">
            <input type="text" name="medicationDosage" placeholder="Dosage">
            <input type="text" name="medicationFrequency" placeholder="Frequency">
            <button type="button" class="btn btn-sm btn-error" onclick="this.parentElement.remove()">
                <i class="fas fa-trash"></i>
            </button>
        `;
        medicationsList.appendChild(medicationItem);
    }

    addEmergencyContactForm() {
        const contactsList = document.getElementById('emergencyContactsFormList');
        const contactForm = document.createElement('div');
        contactForm.className = 'emergency-contact-form';
        contactForm.innerHTML = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Contact Name *</label>
                    <input type="text" name="contactName" required>
                </div>
                <div class="form-group">
                    <label>Relationship *</label>
                    <select name="contactRelationship" required>
                        <option value="">Select Relationship</option>
                        <option value="spouse">Spouse</option>
                        <option value="child">Child</option>
                        <option value="sibling">Sibling</option>
                        <option value="parent">Parent</option>
                        <option value="friend">Friend</option>
                        <option value="neighbor">Neighbor</option>
                        <option value="caregiver">Caregiver</option>
                        <option value="other">Other</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number *</label>
                    <input type="tel" name="contactPhone" required>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input type="email" name="contactEmail">
                </div>
                <div class="form-group full-width">
                    <label>Address</label>
                    <textarea name="contactAddress"></textarea>
                </div>
            </div>
            <button type="button" class="btn btn-sm btn-error" onclick="this.closest('.emergency-contact-form').remove()">
                <i class="fas fa-trash"></i>
                Remove Contact
            </button>
        `;
        contactsList.appendChild(contactForm);
    }

    addFamilyMemberForm() {
        const familyList = document.getElementById('familyMembersList');
        const memberForm = document.createElement('div');
        memberForm.className = 'family-member-form';
        memberForm.innerHTML = `
            <div class="form-grid">
                <div class="form-group">
                    <label>Family Member Name *</label>
                    <input type="text" name="familyMemberName" required>
                </div>
                <div class="form-group">
                    <label>Relationship *</label>
                    <select name="familyMemberRelationship" required>
                        <option value="">Select Relationship</option>
                        <option value="spouse">Spouse</option>
                        <option value="child">Child</option>
                        <option value="grandchild">Grandchild</option>
                        <option value="sibling">Sibling</option>
                        <option value="parent">Parent</option>
                        <option value="other">Other</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Phone Number</label>
                    <input type="tel" name="familyMemberPhone">
                </div>
                <div class="form-group">
                    <label>Lives With You</label>
                    <select name="familyMemberLivesWith">
                        <option value="yes">Yes</option>
                        <option value="no">No</option>
                    </select>
                </div>
            </div>
            <button type="button" class="btn btn-sm btn-error" onclick="this.closest('.family-member-form').remove()">
                <i class="fas fa-trash"></i>
                Remove Member
            </button>
        `;
        familyList.appendChild(memberForm);
    }

    async saveFormData(formType) {
        try {
            const formData = this.collectFormData();
            
            // Validate required fields
            if (!this.validateForm(formData)) {
                return;
            }

            // If Facilitator is creating/updating personal info, enforce verification
            const role = (window.currentUserRole && window.currentUserRole()) || null;
            if (formType === 'personal' && role === ADMIN_ROLES.FACILITATOR) {
                const verdict = window.Verification.verifyResidentOrSenior(formData);
                if (!verdict.passed) {
                    this.showError('Verification failed. Provide valid city/barangay (Davao City) or age ≥ 60.');
                    return;
                }
            }

            // Save to Firebase
            const dataToSave = {
                formType: formType,
                data: formData,
                collectedBy: currentUser().uid,
                collectedAt: FirebaseUtils.getTimestamp(),
                seniorId: this.currentSeniorId || null,
                // Attach verification metadata for auditing; Data Admin can mark as pending
                verification: {
                    performedByRole: role,
                    result: formType === 'personal' ? (window.Verification.verifyResidentOrSenior(formData)) : null,
                    status: role === ADMIN_ROLES.SUPER_ADMIN ? 'auto_verified' : (role === ADMIN_ROLES.FACILITATOR ? 'auto_verified' : 'pending_review')
                }
            };

            await FirebaseUtils.addDoc(COLLECTIONS.DATA_COLLECTION, dataToSave);
            
            this.showSuccess(`${this.getFormTitle(formType)} saved successfully`);
            this.closeModal();
            
        } catch (error) {
            console.error('Error saving form data:', error);
            this.showError('Failed to save form data');
        }
    }

    collectFormData() {
        const form = document.querySelector('.data-collection-form');
        const data = {};
        
        // Collect all form inputs
        form.querySelectorAll('input, select, textarea').forEach(input => {
            if (input.type === 'checkbox') {
                if (!data[input.name]) data[input.name] = [];
                if (input.checked) data[input.name].push(input.value);
            } else if (input.type === 'radio') {
                if (input.checked) data[input.name] = input.value;
            } else {
                // Handle number inputs properly
                if (input.type === 'number') {
                    const value = input.value.trim();
                    if (value === '') {
                        data[input.name] = null;
                    } else {
                        const num = parseFloat(value);
                        data[input.name] = isNaN(num) ? null : num;
                    }
                } else {
                    data[input.name] = input.value.trim();
                }
            }
        });
        
        return data;
    }

    validateForm(data) {
        // Basic validation - can be expanded based on form type
        const requiredFields = ['firstName', 'lastName', 'phoneNumber'];
        
        for (const field of requiredFields) {
            if (!data[field] || data[field].trim() === '') {
                this.showError(`Please fill in the required field: ${field}`);
                return false;
            }
        }
        
        return true;
    }

    openModal(title, body) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalBody').innerHTML = body;
        document.getElementById('modalOverlay').classList.remove('hidden');
        
        // Focus first input after modal opens
        setTimeout(() => {
            const firstInput = document.querySelector('#modalOverlay input:not([type="hidden"]), #modalOverlay select, #modalOverlay textarea');
            if (firstInput) {
                firstInput.focus();
            }
        }, 100);
        
        // Add form validation listeners
        this.setupFormValidation();
    }
    
    setupFormValidation() {
        const form = document.querySelector('.data-collection-form');
        if (!form) return;
        
        // Add real-time validation for number inputs
        form.querySelectorAll('input[type="number"]').forEach(input => {
            input.addEventListener('input', (e) => {
                this.validateNumberInput(e.target);
            });
            
            input.addEventListener('blur', (e) => {
                this.validateNumberInput(e.target);
            });
        });
        
        // Add validation for required fields
        form.querySelectorAll('input[required], select[required], textarea[required]').forEach(input => {
            input.addEventListener('blur', (e) => {
                this.validateRequiredField(e.target);
            });
        });
    }
    
    validateNumberInput(input) {
        const value = input.value.trim();
        const min = parseFloat(input.getAttribute('min')) || 0;
        const max = parseFloat(input.getAttribute('max')) || Infinity;
        
        if (value === '') {
            this.clearFieldError(input);
            return true;
        }
        
        const num = parseFloat(value);
        if (isNaN(num)) {
            this.showFieldError(input, 'Please enter a valid number');
            return false;
        }
        
        if (num < min) {
            this.showFieldError(input, `Value must be at least ${min}`);
            return false;
        }
        
        if (num > max) {
            this.showFieldError(input, `Value must be no more than ${max}`);
            return false;
        }
        
        this.clearFieldError(input);
        return true;
    }
    
    validateRequiredField(input) {
        const value = input.value.trim();
        if (value === '') {
            this.showFieldError(input, 'This field is required');
            return false;
        }
        this.clearFieldError(input);
        return true;
    }
    
    showFieldError(input, message) {
        this.clearFieldError(input);
        
        input.classList.add('is-invalid');
        const errorDiv = document.createElement('div');
        errorDiv.className = 'invalid-feedback';
        errorDiv.textContent = message;
        input.parentNode.appendChild(errorDiv);
    }
    
    clearFieldError(input) {
        input.classList.remove('is-invalid');
        const errorDiv = input.parentNode.querySelector('.invalid-feedback');
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    closeModal() {
        document.getElementById('modalOverlay').classList.add('hidden');
    }

    showSuccess(message) {
        // Implementation for showing success messages
        console.log(message);
    }

    showError(message) {
        // Implementation for showing error messages
        console.error(message);
    }
}

// Initialize Data Collection Manager
const dataCollectionManager = new DataCollectionManager();

// Export for use in other files
window.dataCollectionManager = dataCollectionManager;
