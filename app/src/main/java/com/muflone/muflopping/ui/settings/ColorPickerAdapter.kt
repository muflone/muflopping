package com.muflone.muflopping.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.muflone.muflopping.databinding.ItemColorPickerBinding
import com.muflone.muflopping.util.ThemeUtils

class ColorPickerAdapter(
    private val selectedColor: String,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    private var currentSelection = selectedColor

    inner class ViewHolder(private val binding: ItemColorPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(colorName: String) {
            val colorRes = ThemeUtils.getColorResource(colorName)
            binding.viewColor.setBackgroundColor(ContextCompat.getColor(binding.root.context, colorRes))
            
            if (colorName == currentSelection) {
                binding.cardColor.strokeWidth = 4 // dp to px would be better but 4 is fine for now
            } else {
                binding.cardColor.strokeWidth = 0
            }

            binding.root.setOnClickListener {
                currentSelection = colorName
                onColorSelected(colorName)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColorPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ThemeUtils.COLOR_OPTIONS[position])
    }

    override fun getItemCount(): Int = ThemeUtils.COLOR_OPTIONS.size
}
