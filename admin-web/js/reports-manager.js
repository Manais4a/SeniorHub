// Reports Management
class ReportsManager {
    constructor() {
        this.recentReports = [];
        this.init();
    }

    init() {
        this.loadRecentReports();
    }

    async loadRecentReports() {
        try {
            // Load recent reports from localStorage or database
            const storedReports = localStorage.getItem('recentReports');
            this.recentReports = storedReports ? JSON.parse(storedReports) : [];
            this.updateRecentReportsList();
        } catch (error) {
            console.error('Error loading recent reports:', error);
        }
    }

    updateRecentReportsList() {
        const reportsList = document.getElementById('recentReportsList');
        if (!reportsList) return;

        if (this.recentReports.length === 0) {
            reportsList.innerHTML = '<p class="text-muted">No recent reports</p>';
            return;
        }

        reportsList.innerHTML = this.recentReports.map(report => `
            <div class="report-item">
                <div class="report-info">
                    <h4>${report.name}</h4>
                    <p>${report.description}</p>
                    <small class="text-muted">Generated: ${this.formatDate(report.generatedAt)}</small>
                </div>
                <div class="report-actions">
                    <button class="btn btn-sm btn-outline" onclick="reportsManager.downloadReport('${report.id}')">
                        <i class="fas fa-download"></i>
                    </button>
                    <button class="btn btn-sm btn-outline" onclick="reportsManager.viewReport('${report.id}')">
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </div>
        `).join('');
    }

    formatDate(date) {
        return new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    async generateSystemReport() {
        try {
            this.showLoading('Generating system report...');
            
            // Simulate report generation
            await new Promise(resolve => setTimeout(resolve, 2000));
            
            const report = {
                id: Date.now().toString(),
                name: 'System Report',
                description: 'Comprehensive system overview and analytics',
                type: 'system',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('System report generated successfully!');
        } catch (error) {
            console.error('Error generating system report:', error);
            this.showError('Failed to generate system report');
        } finally {
            this.hideLoading();
        }
    }

    async generateUserReport() {
        try {
            this.showLoading('Generating user analytics report...');
            
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            const report = {
                id: Date.now().toString(),
                name: 'User Analytics Report',
                description: 'Senior citizens demographics and activity analysis',
                type: 'users',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('User analytics report generated successfully!');
        } catch (error) {
            console.error('Error generating user report:', error);
            this.showError('Failed to generate user report');
        } finally {
            this.hideLoading();
        }
    }

    async generateHealthReport() {
        try {
            this.showLoading('Generating health report...');
            
            await new Promise(resolve => setTimeout(resolve, 1800));
            
            const report = {
                id: Date.now().toString(),
                name: 'Health Records Report',
                description: 'Health records and medical data analytics',
                type: 'health',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('Health report generated successfully!');
        } catch (error) {
            console.error('Error generating health report:', error);
            this.showError('Failed to generate health report');
        } finally {
            this.hideLoading();
        }
    }

    async generateEmergencyReport() {
        try {
            this.showLoading('Generating emergency report...');
            
            await new Promise(resolve => setTimeout(resolve, 1200));
            
            const report = {
                id: Date.now().toString(),
                name: 'Emergency Alerts Report',
                description: 'Emergency alerts and response analytics',
                type: 'emergency',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('Emergency report generated successfully!');
        } catch (error) {
            console.error('Error generating emergency report:', error);
            this.showError('Failed to generate emergency report');
        } finally {
            this.hideLoading();
        }
    }

    async generateBenefitsReport() {
        try {
            this.showLoading('Generating benefits report...');
            
            await new Promise(resolve => setTimeout(resolve, 1600));
            
            const report = {
                id: Date.now().toString(),
                name: 'Benefits Usage Report',
                description: 'Benefits usage and distribution analytics',
                type: 'benefits',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('Benefits report generated successfully!');
        } catch (error) {
            console.error('Error generating benefits report:', error);
            this.showError('Failed to generate benefits report');
        } finally {
            this.hideLoading();
        }
    }

    async generateSocialReport() {
        try {
            this.showLoading('Generating social services report...');
            
            await new Promise(resolve => setTimeout(resolve, 1400));
            
            const report = {
                id: Date.now().toString(),
                name: 'Social Services Report',
                description: 'Social services utilization and impact analysis',
                type: 'social',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('Social services report generated successfully!');
        } catch (error) {
            console.error('Error generating social report:', error);
            this.showError('Failed to generate social report');
        } finally {
            this.hideLoading();
        }
    }

    async generatePerformanceReport() {
        try {
            this.showLoading('Generating performance report...');
            
            await new Promise(resolve => setTimeout(resolve, 2000));
            
            const report = {
                id: Date.now().toString(),
                name: 'System Performance Report',
                description: 'System usage, performance, and maintenance analysis',
                type: 'performance',
                generatedAt: new Date().toISOString(),
                status: 'completed'
            };
            
            this.addToRecentReports(report);
            this.showSuccess('Performance report generated successfully!');
        } catch (error) {
            console.error('Error generating performance report:', error);
            this.showError('Failed to generate performance report');
        } finally {
            this.hideLoading();
        }
    }

    async exportAllData() {
        try {
            this.showLoading('Exporting all data...');
            
            // Simulate data export
            await new Promise(resolve => setTimeout(resolve, 3000));
            
            const exportData = {
                timestamp: new Date().toISOString(),
                data: {
                    users: 'User data exported',
                    health: 'Health data exported',
                    benefits: 'Benefits data exported',
                    emergency: 'Emergency data exported',
                    social: 'Social services data exported'
                }
            };
            
            // Create and download file
            const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `seniorhub-export-${new Date().toISOString().split('T')[0]}.json`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            
            this.showSuccess('All data exported successfully!');
        } catch (error) {
            console.error('Error exporting data:', error);
            this.showError('Failed to export data');
        } finally {
            this.hideLoading();
        }
    }

    addToRecentReports(report) {
        this.recentReports.unshift(report);
        // Keep only last 10 reports
        if (this.recentReports.length > 10) {
            this.recentReports = this.recentReports.slice(0, 10);
        }
        
        // Save to localStorage
        localStorage.setItem('recentReports', JSON.stringify(this.recentReports));
        this.updateRecentReportsList();
    }

    downloadReport(reportId) {
        const report = this.recentReports.find(r => r.id === reportId);
        if (report) {
            console.log('Downloading report:', report.name);
            alert(`Downloading report: ${report.name}`);
        }
    }

    viewReport(reportId) {
        const report = this.recentReports.find(r => r.id === reportId);
        if (report) {
            console.log('Viewing report:', report.name);
            alert(`Viewing report: ${report.name}`);
        }
    }

    showLoading(message = 'Loading...') {
        console.log(message);
        // You can implement a loading overlay here
    }

    hideLoading() {
        // Hide loading overlay
    }

    showSuccess(message) {
        console.log(message);
        // You can implement a success notification here
        alert(message);
    }

    showError(message) {
        console.error(message);
        // You can implement an error notification here
        alert(message);
    }
}

// Initialize Reports Manager
const reportsManager = new ReportsManager();
