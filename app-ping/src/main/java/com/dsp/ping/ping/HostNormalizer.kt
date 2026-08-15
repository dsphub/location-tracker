package com.dsp.ping.ping

import android.util.Patterns

/**
 * Нормализация ввода хоста пользователем.
 *
 * Логика добавления схемы [ensureScheme] выделена отдельно и тестируется на JVM,
 * так как [Patterns.WEB_URL] — это android.jar stub, недоступный в unit-тестах.
 */
object HostNormalizer {

    /**
     * Нормализует пользовательский ввод в полный URL.
     *
     * - trim;
     * - пустая/blank строка -> null;
     * - валидация исходного (до добавления схемы) ввода через [Patterns.WEB_URL];
     * - добавление `https://`, если схема `http(s)://` отсутствует;
     * - возвращает нормализованный URL либо null.
     */
    fun normalize(input: String?): String? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (!Patterns.WEB_URL.matcher(trimmed).matches()) return null
        return ensureScheme(trimmed)
    }

    /**
     * Чистая (тестируемая на JVM) функция: добавляет `https://`, если у [input]
     * нет схемы `http://` или `https://`. Регистр схемы не учитывается.
     */
    fun ensureScheme(input: String): String =
        if (hasHttpScheme(input)) input else "https://$input"

    private fun hasHttpScheme(input: String): Boolean {
        val scheme = input.substringBefore("://", "")
        return scheme.equals("http", ignoreCase = true) ||
            scheme.equals("https", ignoreCase = true)
    }
}
