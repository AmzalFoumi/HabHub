package com.amzal.habhub.data.models

/**
 * Immutable data class representing a wellness habit with completion tracking.
 * 
 * @property id String unique identifier for the habit
 * @property name String user-defined habit name
 * @property description String optional detailed description
 * @property isCompleted Boolean today's completion status
 * @property completionDate String most recent completion date
 * @property createdDate String ISO date when habit was created
 * @property completionDates List<String> ISO dates when habit was completed
 * @see SharedPreferencesManager
 * @see HabitsAdapter
 */
data class Habit(
    val id: String = "",                              // Unique identifier (UUID)
    val name: String = "",                            // Display name of the habit
    val description: String = "",                     // Optional description text
    val isCompleted: Boolean = false,                 // Today's completion status
    val completionDate: String = "",                  // Last completion date (YYYY-MM-DD)
    val createdDate: String = "",                     // Creation date (YYYY-MM-DD)
    val completionDates: List<String> = emptyList()   // All completion dates for progress tracking
)
