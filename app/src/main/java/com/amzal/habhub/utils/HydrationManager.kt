package com.amzal.habhub.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amzal.habhub.data.SharedPreferencesManager
import com.amzal.habhub.worker.HydrationWorker
import java.util.concurrent.TimeUnit

/**
 * Utility class for managing hydration reminder WorkManager scheduling.
 * 
 * Provides centralized control over hydration reminder work scheduling
 * with proper integration to SharedPreferences settings. Handles work
 * cancellation and rescheduling based on user preferences.
 * 
 * @param context Application context for WorkManager access
 */
class HydrationManager(private val context: Context) {
    
    /**
     * SharedPreferences manager for accessing hydration settings.
     */
    private val prefsManager = SharedPreferencesManager(context)
    
    /**
     * WorkManager instance for scheduling background work.
     */
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Initializes hydration reminder scheduling based on current settings.
     * 
     * Should be called during app startup to ensure hydration reminders
     * are properly scheduled according to user preferences. Respects
     * enabled state and interval settings.
     */
    fun initializeHydrationReminders() {
        if (prefsManager.isHydrationEnabled()) {
            scheduleHydrationWork()
        } else {
            cancelHydrationWork()
        }
    }
    
    /**
     * Schedules periodic hydration reminder work.
     * 
     * Creates and enqueues periodic WorkManager task with current
     * interval settings. Uses KEEP policy to avoid rescheduling
     * existing work unless settings actually changed.
     */
    fun scheduleHydrationWork() {
        val interval = prefsManager.getHydrationInterval().toLong()
        
        // Add initial delay to prevent immediate execution
        val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(
            interval, TimeUnit.MINUTES
        ).setInitialDelay(interval, TimeUnit.MINUTES)
         .build()
        
        // Use KEEP policy to avoid unnecessary rescheduling
        workManager.enqueueUniquePeriodicWork(
            HydrationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Cancels all hydration reminder work.
     * 
     * Removes scheduled hydration notifications from WorkManager queue.
     * Used when user disables hydration reminders in settings.
     */
    fun cancelHydrationWork() {
        workManager.cancelUniqueWork(HydrationWorker.WORK_NAME)
    }
    
    /**
     * Updates hydration work scheduling based on current settings.
     * 
     * Convenience method that checks enabled state and either schedules
     * or cancels work accordingly. Should be called after settings changes.
     */
    fun updateHydrationScheduling() {
        if (prefsManager.isHydrationEnabled()) {
            // For settings changes, we need to replace existing work
            scheduleHydrationWorkWithReplace()
        } else {
            cancelHydrationWork()
        }
    }
    
    /**
     * Schedules hydration work with REPLACE policy for settings changes.
     * 
     * Used when user changes settings and we need to update the interval.
     * Still includes initial delay to prevent immediate execution.
     */
    private fun scheduleHydrationWorkWithReplace() {
        val interval = prefsManager.getHydrationInterval().toLong()
        
        val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(
            interval, TimeUnit.MINUTES
        ).setInitialDelay(interval, TimeUnit.MINUTES)
         .build()
        
        // Use REPLACE policy only when settings change
        workManager.enqueueUniquePeriodicWork(
            HydrationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}