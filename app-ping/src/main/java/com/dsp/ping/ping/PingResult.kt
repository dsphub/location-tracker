package com.dsp.ping.ping

import com.dsp.ping.data.db.PingStatus
import com.dsp.ping.icons.IconStatus

/**
 * Результат пинга.
 *
 * - [Ok] — получен любой HTTP-ответ (включая 4xx/5xx): сервер отвечает.
 * - [Fail] — IOException/таймаут; [error] содержит описание.
 * - [NoNetwork] — нет активного сетевого подключения.
 */
sealed class PingResult {
    data class Ok(val latencyMs: Long) : PingResult()
    data class Fail(val error: String) : PingResult()
    object NoNetwork : PingResult()
}

/** Маппинг [PingResult] в строковую константу из [PingStatus]. */
fun PingResult.toStatus(): String = when (this) {
    is PingResult.Ok -> PingStatus.OK
    is PingResult.Fail -> PingStatus.FAIL
    PingResult.NoNetwork -> PingStatus.NO_NETWORK
}

/** Маппинг [PingResult] в [IconStatus] для задачи по иконкам. */
fun PingResult.toIconStatus(): IconStatus = when (this) {
    is PingResult.Ok -> IconStatus.GREEN
    is PingResult.Fail -> IconStatus.RED
    PingResult.NoNetwork -> IconStatus.GRAY
}
