package me.shadow.eclipselauncher.core.modloaders.modpacks.models

import java.util.Arrays

/**
 * POJO to represent the modrinth index inside mrpacks
 */
class ModrinthIndex {
    @JvmField
    var formatVersion: Int = 0
    @JvmField
    var game: String = ""
    @JvmField
    var versionId: String = ""
    @JvmField
    var name: String = ""
    @JvmField
    var summary: String = ""
    @JvmField
    var files: Array<ModrinthIndexFile> = arrayOf()
    @JvmField
    var dependencies: Map<String, String>? = null

    class ModrinthIndexFile {
        @JvmField
        var path: String = ""
        @JvmField
        var downloads: Array<String> = arrayOf()
        @JvmField
        var fileSize: Int = 0
        @JvmField
        var hashes: ModrinthIndexFileHashes = ModrinthIndexFileHashes()
        @JvmField
        var env: ModrinthIndexFileEnv? = null

        override fun toString(): String {
            return "ModrinthIndexFile{" +
                    "path='$path" + '\'' +
                    ", downloads=${Arrays.toString(downloads)}" +
                    ", fileSize=$fileSize" +
                    ", hashes=$hashes" +
                    '}'
        }

        class ModrinthIndexFileHashes {
            @JvmField
            var sha1: String? = null
            @JvmField
            var sha512: String? = null

            override fun toString(): String {
                return "ModrinthIndexFileHashes{" +
                        "sha1='$sha1" + '\'' +
                        ", sha512='$sha512" + '\'' +
                        '}'
            }
        }

        class ModrinthIndexFileEnv {
            @JvmField
            var client: String? = null
            @JvmField
            var server: String? = null

            override fun toString(): String {
                return "ModrinthIndexFileEnv{" +
                        "client='$client" + '\'' +
                        ", server='$server" + '\'' +
                        '}'
            }
        }
    }

    override fun toString(): String {
        return "ModrinthIndex{" +
                "formatVersion=$formatVersion" +
                ", game='$game" + '\'' +
                ", versionId='$versionId" + '\'' +
                ", name='$name" + '\'' +
                ", summary='$summary" + '\'' +
                ", files=${Arrays.toString(files)}" +
                '}'
    }
}
