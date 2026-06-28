package com.muflone.muflopping.ui.detail.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muflone.muflopping.R
import com.muflone.muflopping.data.api.RetrofitClient
import com.muflone.muflopping.data.model.Item
import com.muflone.muflopping.data.model.ShoppingListDetail
import com.muflone.muflopping.data.repository.ShoppingRepository
import com.muflone.muflopping.databinding.FragmentShoppingListBinding
import com.muflone.muflopping.ui.detail.DetailItem
import com.muflone.muflopping.ui.detail.ListDetailAdapter
import com.muflone.muflopping.ui.detail.ListDetailViewModel
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.TokenManager

class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListDetailViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext().applicationContext
                val tokenManager = TokenManager(context)
                val settingsManager = SettingsManager(context)
                val repository = ShoppingRepository(tokenManager, settingsManager)
                return ListDetailViewModel(repository) as T
            }
        }
    }

    private lateinit var adapter: ListDetailAdapter
    private var listId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listId = arguments?.getInt(ARG_LIST_ID, -1) ?: -1

        if (listId == -1) {
            Toast.makeText(context, "Invalid list ID", Toast.LENGTH_SHORT).show()
            return
        }

        setupRecyclerView()
        observeViewModel()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchListDetail(listId)
        }

        viewModel.fetchListDetail(listId)
    }

    fun reload() {
        viewModel.fetchListDetail(listId)
    }

    private fun setupRecyclerView() {
        adapter = ListDetailAdapter(
            onItemLongClick = { item ->
                showItemOptionsDialog(item)
            },
            onItemCheckedChange = { item, isChecked ->
                viewModel.toggleItemChecked(listId, item.id, isChecked)
            }
        )
        binding.rvListDetail.adapter = adapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // TYPE_PRODUCT is 1 in ListDetailAdapter
                if (adapter.getItemViewType(viewHolder.adapterPosition) == 1) {
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }
                return 0
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                if (item is DetailItem.ProductItem) {
                    viewModel.deleteItemFromList(listId, item.item.id)
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (dX > 0) { // Swiping to the right
                    // Draw red background
                    val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.RED)
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    background.draw(c)

                    // Draw delete icon
                    val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
                    icon?.let {
                        val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                        
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(android.graphics.Color.WHITE)
                        it.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvListDetail)
    }

    private fun showItemOptionsDialog(item: Item) {
        val options = arrayOf("Edit", "Remove from list")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.productName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditItemDialog(item)
                    1 -> viewModel.deleteItemFromList(listId, item.id)
                }
            }
            .show()
    }

    private fun showEditItemDialog(item: Item) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_item_edit, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val actvUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.actvUnit)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)

        etQuantity.setText(item.quantity)
        etNote.setText(item.note)
        
        val units = arrayOf("pcs", "kg", "g", "l", "ml", "pack")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        actvUnit.setAdapter(unitAdapter)
        actvUnit.setText(item.unit, false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit ${item.productName}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val quantity = etQuantity.text.toString()
                val unit = actvUnit.text.toString()
                val note = etNote.text.toString()
                if (quantity.isNotBlank()) {
                    viewModel.updateItemInList(listId, item.id, quantity, unit, note)
                }
            }
            .setNeutralButton("Remove") { _, _ ->
                viewModel.deleteItemFromList(listId, item.id)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()

        etQuantity.requestFocus()
        if (etQuantity.text.isNotEmpty()) {
            etQuantity.setSelection(etQuantity.text.length)
        }
    }

    private fun observeViewModel() {
        viewModel.listDetail.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            if (result.isSuccess) {
                val detail = result.getOrNull()
                detail?.let { updateUI(it) }
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result.isFailure) {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(detail: ShoppingListDetail) {
        val groupedItems = detail.items.filter { !it.isChecked }
            .groupBy { it.productCategory }
            .toSortedMap()

        val checkedItems = detail.items.filter { it.isChecked }
            .sortedBy { it.productName }

        val listItems = mutableListOf<DetailItem>()
        
        groupedItems.forEach { (categoryName, categoryItems) ->
            listItems.add(DetailItem.CategoryHeader(categoryName))
            categoryItems.sortedBy { it.productName }.forEach { item ->
                listItems.add(DetailItem.ProductItem(item))
            }
        }

        if (checkedItems.isNotEmpty()) {
            listItems.add(DetailItem.CategoryHeader("Completed"))
            checkedItems.forEach { item ->
                listItems.add(DetailItem.ProductItem(item))
            }
        }

        adapter.submitList(listItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LIST_ID = "arg_list_id"

        fun newInstance(listId: Int) = ShoppingListFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_LIST_ID, listId)
            }
        }
    }
}
