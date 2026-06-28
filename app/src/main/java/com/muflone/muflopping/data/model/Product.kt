package com.muflone.muflopping.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(
    val id: Int,
    val name: String,
    val category: Int,
    @Json(name = "category_name") val categoryName: String,
    val image: String? = null,
    @Json(name = "is_global") val isGlobal: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ProductCategory(
    val id: Int,
    val name: String
)
