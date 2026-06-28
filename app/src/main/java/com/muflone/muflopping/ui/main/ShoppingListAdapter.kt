package com.muflone.muflopping.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.muflone.muflopping.data.model.ShoppingList
import com.muflone.muflopping.databinding.ItemShoppingListBinding

class ShoppingListAdapter(
    private val onItemClick: (ShoppingList) -> Unit,
    private val onItemLongClick: (ShoppingList) -> Unit
) : ListAdapter<ShoppingList, ShoppingListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemShoppingListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(list: ShoppingList) {
            binding.tvListName.text = list.name
            binding.tvItemCount.text = "${list.itemCount} items"
            
            val rawDate = if (list.updatedAt.isNotEmpty()) list.updatedAt else list.createdAt
            if (rawDate.length >= 16) {
                val datePart = rawDate.substring(0, 10)
                val timePart = rawDate.substring(11, 16)
                binding.tvCreatedAt.text = "Updated: $datePart $timePart"
            } else if (rawDate.length >= 10) {
                binding.tvCreatedAt.text = "Updated: ${rawDate.take(10)}"
            } else {
                binding.tvCreatedAt.text = ""
            }

            binding.root.setOnClickListener { onItemClick(list) }
            binding.root.setOnLongClickListener {
                onItemLongClick(list)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ShoppingList>() {
        override fun areItemsTheSame(oldItem: ShoppingList, newItem: ShoppingList): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingList, newItem: ShoppingList): Boolean {
            return oldItem == newItem
        }
    }
}
