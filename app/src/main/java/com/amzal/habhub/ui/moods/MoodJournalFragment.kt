package com.amzal.habhub.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.amzal.habhub.data.SharedPreferencesManager
import com.amzal.habhub.data.models.MoodEntry
import com.amzal.habhub.databinding.FragmentMoodJournalBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main UI controller for mood journal with calendar view, trend chart, and mood entries.
 * 
 * @see MoodEntry
 * @see AddMoodDialogFragment
 * @see MoodEntriesAdapter
 */
class MoodJournalFragment : Fragment() {

    private var _binding: FragmentMoodJournalBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var moodEntriesAdapter: MoodEntriesAdapter
    private val moodEntries = mutableListOf<MoodEntry>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefsManager = SharedPreferencesManager(requireContext())
        setupRecyclerView()
        setupClickListeners()
        setupCalendarView()
        loadMoodEntries()
    }
    
    /**
     * Sets up RecyclerView with mood entries adapter.
     */
    private fun setupRecyclerView() {
        moodEntriesAdapter = MoodEntriesAdapter(
            moodEntries,
            ::onMoodEdit,
            ::onMoodDelete,
            ::onMoodShare
        )
        
        binding.rvMoodEntries.apply {
            adapter = moodEntriesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    /**
     * Sets up click listeners for UI elements.
     */
    private fun setupClickListeners() {
        binding.fabAddMood.setOnClickListener {
            showAddMoodDialog()
        }
    }
    
    /**
     * Sets up calendar view for mood date selection.
     */
    private fun setupCalendarView() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            showMoodForDate(selectedDate)
        }
    }
    
    /**
     * Shows mood entries for selected date with times.
     * 
     * @param date String selected date in YYYY-MM-DD format
     */
    private fun showMoodForDate(date: String) {
        val moodsForDate = moodEntries
            .filter { it.date == date }
            .sortedBy { it.timestamp }
        
        if (moodsForDate.isNotEmpty()) {
            // Build message showing all moods for the date with times
            val formattedDate = formatDateForDisplay(date)
            val moodDetails = buildString {
                moodsForDate.forEach { mood ->
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val time = timeFormat.format(Date(mood.timestamp))
                    append("$time - ${mood.emoji} ${mood.moodName}")
                    if (mood.note.isNotBlank()) {
                        append("\n   \"${mood.note}\"")
                    }
                    append("\n\n")
                }
            }.trimEnd()
            
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Moods for $formattedDate")
                .setMessage(moodDetails)
                .setNegativeButton("Close", null)
            
            // If only one mood, allow editing
            if (moodsForDate.size == 1) {
                dialog.setPositiveButton("Edit") { _, _ ->
                    val mood = moodsForDate.first()
                    showAddMoodDialog(mood, moodEntries.indexOf(mood))
                }
            } else {
                // Multiple moods - show option to add new one
                dialog.setPositiveButton("Add New") { _, _ ->
                    showAddMoodDialog()
                }
            }
            
            dialog.show()
        } else {
            // No mood entry for this date
            val formattedDate = formatDateForDisplay(date)
            AlertDialog.Builder(requireContext())
                .setTitle("No mood entries")
                .setMessage("You haven't logged any moods for $formattedDate. Would you like to add one?")
                .setPositiveButton("Add Mood") { _, _ ->
                    showAddMoodDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    /**
     * Formats date string for user-friendly display.
     * 
     * @param dateString String date in YYYY-MM-DD format
     * @return String formatted date for display
     */
    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Displays add/edit mood dialog.
     * 
     * @param moodToEdit MoodEntry? mood to edit (null for new)
     * @param position Int adapter position (-1 for new)
     */
    private fun showAddMoodDialog(moodToEdit: MoodEntry? = null, position: Int = -1) {
        val dialog = if (moodToEdit != null) {
            AddMoodDialogFragment.newInstance(moodToEdit) { savedMood ->
                handleMoodSaved(savedMood, position)
            }
        } else {
            AddMoodDialogFragment.newInstance { savedMood ->
                handleMoodSaved(savedMood, -1)
            }
        }
        
        dialog.show(parentFragmentManager, "AddMoodDialog")
    }
    
    /**
     * Handles mood saved from dialog.
     * 
     * @param savedMood MoodEntry saved mood entry
     * @param position Int adapter position
     */
    private fun handleMoodSaved(savedMood: MoodEntry, position: Int) {
        if (position >= 0) {
            // Edit existing mood
            moodEntries[position] = savedMood
            moodEntriesAdapter.notifyItemChanged(position)
        } else {
            // Add new mood
            moodEntries.add(0, savedMood)  // Add to beginning for newest first
            moodEntriesAdapter.notifyItemInserted(0)
        }
        
        prefsManager.saveMoodEntries(moodEntries)
        updateEmptyState()
        updateMoodChart()
    }
    
    /**
     * Handles mood editing request.
     * 
     * @param mood MoodEntry mood to edit
     * @param position Int adapter position
     */
    private fun onMoodEdit(mood: MoodEntry, position: Int) {
        showAddMoodDialog(mood, position)
    }
    
    /**
     * Handles mood deletion with confirmation.
     * 
     * @param mood MoodEntry mood to delete
     * @param position Int adapter position
     */
    private fun onMoodDelete(mood: MoodEntry, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                moodEntries.removeAt(position)
                moodEntriesAdapter.notifyItemRemoved(position)
                prefsManager.saveMoodEntries(moodEntries)
                updateEmptyState()
                updateMoodChart()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Handles mood sharing via intent.
     * 
     * @param mood MoodEntry mood to share
     */
    private fun onMoodShare(mood: MoodEntry) {
        val shareText = buildString {
            append("My mood today: ${mood.emoji} ${mood.moodName}")
            if (mood.note.isNotBlank()) {
                append("\n\n\"${mood.note}\"")
            }
            append("\n\n- Shared from HabHub")
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share your mood"))
    }
    
    /**
     * Loads mood entries from storage and updates UI.
     */
    private fun loadMoodEntries() {
        moodEntries.clear()
        moodEntries.addAll(prefsManager.getMoodEntries().sortedByDescending { it.timestamp })
        moodEntriesAdapter.notifyDataSetChanged()
        updateEmptyState()
        updateMoodChart()
    }
    
    /**
     * Updates empty state visibility based on mood entries.
     */
    private fun updateEmptyState() {
        if (moodEntries.isEmpty()) {
            binding.rvMoodEntries.visibility = View.GONE
            binding.emptyStateMoods.visibility = View.VISIBLE
        } else {
            binding.rvMoodEntries.visibility = View.VISIBLE
            binding.emptyStateMoods.visibility = View.GONE
        }
    }
    
    /**
     * Updates mood trend chart with recent mood data.
     * Shows ALL mood entries from the last 7 days chronologically.
     */
    private fun updateMoodChart() {
        val chart = binding.moodChart
        
        if (moodEntries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No mood data available")
            return
        }
        
        // Get all mood entries from the last 7 days, sorted chronologically
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
        
        val recentMoods = moodEntries
            .filter { it.timestamp >= sevenDaysAgo }
            .sortedBy { it.timestamp }
        
        if (recentMoods.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No mood data for the past week")
            return
        }
        
        // Create chart entries - each mood gets its own point
        val entries = mutableListOf<Entry>()
        val chartLabels = mutableListOf<String>()
        
        recentMoods.forEachIndexed { index, mood ->
            val moodValue = getMoodValue(mood.emoji)
            entries.add(Entry(index.toFloat(), moodValue))
            chartLabels.add(formatTimestampForChart(mood.timestamp))
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Mood Trend").apply {
            color = Color.parseColor("#FF6B35")  // Primary color
            setCircleColor(Color.parseColor("#FF6B35"))
            lineWidth = 2f
            circleRadius = if (entries.size > 20) 4f else 6f // Smaller circles for more data points
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#FFEBE0")  // Primary 50
            mode = LineDataSet.Mode.CUBIC_BEZIER  // Smooth curves
            cubicIntensity = 0.15f
            setDrawCircleHole(false)
            setDrawHighlightIndicators(true) // Show indicators when touched
        }
        
        // Configure chart
        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(chartLabels)
                granularity = 1f
                setDrawGridLines(false)
                setLabelCount(minOf(chartLabels.size, 8), false) // Limit labels to prevent overcrowding
                labelRotationAngle = -45f // Rotate labels for better readability
                textSize = 10f
            }
            
            // Configure Y-axis
            axisLeft.apply {
                axisMinimum = 0.5f
                axisMaximum = 5.5f
                setLabelCount(6, true)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#666666")
            }
            
            axisRight.isEnabled = false
            
            // Animate chart
            animateX(1000)
            invalidate()
        }
    }
    
    /**
     * Checks if date is within the last week.
     * 
     * @param dateString String date in YYYY-MM-DD format
     * @return Boolean true if within last week
     */
    private fun isWithinLastWeek(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            val weekAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }.time
            
            date != null && date.after(weekAgo)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Converts emoji to numeric value for chart.
     * 
     * @param emoji String mood emoji
     * @return Float numeric mood value (1-5)
     */
    private fun getMoodValue(emoji: String): Float {
        return when (emoji) {
            "�", "�😢", "😞" -> 1f    // Very Sad
            "😟", "😔", "😕" -> 1.8f  // Worried/Down
            "😤", "😠", "😡" -> 2.2f  // Frustrated/Angry
            "😴", "😪", "🥱" -> 2.5f  // Tired/Sleepy
            "😐", "😑", "🤐" -> 3f    // Neutral/Meh
            "🤔", "🙂", "😯" -> 3.3f  // Thoughtful/Okay
            "😊", "🙂", "😄" -> 4f    // Happy
            "😌", "😇", "🥲" -> 4.2f  // Peaceful/Content
            "🥰", "😍", "💕" -> 4.5f  // Grateful/Loved
            "🤩", "😆", "🥳" -> 5f    // Excited/Amazing
            "😂", "🤣", "🎉" -> 5f    // Joyful/Celebration
            else -> 3f                // Default neutral
        }
    }
    
    /**
     * Formats date for chart display.
     * 
     * @param dateString String date in YYYY-MM-DD format
     * @return String formatted date for chart
     */
    private fun formatDateForChart(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Formats timestamp for chart display with day and time.
     * 
     * @param timestamp Long timestamp in milliseconds
     * @return String formatted timestamp for chart
     */
    private fun formatTimestampForChart(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val today = Calendar.getInstance()
            val entryDate = Calendar.getInstance().apply { time = date }
            
            val dayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            // Show different formats based on how recent the entry is
            when {
                isSameDay(today, entryDate) -> timeFormat.format(date) // Just time for today
                isWithinDays(entryDate, 7) -> "${dayFormat.format(date)}\n${timeFormat.format(date)}" // Date + time for this week
                else -> dayFormat.format(date) // Just date for older entries
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Checks if two calendars represent the same day.
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Checks if date is within specified number of days from now.
     */
    private fun isWithinDays(entryDate: Calendar, days: Int): Boolean {
        val now = Calendar.getInstance()
        val daysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return entryDate.after(daysAgo) && entryDate.before(now)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}