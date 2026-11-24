package com.multmaster.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun startOfDay(ts: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfDay(ts: Long = System.currentTimeMillis()): Long = startOfDay(ts) + 24L * 60L * 60L * 1000L

    fun dayIndex(ts: Long = System.currentTimeMillis()): Long = startOfDay(ts) / (24L * 60L * 60L * 1000L)

    fun todayIso(): String = iso.format(Date(System.currentTimeMillis()))

    fun isoDate(daysAgo: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return iso.format(c.time)
    }

    fun formatMinutes(ms: Long): String {
        val totalMinutes = ms / 60000
        return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"
    }

    fun formatMs(ms: Long): String = if (ms <= 0) "—" else String.format(Locale.US, "%.1fs", ms / 1000.0)

    fun formatDate(ts: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(ts))
}