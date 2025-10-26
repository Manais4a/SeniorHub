// Analytics and Data Visualization
class AnalyticsManager {
    constructor() {
        this.charts = {};
        this.analyticsData = {};
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadAnalyticsData();
    }

    setupEventListeners() {
        // Chart resize handling
        window.addEventListener('resize', () => {
            this.resizeCharts();
        });
    }

    async loadAnalyticsData() {
        try {
            this.showLoading();
            
            // Load all analytics data in parallel
            await Promise.all([
                this.loadDemographicsData(),
                this.loadBenefitsData()
            ]);

            this.renderCharts();
        } catch (error) {
            console.error('Error loading analytics data:', error);
            this.showError('Failed to load analytics data');
        } finally {
            this.hideLoading();
        }
    }

    async loadDemographicsData() {
        try {
            const seniors = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            const seniorCitizens = seniors.filter(user => user.role === 'senior_citizen');
            
            // Age distribution
            const ageGroups = {
                '60-69': 0,
                '70-79': 0,
                '80-89': 0,
                '90+': 0
            };

            // Gender distribution
            const genderDistribution = {
                'Male': 0,
                'Female': 0
            };

            // Location distribution
            const locationDistribution = {};
            
            // Verified accounts count
            let verifiedAccounts = 0;

            seniorCitizens.forEach(senior => {
                // Age groups
                if (senior.age >= 60 && senior.age <= 69) ageGroups['60-69']++;
                else if (senior.age >= 70 && senior.age <= 79) ageGroups['70-79']++;
                else if (senior.age >= 80 && senior.age <= 89) ageGroups['80-89']++;
                else if (senior.age >= 90) ageGroups['90+']++;

                // Gender
                const gender = senior.gender ? senior.gender.toLowerCase() : '';
                if (gender === 'male') genderDistribution['Male']++;
                else if (gender === 'female') genderDistribution['Female']++;
                // Removed 'Other' category - only count Male and Female

                // Location
                const city = senior.city || 'Unknown';
                locationDistribution[city] = (locationDistribution[city] || 0) + 1;
                
                // Verified accounts
                if (senior.accountVerified === true || senior.isVerified === true) {
                    verifiedAccounts++;
                }
            });

            this.analyticsData.demographics = {
                ageGroups,
                genderDistribution,
                locationDistribution,
                totalSeniors: seniorCitizens.length,
                verifiedAccounts: verifiedAccounts
            };
            
            console.log('Demographics data loaded:', this.analyticsData.demographics);
        } catch (error) {
            console.error('Error loading demographics data:', error);
        }
    }

    async loadBenefitsData() {
        try {
            const benefits = await FirebaseUtils.getCollection(COLLECTIONS.BENEFITS);
            const claimedBenefits = await FirebaseUtils.getCollection('claimed_benefits');
            
            // Benefits by category
            const benefitsByCategory = {};
            const benefitsByStatus = {
                'Available': 0,
                'Active': 0,
                'Pending': 0,
                'Discontinued': 0
            };

            benefits.forEach(benefit => {
                // Category distribution
                const category = benefit.category || 'Other';
                benefitsByCategory[category] = (benefitsByCategory[category] || 0) + 1;

                // Status distribution
                const status = benefit.status || 'Available';
                benefitsByStatus[status] = (benefitsByStatus[status] || 0) + 1;
            });

            this.analyticsData.benefits = {
                benefitsByCategory,
                benefitsByStatus,
                totalBenefits: benefits.length,
                totalClaimed: claimedBenefits.length
            };
        } catch (error) {
            console.error('Error loading benefits data:', error);
        }
    }

    async loadHealthData() {
        try {
            const seniors = await FirebaseUtils.getCollection(COLLECTIONS.USERS);
            const seniorCitizens = seniors.filter(user => user.role === 'senior_citizen');
            
            // Health conditions
            const healthConditions = {};
            const medicationUsage = {};
            const emergencyAlerts = await FirebaseUtils.getCollection(COLLECTIONS.EMERGENCY_ALERTS);

            seniorCitizens.forEach(senior => {
                // Medical conditions
                if (senior.medicalConditions && Array.isArray(senior.medicalConditions)) {
                    senior.medicalConditions.forEach(condition => {
                        healthConditions[condition] = (healthConditions[condition] || 0) + 1;
                    });
                }

                // Medications
                if (senior.medications && Array.isArray(senior.medications)) {
                    senior.medications.forEach(medication => {
                        const medName = medication.name || 'Unknown';
                        medicationUsage[medName] = (medicationUsage[medName] || 0) + 1;
                    });
                }
            });

            // Emergency alerts by type
            const emergencyTypes = {};
            emergencyAlerts.forEach(alert => {
                const type = alert.type || 'Unknown';
                emergencyTypes[type] = (emergencyTypes[type] || 0) + 1;
            });

            this.analyticsData.health = {
                healthConditions,
                medicationUsage,
                emergencyTypes,
                totalEmergencies: emergencyAlerts.length
            };
        } catch (error) {
            console.error('Error loading health data:', error);
        }
    }

