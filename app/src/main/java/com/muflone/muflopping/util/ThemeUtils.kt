package com.muflone.muflopping.util

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import com.muflone.muflopping.R

object ThemeUtils {
    private var lastThemeColor: String? = null

    fun applyTheme(activity: Activity, noActionBar: Boolean = false) {
        val settingsManager = SettingsManager(activity)
        val colorName = settingsManager.getThemeColor()
        activity.setTheme(getThemeId(colorName, noActionBar))
        
        val window = activity.window
        val decorView = window.decorView
        val wic = WindowInsetsControllerCompat(window, decorView)
        
        // Icone chiare per la barra di stato
        wic.isAppearanceLightStatusBars = false
    }

    fun isThemeChanged(activity: Activity): Boolean {
        val settingsManager = SettingsManager(activity)
        return settingsManager.getThemeColor() != lastThemeColor
    }

    fun updateLastTheme(activity: Activity) {
        val settingsManager = SettingsManager(activity)
        lastThemeColor = settingsManager.getThemeColor()
    }

    private fun getThemeId(colorName: String, noActionBar: Boolean): Int {
        return when (colorName) {
            "RED" -> if (noActionBar) R.style.Theme_Muflopping_RED_NoActionBar else R.style.Theme_Muflopping_RED
            "PINK" -> if (noActionBar) R.style.Theme_Muflopping_PINK_NoActionBar else R.style.Theme_Muflopping_PINK
            "PURPLE" -> if (noActionBar) R.style.Theme_Muflopping_PURPLE_NoActionBar else R.style.Theme_Muflopping_PURPLE
            "DEEP_PURPLE" -> if (noActionBar) R.style.Theme_Muflopping_DEEP_PURPLE_NoActionBar else R.style.Theme_Muflopping_DEEP_PURPLE
            "INDIGO" -> if (noActionBar) R.style.Theme_Muflopping_INDIGO_NoActionBar else R.style.Theme_Muflopping_INDIGO
            "BLUE" -> if (noActionBar) R.style.Theme_Muflopping_BLUE_NoActionBar else R.style.Theme_Muflopping_BLUE
            "LIGHT_BLUE" -> if (noActionBar) R.style.Theme_Muflopping_LIGHT_BLUE_NoActionBar else R.style.Theme_Muflopping_LIGHT_BLUE
            "CYAN" -> if (noActionBar) R.style.Theme_Muflopping_CYAN_NoActionBar else R.style.Theme_Muflopping_CYAN
            "TEAL" -> if (noActionBar) R.style.Theme_Muflopping_TEAL_NoActionBar else R.style.Theme_Muflopping_TEAL
            "GREEN" -> if (noActionBar) R.style.Theme_Muflopping_GREEN_NoActionBar else R.style.Theme_Muflopping_GREEN
            "LIGHT_GREEN" -> if (noActionBar) R.style.Theme_Muflopping_LIGHT_GREEN_NoActionBar else R.style.Theme_Muflopping_LIGHT_GREEN
            "LIME" -> if (noActionBar) R.style.Theme_Muflopping_LIME_NoActionBar else R.style.Theme_Muflopping_LIME
            "YELLOW" -> if (noActionBar) R.style.Theme_Muflopping_YELLOW_NoActionBar else R.style.Theme_Muflopping_YELLOW
            "AMBER" -> if (noActionBar) R.style.Theme_Muflopping_AMBER_NoActionBar else R.style.Theme_Muflopping_AMBER
            "ORANGE" -> if (noActionBar) R.style.Theme_Muflopping_ORANGE_NoActionBar else R.style.Theme_Muflopping_ORANGE
            "DEEP_ORANGE" -> if (noActionBar) R.style.Theme_Muflopping_DEEP_ORANGE_NoActionBar else R.style.Theme_Muflopping_DEEP_ORANGE
            else -> if (noActionBar) R.style.Theme_Muflopping_PURPLE_NoActionBar else R.style.Theme_Muflopping_PURPLE
        }
    }
    
    fun getColorResource(colorName: String): Int {
        return when (colorName) {
            "RED" -> R.color.md_red_500
            "PINK" -> R.color.md_pink_500
            "PURPLE" -> R.color.md_purple_500
            "DEEP_PURPLE" -> R.color.md_deep_purple_500
            "INDIGO" -> R.color.md_indigo_500
            "BLUE" -> R.color.md_blue_500
            "LIGHT_BLUE" -> R.color.md_light_blue_500
            "CYAN" -> R.color.md_cyan_500
            "TEAL" -> R.color.md_teal_500
            "GREEN" -> R.color.md_green_500
            "LIGHT_GREEN" -> R.color.md_light_green_500
            "LIME" -> R.color.md_lime_500
            "YELLOW" -> R.color.md_yellow_500
            "AMBER" -> R.color.md_amber_500
            "ORANGE" -> R.color.md_orange_500
            "DEEP_ORANGE" -> R.color.md_deep_orange_500
            else -> R.color.md_purple_500
        }
    }

    fun getColorVariantResource(colorName: String): Int {
        return when (colorName) {
            "RED" -> R.color.md_red_700
            "PINK" -> R.color.md_pink_700
            "PURPLE" -> R.color.md_purple_700
            "DEEP_PURPLE" -> R.color.md_deep_purple_700
            "INDIGO" -> R.color.md_indigo_700
            "BLUE" -> R.color.md_blue_700
            "LIGHT_BLUE" -> R.color.md_light_blue_700
            "CYAN" -> R.color.md_cyan_700
            "TEAL" -> R.color.md_teal_700
            "GREEN" -> R.color.md_green_700
            "LIGHT_GREEN" -> R.color.md_light_green_700
            "LIME" -> R.color.md_lime_700
            "YELLOW" -> R.color.md_yellow_700
            "AMBER" -> R.color.md_amber_700
            "ORANGE" -> R.color.md_orange_700
            "DEEP_ORANGE" -> R.color.md_deep_orange_700
            else -> R.color.md_purple_700
        }
    }

    val COLOR_OPTIONS = listOf(
        "RED", "PINK", "PURPLE", "DEEP_PURPLE", "INDIGO", 
        "BLUE", "LIGHT_BLUE", "CYAN", "TEAL", "GREEN", 
        "LIGHT_GREEN", "LIME", "YELLOW", "AMBER", "ORANGE", "DEEP_ORANGE"
    )
}
