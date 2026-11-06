package com.smartphonetop.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartphonetop.app.R
import com.smartphonetop.app.data.SecurityCheck
import com.smartphonetop.app.databinding.ItemSecurityCheckBinding

class SecurityAdapter(private val items: List<SecurityCheck>) : RecyclerView.Adapter<SecurityAdapter.Holder>() {
    inner class Holder(val binding: ItemSecurityCheckBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SecurityCheck) {
            binding.title.text = item.title
            binding.description.text = item.description
            if (item.passed) {
                binding.iconHolder.setBackgroundResource(R.drawable.bg_badge_green)
                binding.icon.setImageResource(R.drawable.ic_check)
                binding.icon.setColorFilter(binding.root.context.getColor(R.color.emerald))
                binding.status.text = "OK"
                binding.status.setTextColor(binding.root.context.getColor(R.color.emerald))
                binding.status.setBackgroundResource(R.drawable.bg_badge_green)
            } else {
                binding.iconHolder.setBackgroundResource(R.drawable.bg_badge_red)
                binding.icon.setImageResource(R.drawable.ic_warning)
                binding.icon.setColorFilter(binding.root.context.getColor(R.color.rose))
                binding.status.text = "ATENÇÃO"
                binding.status.setTextColor(binding.root.context.getColor(R.color.rose))
                binding.status.setBackgroundResource(R.drawable.bg_badge_red)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSecurityCheckBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
