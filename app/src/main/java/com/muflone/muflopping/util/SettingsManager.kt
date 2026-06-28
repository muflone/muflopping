package com.muflone.muflopping.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    fun saveServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString("server_url", cleanUrl).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString("server_url", "http://10.0.2.2:8000/") ?: "http://10.0.2.2:8000/"
    }

    fun getApiBaseUrl(): String {
        val url = getServerUrl()
        return if (url.endsWith("/api/")) url else "${url}api/"
    }

    fun getFullImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null

        val serverUrl = getServerUrl().removeSuffix("/")

        // If it's a full URL
        if (path.startsWith("http")) {
            // Fix for Django servers returning absolute URLs with wrong host/IP (like 127.0.0.1)
            // If the URL contains /media/, we force it to use our configured server
            val mediaIndex = path.indexOf("/media/")
            if (mediaIndex != -1) {
                val mediaPath = path.substring(mediaIndex)
                return "$serverUrl$mediaPath"
            }
            return path
        }

        // If it's a relative path, prepend server URL
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$serverUrl$cleanPath"
    }

    fun saveThemeColor(colorName: String) {
        prefs.edit().putString("theme_color", colorName).apply()
    }

    fun getThemeColor(): String {
        return prefs.getString("theme_color", "PURPLE") ?: "PURPLE"
    }
}