    async loadActivityData() {
        try {
            const activities = await FirebaseUtils.getCollection(COLLECTIONS.ACTIVITIES);
            const appointments = await FirebaseUtils.getCollection(COLLECTIONS.APPOINTMENTS);
            
            // Activity trends (last 30 days)
            const activityTrends = {};
            const appointmentTrends = {};
            
            const now = new Date();
            const thirtyDaysAgo = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));

            // Process activities
            activities.forEach(activity => {
                const activityDate = activity.timestamp ? activity.timestamp.toDate() : new Date();
                if (activityDate >= thirtyDaysAgo) {
                    const dateKey = activityDate.toISOString().split('T')[0];
                    activityTrends[dateKey] = (activityTrends[dateKey] || 0) + 1;
                }
            });

            // Process appointments
            appointments.forEach(appointment => {
                const appointmentDate = new Date(appointment.dateTime);
                if (appointmentDate >= thirtyDaysAgo) {
                    const dateKey = appointmentDate.toISOString().split('T')[0];
                    appointmentTrends[dateKey] = (appointmentTrends[dateKey] || 0) + 1;
                }
            });

            this.analyticsData.activity = {
                activityTrends,
                appointmentTrends,
                totalActivities: activities.length,
                totalAppointments: appointments.length
            };
        } catch (error) {
            console.error('Error loading activity data:', error);
        }
    }

    renderCharts() {
        console.log('Rendering all charts...');
        this.renderDemographicsChart();
        this.renderGenderChart();
        this.renderBenefitsChart();
    }

    renderDemographicsChart() {
        const ctx = document.getElementById('demographicsChart');
        if (!ctx || !this.analyticsData.demographics) {
            console.log('Demographics chart not rendered - ctx:', !!ctx, 'data:', !!this.analyticsData.demographics);
            return;
        }

        const data = this.analyticsData.demographics;
        console.log('Rendering demographics chart with data:', data);
        
        // Update KPI
        document.getElementById('totalSeniorsKPI').textContent = data.totalSeniors || 0;
        document.getElementById('verifiedAccountsKPI').textContent = data.verifiedAccounts || 0;
        
        this.charts.demographics = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: Object.keys(data.ageGroups),
                datasets: [{
                    data: Object.values(data.ageGroups),
                    backgroundColor: [
                        '#007bff',
                        '#00bcd4',
                        '#28a745',
                        '#ffc107'
                    ],
                    borderWidth: 3,
                    borderColor: '#ffffff',
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 20,
                            usePointStyle: true,
                            font: {
                                size: 12,
                                weight: '500'
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#ffffff',
                        bodyColor: '#ffffff',
                        borderColor: '#007bff',
                        borderWidth: 1,
                        cornerRadius: 8
                    }
                },
                animation: {
                    animateRotate: true,
                    animateScale: true,
                    duration: 1000
                }
            }
        });
    }

    renderGenderChart() {
        const ctx = document.getElementById('genderChart');
        if (!ctx || !this.analyticsData.demographics) {
            console.log('Gender chart not rendered - ctx:', !!ctx, 'data:', !!this.analyticsData.demographics);
            return;
        }

        const data = this.analyticsData.demographics;
        console.log('Rendering gender chart with data:', data);
        
        this.charts.gender = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: Object.keys(data.genderDistribution),
                datasets: [{
                    data: Object.values(data.genderDistribution),
                    backgroundColor: [
                        '#007bff',  // Blue for Male
                        '#e91e63'   // Pink for Female
                    ],
                    borderWidth: 3,
                    borderColor: '#ffffff',
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 20,
                            usePointStyle: true,
                            font: {
                                size: 12,
                                weight: '500'
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#ffffff',
                        bodyColor: '#ffffff',
                        borderColor: '#007bff',
                        borderWidth: 1,
                        cornerRadius: 8,
                        displayColors: true,
                        callbacks: {
                            label: function(context) {
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((context.parsed / total) * 100).toFixed(1);
                                return `${context.label}: ${context.parsed} (${percentage}%)`;
                            }
                        }
                    }
                },
                cutout: '60%',
                animation: {
                    animateRotate: true,
                    animateScale: true,
                    duration: 1000
                }
            }
        });
    }

    renderBenefitsChart() {
        const ctx = document.getElementById('benefitsChart');
        if (!ctx || !this.analyticsData.benefits) return;

        const data = this.analyticsData.benefits;
        
        // Update KPI
        document.getElementById('totalBenefitsKPI').textContent = data.totalBenefits || 0;
        
        this.charts.benefits = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: Object.keys(data.benefitsByCategory),
                datasets: [{
                    label: 'Benefits by Category',
                    data: Object.values(data.benefitsByCategory),
                    backgroundColor: 'rgba(0, 123, 255, 0.8)',
                    borderColor: '#007bff',
                    borderWidth: 2,
                    borderRadius: 8,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        titleColor: '#ffffff',
                        bodyColor: '#ffffff',
                        borderColor: '#007bff',
                        borderWidth: 1,
                        cornerRadius: 8
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        },
                        ticks: {
                            stepSize: 1,
                            font: {
                                size: 12
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                size: 12
                            }
                        }
                    }
                },
                animation: {
                    duration: 1000,
                    easing: 'easeInOutQuart'
                }
            }
        });
    }

    renderHealthChart() {
        const ctx = document.getElementById('healthChart');
        if (!ctx || !this.analyticsData.health) return;

        const data = this.analyticsData.health;
        
        // Update KPI
        document.getElementById('emergencyAlertsKPI').textContent = data.totalEmergencies || 0;
        
        // Get top 5 health conditions
        const topConditions = Object.entries(data.healthConditions)
            .sort(([,a], [,b]) => b - a)
            .slice(0, 5)
            .reduce((obj, [key, value]) => ({ ...obj, [key]: value }), {});

        this.charts.health = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: Object.keys(topConditions),
                datasets: [{
                    label: 'Number of Seniors',
                    data: Object.values(topConditions),
                    backgroundColor: '#10B981',
                    borderColor: '#059669',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                plugins: {
                    legend: {
                        display: false
                    },
                    title: {
                        display: true,
                        text: 'Top Health Conditions',
                        font: {
                            size: 16,
                            weight: 'bold'
                        }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    renderActivityChart() {
        const ctx = document.getElementById('activityChart');
        if (!ctx || !this.analyticsData.activity) return;

        const data = this.analyticsData.activity;
        
        // Prepare data for last 7 days
        const last7Days = [];
        const activityData = [];
        const appointmentData = [];
        
        for (let i = 6; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            const dateKey = date.toISOString().split('T')[0];
            const dayName = date.toLocaleDateString('en-US', { weekday: 'short' });
            
            last7Days.push(dayName);
            activityData.push(data.activityTrends[dateKey] || 0);
            appointmentData.push(data.appointmentTrends[dateKey] || 0);
        }

        this.charts.activity = new Chart(ctx, {
            type: 'line',
            data: {
                labels: last7Days,
                datasets: [
                    {
                        label: 'Activities',
                        data: activityData,
                        borderColor: '#3B82F6',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4
                    },
                    {
                        label: 'Appointments',
                        data: appointmentData,
                        borderColor: '#10B981',
                        backgroundColor: 'rgba(16, 185, 129, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            usePointStyle: true
                        }
                    },
                    title: {
                        display: true,
                        text: 'Activity Trends (Last 7 Days)',
                        font: {
                            size: 16,
                            weight: 'bold'
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    resizeCharts() {
        Object.values(this.charts).forEach(chart => {
            if (chart && chart.resize) {
                chart.resize();
            }
        });
    }

    async generateReport(reportType) {
        try {
            this.showLoading('Generating report...');
            
            let reportData = {};
            
            switch (reportType) {
                case 'demographics':
                    reportData = await this.generateDemographicsReport();
                    break;
                case 'health':
                    reportData = await this.generateHealthReport();
                    break;
                case 'benefits':
                    reportData = await this.generateBenefitsReport();
                    break;
                case 'comprehensive':
                    reportData = await this.generateComprehensiveReport();
                    break;
                default:
                    throw new Error('Invalid report type');
            }

            this.downloadReport(reportData, reportType);
            this.showSuccess('Report generated successfully');
            
        } catch (error) {
            console.error('Error generating report:', error);
            this.showError('Failed to generate report');
        } finally {
            this.hideLoading();
        }
    }

    async generateDemographicsReport() {
        const data = this.analyticsData.demographics;
        return {
            title: 'Senior Citizens Demographics Report',
            generatedAt: new Date().toISOString(),
            data: {
                totalSeniors: data.totalSeniors,
                ageDistribution: data.ageGroups,
                genderDistribution: data.genderDistribution,
                locationDistribution: data.locationDistribution
            }
        };
    }

    async generateHealthReport() {
        const data = this.analyticsData.health;
        return {
            title: 'Senior Citizens Health Report',
            generatedAt: new Date().toISOString(),
            data: {
                healthConditions: data.healthConditions,
                medicationUsage: data.medicationUsage,
                emergencyTypes: data.emergencyTypes,
                totalEmergencies: data.totalEmergencies
            }
        };
    }

    async generateBenefitsReport() {
        const data = this.analyticsData.benefits;
        return {
            title: 'Benefits Distribution Report',
            generatedAt: new Date().toISOString(),
            data: {
                benefitsByCategory: data.benefitsByCategory,
                benefitsByStatus: data.benefitsByStatus,
                totalBenefits: data.totalBenefits,
                totalClaimed: data.totalClaimed
            }
        };
    }

    async generateComprehensiveReport() {
        return {
            title: 'Comprehensive Senior Citizens Report',
            generatedAt: new Date().toISOString(),
            demographics: this.analyticsData.demographics,
            health: this.analyticsData.health,
            benefits: this.analyticsData.benefits,
            activity: this.analyticsData.activity
        };
    }

    downloadReport(reportData, reportType) {
        const jsonString = JSON.stringify(reportData, null, 2);
        const blob = new Blob([jsonString], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${reportType}_report_${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        window.URL.revokeObjectURL(url);
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
}

// Initialize Analytics Manager
const analyticsManager = new AnalyticsManager();

// Export for use in other files
window.analyticsManager = analyticsManager;
