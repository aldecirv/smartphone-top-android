package com.smartphonetop.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.ActivityHistoryBinding
import com.smartphonetop.app.ui.adapters.HistoryAdapter
import com.smartphonetop.app.util.ThemeUtils

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        binding.back.setOnClickListener { finish() }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.clear.setOnClickListener { confirmClear() }
        refresh()
    }

    private fun refresh() {
        val history = SmartStore.history()
        binding.total.text = formatBytes(SmartStore.totalFreedBytes())
        binding.count.text = if (history.isEmpty()) "Nenhuma limpeza registrada" else "${history.size} registro(s)"
        binding.list.visibility = if (history.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.emptyState.visibility = if (history.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.clear.visibility = if (history.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.list.adapter = HistoryAdapter(history)
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar histórico?")
            .setMessage("O total liberado também será zerado. Suas listas e permissões permanecem.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                SmartStore.clearHistory()
                refresh()
            }
            .show()
    }
}
