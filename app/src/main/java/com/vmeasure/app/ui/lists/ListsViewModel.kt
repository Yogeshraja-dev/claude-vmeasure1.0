package com.vmeasure.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.model.FilterState
import com.vmeasure.app.data.model.User
import com.vmeasure.app.data.repository.UserRepository
import com.vmeasure.app.util.ShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 100
        const val MAX_SELECTION = 50
        private const val DEBOUNCE_MS = 300L
        private const val SEARCH_DEBOUNCE_MS = 400L
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    data class UiState(
        val users: List<User> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val canLoadMore: Boolean = false,
        val searchQuery: String = "",
        val filter: FilterState = FilterState(),
        val isSelectionMode: Boolean = false,
        val selectedIds: Set<String> = emptySet()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Event channel ─────────────────────────────────────────────────────────

    sealed class Event {
        data class ShareText(val text: String) : Event()
        data class ShowMessage(val message: String) : Event()
        object DeleteSuccess : Event()
    }

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    // ── Debounce jobs ─────────────────────────────────────────────────────────

    private val toggleJobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null
    private var currentOffset = 0

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadUsers(reset = true)
    }

    // ── Load users ────────────────────────────────────────────────────────────

    fun loadUsers(reset: Boolean = false) {
        if (reset) currentOffset = 0
        val state = _uiState.value
        if (!reset && !state.canLoadMore) return
        if (state.isLoadingMore && !reset) return

        viewModelScope.launch {
            _uiState.value = if (reset)
                state.copy(isLoading = true, error = null)
            else
                state.copy(isLoadingMore = true)

            try {
                val results = userRepository.getUsers(
                    searchQuery = state.searchQuery,
                    filter = state.filter,
                    limit = PAGE_SIZE,
                    offset = currentOffset
                )
                val updated = if (reset) results else state.users + results
                currentOffset += results.size

                _uiState.value = _uiState.value.copy(
                    users = updated,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = results.size == PAGE_SIZE
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load users"
                )
            }
        }
    }

    fun loadMore() = loadUsers(reset = false)

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadUsers(reset = true)
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    fun applyFilter(filter: FilterState) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadUsers(reset = true)
    }

    // ── Favourite toggle (debounced) ──────────────────────────────────────────

    fun toggleFavourite(userId: String, newValue: Boolean) {
        // Update UI immediately
        _uiState.value = _uiState.value.copy(
            users = _uiState.value.users.map {
                if (it.userId == userId) it.copy(isFavorite = newValue) else it
            }
        )
        // Debounce DB write
        val key = "${userId}_fav"
        toggleJobs[key]?.cancel()
        toggleJobs[key] = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            try {
                userRepository.toggleFavourite(userId, newValue)
            } catch (e: Exception) {
                // Revert on failure
                _uiState.value = _uiState.value.copy(
                    users = _uiState.value.users.map {
                        if (it.userId == userId) it.copy(isFavorite = !newValue) else it
                    }
                )
            }
            toggleJobs.remove(key)
        }
    }

    // ── Pin toggle (debounced) ────────────────────────────────────────────────

    fun togglePin(userId: String, newValue: Boolean) {
        _uiState.value = _uiState.value.copy(
            users = _uiState.value.users.map {
                if (it.userId == userId) it.copy(isPinned = newValue) else it
            }
        )
        val key = "${userId}_pin"
        toggleJobs[key]?.cancel()
        toggleJobs[key] = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            try {
                userRepository.togglePin(userId, newValue)
                // Reload to re-sort (pinned users go to top)
                loadUsers(reset = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    users = _uiState.value.users.map {
                        if (it.userId == userId) it.copy(isPinned = !newValue) else it
                    }
                )
            }
            toggleJobs.remove(key)
        }
    }

    // ── Delete single user ────────────────────────────────────────────────────

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepository.deleteUser(userId)
                _events.value = Event.DeleteSuccess
                loadUsers(reset = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete user"
                )
            }
        }
    }

    // ── Share single user ─────────────────────────────────────────────────────

    fun shareUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = userRepository.getUserWithSections(userId)
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (user != null) {
                    val text = ShareHelper.buildUserSummary(user)
                    _events.value = Event.ShareText(text)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to prepare share"
                )
            }
        }
    }

    // ── Bulk Selection ────────────────────────────────────────────────────────

    fun enterSelectionMode(userId: String) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = setOf(userId)
        )
    }

    fun toggleSelection(userId: String) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (userId in current) {
            current.remove(userId)
        } else {
            if (current.size >= MAX_SELECTION) {
                _events.value = Event.ShowMessage("Maximum $MAX_SELECTION users can be selected")
                return
            }
            current.add(userId)
        }
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    fun selectAll() {
        val allIds = _uiState.value.users
            .take(MAX_SELECTION)
            .map { it.userId }
            .toSet()
        _uiState.value = _uiState.value.copy(selectedIds = allIds)
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun bulkDelete() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userRepository.deleteUsers(ids)
                exitSelectionMode()
                _events.value = Event.DeleteSuccess
                loadUsers(reset = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete users"
                )
            }
        }
    }

    fun bulkShare() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val users = ids.mapNotNull { userRepository.getUserWithSections(it) }
                _uiState.value = _uiState.value.copy(isLoading = false)
                val text = ShareHelper.buildBulkSummary(users)
                _events.value = Event.ShareText(text)
                exitSelectionMode()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to prepare share"
                )
            }
        }
    }

    // ── Clear state on tab switch ─────────────────────────────────────────────

    fun clearState() {
        searchJob?.cancel()
        toggleJobs.values.forEach { it.cancel() }
        toggleJobs.clear()
        _uiState.value = UiState()
        _events.value = null
        loadUsers(reset = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearEvent() {
        _events.value = null
    }

    // ── Flush pending writes on stop ──────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // Flush all pending debounced toggles immediately
        toggleJobs.values.forEach { it.cancel() }
    }
}