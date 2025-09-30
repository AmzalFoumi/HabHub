package com.amzal.habhub.ui.habits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amzal.habhub.data.models.Habit
import com.amzal.habhub.databinding.ItemHabitBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for habit items with progress tracking and user interactions.
 * 
 * @param habits MutableList<Habit> list of habits to display
 * @param onHabitClick Function2<Habit, Int, Unit> callback for habit item clicks
 * @param onHabitLongClick Function2<Habit, Int, Unit> callback for long press actions
 * @see Habit
 * @see HabitsFragment
 */
class HabitsAdapter(
    private val habits: List<Habit>,                           // Data source (read-only reference)
    private val onHabitToggle: (Habit, Int) -> Unit,          // Completion toggle callback
    private val onHabitEdit: (Habit, Int) -> Unit,            // Edit button callback
    private val onHabitDelete: (Habit, Int) -> Unit           // Delete button callback
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    /**
     * ViewHolder for habit item views.
     * 
     * @param binding ItemHabitBinding view binding instance
     */
    class HabitViewHolder(private val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds habit data to item view and sets up interactions.
         * 
         * @param habit Habit data to display
         * @param position Int adapter position
         * @param onToggle Function2<Habit, Int, Unit> completion toggle callback
         * @param onEdit Function2<Habit, Int, Unit> edit button callback
         * @param onDelete Function2<Habit, Int, Unit> delete button callback
         */
        fun bind(
            habit: Habit, 
            position: Int, 
            onToggle: (Habit, Int) -> Unit,
            onEdit: (Habit, Int) -> Unit,
            onDelete: (Habit, Int) -> Unit
        ) {
            binding.tvHabitName.text = habit.name
            
            if (habit.description.isNotEmpty()) {
                binding.tvHabitDescription.text = habit.description
                binding.tvHabitDescription.visibility = View.VISIBLE
            } else {
                binding.tvHabitDescription.visibility = View.GONE
            }
            

            
            val today = getCurrentDate()
            val isCompletedToday = habit.completionDates.contains(today)
            val streakCount = calculateCurrentStreak(habit.completionDates)
            
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = isCompletedToday
            

            
            val daysSinceCreated = getDaysSinceCreated(habit.createdDate)
            val completionRate = if (daysSinceCreated > 0) {
                (habit.completionDates.size * 100) / daysSinceCreated
            } else {
                0
            }
            

            
            binding.tvStreakInfo.text = when {
                streakCount > 0 -> "🔥 $streakCount day streak • $completionRate% complete"
                habit.completionDates.isNotEmpty() -> "$completionRate% complete"
                else -> "Created ${formatDate(habit.createdDate)}"
            }
            

            
            binding.cbCompleted.setOnCheckedChangeListener { _, _ ->
                binding.root.post {
                    onToggle(habit, absoluteAdapterPosition)
                }
            }
            
            binding.btnEdit.setOnClickListener {
                onEdit(habit, absoluteAdapterPosition)
            }
            
            binding.btnDelete.setOnClickListener {
                onDelete(habit, absoluteAdapterPosition)
            }
        }
        

        
        /**
         * Returns current date in YYYY-MM-DD format.
         * 
         * @return String current date
         */
        private fun getCurrentDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        
        /**
         * Calculates consecutive completion streak for a habit.
         * 
         * @param completionDates List<String> completion dates in YYYY-MM-DD format
         * @return Int current streak count
         */
        private fun calculateCurrentStreak(completionDates: List<String>): Int {
            if (completionDates.isEmpty()) return 0
            
            val sortedDates = completionDates.sorted().reversed()
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            var streak = 0
            var currentDate = getCurrentDate()
            
            for (dateString in sortedDates) {
                if (dateString == currentDate) {
                    streak++
                    calendar.time = dateFormat.parse(currentDate) ?: return streak
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    currentDate = dateFormat.format(calendar.time)
                } else {
                    break
                }
            }
            
            return streak
        }
        
        /**
         * Calculates days since habit creation.
         * 
         * @param createdDate String creation date in yyyy-MM-dd format
         * @return Int days since creation
         */
        private fun getDaysSinceCreated(createdDate: String): Int {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            return try {
                val created = dateFormat.parse(createdDate)
                val today = Date()
                val diffInMillis = today.time - (created?.time ?: 0)
                (diffInMillis / (1000 * 60 * 60 * 24)).toInt() + 1
            } catch (e: Exception) {
                1
            }
        }
        
        /**
         * Formats date for display.
         * 
         * @param dateString String date in yyyy-MM-dd format
         * @return String formatted date
         */
        private fun formatDate(dateString: String): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            
            return try {
                val date = dateFormat.parse(dateString)
                displayFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }



    /**
     * Creates new ViewHolder instances.
     * 
     * @param parent ViewGroup parent container
     * @param viewType Int view type
     * @return HabitViewHolder new instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    /**
     * Binds data to ViewHolder.
     * 
     * @param holder HabitViewHolder to bind data to
     * @param position Int item position
     */
    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(
            habits[position],
            position,
            onHabitToggle,
            onHabitEdit,
            onHabitDelete
        )
    }

    /**
     * Returns total item count.
     * 
     * @return Int number of habits
     */
    override fun getItemCount() = habits.size
}
