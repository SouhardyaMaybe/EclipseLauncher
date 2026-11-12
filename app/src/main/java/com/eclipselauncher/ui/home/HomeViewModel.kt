package com.eclipselauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eclipselauncher.model.Account
import com.eclipselauncher.model.Instance
import com.eclipselauncher.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // Account state
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount

    // Instance state
    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances

    private val _selectedInstance = MutableStateFlow<Instance?>(null)
    val selectedInstance: StateFlow<Instance?> = _selectedInstance

    // UI state
    private val _showAccountSwitcher = MutableStateFlow(false)
    val showAccountSwitcher: StateFlow<Boolean> = _showAccountSwitcher

    private val _showInstanceSwitcher = MutableStateFlow(false)
    val showInstanceSwitcher: StateFlow<Boolean> = _showInstanceSwitcher

    private val _showPlayBlockedDialog = MutableStateFlow(false)
    val showPlayBlockedDialog: StateFlow<Boolean> = _showPlayBlockedDialog

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Settings
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings

    init {
        loadMockData()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate loading delay
            kotlinx.coroutines.delay(300)
            
            // Mock accounts
            val mockAccounts = listOf(
                Account("1", "Steve", Account.AccountType.LOCAL, isSelected = true),
                Account("2", "Alex", Account.AccountType.MICROSOFT)
            )
            _accounts.value = mockAccounts
            _selectedAccount.value = mockAccounts.first()

            // Mock instances
            val mockInstances = listOf(
                Instance("1", "Vanilla 1.21.4", "1.21.4", Instance.LoaderType.VANILLA, isSelected = true),
                Instance("2", "Fabulous", "1.20.1", Instance.LoaderType.FABRIC),
                Instance("3", "Modded 1.21", "1.21", Instance.LoaderType.FORGE)
            )
            _instances.value = mockInstances
            _selectedInstance.value = mockInstances.first()

            _isLoading.value = false
        }
    }

    fun selectAccount(account: Account) {
        _accounts.value = _accounts.value.map { 
            if (it.id == account.id) it.copy(isSelected = true) else it.copy(isSelected = false)
        }
        _selectedAccount.value = account
        _showAccountSwitcher.value = false
    }

    fun selectInstance(instance: Instance) {
        _instances.value = _instances.value.map { 
            if (it.id == instance.id) it.copy(isSelected = true) else it.copy(isSelected = false)
        }
        _selectedInstance.value = instance
        _showInstanceSwitcher.value = false
    }

    fun toggleAccountSwitcher() {
        _showAccountSwitcher.value = !_showAccountSwitcher.value
    }

    fun toggleInstanceSwitcher() {
        _showInstanceSwitcher.value = !_showInstanceSwitcher.value
    }

    fun onPlayClick() {
        if (selectedAccount.value == null) {
            _showPlayBlockedDialog.value = true
        } else if (selectedInstance.value == null) {
            // Navigate to create instance
        } else {
            // Launch game
        }
    }

    fun dismissPlayBlockedDialog() {
        _showPlayBlockedDialog.value = false
    }

    fun openAccountsScreen() {
        _showAccountSwitcher.value = false
        // Navigate to full Accounts screen
    }

    fun createNewInstance() {
        _showInstanceSwitcher.value = false
        // Navigate to Create Instance screen
    }
}