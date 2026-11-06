package com.smartphonetop.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.R
import com.smartphonetop.app.core.CacheAnalyzer
import com.smartphonetop.app.core.MediaScanner
import com.smartphonetop.app.data.MediaKind
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeepScanDialog : DialogFragment() {

    private var cacheResult: TextView? = null
    private var largeResult: TextView? = null
    private var screenshotResult: TextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_deep_scan, null)
        cacheResult = view.findViewById(R.id.cacheResult)
        largeResult = view.findViewById(R.id.largeResult)
        screenshotResult = view.findViewById(R.id.screenshotResult)
        val status = view.findViewById<TextView>(R.id.statusText)
        val progress = view.findViewById<android.widget.ProgressBar>(R.id.progress)
        val hint = view.findViewById<TextView>(R.id.permissionHint)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Análise do aparelho")
            .setView(view)
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Arquivos", null)
            .setPositiveButton("Limpeza", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dismiss()
                (activity as? MainActivity)?.openTab(R.id.navClean)
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dismiss()
                (activity as? MainActivity)?.openTab(R.id.navFiles)
            }
        }
        lifecycleScope.launch {
            val hasUsage = Permissions.hasUsageAccess(requireContext())
            val hasMedia = Permissions.hasMediaPermission(requireContext())
            if (!hasUsage || !hasMedia) {
                hint.visibility = View.VISIBLE
                hint.text = buildString {
                    if (!hasUsage) append("Conceda Acesso a Dados de Uso para medir o cache. ")
                    if (!hasMedia) append("Conceda acesso à mídia para analisar fotos e vídeos.")
                }
            }
            status.text = "Analisando cache e mídia..."
            val cache = if (hasUsage) withContext(Dispatchers.IO) {
                CacheAnalyzer(requireContext()).analyzeAllApps().filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L }
            } else null
            val media = if (hasMedia) withContext(Dispatchers.IO) { MediaScanner.scan(requireContext()) } else emptyList()
            val now = System.currentTimeMillis()
            val large = media.filter { it.sizeBytes > 50L * 1024 * 1024 }
            val oldScreenshots = media.filter {
                it.kind == MediaKind.IMAGE &&
                    (it.name.contains("screenshot", true) || it.name.contains("captura", true)) &&
                    now - it.dateAdded * 1000L > TimeUnit.DAYS.toMillis(30)
            }
            cacheResult?.text = cache?.let { formatBytes(it) } ?: "Sem permissão"
            largeResult?.text = if (hasMedia) "${large.size} • ${formatBytes(large.sumOf { it.sizeBytes })}" else "Sem permissão"
            screenshotResult?.text = if (hasMedia) "${oldScreenshots.size} • ${formatBytes(oldScreenshots.sumOf { it.sizeBytes })}" else "Sem permissão"
            status.text = "Análise concluída."
            progress.isIndeterminate = false
            progress.progress = 100
        }
        return dialog
    }
}
