package com.dsp.ping.ping

/**
 * Период пинга в секундах: парсинг пользовательского ввода и человекочитаемое
 * форматирование. Чистые функции без android-зависимостей — тестируются на JVM.
 *
 * Границы заданы в [com.dsp.ping.data.SettingsStore]: 1 сек .. 1 день.
 */
object PingInterval {

    /**
     * Парсит ввод пользователя в период (секунды).
     *
     * - trim;
     * - целое число [min]..[max];
     * - возвращает null при нечисловом вводе или выходе за границы.
     */
    fun parse(input: String?, min: Long, max: Long): Long? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        // Отбрасываем дробные ("1.5"), плюс/пробелы и переполнение Long.
        val value = trimmed.toLongOrNull() ?: return null
        if (value < min || value > max) return null
        return value
    }

    /**
     * Человекочитаемое представление: "45 сек", "5 мин", "1 мин 30 сек",
     * "2 ч 30 мин", "1 день". Нулевые компоненты опускаются.
     */
    fun format(sec: Long): String {
        if (sec <= 0L) return "0 сек"
        if (sec == DAY_SEC) return "1 день"

        val hours = sec / HOUR_SEC
        val minutes = (sec % HOUR_SEC) / MIN_SEC
        val seconds = sec % MIN_SEC
        return buildString {
            if (hours > 0) append(hours).append(" ч")
            if (minutes > 0) {
                if (isNotEmpty()) append(' ')
                append(minutes).append(" мин")
            }
            if (seconds > 0) {
                if (isNotEmpty()) append(' ')
                append(seconds).append(" сек")
            }
        }
    }

    private const val MIN_SEC = 60L
    private const val HOUR_SEC = 60L * MIN_SEC
    private const val DAY_SEC = 24L * HOUR_SEC
}
