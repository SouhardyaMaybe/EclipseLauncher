package me.shadow.eclipselauncher.core

import androidx.annotation.Keep

import me.shadow.eclipselauncher.core.value.DependentLibrary
import me.shadow.eclipselauncher.core.value.MinecraftClientInfo

@Keep
@Suppress("unused") // all unused fields here are parts of JSON structures
class JMinecraftVersionList {
    @JvmField
    var latest: Map<String, String>? = null
    @JvmField
    var versions: Array<Version> = emptyArray()

    @Keep
    open class FileProperties {
        @JvmField
        var id: String = ""
        @JvmField
        var sha1: String = ""
        @JvmField
        var url: String = ""
        @JvmField
        var size: Long = 0
    }

    @Keep
    class Version : FileProperties() {
        // Since 1.13, so it's one of ways to check
        @JvmField
        var arguments: Arguments? = null
        @JvmField
        var assetIndex: AssetIndex? = null

        @JvmField
        var assets: String = ""
        @JvmField
        var downloads: MutableMap<String, MinecraftClientInfo>? = null
        @JvmField
        var inheritsFrom: String? = null
        @JvmField
        var javaVersion: JavaVersionInfo? = null
        @JvmField
        var libraries: Array<DependentLibrary> = emptyArray()
        @JvmField
        var mainClass: String = ""
        @JvmField
        var minecraftArguments: String? = null
        @JvmField
        var minimumLauncherVersion: Int = 0
        @JvmField
        var releaseTime: String = ""
        @JvmField
        var time: String = ""
        @JvmField
        var type: String = ""
    }

    @Keep
    class JavaVersionInfo {
        @JvmField
        var component: String? = null
        @JvmField
        var majorVersion: Int = 0
        @JvmField
        var version: Int = 0 // parameter used by LabyMod 4
    }

    // Since 1.13
    @Keep
    class Arguments {
        @JvmField
        var game: Array<Any?> = emptyArray()
        @JvmField
        var jvm: Array<Any?>? = null

        @Keep
        class ArgValue {
            @JvmField
            var rules: Array<ArgRules>? = null
            @JvmField
            var value: String? = null

            // TLauncher styled argument...
            @JvmField
            var values: Array<String>? = null

            @Keep
            class ArgRules {
                @JvmField
                var action: String = ""
                @JvmField
                var features: String? = null
                @JvmField
                var os: ArgOS? = null

                @Keep
                class ArgOS {
                    @JvmField
                    var name: String? = null
                    @JvmField
                    var version: String? = null
                }
            }
        }
    }

    @Keep
    class AssetIndex : FileProperties() {
        @JvmField
        var totalSize: Long = 0
    }
}
