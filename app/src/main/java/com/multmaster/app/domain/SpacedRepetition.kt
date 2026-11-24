package com.multmaster.app.domain

/** Interval ladder in days. Index 0 means "due again in the next session". */
object SpacedRepetition {
    val intervalsDays = intArrayOf(0, 1, 3, 7, 14, 30, 60)
    const val DAY_MS = 24L * 60L * 60L * 1000L

    fun dueAfter(intervalIndex: Int, now: Long): Long {
        val days = intervalsDays[intervalIndex.coerceIn(0, intervalsDays.lastIndex)]
        return if (days == 0) now else now + days * DAY_MS
    }

    fun label(intervalIndex: Int): String = when (val d = intervalsDays[intervalIndex.coerceIn(0, intervalsDays.lastIndex)]) {
        0 -> "next session"
        else -> "in $d day" + (if (d == 1) "" else "s")
    }
}