package com.smartphonetop.app.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.R
import com.smartphonetop.app.data.MediaEntry
import com.smartphonetop.app.data.MediaKind
import com.smartphonetop.app.databinding.ItemMediaBinding

class MediaAdapter(
    private val items: List<MediaEntry>,
    private val selectionMode: Boolean,
    private val selected: Set<Uri>,
    private val onOpen: (MediaEntry) -> Unit,
    private val onToggle: (MediaEntry) -> Unit,
    private val onDelete: (MediaEntry) -> Unit
) : RecyclerView.Adapter<MediaAdapter.Holder>() {

    inner class Holder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaEntry) {
            binding.name.text = item.name
            binding.meta.text = "${item.kind.label}  •  ${item.formattedSize}  •  ${item.dateLabel}"
            binding.selectBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.selectBox.isChecked = selected.contains(item.uri)
            binding.menu.visibility = if (selectionMode) View.GONE else View.VISIBLE
            binding.icon.scaleType = android.widget.ImageView.ScaleType.CENTER
            if (item.kind == MediaKind.IMAGE) {
                binding.icon.setColorFilter(null)
                runCatching { binding.icon.setImageURI(item.uri) }
                    .onFailure { binding.icon.setImageResource(R.drawable.ic_image) }
            } else {
                val (icon, color) = when (item.kind) {
                    MediaKind.VIDEO -> R.drawable.ic_video to R.color.purple
                    MediaKind.AUDIO -> R.drawable.ic_music to R.color.emerald
                    else -> R.drawable.ic_doc to R.color.cyan
                }
                binding.icon.setImageResource(icon)
                binding.icon.setColorFilter(ContextCompat.getColor(binding.root.context, color))
            }
            binding.root.setOnClickListener {
                if (selectionMode) onToggle(item) else onOpen(item)
            }
            binding.root.setOnLongClickListener {
                if (!selectionMode) onToggle(item)
                true
            }
            binding.menu.setOnClickListener { anchor ->
                if (selectionMode) return@setOnClickListener
                PopupMenu(anchor.context, anchor).apply {
                    menu.add("Abrir")
                    menu.add("Excluir")
                    setOnMenuItemClickListener {
                        if (it.title == "Excluir") onDelete(item) else onOpen(item)
                        true
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
