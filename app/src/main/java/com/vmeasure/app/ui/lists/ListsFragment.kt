package com.vmeasure.app.ui.lists

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vmeasure.app.MainActivity
import com.vmeasure.app.R
import com.vmeasure.app.databinding.FragmentListsBinding
import com.vmeasure.app.util.ShareHelper
import com.vmeasure.app.util.NetworkUtils
import com.vmeasure.app.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListsFragment : Fragment() {

    private var _binding: FragmentListsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListsViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupRecyclerView()
        setupSearch()
        setupListeners()
        observeState()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning from add/edit/detail screens
        viewModel.loadUsers(reset = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter setup ─────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = UserAdapter(
            onNameClick = { user ->
                val bundle = Bundle().apply {
                    putString("userId", user.userId)
                }
                findNavController().navigate(R.id.action_lists_to_detail, bundle)
            },
            onPinClick = { user, newValue ->
                viewModel.togglePin(user.userId, newValue)
            },
            onFavClick = { user, newValue ->
                viewModel.toggleFavourite(user.userId, newValue)
            },
            onDeleteClick = { user ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_delete_title)
                    .setMessage(R.string.dialog_delete_message)
                    .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                        viewModel.deleteUser(user.userId)
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            },
            onShareClick = { user ->
                viewModel.shareUser(user.userId)
            },
            onLongPress = { user ->
                viewModel.enterSelectionMode(user.userId)
            },
            onSelectionToggle = { user ->
                viewModel.toggleSelection(user.userId)
            }
        )
    }

    // ── RecyclerView setup ────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.layoutManager = layoutManager
        binding.rvUsers.adapter = adapter

        // Lazy load more when near bottom
        binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (total > 0 && lastVisible >= total - 10) {
                    viewModel.loadMore()
                }
            }
        })
    }

    // ── Search setup ──────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onSearchQueryChanged(s?.toString() ?: "")
            }
        })
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private fun setupListeners() {

        // FAB — navigate to Add New User screen
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_lists_to_addEdit)
        }

        // Filter button — open filter bottom sheet
        binding.btnFilter.setOnClickListener {
            FilterBottomSheet.show(
                childFragmentManager,
                viewModel.uiState.value.filter
            ) { newFilter ->
                viewModel.applyFilter(newFilter)
            }
        }

        // Selection mode — Cancel button
        binding.btnCancelSelection.setOnClickListener {
            viewModel.exitSelectionMode()
        }

        // Selection mode — Select All button
        binding.btnSelectAll.setOnClickListener {
            viewModel.selectAll()
        }

        // Selection mode — Bulk Delete button
        binding.btnBulkDelete.setOnClickListener {
            val count = viewModel.uiState.value.selectedIds.size
            if (count == 0) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_bulk_message, count))
                .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                    viewModel.bulkDelete()
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        // Selection mode — Bulk Share button
        binding.btnBulkShare.setOnClickListener {
            viewModel.bulkShare()
        }
    }

    // ── Observe UI state ──────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // ── Full-screen loader ────────────────────────────────────
                    if (state.isLoading) {
                        (activity as? MainActivity)?.showLoader()
                    } else {
                        (activity as? MainActivity)?.hideLoader()
                    }

                    // ── Load-more spinner at bottom ───────────────────────────
                    binding.progressLoadMore.visibility =
                        if (state.isLoadingMore) View.VISIBLE else View.GONE

                    // ── Error toast ───────────────────────────────────────────
                    state.error?.let {
                        requireContext().toast(it)
                        viewModel.clearError()
                    }

                    // ── Submit list to adapter ────────────────────────────────
                    adapter.submitList(state.users.toList())
                    adapter.isSelectionMode = state.isSelectionMode
                    adapter.selectedIds = state.selectedIds

                    // ── Empty state ───────────────────────────────────────────
                    binding.tvEmpty.visibility =
                        if (!state.isLoading && !state.isLoadingMore && state.users.isEmpty())
                            View.VISIBLE
                        else
                            View.GONE

                    // ── Selection mode UI switch ──────────────────────────────
                    if (state.isSelectionMode) {
                        binding.normalTopBar.visibility = View.GONE
                        binding.selectionTopBar.visibility = View.VISIBLE
                        binding.tvSelectionCount.text =
                            getString(R.string.bulk_selected, state.selectedIds.size)
                        binding.fabAdd.hide()
                    } else {
                        binding.normalTopBar.visibility = View.VISIBLE
                        binding.selectionTopBar.visibility = View.GONE
                        binding.fabAdd.show()
                    }
                }
            }
        }
    }

    // ── Observe one-time events ───────────────────────────────────────────────

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {

                        is ListsViewModel.Event.ShareText -> {
                            ShareHelper.shareText(
                                requireContext(),
                                event.text,
                                getString(R.string.share_via)
                            )
                            viewModel.clearEvent()
                        }

                        is ListsViewModel.Event.ShowMessage -> {
                            requireContext().toast(event.message)
                            viewModel.clearEvent()
                        }

                        is ListsViewModel.Event.DeleteSuccess -> {
                            // List is already refreshed by ViewModel
                            viewModel.clearEvent()
                        }

                        null -> { /* no-op */ }
                    }
                }
            }
        }
    }
}