package com.muflone.muflopping.data.repository

import com.muflone.muflopping.data.api.ApiService
import com.muflone.muflopping.data.api.RetrofitClient
import com.muflone.muflopping.data.model.AuthRequest
import com.muflone.muflopping.data.model.AuthResponse
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.TokenManager

class AuthRepository(
    private val tokenManager: TokenManager,
    private val settingsManager: SettingsManager
) {
    private fun getService(): ApiService = RetrofitClient.getService(tokenManager, settingsManager)

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = getService().login(AuthRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.access)
                authResponse.refresh?.let { tokenManager.saveRefreshToken(it) }
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenManager.clear()
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}
