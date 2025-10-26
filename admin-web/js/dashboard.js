// Dashboard Management
class DashboardManager {
    constructor() {
        this.stats = {};
        this.recentActivities = [];
        this.isLoading = false;
        this.dataLoaded = false;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadDashboardData();
    }

    setupEventListeners() {
        // Real-time updates
        this.setupRealtimeUpdates();
    }

    setupRealtimeUpdates() {
        // Only set up real-time updates if not already set up
        if (this.realtimeSetupComplete) {
            return;
        }
        
        // Debounce updates to prevent constant refreshing
        this.updateUserStatsDebounced = this.debounce(this.updateUserStats.bind(this), 2000);
        this.updateBenefitsStatsDebounced = this.debounce(this.updateBenefitsStats.bind(this), 2000);
        this.updateEmergencyStatsDebounced = this.debounce(this.updateEmergencyStats.bind(this), 2000);
        this.updateRecentActivitiesDebounced = this.debounce(this.updateRecentActivities.bind(this), 2000);

        // Listen for real-time updates from various collections
        FirebaseUtils.onSnapshot(COLLECTIONS.USERS, (snapshot) => {
            this.updateUserStatsDebounced(snapshot);
        });

        FirebaseUtils.onSnapshot(COLLECTIONS.BENEFITS, (snapshot) => {
            this.updateBenefitsStatsDebounced(snapshot);
        });

        FirebaseUtils.onSnapshot(COLLECTIONS.EMERGENCY_ALERTS, (snapshot) => {
            this.updateEmergencyStatsDebounced(snapshot);
        });

        FirebaseUtils.onSnapshot(COLLECTIONS.ACTIVITIES, (snapshot) => {
            this.updateRecentActivitiesDebounced(snapshot);
        });
        
        this.realtimeSetupComplete = true;
    }

    // Debounce function to prevent excessive updates
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    async loadDashboardData() {
        // Prevent multiple simultaneous loads
        if (this.isLoading) {
            console.log('Dashboard already loading, skipping...');
            return;
        }
        
        // If data is already loaded, don't reload unless forced
        if (this.dataLoaded && !this.forceReload) {
            console.log('Dashboard data already loaded, skipping...');
            return;
        }
        
        this.isLoading = true;
        
        try {
            this.showLoading();
            
            // Load all dashboard data in parallel
            const loadPromises = [
                this.loadUserStats(),
                this.loadBenefitsStats(),
                this.loadEmergencyStats(),
                this.loadRecentActivities()
            ];

            // Add role-specific data loading
            const currentUserRole = window.mainApp?.currentUserRole;
            if (currentUserRole === 'facilitator' || currentUserRole === 'admin') {
                loadPromises.push(this.loadSeniorDemographics());
            } else if (currentUserRole === 'super_admin') {
                loadPromises.push(
                    this.loadAdminStats(),
                    this.loadSystemHealth(),
                    this.loadPerformanceMetrics(),
                    this.loadSeniorDemographics() // Super admins should also see demographics
                );
            }

            await Promise.all(loadPromises);
            this.updateDashboardUI();
            this.dataLoaded = true;
            this.forceReload = false;
        } catch (error) {
            console.error('Error loading dashboard data:', error);
            this.showError('Failed to load dashboard data');
        } finally {
            this.hideLoading();
            this.isLoading = false;
        }
    }

    async loadUserStats() {
        try {
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            const seniorCitizens = users.filter(user => user.role === 'senior_citizen');
            
            // For facilitator admin: Total Seniors includes all users (verified and unverified)
            const currentUserRole = window.mainApp?.currentUserRole;
            if (currentUserRole === 'facilitator' || currentUserRole === 'super_admin') {
                this.stats.totalSeniors = users.length; // All users (verified and unverified)
            } else {
                this.stats.totalSeniors = seniorCitizens.length;
            }
            
            // For facilitator admin: Verified Accounts replaces Active Users
            if (currentUserRole === 'facilitator' || currentUserRole === 'super_admin') {
                this.stats.verifiedAccountsFacilitator = users.filter(user => user.accountVerified || user.isVerified).length;
            } else {
                this.stats.activeSeniors = seniorCitizens.filter(senior => senior.isActive).length;
            }
            
            this.stats.newThisMonth = seniorCitizens.filter(senior => 
                this.isNewThisMonth(senior.createdAt)
            ).length;
        } catch (error) {
            console.error('Error loading user stats:', error);
        }
    }

