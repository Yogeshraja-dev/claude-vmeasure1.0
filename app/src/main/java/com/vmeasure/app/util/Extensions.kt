package com.vmeasure.app.util

import android.app.DatePickerDialog
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vmeasure.app.data.db.entity.SectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.data.model.User
import java.util.Calendar

private val gson = Gson()

// ── View helpers ──────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

// ── Date Picker ───────────────────────────────────────────────────────────────

fun EditText.showDatePicker(context: Context, onDateSelected: (String, Long) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val formatted = "%02d/%02d/%04d".format(day, month + 1, year)
            val millis = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            setText(formatted)
            onDateSelected(formatted, millis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// ── JSON helpers ──────────────────────────────────────────────────────────────

fun Map<String, Int>.toJson(): String = gson.toJson(this)

fun String.toTagMap(): Map<String, Int> {
    if (isBlank() || this == "{}") return emptyMap()
    return try {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        gson.fromJson(this, type) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

// ── Entity ↔ Domain mappers ───────────────────────────────────────────────────

fun UserEntity.toDomain(sections: List<MeasurementSection> = emptyList()): User = User(
    userId = userId,
    name = name,
    dateOfBirth = dateOfBirth,
    specialDate = specialDate,
    isFavorite = isFavorite,
    isPinned = isPinned,
    contactNumber = contactNumber,
    instagramId = instagramId,
    otherMediaId = otherMediaId,
    location = location,
    createdAt = createdAt,
    updatedAt = updatedAt,
    selectedTags = selectedTags.toTagMap(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    specialDateMillis = specialDateMillis,
    dobMillis = dobMillis,
    sections = sections
)

fun SectionEntity.toDomain(): MeasurementSection = MeasurementSection(
    sectionId = sectionId, userId = userId, type = type,
    createdAt = createdAt, updatedAt = updatedAt,
    createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    sortOrder = sortOrder,
    uBust = uBust, bust = bust, waist = waist, hip = hip,
    armhole = armhole, shoulder = shoulder, length = length,
    fNeck = fNeck, bNeck = bNeck, sleeveLength = sleeveLength, sleeveRound = sleeveRound,
    blouseCut = blouseCut, blouseField = blouseField,
    thighRound = thighRound, kneeRound = kneeRound, bottom = bottom, inseam = inseam,
    frockLength = frockLength, yokeLength = yokeLength,
    blouseWaist = blouseWaist, blouseLength = blouseLength,
    skirtLength = skirtLength, waistLength = waistLength,
    chest = chest, pantLength = pantLength, pantWaist = pantWaist,
    notes = notes
)

fun MeasurementSection.toEntity(): SectionEntity = SectionEntity(
    sectionId = sectionId, userId = userId, type = type,
    createdAt = createdAt, updatedAt = updatedAt,
    createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    sortOrder = sortOrder,
    uBust = uBust, bust = bust, waist = waist, hip = hip,
    armhole = armhole, shoulder = shoulder, length = length,
    fNeck = fNeck, bNeck = bNeck, sleeveLength = sleeveLength, sleeveRound = sleeveRound,
    blouseCut = blouseCut, blouseField = blouseField,
    thighRound = thighRound, kneeRound = kneeRound, bottom = bottom, inseam = inseam,
    frockLength = frockLength, yokeLength = yokeLength,
    blouseWaist = blouseWaist, blouseLength = blouseLength,
    skirtLength = skirtLength, waistLength = waistLength,
    chest = chest, pantLength = pantLength, pantWaist = pantWaist,
    notes = notes
)