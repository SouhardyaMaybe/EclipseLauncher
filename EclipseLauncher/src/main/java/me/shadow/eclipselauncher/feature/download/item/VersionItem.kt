package me.shadow.eclipselauncher.feature.download.item

import me.shadow.eclipselauncher.feature.download.enums.VersionType
import java.util.Date

/**
 * Version information class
 * @param projectId the unique identifier of the project this version belongs to
 * @param title the title of the version
 * @param downloadCount the total download count of the version
 * @param uploadDate the upload date of the version
 * @param mcVersions the MC versions supported by this version
 * @param versionType the release status of the version
 * @param fileName the file name of the version
 * @param fileHash the file hash of the version
 * @param fileUrl the file download link of the version
 */
open class VersionItem(
    val projectId: String,
    val title: String,
    val downloadCount: Long,
    val uploadDate: Date,
    val mcVersions: List<String>,
    val versionType: VersionType,
    val fileName: String,
    val fileHash: String?,
    val fileUrl: String
) {
    override fun toString(): String {
        return "VersionItem(" +
                "projectId='$projectId', " +
                "title='$title', " +
                "downloadCount=$downloadCount, " +
                "uploadDate=$uploadDate, " +
                "mcVersions=$mcVersions, " +
                "versionType=$versionType, " +
                "fileName='$fileName'" +
                "fileHash='$fileHash'" +
                "fileUrl='$fileUrl'" +
                ")"
    }
}