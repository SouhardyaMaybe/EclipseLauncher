package com.eclipselauncher.ui.managecontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eclipselauncher.model.ModInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageContentViewModel : ViewModel() {

    enum class ContentTab { INSTALLED, BROWSE }
    enum class ContentType { MODS, RESOURCE_PACKS, DATA_PACKS, SHADERS, WORLDS }
    enum class Provider { MODRINTH, CURSEFORGE }

    private val _currentTab = MutableStateFlow(ContentTab.INSTALLED)
    val currentTab: StateFlow<ContentTab> = _currentTab

    private val _installedType = MutableStateFlow(ContentType.MODS)
    val installedType: StateFlow<ContentType> = _installedType

    private val _browseType = MutableStateFlow(ContentType.MODS)
    val browseType: StateFlow<ContentType> = _browseType

    private val _browseProvider = MutableStateFlow(Provider.MODRINTH)
    val browseProvider: StateFlow<Provider> = _browseProvider

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchResults = MutableStateFlow<List<ModInfo>>(emptyList())
    val searchResults: StateFlow<List<ModInfo>> = _searchResults

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    // Mock installed content
    private val _installedMods = MutableStateFlow<List<ModInfo>>(listOf(
        ModInfo("sodium", "Sodium", "jellysquid3", 12400000, "Modern rendering engine optimization", "", listOf("optimization"), listOf("1.21.4")),
        ModInfo("iris", "Iris Shaders", "coderbot", 8100000, "Beautiful shader support", "", listOf("shaders"), listOf("1.21.4")),
        ModInfo("fabric-api", "Fabric API", "modmuss50", 21700000, "Essential hooks for mods", "", listOf("library"), listOf("1.21.4"))
    ))
    val installedMods: StateFlow<List<ModInfo>> = _installedMods

    private val _installedResourcePacks = MutableStateFlow<List<ModInfo>>(emptyList())
    val installedResourcePacks: StateFlow<List<ModInfo>> = _installedResourcePacks

    private val _installedDataPacks = MutableStateFlow<List<ModInfo>>(emptyList())
    val installedDataPacks: StateFlow<List<ModInfo>> = _installedDataPacks

    private val _installedShaders = MutableStateFlow<List<ModInfo>>(emptyList())
    val installedShaders: StateFlow<List<ModInfo>> = _installedShaders

    private val _installedWorlds = MutableStateFlow<List<ModInfo>>(emptyList())
    val installedWorlds: StateFlow<List<ModInfo>> = _installedWorlds

    fun setTab(tab: ContentTab) {
        _currentTab.value = tab
    }

    fun setInstalledType(type: ContentType) {
        _installedType.value = type
    }

    fun setBrowseType(type: ContentType) {
        _browseType.value = type
    }

    fun setBrowseProvider(provider: Provider) {
        _browseProvider.value = provider
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        
        _isSearching.value = true
        _searchError.value = null
        
        viewModelScope.launch {
            // Simulate search delay
            kotlinx.coroutines.delay(800)
            
            // Mock search results
            _searchResults.value = listOf(
                ModInfo("sodium", "Sodium", "jellysquid3", 12400000, "Modern rendering engine optimization", "", listOf("optimization"), listOf("1.21.4")),
                ModInfo("iris", "Iris Shaders", "coderbot", 8100000, "Beautiful shader support", "", listOf("shaders"), listOf("1.21.4")),
                ModInfo("fabric-api", "Fabric API", "modmuss50", 21700000, "Essential hooks for mods", "", listOf("library"), listOf("1.21.4"))
            )
            _isSearching.value = false
        }
    }

    fun getInstalledContent(): List<ModInfo> {
        return when (_installedType.value) {
            ContentType.MODS -> _installedMods.value
            ContentType.RESOURCE_PACKS -> _installedResourcePacks.value
            ContentType.DATA_PACKS -> _installedDataPacks.value
            ContentType.SHADERS -> _installedShaders.value
            ContentType.WORLDS -> _installedWorlds.value
        }
    }
}