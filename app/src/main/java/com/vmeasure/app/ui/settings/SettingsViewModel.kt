package com.vmeasure.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSignedIn: Boolean = false,
        val accountEmail: String = "",
        val lastSyncTime: String = "",
        val error: String? = null,
        val successMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    sealed class Event {
        object RequestSignIn : Event()
        object RequestSyncConflictWarning : Event()
    }

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    // Whether conflict was acknowledged by user
    private var conflictAcknowledged = false

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            val isSignedIn = syncRepository.isSignedIn()
            val email = syncRepository.getStoredEmail() ?: ""
            val lastSync = syncRepository.getLastSyncTime() ?: ""
            _uiState.value = _uiState.value.copy(
                isSignedIn = isSignedIn,
                accountEmail = email,
                lastSyncTime = lastSync
            )
        }
    }

    // ── Google Sign-In result ─────────────────────────────────────────────────

    fun onSignInSuccess(email: String, token: String) {
        viewModelScope.launch {
            syncRepository.storeAccountInfo(email, token)
            _uiState.value = _uiState.value.copy(
                isSignedIn = true,
                accountEmail = email
            )
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    fun onSyncTapped() {
        if (!_uiState.value.isSignedIn) {
            _events.value = Event.RequestSignIn
            return
        }
        conflictAcknowledged = false
        startSync()
    }

    fun onConflictAcknowledged() {
        conflictAcknowledged = true
    }

    private fun startSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            val token = syncRepository.getStoredToken() ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Please sign in again"
                )
                return@launch
            }

            val result = syncRepository.performSync(
                token = token,
                onConflict = {
                    // Signal UI to show dialog and wait for user response
                    _events.value = Event.RequestSyncConflictWarning
                    // Wait for user decision (max 30s)
                    var waited = 0
                    while (!conflictAcknowledged && waited < 30000) {
                        kotlinx.coroutines.delay(100)
                        waited += 100
                    }
                    conflictAcknowledged
                }
            )

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Sync completed successfully",
                    lastSyncTime = syncRepository.getLastSyncTime() ?: ""
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = "Sync failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearState() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
        _events.value = null
        loadStatus()
    }

    fun clearEvent() { _events.value = null }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(successMessage = null) }
}