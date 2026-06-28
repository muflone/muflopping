package com.muflone.muflopping.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductUnit(
    val id: Int,
    val name: String,
    val order: Int
)
