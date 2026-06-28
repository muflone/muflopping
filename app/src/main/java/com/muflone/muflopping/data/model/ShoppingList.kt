package com.muflone.muflopping.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShoppingList(
    val id: Int,
    val name: String,
    @Json(name = "item_count") val itemCount: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ShoppingListDetail(
    val id: Int,
    val name: String,
    val items: List<Item> = emptyList(),
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ListRequest(val name: String)
