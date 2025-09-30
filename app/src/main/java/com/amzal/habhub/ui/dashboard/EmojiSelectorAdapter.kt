package com.amzal.habhub.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amzal.habhub.databinding.ItemEmojiSelectorBinding

/**
 * Data class representing a mood option with emoji and name.
 * 
 * @param emoji String emoji character
 * @param name String mood name
 */
data class MoodOption(
    val emoji: String,
    val name: String
)

/**
 * RecyclerView adapter for emoji mood selection.
 * 
 * @param moodOptions List<MoodOption> available mood options
 * @param onMoodSelected Function callback for mood selection
 */
class EmojiSelectorAdapter(
    private val moodOptions: List<MoodOption>,
    private val onMoodSelected: (MoodOption) -> Unit
) : RecyclerView.Adapter<EmojiSelectorAdapter.EmojiViewHolder>() {

    private var selectedPosition = -1

    /**
     * ViewHolder for emoji selector items.
     * 
     * @param binding ItemEmojiSelectorBinding view binding
     */
    class EmojiViewHolder(private val binding: ItemEmojiSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds mood option data to views.
         * 
         * @param moodOption MoodOption to display
         * @param isSelected Boolean whether this item is selected
         * @param onClick Function callback for selection
         */
        fun bind(moodOption: MoodOption, isSelected: Boolean, onClick: () -> Unit) {
            binding.tvEmoji.text = moodOption.emoji
            binding.tvMoodName.text = moodOption.name
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener { onClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemEmojiSelectorBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        
        // Set proper layout params for grid items
        val layoutParams = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
        layoutParams?.let {
            binding.root.layoutParams = it
        }
        
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(
            moodOptions[position],
            position == selectedPosition
        ) {
            val previousSelected = selectedPosition
            selectedPosition = position
            
            // Notify changes for visual update
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
            
            // Trigger callback
            onMoodSelected(moodOptions[position])
        }
    }

    override fun getItemCount(): Int = moodOptions.size

    /**
     * Gets currently selected mood option.
     * 
     * @return MoodOption? selected mood or null
     */
    fun getSelectedMood(): MoodOption? {
        return if (selectedPosition != -1) moodOptions[selectedPosition] else null
    }

    /**
     * Sets selected mood by emoji.
     * 
     * @param emoji String emoji to select
     */
    fun setSelectedMood(emoji: String) {
        val index = moodOptions.indexOfFirst { it.emoji == emoji }
        if (index != -1) {
            val previousSelected = selectedPosition
            selectedPosition = index
            
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
        }
    }

    companion object {
        /**
         * Default mood options with emojis and names.
         */
        val DEFAULT_MOOD_OPTIONS = listOf(
            MoodOption("😢", "Sad"),
            MoodOption("😟", "Worried"),
            MoodOption("😐", "Neutral"),
            MoodOption("😊", "Happy"),
            MoodOption("🤩", "Excited"),
            MoodOption("😴", "Tired"),
            MoodOption("😤", "Frustrated"),
            MoodOption("🥰", "Grateful"),
            MoodOption("😌", "Peaceful"),
            MoodOption("🤔", "Thoughtful")
        )
    }
}