/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import com.google.common.truth.Truth.assertThat
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class HttpDownloadSourceTest {
    private lateinit var server: HttpServer
    private var port = 0
    private val requests = AtomicInteger()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        port = server.address.port
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun url(path: String): String = "http://127.0.0.1:$port$path"

    private fun source(
        guard: UrlGuard = permissive,
        maxRedirects: Int = 5,
        maxAttempts: Int = 4,
        delays: MutableList<Long> = mutableListOf(),
    ): HttpDownloadSource = HttpDownloadSource(
        guard = guard,
        connectTimeoutMillis = 5_000,
        readTimeoutMillis = 5_000,
        maxRedirects = maxRedirects,
        maxAttempts = maxAttempts,
        sleeper = { delays += it },
        jitterMillis = { 0L },
    )

    private fun respond(exchange: com.sun.net.httpserver.HttpExchange, status: Int, body: String) {
        requests.incrementAndGet()
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun redirect(exchange: com.sun.net.httpserver.HttpExchange, location: String) {
        requests.incrementAndGet()
        exchange.responseHeaders.add("Location", location)
        exchange.sendResponseHeaders(302, -1)
        exchange.close()
    }

    @Test
    fun `downloads a body`() {
        server.createContext("/ok") { exchange -> respond(exchange, 200, "hello") }

        val bytes = source().open(url("/ok")).use { it.readBytes() }

        assertThat(String(bytes, Charsets.UTF_8)).isEqualTo("hello")
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `follows a relative redirect`() {
        server.createContext("/start") { exchange -> redirect(exchange, "/ok") }
        server.createContext("/ok") { exchange -> respond(exchange, 200, "redirected") }

        val bytes = source().open(url("/start")).use { it.readBytes() }

        assertThat(String(bytes, Charsets.UTF_8)).isEqualTo("redirected")
        assertThat(requests.get()).isEqualTo(2)
    }

    @Test
    fun `stops a redirect loop at the limit`() {
        server.createContext("/loop") { exchange -> redirect(exchange, "/loop") }

        val error = assertThrows(NetworkException::class.java) {
            source(maxRedirects = 2).open(url("/loop"))
        }

        assertThat(error.code).isEqualTo("NET_REDIRECT_LIMIT")
        assertThat(error.retryable).isFalse()
        assertThat(requests.get()).isEqualTo(3)
    }

    @Test
    fun `rejects a redirect to a blocked host`() {
        server.createContext("/evil") { exchange -> redirect(exchange, "http://evil.invalid/") }

        val error = assertThrows(NetworkException::class.java) {
            source(guard = rejectEvil).open(url("/evil"))
        }

        assertThat(error.code).isEqualTo("NET_HOST_NOT_ALLOWED")
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `honours Retry-After on 429`() {
        val attempts = AtomicInteger()
        server.createContext("/busy") { exchange ->
            if (attempts.incrementAndGet() <= 2) {
                requests.incrementAndGet()
                exchange.responseHeaders.add("Retry-After", "1")
                exchange.sendResponseHeaders(429, -1)
                exchange.close()
            } else {
                respond(exchange, 200, "eventually")
            }
        }
        val delays = mutableListOf<Long>()

        val bytes = source(delays = delays).open(url("/busy")).use { it.readBytes() }

        assertThat(String(bytes, Charsets.UTF_8)).isEqualTo("eventually")
        assertThat(attempts.get()).isEqualTo(3)
        assertThat(delays).containsExactly(1_000L, 1_000L).inOrder()
    }

    @Test
    fun `retries a server error with exponential backoff`() {
        server.createContext("/error") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        val delays = mutableListOf<Long>()

        val error = assertThrows(NetworkException::class.java) {
            source(maxAttempts = 3, delays = delays).open(url("/error"))
        }

        assertThat(error.code).isEqualTo("NET_RETRY_EXHAUSTED")
        assertThat(error.retryable).isTrue()
        assertThat(requests.get()).isEqualTo(3)
        assertThat(delays).containsExactly(1_000L, 2_000L).inOrder()
    }

    @Test
    fun `does not retry a missing file`() {
        server.createContext("/missing") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }

        val error = assertThrows(NetworkException::class.java) {
            source().open(url("/missing"))
        }

        assertThat(error.code).isEqualTo("NET_NOT_FOUND")
        assertThat(error.retryable).isFalse()
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `reports a non-retryable HTTP status`() {
        server.createContext("/forbidden") { exchange ->
            requests.incrementAndGet()
            exchange.sendResponseHeaders(403, -1)
            exchange.close()
        }

        val error = assertThrows(NetworkException::class.java) {
            source().open(url("/forbidden"))
        }

        assertThat(error.code).isEqualTo("NET_HTTP_STATUS")
        assertThat(error.retryable).isFalse()
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `the strict policy rejects cleartext before connecting`() {
        server.createContext("/ok") { exchange -> respond(exchange, 200, "hello") }
        val strict = HttpDownloadSource(
            guard = StrictUrlGuard(HostAllowlist(setOf("127.0.0.1"))),
            connectTimeoutMillis = 5_000,
            readTimeoutMillis = 5_000,
        )

        val error = assertThrows(NetworkException::class.java) {
            strict.open(url("/ok"))
        }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
        assertThat(requests.get()).isEqualTo(0)
    }

    private val permissive: UrlGuard = object : UrlGuard {
        override fun validate(raw: String): URI = URI(raw)

        override fun resolve(current: URI, location: String): URI =
            URI(current.resolve(location).toString())
    }

    private val rejectEvil: UrlGuard = object : UrlGuard {
        override fun validate(raw: String): URI {
            val uri = URI(raw)
            if (uri.host == "evil.invalid") {
                throw NetworkException("NET_HOST_NOT_ALLOWED", false, "blocked host")
            }
            return uri
        }

        override fun resolve(current: URI, location: String): URI =
            validate(current.resolve(location).toString())
    }
}
