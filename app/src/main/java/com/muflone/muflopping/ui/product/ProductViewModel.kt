package com.muflone.muflopping.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muflone.muflopping.data.model.Item
import com.muflone.muflopping.data.model.Product
import com.muflone.muflopping.data.model.ProductCategory
import com.muflone.muflopping.data.repository.ShoppingRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ProductViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _products = MutableLiveData<Result<List<Product>>>()
    val products: LiveData<Result<List<Product>>> = _products

    private val _categories = MutableLiveData<Result<List<ProductCategory>>>()
    val categories: LiveData<Result<List<ProductCategory>>> = _categories

    private val _itemAdded = MutableLiveData<Result<Item>>()
    val itemAdded: LiveData<Result<Item>> = _itemAdded

    private val _productOperationResult = MutableLiveData<Result<Any>>()
    val productOperationResult: LiveData<Result<Any>> = _productOperationResult

    fun fetchProducts() {
        viewModelScope.launch {
            val result = repository.getProducts()
            _products.value = result
        }
    }

    fun fetchGlobalCategories() {
        viewModelScope.launch {
            val result = repository.getGlobalCategories()
            _categories.value = result
        }
    }

    fun addProductToList(listId: Int, productId: Int) {
        viewModelScope.launch {
            val result = repository.addItemToList(listId, productId)
            _itemAdded.value = result
        }
    }

    fun createProduct(name: String, categoryId: Int, imagePart: MultipartBody.Part?) {
        viewModelScope.launch {
            val result = repository.createProduct(name, categoryId, imagePart)
            if (result.isSuccess) {
                _productOperationResult.value = Result.success(Unit)
                fetchProducts()
            } else {
                _productOperationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun updateProduct(productId: Int, name: String?, categoryId: Int?, imagePart: MultipartBody.Part?) {
        viewModelScope.launch {
            val result = repository.updateProduct(productId, name, categoryId, imagePart)
            if (result.isSuccess) {
                _productOperationResult.value = Result.success(Unit)
                fetchProducts()
            } else {
                _productOperationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            val result = repository.deleteProduct(productId)
            if (result.isSuccess) {
                _productOperationResult.value = Result.success(Unit)
                fetchProducts()
            } else {
                _productOperationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }
}
