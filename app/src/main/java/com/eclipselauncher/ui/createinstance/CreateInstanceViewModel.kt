package com.eclipselauncher.ui.createinstance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eclipselauncher.model.GameVersion
import com.eclipselauncher.model.Instance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreateInstanceViewModel : ViewModel() {

    // Version selection state
    private val _versions = MutableStateFlow<List<GameVersion>>(emptyList())
    val versions: StateFlow<List<GameVersion>> = _versions

    private val _selectedVersionTab = MutableStateFlow<GameVersion.VersionType>(GameVersion.VersionType.RELEASE)
    val selectedVersionTab: StateFlow<GameVersion.VersionType> = _selectedVersionTab

    private val _selectedVersion = MutableStateFlow<GameVersion?>(null)
    val selectedVersion: StateFlow<GameVersion?> = _selectedVersion

    private val _versionsLoading = MutableStateFlow(false)
    val versionsLoading: StateFlow<Boolean> = _versionsLoading

    private val _versionsError = MutableStateFlow<String?>(null)
    val versionsError: StateFlow<String?> = _versionsError

    // Loader selection state
    private val _selectedLoader = MutableStateFlow<Instance.LoaderType>(Instance.LoaderType.VANILLA)
    val selectedLoader: StateFlow<Instance.LoaderType> = _selectedLoader

    private val _selectedLoaderVersion = MutableStateFlow<String>("")
    val selectedLoaderVersion: StateFlow<String> = _selectedLoaderVersion

    private val _loaderVersions = MutableStateFlow<Map<Instance.LoaderType, List<String>>>(emptyMap())
    val loaderVersions: StateFlow<Map<Instance.LoaderType, List<String>>> = _loaderVersions

    private val _loaderVersionsLoading = MutableStateFlow(false)
    val loaderVersionsLoading: StateFlow<Boolean> = _loaderVersionsLoading

    // Instance editor state
    private val _instanceName = MutableStateFlow("")
    val instanceName: StateFlow<String> = _instanceName

    private val _controlMap = MutableStateFlow("global_default")
    val controlMap: StateFlow<String> = _controlMap

    private val _sharedData = MutableStateFlow(false)
    val sharedData: StateFlow<Boolean> = _sharedData

    private val _jvmArguments = MutableStateFlow("")
    val jvmArguments: StateFlow<String> = _jvmArguments

    private val _javaRuntime = MutableStateFlow("Auto")
    val javaRuntime: StateFlow<String> = _javaRuntime

    private val _renderer = MutableStateFlow("Global")
    val renderer: StateFlow<String> = _renderer

    private val _customIconPath = MutableStateFlow<String?>(null)
    val customIconPath: StateFlow<String?> = _customIconPath

    private val _isEditingExisting = MutableStateFlow(false)
    val isEditingExisting: StateFlow<Boolean> = _isEditingExisting

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges

    init {
        loadMockVersions()
    }

    private fun loadMockVersions() {
        viewModelScope.launch {
            _versionsLoading.value = true
            kotlinx.coroutines.delay(500)
            
            // Mock version data
            val mockVersions = listOf(
                GameVersion("1.21.4", "Dec 3, 2024", GameVersion.VersionType.RELEASE, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.OPTIFINE to listOf("HD_U_I1", "HD_U_I2"),
                    Instance.LoaderType.FABRIC to listOf("0.16.10", "0.16.9", "0.15.11", "0.14.24"),
                    Instance.LoaderType.FORGE to listOf("54.0.0", "53.1.0", "52.0.0"),
                    Instance.LoaderType.NEOFORGE to listOf("21.0.0", "20.0.0"),
                    Instance.LoaderType.QUILT to listOf("0.1.0", "0.0.9")
                )),
                GameVersion("1.21.3", "Oct 23, 2024", GameVersion.VersionType.RELEASE, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FABRIC to listOf("0.16.9", "0.15.11"),
                    Instance.LoaderType.FORGE to listOf("53.1.0", "52.0.0")
                )),
                GameVersion("1.21.2", "Oct 22, 2024", GameVersion.VersionType.RELEASE, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FABRIC to listOf("0.16.9"),
                    Instance.LoaderType.FORGE to listOf("52.0.0")
                )),
                GameVersion("1.21.1", "Aug 8, 2024", GameVersion.VersionType.RELEASE, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FABRIC to listOf("0.15.11"),
                    Instance.LoaderType.FORGE to listOf("51.0.0")
                )),
                GameVersion("1.21", "Jun 13, 2024", GameVersion.VersionType.RELEASE, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FABRIC to listOf("0.14.24"),
                    Instance.LoaderType.FORGE to listOf("50.0.0")
                )),
                GameVersion("24w44a", "Oct 30, 2024", GameVersion.VersionType.SNAPSHOT, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FABRIC to listOf("0.16.10-pre.1")
                )),
                GameVersion("1.12.2", "Sep 18, 2017", GameVersion.VersionType.OLD_BETA, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FORGE to listOf("14.23.5.2860")
                )),
                GameVersion("1.7.10", "Jul 25, 2014", GameVersion.VersionType.OLD_ALPHA, mapOf(
                    Instance.LoaderType.VANILLA to listOf(""),
                    Instance.LoaderType.FORGE to listOf("10.13.4.1614")
                ))
            )
            _versions.value = mockVersions
            _versionsLoading.value = false
        }
    }

    fun setVersionTab(tab: GameVersion.VersionType) {
        _selectedVersionTab.value = tab
        _selectedVersion.value = null
    }

    fun selectVersion(version: GameVersion) {
        _selectedVersion.value = version
        // Load loader versions for this version
        _loaderVersions.value = version.loaders
        // Default to Vanilla
        _selectedLoader.value = Instance.LoaderType.VANILLA
        _selectedLoaderVersion.value = ""
    }

    fun selectLoader(loader: Instance.LoaderType) {
        _selectedLoader.value = loader
        val versions = _loaderVersions.value[loader] ?: emptyList()
        _selectedLoaderVersion.value = if (versions.isNotEmpty()) versions.first() else ""
    }

    fun selectLoaderVersion(version: String) {
        _selectedLoaderVersion.value = version
    }

    fun loadInstanceForEditing(instance: Instance) {
        _isEditingExisting.value = true
        _instanceName.value = instance.name
        _controlMap.value = instance.controlMap
        _sharedData.value = instance.sharedData
        _jvmArguments.value = instance.jvmArguments
        _javaRuntime.value = instance.javaRuntime
        _renderer.value = instance.renderer
        _customIconPath.value = instance.iconPath
        _hasChanges.value = false
    }

    fun updateInstanceName(name: String) {
        _instanceName.value = name
        _hasChanges.value = true
    }

    fun updateControlMap(map: String) {
        _controlMap.value = map
        _hasChanges.value = true
    }

    fun updateSharedData(enabled: Boolean) {
        _sharedData.value = enabled
        _hasChanges.value = true
    }

    fun updateJvmArguments(args: String) {
        _jvmArguments.value = args
        _hasChanges.value = true
    }

    fun updateJavaRuntime(runtime: String) {
        _javaRuntime.value = runtime
        _hasChanges.value = true
    }

    fun updateRenderer(renderer: String) {
        _renderer.value = renderer
        _hasChanges.value = true
    }

    fun updateCustomIcon(path: String?) {
        _customIconPath.value = path
        _hasChanges.value = true
    }

    fun createInstance(): Instance {
        val version = _selectedVersion.value ?: return Instance("", "", "", Instance.LoaderType.VANILLA)
        return Instance(
            id = if (_isEditingExisting.value) "existing" else "new_${System.currentTimeMillis()}",
            name = _instanceName.value.ifBlank { "${version.id} ${_selectedLoader.value.name}" },
            version = version.id,
            loader = _selectedLoader.value,
            iconPath = _customIconPath.value,
            jvmArguments = _jvmArguments.value,
            javaRuntime = _javaRuntime.value,
            renderer = _renderer.value,
            sharedData = _sharedData.value,
            controlMap = _controlMap.value
        )
    }

    fun reset() {
        _selectedVersionTab.value = GameVersion.VersionType.RELEASE
        _selectedVersion.value = null
        _selectedLoader.value = Instance.LoaderType.VANILLA
        _selectedLoaderVersion.value = ""
        _instanceName.value = ""
        _controlMap.value = "global_default"
        _sharedData.value = false
        _jvmArguments.value = ""
        _javaRuntime.value = "Auto"
        _renderer.value = "Global"
        _customIconPath.value = null
        _isEditingExisting.value = false
        _hasChanges.value = false
    }

    val filteredVersions: StateFlow<List<GameVersion>> = combine(_versions, _selectedVersionTab) { versions, tab ->
        versions.filter { it.type == tab }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}