package com.muflone.muflopping.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muflone.muflopping.data.model.ShoppingListDetail
import com.muflone.muflopping.data.repository.ShoppingRepository
import kotlinx.coroutines.launch

class ListDetailViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _listDetail = MutableLiveData<Result<ShoppingListDetail>>()
    val listDetail: LiveData<Result<ShoppingListDetail>> = _listDetail

    private val _operationResult = MutableLiveData<Result<Any>>()
    val operationResult: LiveData<Result<Any>> = _operationResult

    fun fetchListDetail(listId: Int) {
        viewModelScope.launch {
            val result = repository.getListDetail(listId)
            _listDetail.value = result
        }
    }

    fun updateItemInList(listId: Int, itemId: Int, quantity: String? = null, unit: String? = null, note: String? = null) {
        viewModelScope.launch {
            val result = repository.updateItemInList(listId, itemId, quantity, unit, note)
            if (result.isSuccess) {
                _operationResult.value = Result.success(Unit)
                fetchListDetail(listId)
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun toggleItemChecked(listId: Int, itemId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleItemChecked(listId, itemId, isChecked)
            if (result.isSuccess) {
                fetchListDetail(listId)
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun deleteItemFromList(listId: Int, itemId: Int) {
        viewModelScope.launch {
            val result = repository.deleteItemFromList(listId, itemId)
            if (result.isSuccess) {
                _operationResult.value = Result.success(Unit)
                fetchListDetail(listId)
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }
}
