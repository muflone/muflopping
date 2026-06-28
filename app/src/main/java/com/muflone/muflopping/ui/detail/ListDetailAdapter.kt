package com.muflone.muflopping.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.muflone.muflopping.R
import com.muflone.muflopping.data.model.Item
import com.muflone.muflopping.databinding.ItemCategoryBinding
import com.muflone.muflopping.databinding.ItemProductBinding
import com.muflone.muflopping.util.SettingsManager

sealed class DetailItem {
    data class CategoryHeader(val categoryName: String) : DetailItem()
    data class ProductItem(val item: Item) : DetailItem()
}

class ListDetailAdapter(
    private val onItemLongClick: (Item) -> Unit,
    private val onItemCheckedChange: (Item, Boolean) -> Unit
) : ListAdapter<DetailItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var settingsManager: SettingsManager? = null

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DetailItem.CategoryHeader -> TYPE_CATEGORY
            is DetailItem.ProductItem -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> {
                val binding = ItemCategoryBinding.inflate(inflater, parent, false)
                CategoryViewHolder(binding)
            }
            TYPE_PRODUCT -> {
                val binding = ItemProductBinding.inflate(inflater, parent, false)
                ProductViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (settingsManager == null) {
            settingsManager = SettingsManager(holder.itemView.context)
        }
        when (val item = getItem(position)) {
            is DetailItem.CategoryHeader -> (holder as CategoryViewHolder).bind(item.categoryName)
            is DetailItem.ProductItem -> (holder as ProductViewHolder).bind(item.item)
        }
    }

    class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryName: String) {
            binding.tvCategoryName.text = categoryName
            binding.ivDragHandle.visibility = android.view.View.GONE
        }
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.tvProductName.text = item.productName
            binding.tvQuantity.text = "${item.quantity} ${item.unitName}"
            
            if (item.isChecked) {
                binding.tvProductName.paintFlags = binding.tvProductName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvQuantity.paintFlags = binding.tvQuantity.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvProductName.paintFlags = binding.tvProductName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvQuantity.paintFlags = binding.tvQuantity.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.ivProductImage.load(settingsManager?.getFullImageUrl(item.productImage)) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            binding.cbChecked.isClickable = false
            binding.cbChecked.isFocusable = false
            binding.cbChecked.isChecked = item.isChecked

            binding.root.setOnClickListener { 
                onItemCheckedChange(item, !item.isChecked) 
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }

            if (!item.note.isNullOrEmpty()) {
                binding.tvNote.text = item.note
                binding.tvNote.visibility = android.view.View.VISIBLE
                if (item.isChecked) {
                    binding.tvNote.paintFlags = binding.tvNote.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.tvNote.paintFlags = binding.tvNote.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            } else {
                binding.tvNote.visibility = android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DetailItem>() {
        override fun areItemsTheSame(oldItem: DetailItem, newItem: DetailItem): Boolean {
            return when {
                oldItem is DetailItem.CategoryHeader && newItem is DetailItem.CategoryHeader ->
                    oldItem.categoryName == newItem.categoryName
                oldItem is DetailItem.ProductItem && newItem is DetailItem.ProductItem ->
                    oldItem.item.id == newItem.item.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DetailItem, newItem: DetailItem): Boolean {
            return oldItem == newItem
        }
    }
}
