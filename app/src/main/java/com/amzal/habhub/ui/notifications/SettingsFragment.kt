package com.amzal.habhub.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amzal.habhub.data.SharedPreferencesManager
import com.amzal.habhub.databinding.FragmentSettingsBinding
import com.amzal.habhub.utils.NotificationHelper
import com.amzal.habhub.utils.HydrationManager
import com.amzal.habhub.worker.HydrationWorker
import java.util.concurrent.TimeUnit

/**
 * Fragment for managing notification settings and hydration reminders.
 * 
 * Provides user interface for configuring hydration reminder intervals,
 * enabling/disabling notifications, and testing notification functionality.
 * Integrates with WorkManager for reliable background notifications.
 * 
 * @see HydrationWorker
 * @see NotificationHelper
 * @see SharedPreferencesManager
 */
class SettingsFragment : Fragment() {

    /**
     * ViewBinding instance for accessing layout components.
     */
    private var _binding: FragmentSettingsBinding? = null
    
    /**
     * Safe binding accessor that's only valid between onCreateView and onDestroyView.
     */
    private val binding get() = _binding!!
    
    /**
     * SharedPreferences manager for persisting notification settings.
     */
    private lateinit var prefsManager: SharedPreferencesManager
    
    /**
     * Notification helper for testing notification functionality.
     */
    private lateinit var notificationHelper: NotificationHelper
    
    /**
     * Hydration manager for work scheduling coordination.
     */
    private lateinit var hydrationManager: HydrationManager
    
    /**
     * Modern permission request launcher for notification permissions.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, show test notification
            showTestNotification()
        } else {
            // Permission denied, show user feedback
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Notification permission is required to show hydration reminders.",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                // Open app settings for user to manually enable notifications
                val intent = android.content.Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }.show()
        }
    }

    /**
     * Creates and configures the fragment view with notification settings.
     * 
     * Sets up hydration reminder controls including enable/disable switch,
     * interval slider, and test notification functionality.
     * 
     * @param inflater LayoutInflater for creating view
     * @param container Parent ViewGroup container
     * @param savedInstanceState Saved fragment state
     * @return View configured notification settings view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        prefsManager = SharedPreferencesManager(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        hydrationManager = HydrationManager(requireContext())
        
        setupHydrationControls()
        
        return binding.root
    }

    /**
     * Configures hydration reminder controls and event handlers.
     * 
     * Sets up switch toggle, interval slider, and test button with
     * proper event handling and state persistence.
     */
    private fun setupHydrationControls() {
        // Load current settings
        binding.switchHydrationEnabled.isChecked = prefsManager.isHydrationEnabled()
        val currentInterval = prefsManager.getHydrationInterval()
        binding.sliderHydrationInterval.value = currentInterval.toFloat()
        updateIntervalDisplay(currentInterval)
        
        // Enable/Disable toggle
        binding.switchHydrationEnabled.setOnCheckedChangeListener { _, isEnabled ->
            prefsManager.setHydrationEnabled(isEnabled)
            updateIntervalDisplay(binding.sliderHydrationInterval.value.toInt())
            // Only update scheduling after user stops interacting
            updateHydrationSchedulingDelayed()
        }
        
        // Interval slider - only update when user stops dragging
        binding.sliderHydrationInterval.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                // User started dragging - don't update yet
            }
            
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                // User finished dragging - now update
                val interval = slider.value.toInt()
                prefsManager.setHydrationInterval(interval)
                updateIntervalDisplay(interval)
                updateHydrationSchedulingDelayed()
            }
        })
        
        // Also listen for value changes to update display only
        binding.sliderHydrationInterval.addOnChangeListener { _, value, _ ->
            val interval = value.toInt()
            updateIntervalDisplay(interval)
        }
        
        // Test notification button
        binding.btnTestNotification.setOnClickListener {
            testNotification()
        }
    }

    /**
     * Updates interval display text based on current slider value.
     * 
     * @param intervalMinutes Int current interval value in minutes
     */
    private fun updateIntervalDisplay(intervalMinutes: Int) {
        val enabled = prefsManager.isHydrationEnabled()
        binding.tvIntervalValue.text = if (enabled) {
            "Every $intervalMinutes minutes"
        } else {
            "Reminders disabled"
        }
    }
    
    /**
     * Tests hydration notification with permission checking.
     * 
     * Handles Android 13+ notification permissions and provides user feedback
     * for successful notification display or permission issues.
     */
    private fun testNotification() {
        // For Android 13+ (API 33+), check notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    requireContext(), 
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission using modern API
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        
        // Permission already granted or not required, show test notification
        showTestNotification()
    }
    
    /**
     * Shows the actual test notification after permission is confirmed.
     */
    private fun showTestNotification() {
        try {
            // Show test notification
            notificationHelper.showHydrationReminder()
            
            // Provide user feedback
            com.google.android.material.snackbar.Snackbar.make(
                binding.root, 
                "Test notification sent! Check your notification panel.", 
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            // Handle any errors gracefully
            com.google.android.material.snackbar.Snackbar.make(
                binding.root, 
                "Failed to send test notification. Please check notification permissions.", 
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Updates hydration scheduling with a delay to prevent frequent rescheduling.
     * 
     * Uses a handler to delay the update, canceling previous updates if
     * user continues to interact with controls.
     */
    private fun updateHydrationSchedulingDelayed() {
        // Cancel any pending updates
        view?.removeCallbacks(scheduleUpdateRunnable)
        
        // Schedule new update with delay
        view?.postDelayed(scheduleUpdateRunnable, 1000) // 1 second delay
    }
    
    /**
     * Runnable for delayed hydration scheduling updates.
     */
    private val scheduleUpdateRunnable = Runnable {
        hydrationManager.updateHydrationScheduling()
    }





    /**
     * Cleans up ViewBinding reference to prevent memory leaks.
     */
    override fun onDestroyView() {
        // Cancel any pending scheduling updates
        view?.removeCallbacks(scheduleUpdateRunnable)
        super.onDestroyView()
        _binding = null
    }
}