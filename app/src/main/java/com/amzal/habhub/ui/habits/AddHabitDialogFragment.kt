package com.amzal.habhub.ui.habits

// Android Framework Imports
import android.app.Dialog                         // Base dialog class
import android.os.Bundle                          // For fragment lifecycle data
import androidx.appcompat.app.AlertDialog        // Material Design dialog
import androidx.fragment.app.DialogFragment      // Base class for modal dialogs

// App-specific Imports
import com.amzal.habhub.data.models.Habit         // Habit data model
import com.amzal.habhub.databinding.DialogAddHabitBinding // Generated binding class

// Java/Kotlin Standard Library
import java.text.SimpleDateFormat                 // For date formatting
import java.util.*                                // For Date, Locale, UUID

/**
 * Modal dialog for adding/editing habits.
 */
class AddHabitDialogFragment : DialogFragment() {
    

    

    private var _binding: DialogAddHabitBinding? = null
    

    private val binding get() = _binding!!
    

    

    private var habitToEdit: Habit? = null
    

    private var onHabitSaved: ((Habit) -> Unit)? = null
    

    
    companion object {
        /**
         * Creates dialog instance.
         * 
         * @param habit Habit? habit to edit or null for new
         * @param onSaved Function1<Habit, Unit> save callback
         * @return AddHabitDialogFragment configured instance
         */
        fun newInstance(habit: Habit? = null, onSaved: (Habit) -> Unit): AddHabitDialogFragment {
            return AddHabitDialogFragment().apply {
                this.habitToEdit = habit
                this.onHabitSaved = onSaved
            }
        }
    }
    

    
    /**
     * Creates and configures the modal dialog.
     * 
     * @param savedInstanceState Bundle? saved dialog state
     * @return Dialog configured AlertDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddHabitBinding.inflate(layoutInflater)
        
        if (habitToEdit != null) {
            binding.tvDialogTitle.text = "Edit Habit"
            binding.etHabitName.setText(habitToEdit!!.name)
            binding.etHabitDescription.setText(habitToEdit!!.description)
            binding.btnSave.text = "Update"
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveHabit()
        }
        
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
    

    
    /**
     * Validates form and saves habit.
     */
    private fun saveHabit() {
        val name = binding.etHabitName.text.toString().trim()
        val description = binding.etHabitDescription.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.etHabitName.error = "Habit name is required"
            return
        }
        
        val habit = if (habitToEdit != null) {
            habitToEdit!!.copy(
                name = name,
                description = description
            )
        } else {
            Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                createdDate = getCurrentDate(),
                completionDates = emptyList()
            )
        }
        
        onHabitSaved?.invoke(habit)
        dismiss()
    }
    

    
    /**
     * Gets current date in yyyy-MM-dd format.
     * 
     * @return String current date
     */
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    // === LIFECYCLE METHODS ===
    
    /**
     * Cleans up view binding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}