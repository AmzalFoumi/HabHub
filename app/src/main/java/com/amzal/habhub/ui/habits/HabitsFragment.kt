package com.amzal.habhub.ui.habits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.amzal.habhub.data.SharedPreferencesManager
import com.amzal.habhub.data.models.Habit
import com.amzal.habhub.databinding.FragmentHabitsBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main UI controller for habit management with CRUD operations and progress tracking.
 * 
 * @see HabitsAdapter
 * @see AddHabitDialogFragment
 * @see SharedPreferencesManager
 */
class HabitsFragment : Fragment() {
    

    

    private var _binding: FragmentHabitsBinding? = null
    

    private val binding get() = _binding!!


    

    private lateinit var habitsAdapter: HabitsAdapter
    

    private lateinit var prefsManager: SharedPreferencesManager
    

    private val habits = mutableListOf<Habit>()



    /**
     * Creates fragment view.
     * 
     * @param inflater LayoutInflater to inflate views
     * @param container ViewGroup? parent container
     * @param savedInstanceState Bundle? saved state
     * @return View root view
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configures view after creation.
     * 
     * @param view View root view
     * @param savedInstanceState Bundle? saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsManager = SharedPreferencesManager(requireContext())
        setupRecyclerView()
        setupClickListeners()
        loadHabits()
    }



    /**
     * Sets up RecyclerView with adapter.
     */
    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter(
            habits,
            ::onHabitToggle,
            ::onHabitEdit,
            ::onHabitDelete
        )

