package com.amzal.habhub.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amzal.habhub.data.models.MoodEntry
import com.amzal.habhub.databinding.ItemMoodEntryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying mood entries with interactive capabilities.
 * 
 * @param moods MutableList<MoodEntry> list of mood entries
 * @param onMoodEdit Function for handling mood editing
 * @param onMoodDelete Function for handling mood deletion
 * @param onMoodShare Function for handling mood sharing
 */
class MoodEntriesAdapter(
    private val moods: MutableList<MoodEntry>,
    private val onMoodEdit: (MoodEntry, Int) -> Unit,
    private val onMoodDelete: (MoodEntry, Int) -> Unit,
    private val onMoodShare: (MoodEntry) -> Unit
) : RecyclerView.Adapter<MoodEntriesAdapter.MoodViewHolder>() {

    /**
     * ViewHolder for mood entry items.
     * 
     * @param binding ItemMoodEntryBinding view binding
     */
    class MoodViewHolder(private val binding: ItemMoodEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds mood entry data to views.
         * 
         * @param mood MoodEntry to display
         * @param onEdit Function for edit action
         * @param onDelete Function for delete action
         * @param onShare Function for share action
         */
        fun bind(
            mood: MoodEntry, 
            onEdit: (MoodEntry, Int) -> Unit,
            onDelete: (MoodEntry, Int) -> Unit,
            onShare: (MoodEntry) -> Unit
        ) {
            binding.tvMoodEmoji.text = mood.emoji
            binding.tvMoodName.text = mood.moodName
            binding.tvMoodDate.text = formatDateTime(mood.timestamp, mood.date)
            
            // Show/hide note based on content
            if (mood.note.isNotBlank()) {
                binding.tvMoodNote.text = mood.note
                binding.tvMoodNote.visibility = View.VISIBLE
            } else {
                binding.tvMoodNote.visibility = View.GONE
            }
            
            // Handle menu button click
            binding.btnMoodMenu.setOnClickListener {
                showMoodMenu(it, mood, onEdit, onDelete, onShare)
            }
        }
        
        /**
         * Formats date string for display.
         * 
         * @param dateString String date in YYYY-MM-DD format
         * @return String formatted date
         */
        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
        
        /**
         * Formats timestamp to show both date and time.
         * 
         * @param timestamp Long timestamp in milliseconds
         * @param fallbackDate String fallback date if timestamp is invalid
         * @return String formatted date and time
         */
        private fun formatDateTime(timestamp: Long, fallbackDate: String): String {
            return try {
                if (timestamp > 0) {
                    val date = Date(timestamp)
                    val today = Calendar.getInstance()
                    val entryDate = Calendar.getInstance().apply { time = date }
                    
                    when {
                        isSameDay(today, entryDate) -> {
                            // Today - show "Today at HH:mm"
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            "Today at ${timeFormat.format(date)}"
                        }
                        isYesterday(today, entryDate) -> {
                            // Yesterday - show "Yesterday at HH:mm"
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            "Yesterday at ${timeFormat.format(date)}"
                        }
                        isWithinWeek(today, entryDate) -> {
                            // This week - show "Day at HH:mm"
                            val dayTimeFormat = SimpleDateFormat("EEEE 'at' HH:mm", Locale.getDefault())
                            dayTimeFormat.format(date)
                        }
                        else -> {
                            // Older - show "MMM dd, yyyy at HH:mm"
                            val fullFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            fullFormat.format(date)
                        }
                    }
                } else {
                    formatDate(fallbackDate)
                }
            } catch (e: Exception) {
                formatDate(fallbackDate)
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
         * Checks if entry date was yesterday.
         */
        private fun isYesterday(today: Calendar, entryDate: Calendar): Boolean {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            return isSameDay(yesterday, entryDate)
        }
        
        /**
         * Checks if entry date is within this week.
         */
        private fun isWithinWeek(today: Calendar, entryDate: Calendar): Boolean {
            val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            return entryDate.after(weekAgo) && entryDate.before(today)
        }
        
        /**
         * Shows popup menu for mood actions.
         * 
         * @param view View anchor for popup
         * @param mood MoodEntry for actions
         * @param onEdit Function for edit action
         * @param onDelete Function for delete action
         * @param onShare Function for share action
         */
        private fun showMoodMenu(
            view: View,
            mood: MoodEntry,
            onEdit: (MoodEntry, Int) -> Unit,
            onDelete: (MoodEntry, Int) -> Unit,
            onShare: (MoodEntry) -> Unit
        ) {
            val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(com.amzal.habhub.R.menu.mood_entry_menu, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    com.amzal.habhub.R.id.action_edit_mood -> {
                        onEdit(mood, bindingAdapterPosition)
                        true
                    }
                    com.amzal.habhub.R.id.action_share_mood -> {
                        onShare(mood)
                        true
                    }
                    com.amzal.habhub.R.id.action_delete_mood -> {
                        onDelete(mood, bindingAdapterPosition)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val binding = ItemMoodEntryBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return MoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        holder.bind(moods[position], onMoodEdit, onMoodDelete, onMoodShare)
    }

    override fun getItemCount(): Int = moods.size
}