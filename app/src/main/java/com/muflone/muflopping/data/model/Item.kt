package com.muflone.muflopping.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Item(
    val id: Int,
    val product: Int,
    @Json(name = "product_name") val productName: String,
    @Json(name = "product_image") val productImage: String? = null,
    @Json(name = "product_category") val productCategory: String,
    val quantity: String,
    val unit: Int = 0,
    @Json(name = "unit_name") val unitName: String = "",
    @Json(name = "is_checked") val isChecked: Boolean,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class AddItemRequest(
    @Json(name = "product") val product: Int,
    val quantity: String? = null,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateItemRequest(
    val quantity: String? = null,
    @Json(name = "is_checked") val isChecked: Boolean? = null,
    val note: String? = null
)
