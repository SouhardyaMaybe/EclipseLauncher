package me.shadow.eclipselauncher.feature.update

import me.shadow.eclipselauncher.json.annotations.SerializedName

class LauncherVersion(
    @SerializedName("version_code")
    val versionCode: Int = 0,
    @SerializedName("version_name")
    val versionName: String = "",
    val title: WhatsNew = WhatsNew(),
    val description: WhatsNew = WhatsNew(),
    @SerializedName("published_at")
    val publishedAt: String = "",
    @SerializedName("file_size")
    val fileSize: FileSize = FileSize(),
    @SerializedName("pre_release")
    val isPreRelease: Boolean = false
) {

    override fun toString(): String {
        return "Version{" +
                "versionCode=$versionCode" +
                ", versionName='$versionName" + '\'' +
                ", title=$title" +
                ", description=$description" +
                ", publishedAt='$publishedAt" + '\'' +
                ", fileSize=$fileSize" +
                '}'
    }

    class WhatsNew(
        @SerializedName("en_us")
        val enUS: String = ""
    ) {

        override fun toString(): String {
            return "WhatsNew{" +
                    "enUS='$enUS" + '\'' +
                    '}'
        }
    }

    class FileSize(
        val all: Long = 0,
        val arm: Long = 0,
        val arm64: Long = 0,
        val x86: Long = 0,
        val x86_64: Long = 0
    ) {

        override fun toString(): String {
            return "FileSize{" +
                    "all=$all" +
                    ", arm=$arm" +
                    ", arm64=$arm64" +
                    ", x86=$x86" +
                    ", x86_64=$x86_64" +
                    '}'
        }
    }
}
