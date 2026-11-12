package com.eclipselauncher.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eclipselauncher.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountsViewModel : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _showDeleteDialog = MutableStateFlow<Account?>(null)
    val showDeleteDialog: StateFlow<Account?> = _showDeleteDialog

    // Sign-in state
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    sealed interface SignInState {
        object Idle : SignInState
        object Loading : SignInState
        object Progress : SignInState
        data class Failure(val message: String) : SignInState
    }

    // Sub-screen states
    private val _showCreateLocalDialog = MutableStateFlow(false)
    val showCreateLocalDialog: StateFlow<Boolean> = _showCreateLocalDialog

    private val _showSkinAndCape = MutableStateFlow(false)
    val showSkinAndCape: StateFlow<Boolean> = _showSkinAndCape

    init {
        loadMockAccounts()
    }

    private fun loadMockAccounts() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            val mockAccounts = listOf(
                Account("1", "Steve", Account.AccountType.LOCAL, isSelected = true),
                Account("2", "Alex", Account.AccountType.MICROSOFT)
            )
            _accounts.value = mockAccounts
        }
    }

    fun selectAccount(account: Account) {
        _accounts.value = _accounts.value.map { 
            if (it.id == account.id) it.copy(isSelected = true) else it.copy(isSelected = false)
        }
    }

    fun showDeleteConfirmation(account: Account) {
        _showDeleteDialog.value = account
    }

    fun confirmDelete() {
        val account = _showDeleteDialog.value ?: return
        _accounts.value = _accounts.value.filter { it.id != account.id }
        _showDeleteDialog.value = null
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }

    // Sign-in methods
    fun startMicrosoftSignIn() {
        _signInState.value = SignInState.Loading
        // Simulate loading the WebView page
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _signInState.value = SignInState.Progress
            // Simulate progress through stages
            kotlinx.coroutines.delay(1500)
            // Simulate failure for demo
            _signInState.value = SignInState.Failure("Network error during sign-in")
        }
    }

    fun retrySignIn() {
        startMicrosoftSignIn()
    }

    // Create local account
    fun showCreateLocalAccountDialog() {
        _showCreateLocalDialog.value = true
    }

    fun dismissCreateLocalDialog() {
        _showCreateLocalDialog.value = false
    }

    fun createLocalAccount(username: String) {
        val newAccount = Account(
            id = "${System.currentTimeMillis()}",
            username = username,
            type = Account.AccountType.LOCAL,
            isSelected = _accounts.value.isEmpty()
        )
        _accounts.value = _accounts.value.map { it.copy(isSelected = false) } + newAccount
        _showCreateLocalDialog.value = false
    }

    // Skin and cape
    fun showSkinAndCape() {
        _showSkinAndCape.value = true
    }

    fun dismissSkinAndCape() {
        _showSkinAndCape.value = false
    }
}