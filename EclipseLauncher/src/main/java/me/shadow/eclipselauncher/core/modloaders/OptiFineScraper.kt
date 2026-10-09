package me.shadow.eclipselauncher.core.modloaders

import me.shadow.eclipselauncher.core.utils.DownloadUtils
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode

/**
 * Parses the OptiFine downloads page into [OptiFineUtils.OptiFineVersions]: every `h2` heading
 * starts a Minecraft version block, every `tr.downloadLine*` row contributes one OptiFine version.
 */
class OptiFineScraper : DownloadUtils.ParseCallback<OptiFineUtils.OptiFineVersions> {
    private val mOptiFineVersions: OptiFineUtils.OptiFineVersions
    private var mListInProgress: MutableList<OptiFineUtils.OptiFineVersion>? = null
    private var mMinecraftVersion: String? = null

    init {
        mOptiFineVersions = OptiFineUtils.OptiFineVersions()
        mOptiFineVersions.minecraftVersions = ArrayList()
        mOptiFineVersions.optifineVersions = ArrayList()
    }

    @Throws(DownloadUtils.ParseException::class)
    override fun process(input: String): OptiFineUtils.OptiFineVersions {
        val htmlCleaner = HtmlCleaner()
        val tagNode = htmlCleaner.clean(input)
        traverseTagNode(tagNode)
        insertVersionContent(null)
        if (mOptiFineVersions.optifineVersions.isEmpty() || mOptiFineVersions.minecraftVersions.isEmpty()) throw DownloadUtils.ParseException(null)
        return mOptiFineVersions
    }

    fun traverseTagNode(tagNode: TagNode) {
        if (isDownloadLine(tagNode) && mMinecraftVersion != null) {
            traverseDownloadLine(tagNode)
        } else if (isMinecraftVersionTag(tagNode)) {
            insertVersionContent(tagNode)
        } else {
            for (tagNodes in tagNode.childTags) {
                traverseTagNode(tagNodes)
            }
        }
    }

    private fun isDownloadLine(tagNode: TagNode): Boolean {
        return tagNode.name == "tr" &&
                tagNode.hasAttribute("class") &&
                tagNode.getAttributeByName("class").startsWith("downloadLine")
    }

    private fun isMinecraftVersionTag(tagNode: TagNode): Boolean {
        return tagNode.name == "h2" &&
                tagNode.text.toString().startsWith("Minecraft ")
    }

    private fun traverseDownloadLine(tagNode: TagNode) {
        val optiFineVersion = OptiFineUtils.OptiFineVersion()
        optiFineVersion.minecraftVersion = mMinecraftVersion
        for (subNode in tagNode.childTags) {
            if (subNode.name != "td") continue
            when (subNode.getAttributeByName("class")) {
                "colFile" -> {
                    optiFineVersion.versionName = subNode.text.toString()
                }
                "colMirror" -> {
                    optiFineVersion.downloadUrl = getLinkHref(subNode)
                }
            }
        }
        mListInProgress!!.add(optiFineVersion)
    }

    private fun getLinkHref(parent: TagNode): String? {
        for (subNode in parent.childTags) {
            if (subNode.name == "a" && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://")
            }
        }
        return null
    }

    private fun insertVersionContent(tagNode: TagNode?) {
        if (mListInProgress != null && mMinecraftVersion != null) {
            mOptiFineVersions.minecraftVersions.add(mMinecraftVersion!!)
            mOptiFineVersions.optifineVersions.add(mListInProgress!!)
        }
        if (tagNode != null) {
            mMinecraftVersion = tagNode.text.toString()
            mListInProgress = ArrayList()
        }
    }
}
