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
     * Shows mood entry for selected date.
     * 
     * @param date String selected date in YYYY-MM-DD format
     */
    private fun showMoodForDate(date: String) {
        val moodForDate = moodEntries.find { it.date == date }
        
        if (moodForDate != null) {
            // Show mood details for the date
            AlertDialog.Builder(requireContext())
                .setTitle("Mood for $date")
                .setMessage("${moodForDate.emoji} ${moodForDate.moodName}\n\n${moodForDate.note}")
                .setPositiveButton("Edit") { _, _ ->
                    showAddMoodDialog(moodForDate, moodEntries.indexOf(moodForDate))
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            // No mood entry for this date
            AlertDialog.Builder(requireContext())
                .setTitle("No mood entry")
                .setMessage("You haven't logged your mood for $date. Would you like to add one?")
                .setPositiveButton("Add Mood") { _, _ ->
                    showAddMoodDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
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
     */
    private fun updateMoodChart() {
        val chart = binding.moodChart
        
        if (moodEntries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No mood data available")
            return
        }
        
        // Get last 7 days of mood data
        val last7Days = moodEntries
            .filter { isWithinLastWeek(it.date) }
            .sortedBy { it.date }
        
        if (last7Days.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No mood data for the past week")
            return
        }
        
        // Create chart entries
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        last7Days.forEachIndexed { index, mood ->
            val moodValue = getMoodValue(mood.emoji)
            entries.add(Entry(index.toFloat(), moodValue))
            labels.add(formatDateForChart(mood.date))
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Mood Trend").apply {
            color = Color.parseColor("#FF6B35")  // Primary color
            setCircleColor(Color.parseColor("#FF6B35"))
            lineWidth = 3f
            circleRadius = 6f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#FFEBE0")  // Primary 50
        }
        
        // Configure chart
        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }
            
            // Configure Y-axis
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 5f
                setLabelCount(6, true)
                setDrawGridLines(true)
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
     * @return Float numeric mood value (0-5)
     */
    private fun getMoodValue(emoji: String): Float {
        return when (emoji) {
            "😢" -> 1f  // Sad
            "😟" -> 1.5f // Worried
            "😤" -> 2f   // Frustrated
            "😴" -> 2.5f // Tired
            "😐" -> 3f   // Neutral
            "🤔" -> 3f   // Thoughtful
            "😊" -> 4f   // Happy
            "😌" -> 4f   // Peaceful
            "🥰" -> 4.5f // Grateful
            "🤩" -> 5f   // Excited
            else -> 3f   // Default neutral
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}