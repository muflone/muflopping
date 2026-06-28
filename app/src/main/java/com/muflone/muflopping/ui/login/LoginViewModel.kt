package com.muflone.muflopping.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muflone.muflopping.data.model.AuthResponse
import com.muflone.muflopping.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<AuthResponse>>()
    val loginResult: LiveData<Result<AuthResponse>> = _loginResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(username, password)
            _loginResult.value = result
        }
    }

    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }
}
