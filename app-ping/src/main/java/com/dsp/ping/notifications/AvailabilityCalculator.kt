package com.dsp.ping.notifications

/**
 * Чистая функция расчёта процента доступности по числу ok/fail ping-ов.
 * Записи no_network не учитываются (они не отражают состояние хоста).
 *
 * @return процент успешных пингов (0..100) или null, если нет ни ok, ни fail.
 */
object AvailabilityCalculator {

    fun percent(ok: Int, fail: Int): Int? {
        require(ok >= 0) { "ok must be >= 0" }
        require(fail >= 0) { "fail must be >= 0" }

        val total = ok + fail
        if (total == 0) return null
        return (ok * 100) / total
    }
}
