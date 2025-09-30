package com.amzal.habhub.ui.dashboard

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amzal.habhub.data.models.MoodEntry
import com.amzal.habhub.databinding.DialogAddMoodBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog fragment for adding and editing mood entries.
 * 
 * @see MoodEntry
 * @see EmojiSelectorAdapter
 */
class AddMoodDialogFragment : DialogFragment() {

    private var _binding: DialogAddMoodBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var emojiAdapter: EmojiSelectorAdapter
    private var onMoodSavedListener: ((MoodEntry) -> Unit)? = null
    private var moodToEdit: MoodEntry? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Make dialog wider on all screen sizes
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupEmojiSelector()
        setupClickListeners()
        
        // Configure for editing if mood provided
        moodToEdit?.let { mood ->
            binding.tvDialogTitle.text = "Update your mood"
            binding.etMoodNote.setText(mood.note)
            emojiAdapter.setSelectedMood(mood.emoji)
            updateSelectedMoodDisplay(MoodOption(mood.emoji, mood.moodName))
        }
    }
    
    /**
     * Sets up emoji selector RecyclerView.
     */
    private fun setupEmojiSelector() {
        emojiAdapter = EmojiSelectorAdapter(EmojiSelectorAdapter.DEFAULT_MOOD_OPTIONS) { selectedMood ->
            updateSelectedMoodDisplay(selectedMood)
            validateForm()
        }
        
        binding.rvEmojiSelector.apply {
            adapter = emojiAdapter
            // Use fewer columns for better readability on narrow screens
            layoutManager = GridLayoutManager(requireContext(), 3)
            
            // Add spacing between grid items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.set(4, 4, 4, 4) // 4dp spacing on all sides
                }
            })
        }
    }
    
    /**
     * Updates selected mood display section.
     * 
     * @param moodOption MoodOption selected mood
     */
    private fun updateSelectedMoodDisplay(moodOption: MoodOption) {
        binding.tvSelectedEmoji.text = moodOption.emoji
        binding.tvSelectedMoodName.text = moodOption.name
        binding.layoutSelectedMood.visibility = View.VISIBLE
    }
    
    /**
     * Sets up click listeners for buttons.
     */
    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSaveMood.setOnClickListener {
            saveMoodEntry()
        }
        
        // Enable save button when text changes
        binding.etMoodNote.setOnFocusChangeListener { _, _ ->
            validateForm()
        }
    }
    
    /**
     * Validates form and enables/disables save button.
     */
    private fun validateForm() {
        val hasSelectedMood = emojiAdapter.getSelectedMood() != null
        binding.btnSaveMood.isEnabled = hasSelectedMood
    }
    
    /**
     * Saves mood entry and triggers callback.
     */
    private fun saveMoodEntry() {
        val selectedMood = emojiAdapter.getSelectedMood() ?: return
        val note = binding.etMoodNote.text?.toString()?.trim() ?: ""
        val currentDate = getCurrentDate()
        
        val moodEntry = MoodEntry(
            id = moodToEdit?.id ?: UUID.randomUUID().toString(),
            emoji = selectedMood.emoji,
            moodName = selectedMood.name,
            note = note,
            date = currentDate,
            timestamp = System.currentTimeMillis()
        )
        
        onMoodSavedListener?.invoke(moodEntry)
        dismiss()
    }
    
    /**
     * Gets current date in YYYY-MM-DD format.
     * 
     * @return String current date
     */
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Creates new instance for adding mood.
         * 
         * @param onMoodSaved Function callback for mood saved
         * @return AddMoodDialogFragment new instance
         */
        fun newInstance(onMoodSaved: (MoodEntry) -> Unit): AddMoodDialogFragment {
            return AddMoodDialogFragment().apply {
                onMoodSavedListener = onMoodSaved
            }
        }
        
        /**
         * Creates new instance for editing mood.
         * 
         * @param moodToEdit MoodEntry mood to edit
         * @param onMoodSaved Function callback for mood saved
         * @return AddMoodDialogFragment new instance
         */
        fun newInstance(moodToEdit: MoodEntry, onMoodSaved: (MoodEntry) -> Unit): AddMoodDialogFragment {
            return AddMoodDialogFragment().apply {
                this.moodToEdit = moodToEdit
                onMoodSavedListener = onMoodSaved
            }
        }
    }
}