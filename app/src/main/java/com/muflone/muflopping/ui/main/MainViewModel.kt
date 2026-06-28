package com.muflone.muflopping.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muflone.muflopping.data.model.ShoppingList
import com.muflone.muflopping.data.repository.AuthRepository
import com.muflone.muflopping.data.repository.ShoppingRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ShoppingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _shoppingLists = MutableLiveData<Result<List<ShoppingList>>>()
    val shoppingLists: LiveData<Result<List<ShoppingList>>> = _shoppingLists

    private val _operationResult = MutableLiveData<Result<Any>>()
    val operationResult: LiveData<Result<Any>> = _operationResult

    fun fetchShoppingLists() {
        viewModelScope.launch {
            val result = repository.getLists()
            _shoppingLists.value = result
        }
    }

    fun createList(name: String) {
        viewModelScope.launch {
            val result = repository.createList(name)
            if (result.isSuccess) {
                _operationResult.value = Result.success(Unit)
                fetchShoppingLists()
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun updateList(listId: Int, name: String) {
        viewModelScope.launch {
            val result = repository.updateList(listId, name)
            if (result.isSuccess) {
                _operationResult.value = Result.success(Unit)
                fetchShoppingLists()
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun deleteList(listId: Int) {
        viewModelScope.launch {
            val result = repository.deleteList(listId)
            if (result.isSuccess) {
                _operationResult.value = Result.success(Unit)
                fetchShoppingLists()
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
