package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.data.HistoryRecord
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.ItemHistoryBinding

class HistoryAdapter(private val items: List<HistoryRecord>) : RecyclerView.Adapter<HistoryAdapter.Holder>() {
    inner class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryRecord) {
            binding.title.text = item.type
            binding.summary.text = item.summary
            binding.date.text = item.dateLabel
            binding.freed.text = formatBytes(item.freedBytes)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
