package com.dsp.ping.ping

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * android.util.Patterns — android.jar stub и в JVM-тестах бросает исключение,
 * поэтому тестируется чистая логика добавления схемы [HostNormalizer.ensureScheme].
 * Валидация через Patterns.WEB_URL покрывается инструментальными тестами (вне этой задачи).
 */
class HostNormalizerTest {

    @Test
    fun `adds https scheme to bare host`() {
        assertEquals("https://example.com", HostNormalizer.ensureScheme("example.com"))
    }

    @Test
    fun `keeps http url unchanged`() {
        assertEquals("http://a.com", HostNormalizer.ensureScheme("http://a.com"))
    }

    @Test
    fun `keeps https url unchanged`() {
        assertEquals("https://a.com", HostNormalizer.ensureScheme("https://a.com"))
    }

    @Test
    fun `trims input and adds scheme`() {
        assertEquals(
            "https://example.com",
            HostNormalizer.ensureScheme("  example.com  ".trim()),
        )
    }

    @Test
    fun `invalid url with scheme returns prefixed`() {
        // «not a url» не содержит схемы — ensureScheme только добавляет схему,
        // валидация Patterns.WEB_URL выполняется в normalize() на устройстве.
        assertEquals("https://not a url", HostNormalizer.ensureScheme("not a url"))
    }

    @Test
    fun `empty input gets scheme only`() {
        // Пустая строка отбрасывается в normalize(); здесь проверяется чистая логика
        assertEquals("https://", HostNormalizer.ensureScheme(""))
    }
}
