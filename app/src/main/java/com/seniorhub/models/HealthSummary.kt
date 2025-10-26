package com.seniorhub.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * HealthSummary - Represents a summary of user's current health status
 *
 * This model provides a consolidated view of the user's latest health metrics
 * and overall health status for quick reference on dashboards and reports.
 */
@Parcelize
data class HealthSummary(
    var userId: String = "",

    // Latest vital signs
    var bloodPressure: String = "N/A", // "120/80"
    var heartRate: String = "N/A", // "72 bpm"
    var bloodSugar: String = "N/A", // "95 mg/dL"
    var weight: String = "N/A", // "150 lbs"
    var temperature: String = "N/A", // "98.6°F"

    // Health status indicators
    var overallStatus: String = "Unknown", // "Good", "Fair", "Poor", "Critical"
    var riskLevel: String = "Low", // "Low", "Moderate", "High", "Critical"
    var alertsCount: Int = 0,
    var criticalAlertsCount: Int = 0,

    // Activity and compliance
    var medicationCompliance: Double = 0.0, // Percentage
    var appointmentsUpcoming: Int = 0,
    var appointmentsOverdue: Int = 0,
    var lastCheckupDays: Int = -1, // Days since last checkup

    // Trends (compared to previous period)
    var bloodPressureTrend: String = "stable", // "improving", "stable", "worsening"
    var heartRateTrend: String = "stable",
    var weightTrend: String = "stable",
    var bloodSugarTrend: String = "stable",

    // Recent activity
    var lastRecordDate: Date? = null,
    var recordsThisWeek: Int = 0,
    var recordsThisMonth: Int = 0,

    // System fields
    var lastUpdated: Date? = null,
    var createdAt: Date? = null,
    var isValid: Boolean = true
) : Parcelable {

    companion object {
        // Health status constants
        const val STATUS_EXCELLENT = "excellent"
        const val STATUS_GOOD = "good"
        const val STATUS_FAIR = "fair"
        const val STATUS_MODERATE = "moderate"
        const val STATUS_POOR = "poor"
        const val STATUS_CONCERNING = "concerning"
        const val STATUS_CRITICAL = "critical"
        const val STATUS_EMERGENCY = "emergency"
        const val STATUS_UNKNOWN = "unknown"

        // Risk levels
        const val RISK_LOW = "low"
        const val RISK_MODERATE = "moderate"
        const val RISK_HIGH = "high"
        const val RISK_CRITICAL = "critical"

        // Trend indicators
        const val TREND_IMPROVING = "improving"
        const val TREND_STABLE = "stable"
        const val TREND_WORSENING = "worsening"

        // Compliance thresholds
        private const val EXCELLENT_COMPLIANCE = 95.0
        private const val GOOD_COMPLIANCE = 85.0
        private const val FAIR_COMPLIANCE = 70.0
        private const val POOR_COMPLIANCE = 50.0

        // Activity levels
        private const val VERY_ACTIVE_RECORDS = 7
        private const val ACTIVE_RECORDS = 4
        private const val MODERATE_RECORDS = 2
        private const val LIGHT_RECORDS = 1

        // Time constants
        private const val DAYS_IN_WEEK = 7
        private const val DAYS_IN_MONTH = 30
        private const val DAYS_IN_YEAR = 365
        private const val CHECKUP_OVERDUE_DAYS = 90
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000
    }

    /**
     * Get overall health status color code
     * @return Color identifier for UI display
     */
    fun getStatusColor(): String {
        return when (overallStatus.lowercase()) {
            STATUS_EXCELLENT, STATUS_GOOD -> "green"
            STATUS_FAIR, STATUS_MODERATE -> "yellow"
            STATUS_POOR, STATUS_CONCERNING -> "orange"
            STATUS_CRITICAL, STATUS_EMERGENCY -> "red"
            else -> "gray"
        }
    }

    /**
     * Get risk level color code
     * @return Color identifier for risk level
     */
    fun getRiskColor(): String {
        return when (riskLevel.lowercase()) {
            RISK_LOW -> "green"
            RISK_MODERATE -> "yellow"
            RISK_HIGH -> "orange"
            RISK_CRITICAL -> "red"
            else -> "gray"
        }
    }

    /**
     * Check if user needs immediate medical attention
     * @return True if critical alerts exist
     */
    fun needsImmediateAttention(): Boolean {
        return criticalAlertsCount > 0 || riskLevel.lowercase() == RISK_CRITICAL
    }

    /**
     * Get medication compliance status
     * @return Human-readable compliance status
     */
    fun getComplianceStatus(): String {
        return when {
            medicationCompliance >= EXCELLENT_COMPLIANCE -> "Excellent"
            medicationCompliance >= GOOD_COMPLIANCE -> "Good"
            medicationCompliance >= FAIR_COMPLIANCE -> "Fair"
            medicationCompliance >= POOR_COMPLIANCE -> "Poor"
            else -> "Critical"
        }
    }

    /**
     * Get compliance color for UI display
     * @return Color identifier based on compliance level
     */
    fun getComplianceColor(): String {
        return when {
            medicationCompliance >= EXCELLENT_COMPLIANCE -> "green"
            medicationCompliance >= GOOD_COMPLIANCE -> "lightgreen"
            medicationCompliance >= FAIR_COMPLIANCE -> "yellow"
            medicationCompliance >= POOR_COMPLIANCE -> "orange"
            else -> "red"
        }
    }

    /**
     * Get summary of recent activity
     * @return Description of recent health tracking activity
     */
    fun getActivitySummary(): String {
        return when {
            recordsThisWeek >= VERY_ACTIVE_RECORDS -> "Very Active"
            recordsThisWeek >= ACTIVE_RECORDS -> "Active"
            recordsThisWeek >= MODERATE_RECORDS -> "Moderate"
            recordsThisWeek >= LIGHT_RECORDS -> "Light"
            else -> "Inactive"
        }
    }

    /**
     * Get activity color for UI display
     * @return Color identifier based on activity level
     */
    fun getActivityColor(): String {
        return when {
            recordsThisWeek >= VERY_ACTIVE_RECORDS -> "green"
            recordsThisWeek >= ACTIVE_RECORDS -> "lightgreen"
            recordsThisWeek >= MODERATE_RECORDS -> "yellow"
            recordsThisWeek >= LIGHT_RECORDS -> "orange"
            else -> "red"
        }
    }

    /**
     * Get days since last health record
     * @return Number of days since last record or -1 if no records
     */
    fun getDaysSinceLastRecord(): Int {
        return lastRecordDate?.let { lastRecord ->
            val now = Calendar.getInstance()
            val recordDate = Calendar.getInstance().apply { time = lastRecord }
            val diffMillis = now.timeInMillis - recordDate.timeInMillis
            (diffMillis / MILLIS_PER_DAY).toInt()
        } ?: -1
    }

    /**
     * Get formatted last checkup information
     * @return Human-readable last checkup status
     */
    fun getLastCheckupStatus(): String {
        return when {
            lastCheckupDays < 0 -> "No checkup recorded"
            lastCheckupDays == 0 -> "Today"
            lastCheckupDays == 1 -> "Yesterday"
            lastCheckupDays <= DAYS_IN_WEEK -> "$lastCheckupDays days ago"
            lastCheckupDays <= DAYS_IN_MONTH -> "${lastCheckupDays / DAYS_IN_WEEK} weeks ago"
            lastCheckupDays <= DAYS_IN_YEAR -> "${lastCheckupDays / DAYS_IN_MONTH} months ago"
            else -> "Over a year ago"
        }
    }

    /**
     * Check if checkup is overdue
     * @return True if checkup is needed
     */
    fun isCheckupOverdue(): Boolean {
        return lastCheckupDays > CHECKUP_OVERDUE_DAYS
    }

    /**
     * Get checkup status color
     * @return Color identifier based on checkup timing
     */
    fun getCheckupColor(): String {
        return when {
            lastCheckupDays < 0 -> "gray"
            lastCheckupDays <= 30 -> "green"
            lastCheckupDays <= 60 -> "yellow"
            lastCheckupDays <= CHECKUP_OVERDUE_DAYS -> "orange"
            else -> "red"
        }
    }

    /**
     * Get list of health concerns based on current data
     * @return List of health concerns that need attention
     */
    fun getHealthConcerns(): List<String> {
        val concerns = mutableListOf<String>()

        if (criticalAlertsCount > 0) {
            concerns.add("$criticalAlertsCount critical health alert${if (criticalAlertsCount > 1) "s" else ""}")
        }

        if (medicationCompliance < FAIR_COMPLIANCE) {
            concerns.add("Low medication compliance (${medicationCompliance.toInt()}%)")
        }

        if (appointmentsOverdue > 0) {
            concerns.add("$appointmentsOverdue overdue appointment${if (appointmentsOverdue > 1) "s" else ""}")
        }

        if (isCheckupOverdue()) {
            concerns.add("Regular checkup overdue")
        }

        if (getDaysSinceLastRecord() > DAYS_IN_WEEK) {
            concerns.add("No recent health records")
        }

        return concerns
    }

    /**
     * Get health summary statistics
     * @return Map of key health statistics
     */
    fun getHealthStats(): Map<String, String> {
        return mapOf(
            "Overall Status" to overallStatus,
            "Risk Level" to riskLevel,
            "Medication Compliance" to "${medicationCompliance.toInt()}%",
            "Activity Level" to getActivitySummary(),
            "Last Checkup" to getLastCheckupStatus(),
            "Critical Alerts" to criticalAlertsCount.toString(),
            "Upcoming Appointments" to appointmentsUpcoming.toString(),
            "Records This Week" to recordsThisWeek.toString(),
            "Records This Month" to recordsThisMonth.toString()
        )
    }

    /**
     * Get overall health score (0-100)
     * @return Calculated health score
     */
    fun getHealthScore(): Int {
        var score = 50 // Base score

        // Adjust based on compliance
        score += ((medicationCompliance - 50) / 2).toInt()

        // Adjust based on activity
        score += when {
            recordsThisWeek >= VERY_ACTIVE_RECORDS -> 15
            recordsThisWeek >= ACTIVE_RECORDS -> 10
            recordsThisWeek >= MODERATE_RECORDS -> 5
            recordsThisWeek >= LIGHT_RECORDS -> 0
            else -> -10
        }

        // Adjust based on alerts
        score -= (criticalAlertsCount * 10)
        score -= (alertsCount * 2)

        // Adjust based on overdue items
        score -= (appointmentsOverdue * 5)
        if (isCheckupOverdue()) score -= 10

        return score.coerceIn(0, 100)
    }

    /**
     * Get health score color
     * @return Color identifier based on health score
     */
    fun getHealthScoreColor(): String {
        val score = getHealthScore()
        return when {
            score >= 80 -> "green"
            score >= 60 -> "lightgreen"
            score >= 40 -> "yellow"
            score >= 20 -> "orange"
            else -> "red"
        }
    }

    /**
     * Convert to map for database storage
     * @return Map representation of health summary
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "bloodPressure" to bloodPressure,
            "heartRate" to heartRate,
            "bloodSugar" to bloodSugar,
            "weight" to weight,
            "temperature" to temperature,
            "overallStatus" to overallStatus,
            "riskLevel" to riskLevel,
            "alertsCount" to alertsCount,
            "criticalAlertsCount" to criticalAlertsCount,
            "medicationCompliance" to medicationCompliance,
            "appointmentsUpcoming" to appointmentsUpcoming,
            "appointmentsOverdue" to appointmentsOverdue,
            "lastCheckupDays" to lastCheckupDays,
            "bloodPressureTrend" to bloodPressureTrend,
            "heartRateTrend" to heartRateTrend,
            "weightTrend" to weightTrend,
            "bloodSugarTrend" to bloodSugarTrend,
            "lastRecordDate" to lastRecordDate,
            "recordsThisWeek" to recordsThisWeek,
            "recordsThisMonth" to recordsThisMonth,
            "isValid" to isValid,
            "createdAt" to createdAt,
            "lastUpdated" to Date()
        )
    }
}