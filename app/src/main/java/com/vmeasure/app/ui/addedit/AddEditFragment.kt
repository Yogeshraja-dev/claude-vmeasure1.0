package com.vmeasure.app.ui.addedit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.vmeasure.app.MainActivity
import com.vmeasure.app.R
import com.vmeasure.app.data.model.TagType
import com.vmeasure.app.databinding.FragmentAddEditBinding
import com.vmeasure.app.util.SectionHelper
import com.vmeasure.app.util.show
import com.vmeasure.app.util.hide
import com.vmeasure.app.util.showDatePicker
import com.vmeasure.app.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditFragment : Fragment() {

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditViewModel by viewModels()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
        observeEvents()
        setupBackPress()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetState()
        _binding = null
    }

    // ── Back press ────────────────────────────────────────────────────────────

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = showDiscardDialog()
            }
        )
        binding.btnBack.setOnClickListener { showDiscardDialog() }
    }

    private fun showDiscardDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_discard_title)
            .setMessage(R.string.dialog_discard_message)
            .setPositiveButton(R.string.dialog_discard_confirm) { _, _ ->
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onNameChanged(s.toString())
            }
        })

        binding.etDob.setOnClickListener {
            binding.etDob.showDatePicker(requireContext()) { date, millis ->
                viewModel.onDobChanged(date, millis)
            }
        }

        binding.etSpecialDate.setOnClickListener {
            binding.etSpecialDate.showDatePicker(requireContext()) { date, millis ->
                viewModel.onSpecialDateChanged(date, millis)
            }
        }

        binding.switchFavourite.setOnCheckedChangeListener { _, checked ->
            viewModel.onFavouriteChanged(checked)
        }

        binding.switchPin.setOnCheckedChangeListener { _, checked ->
            viewModel.onPinChanged(checked)
        }

        binding.etContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onContactChanged(s.toString())
            }
        })

        binding.etInstagram.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onInstagramChanged(s.toString())
            }
        })

        binding.etOtherMedia.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onOtherMediaChanged(s.toString())
            }
        })

        binding.etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onLocationChanged(s.toString())
            }
        })

        binding.btnSave.setOnClickListener { viewModel.save() }
    }

    // ── Observe state ─────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loader
                    if (state.isLoading) (activity as? MainActivity)?.showLoader()
                    else (activity as? MainActivity)?.hideLoader()

                    // Name error
                    if (state.nameError != null) {
                        binding.tvNameError.text = state.nameError
                        binding.tvNameError.show()
                    } else {
                        binding.tvNameError.hide()
                    }

                    // General error
                    state.error?.let {
                        requireContext().toast(it)
                        viewModel.clearError()
                    }

                    // Rebuild tag chips and sections
                    rebuildTagChips(state.activeTags, state.sections
                        .groupBy { it.type }
                        .mapValues { it.value.size })
                    rebuildSections(state)
                }
            }
        }
    }

    // ── Rebuild tag chips ─────────────────────────────────────────────────────

    private fun rebuildTagChips(
        activeTags: Set<String>,
        tagCounts: Map<String, Int>
    ) {
        binding.tagChipsContainer.removeAllViews()
        TagType.orderedList.forEach { tagType ->
            val tag = tagType.displayName
            val count = tagCounts[tag] ?: 0
            val chip = SectionHelper.createTagChip(
                requireContext(), tag, count, tag in activeTags
            ) {
                viewModel.onTagTapped(tag)
                // Scroll to first section of this tag after adding
                scrollToTag(tag)
            }
            binding.tagChipsContainer.addView(chip)
        }
    }

    // ── Rebuild sections ──────────────────────────────────────────────────────

    private fun rebuildSections(state: AddEditViewModel.UiState) {
        binding.sectionsContainer.removeAllViews()
        state.sections.forEach { section ->
            val sectionView = SectionHelper.createSectionView(
                context = requireContext(),
                section = section,
                isEditable = true,
                showTimestamps = false,
                callback = object : SectionHelper.SectionCallback {
                    override fun onFieldChanged(
                        sectionId: String,
                        updater: (com.vmeasure.app.data.model.MeasurementSection)
                        -> com.vmeasure.app.data.model.MeasurementSection
                    ) {
                        viewModel.updateSectionField(sectionId, updater)
                    }
                    override fun onClearAll(sectionId: String) {
                        viewModel.clearSection(sectionId)
                    }
                    override fun onDuplicate(sectionId: String) {
                        viewModel.duplicateSection(sectionId)
                    }
                    override fun onDelete(sectionId: String) {
                        viewModel.deleteSection(sectionId)
                    }
                }
            )
            sectionView.tag = section.sectionId
            binding.sectionsContainer.addView(sectionView)
        }
    }

    private fun scrollToTag(tag: String) {
        binding.scrollBody.post {
            val sections = viewModel.uiState.value.sections
            val firstForTag = sections.firstOrNull { it.type == tag } ?: return@post
            val sectionView = binding.sectionsContainer.findViewWithTag<View>(firstForTag.sectionId)
                ?: return@post
            val scrollY = sectionView.top + binding.sectionsContainer.top
            binding.scrollBody.smoothScrollTo(0, scrollY)
        }
    }

    // ── Observe events ────────────────────────────────────────────────────────

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AddEditViewModel.Event.SaveSuccess -> {
                            viewModel.clearEvent()
                            findNavController().popBackStack()
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}