package com.muflone.muflopping.data.api

import com.muflone.muflopping.data.model.RefreshRequest
import com.muflone.muflopping.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import kotlinx.coroutines.runBlocking

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val apiService: ApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        synchronized(this) {
            val authResponse = runBlocking {
                try {
                    apiService.refreshToken(RefreshRequest(refreshToken))
                } catch (e: Exception) {
                    null
                }
            }

            if (authResponse != null && authResponse.isSuccessful) {
                val body = authResponse.body()
                if (body != null) {
                    tokenManager.saveToken(body.access)
                    body.refresh?.let { tokenManager.saveRefreshToken(it) }
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.access}")
                        .build()
                }
            }

            tokenManager.clear()
            return null
        }
    }
}
