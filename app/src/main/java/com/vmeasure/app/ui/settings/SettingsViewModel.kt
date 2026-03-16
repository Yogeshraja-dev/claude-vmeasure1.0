package com.vmeasure.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.repository.DriveTokenManager
import com.vmeasure.app.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val driveTokenManager: DriveTokenManager
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

    // Tracks whether user acknowledged the conflict warning dialog
    private var conflictAcknowledged = false
    // Stored temporarily during sign-in flow
    private var pendingServerAuthCode: String = ""
    private var pendingClientId: String = ""
    private var pendingClientSecret: String = ""

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            val isSignedIn = syncRepository.isSignedIn()
            val email      = syncRepository.getStoredEmail() ?: ""
            val lastSync   = syncRepository.getLastSyncTime() ?: ""
            _uiState.value = _uiState.value.copy(
                isSignedIn   = isSignedIn,
                accountEmail = email,
                lastSyncTime = lastSync
            )
        }
    }

    // ── Called when Google Sign-In returns a server auth code ─────────────────

    fun onSignInSuccess(
        email: String,
        serverAuthCode: String,
        clientId: String,
        clientSecret: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Exchange server auth code for tokens
            val tokenResult = driveTokenManager.storeTokensFromAuthCode(
                serverAuthCode, clientId, clientSecret
            )
            if (tokenResult.isSuccess) {
                syncRepository.storeAccountInfo(email, tokenResult.getOrDefault(""))
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    isSignedIn   = true,
                    accountEmail = email
                )
            } else {
                // Store email even if token exchange failed — retry on next sync
                syncRepository.storeAccountInfo(email, "")
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    isSignedIn   = true,
                    accountEmail = email,
                    error        = "Signed in but Drive access needs re-authorisation. Tap Sync to retry."
                )
            }
        }
    }

    // ── Sync tap ──────────────────────────────────────────────────────────────

    fun onSyncTapped(clientId: String, clientSecret: String) {
        if (!_uiState.value.isSignedIn) {
            _events.value = Event.RequestSignIn
            return
        }
        conflictAcknowledged = false
        pendingClientId      = clientId
        pendingClientSecret  = clientSecret
        startSync()
    }

    fun onConflictAcknowledged() {
        conflictAcknowledged = true
    }

    private fun startSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading      = true,
                error          = null,
                successMessage = null
            )

            // Get a valid access token (auto-refresh if expired)
            val tokenResult = driveTokenManager.getValidAccessToken(
                pendingClientId, pendingClientSecret
            )

            if (tokenResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error     = "Authentication expired. Please sign in again."
                )
                _events.value = Event.RequestSignIn
                return@launch
            }

            val accessToken = tokenResult.getOrThrow()

            val syncResult = syncRepository.performSync(
                token      = accessToken,
                onConflict = {
                    _events.value = Event.RequestSyncConflictWarning
                    // Wait up to 30 seconds for user to respond
                    var waited = 0
                    while (!conflictAcknowledged && waited < 30_000) {
                        kotlinx.coroutines.delay(150)
                        waited += 150
                    }
                    conflictAcknowledged
                }
            )

            _uiState.value = if (syncResult.isSuccess) {
                _uiState.value.copy(
                    isLoading      = false,
                    successMessage = "Sync completed successfully",
                    lastSyncTime   = syncRepository.getLastSyncTime() ?: ""
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error     = "Sync failed: ${syncResult.exceptionOrNull()?.message}"
                )
            }
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    fun clearState() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
        _events.value  = null
        loadStatus()
    }

    fun clearEvent()   { _events.value = null }
    fun clearError()   { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(successMessage = null) }
}