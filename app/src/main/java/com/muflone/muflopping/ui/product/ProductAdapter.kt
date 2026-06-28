package com.muflone.muflopping.ui.product

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.muflone.muflopping.R
import com.muflone.muflopping.data.model.Product
import com.muflone.muflopping.databinding.ItemCategoryGridBinding
import com.muflone.muflopping.databinding.ItemProductGridBinding
import com.muflone.muflopping.util.SettingsManager

sealed class ProductListItem {
    data class CategoryItem(val categoryName: String) : ProductListItem()
    data class ProductItem(val product: Product) : ProductListItem()
}

class ProductAdapter(
    private val onCategoryClick: (String) -> Unit,
    private val onProductClick: (Product) -> Unit,
    private val onProductLongClick: (Product) -> Unit
) : ListAdapter<ProductListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var settingsManager: SettingsManager? = null
    private var addedProductIds: MutableSet<Int> = mutableSetOf()
    private var isGridView: Boolean = false

    fun setViewMode(isGrid: Boolean) {
        isGridView = isGrid
        notifyDataSetChanged()
    }

    fun setInitialAddedProducts(ids: List<Int>) {
        addedProductIds.clear()
        addedProductIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun markAsAdded(productId: Int) {
        addedProductIds.add(productId)
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_CATEGORY_GRID = 0
        private const val TYPE_PRODUCT_GRID = 1
        private const val TYPE_CATEGORY_LIST = 2
        private const val TYPE_PRODUCT_LIST = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductListItem.CategoryItem -> if (isGridView) TYPE_CATEGORY_GRID else TYPE_CATEGORY_LIST
            is ProductListItem.ProductItem -> if (isGridView) TYPE_PRODUCT_GRID else TYPE_PRODUCT_LIST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY_GRID -> {
                val binding = ItemCategoryGridBinding.inflate(inflater, parent, false)
                CategoryGridViewHolder(binding)
            }
            TYPE_PRODUCT_GRID -> {
                val binding = ItemProductGridBinding.inflate(inflater, parent, false)
                ProductGridViewHolder(binding)
            }
            TYPE_CATEGORY_LIST -> {
                val binding = com.muflone.muflopping.databinding.ItemCategoryBinding.inflate(inflater, parent, false)
                CategoryListViewHolder(binding)
            }
            TYPE_PRODUCT_LIST -> {
                val binding = com.muflone.muflopping.databinding.ItemProductPickBinding.inflate(inflater, parent, false)
                ProductListViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (settingsManager == null) {
            settingsManager = SettingsManager(holder.itemView.context)
        }
        val item = getItem(position)
        when {
            holder is CategoryGridViewHolder && item is ProductListItem.CategoryItem -> holder.bind(item.categoryName)
            holder is ProductGridViewHolder && item is ProductListItem.ProductItem -> holder.bind(item.product)
            holder is CategoryListViewHolder && item is ProductListItem.CategoryItem -> holder.bind(item.categoryName)
            holder is ProductListViewHolder && item is ProductListItem.ProductItem -> holder.bind(item.product)
        }
    }

    inner class CategoryGridViewHolder(private val binding: ItemCategoryGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryName: String) {
            binding.tvCategoryName.text = categoryName
            binding.root.setOnClickListener { onCategoryClick(categoryName) }
        }
    }

    inner class ProductGridViewHolder(private val binding: ItemProductGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            
            val isAdded = addedProductIds.contains(product.id)
            binding.ivCheckOverlay.visibility = if (isAdded) android.view.View.VISIBLE else android.view.View.GONE
            
            binding.root.setOnClickListener { onProductClick(product) }

            binding.ivProductImage.load(settingsManager?.getFullImageUrl(product.image)) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            binding.root.setOnLongClickListener {
                onProductLongClick(product)
                true
            }
        }
    }

    inner class CategoryListViewHolder(private val binding: com.muflone.muflopping.databinding.ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryName: String) {
            binding.tvCategoryName.text = categoryName
            binding.ivDragHandle.visibility = android.view.View.GONE
            binding.root.setOnClickListener { onCategoryClick(categoryName) }
        }
    }

    inner class ProductListViewHolder(private val binding: com.muflone.muflopping.databinding.ItemProductPickBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            
            val isAdded = addedProductIds.contains(product.id)
            if (isAdded) {
                binding.ivAdd.setImageResource(R.drawable.ic_check)
            } else {
                binding.ivAdd.setImageResource(android.R.drawable.ic_input_add)
            }
            binding.root.setOnClickListener { onProductClick(product) }

            binding.ivProductImage.load(settingsManager?.getFullImageUrl(product.image)) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }

            binding.root.setOnLongClickListener {
                onProductLongClick(product)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
        override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return when {
                oldItem is ProductListItem.CategoryItem && newItem is ProductListItem.CategoryItem ->
                    oldItem.categoryName == newItem.categoryName
                oldItem is ProductListItem.ProductItem && newItem is ProductListItem.ProductItem ->
                    oldItem.product.id == newItem.product.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean {
            return oldItem == newItem
        }
    }
}
