package com.vmeasure.app.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.data.model.TagType
import com.vmeasure.app.data.model.User
import com.vmeasure.app.data.repository.UserRepository
import com.vmeasure.app.util.DateFormatter
import com.vmeasure.app.util.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        const val MAX_SECTIONS_PER_TAG = 10
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    data class UiState(
        val name: String = "",
        val dateOfBirth: String = "",
        val dobMillis: Long = 0L,
        val specialDate: String = "",
        val specialDateMillis: Long = 0L,
        val isFavourite: Boolean = false,
        val isPinned: Boolean = false,
        val contactNumber: String = "",
        val instagramId: String = "",
        val otherMediaId: String = "",
        val location: String = "",
        val sections: List<MeasurementSection> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val nameError: String? = null
    ) {
        val activeTags: Set<String>
            get() = sections.map { it.type }.toSet()

        fun sectionCountForTag(tag: String): Int =
            sections.count { it.type == tag }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed class Event {
        object SaveSuccess : Event()
    }

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    // Existing userId for edit mode; null = add mode
    private var editingUserId: String? = null

    // ── Init for edit mode ────────────────────────────────────────────────────

    fun loadUser(userId: String) {
        editingUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = userRepository.getUserWithSections(userId)
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        name = user.name,
                        dateOfBirth = user.dateOfBirth,
                        dobMillis = user.dobMillis,
                        specialDate = user.specialDate,
                        specialDateMillis = user.specialDateMillis,
                        isFavourite = user.isFavorite,
                        isPinned = user.isPinned,
                        contactNumber = user.contactNumber,
                        instagramId = user.instagramId,
                        otherMediaId = user.otherMediaId,
                        location = user.location,
                        sections = user.sections,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load user"
                )
            }
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onNameChanged(v: String) {
        _uiState.value = _uiState.value.copy(name = v, nameError = null)
    }

    fun onDobChanged(v: String, millis: Long) {
        _uiState.value = _uiState.value.copy(dateOfBirth = v, dobMillis = millis)
    }

    fun onSpecialDateChanged(v: String, millis: Long) {
        _uiState.value = _uiState.value.copy(specialDate = v, specialDateMillis = millis)
    }

    fun onFavouriteChanged(v: Boolean) {
        _uiState.value = _uiState.value.copy(isFavourite = v)
    }

    fun onPinChanged(v: Boolean) {
        _uiState.value = _uiState.value.copy(isPinned = v)
    }

    fun onContactChanged(v: String) {
        _uiState.value = _uiState.value.copy(contactNumber = v)
    }

    fun onInstagramChanged(v: String) {
        _uiState.value = _uiState.value.copy(instagramId = v)
    }

    fun onOtherMediaChanged(v: String) {
        _uiState.value = _uiState.value.copy(otherMediaId = v)
    }

    fun onLocationChanged(v: String) {
        _uiState.value = _uiState.value.copy(location = v)
    }

    // ── Tag / Section operations ──────────────────────────────────────────────

    fun onTagTapped(tag: String) {
        val state = _uiState.value
        // If tag already has sections, do nothing
        if (state.sectionCountForTag(tag) > 0) return
        // Add first section for this tag
        addSectionForTag(tag)
    }

    fun addSectionForTag(tag: String) {
        val state = _uiState.value
        if (state.sectionCountForTag(tag) >= MAX_SECTIONS_PER_TAG) {
            _uiState.value = state.copy(
                error = "Maximum $MAX_SECTIONS_PER_TAG sections allowed per tag"
            )
            return
        }
        val now = DateFormatter.nowTimestamp()
        val nowMillis = DateFormatter.nowMillis()
        val newSection = MeasurementSection(
            sectionId = IdGenerator.generate(),
            userId = editingUserId ?: "",
            type = tag,
            createdAt = now,
            updatedAt = "",
            createdAtMillis = nowMillis,
            updatedAtMillis = 0L,
            sortOrder = state.sections.size
        )
        _uiState.value = state.copy(sections = state.sections + newSection)
    }

    fun duplicateSection(sectionId: String) {
        val state = _uiState.value
        val idx = state.sections.indexOfFirst { it.sectionId == sectionId }
        if (idx < 0) return
        val original = state.sections[idx]

        if (state.sectionCountForTag(original.type) >= MAX_SECTIONS_PER_TAG) {
            _uiState.value = state.copy(
                error = "Maximum $MAX_SECTIONS_PER_TAG sections allowed per tag"
            )
            return
        }

        val now = DateFormatter.nowTimestamp()
        val copy = original.copy(
            sectionId = IdGenerator.generate(),
            createdAt = now,
            updatedAt = "",
            createdAtMillis = DateFormatter.nowMillis(),
            updatedAtMillis = 0L
        )
        // Insert immediately after original
        val newList = state.sections.toMutableList()
        newList.add(idx + 1, copy)
        _uiState.value = state.copy(sections = newList)
    }

    fun deleteSection(sectionId: String) {
        val state = _uiState.value
        val newList = state.sections.filter { it.sectionId != sectionId }
        _uiState.value = state.copy(sections = newList)
    }

    fun clearSection(sectionId: String) {
        val state = _uiState.value
        val newList = state.sections.map { s ->
            if (s.sectionId == sectionId) {
                s.copy(
                    uBust = "", bust = "", waist = "", hip = "",
                    armhole = "", shoulder = "", length = "",
                    fNeck = "", bNeck = "", sleeveLength = "", sleeveRound = "",
                    blouseCut = "", blouseField = "",
                    thighRound = "", kneeRound = "", bottom = "", inseam = "",
                    frockLength = "", yokeLength = "",
                    blouseWaist = "", blouseLength = "", skirtLength = "", waistLength = "",
                    chest = "", pantLength = "", pantWaist = "",
                    notes = ""
                )
            } else s
        }
        _uiState.value = state.copy(sections = newList)
    }

    fun updateSectionField(sectionId: String, updater: (MeasurementSection) -> MeasurementSection) {
        val state = _uiState.value
        val newList = state.sections.map { s ->
            if (s.sectionId == sectionId) updater(s) else s
        }
        _uiState.value = state.copy(sections = newList)
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        // Validate name
        if (state.name.trim().isEmpty()) {
            _uiState.value = state.copy(nameError = "Customer name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, nameError = null)
            try {
                val userId = editingUserId ?: IdGenerator.generate()
                val user = User(
                    userId = userId,
                    name = state.name.trim(),
                    dateOfBirth = state.dateOfBirth,
                    specialDate = state.specialDate,
                    isFavorite = state.isFavourite,
                    isPinned = state.isPinned,
                    contactNumber = state.contactNumber,
                    instagramId = state.instagramId,
                    otherMediaId = state.otherMediaId,
                    location = state.location,
                    createdAt = "",
                    updatedAt = "",
                    selectedTags = emptyMap(),
                    createdAtMillis = 0L,
                    updatedAtMillis = 0L,
                    specialDateMillis = state.specialDateMillis,
                    dobMillis = state.dobMillis
                )
                if (editingUserId == null) {
                    userRepository.saveNewUser(user, state.sections)
                } else {
                    userRepository.updateUser(user, state.sections, emptySet())
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.value = Event.SaveSuccess
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearEvent() { _events.value = null }

    fun resetState() {
        editingUserId = null
        _uiState.value = UiState()
        _events.value = null
    }
}