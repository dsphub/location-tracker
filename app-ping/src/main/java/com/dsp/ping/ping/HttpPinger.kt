package com.dsp.ping.ping

import android.os.SystemClock
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP-пингер на [HttpURLConnection].
 *
 * Метод: HEAD с fallback на GET при responseCode 405 или 501.
 * `connectTimeout = readTimeout = 10_000`, `instanceFollowRedirects = false`.
 *
 * Семантика результата:
 * - [PingResult.Ok] — получен любой HTTP status (включая 4xx/5xx — сервер отвечает);
 *   [latencyMs] замеряется [SystemClock.elapsedRealtime] вокруг connect + чтения responseCode;
 * - [PingResult.Fail] — [IOException]/таймаут; [PingResult.Fail.error] = `e.message ?: e.javaClass.simpleName`.
 */
class HttpPinger {

    fun ping(host: String): PingResult {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(host, METHOD_HEAD)

            var latencyMs = measureRoundTrip(connection)

            val code = connection.responseCode
            if (code == HTTP_METHOD_NOT_ALLOWED || code == HTTP_NOT_IMPLEMENTED) {
                connection.disconnect()
                connection = openConnection(host, METHOD_GET)
                latencyMs = measureRoundTrip(connection)
            }

            return PingResult.Ok(latencyMs)
        } catch (e: IOException) {
            return PingResult.Fail(e.message ?: e.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private fun openConnection(host: String, method: String): HttpURLConnection =
        (URL(host).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            requestMethod = method
        }

    /** Замеряет elapsedRealtime вокруг connect + чтения responseCode. */
    private fun measureRoundTrip(connection: HttpURLConnection): Long {
        val start = SystemClock.elapsedRealtime()
        connection.connect()
        connection.responseCode
        return SystemClock.elapsedRealtime() - start
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val METHOD_HEAD = "HEAD"
        const val METHOD_GET = "GET"
        const val HTTP_METHOD_NOT_ALLOWED = 405
        const val HTTP_NOT_IMPLEMENTED = 501
    }
}
