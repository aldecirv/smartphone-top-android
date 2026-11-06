package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.data.CacheReport
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.ItemCacheReportBinding

class CacheReportAdapter(
    private val items: List<CacheReport>,
    private val onClick: (CacheReport) -> Unit
) : RecyclerView.Adapter<CacheReportAdapter.Holder>() {

    inner class Holder(val binding: ItemCacheReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CacheReport) {
            binding.date.text = item.dateLabel
            binding.details.text = "${item.analyzedApps} analisados • ${item.appsWithCache} com cache • ${item.operations} operação(ões)"
            binding.result.text = "${item.resultLabel} • ${item.notPermitted} não permitida(s)"
            binding.freed.text = formatBytes(item.freedBytes)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCacheReportBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
