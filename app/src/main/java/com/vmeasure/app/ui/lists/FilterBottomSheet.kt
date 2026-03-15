package com.vmeasure.app.ui.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vmeasure.app.data.model.FilterState
import com.vmeasure.app.data.model.SortMode
import com.vmeasure.app.data.model.TagType
import com.vmeasure.app.databinding.BottomSheetFilterBinding
import com.vmeasure.app.util.DateFormatter
import com.vmeasure.app.util.SectionHelper
import com.vmeasure.app.util.showDatePicker

class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    var currentFilter: FilterState = FilterState()
    var onApply: ((FilterState) -> Unit)? = null

    // Local working copy
    private var workingFilter = FilterState()
    private val selectedTags = mutableSetOf<String>()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        workingFilter = currentFilter.copy()
        selectedTags.addAll(currentFilter.selectedTags)
        setupViews()
        populateFromFilter()
        updateApplyButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupViews() {
        binding.btnClose.setOnClickListener { dismiss() }

        // Sort by radio group
        binding.rgSort.setOnCheckedChangeListener { _, checkedId ->
            workingFilter = workingFilter.copy(
                sortMode = when (checkedId) {
                    binding.rbCustomDate.id -> SortMode.CUSTOM_DATE
                    binding.rbRecent.id     -> SortMode.RECENT
                    binding.rbOldest.id     -> SortMode.OLDEST
                    binding.rbAZ.id         -> SortMode.AZ
                    binding.rbZA.id         -> SortMode.ZA
                    else                    -> SortMode.RECENT
                }
            )
            binding.customDateContainer.visibility =
                if (workingFilter.sortMode == SortMode.CUSTOM_DATE) View.VISIBLE else View.GONE
            updateApplyButton()
        }

        // Custom date pickers
        binding.etCustomFrom.setOnClickListener {
            binding.etCustomFrom.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(customDateFrom = millis)
                updateApplyButton()
            }
        }
        binding.etCustomTo.setOnClickListener {
            binding.etCustomTo.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(customDateTo = millis)
                updateApplyButton()
            }
        }

        // Type chips
        val row1Tags = listOf("Blouse", "Kurti", "Pant", "Frock")
        val row2Tags = listOf("Crop Blouse and Skirt", "Kids Boy")

        row1Tags.forEach { tag ->
            val chip = SectionHelper.createTagChip(
                requireContext(), tag, 0, tag in selectedTags
            ) {
                if (tag in selectedTags) selectedTags.remove(tag)
                else selectedTags.add(tag)
                workingFilter = workingFilter.copy(selectedTags = selectedTags.toSet())
                refreshTypeChips(row1Tags, row2Tags)
                updateApplyButton()
            }
            binding.typeChipsRow1.addView(chip)
        }

        row2Tags.forEach { tag ->
            val chip = SectionHelper.createTagChip(
                requireContext(), tag, 0, tag in selectedTags
            ) {
                if (tag in selectedTags) selectedTags.remove(tag)
                else selectedTags.add(tag)
                workingFilter = workingFilter.copy(selectedTags = selectedTags.toSet())
                refreshTypeChips(row1Tags, row2Tags)
                updateApplyButton()
            }
            binding.typeChipsRow2.addView(chip)
        }

        // Favourite / Pinned
        binding.switchFavourite.setOnCheckedChangeListener { _, checked ->
            workingFilter = workingFilter.copy(favouriteOnly = checked)
            updateApplyButton()
        }
        binding.switchPinned.setOnCheckedChangeListener { _, checked ->
            workingFilter = workingFilter.copy(pinnedOnly = checked)
            updateApplyButton()
        }

        // Special date
        binding.etSpecialFrom.setOnClickListener {
            binding.etSpecialFrom.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(specialDateFrom = millis)
                updateApplyButton()
            }
        }
        binding.etSpecialTo.setOnClickListener {
            binding.etSpecialTo.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(specialDateTo = millis)
                updateApplyButton()
            }
        }

        // Birth date
        binding.etBirthFrom.setOnClickListener {
            binding.etBirthFrom.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(birthDateFrom = millis)
                updateApplyButton()
            }
        }
        binding.etBirthTo.setOnClickListener {
            binding.etBirthTo.showDatePicker(requireContext()) { _, millis ->
                workingFilter = workingFilter.copy(birthDateTo = millis)
                updateApplyButton()
            }
        }

        // Apply
        binding.btnApply.setOnClickListener {
            // If custom date but both from and to are 0 → reset to RECENT
            val finalFilter = if (workingFilter.sortMode == SortMode.CUSTOM_DATE &&
                workingFilter.customDateFrom == 0L && workingFilter.customDateTo == 0L
            ) {
                workingFilter.copy(sortMode = SortMode.RECENT)
            } else {
                workingFilter
            }
            onApply?.invoke(finalFilter)
            dismiss()
        }
    }

    private fun populateFromFilter() {
        when (currentFilter.sortMode) {
            SortMode.CUSTOM_DATE -> binding.rbCustomDate.isChecked = true
            SortMode.RECENT      -> binding.rbRecent.isChecked = true
            SortMode.OLDEST      -> binding.rbOldest.isChecked = true
            SortMode.AZ          -> binding.rbAZ.isChecked = true
            SortMode.ZA          -> binding.rbZA.isChecked = true
        }

        if (currentFilter.sortMode == SortMode.CUSTOM_DATE) {
            binding.customDateContainer.visibility = View.VISIBLE
            if (currentFilter.customDateFrom > 0)
                binding.etCustomFrom.setText(DateFormatter.millisToDate(currentFilter.customDateFrom))
            if (currentFilter.customDateTo > 0)
                binding.etCustomTo.setText(DateFormatter.millisToDate(currentFilter.customDateTo))
        }

        binding.switchFavourite.isChecked = currentFilter.favouriteOnly
        binding.switchPinned.isChecked = currentFilter.pinnedOnly

        if (currentFilter.specialDateFrom > 0)
            binding.etSpecialFrom.setText(DateFormatter.millisToDate(currentFilter.specialDateFrom))
        if (currentFilter.specialDateTo > 0)
            binding.etSpecialTo.setText(DateFormatter.millisToDate(currentFilter.specialDateTo))
        if (currentFilter.birthDateFrom > 0)
            binding.etBirthFrom.setText(DateFormatter.millisToDate(currentFilter.birthDateFrom))
        if (currentFilter.birthDateTo > 0)
            binding.etBirthTo.setText(DateFormatter.millisToDate(currentFilter.birthDateTo))
    }

    private fun refreshTypeChips(row1Tags: List<String>, row2Tags: List<String>) {
        binding.typeChipsRow1.removeAllViews()
        binding.typeChipsRow2.removeAllViews()
        row1Tags.forEach { tag ->
            binding.typeChipsRow1.addView(SectionHelper.createTagChip(
                requireContext(), tag, 0, tag in selectedTags
            ) {
                if (tag in selectedTags) selectedTags.remove(tag) else selectedTags.add(tag)
                workingFilter = workingFilter.copy(selectedTags = selectedTags.toSet())
                refreshTypeChips(row1Tags, row2Tags)
                updateApplyButton()
            })
        }
        row2Tags.forEach { tag ->
            binding.typeChipsRow2.addView(SectionHelper.createTagChip(
                requireContext(), tag, 0, tag in selectedTags
            ) {
                if (tag in selectedTags) selectedTags.remove(tag) else selectedTags.add(tag)
                workingFilter = workingFilter.copy(selectedTags = selectedTags.toSet())
                refreshTypeChips(row1Tags, row2Tags)
                updateApplyButton()
            })
        }
    }

    private fun updateApplyButton() {
        binding.btnApply.isEnabled = !workingFilter.isDefault
    }

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            currentFilter: FilterState,
            onApply: (FilterState) -> Unit
        ): FilterBottomSheet {
            return FilterBottomSheet().also {
                it.currentFilter = currentFilter
                it.onApply = onApply
                it.show(fragmentManager, "filter")
            }
        }
    }
}