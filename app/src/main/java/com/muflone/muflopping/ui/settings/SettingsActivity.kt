package com.muflone.muflopping.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.muflone.muflopping.databinding.ActivitySettingsBinding
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.ThemeUtils
import coil.imageLoader

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager
    private var selectedThemeColor: String = "PURPLE"

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        selectedThemeColor = settingsManager.getThemeColor()
        
        ThemeUtils.applyTheme(this, noActionBar = true)
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.etServerUrl.setText(settingsManager.getServerUrl())

        setupColorPicker()

        binding.btnSave.setOnClickListener {
            val url = binding.etServerUrl.text.toString()
            if (url.isNotBlank()) {
                settingsManager.saveServerUrl(url)
                settingsManager.saveThemeColor(selectedThemeColor)
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearCache.setOnClickListener {
            imageLoader.memoryCache?.clear()
            @OptIn(coil.annotation.ExperimentalCoilApi::class)
            imageLoader.diskCache?.clear()
            Toast.makeText(this, "Image cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupColorPicker() {
        binding.rvColors.layoutManager = GridLayoutManager(this, 4)
        binding.rvColors.adapter = ColorPickerAdapter(selectedThemeColor) { colorName ->
            if (selectedThemeColor != colorName) {
                selectedThemeColor = colorName
                settingsManager.saveThemeColor(colorName)
                recreate() // Apply theme to current activity immediately
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
