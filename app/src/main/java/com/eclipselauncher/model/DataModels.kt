package com.eclipselauncher.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val username: String,
    val type: AccountType,
    val skinPath: String? = null,
    val capePath: String? = null,
    val isSelected: Boolean = false
) {
    enum class AccountType {
        LOCAL, MICROSOFT
    }
}

@Serializable
data class Instance(
    val id: String,
    val name: String,
    val version: String,
    val loader: LoaderType,
    val iconPath: String? = null,
    val jvmArguments: String = "",
    val javaRuntime: String = "Auto",
    val renderer: String = "Global",
    val sharedData: Boolean = false,
    val controlMap: String = "global_default",
    val isSelected: Boolean = false
) {
    enum class LoaderType {
        VANILLA, OPTIFINE, FABRIC, FORGE, NEOFORGE, QUILT
    }
}

data class GameVersion(
    val id: String,
    val releaseDate: String,
    val type: VersionType,
    val loaders: Map<Instance.LoaderType, List<String>>
) {
    enum class VersionType {
        RELEASE, SNAPSHOT, OLD_BETA, OLD_ALPHA
    }
}

data class ModInfo(
    val id: String,
    val name: String,
    val author: String,
    val downloads: Long,
    val description: String,
    val iconUrl: String,
    val categories: List<String>,
    val gameVersions: List<String>
)

data class TaskInfo(
    val id: String,
    val title: String,
    val detail: String,
    val progress: Float, // 0-1, -1 for indeterminate
    val status: TaskStatus,
    val isExpanded: Boolean = false
) {
    enum class TaskStatus {
        RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
}

data class SettingsState(
    val forceEnglish: Boolean = false,
    val allowNotifications: Boolean = false,
    // Video & Renderer
    val renderer: String = "GL4ES",
    val ignoreNotch: Boolean = false,
    val resolutionScaler: Int = 100,
    val sustainedPerformance: Boolean = false,
    val alternateSurface: Boolean = false,
    val useSystemVulkan: Boolean = false,
    // Control Customization
    val disableGestures: Boolean = false,
    val disableDoubleTapSwap: Boolean = false,
    val longPressDelay: Int = 300,
    val controlScale: Int = 100,
    val buttonTransparency: Int = 100,
    val capitalLetters: Boolean = true,
    val mouseScale: Int = 100,
    val mouseSpeed: Int = 100,
    val startWithVirtualMouse: Boolean = false,
    val enableGyro: Boolean = false,
    val gyroSensitivity: Int = 100,
    val gyroSampleRate: Int = 10,
    val gyroSmoothing: Boolean = true,
    val invertGyroX: Boolean = false,
    val invertGyroY: Boolean = false,
    val joystickDeadzone: Int = 100,
    val keyboardPanning: Boolean = true,
    // Java Tweaks
    val jvmArguments: String = "",
    val memoryAllocation: Int = 2048,
    val sandboxJar: Boolean = true,
    // Miscellaneous
    val verifyGameFiles: Boolean = true,
    val fastStartup: Boolean = true,
    val downloadSource: String = "Default",
    val verifyManifest: Boolean = true,
    val clearMetadataCache: Boolean = false,
    val clearDownloadCache: Boolean = false,
    val memoryWarning: Boolean = true,
    val disableBlocklist: Boolean = true,
    val curseforgeApiKey: String = "",
    val autoInstallDeps: Boolean = true,
    // Experimental
    val shaderDumping: Boolean = false,
    val forceBigCore: Boolean = false,
    val forceSystemMemory: Boolean = false,
    val forceOpenSL: Boolean = false,
    val ubwcWorkaround: Boolean = false
)