package com.amzal.habhub.data.models

data class MoodEntry(
    val id: String = "",
    val emoji: String = "",
    val moodName: String = "",
    val note: String = "",
    val date: String = "",
    val timestamp: Long = 0L
)
