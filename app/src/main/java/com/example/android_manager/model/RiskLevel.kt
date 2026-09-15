package com.example.android_manager.model

/**
 * Standard APVM Risk Level classifications.
 */
enum class RiskLevel(
    val title: String,
    val minScore: Int,
    val maxScore: Int,
    val severityRank: Int
) {
    LOW(
        title = "LOW",
        minScore = 0,
        maxScore = 29,
        severityRank = 1
    ),
    MEDIUM(
        title = "MEDIUM",
        minScore = 30,
        maxScore = 59,
        severityRank = 2
    ),
    HIGH(
        title = "HIGH",
        minScore = 60,
        maxScore = 79,
        severityRank = 3
    ),
    CRITICAL(
        title = "CRITICAL",
        minScore = 80,
        maxScore = 100,
        severityRank = 4
    );

    fun isHighOrCritical(): Boolean = this == HIGH || this == CRITICAL

    companion object {
        /**
         * Resolves the RiskLevel from an integer score between 0 and 100.
         */
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped >= CRITICAL.minScore -> CRITICAL
                clamped >= HIGH.minScore -> HIGH
                clamped >= MEDIUM.minScore -> MEDIUM
                else -> LOW
            }
        }

        /**
         * Resolves the max severity among a collection of findings or levels.
         */
        fun max(vararg levels: RiskLevel): RiskLevel {
            return levels.maxByOrNull { it.severityRank } ?: LOW
        }
    }
}