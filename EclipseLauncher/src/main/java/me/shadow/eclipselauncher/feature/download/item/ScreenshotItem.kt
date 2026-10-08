package me.shadow.eclipselauncher.feature.download.item

/**
 * Screenshot information record
 * @param imageUrl the address of the screenshot
 * @param title the title of the screenshot
 * @param description the description of the screenshot
 */
class ScreenshotItem(
    val imageUrl: String,
    val title: String?,
    val description: String?
) {
    override fun toString(): String {
        return "ScreenshotItem(imageUrl='$imageUrl', title='$title', description='$description')"
    }
}