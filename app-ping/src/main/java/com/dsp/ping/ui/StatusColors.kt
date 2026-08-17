package com.dsp.ping.ui

import androidx.annotation.ColorRes
import com.dsp.ping.R
import com.dsp.ping.data.db.PingStatus

/**
 * Цвет статуса пинга — тот же, что у точки-индикатора в истории и иконки лаунчера:
 * ok — зелёный, fail — красный, no_network/нет данных — серый.
 */
@ColorRes
fun statusColorRes(status: String?): Int = when (status) {
    PingStatus.OK -> R.color.ping_green
    PingStatus.FAIL -> R.color.ping_red
    else -> R.color.ping_gray
}
