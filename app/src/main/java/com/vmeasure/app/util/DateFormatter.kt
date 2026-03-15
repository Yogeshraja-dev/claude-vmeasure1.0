package com.vmeasure.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {

    // Display format for dates: DD/MM/YYYY
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Display format for timestamps: DD/MM/YYYY HH:MM AM/PM
    private val timestampFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    // Parse format for date-only strings (dd/MM/yyyy)
    private val parseDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /** Returns current timestamp string: DD/MM/YYYY HH:MM AM/PM */
    fun nowTimestamp(): String = timestampFormat.format(Date())

    /** Returns current date string: DD/MM/YYYY */
    fun nowDate(): String = dateFormat.format(Date())

    /** Returns current time as millis */
    fun nowMillis(): Long = System.currentTimeMillis()

    /** Parses "DD/MM/YYYY" to millis; returns 0 if invalid */
    fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            parseDateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** Parses "DD/MM/YYYY HH:MM AM/PM" to millis; returns 0 if invalid */
    fun parseTimestampToMillis(ts: String): Long {
        if (ts.isBlank()) return 0L
        return try {
            timestampFormat.parse(ts)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** Returns end of the given day (23:59:59) in millis */
    fun endOfDayMillis(dayMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /** Returns end of today in millis */
    fun endOfTodayMillis(): Long = endOfDayMillis(System.currentTimeMillis())

    /** Formats millis to "DD/MM/YYYY" */
    fun millisToDate(millis: Long): String =
        if (millis == 0L) "" else dateFormat.format(Date(millis))

    /** Formats millis to "DD/MM/YYYY HH:MM AM/PM" */
    fun millisToTimestamp(millis: Long): String =
        if (millis == 0L) "" else timestampFormat.format(Date(millis))
}