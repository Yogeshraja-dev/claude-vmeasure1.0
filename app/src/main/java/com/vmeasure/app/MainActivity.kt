package com.vmeasure.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.vmeasure.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // The same view id is used in both layouts but the type differs.
        // Find the view at runtime and cast accordingly.
        val navViewRaw = binding.root.findViewById<View>(R.id.bottomNavigationView)

        when (navViewRaw) {

            // ── Phone: BottomNavigationView ───────────────────────────────────
            is BottomNavigationView -> {
                navViewRaw.setupWithNavController(navController)

                navViewRaw.setOnItemSelectedListener { item ->
                    clearTabState(navController.currentDestination?.id)
                    navController.navigate(item.itemId)
                    true
                }
                navViewRaw.setOnItemReselectedListener { /* consume */ }
            }

            // ── Tablet: NavigationRailView ────────────────────────────────────
            is NavigationRailView -> {
                navViewRaw.setupWithNavController(navController)

                navViewRaw.setOnItemSelectedListener { item ->
                    clearTabState(navController.currentDestination?.id)
                    navController.navigate(item.itemId)
                    true
                }
                navViewRaw.setOnItemReselectedListener { /* consume */ }
            }
        }

        // Hide bottom/rail nav on sub-screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addEditFragment,
                R.id.detailFragment -> hideBottomNav()
                else                -> showBottomNav()
            }
        }
    }

    // ── Clear tab state when switching tabs ───────────────────────────────────

    private fun clearTabState(leavingDestinationId: Int?) {
        // ViewModels handle their own state clearing via clearState().
        // This hook is here for any future fragment-level cleanup needed.
        when (leavingDestinationId) {
            R.id.listsFragment    -> { /* ListsViewModel.clearState() called in onDestroyView */ }
            R.id.settingsFragment -> { /* SettingsViewModel.clearState() called in onDestroyView */ }
        }
    }

    // ── Nav visibility ────────────────────────────────────────────────────────

    fun showBottomNav() {
        binding.root.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    fun hideBottomNav() {
        binding.root.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    // ── Global full-screen loader ─────────────────────────────────────────────

    fun showLoader() {
        binding.loaderOverlay.visibility = View.VISIBLE
    }

    fun hideLoader() {
        binding.loaderOverlay.visibility = View.GONE
    }

    // ── Back press ────────────────────────────────────────────────────────────

//    @Deprecated("Use OnBackPressedDispatcher in Fragments")
//    override fun onBackPressed() {
//        if (!navController.navigateUp()) {
//            @Suppress("DEPRECATION")
//            super.onBackPressed()
//        }
//    }
}