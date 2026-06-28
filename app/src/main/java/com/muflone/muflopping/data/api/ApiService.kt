package com.muflone.muflopping.data.api

import com.muflone.muflopping.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("accounts/login/")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("accounts/refresh/")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthResponse>

    @GET("categories/")
    suspend fun getGlobalCategories(): Response<List<ProductCategory>>

    @GET("units/")
    suspend fun getUnits(): Response<List<ProductUnit>>

    @GET("products/")
    suspend fun getProducts(): Response<List<Product>>

    @Multipart
    @POST("products/")
    suspend fun createProduct(
        @Part("name") name: okhttp3.RequestBody,
        @Part("category") category: okhttp3.RequestBody,
        @Part("unit") unit: okhttp3.RequestBody,
        @Part image: okhttp3.MultipartBody.Part? = null
    ): Response<Product>

    @Multipart
    @PATCH("products/{productId}/")
    suspend fun updateProduct(
        @Path("productId") productId: Int,
        @Part("name") name: okhttp3.RequestBody? = null,
        @Part("category") category: okhttp3.RequestBody? = null,
        @Part("unit") unit: okhttp3.RequestBody? = null,
        @Part image: okhttp3.MultipartBody.Part? = null
    ): Response<Product>

    @DELETE("products/{productId}/")
    suspend fun deleteProduct(@Path("productId") productId: Int): Response<Unit>

    @GET("lists/")
    suspend fun getLists(): Response<List<ShoppingList>>

    @POST("lists/")
    suspend fun createList(@Body request: ListRequest): Response<ShoppingList>

    @GET("lists/{listId}/")
    suspend fun getListDetail(@Path("listId") listId: Int): Response<ShoppingList>

    @GET("lists/{listId}/items/")
    suspend fun getListItems(@Path("listId") listId: Int): Response<List<Item>>

    @PUT("lists/{listId}/")
    suspend fun updateList(@Path("listId") listId: Int, @Body request: ListRequest): Response<ShoppingList>

    @DELETE("lists/{listId}/")
    suspend fun deleteList(@Path("listId") listId: Int): Response<Unit>

    @POST("lists/{listId}/items/")
    suspend fun createItem(@Path("listId") listId: Int, @Body request: AddItemRequest): Response<Item>

    @PATCH("lists/{listId}/items/{itemId}/")
    suspend fun updateItemInList(
        @Path("listId") listId: Int,
        @Path("itemId") itemId: Int,
        @Body request: UpdateItemRequest
    ): Response<Item>

    @DELETE("lists/{listId}/items/{itemId}/")
    suspend fun deleteItemFromList(
        @Path("listId") listId: Int,
        @Path("itemId") itemId: Int
    ): Response<Unit>
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PartialItem(@com.squareup.moshi.Json(name = "is_checked") val isChecked: Boolean)
