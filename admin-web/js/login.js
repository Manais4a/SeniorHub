// Login Page Controller
// Login Page Controller
class LoginController {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthState();
    }

    setupEventListeners() {
        // No tab switching needed for new design

        // Login form
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleLogin();
            });
        }

        // No register form in new design

        // No password validation needed for new design

        this.fixInputFields();
    }

    checkAuthState() {
        // Only redirect if user is already authenticated
        auth.onAuthStateChanged((user) => {
            if (user) {
                console.log('User already logged in, redirecting to dashboard');
                // Small delay to ensure auth is stable
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1000);
            } else {
                this.showAuthScreen();
            }
        });
    }

    async handleLogin() {
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        const role = document.querySelector('input[name="loginRole"]:checked')?.value || 'admin';

        if (!email || !password) {
            this.showError('Please enter your email and password');
            return;
        }

        const loginBtn = document.querySelector('#loginForm button[type="submit"]');
        this.setButtonLoading(loginBtn, true);

        try {
            // Set auth persistence before signing in
            await auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL);
            
            // Sign in with Firebase Auth
            const userCredential = await auth.signInWithEmailAndPassword(email, password);
            const user = userCredential.user;
            
            console.log('Login successful, user:', user.email);

            // Verify admin role
            const adminDoc = await db.collection(COLLECTIONS.ADMIN_USERS).doc(user.uid).get();
            
            if (!adminDoc.exists) {
                await auth.signOut();
                throw new Error('Admin account not found');
            }

            const adminData = adminDoc.data();
            const storedRole = (adminData.role || '').toLowerCase().trim();
            
            // All admin users have the same role now
            if (storedRole !== 'admin') {
                await auth.signOut();
                throw new Error('Invalid account type. This is not an admin account.');
            }

            if (adminData.isActive === false) {
                await auth.signOut();
                throw new Error('Account is deactivated');
            }

            // Update last login
            await db.collection(COLLECTIONS.ADMIN_USERS).doc(user.uid).update({
                lastLogin: FirebaseUtils.getTimestamp()
            });

            // Store role in session storage for dashboard initialization
            sessionStorage.setItem('userRole', 'admin');
            sessionStorage.setItem('userEmail', user.email);
            sessionStorage.setItem('userName', adminData.name || user.displayName);

            this.showSuccess('Login successful! Redirecting to admin dashboard...');
            
            // Wait for auth state to be fully established
            console.log('Waiting for auth state to stabilize...');
            
            // Use a more reliable redirect approach
            setTimeout(() => {
                console.log('Redirecting to admin dashboard...');
                // Force reload to ensure fresh state
                window.location.href = 'index.html';
            }, 1500);

        } catch (error) {
            console.error('Login error:', error);
            this.showError(this.getErrorMessage(error));
        } finally {
            this.setButtonLoading(loginBtn, false);
        }
    }

    async handleRegister() {
        const name = document.getElementById('regName').value;
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;
        const confirmPassword = document.getElementById('regConfirmPassword').value;
        const role = document.getElementById('regRole').value;
        const department = document.getElementById('regDepartment').value;

        // Validation
        if (!name || !email || !password || !confirmPassword || !role || !department) {
            this.showError('Please fill in all fields');
            return;
        }

        if (password !== confirmPassword) {
            this.showError('Passwords do not match');
            return;
        }

        if (password.length < 6) {
            this.showError('Password must be at least 6 characters long');
            return;
        }

        const registerBtn = document.querySelector('#registerForm button[type="submit"]');
        this.setButtonLoading(registerBtn, true);

        try {
            // Create user with Firebase Auth
            const userCredential = await auth.createUserWithEmailAndPassword(email, password);
            const user = userCredential.user;

            // Create admin user document
            await db.collection(COLLECTIONS.ADMIN_USERS).doc(user.uid).set({
                name: name,
                email: email,
                role: role,
                department: department,
                isActive: true,
                createdAt: FirebaseUtils.getTimestamp(),
                updatedAt: FirebaseUtils.getTimestamp(),
                lastLogin: FirebaseUtils.getTimestamp()
            });

            // Update user profile
            await user.updateProfile({
                displayName: name
            });

            this.showSuccess('Registration successful! Please wait for approval.');
            
            // Sign out and show login
            await auth.signOut();
            setTimeout(() => {
                this.switchTab('login');
                document.getElementById('loginEmail').value = email;
            }, 2000);

        } catch (error) {
            console.error('Registration error:', error);
            this.showError(this.getErrorMessage(error));
        } finally {
            this.setButtonLoading(registerBtn, false);
        }
    }

    validatePasswordConfirmation() {
        const password = document.getElementById('regPassword').value;
        const confirmPassword = document.getElementById('regConfirmPassword').value;
        const confirmField = document.getElementById('regConfirmPassword');

        if (confirmPassword && password !== confirmPassword) {
            confirmField.classList.add('error');
            this.showFieldError(confirmField, 'Passwords do not match');
        } else {
            confirmField.classList.remove('error');
            this.clearFieldError(confirmField);
        }
    }

    updatePasswordStrength(password) {
        const strengthBar = document.querySelector('.password-strength-fill');
        const strengthText = document.querySelector('.password-strength-text');
        
        if (!strengthBar || !strengthText) return;

        const strength = this.calculatePasswordStrength(password);
        
        strengthBar.className = 'password-strength-fill';
        strengthBar.classList.add(strength.level);
        
        strengthText.textContent = strength.text;
    }

    calculatePasswordStrength(password) {
        let score = 0;
        let feedback = [];

        if (password.length >= 8) score += 1;
        else feedback.push('at least 8 characters');

        if (/[a-z]/.test(password)) score += 1;
        else feedback.push('lowercase letters');

        if (/[A-Z]/.test(password)) score += 1;
        else feedback.push('uppercase letters');

        if (/[0-9]/.test(password)) score += 1;
        else feedback.push('numbers');

        if (/[^A-Za-z0-9]/.test(password)) score += 1;
        else feedback.push('special characters');

        if (score < 2) {
            return { level: 'weak', text: 'Weak password' };
        } else if (score < 3) {
            return { level: 'fair', text: 'Fair password' };
        } else if (score < 4) {
            return { level: 'good', text: 'Good password' };
        } else {
            return { level: 'strong', text: 'Strong password' };
        }
    }

    setButtonLoading(button, loading) {
        if (loading) {
            button.classList.add('loading');
            button.disabled = true;
        } else {
            button.classList.remove('loading');
            button.disabled = false;
        }
    }

    showError(message) {
        this.clearMessages();
        const errorDiv = document.createElement('div');
        errorDiv.className = 'auth-message error';
        errorDiv.innerHTML = `
            <i class="fas fa-exclamation-circle"></i>
            <span>${message}</span>
        `;
        
        const container = document.querySelector('.auth-form-container');
        container.insertBefore(errorDiv, container.firstChild);
    }

    showSuccess(message) {
        this.clearMessages();
        const successDiv = document.createElement('div');
        successDiv.className = 'auth-message success';
        successDiv.innerHTML = `
            <i class="fas fa-check-circle"></i>
            <span>${message}</span>
        `;
        
        const container = document.querySelector('.auth-form-container');
        container.insertBefore(successDiv, container.firstChild);
    }

    showFieldError(field, message) {
        this.clearFieldError(field);
        const errorDiv = document.createElement('div');
        errorDiv.className = 'form-error';
        errorDiv.innerHTML = `
            <i class="fas fa-exclamation-circle"></i>
            <span>${message}</span>
        `;
        field.parentNode.appendChild(errorDiv);
    }

    clearFieldError(field) {
        const errorDiv = field.parentNode.querySelector('.form-error');
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    getErrorMessage(error) {
        switch (error.code) {
            case 'auth/user-not-found':
                return 'No account found with this email address';
            case 'auth/wrong-password':
                return 'Incorrect password';
            case 'auth/email-already-in-use':
                return 'An account with this email already exists';
            case 'auth/weak-password':
                return 'Password is too weak';
            case 'auth/invalid-email':
                return 'Invalid email address';
            case 'auth/too-many-requests':
                return 'Too many failed attempts. Please try again later';
            default:
                return error.message || 'An error occurred. Please try again';
        }
    }

    getRoleDisplayName(role) {
        const roleMap = {
            'super_admin': 'Super Admin',
            'facilitator': 'Facilitator',
            'admin': 'Admin'
        };
        return roleMap[role] || 'Admin';
    }

    showAuthScreen() {
        document.getElementById('loadingScreen').classList.add('hidden');
        document.getElementById('authScreen').classList.remove('hidden');
    }

     showManualRedirectButton() {
        // Check if we're still on the login page
        if (window.location.pathname.includes('login.html')) {
            const container = document.querySelector('.auth-form-container');
            const existingManual = container.querySelector('.manual-redirect');
            if (!existingManual) {
                const manualRedirectDiv = document.createElement('div');
                manualRedirectDiv.className = 'auth-message info manual-redirect';
                manualRedirectDiv.innerHTML = `
                    <i class="fas fa-info-circle"></i>
                    <span>Login successful! If not redirected automatically:</span>
                    <br><br>
                    <button onclick="window.location.href='index.html'" class="btn btn-primary" style="margin-top: 10px;">
                        <i class="fas fa-arrow-right"></i>
                        Go to Dashboard
                    </button>
                `;
                container.appendChild(manualRedirectDiv);
            }
        }
    }


    fixInputFields() {
        // Remove any problematic attributes that might prevent input
        const inputs = document.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            // Remove readonly attribute if present
            input.removeAttribute('readonly');
            
            // Ensure input is not disabled
            input.disabled = false;
            
            // Remove any problematic event listeners
            input.onkeydown = null;
            input.onkeyup = null;
            input.onkeypress = null;
            
            // Add proper event listeners
            input.addEventListener('keydown', (e) => {
                // Allow all key inputs
                e.stopPropagation();
            });
            
            input.addEventListener('input', (e) => {
                // Clear any error states
                e.target.classList.remove('error');
                this.clearFieldError(e.target);
            });
        });
    }
}

// Initialize Login Controller
const loginController = new LoginController();
