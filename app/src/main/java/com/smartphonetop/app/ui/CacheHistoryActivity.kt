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
import com.smartphonetop.app.databinding.ActivityCacheHistoryBinding
import com.smartphonetop.app.ui.adapters.CacheReportAdapter
import com.smartphonetop.app.util.ThemeUtils

class CacheHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCacheHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivityCacheHistoryBinding.inflate(layoutInflater)
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
        val reports = SmartStore.cacheReports()
        binding.total.text = formatBytes(reports.sumOf { it.freedBytes })
        binding.count.text = if (reports.isEmpty()) "Nenhum relatório" else "${reports.size} relatório(s)"
        binding.clear.visibility = if (reports.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.list.visibility = if (reports.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.emptyState.visibility = if (reports.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.list.adapter = CacheReportAdapter(reports) { report ->
            CacheReportDialog.newInstance(report).show(supportFragmentManager, "cache_report")
        }
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar histórico de cache?")
            .setMessage("Os relatórios de cache serão removidos. O histórico geral de limpezas permanece.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                SmartStore.clearCacheReports()
                refresh()
            }
            .show()
    }
}
