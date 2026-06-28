package com.muflone.muflopping.data.api

import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var currentApiBaseUrl: String? = null
    private var cachedService: ApiService? = null

    fun getService(tokenManager: TokenManager, settingsManager: SettingsManager): ApiService {
        val newBaseUrl = settingsManager.getApiBaseUrl()
        if (newBaseUrl != currentApiBaseUrl) {
            currentApiBaseUrl = newBaseUrl
            cachedService = createService(tokenManager, settingsManager)
        }
        return cachedService!!
    }

    private fun createService(tokenManager: TokenManager, settingsManager: SettingsManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(tokenManager)
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(tokenManager, createBasicService(settingsManager)))
            .build()

        return Retrofit.Builder()
            .baseUrl(settingsManager.getApiBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    private fun createBasicService(settingsManager: SettingsManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(settingsManager.getApiBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
