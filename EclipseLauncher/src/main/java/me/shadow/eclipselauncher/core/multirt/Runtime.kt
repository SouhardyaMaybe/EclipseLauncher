package me.shadow.eclipselauncher.core.multirt

import java.util.Objects

class Runtime {
    @JvmField
    val name: String
    @JvmField
    val versionString: String?
    @JvmField
    val arch: String?
    @JvmField
    val javaVersion: Int
    @JvmField
    var isProvidedByLauncher = false

    constructor(name: String) {
        this.name = name
        this.versionString = null
        this.arch = null
        this.javaVersion = 0
    }

    /**
     * Only MultiRTUtils fills in the release file data, hence the internal visibility:
     * Kotlin has no package-private constructors, and no other package builds a runtime.
     */
    internal constructor(name: String, versionString: String?, arch: String?, javaVersion: Int) {
        this.name = name
        this.versionString = versionString
        this.arch = arch
        this.javaVersion = javaVersion
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Runtime
        return name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}
