package com.dsp.androidsample.ui.history.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsp.androidsample.R

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.EventHolder>() {
    private val events = mutableListOf<EventItem>()

    fun setData(events: List<EventItem>) {
        this.events.clear()
        this.events.addAll(events)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventHolder(view)
    }

    override fun getItemCount(): Int {
        return events.size
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        val event = events[position]
        holder.textView.text = event.toString()
    }

    class EventHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView = itemView.findViewById<TextView>(R.id.textView_history_item)
    }
}