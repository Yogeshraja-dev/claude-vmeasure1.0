package com.vmeasure.app.util

import java.util.Calendar

object IdGenerator {

    /**
     * Generates an 18-digit ID: DDMMYYYYHHmmSSSSS + 2 random digits
     * Format: 2+2+4+2+2+3+1+2 = 18 chars (dd+mm+yyyy+hh+min+ms+sec-unit+random)
     * Effectively: ddmmyyyyhhmmssmsR where R adds collision avoidance
     */
    fun generate(): String {
        val cal = Calendar.getInstance()
        val dd  = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val mm  = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val yyyy = cal.get(Calendar.YEAR).toString()
        val hh  = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val ss  = cal.get(Calendar.SECOND).toString().padStart(2, '0')
        val ms  = cal.get(Calendar.MILLISECOND).toString().padStart(3, '0')
        val rr  = (10..99).random().toString()
        return "$dd$mm$yyyy$hh$min$ss$ms$rr"  // 2+2+4+2+2+2+3+2 = 19 → trim to 18
            .take(18)
    }
}