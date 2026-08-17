package com.dsp.ping.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dsp.ping.R
import com.dsp.ping.data.db.PingEntity
import com.dsp.ping.data.db.PingStatus
import com.dsp.ping.databinding.ItemHistoryBinding
import com.dsp.ping.databinding.ItemHistoryHeaderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * История пингов: заголовки-даты между группами одного дня и строки
 * «время, точка-индикатор статуса, задержка или текст ошибки».
 *
 * Форматирование дат вынесено в [HistoryDateFormatter] (чистое, тестируемое),
 * заголовок вставляется перед первым пингом каждого дня ([buildHistoryItems]).
 */
class HistoryAdapter : ListAdapter<HistoryItem, HistoryAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        when (viewType) {
            VIEW_TYPE_DATE_HEADER -> Holder.Date(
                ItemHistoryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> Holder.Ping(
                ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        when (holder) {
            is Holder.Date -> holder.bind(getItem(position) as HistoryItem.DateHeader)
            is Holder.Ping -> holder.bind(getItem(position) as HistoryItem.PingRow)
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryItem.DateHeader -> VIEW_TYPE_DATE_HEADER
        is HistoryItem.PingRow -> VIEW_TYPE_PING
    }

    sealed class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {

        class Date(private val binding: ItemHistoryHeaderBinding) : Holder(binding.root) {
            fun bind(item: HistoryItem.DateHeader) {
                binding.tvDate.text = item.title
            }
        }

        class Ping(private val binding: ItemHistoryBinding) : Holder(binding.root) {
            fun bind(item: HistoryItem.PingRow) {
                val ping = item.ping
                val context = binding.root.context
                binding.tvTime.text =
                    SimpleDateFormat(TIME_FORMAT, Locale.US).format(Date(ping.timestamp))
                ViewCompat.setBackgroundTintList(
                    binding.viewDot,
                    ContextCompat.getColorStateList(context, statusColorRes(ping.status))
                )
                binding.tvDetail.text = when (ping.status) {
                    PingStatus.OK -> context.getString(
                        R.string.history_latency_format,
                        ping.latencyMs ?: 0L
                    )
                    PingStatus.FAIL -> ping.error.orEmpty()
                    else -> context.getString(R.string.status_no_network)
                }
            }
        }
    }

    private companion object {
        const val TIME_FORMAT = "HH:mm:ss"

        const val VIEW_TYPE_DATE_HEADER = 0
        const val VIEW_TYPE_PING = 1

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem) =
                if (oldItem is HistoryItem.PingRow && newItem is HistoryItem.PingRow) {
                    oldItem.ping.id == newItem.ping.id
                } else {
                    // Заголовки дней уникальны по дню: сравниваем timestamp дня.
                    (oldItem is HistoryItem.DateHeader && newItem is HistoryItem.DateHeader &&
                            oldItem.timestamp == newItem.timestamp)
                }

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem) =
                oldItem == newItem
        }
    }
}
