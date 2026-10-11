package me.shadow.eclipselauncher.core.modloaders.modpacks.api

import android.util.ArrayMap

import me.shadow.eclipselauncher.json.JsonCodec
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.UrlManager

import me.shadow.eclipselauncher.core.Tools

import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.HashMap
import java.util.Objects

@Suppress("unused")
class ApiHandler(val baseUrl: String) {
    val additionalHeaders: MutableMap<String, String> = ArrayMap()

    constructor(url: String, apiKey: String) : this(url) {
        additionalHeaders["x-api-key"] = apiKey
    }

    fun <T> get(endpoint: String, tClass: Class<T>): T {
        return getFullUrl(additionalHeaders, "$baseUrl/$endpoint", tClass)
    }

    fun <T> get(endpoint: String, query: HashMap<String, Any>, tClass: Class<T>): T {
        return getFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, tClass)
    }

    fun <T> post(endpoint: String, body: T?, tClass: Class<T>): T {
        return postFullUrl(additionalHeaders, "$baseUrl/$endpoint", body, tClass)
    }

    fun <T> post(endpoint: String, query: HashMap<String, Any>, body: T?, tClass: Class<T>): T {
        return postFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, body, tClass)
    }

    companion object {
        //Make a get request and return the response as a raw string;
        @JvmStatic
        fun getRaw(url: String): String? {
            return getRaw(null, url)
        }

        @JvmStatic
        fun getRaw(headers: Map<String, String>?, url: String): String? {
            Logging.d("ApiHandler", url)
            var conn: HttpURLConnection? = null
            try {
                conn = UrlManager.createHttpConnection(URL(url))
                addHeaders(conn, headers)
                val inputStream = BufferedInputStream(conn.getInputStream())
                val data = Tools.read(inputStream)

                inputStream.close()
                conn.disconnect()
                return data
            } catch (e: FileNotFoundException) {
                Logging.d("ApiHandler", "File Not Found! " + Tools.printToString(e))
                return null
            } catch (e: InterruptedIOException) {
                Logging.d("ApiHandler", "The connection has been interrupted, or has been canceled.\n" + Tools.printToString(e))
                return null
            } catch (e: Exception) {
                Logging.e("ApiHandler", Tools.printToString(e))
                conn?.disconnect()
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun postRaw(url: String, body: String): String? {
            return postRaw(null, url, body)
        }

        @JvmStatic
        fun postRaw(headers: Map<String, String>?, url: String, body: String): String? {
            try {
                val conn = UrlManager.createHttpConnection(URL(url))
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                addHeaders(conn, headers)
                conn.doOutput = true

                val outputStream = conn.getOutputStream()
                val input = body.toByteArray(Charsets.UTF_8)
                outputStream.write(input, 0, input.size)
                outputStream.close()

                val inputStream = conn.getInputStream()
                val data = Tools.read(inputStream)
                inputStream.close()

                conn.disconnect()
                return data
            } catch (e: IOException) {
                Logging.e("ApiHandler", Tools.printToString(e))
            }
            return null
        }

        private fun addHeaders(connection: HttpURLConnection, headers: Map<String, String>?) {
            if (headers != null) {
                for (key in headers.keys)
                    connection.addRequestProperty(key, headers[key])
            }
        }

        private fun parseQueries(query: HashMap<String, Any>): String {
            val params = StringBuilder("?")
            for (param in query.keys) {
                val value = Objects.toString(query[param])
                params.append(urlEncodeUTF8(param))
                    .append("=")
                    .append(urlEncodeUTF8(value))
                    .append("&")
            }
            return params.substring(0, params.length - 1)
        }

        @JvmStatic
        fun <T> getFullUrl(url: String, tClass: Class<T>): T {
            return getFullUrl(null, url, tClass)
        }

        @JvmStatic
        fun <T> getFullUrl(url: String, query: HashMap<String, Any>, tClass: Class<T>): T {
            return getFullUrl(null, url, query, tClass)
        }

        @JvmStatic
        fun <T> postFullUrl(url: String, body: T?, tClass: Class<T>): T {
            return postFullUrl(null, url, body, tClass)
        }

        @JvmStatic
        fun <T> postFullUrl(url: String, query: HashMap<String, Any>, body: T?, tClass: Class<T>): T {
            return postFullUrl(null, url, query, body, tClass)
        }

        @JvmStatic
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, tClass: Class<T>): T {
            return JsonCodec().fromJson(getRaw(headers, url), tClass)
        }

        @JvmStatic
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, tClass: Class<T>): T {
            return getFullUrl(headers, url + parseQueries(query), tClass)
        }

        @JvmStatic
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, body: T?, tClass: Class<T>): T {
            return JsonCodec().fromJson(postRaw(headers, url, body!!.toString()), tClass)
        }

        @JvmStatic
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, body: T?, tClass: Class<T>): T {
            return JsonCodec().fromJson(postRaw(headers, url + parseQueries(query), body!!.toString()), tClass)
        }

        private fun urlEncodeUTF8(input: String): String {
            return try {
                URLEncoder.encode(input, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("UTF-8 is required")
            }
        }
    }
}
