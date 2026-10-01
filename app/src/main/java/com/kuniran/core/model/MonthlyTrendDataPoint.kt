package com.kuniran.core.model

/**
 * Recharts-compatible data structure representing monthly finance trend points.
 * Compatible with chart libraries and JSON-based visualization engines:
 * { "monthKey": "2026-08", "monthLabel": "Agu", "income": 1500000, "expense": 950000, "net": 550000 }
 */
data class MonthlyTrendDataPoint(
    val monthKey: String,
    val monthLabel: String,
    val income: Long,
    val expense: Long,
    val net: Long = income - expense
)
