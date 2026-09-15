package com.earthnow.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormat {
    fun ago(tsMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - tsMillis
        if (diff < 0) return "just now"
        val min = diff / 60_000
        return when {
            min < 1 -> "just now"
            min < 60 -> "$min min ago"
            min < 1440 -> "${min / 60} h ${min % 60} min ago"
            else -> "${min / 1440} d ago"
        }
    }

    fun hhmm(tsMillis: Long, tz: TimeZone = TimeZone.getDefault()): String {
        val f = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = tz }
        return f.format(Date(tsMillis))
    }

    fun hhmmZ(iso: String?): String? {
        if (iso == null) return null
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            hhmm(f.parse(iso)?.time ?: return null, TimeZone.getDefault())
        } catch (e: Exception) { null }
    }

    fun isoToMillis(iso: String): Long? = try {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        f.parse(iso)?.time
    } catch (e: Exception) { null }

    fun dayLabel(iso: String): String = try {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = f.parse(iso) ?: return iso
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(d)
    } catch (e: Exception) { iso }
}