package com.vmeasure.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.model.MeasurementSection
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
class DetailViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        const val MAX_SECTIONS_PER_TAG = 10
    }

    data class UiState(
        val user: User? = null,
        val editUser: User? = null,               // mutable copy during edit
        val editSections: List<MeasurementSection> = emptyList(),
        val changedSectionIds: Set<String> = emptySet(),
        val isEditMode: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val nameError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed class Event {
        object SaveSuccess : Event()
    }

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = userRepository.getUserWithSections(userId)
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false,
                    isEditMode = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load user"
                )
            }
        }
    }

    // ── Edit mode ─────────────────────────────────────────────────────────────

    fun enterEditMode() {
        val user = _uiState.value.user ?: return
        _uiState.value = _uiState.value.copy(
            isEditMode = true,
            editUser = user,
            editSections = user.sections,
            changedSectionIds = emptySet()
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            editUser = null,
            editSections = emptyList(),
            changedSectionIds = emptySet(),
            nameError = null
        )
    }

    // ── Field updates (edit mode only) ────────────────────────────────────────

    fun onNameChanged(v: String) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(
            editUser = eu.copy(name = v),
            nameError = null
        )
    }

    fun onDobChanged(v: String, millis: Long) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(dateOfBirth = v, dobMillis = millis))
    }

    fun onSpecialDateChanged(v: String, millis: Long) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(specialDate = v, specialDateMillis = millis))
    }

    fun onFavouriteChanged(v: Boolean) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(isFavorite = v))
    }

    fun onPinChanged(v: Boolean) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(isPinned = v))
    }

    fun onContactChanged(v: String) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(contactNumber = v))
    }

    fun onInstagramChanged(v: String) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(instagramId = v))
    }

    fun onOtherMediaChanged(v: String) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(otherMediaId = v))
    }

    fun onLocationChanged(v: String) {
        val eu = _uiState.value.editUser ?: return
        _uiState.value = _uiState.value.copy(editUser = eu.copy(location = v))
    }

    // ── Section operations (edit mode) ────────────────────────────────────────

    fun onTagTapped(tag: String) {
        val state = _uiState.value
        val count = state.editSections.count { it.type == tag }
        if (count > 0) return
        addSectionForTag(tag)
    }

    fun addSectionForTag(tag: String) {
        val state = _uiState.value
        val count = state.editSections.count { it.type == tag }
        if (count >= MAX_SECTIONS_PER_TAG) {
            _uiState.value = state.copy(error = "Maximum $MAX_SECTIONS_PER_TAG sections per tag")
            return
        }
        val now = DateFormatter.nowTimestamp()
        val newSection = MeasurementSection(
            sectionId = IdGenerator.generate(),
            userId = state.user?.userId ?: "",
            type = tag,
            createdAt = now,
            updatedAt = "",
            createdAtMillis = DateFormatter.nowMillis(),
            updatedAtMillis = 0L,
            sortOrder = state.editSections.size
        )
        _uiState.value = state.copy(
            editSections = state.editSections + newSection,
            changedSectionIds = state.changedSectionIds + newSection.sectionId
        )
    }

    fun duplicateSection(sectionId: String) {
        val state = _uiState.value
        val idx = state.editSections.indexOfFirst { it.sectionId == sectionId }
        if (idx < 0) return
        val original = state.editSections[idx]
        val count = state.editSections.count { it.type == original.type }
        if (count >= MAX_SECTIONS_PER_TAG) {
            _uiState.value = state.copy(error = "Maximum $MAX_SECTIONS_PER_TAG sections per tag")
            return
        }
        val copy = original.copy(
            sectionId = IdGenerator.generate(),
            createdAt = DateFormatter.nowTimestamp(),
            updatedAt = "",
            createdAtMillis = DateFormatter.nowMillis(),
            updatedAtMillis = 0L
        )
        val newList = state.editSections.toMutableList()
        newList.add(idx + 1, copy)
        _uiState.value = state.copy(
            editSections = newList,
            changedSectionIds = state.changedSectionIds + copy.sectionId
        )
    }

    fun deleteSection(sectionId: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            editSections = state.editSections.filter { it.sectionId != sectionId }
        )
    }

    fun clearSection(sectionId: String) {
        val state = _uiState.value
        val newList = state.editSections.map { s ->
            if (s.sectionId == sectionId) {
                s.copy(
                    uBust = "", bust = "", waist = "", hip = "",
                    armhole = "", shoulder = "", length = "",
                    fNeck = "", bNeck = "", sleeveLength = "", sleeveRound = "",
                    blouseCut = "", blouseField = "",
                    thighRound = "", kneeRound = "", bottom = "", inseam = "",
                    frockLength = "", yokeLength = "",
                    blouseWaist = "", blouseLength = "", skirtLength = "", waistLength = "",
                    chest = "", pantLength = "", pantWaist = "", notes = ""
                )
            } else s
        }
        _uiState.value = state.copy(
            editSections = newList,
            changedSectionIds = state.changedSectionIds + sectionId
        )
    }

    fun updateSectionField(
        sectionId: String,
        updater: (MeasurementSection) -> MeasurementSection
    ) {
        val state = _uiState.value
        val newList = state.editSections.map { s ->
            if (s.sectionId == sectionId) updater(s) else s
        }
        _uiState.value = state.copy(
            editSections = newList,
            changedSectionIds = state.changedSectionIds + sectionId
        )
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        val editUser = state.editUser ?: return
        if (editUser.name.trim().isEmpty()) {
            _uiState.value = state.copy(nameError = "Customer name is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, nameError = null)
            try {
                userRepository.updateUser(
                    user = editUser,
                    sections = state.editSections,
                    changedSectionIds = state.changedSectionIds
                )
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

    // ── Reset on back ─────────────────────────────────────────────────────────

    fun resetState() {
        _uiState.value = UiState()
        _events.value = null
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearEvent() { _events.value = null }
}