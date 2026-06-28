package com.muflone.muflopping.data.repository

import com.muflone.muflopping.data.api.ApiService
import com.muflone.muflopping.data.api.RetrofitClient
import com.muflone.muflopping.data.model.*
import com.muflone.muflopping.util.SettingsManager
import com.muflone.muflopping.util.TokenManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ShoppingRepository(
    private val tokenManager: TokenManager,
    private val settingsManager: SettingsManager
) {
    private fun getService(): ApiService = RetrofitClient.getService(tokenManager, settingsManager)

    suspend fun getLists(): Result<List<ShoppingList>> {
        return try {
            val response = getService().getLists()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch lists: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListDetail(listId: Int): Result<ShoppingListDetail> = coroutineScope {
        try {
            val listDeferred = async { getService().getListDetail(listId) }
            val itemsDeferred = async { getService().getListItems(listId) }

            val listResponse = listDeferred.await()
            val itemsResponse = itemsDeferred.await()

            if (listResponse.isSuccessful && listResponse.body() != null &&
                itemsResponse.isSuccessful && itemsResponse.body() != null
            ) {
                val shoppingList = listResponse.body()!!
                Result.success(
                    ShoppingListDetail(
                        id = shoppingList.id,
                        name = shoppingList.name,
                        items = itemsResponse.body()!!,
                        createdAt = shoppingList.createdAt,
                        updatedAt = shoppingList.updatedAt
                    )
                )
            } else {
                val error = if (!listResponse.isSuccessful) listResponse.code() else itemsResponse.code()
                Result.failure(Exception("Failed to fetch list detail: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createList(name: String): Result<ShoppingList> {
        return try {
            val response = getService().createList(ListRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create list: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateList(listId: Int, name: String): Result<ShoppingList> {
        return try {
            val response = getService().updateList(listId, ListRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update list: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteList(listId: Int): Result<Unit> {
        return try {
            val response = getService().deleteList(listId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete list: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = getService().getProducts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch products: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGlobalCategories(): Result<List<ProductCategory>> {
        return try {
            val response = getService().getGlobalCategories()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(name: String, categoryId: Int, imagePart: MultipartBody.Part?): Result<Product> {
        return try {
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = categoryId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val response = getService().createProduct(nameBody, categoryBody, imagePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(productId: Int, name: String?, categoryId: Int?, imagePart: MultipartBody.Part?): Result<Product> {
        return try {
            val nameBody = name?.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = categoryId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = getService().updateProduct(productId, nameBody, categoryBody, imagePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: Int): Result<Unit> {
        return try {
            val response = getService().deleteProduct(productId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addItemToList(listId: Int, productId: Int): Result<Item> {
        return try {
            val response = getService().createItem(listId, AddItemRequest(product = productId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to add item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateItemInList(listId: Int, itemId: Int, quantity: String? = null, unit: String? = null, note: String? = null): Result<Item> {
        return try {
            val response = getService().updateItemInList(listId, itemId, UpdateItemRequest(quantity, unit, note = note))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItemFromList(listId: Int, itemId: Int): Result<Unit> {
        return try {
            val response = getService().deleteItemFromList(listId, itemId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleItemChecked(listId: Int, itemId: Int, isChecked: Boolean): Result<Item> {
        return try {
            val response = getService().updateItemInList(listId, itemId, UpdateItemRequest(isChecked = isChecked))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to toggle item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
