package com.dsp.ping.ui

import androidx.annotation.ColorRes
import com.dsp.ping.R
import com.dsp.ping.data.db.PingStatus

/** Цвет статуса пинга: ok — зелёный, fail — красный, остальное — серый. */
@ColorRes
fun statusColorRes(status: String?): Int = when (status) {
    PingStatus.OK -> R.color.ping_green
    PingStatus.FAIL -> R.color.ping_red
    else -> R.color.ping_gray
}