    async loadBenefitsStats() {
        try {
            const benefits = await FirebaseUtils.getCollection(COLLECTIONS.BENEFITS);
            const claimedBenefits = await FirebaseUtils.getCollection('claimed_benefits');
            
            this.stats.totalBenefits = benefits.length;
            this.stats.availableBenefits = benefits.filter(benefit => 
                benefit.status === 'Available' && benefit.isActive
            ).length;
            this.stats.totalClaimed = claimedBenefits.length;
        } catch (error) {
            console.error('Error loading benefits stats:', error);
        }
    }

    async loadEmergencyStats() {
        try {
            const emergencyAlerts = await FirebaseUtils.getCollection(COLLECTIONS.EMERGENCY_ALERTS);
            const now = new Date();
            const last24Hours = new Date(now.getTime() - (24 * 60 * 60 * 1000));
            
            this.stats.emergencyAlerts = emergencyAlerts.length;
            this.stats.recentEmergencies = emergencyAlerts.filter(alert => {
                const alertTime = alert.timestamp ? alert.timestamp.toDate() : new Date();
                return alertTime >= last24Hours;
            }).length;
            this.stats.activeEmergencies = emergencyAlerts.filter(alert => 
                alert.status === 'ACTIVE'
            ).length;
        } catch (error) {
            console.error('Error loading emergency stats:', error);
        }
    }

    async loadRecentActivities() {
        try {
            const activities = await FirebaseUtils.getCollection(COLLECTIONS.ACTIVITIES, 'timestamp', 10);
            const appointments = await FirebaseUtils.getCollection(COLLECTIONS.APPOINTMENTS, 'dateTime', 5);
            
            // Combine and sort activities
            const allActivities = [
                ...activities.map(activity => ({
                    type: 'activity',
                    title: activity.title || 'Activity',
                    description: activity.description || '',
                    timestamp: activity.timestamp,
                    icon: 'fas fa-user'
                })),
                ...appointments.map(appointment => ({
                    type: 'appointment',
                    title: appointment.title || 'Appointment',
                    description: `With ${appointment.doctorName || 'Doctor'}`,
                    timestamp: appointment.dateTime,
                    icon: 'fas fa-calendar'
                }))
            ];

            // Sort by timestamp (most recent first)
            this.recentActivities = allActivities
                .sort((a, b) => {
                    const timeA = a.timestamp ? a.timestamp.toDate() : new Date(a.timestamp);
                    const timeB = b.timestamp ? b.timestamp.toDate() : new Date(b.timestamp);
                    return timeB - timeA;
                })
                .slice(0, 10);
        } catch (error) {
            console.error('Error loading recent activities:', error);
        }
    }

    // Facilitator-specific analytics methods
    async loadHealthAnalytics() {
        try {
            const healthRecords = await FirebaseUtils.getCollection('health_records');
            const now = new Date();
            const last30Days = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));
            
            this.stats.healthRecords = healthRecords.length;
            this.stats.recentHealthUpdates = healthRecords.filter(record => {
                const updateTime = record.updatedAt ? record.updatedAt.toDate() : new Date();
                return updateTime >= last30Days;
            }).length;
            
            // Calculate health trends
            const healthConditions = healthRecords.reduce((acc, record) => {
                if (record.conditions) {
                    record.conditions.forEach(condition => {
                        acc[condition] = (acc[condition] || 0) + 1;
                    });
                }
                return acc;
            }, {});
            
