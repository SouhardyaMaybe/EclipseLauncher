package me.shadow.eclipselauncher.feature.download.item

import me.shadow.eclipselauncher.feature.download.enums.Category
import me.shadow.eclipselauncher.feature.download.enums.Classify
import me.shadow.eclipselauncher.feature.download.enums.Platform
import java.util.Date

/**
 * Basic information class
 * @param classify the category of the project
 * @param platform the platform the project belongs to
 * @param projectId the unique identifier of the project
 * @param slug the slug of the project
 * @param author the author of the project
 * @param title the title of the project
 * @param description the description of the project
 * @param downloadCount the total download count of the project
 * @param uploadDate the upload date of the project
 * @param iconUrl the cover image link of the project
 * @param category the tags of the project
 */
open class InfoItem(
    val classify: Classify,
    val platform: Platform,
    val projectId: String,
    val slug: String,
    val author: Array<String>?,
    val title: String,
    val description: String,
    val downloadCount: Long,
    val uploadDate: Date,
    val iconUrl: String?,
    val category: List<Category>
) {
    fun copy() = InfoItem(
        classify, platform, projectId, slug, author, title, description, downloadCount, uploadDate, iconUrl, category
    )

    override fun toString(): String {
        return "InfoItem(" +
                "classify='$classify', " +
                "platform='$platform', " +
                "projectId='$projectId', " +
                "slug='$slug', " +
                "author=${author.contentToString()}, " +
                "title='$title', " +
                "description='$description', " +
                "downloadCount=$downloadCount, " +
                "uploadDate=$uploadDate, " +
                "iconUrl='$iconUrl', " +
                "category=$category" +
                ")"
    }
}