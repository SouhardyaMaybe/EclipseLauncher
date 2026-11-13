package com.eclipselauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eclipselauncher.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings

    private val _currentCategory = MutableStateFlow<SettingsCategory?>(null)
    val currentCategory: StateFlow<SettingsCategory?> = _currentCategory

    // Modal states
    private val _showRendererPicker = MutableStateFlow(false)
    val showRendererPicker: StateFlow<Boolean> = _showRendererPicker

    private val _showJvmArgsEditor = MutableStateFlow(false)
    val showJvmArgsEditor: StateFlow<Boolean> = _showJvmArgsEditor

    private val _showDownloadSourcePicker = MutableStateFlow(false)
    val showDownloadSourcePicker: StateFlow<Boolean> = _showDownloadSourcePicker

    private val _showCurseForgeKeyEditor = MutableStateFlow(false)
    val showCurseForgeKeyEditor: StateFlow<Boolean> = _showCurseForgeKeyEditor

    enum class SettingsCategory {
        VIDEO_RENDERER,
        CONTROL_CUSTOMIZATION,
        JAVA_TWEAKS,
        MISCELLANEOUS,
        EXPERIMENTAL
    }

    fun selectCategory(category: SettingsCategory) {
        _currentCategory.value = category
    }

    fun goBack() {
        _currentCategory.value = null
    }

    // Modal controls
    fun showRendererPicker() { _showRendererPicker.value = true }
    fun dismissRendererPicker() { _showRendererPicker.value = false }

    fun showJvmArgsEditor() { _showJvmArgsEditor.value = true }
    fun dismissJvmArgsEditor() { _showJvmArgsEditor.value = false }

    fun showDownloadSourcePicker() { _showDownloadSourcePicker.value = true }
    fun dismissDownloadSourcePicker() { _showDownloadSourcePicker.value = false }

    fun showCurseForgeKeyEditor() { _showCurseForgeKeyEditor.value = true }
    fun dismissCurseForgeKeyEditor() { _showCurseForgeKeyEditor.value = false }

    // Video & Renderer settings
    fun setRenderer(renderer: String) {
        _settings.value = _settings.value.copy(renderer = renderer)
        dismissRendererPicker()
    }

    fun setIgnoreNotch(enabled: Boolean) {
        _settings.value = _settings.value.copy(ignoreNotch = enabled)
    }

    fun setResolutionScaler(value: Int) {
        _settings.value = _settings.value.copy(resolutionScaler = value.coerceIn(25, 100))
    }

    fun setSustainedPerformance(enabled: Boolean) {
        _settings.value = _settings.value.copy(sustainedPerformance = enabled)
    }

    fun setAlternateSurface(enabled: Boolean) {
        _settings.value = _settings.value.copy(alternateSurface = enabled)
    }

    fun setUseSystemVulkan(enabled: Boolean) {
        _settings.value = _settings.value.copy(useSystemVulkan = enabled)
    }

    // Control Customization
    fun setDisableGestures(enabled: Boolean) {
        _settings.value = _settings.value.copy(disableGestures = enabled)
    }

    fun setDisableDoubleTapSwap(enabled: Boolean) {
        _settings.value = _settings.value.copy(disableDoubleTapSwap = enabled)
    }

    fun setLongPressDelay(value: Int) {
        _settings.value = _settings.value.copy(longPressDelay = value.coerceIn(100, 1000))
    }

    fun setControlScale(value: Int) {
        _settings.value = _settings.value.copy(controlScale = value.coerceIn(80, 250))
    }

    fun setButtonTransparency(value: Int) {
        _settings.value = _settings.value.copy(buttonTransparency = value.coerceIn(0, 100))
    }

    fun setCapitalLetters(enabled: Boolean) {
        _settings.value = _settings.value.copy(capitalLetters = enabled)
    }

    fun setMouseScale(value: Int) {
        _settings.value = _settings.value.copy(mouseScale = value.coerceIn(25, 300))
    }

    fun setMouseSpeed(value: Int) {
        _settings.value = _settings.value.copy(mouseSpeed = value.coerceIn(25, 300))
    }

    fun setStartWithVirtualMouse(enabled: Boolean) {
        _settings.value = _settings.value.copy(startWithVirtualMouse = enabled)
    }

    fun setEnableGyro(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableGyro = enabled)
    }

    fun setGyroSensitivity(value: Int) {
        _settings.value = _settings.value.copy(gyroSensitivity = value.coerceIn(25, 300))
    }

    fun setGyroSampleRate(value: Int) {
        _settings.value = _settings.value.copy(gyroSampleRate = value.coerceIn(5, 50))
    }

    fun setGyroSmoothing(enabled: Boolean) {
        _settings.value = _settings.value.copy(gyroSmoothing = enabled)
    }

    fun setInvertGyroX(enabled: Boolean) {
        _settings.value = _settings.value.copy(invertGyroX = enabled)
    }

    fun setInvertGyroY(enabled: Boolean) {
        _settings.value = _settings.value.copy(invertGyroY = enabled)
    }

    fun setJoystickDeadzone(value: Int) {
        _settings.value = _settings.value.copy(joystickDeadzone = value.coerceIn(50, 200))
    }

    fun setKeyboardPanning(enabled: Boolean) {
        _settings.value = _settings.value.copy(keyboardPanning = enabled)
    }

    // Java Tweaks
    fun setJvmArguments(args: String) {
        _settings.value = _settings.value.copy(jvmArguments = args)
    }

    fun setMemoryAllocation(mb: Int) {
        _settings.value = _settings.value.copy(memoryAllocation = mb.coerceAtLeast(256))
    }

    fun setSandboxJar(enabled: Boolean) {
        _settings.value = _settings.value.copy(sandboxJar = enabled)
    }

    // Miscellaneous
    fun setVerifyGameFiles(enabled: Boolean) {
        _settings.value = _settings.value.copy(verifyGameFiles = enabled)
    }

    fun setFastStartup(enabled: Boolean) {
        _settings.value = _settings.value.copy(fastStartup = enabled)
    }

    fun setDownloadSource(source: String) {
        _settings.value = _settings.value.copy(downloadSource = source)
    }

    fun setVerifyManifest(enabled: Boolean) {
        _settings.value = _settings.value.copy(verifyManifest = enabled)
    }

    fun setMemoryWarning(enabled: Boolean) {
        _settings.value = _settings.value.copy(memoryWarning = enabled)
    }

    fun setDisableBlocklist(enabled: Boolean) {
        _settings.value = _settings.value.copy(disableBlocklist = enabled)
    }

    fun setCurseforgeApiKey(key: String) {
        _settings.value = _settings.value.copy(curseforgeApiKey = key)
    }

    fun setAutoInstallDeps(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoInstallDeps = enabled)
    }

    // Experimental
    fun setShaderDumping(enabled: Boolean) {
        _settings.value = _settings.value.copy(shaderDumping = enabled)
    }

    fun setForceBigCore(enabled: Boolean) {
        _settings.value = _settings.value.copy(forceBigCore = enabled)
    }

    fun setForceSystemMemory(enabled: Boolean) {
        _settings.value = _settings.value.copy(forceSystemMemory = enabled)
    }

    fun setForceOpenSL(enabled: Boolean) {
        _settings.value = _settings.value.copy(forceOpenSL = enabled)
    }

    fun setUbwcWorkaround(enabled: Boolean) {
        _settings.value = _settings.value.copy(ubwcWorkaround = enabled)
    }

    // General
    fun setForceEnglish(enabled: Boolean) {
        _settings.value = _settings.value.copy(forceEnglish = enabled)
    }

    fun setAllowNotifications(enabled: Boolean) {
        _settings.value = _settings.value.copy(allowNotifications = enabled)
    }
}