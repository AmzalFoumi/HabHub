package com.amzal.habhub

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.amzal.habhub.databinding.ActivityMainBinding
import com.amzal.habhub.utils.HydrationManager

/**
 * Main activity for the HabHub wellness application.
 * 
 * Serves as entry point and navigation host for the app's fragments.
 * Initializes hydration reminder system and manages bottom navigation.
 * 
 * @see HydrationManager
 */
class MainActivity : AppCompatActivity() {

    /**
     * ViewBinding instance for accessing layout components.
     */
    private lateinit var binding: ActivityMainBinding
    
    /**
     * HydrationManager for initializing reminder system.
     */
    private lateinit var hydrationManager: HydrationManager

    /**
     * Creates main activity and initializes navigation and reminder systems.
     * 
     * Sets up bottom navigation with fragment navigation controller and
     * initializes hydration reminder scheduling based on user preferences.
     * 
     * @param savedInstanceState Bundle saved activity state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Setup bottom navigation with nav controller (no ActionBar needed)
        navView.setupWithNavController(navController)
        
        // Initialize hydration reminder system only on first creation
        if (savedInstanceState == null) {
            hydrationManager = HydrationManager(this)
            hydrationManager.initializeHydrationReminders()
        }
    }
}