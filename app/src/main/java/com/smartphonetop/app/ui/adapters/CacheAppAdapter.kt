package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.R
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.ItemCacheAppBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CacheAppAdapter(
    private val items: List<AppEntry>,
    private val selectedPackages: Set<String>,
    private val analyzedAt: Long,
    private val statuses: Map<String, String>,
    private val onToggle: (AppEntry) -> Unit,
    private val onClean: (AppEntry) -> Unit
) : RecyclerView.Adapter<CacheAppAdapter.Holder>() {

    inner class Holder(val binding: ItemCacheAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppEntry) {
            binding.name.text = item.label
            binding.packageName.text = item.packageName
            if (item.icon != null) binding.icon.setImageDrawable(item.icon) else binding.icon.setImageResource(R.drawable.ic_apps)
            val cache = item.cacheBytes
            binding.cache.text = when {
                item.isQueryable && cache != null -> "Cache: ${formatBytes(cache)}  •  Ocupa: ${formatBytes((item.appBytes ?: 0) + (item.dataBytes ?: 0))}"
                else -> "Cache indisponível  •  ${item.unavailableReason ?: "sem dados do sistema"}"
            }
            val time = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(analyzedAt))
            binding.details.text = "${item.category}  •  analisado às $time"
            binding.status.text = statuses[item.packageName] ?: when {
                !item.isQueryable -> "Sem acesso ao cache"
                (cache ?: 0L) <= 0L -> "Sem cache mensurável"
                else -> "Pronto para limpeza assistida"
            }
            binding.status.setTextColor(binding.root.context.getColor(
                when {
                    statuses[item.packageName]?.contains("Limpo", true) == true -> R.color.emerald
                    statuses[item.packageName]?.contains("Aguardando", true) == true -> R.color.amber
                    !item.isQueryable -> R.color.amber
                    else -> R.color.text_tertiary
                }
            ))
            val selectable = item.isQueryable && (item.cacheBytes ?: 0L) > 0
            binding.selectBox.isChecked = selectedPackages.contains(item.packageName)
            binding.selectBox.isEnabled = selectable
            binding.selectBox.alpha = if (selectable) 1f else 0.35f
            binding.root.setOnClickListener { if (selectable) onToggle(item) }
            binding.clean.setOnClickListener { onClean(item) }
            binding.clean.text = if (item.isQueryable) "Esvaziar" else "Ajustes"
            binding.clean.isEnabled = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCacheAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
