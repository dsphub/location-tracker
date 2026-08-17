package com.dsp.ping.ui

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * Форматирование дат истории: «1 сентября 2026». Чистая JVM-логика.
 * [DateFormatSymbols.months] даёт родительный падеж месяца, корректный с числом.
 */
object HistoryDateFormatter {

    /** Заголовок дня: "1 сентября 2026". */
    fun dayTitle(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance(locale)
        calendar.timeInMillis = timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = DateFormatSymbols(locale).months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$day $month $year"
    }

    /** Ключ дня для группировки: «2026-09-01». */
    fun dayKey(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance(locale)
        calendar.timeInMillis = timestamp
        val day = twoDigits(calendar.get(Calendar.DAY_OF_MONTH))
        val month = twoDigits(calendar.get(Calendar.MONTH) + 1)
        return "${calendar.get(Calendar.YEAR)}-$month-$day"
    }

    private fun twoDigits(value: Int): String =
        if (value < 10) "0$value" else value.toString()
}
