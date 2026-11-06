package com.smartphonetop.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.R
import com.smartphonetop.app.data.CacheReport
import com.smartphonetop.app.data.formatBytes
import kotlin.math.max

class CacheReportDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val report = CacheReport(
            timestamp = arguments?.getLong(ARG_TIME) ?: System.currentTimeMillis(),
            analyzedApps = arguments?.getInt(ARG_ANALYZED) ?: 0,
            appsWithCache = arguments?.getInt(ARG_WITH_CACHE) ?: 0,
            operations = arguments?.getInt(ARG_OPERATIONS) ?: 0,
            notPermitted = arguments?.getInt(ARG_NOT_PERMITTED) ?: 0,
            estimatedBefore = arguments?.getLong(ARG_BEFORE) ?: 0L,
            freedBytes = arguments?.getLong(ARG_FREED) ?: 0L,
            summary = arguments?.getString(ARG_SUMMARY).orEmpty()
        )
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_cache_report, null)
        view.findViewById<TextView>(R.id.date).text = report.dateLabel
        view.findViewById<TextView>(R.id.analyzed).text = report.analyzedApps.toString()
        view.findViewById<TextView>(R.id.withCache).text = report.appsWithCache.toString()
        view.findViewById<TextView>(R.id.operations).text = report.operations.toString()
        view.findViewById<TextView>(R.id.notPermitted).text = report.notPermitted.toString()
        val maxBytes = max(report.estimatedBefore, 1L)
        val after = (report.estimatedBefore - report.freedBytes).coerceAtLeast(0L)
        view.findViewById<ProgressBar>(R.id.beforeBar).progress = ((report.estimatedBefore * 100) / maxBytes).toInt().coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.afterBar).progress = ((after * 100) / maxBytes).toInt().coerceIn(0, 100)
        view.findViewById<TextView>(R.id.beforeLabel).text = "Antes: ${formatBytes(report.estimatedBefore)}"
        view.findViewById<TextView>(R.id.afterLabel).text = "Depois (medido): ${formatBytes(after)}"
        view.findViewById<TextView>(R.id.freed).text = "${formatBytes(report.freedBytes)} liberados"
        view.findViewById<TextView>(R.id.summary).text = report.summary
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Relatório de cache")
            .setView(view)
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Histórico") { _, _ ->
                startActivity(android.content.Intent(requireContext(), CacheHistoryActivity::class.java))
            }
            .create()
    }

    companion object {
        private const val ARG_TIME = "time"
        private const val ARG_ANALYZED = "analyzed"
        private const val ARG_WITH_CACHE = "with_cache"
        private const val ARG_OPERATIONS = "operations"
        private const val ARG_NOT_PERMITTED = "not_permitted"
        private const val ARG_BEFORE = "before"
        private const val ARG_FREED = "freed"
        private const val ARG_SUMMARY = "summary"

        fun newInstance(report: CacheReport): CacheReportDialog = CacheReportDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_TIME, report.timestamp)
                putInt(ARG_ANALYZED, report.analyzedApps)
                putInt(ARG_WITH_CACHE, report.appsWithCache)
                putInt(ARG_OPERATIONS, report.operations)
                putInt(ARG_NOT_PERMITTED, report.notPermitted)
                putLong(ARG_BEFORE, report.estimatedBefore)
                putLong(ARG_FREED, report.freedBytes)
                putString(ARG_SUMMARY, report.summary)
            }
        }
    }
}
