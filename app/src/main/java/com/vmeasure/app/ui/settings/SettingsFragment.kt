package com.vmeasure.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.vmeasure.app.MainActivity
import com.vmeasure.app.R
import com.vmeasure.app.databinding.FragmentSettingsBinding
import com.vmeasure.app.util.NetworkUtils
import com.vmeasure.app.util.hide
import com.vmeasure.app.util.show
import com.vmeasure.app.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupGoogleSignIn()
        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleSignInResult(result.data)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearState()
        _binding = null
    }

    // ── Google Sign-In setup ──────────────────────────────────────────────────

    private fun setupGoogleSignIn() {
        val clientId = getString(R.string.google_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .requestServerAuthCode(clientId, true)
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task    = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            val email      = account.email ?: ""
            val authCode   = account.serverAuthCode ?: ""
            val clientId   = getString(R.string.google_client_id)
            val clientSecret = getString(R.string.google_client_secret)

            viewModel.onSignInSuccess(email, authCode, clientId, clientSecret)

        } catch (e: Exception) {
            requireContext().toast("Sign in failed: ${e.message}")
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnSync.setOnClickListener {
            // Check internet first
            if (!NetworkUtils.isConnected(requireContext())) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_no_internet_title)
                    .setMessage(R.string.dialog_no_internet_message)
                    .setPositiveButton(R.string.dialog_ok, null)
                    .show()
                return@setOnClickListener
            }
            val clientId     = getString(R.string.google_client_id)
            val clientSecret = getString(R.string.google_client_secret)
            viewModel.onSyncTapped(clientId, clientSecret)
        }
    }

    // ── Observe state ─────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Full-screen loader
                    if (state.isLoading) (activity as? MainActivity)?.showLoader()
                    else (activity as? MainActivity)?.hideLoader()

                    // Account info
                    binding.tvAccountEmail.text =
                        if (state.isSignedIn && state.accountEmail.isNotBlank())
                            state.accountEmail
                        else getString(R.string.msg_sign_in_required)

                    // Last sync
                    binding.tvLastSync.text =
                        if (state.lastSyncTime.isNotBlank()) state.lastSyncTime
                        else getString(R.string.label_never_synced)

                    // Success message
                    if (state.successMessage != null) {
                        binding.tvStatusMessage.text = state.successMessage
                        binding.tvStatusMessage.setTextColor(
                            requireContext().getColor(R.color.colorPrimary)
                        )
                        binding.tvStatusMessage.show()
                        viewModel.clearSuccess()
                    }

                    // Error message
                    if (state.error != null) {
                        binding.tvStatusMessage.text = state.error
                        binding.tvStatusMessage.setTextColor(
                            requireContext().getColor(R.color.colorError)
                        )
                        binding.tvStatusMessage.show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    // ── Observe events ────────────────────────────────────────────────────────

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {

                        is SettingsViewModel.Event.RequestSignIn -> {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                            viewModel.clearEvent()
                        }

                        is SettingsViewModel.Event.RequestSyncConflictWarning -> {
                            AlertDialog.Builder(requireContext())
                                .setTitle(R.string.dialog_sync_warn_title)
                                .setMessage(R.string.dialog_sync_warn_message)
                                .setPositiveButton(R.string.dialog_sync_continue) { _, _ ->
                                    viewModel.onConflictAcknowledged()
                                }
                                .setNegativeButton(R.string.dialog_cancel, null)
                                .setCancelable(false)
                                .show()
                            viewModel.clearEvent()
                        }

                        null -> { /* no-op */ }
                    }
                }
            }
        }
    }
}