package com.vmeasure.app.util

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.vmeasure.app.R
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.databinding.ItemMeasurementSectionBinding

/**
 * Shared helper that inflates and binds a measurement section card.
 * Used by both AddEditFragment and DetailFragment to avoid duplication.
 */
object SectionHelper {

    interface SectionCallback {
        fun onFieldChanged(sectionId: String, updater: (MeasurementSection) -> MeasurementSection)
        fun onClearAll(sectionId: String)
        fun onDuplicate(sectionId: String)
        fun onDelete(sectionId: String)
    }

    // ── Inflate and configure a section card ──────────────────────────────────

    fun createSectionView(
        context: Context,
        section: MeasurementSection,
        isEditable: Boolean,
        showTimestamps: Boolean,
        callback: SectionCallback
    ): View {
        val binding = ItemMeasurementSectionBinding.inflate(
            LayoutInflater.from(context), null, false
        )

        // Header
        binding.tvSectionType.text = section.type

        if (showTimestamps) {
            val tsText = buildTimestampText(section)
            if (tsText.isNotBlank()) {
                binding.tvTimestamps.text = tsText
                binding.tvTimestamps.visibility = View.VISIBLE
            }
        }

        // 3-dot section menu
        binding.btnSectionMenu.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            popup.menu.apply {
                add(0, 1, 0, context.getString(R.string.section_menu_clear))
                add(0, 2, 1, context.getString(R.string.section_menu_duplicate))
                val deleteItem = add(0, 3, 2, context.getString(R.string.section_menu_delete))
                deleteItem.setTitleColor(context.getColor(R.color.colorError))
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { callback.onClearAll(section.sectionId); true }
                    2 -> { callback.onDuplicate(section.sectionId); true }
                    3 -> { callback.onDelete(section.sectionId); true }
                    else -> false
                }
            }
            popup.show()
        }

        // Apply field visibility based on type
        applyFieldVisibility(binding, section.type)

        // Set field values (remove then re-add watchers to prevent loops)
        bindFieldValues(binding, section)

        // Set editable state
        setEditableState(binding, isEditable)

        // Set up text watchers only in editable mode
        if (isEditable) {
            setupTextWatchers(binding, section, callback)
        }

        return binding.root
    }

    // ── Field visibility per type ─────────────────────────────────────────────

    private fun applyFieldVisibility(b: ItemMeasurementSectionBinding, type: String) {
        // Hide everything first
        b.rowUBustBust.visibility = View.GONE
        b.rowWaistHip.visibility = View.GONE
        b.rowArmholeShoulder.visibility = View.GONE
        b.rowLengthFNeck.visibility = View.GONE
        b.rowBNeckSleeveLength.visibility = View.GONE
        b.rowSleeveRoundBlouseField.visibility = View.GONE
        b.rowBlouseCut.visibility = View.GONE
        b.rowThighKnee.visibility = View.GONE
        b.rowBottomInseam.visibility = View.GONE
        b.rowFrockYoke.visibility = View.GONE
        b.rowBlouseWaistLength.visibility = View.GONE
        b.rowSkirtWaistLength.visibility = View.GONE
        b.rowChestPantLength.visibility = View.GONE
        b.rowPantWaist.visibility = View.GONE
        b.containerHip.visibility = View.GONE
        b.containerArmhole.visibility = View.GONE
        b.containerBNeck.visibility = View.GONE
        b.containerBlouseField.visibility = View.GONE
        b.containerLength.visibility = View.GONE
        b.containerFNeck.visibility = View.GONE

        when (type) {
            "Blouse" -> {
                b.rowUBustBust.visibility = View.VISIBLE
                b.rowWaistHip.visibility = View.VISIBLE
                b.containerHip.visibility = View.VISIBLE
                b.rowArmholeShoulder.visibility = View.VISIBLE
                b.containerArmhole.visibility = View.VISIBLE
                b.rowLengthFNeck.visibility = View.VISIBLE
                b.containerLength.visibility = View.VISIBLE
                b.containerFNeck.visibility = View.VISIBLE
                b.rowBNeckSleeveLength.visibility = View.VISIBLE
                b.containerBNeck.visibility = View.VISIBLE
                b.rowSleeveRoundBlouseField.visibility = View.VISIBLE
            }
            "Kurti" -> {
                b.rowBlouseCut.visibility = View.VISIBLE
                b.rowUBustBust.visibility = View.VISIBLE
                b.rowWaistHip.visibility = View.VISIBLE
                // hip gone for kurti
                b.rowArmholeShoulder.visibility = View.VISIBLE
                b.containerArmhole.visibility = View.VISIBLE
                b.rowLengthFNeck.visibility = View.VISIBLE
                // length gone for kurti
                b.containerFNeck.visibility = View.VISIBLE
                b.rowBNeckSleeveLength.visibility = View.VISIBLE
                b.containerBNeck.visibility = View.VISIBLE
                b.rowSleeveRoundBlouseField.visibility = View.VISIBLE
                b.containerBlouseField.visibility = View.VISIBLE
            }
            "Pant" -> {
                b.rowWaistHip.visibility = View.VISIBLE
                b.containerHip.visibility = View.VISIBLE
                b.rowLengthFNeck.visibility = View.VISIBLE
                b.containerLength.visibility = View.VISIBLE
                // fNeck gone for pant
                b.rowThighKnee.visibility = View.VISIBLE
                b.rowBottomInseam.visibility = View.VISIBLE
            }
            "Frock" -> {
                b.rowWaistHip.visibility = View.VISIBLE
                // hip gone for frock
                b.rowFrockYoke.visibility = View.VISIBLE
            }
            "Crop Blouse and Skirt" -> {
                b.rowBlouseWaistLength.visibility = View.VISIBLE
                b.rowSkirtWaistLength.visibility = View.VISIBLE
            }
            "Kids Boy" -> {
                b.rowWaistHip.visibility = View.VISIBLE
                // hip gone for kids
                b.rowArmholeShoulder.visibility = View.VISIBLE
                // armhole gone for kids
                b.rowLengthFNeck.visibility = View.VISIBLE
                b.containerLength.visibility = View.VISIBLE
                // fNeck gone for kids
                b.rowBNeckSleeveLength.visibility = View.VISIBLE
                // bNeck gone for kids
                b.rowChestPantLength.visibility = View.VISIBLE
                b.rowPantWaist.visibility = View.VISIBLE
            }
        }
    }

    // ── Set field values ──────────────────────────────────────────────────────

    private fun bindFieldValues(b: ItemMeasurementSectionBinding, s: MeasurementSection) {
        b.etUBust.setTextSilently(s.uBust)
        b.etBust.setTextSilently(s.bust)
        b.etWaist.setTextSilently(s.waist)
        b.etHip.setTextSilently(s.hip)
        b.etArmhole.setTextSilently(s.armhole)
        b.etShoulder.setTextSilently(s.shoulder)
        b.etLength.setTextSilently(s.length)
        b.etFNeck.setTextSilently(s.fNeck)
        b.etBNeck.setTextSilently(s.bNeck)
        b.etSleeveLength.setTextSilently(s.sleeveLength)
        b.etSleeveRound.setTextSilently(s.sleeveRound)
        b.etBlouseCut.setTextSilently(s.blouseCut)
        b.etBlouseField.setTextSilently(s.blouseField)
        b.etThighRound.setTextSilently(s.thighRound)
        b.etKneeRound.setTextSilently(s.kneeRound)
        b.etBottom.setTextSilently(s.bottom)
        b.etInseam.setTextSilently(s.inseam)
        b.etFrockLength.setTextSilently(s.frockLength)
        b.etYokeLength.setTextSilently(s.yokeLength)
        b.etBlouseWaist.setTextSilently(s.blouseWaist)
        b.etBlouseLength.setTextSilently(s.blouseLength)
        b.etSkirtLength.setTextSilently(s.skirtLength)
        b.etWaistLength.setTextSilently(s.waistLength)
        b.etChest.setTextSilently(s.chest)
        b.etPantLength.setTextSilently(s.pantLength)
        b.etPantWaist.setTextSilently(s.pantWaist)
        b.etNotes.setTextSilently(s.notes)
    }

    private fun EditText.setTextSilently(value: String) {
        val watcher = tag as? TextWatcher
        watcher?.let { removeTextChangedListener(it) }
        setText(value)
        watcher?.let { addTextChangedListener(it) }
    }

    // ── Set editable state ────────────────────────────────────────────────────

    private fun setEditableState(b: ItemMeasurementSectionBinding, enabled: Boolean) {
        val fields = listOf(
            b.etUBust, b.etBust, b.etWaist, b.etHip, b.etArmhole, b.etShoulder,
            b.etLength, b.etFNeck, b.etBNeck, b.etSleeveLength, b.etSleeveRound,
            b.etBlouseCut, b.etBlouseField, b.etThighRound, b.etKneeRound,
            b.etBottom, b.etInseam, b.etFrockLength, b.etYokeLength,
            b.etBlouseWaist, b.etBlouseLength, b.etSkirtLength, b.etWaistLength,
            b.etChest, b.etPantLength, b.etPantWaist, b.etNotes
        )
        fields.forEach { it.isEnabled = enabled }
        b.btnSectionMenu.isEnabled = enabled
        b.btnSectionMenu.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    // ── Text watchers ─────────────────────────────────────────────────────────

    private fun setupTextWatchers(
        b: ItemMeasurementSectionBinding,
        section: MeasurementSection,
        callback: SectionCallback
    ) {
        b.etUBust.watchField(section.sectionId, callback) { s, v -> s.copy(uBust = v) }
        b.etBust.watchField(section.sectionId, callback) { s, v -> s.copy(bust = v) }
        b.etWaist.watchField(section.sectionId, callback) { s, v -> s.copy(waist = v) }
        b.etHip.watchField(section.sectionId, callback) { s, v -> s.copy(hip = v) }
        b.etArmhole.watchField(section.sectionId, callback) { s, v -> s.copy(armhole = v) }
        b.etShoulder.watchField(section.sectionId, callback) { s, v -> s.copy(shoulder = v) }
        b.etLength.watchField(section.sectionId, callback) { s, v -> s.copy(length = v) }
        b.etFNeck.watchField(section.sectionId, callback) { s, v -> s.copy(fNeck = v) }
        b.etBNeck.watchField(section.sectionId, callback) { s, v -> s.copy(bNeck = v) }
        b.etSleeveLength.watchField(section.sectionId, callback) { s, v -> s.copy(sleeveLength = v) }
        b.etSleeveRound.watchField(section.sectionId, callback) { s, v -> s.copy(sleeveRound = v) }
        b.etBlouseCut.watchField(section.sectionId, callback) { s, v -> s.copy(blouseCut = v) }
        b.etBlouseField.watchField(section.sectionId, callback) { s, v -> s.copy(blouseField = v) }
        b.etThighRound.watchField(section.sectionId, callback) { s, v -> s.copy(thighRound = v) }
        b.etKneeRound.watchField(section.sectionId, callback) { s, v -> s.copy(kneeRound = v) }
        b.etBottom.watchField(section.sectionId, callback) { s, v -> s.copy(bottom = v) }
        b.etInseam.watchField(section.sectionId, callback) { s, v -> s.copy(inseam = v) }
        b.etFrockLength.watchField(section.sectionId, callback) { s, v -> s.copy(frockLength = v) }
        b.etYokeLength.watchField(section.sectionId, callback) { s, v -> s.copy(yokeLength = v) }
        b.etBlouseWaist.watchField(section.sectionId, callback) { s, v -> s.copy(blouseWaist = v) }
        b.etBlouseLength.watchField(section.sectionId, callback) { s, v -> s.copy(blouseLength = v) }
        b.etSkirtLength.watchField(section.sectionId, callback) { s, v -> s.copy(skirtLength = v) }
        b.etWaistLength.watchField(section.sectionId, callback) { s, v -> s.copy(waistLength = v) }
        b.etChest.watchField(section.sectionId, callback) { s, v -> s.copy(chest = v) }
        b.etPantLength.watchField(section.sectionId, callback) { s, v -> s.copy(pantLength = v) }
        b.etPantWaist.watchField(section.sectionId, callback) { s, v -> s.copy(pantWaist = v) }
        b.etNotes.watchField(section.sectionId, callback) { s, v -> s.copy(notes = v) }
    }

    private fun EditText.watchField(
        sectionId: String,
        callback: SectionCallback,
        updater: (MeasurementSection, String) -> MeasurementSection
    ) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                callback.onFieldChanged(sectionId) { section -> updater(section, s.toString()) }
            }
        }
        tag = watcher
        addTextChangedListener(watcher)
    }

    private fun buildTimestampText(section: MeasurementSection): String {
        val parts = mutableListOf<String>()
        if (section.createdAt.isNotBlank()) parts.add("Created: ${section.createdAt}")
        if (section.updatedAt.isNotBlank()) parts.add("Updated: ${section.updatedAt}")
        return parts.joinToString("  |  ")
    }

    // ── Build tag chip view ───────────────────────────────────────────────────

    fun createTagChip(
        context: Context,
        tag: String,
        sectionCount: Int,
        isActive: Boolean,
        onClick: () -> Unit
    ): View {
        val tv = TextView(context)
        tv.apply {
            val countSuffix = if (sectionCount > 0) " ($sectionCount)" else ""
            text = "$tag$countSuffix"
            textSize = 13f
            setPadding(24, 12, 24, 12)
            setBackgroundResource(
                if (isActive) R.drawable.bg_tag_chip_selected
                else R.drawable.bg_tag_chip
            )
            setTextColor(context.getColor(
                if (isActive) R.color.colorTagTextSelected else R.color.colorTagText
            ))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 8
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        return tv
    }
}

// Extension to set color on MenuItem (Android API-specific)
private fun android.view.MenuItem.setTitleColor(color: Int) {
    val spannable = android.text.SpannableString(title)
    spannable.setSpan(
        android.text.style.ForegroundColorSpan(color),
        0, spannable.length, 0
    )
    title = spannable
}