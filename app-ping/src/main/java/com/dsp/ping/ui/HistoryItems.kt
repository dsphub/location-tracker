package com.dsp.ping.ui

import com.dsp.ping.data.db.PingEntity

/** Элементы списка истории: заголовок дня и строка пинга. */
sealed interface HistoryItem {

    /** Заголовок-дата для группы пингов одного дня ("1 сентября 2026"). */
    data class DateHeader(val timestamp: Long, val title: String) : HistoryItem

    /** Один пинг. */
    data class PingRow(val ping: PingEntity) : HistoryItem
}

/**
 * Вставляет заголовок-дату перед первым пингом каждого дня. Список ожидается
 * отсортированным по убыванию времени (позиция 0 — самый свежий пинг).
 */
fun buildHistoryItems(
    pings: List<PingEntity>,
    dayKey: (Long) -> String,
    dayTitle: (Long) -> String
): List<HistoryItem> = buildList {
    var lastDay: String? = null
    for (ping in pings) {
        val key = dayKey(ping.timestamp)
        if (key != lastDay) {
            add(HistoryItem.DateHeader(ping.timestamp, dayTitle(ping.timestamp)))
            lastDay = key
        }
        add(HistoryItem.PingRow(ping))
    }
}
