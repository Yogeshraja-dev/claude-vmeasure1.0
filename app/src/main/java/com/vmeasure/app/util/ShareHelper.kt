package com.vmeasure.app.util

import android.content.Context
import android.content.Intent
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.data.model.User

object ShareHelper {

    fun buildUserSummary(user: User): String {
        val sb = StringBuilder()
        sb.appendLine("=== User Details ===")
        sb.appendLine("Name: ${user.name}")
        if (user.dateOfBirth.isNotBlank()) sb.appendLine("Date of Birth: ${user.dateOfBirth}")
        if (user.specialDate.isNotBlank()) sb.appendLine("Special Date: ${user.specialDate}")
        if (user.contactNumber.isNotBlank()) sb.appendLine("Contact: ${user.contactNumber}")
        if (user.instagramId.isNotBlank()) sb.appendLine("Instagram: ${user.instagramId}")
        if (user.otherMediaId.isNotBlank()) sb.appendLine("Other Media: ${user.otherMediaId}")
        if (user.location.isNotBlank()) sb.appendLine("Location: ${user.location}")

        if (user.sections.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== Measurements ===")
            val grouped = user.sections.groupBy { it.type }
            grouped.forEach { (type, sections) ->
                sb.appendLine("-- $type --")
                sections.forEachIndexed { idx, section ->
                    if (sections.size > 1) sb.appendLine("  Section ${idx + 1}:")
                    sb.append(buildSectionText(section))
                }
            }
        }
        return sb.toString().trim()
    }

    fun buildBulkSummary(users: List<User>): String {
        return users.joinToString("\n\n${"-".repeat(40)}\n\n") { buildUserSummary(it) }
    }

    private fun buildSectionText(s: MeasurementSection): String {
        val sb = StringBuilder()
        fun appendIfNotEmpty(label: String, value: String) {
            if (value.isNotBlank()) sb.appendLine("  $label: $value")
        }
        appendIfNotEmpty("U.Bust", s.uBust)
        appendIfNotEmpty("Bust", s.bust)
        appendIfNotEmpty("Waist", s.waist)
        appendIfNotEmpty("Hip", s.hip)
        appendIfNotEmpty("Armhole", s.armhole)
        appendIfNotEmpty("Shoulder", s.shoulder)
        appendIfNotEmpty("Length", s.length)
        appendIfNotEmpty("F Neck", s.fNeck)
        appendIfNotEmpty("B Neck", s.bNeck)
        appendIfNotEmpty("Sleeve Length", s.sleeveLength)
        appendIfNotEmpty("Sleeve Round", s.sleeveRound)
        appendIfNotEmpty("Blouse Cut", s.blouseCut)
        appendIfNotEmpty("Blouse", s.blouseField)
        appendIfNotEmpty("Thigh Round", s.thighRound)
        appendIfNotEmpty("Knee Round", s.kneeRound)
        appendIfNotEmpty("Bottom", s.bottom)
        appendIfNotEmpty("Inseam", s.inseam)
        appendIfNotEmpty("Frock Length", s.frockLength)
        appendIfNotEmpty("Yoke Length", s.yokeLength)
        appendIfNotEmpty("Blouse Waist", s.blouseWaist)
        appendIfNotEmpty("Blouse Length", s.blouseLength)
        appendIfNotEmpty("Skirt Length", s.skirtLength)
        appendIfNotEmpty("Waist Length", s.waistLength)
        appendIfNotEmpty("Chest", s.chest)
        appendIfNotEmpty("Pant Length", s.pantLength)
        appendIfNotEmpty("Pant Waist", s.pantWaist)
        appendIfNotEmpty("Notes", s.notes)
        return sb.toString()
    }

    fun shareText(context: Context, text: String, title: String = "Share via") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}