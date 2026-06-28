package com.muflone.muflopping.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muflone.muflopping.R
import com.muflone.muflopping.data.model.ShoppingList
import com.muflone.muflopping.data.repository.AuthRepository
import com.muflone.muflopping.data.repository.ShoppingRepository
import com.muflone.muflopping.databinding.ActivityMainBinding
import com.muflone.muflopping.ui.detail.ListDetailActivity
import com.muflone.muflopping.ui.login.LoginActivity
import com.muflone.muflopping.ui.settings.SettingsActivity
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.ThemeUtils
import com.muflone.muflopping.util.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val tokenManager = TokenManager(applicationContext)
                val settingsManager = SettingsManager(applicationContext)
                val repository = ShoppingRepository(tokenManager, settingsManager)
                val authRepository = AuthRepository(tokenManager, settingsManager)
                return MainViewModel(repository, authRepository) as T
            }
        }
    }

    private lateinit var adapter: ShoppingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        ThemeUtils.updateLastTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        binding.fabAddList.setOnClickListener {
            showListDialog(null)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchShoppingLists()
        }

        viewModel.fetchShoppingLists()
    }

    override fun onResume() {
        super.onResume()
        if (ThemeUtils.isThemeChanged(this)) {
            recreate()
        }
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(
            onItemClick = { list ->
                val intent = Intent(this, ListDetailActivity::class.java).apply {
                    putExtra(ListDetailActivity.EXTRA_LIST_ID, list.id)
                    putExtra(ListDetailActivity.EXTRA_LIST_NAME, list.name)
                }
                startActivity(intent)
            },
            onItemLongClick = { list ->
                showListOptionsDialog(list)
            }
        )
        binding.rvShoppingLists.adapter = adapter
    }

    private fun showListOptionsDialog(list: ShoppingList) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(this)
            .setTitle(list.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showListDialog(list)
                    1 -> viewModel.deleteList(list.id)
                }
            }
            .show()
    }

    private fun showListDialog(list: ShoppingList?) {
        val editText = EditText(this)
        editText.setText(list?.name)
        editText.setHint("List Name")

        val title = if (list == null) "New Shopping List" else "Edit List"

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    if (list == null) {
                        viewModel.createList(name)
                    } else {
                        viewModel.updateList(list.id, name)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.shoppingLists.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            if (result.isSuccess) {
                adapter.submitList(result.getOrNull())
            } else {
                Toast.makeText(this, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.operationResult.observe(this) { result ->
            if (result.isFailure) {
                Toast.makeText(this, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
