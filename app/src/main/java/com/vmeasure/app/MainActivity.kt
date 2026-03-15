package com.vmeasure.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
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

    // ── Navigation setup ──────────────────────────────────────────────────────

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire bottom nav to nav controller
        binding.bottomNavigationView.setupWithNavController(navController)

        // Control bottom nav visibility — hide on sub-screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addEditFragment,
                R.id.detailFragment -> hideBottomNav()
                else -> showBottomNav()
            }
        }

        // When the user taps a bottom nav item that is already selected,
        // pop back to the start destination of that tab (standard UX)
        binding.bottomNavigationView.setOnItemReselectedListener { /* consume — no action */ }
    }

    // ── Bottom nav visibility ─────────────────────────────────────────────────

    private fun showBottomNav() {
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    private fun hideBottomNav() {
        binding.bottomNavigationView.visibility = View.GONE
    }

    // ── Global full-screen loader ─────────────────────────────────────────────
    // Call showLoader() / hideLoader() from any Fragment via (activity as MainActivity)

    fun showLoader() {
        binding.loaderOverlay.visibility = View.VISIBLE
    }

    fun hideLoader() {
        binding.loaderOverlay.visibility = View.GONE
    }

    // ── Back press handling ───────────────────────────────────────────────────

//    @Deprecated("Using OnBackPressedDispatcher is preferred in fragments; " +
//            "this override handles the activity-level fallback.")
//    override fun onBackPressed() {
//        // If the nav controller can go back, let it handle it
//        if (!navController.navigateUp()) {
//            super.onBackPressed()
//        }
//    }
}


//package com.vmeasure.app
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
////import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
////import androidx.compose.foundation.layout.fillMaxSize
////import androidx.compose.foundation.layout.padding
////import androidx.compose.material3.Scaffold
////import androidx.compose.material3.Text
////import androidx.compose.runtime.Composable
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.tooling.preview.Preview
////import com.vmeasure.app.ui.theme.VmeasureTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
////        setContent {
////            VmeasureTheme {
////                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
////                    Greeting(
////                        name = "Android",
////                        modifier = Modifier.padding(innerPadding)
////                    )
////                }
////            }
////        }
//    }
//}
//
////@Composable
////fun Greeting(name: String, modifier: Modifier = Modifier) {
////    Text(
////        text = "Hello test user $name!",
////        modifier = modifier
////    )
////}
//
////@Preview(showBackground = true)
////@Composable
////fun GreetingPreview() {
////    VmeasureTheme {
////        Greeting("Android")
////    }
////}