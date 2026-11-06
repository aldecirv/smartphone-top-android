package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.R
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.databinding.ItemCacheAppBinding

class CacheAppAdapter(
    private val items: List<AppEntry>,
    private val onClean: (AppEntry) -> Unit
) : RecyclerView.Adapter<CacheAppAdapter.Holder>() {

    inner class Holder(val binding: ItemCacheAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppEntry) {
            binding.name.text = item.label
            if (item.icon != null) binding.icon.setImageDrawable(item.icon) else binding.icon.setImageResource(R.drawable.ic_apps)
            if (item.isQueryable) {
                binding.cache.text = "Cache: ${item.formattedCache}   •   Dados: ${item.formattedData}"
                binding.reason.visibility = View.GONE
                binding.clean.isEnabled = true
                binding.clean.text = "Abrir ajustes"
                binding.clean.alpha = 1f
            } else {
                binding.cache.text = "Cache indisponível"
                binding.reason.text = item.unavailableReason
                binding.reason.visibility = if (item.unavailableReason.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.clean.isEnabled = false
                binding.clean.text = "Indisponível"
                binding.clean.alpha = 0.45f
            }
            binding.clean.setOnClickListener { if (item.isQueryable) onClean(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCacheAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