        binding.rvHabits.apply {
            adapter = habitsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Sets up click listeners.
     */
    private fun setupClickListeners() {
        binding.fabAddHabit.setOnClickListener {
            showAddHabitDialog()
        }
    }



    /**
     * Handles habit completion toggle.
     * 
     * @param habit Habit being toggled
     * @param position Int adapter position
     */
    private fun onHabitToggle(habit: Habit, position: Int) {
        // Get today's date in standardized format
        val today = getCurrentDate()
        
        // Create mutable copy of completion dates for modification
        val updatedCompletionDates = habit.completionDates.toMutableList()
        
        // Toggle completion status for today
        if (updatedCompletionDates.contains(today)) {
            // Habit was completed today → mark as incomplete
            updatedCompletionDates.remove(today)
        } else {
            // Habit was not completed today → mark as complete
            updatedCompletionDates.add(today)
        }
        
        // Create updated habit with new completion data
        val updatedHabit = habit.copy(
            completionDates = updatedCompletionDates,                                    // Updated history
            isCompleted = updatedCompletionDates.contains(today),                        // Boolean for today
            completionDate = if (updatedCompletionDates.contains(today)) today else ""  // Most recent date
        )
        
        // Update the data source and notify UI
        habits[position] = updatedHabit                          // Update local list
        habitsAdapter.notifyItemChanged(position)                // Refresh specific item view
        prefsManager.saveHabits(habits)                         // Persist to storage
        updateProgress()                                         // Refresh progress indicators
    }

    /**
     * Handles habit editing.
     * 
     * @param habit Habit to edit
     * @param position Int adapter position
     */
    private fun onHabitEdit(habit: Habit, position: Int) {
        showAddHabitDialog(habit, position)
    }

    /**
     * Handles habit deletion with confirmation.
     * 
     * @param habit Habit to delete
     * @param position Int adapter position
     */
    private fun onHabitDelete(habit: Habit, position: Int) {
        // Create and show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")                                              // Dialog title
            .setMessage("Are you sure you want to delete \"${habit.name}\"? This action cannot be undone.") // Warning message
            .setPositiveButton("Delete") { _, _ ->                                 // Confirm deletion
                // User confirmed deletion - proceed with removal
                habits.removeAt(position)                                          // Remove from data source
                habitsAdapter.notifyItemRemoved(position)                          // Animate removal in UI
                
                // Update remaining item positions to prevent index mismatches
                habitsAdapter.notifyItemRangeChanged(position, habits.size)
                
                // Persist changes and update UI state
                prefsManager.saveHabits(habits)                                   // Save to storage
                updateProgress()                                                   // Refresh progress display
                updateEmptyState()                                                 // Show empty state if needed
            }
            .setNegativeButton("Cancel", null)                                    // Cancel - no action needed
            .show()                                                                // Display the dialog
    }



    /**
     * Displays the add/edit habit dialog with appropriate configuration.
     * 
     * This method serves dual purposes:
     * - Adding new habits (when habitToEdit is null)
     * - Editing existing habits (when habitToEdit is provided)
     * 
     * The dialog uses a callback pattern to communicate results back to the fragment,
     * maintaining loose coupling between the dialog and fragment components.
     * 
     * @param habitToEdit The habit to edit (null for adding new habit)
     * @param position The adapter position (used for efficient updates, -1 for new habits)
     */
    private fun showAddHabitDialog(habitToEdit: Habit? = null, position: Int = -1) {
        // Create dialog instance with factory method and result callback
        val dialog = AddHabitDialogFragment.newInstance(habitToEdit) { savedHabit ->
            // This callback executes when user saves the habit in the dialog
            
            if (habitToEdit != null && position >= 0) {
                // EDIT MODE: Update existing habit in the list
                habits[position] = savedHabit                      // Replace habit data
                habitsAdapter.notifyItemChanged(position)          // Refresh specific item view
            } else {
                // ADD MODE: Insert new habit at end of list
                habits.add(savedHabit)                            // Add to data source
                habitsAdapter.notifyItemInserted(habits.size - 1) // Animate insertion
            }
            
            // Update persistent storage and UI state
            prefsManager.saveHabits(habits)                       // Save to SharedPreferences
            updateProgress()                                       // Refresh progress indicators
            updateEmptyState()                                     // Hide empty state if needed
        }
        
        // Show dialog using fragment manager with unique tag
        dialog.show(parentFragmentManager, "AddHabitDialog")
    }



    /**
     * Loads all habits from persistent storage and refreshes the UI.
     * 
     * This method is called during fragment initialization and whenever
     * we need to refresh the entire habits list from storage. It:
     * 1. Clears the current habits list
     * 2. Loads fresh data from SharedPreferences
     * 3. Notifies the adapter of the complete data change
     * 4. Updates all UI indicators
     * 
     * Uses notifyDataSetChanged() because we're replacing the entire dataset.
     */
    private fun loadHabits() {
        // Clear current data to prevent duplicates
        habits.clear()
        
        // Load fresh data from persistent storage
        habits.addAll(prefsManager.getHabits())
        
        // Notify adapter that entire dataset changed
        habitsAdapter.notifyDataSetChanged()
        
        // Update UI indicators based on loaded data
        updateProgress()     // Calculate and display progress statistics
        updateEmptyState()   // Show/hide empty state based on data presence
    }



    /**
     * Updates the progress display with today's completion statistics.
     * 
     * This method calculates and displays real-time progress information:
     * - Counts habits completed today by checking completion dates
     * - Calculates completion percentage
     * - Updates both text display and visual progress bar
     * 
     * The progress calculation is safe against division by zero and
     * provides immediate feedback to users about their daily progress.
     */
    private fun updateProgress() {
        // Get today's date for comparison
        val today = getCurrentDate()
        
        // Count habits completed today by checking their completion dates list
        val completedToday = habits.count { habit -> 
            habit.completionDates.contains(today) 
        }
        
        // Get total number of habits
        val totalHabits = habits.size
        
        // Calculate percentage with zero-division protection
        val percentage = if (totalHabits > 0) {
            (completedToday * 100) / totalHabits  // Integer division for percentage
        } else {
            0  // No habits = 0% progress
        }
        
        // Update text display with formatted progress information
        binding.tvProgress.text = "Today's Progress: $percentage% ($completedToday/$totalHabits)"
        
        // Update visual progress bar
        binding.progressBar.progress = percentage
    }

    /**
     * Shows or hides the empty state based on whether habits exist.
     * 
     * This method provides better UX by:
     * - Showing helpful guidance when no habits exist
     * - Hiding empty state and showing habit list when data is present
     * - Using proper view visibility management
     * 
     * The empty state includes encouraging text and instructions to help
     * users understand how to get started with the app.
     */
    private fun updateEmptyState() {
        if (habits.isEmpty()) {
            // No habits exist - show empty state with helpful message
            binding.rvHabits.visibility = View.GONE        // Hide habit list
            binding.emptyState.visibility = View.VISIBLE   // Show empty state
        } else {
            // Habits exist - show the habit list
            binding.rvHabits.visibility = View.VISIBLE     // Show habit list
            binding.emptyState.visibility = View.GONE      // Hide empty state
        }
    }



    /**
     * Returns the current date in standardized YYYY-MM-DD format.
     * 
     * This method provides consistent date formatting throughout the app:
     * - Uses ISO 8601 date format (YYYY-MM-DD)
     * - Uses default locale for proper internationalization
     * - Creates new Date() for current system time
     * 
     * This format is ideal for:
     * - String-based date comparisons
     * - Consistent storage in SharedPreferences
     * - Easy parsing and sorting
     * 
     * @return Current date as formatted string (e.g., "2025-09-30")
     */
    private fun getCurrentDate(): String {
        // Create date formatter with ISO 8601 format and default locale
        // Use default locale to respect user's regional settings
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }



    /**
     * Called when the fragment's view is being destroyed.
     * 
     * This method handles proper cleanup to prevent memory leaks:
     * - Sets ViewBinding reference to null
     * - Allows garbage collector to reclaim view memory
     * - Prevents crashes from accessing destroyed views
     * 
     * This is a critical part of proper Android memory management.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clear binding reference to prevent memory leaks
        _binding = null
    }
}
