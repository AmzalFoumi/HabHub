package com.amzal.habhub.data

import android.content.Context
import android.util.Log
import com.amzal.habhub.data.models.Habit
import com.amzal.habhub.data.models.MoodEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import com.google.gson.JsonSyntaxException

/**
 * Data persistence manager using SharedPreferences with JSON serialization.
 * 
 * @param context Context application context
 */
class SharedPreferencesManager(private val context: Context) {
    
    /**
     * SharedPreferences instance for data persistence.
     */
    private val prefs = context.applicationContext.getSharedPreferences("wellness_prefs", Context.MODE_PRIVATE)

    /**
     * Gson instance for JSON serialization.
     */
    private val gson = Gson()



    /**
     * Persists habits list to SharedPreferences as JSON.
     * 
     * @param habits List<Habit> complete list of habits to save
     * @see getHabits
     */
    fun saveHabits(habits: List<Habit>) {
        val habitsJson = gson.toJson(habits)
        prefs.edit(commit = true) { 
            putString("habits", habitsJson) 
        }
    }

    /**
     * Retrieves stored habits list from SharedPreferences.
     * 
     * @return List<Habit> stored habits or empty list if none found
     * @see saveHabits
     */
    fun getHabits(): List<Habit> {
        val habitsJson = prefs.getString("habits", null)

        if (habitsJson.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson<List<Habit>>(habitsJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e("SharedPreferencesManager", "Failed to parse habits JSON: ${e.message}", e)
            emptyList()
        }
    }



    /**
     * Saves mood entries list to SharedPreferences as JSON.
     * 
     * @param moods List<MoodEntry> complete list of mood entries to save
     * @see getMoodEntries
     */
    fun saveMoodEntries(moods: List<MoodEntry>) {
        val moodsJson = gson.toJson(moods)
        prefs.edit().putString("moods", moodsJson).apply()
    }

    /**
     * Retrieves stored mood entries.
     * 
     * @return List<MoodEntry> stored mood entries
     */
    fun getMoodEntries(): List<MoodEntry> {
        val moodsJson = prefs.getString("moods", null)
        
        return if (moodsJson != null) {
            val type = object : TypeToken<List<MoodEntry>>() {}.type
            gson.fromJson(moodsJson, type)
        } else {
            emptyList()
        }
    }



    /**
     * Sets hydration reminder interval.
     * 
     * @param minutes Int interval in minutes
     */
    fun setHydrationInterval(minutes: Int) {
        prefs.edit().putInt("hydration_interval", minutes).apply()
    }

    /**
     * Gets hydration reminder interval.
     * 
     * @return Int interval in minutes
     */
    fun getHydrationInterval(): Int {
        return prefs.getInt("hydration_interval", 60)
    }

    /**
     * Sets hydration reminder enabled state.
     * 
     * @param enabled Boolean enable state
     */
    fun setHydrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hydration_enabled", enabled).apply()
    }

    /**
     * Checks if hydration reminders are enabled.
     * 
     * @return Boolean enabled state
     */
    fun isHydrationEnabled(): Boolean {
        return prefs.getBoolean("hydration_enabled", true)
    }
}
