package me.shadow.eclipselauncher.core.modloaders

import me.shadow.eclipselauncher.core.utils.DownloadUtils
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

object ForgeUtils {
    private const val FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    private const val FORGE_INSTALLER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar"

    @JvmStatic
    @Throws(Exception::class)
    fun downloadForgeVersions(force: Boolean): List<String> {
        val parserFactory = SAXParserFactory.newInstance()
        val saxParser = parserFactory.newSAXParser()

        return DownloadUtils.downloadStringCached(FORGE_METADATA_URL, "forge_versions", force) { input ->
            try {
                val handler = ForgeVersionListHandler()
                saxParser.parse(InputSource(StringReader(input)), handler)
                handler.versions
                // IOException is present here StringReader throws it only if the parser called close()
                // sooner than needed, which is a parser issue and not an I/O one
            } catch (e: SAXException) {
                throw DownloadUtils.ParseException(e)
            } catch (e: IOException) {
                throw DownloadUtils.ParseException(e)
            }
        }
    }

    @JvmStatic
    fun getInstallerUrl(version: String?): String {
        return String.format(FORGE_INSTALLER_URL, version)
    }
}
