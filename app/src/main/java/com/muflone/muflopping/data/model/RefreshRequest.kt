package com.muflone.muflopping.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    val refresh: String
)
