package com.amzal.habhub.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amzal.habhub.MainActivity
import com.amzal.habhub.R

/**
 * Helper class for creating and managing hydration reminder notifications.
 * 
 * Handles notification channel creation, notification building with actions,
 * and proper permission checking for Android 13+ devices.
 * 
 * @param context Application context for accessing system services
 */
class NotificationHelper(private val context: Context) {
    
    /**
     * NotificationManager instance for system notification access.
     */
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Creates notification channel for hydration reminders on Android O+.
     * 
     * Sets up proper channel with medium importance for timely delivery
     * without being overly intrusive to user experience.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications to remind you to drink water"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Displays hydration reminder notification with drink water action.
     * 
     * Creates rich notification with app icon, motivational message,
     * and tap action to open main app. Includes proper permission
     * checking for Android 13+ notification requirements.
     */
    fun showHydrationReminder() {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                android.util.Log.w("NotificationHelper", "Notifications are not enabled for this app")
                return
            }
        }
        
        android.util.Log.d("NotificationHelper", "Attempting to show hydration reminder notification")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop_24)
            .setContentTitle("Time to Hydrate! 💧")
            .setContentText("Don't forget to drink some water and stay healthy!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Remember to drink water regularly throughout the day. Your body needs proper hydration to function at its best!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            android.util.Log.d("NotificationHelper", "Hydration reminder notification sent successfully")
        } catch (e: SecurityException) {
            // Handle notification permission denial gracefully
            android.util.Log.e("NotificationHelper", "SecurityException when showing notification: ${e.message}", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to show notification: ${e.message}", e)
        }
    }
    
    companion object {
        /**
         * Notification channel identifier for hydration reminders.
         */
        private const val CHANNEL_ID = "hydration_reminders"
        
        /**
         * Unique notification ID for hydration alerts.
         */
        private const val NOTIFICATION_ID = 1001
    }
}