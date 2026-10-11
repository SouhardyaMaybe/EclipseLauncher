package me.shadow.eclipselauncher.core.modloaders.modpacks.models

class CurseManifest {
    @JvmField
    var name: String = ""
    @JvmField
    var version: String = ""
    @JvmField
    var author: String = ""
    @JvmField
    var manifestType: String = ""
    @JvmField
    var manifestVersion: Int = 0
    @JvmField
    var files: Array<CurseFile> = arrayOf()
    @JvmField
    var minecraft: CurseMinecraft = CurseMinecraft()
    @JvmField
    var overrides: String? = null

    class CurseFile {
        @JvmField
        var projectID: Long = 0
        @JvmField
        var fileID: Long = 0
        @JvmField
        var required: Boolean = false
    }

    class CurseMinecraft {
        @JvmField
        var version: String? = null
        @JvmField
        var modLoaders: Array<CurseModLoader> = arrayOf()
    }

    class CurseModLoader {
        @JvmField
        var id: String = ""
        @JvmField
        var primary: Boolean = false
    }
}
