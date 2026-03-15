package com.vmeasure.app.ui.detail

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
import com.vmeasure.app.data.model.User
import com.vmeasure.app.databinding.FragmentDetailBinding
import com.vmeasure.app.util.SectionHelper
import com.vmeasure.app.util.show
import com.vmeasure.app.util.hide
import com.vmeasure.app.util.showDatePicker
import com.vmeasure.app.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString("userId") ?: return
        viewModel.loadUser(userId)
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
                override fun handleOnBackPressed() {
                    if (viewModel.uiState.value.isEditMode) showDiscardDialog()
                    else findNavController().popBackStack()
                }
            }
        )
        binding.btnBack.setOnClickListener {
            if (viewModel.uiState.value.isEditMode) showDiscardDialog()
            else findNavController().popBackStack()
        }
    }

    private fun showDiscardDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_discard_title)
            .setMessage(R.string.dialog_discard_message)
            .setPositiveButton(R.string.dialog_discard_confirm) { _, _ ->
                viewModel.cancelEdit()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnEdit.setOnClickListener { viewModel.enterEditMode() }
        binding.btnCancel.setOnClickListener { showDiscardDialog() }
        binding.btnSave.setOnClickListener { viewModel.save() }

        binding.etName.addTextChangedListener(simpleWatcher { viewModel.onNameChanged(it) })
        binding.etContact.addTextChangedListener(simpleWatcher { viewModel.onContactChanged(it) })
        binding.etInstagram.addTextChangedListener(simpleWatcher { viewModel.onInstagramChanged(it) })
        binding.etOtherMedia.addTextChangedListener(simpleWatcher { viewModel.onOtherMediaChanged(it) })
        binding.etLocation.addTextChangedListener(simpleWatcher { viewModel.onLocationChanged(it) })

        binding.switchFavourite.setOnCheckedChangeListener { _, checked ->
            if (viewModel.uiState.value.isEditMode) viewModel.onFavouriteChanged(checked)
        }
        binding.switchPin.setOnCheckedChangeListener { _, checked ->
            if (viewModel.uiState.value.isEditMode) viewModel.onPinChanged(checked)
        }

        binding.etDob.setOnClickListener {
            if (viewModel.uiState.value.isEditMode)
                binding.etDob.showDatePicker(requireContext()) { date, millis ->
                    viewModel.onDobChanged(date, millis)
                }
        }
        binding.etSpecialDate.setOnClickListener {
            if (viewModel.uiState.value.isEditMode)
                binding.etSpecialDate.showDatePicker(requireContext()) { date, millis ->
                    viewModel.onSpecialDateChanged(date, millis)
                }
        }
    }

    // ── Observe state ─────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) (activity as? MainActivity)?.showLoader()
                    else (activity as? MainActivity)?.hideLoader()

                    state.error?.let {
                        requireContext().toast(it)
                        viewModel.clearError()
                    }

                    if (state.isEditMode) {
                        val editUser = state.editUser ?: return@collect
                        renderForm(editUser, editable = true)
                        rebuildTagChips(
                            activeTags = state.editSections.map { it.type }.toSet(),
                            tagCounts = state.editSections.groupBy { it.type }.mapValues { it.value.size },
                            editable = true
                        )
                        rebuildSections(state.editSections, editable = true)
                        binding.btnEdit.hide()
                        binding.footer.show()
                        // Name error
                        if (state.nameError != null) {
                            binding.tvNameError.text = state.nameError
                            binding.tvNameError.show()
                        } else {
                            binding.tvNameError.hide()
                        }
                    } else {
                        val user = state.user ?: return@collect
                        renderForm(user, editable = false)
                        rebuildTagChips(
                            activeTags = user.sections.map { it.type }.toSet(),
                            tagCounts = user.sections.groupBy { it.type }.mapValues { it.value.size },
                            editable = false
                        )
                        rebuildSections(user.sections, editable = false)
                        binding.btnEdit.show()
                        binding.footer.hide()
                        binding.tvNameError.hide()
                    }
                }
            }
        }
    }

    // ── Render form fields ────────────────────────────────────────────────────

    private fun renderForm(user: User, editable: Boolean) {
        fun setField(et: android.widget.EditText, value: String) {
            if (et.text.toString() != value) et.setText(value)
            et.isEnabled = editable
        }

        setField(binding.etName, user.name)
        setField(binding.etDob, user.dateOfBirth)
        setField(binding.etSpecialDate, user.specialDate)
        setField(binding.etContact, user.contactNumber)
        setField(binding.etInstagram, user.instagramId)
        setField(binding.etOtherMedia, user.otherMediaId)
        setField(binding.etLocation, user.location)

        binding.switchFavourite.isChecked = user.isFavorite
        binding.switchFavourite.isEnabled = editable
        binding.switchPin.isChecked = user.isPinned
        binding.switchPin.isEnabled = editable
    }

    // ── Rebuild tag chips ─────────────────────────────────────────────────────

    private fun rebuildTagChips(
        activeTags: Set<String>,
        tagCounts: Map<String, Int>,
        editable: Boolean
    ) {
        binding.tagChipsContainer.removeAllViews()
        TagType.orderedList.forEach { tagType ->
            val tag = tagType.displayName
            val count = tagCounts[tag] ?: 0
            val chip = SectionHelper.createTagChip(
                requireContext(), tag, count, tag in activeTags
            ) {
                if (editable) {
                    viewModel.onTagTapped(tag)
                    scrollToTag(tag)
                }
            }
            binding.tagChipsContainer.addView(chip)
        }
    }

    // ── Rebuild sections ──────────────────────────────────────────────────────

    private fun rebuildSections(
        sections: List<com.vmeasure.app.data.model.MeasurementSection>,
        editable: Boolean
    ) {
        binding.sectionsContainer.removeAllViews()
        sections.forEach { section ->
            val sectionView = SectionHelper.createSectionView(
                context = requireContext(),
                section = section,
                isEditable = editable,
                showTimestamps = true,
                callback = object : SectionHelper.SectionCallback {
                    override fun onFieldChanged(
                        sectionId: String,
                        updater: (com.vmeasure.app.data.model.MeasurementSection)
                        -> com.vmeasure.app.data.model.MeasurementSection
                    ) {
                        if (editable) viewModel.updateSectionField(sectionId, updater)
                    }
                    override fun onClearAll(sectionId: String) {
                        if (editable) viewModel.clearSection(sectionId)
                    }
                    override fun onDuplicate(sectionId: String) {
                        if (editable) viewModel.duplicateSection(sectionId)
                    }
                    override fun onDelete(sectionId: String) {
                        if (editable) viewModel.deleteSection(sectionId)
                    }
                }
            )
            sectionView.tag = section.sectionId
            binding.sectionsContainer.addView(sectionView)
        }
    }

    private fun scrollToTag(tag: String) {
        binding.scrollBody.post {
            val sections = viewModel.uiState.value.editSections
            val first = sections.firstOrNull { it.type == tag } ?: return@post
            val v = binding.sectionsContainer.findViewWithTag<View>(first.sectionId) ?: return@post
            binding.scrollBody.smoothScrollTo(0, v.top + binding.sectionsContainer.top)
        }
    }

    // ── Observe events ────────────────────────────────────────────────────────

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is DetailViewModel.Event.SaveSuccess -> {
                            viewModel.clearEvent()
                            findNavController().popBackStack()
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { onChanged(s.toString()) }
        }
}