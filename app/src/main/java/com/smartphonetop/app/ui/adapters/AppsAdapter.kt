package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.R
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.databinding.ItemAppBinding

class AppsAdapter(
    private val items: List<AppEntry>,
    private val onOpen: (AppEntry) -> Unit,
    private val onInfo: (AppEntry) -> Unit,
    private val onUninstall: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppsAdapter.Holder>() {

    inner class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppEntry) {
            binding.name.text = item.label
            binding.packageName.text = item.packageName
            binding.sizes.text = if (item.isQueryable)
                "App ${item.formattedAppSize}  •  Dados ${item.formattedData}  •  Cache ${item.formattedCache}"
            else "Tamanho indisponível"
            if (item.icon != null) binding.icon.setImageDrawable(item.icon) else binding.icon.setImageResource(R.drawable.ic_apps)
            binding.root.setOnClickListener { onOpen(item) }
            binding.menu.setOnClickListener { anchor ->
                PopupMenu(anchor.context, anchor).apply {
                    menu.add(if (item.isSystem) "App do sistema" else "Desinstalar")
                    menu.add("Informações do app")
                    menu.add("Abrir")
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title.toString()) {
                            "Desinstalar" -> onUninstall(item)
                            "Informações do app" -> onInfo(item)
                            "Abrir" -> onOpen(item)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
