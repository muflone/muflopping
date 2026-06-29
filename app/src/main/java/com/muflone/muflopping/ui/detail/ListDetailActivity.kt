package com.muflone.muflopping.ui.detail

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.muflone.muflopping.R
import com.muflone.muflopping.databinding.ActivityListDetailBinding
import com.muflone.muflopping.ui.detail.fragment.ProductPickerFragment
import com.muflone.muflopping.ui.detail.fragment.ShoppingListFragment
import com.muflone.muflopping.util.ThemeUtils

class ListDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListDetailBinding
    private var listId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this, noActionBar = true)
        ThemeUtils.updateLastTheme(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.listDetailRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        listId = intent.getIntExtra(EXTRA_LIST_ID, -1)
        val listName = intent.getStringExtra(EXTRA_LIST_NAME)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = listName ?: "List Detail"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (listId == -1) {
            Toast.makeText(this, "Invalid list ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewPager()
    }

    override fun onResume() {
        super.onResume()
        if (ThemeUtils.isThemeChanged(this)) {
            recreate()
        }
    }

    private var currentTabPosition = 0

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ShoppingListFragment.newInstance(listId)
                    else -> ProductPickerFragment.newInstance(listId)
                }
            }
        }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "LIST"
                else -> "PRODUCTS"
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTabPosition = position
                invalidateOptionsMenu()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.list_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: android.view.Menu?): Boolean {
        val isProductsTab = currentTabPosition == 1
        menu?.findItem(R.id.action_view_mode)?.let { item ->
            item.isVisible = isProductsTab
            if (isProductsTab) {
                val fragment = supportFragmentManager.findFragmentByTag("f$currentTabPosition") as? ProductPickerFragment
                val isGrid = fragment?.isGridView() ?: false
                // Show LIST icon when in grid mode, and GRID icon when in list mode
                item.setIcon(if (isGrid) android.R.drawable.ic_menu_agenda else android.R.drawable.ic_menu_sort_by_size)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_view_mode -> {
                val fragment = supportFragmentManager.findFragmentByTag("f$currentTabPosition") as? ProductPickerFragment
                fragment?.toggleViewMode()
                invalidateOptionsMenu()
                true
            }
            R.id.action_reload -> {
                val fragment = supportFragmentManager.findFragmentByTag("f$currentTabPosition")
                when (fragment) {
                    is ShoppingListFragment -> fragment.reload()
                    is ProductPickerFragment -> fragment.reload()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_LIST_ID = "extra_list_id"
        const val EXTRA_LIST_NAME = "extra_list_name"
    }
}
