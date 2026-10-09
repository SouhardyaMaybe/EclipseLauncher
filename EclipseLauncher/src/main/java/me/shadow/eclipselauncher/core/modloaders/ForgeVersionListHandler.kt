package me.shadow.eclipselauncher.core.modloaders

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * SAX handler collecting every `<version>` element text of a Forge/NeoForge maven-metadata.xml.
 */
class ForgeVersionListHandler : DefaultHandler() {
    private var mForgeVersions: MutableList<String> = ArrayList()
    private var mCurrentVersion: StringBuilder? = null

    override fun startDocument() {
        mForgeVersions = ArrayList()
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        mCurrentVersion?.append(ch, start, length)
    }

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        if (qName == "version") mCurrentVersion = StringBuilder()
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (qName == "version") {
            // Only reachable after startElement matched the same tag, so the builder is present
            val version = mCurrentVersion!!.toString()
            mForgeVersions.add(version)
            mCurrentVersion = null
        }
    }

    val versions: List<String>
        get() = mForgeVersions
}
