/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * One Ktor client for the whole process, backed by OkHttp. JSON uses
 * kotlinx.serialization; every request carries the launcher user agent and a
 * bounded timeout so a dead endpoint cannot hang the UI.
 */
package me.shadow.eclipselauncher.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class NetworkClient @Inject constructor() {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val http: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
    }

    /**
     * Fetches a small document as text. Returns null for a non-2xx response,
     * a transport failure, or a body larger than [maxBytes].
     */
    suspend fun getText(url: String, maxBytes: Int): String? = withContext(Dispatchers.IO) {
        try {
            val response = http.get(url)
            if (!response.status.isSuccess()) return@withContext null
            val text = response.bodyAsText()
            if (text.length > maxBytes) null else text
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        const val USER_AGENT = "EclipseLauncher/1.0 (github.com/SouhardyaMaybe/Eclipse)"
    }
}
