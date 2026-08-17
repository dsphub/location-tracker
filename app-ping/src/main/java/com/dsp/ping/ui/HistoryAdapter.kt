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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * История пингов: время, точка-индикатор статуса, задержка или текст ошибки.
 */
class HistoryAdapter : ListAdapter<PingEntity, HistoryAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PingEntity) {
            val context = binding.root.context
            binding.tvTime.text =
                SimpleDateFormat(TIME_FORMAT, Locale.US).format(Date(item.timestamp))
            ViewCompat.setBackgroundTintList(
                binding.viewDot,
                ContextCompat.getColorStateList(context, statusColorRes(item.status))
            )
            binding.tvDetail.text = when (item.status) {
                PingStatus.OK -> context.getString(
                    R.string.history_latency_format,
                    item.latencyMs ?: 0L
                )
                PingStatus.FAIL -> item.error.orEmpty()
                else -> context.getString(R.string.status_no_network)
            }
        }

        private companion object {
            const val TIME_FORMAT = "HH:mm:ss"
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PingEntity>() {
            override fun areItemsTheSame(oldItem: PingEntity, newItem: PingEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PingEntity, newItem: PingEntity) =
                oldItem == newItem
        }
    }
}
