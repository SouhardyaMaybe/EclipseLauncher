@file:Suppress("IOStreamConstructor")

package me.shadow.eclipselauncher.core.utils

import androidx.annotation.Nullable

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.path.UrlManager

import me.shadow.eclipselauncher.core.Tools

import me.shadow.eclipselauncher.utils.io.IOUtils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

object DownloadUtils {
    @JvmStatic
    @Throws(IOException::class)
    fun download(url: String, os: OutputStream) {
        download(URL(url), os)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun download(url: URL, os: OutputStream) {
        var inputStream: InputStream? = null
        try {
            val conn = UrlManager.createHttpConnection(url)
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP " + conn.responseCode +
                        ": " + conn.responseMessage)
            }
            inputStream = conn.inputStream
            IOUtils.copy(inputStream, os)
        } catch (e: SocketTimeoutException) {
            throw IOException("Download timed out: $url", e)
        } catch (e: IOException) {
            throw IOException("Unable to download from $url", e)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Logging.e("DownloadUtils", Tools.printToString(e))
                }
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadString(url: String): String {
        val bos = ByteArrayOutputStream()
        download(url, bos)
        bos.close()
        return String(bos.toByteArray(), StandardCharsets.UTF_8)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadFile(url: String, out: File) {
        FileUtils.ensureParentDirectory(out)
        FileOutputStream(out).use { fileOutputStream ->
            download(url, fileOutputStream)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadFileMonitored(urlInput: String, outputFile: File, @Nullable buffer: ByteArray?, monitor: Tools.DownloaderFeedback) {
        FileUtils.ensureParentDirectory(outputFile)

        val conn = URL(urlInput).openConnection() as HttpURLConnection
        conn.readTimeout = UrlManager.TIME_OUT.first
        val readStr = conn.inputStream
        var current: Int
        var overall = 0
        val length = conn.contentLength

        val readBuffer = buffer ?: ByteArray(65535)

        FileOutputStream(outputFile).use { fos ->
            while (true) {
                current = readStr.read(readBuffer)
                if (current == -1) break
                overall += current
                fos.write(readBuffer, 0, current)
                monitor.updateProgress(overall.toLong(), length.toLong())
            }
            conn.disconnect()
        }
    }

    @JvmStatic
    @Throws(IOException::class, ParseException::class)
    fun <T> downloadStringCached(url: String, cacheName: String, force: Boolean, parseCallback: ParseCallback<T>): T {
        val cacheDestination = File(PathManager.DIR_CACHE_STRING, cacheName)
        if (force && cacheDestination.exists()) me.shadow.eclipselauncher.utils.io.FileUtils.deleteQuietly(cacheDestination)
        if (cacheDestination.isFile && cacheDestination.canRead() &&
                EclipseTools.getCurrentTimeMillis() < (cacheDestination.lastModified() + 86400000)) {
            try {
                val cachedString = Tools.read(FileInputStream(cacheDestination))
                return parseCallback.process(cachedString)
            } catch (e: IOException) {
                Logging.i("DownloadUtils", "Failed to read the cached file", e)
            } catch (e: ParseException) {
                Logging.i("DownloadUtils", "Failed to parse the cached file", e)
            }
        }
        val urlContent = downloadString(url)
        // if we download the file and fail parsing it, we will yeet outta there
        // and not cache the unparseable sting. We will return this after trying to save the downloaded
        // string into cache
        val parseResult = parseCallback.process(urlContent)

        val tryWriteCache = if (cacheDestination.exists()) {
            cacheDestination.canWrite()
        } else {
            FileUtils.ensureParentDirectorySilently(cacheDestination)
        }

        if (tryWriteCache) {
            try {
                Tools.write(cacheDestination.absolutePath, urlContent)
            } catch (e: IOException) {
                Logging.i("DownloadUtils", "Failed to cache the string", e)
            }
        }
        return parseResult
    }

    @Throws(IOException::class)
    private fun <T> downloadFile(downloadFunction: DownloadFunction<T>): T {
        return try {
            downloadFunction.download()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun verifyFile(file: File, sha1: String): Boolean {
        return file.exists() && Tools.compareSHA1(file, sha1)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun <T> ensureSha1(outputFile: File, @Nullable sha1: String?, downloadFunction: DownloadFunction<T>): T? {
        // Skip if needed
        if (sha1 == null) {
            // If the file exists and we don't know it's SHA1, don't try to redownload it.
            return if (outputFile.exists()) null else downloadFile(downloadFunction)
        }

        var attempts = 0
        var fileOkay = verifyFile(outputFile, sha1)
        var lastNetworkError: IOException? = null
        while (attempts < 5 && !fileOkay) {
            attempts++
            try {
                downloadFile(downloadFunction)
                lastNetworkError = null
            } catch (e: IOException) {
                // Transient network failure (e.g. SocketTimeoutException): retry
                // within the same attempt budget instead of aborting the install
                lastNetworkError = e
            }
            fileOkay = verifyFile(outputFile, sha1)
        }
        if (!fileOkay) {
            // A download that never completed is a network failure, not a checksum
            // mismatch: rethrow it as-is so callers see the original error
            throw lastNetworkError
                ?: SHA1VerificationException("SHA1 verifcation failed after 5 download attempts")
        }
        return null
    }

    /**
     * Get the content length for a given URL.
     * @param url the URL to get the length for
     * @return the length in bytes or -1 if not available
     * @throws IOException if an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getContentLength(url: String): Long {
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.requestMethod = "HEAD"
        urlConnection.doInput = false
        urlConnection.doOutput = false
        urlConnection.connect()
        val responseCode = urlConnection.responseCode
        if (responseCode in 200..299) return urlConnection.contentLength.toLong()
        return -1
    }

    fun interface ParseCallback<T> {
        @Throws(ParseException::class)
        fun process(input: String): T
    }

    /**
     * The blocking download body handed to [ensureSha1]; it declares a checked IOException,
     * so a single checked exception still reaches the Java callers of this method.
     */
    fun interface DownloadFunction<T> {
        @Throws(IOException::class)
        fun download(): T
    }

    class ParseException(e: Exception?) : Exception(e)

    class SHA1VerificationException(message: String) : IOException(message)
}
