package com.amzal.habhub.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.amzal.habhub.data.SharedPreferencesManager
import com.amzal.habhub.utils.NotificationHelper

/**
 * WorkManager worker for periodic hydration reminder notifications.
 * 
 * Executes background task to check hydration settings and display
 * notifications when enabled. Handles battery optimization and system
 * constraints automatically through WorkManager.
 * 
 * @param context Application context for accessing system services
 * @param params WorkManager parameters and configuration
 * @see NotificationHelper
 * @see SharedPreferencesManager
 */
class HydrationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    /**
     * SharedPreferences manager for accessing hydration settings.
     */
    private val prefsManager = SharedPreferencesManager(context)
    
    /**
     * Notification helper for creating and displaying hydration alerts.
     */
    private val notificationHelper = NotificationHelper(context)
    
    /**
     * Executes the hydration reminder work.
     * 
     * Checks if hydration reminders are enabled and displays notification
     * if configured. Returns success regardless of notification state to
     * maintain periodic scheduling.
     * 
     * @return Result.success() to continue periodic work scheduling
     */
    override fun doWork(): Result {
        return try {
            if (prefsManager.isHydrationEnabled()) {
                notificationHelper.showHydrationReminder()
            }
            Result.success()
        } catch (e: Exception) {
            // Log error but return success to maintain scheduling
            Result.success()
        }
    }
    
    companion object {
        /**
         * Unique work name for hydration reminder periodic task.
         */
        const val WORK_NAME = "hydration_reminder_work"
    }
}