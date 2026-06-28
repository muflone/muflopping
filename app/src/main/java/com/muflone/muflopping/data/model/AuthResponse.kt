package com.muflone.muflopping.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val access: String,
    val refresh: String? = null,
    val user: User? = null
)