            this.stats.topHealthConditions = Object.entries(healthConditions)
                .sort(([,a], [,b]) => b - a)
                .slice(0, 5);
                
        } catch (error) {
            console.error('Error loading health analytics:', error);
        }
    }

    async loadAppointmentStats() {
        try {
            const appointments = await FirebaseUtils.getCollection(COLLECTIONS.APPOINTMENTS);
            const now = new Date();
            const next7Days = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000));
            
            this.stats.totalAppointments = appointments.length;
            this.stats.upcomingAppointments = appointments.filter(appointment => {
                const appointmentTime = appointment.dateTime ? appointment.dateTime.toDate() : new Date();
                return appointmentTime >= now && appointmentTime <= next7Days;
            }).length;
            
            this.stats.completedAppointments = appointments.filter(appointment => 
                appointment.status === 'completed'
            ).length;
            
        } catch (error) {
            console.error('Error loading appointment stats:', error);
        }
    }


    async loadSeniorDemographics() {
        try {
            console.log('Loading senior demographics...');
            const users = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            const seniorCitizens = users.filter(user => user.role === 'senior_citizen');
            
            console.log('Total users found:', users.length);
            console.log('Senior citizens found:', seniorCitizens.length);
            console.log('Senior citizens data:', seniorCitizens.map(s => ({ name: `${s.firstName} ${s.lastName}`, age: s.age, gender: s.gender })));
            
            // Age demographics
            const ageGroups = {
                '60-69': 0,
                '70-79': 0,
                '80-89': 0,
                '90+': 0
            };
            
            seniorCitizens.forEach(senior => {
                const age = senior.age || 0;
                if (age >= 60 && age <= 69) ageGroups['60-69']++;
                else if (age >= 70 && age <= 79) ageGroups['70-79']++;
                else if (age >= 80 && age <= 89) ageGroups['80-89']++;
                else if (age >= 90) ageGroups['90+']++;
            });
            
            this.stats.ageDemographics = Object.entries(ageGroups);
            
            // Gender demographics
            const genderGroups = {
                'Male': 0,
                'Female': 0
            };
            
            seniorCitizens.forEach(senior => {
                const gender = senior.gender ? senior.gender.toLowerCase() : '';
                if (gender === 'male') genderGroups['Male']++;
                else if (gender === 'female') genderGroups['Female']++;
                // Removed 'Other' category - only count Male and Female
            });
            
            this.stats.genderDemographics = Object.entries(genderGroups);
                
        } catch (error) {
            console.error('Error loading senior demographics:', error);
        }
    }

    updateUserStats(snapshot) {
        const seniorCitizens = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            if (data.role === 'senior_citizen') {
                seniorCitizens.push(data);
            }
        });

        this.stats.totalSeniors = seniorCitizens.length;
        this.stats.activeSeniors = seniorCitizens.filter(senior => senior.isActive).length;
        this.updateStatsUI();
    }

    updateBenefitsStats(snapshot) {
        const benefits = [];
        snapshot.forEach(doc => {
            benefits.push(doc.data());
        });

        this.stats.totalBenefits = benefits.length;
        this.stats.availableBenefits = benefits.filter(benefit => 
            benefit.status === 'Available' && benefit.isActive
        ).length;
        this.updateStatsUI();
    }

    updateEmergencyStats(snapshot) {
        const emergencyAlerts = [];
        snapshot.forEach(doc => {
            emergencyAlerts.push(doc.data());
        });

        this.stats.emergencyAlerts = emergencyAlerts.length;
        this.stats.activeEmergencies = emergencyAlerts.filter(alert => 
            alert.status === 'ACTIVE'
        ).length;
        this.updateStatsUI();
    }

    updateRecentActivities(snapshot) {
        const activities = [];
        snapshot.forEach(doc => {
            activities.push({ id: doc.id, ...doc.data() });
        });

        // Update recent activities (keep existing appointments)
        const currentAppointments = this.recentActivities.filter(activity => 
            activity.type === 'appointment'
        );

        const newActivities = activities
            .map(activity => ({
                type: 'activity',
                title: activity.title || 'Activity',
                description: activity.description || '',
                timestamp: activity.timestamp,
                icon: 'fas fa-user'
            }))
            .slice(0, 5);

        this.recentActivities = [...newActivities, ...currentAppointments]
            .sort((a, b) => {
                const timeA = a.timestamp ? a.timestamp.toDate() : new Date(a.timestamp);
                const timeB = b.timestamp ? b.timestamp.toDate() : new Date(b.timestamp);
                return timeB - timeA;
            })
            .slice(0, 10);

        this.updateActivitiesUI();
    }

    updateDashboardUI() {
        this.updateStatsUI();
        this.updateActivitiesUI();
        
        // Update facilitator-specific analytics
        const currentUserRole = window.mainApp?.currentUserRole;
        if (currentUserRole === 'facilitator' || currentUserRole === 'admin' || currentUserRole === 'super_admin') {
            this.updateSeniorDemographicsUI();
        }
    }

    updateStatsUI() {
        // Update basic stat cards with null checks
        const totalSeniorsElement = document.getElementById('totalSeniors');
        if (totalSeniorsElement) {
            totalSeniorsElement.textContent = this.stats.totalSeniors || 0;
        }
        
        const totalBenefitsElement = document.getElementById('totalBenefits');
        if (totalBenefitsElement) {
            totalBenefitsElement.textContent = this.stats.totalBenefits || 0;
        }
        
        const emergencyAlertsElement = document.getElementById('emergencyAlerts');
        if (emergencyAlertsElement) {
            emergencyAlertsElement.textContent = this.stats.emergencyAlerts || 0;
        }

        // Update role-specific stat cards
        const currentUserRole = window.mainApp?.currentUserRole;
        if (currentUserRole === 'facilitator' || currentUserRole === 'super_admin') {
            // For facilitator admin: show verified accounts
            const verifiedElement = document.getElementById('verifiedAccountsFacilitator');
            if (verifiedElement) {
                verifiedElement.textContent = this.stats.verifiedAccountsFacilitator || 0;
            }
        } else {
            // For other admin types: show active seniors
            const activeElement = document.getElementById('activeSeniors');
            if (activeElement) {
                activeElement.textContent = this.stats.activeSeniors || 0;
            }
        }

        // Add animation to stat cards
        this.animateStatCards();
    }

    updateActivitiesUI() {
        const activitiesContainer = document.getElementById('recentActivities');
        if (!activitiesContainer) return;

        if (this.recentActivities.length === 0) {
            activitiesContainer.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-clock"></i>
                    <p>No recent activities</p>
                </div>
            `;
            return;
        }

        activitiesContainer.innerHTML = this.recentActivities
            .map(activity => this.createActivityItem(activity))
            .join('');
    }

    createActivityItem(activity) {
        const timeAgo = this.getTimeAgo(activity.timestamp);
        const activityClass = activity.type === 'emergency' ? 'emergency' : 'normal';
        
        return `
            <div class="activity-item ${activityClass}">
                <div class="activity-icon">
                    <i class="${activity.icon}"></i>
                </div>
                <div class="activity-content">
                    <h4>${activity.title}</h4>
                    <p>${activity.description}</p>
                    <span class="activity-time">${timeAgo}</span>
                </div>
            </div>
        `;
    }

    updateSeniorDemographicsUI() {
        console.log('Updating senior demographics UI...');
        const demographicsContainer = document.getElementById('seniorDemographics');
        if (!demographicsContainer) {
            console.error('Demographics container not found!');
            return;
        }
        
        console.log('Demographics data:', this.stats.ageDemographics, this.stats.genderDemographics);

        const ageData = this.stats.ageDemographics || [];
        const genderData = this.stats.genderDemographics || [];
        const totalSeniors = this.stats.totalSeniors || 0;
        
        // Calculate percentages
        const ageDataWithPercentages = ageData.map(([ageGroup, count]) => ({
            group: ageGroup,
            count: count,
            percentage: totalSeniors > 0 ? Math.round((count / totalSeniors) * 100) : 0
        }));
        
        const genderDataWithPercentages = genderData.map(([gender, count]) => ({
            group: gender,
            count: count,
            percentage: totalSeniors > 0 ? Math.round((count / totalSeniors) * 100) : 0
        }));
        
        demographicsContainer.innerHTML = `
            <div class="demographics-overview">
                <div class="overview-stats">
                    <div class="overview-item">
                        <span class="overview-label">Total Seniors</span>
                        <span class="overview-value">${totalSeniors}</span>
                    </div>
                    <div class="overview-item">
                        <span class="overview-label">Average Age</span>
                        <span class="overview-value">${this.calculateAverageAge()}</span>
                    </div>
                </div>
            </div>
            
            <div class="demographics-grid">
                <div class="demographic-section">
                    <div class="section-header">
                        <h4><i class="fas fa-birthday-cake"></i> Age Distribution</h4>
                        <span class="section-total">${totalSeniors} total</span>
                    </div>
                    <div class="demographic-chart">
                        ${ageDataWithPercentages.map(item => `
                            <div class="demographic-item">
                                <div class="demographic-info">
                                    <span class="demographic-label">${item.group} years</span>
                                    <span class="demographic-count">${item.count} (${item.percentage}%)</span>
                                </div>
                                <div class="demographic-bar">
                                    <div class="demographic-fill" style="width: ${item.percentage}%"></div>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
                
                <div class="demographic-section">
                    <div class="section-header">
                        <h4><i class="fas fa-venus-mars"></i> Gender Distribution</h4>
                        <span class="section-total">${totalSeniors} total</span>
                    </div>
                    <div class="demographic-chart">
                        ${genderDataWithPercentages.map(item => `
                            <div class="demographic-item">
                                <div class="demographic-info">
                                    <span class="demographic-label">${item.group}</span>
                                    <span class="demographic-count">${item.count} (${item.percentage}%)</span>
                                </div>
                                <div class="demographic-bar">
                                    <div class="demographic-fill" style="width: ${item.percentage}%"></div>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
            
            <div class="demographics-summary">
                <div class="summary-item">
                    <i class="fas fa-users"></i>
                    <span>Largest Age Group: ${this.getLargestAgeGroup(ageDataWithPercentages)}</span>
                </div>
                <div class="summary-item">
                    <i class="fas fa-chart-pie"></i>
                    <span>Gender Balance: ${this.getGenderBalance(genderDataWithPercentages)}</span>
                </div>
            </div>
        `;
    }
    
    calculateAverageAge() {
        const ageData = this.stats.ageDemographics || [];
        if (ageData.length === 0) return 'N/A';
        
        let totalAge = 0;
        let totalCount = 0;
        
        ageData.forEach(([ageGroup, count]) => {
            const ageRange = ageGroup.split('-');
            if (ageRange.length === 2) {
                const avgAge = (parseInt(ageRange[0]) + parseInt(ageRange[1])) / 2;
                totalAge += avgAge * count;
                totalCount += count;
            } else if (ageGroup === '90+') {
                totalAge += 95 * count; // Assume average of 95 for 90+
                totalCount += count;
            }
        });
        
        return totalCount > 0 ? Math.round(totalAge / totalCount) : 'N/A';
    }
    
    getLargestAgeGroup(ageData) {
        if (ageData.length === 0) return 'N/A';
        const largest = ageData.reduce((max, current) => 
            current.count > max.count ? current : max
        );
        return `${largest.group} (${largest.count} seniors)`;
    }
    
    getGenderBalance(genderData) {
        if (genderData.length === 0) return 'N/A';
        const male = genderData.find(g => g.group === 'Male')?.count || 0;
        const female = genderData.find(g => g.group === 'Female')?.count || 0;
        const total = male + female;
        
        if (total === 0) return 'No data';
        
        const malePercent = Math.round((male / total) * 100);
        const femalePercent = Math.round((female / total) * 100);
        
        return `${malePercent}% Male, ${femalePercent}% Female`;
    }
    
    refreshDemographics() {
        console.log('Refreshing demographics data...');
        this.loadSeniorDemographics();
    }

    getTimeAgo(timestamp) {
        if (!timestamp) return 'Unknown time';
        
        const now = new Date();
        const time = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
        const diffMs = now - time;
        const diffMinutes = Math.floor(diffMs / (1000 * 60));
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

        if (diffMinutes < 1) return 'Just now';
        if (diffMinutes < 60) return `${diffMinutes}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        if (diffDays < 7) return `${diffDays}d ago`;
        return time.toLocaleDateString();
    }

    isNewThisMonth(timestamp) {
        if (!timestamp) return false;
        const now = new Date();
        const createdDate = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
        return createdDate.getMonth() === now.getMonth() && 
               createdDate.getFullYear() === now.getFullYear();
    }

    animateStatCards() {
        const statCards = document.querySelectorAll('.stat-card');
        statCards.forEach((card, index) => {
            setTimeout(() => {
                card.style.transform = 'scale(1.05)';
                setTimeout(() => {
                    card.style.transform = 'scale(1)';
                }, 200);
            }, index * 100);
        });
    }

    showLoading(message = 'Loading...') {
        // Implementation for loading state
        console.log(message);
    }

    hideLoading() {
        // Implementation for hiding loading state
    }

    showError(message) {
        // Implementation for showing error messages
        console.error(message);
    }

    showSuccess(message) {
        // Implementation for showing success messages
        console.log(message);
    }

    // Verification queue entry point (for Facilitator and Super Admin)
    openVerificationQueue() {
        // Placeholder hook; can be expanded to load pending_review items
        console.log('Open verification queue');
    }

    // Force refresh dashboard data
    async refreshDashboard() {
        this.forceReload = true;
        this.dataLoaded = false;
        await this.loadDashboardData();
    }
}

// Initialize Dashboard Manager
const dashboardManager = new DashboardManager();

// Export for use in other files
window.dashboardManager = dashboardManager;
