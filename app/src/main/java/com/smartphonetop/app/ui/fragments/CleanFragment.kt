package com.smartphonetop.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.R
import com.smartphonetop.app.core.CacheAnalyzer
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.data.CacheReport
import com.smartphonetop.app.data.CacheSnapshot
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.FragmentCleanBinding
import com.smartphonetop.app.ui.CacheHistoryActivity
import com.smartphonetop.app.ui.CacheReportDialog
import com.smartphonetop.app.ui.MainActivity
import com.smartphonetop.app.ui.Refreshable
import com.smartphonetop.app.ui.adapters.CacheAppAdapter
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CleanFragment : Fragment(), Refreshable {

    private var _binding: FragmentCleanBinding? = null
    private val binding get() = _binding!!
    private lateinit var analyzer: CacheAnalyzer
    private var apps: List<AppEntry> = emptyList()
    private var analyzing = false
    private var analyzedAt = 0L
    private val selected = mutableSetOf<String>()
    private val statuses = mutableMapOf<String, String>()

    private var awaitingPackage: String? = null
    private var awaitingBatch = false
    private var beforeCache = 0L
    private var awaitingName = ""

    private var batchActive = false
    private var batchQueue: List<AppEntry> = emptyList()
    private var batchIndex = 0
    private var batchEstimated = 0L
    private var batchFreed = 0L
    private var batchOperations = 0
    private var batchNotPermitted = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCleanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyzer = CacheAnalyzer(requireContext())
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.grantUsage.setOnClickListener { startActivity(Permissions.usageAccessIntent(requireContext())) }
        binding.mediaShortcut.setOnClickListener { (activity as? MainActivity)?.openTab(R.id.navFiles) }
        binding.historyButton.setOnClickListener { startActivity(Intent(requireContext(), CacheHistoryActivity::class.java)) }
        binding.scanButton.setOnClickListener { scan() }
        binding.selectAll.setOnClickListener { selectAll() }
        binding.deselectAll.setOnClickListener { deselectAll() }
        binding.cleanAllButton.setOnClickListener { confirmBatch(cacheCandidates(), all = true) }
        binding.cleanSelectedButton.setOnClickListener {
            val entries = apps.filter { selected.contains(it.packageName) && it.isQueryable && (it.cacheBytes ?: 0L) > 0 }
            confirmBatch(entries, all = false)
        }
        SmartStore.lastCacheSnapshot()?.let {
            if (it.timestamp > analyzedAt) analyzedAt = it.timestamp
        }
        render()
        if (arguments?.getBoolean(ARG_AUTO_SCAN) == true && Permissions.hasUsageAccess(requireContext())) {
            binding.root.post { scan() }
        } else if (Permissions.hasUsageAccess(requireContext()) && apps.isEmpty()) {
            scan()
        }
    }

    override fun onResume() {
        super.onResume()
        val target = awaitingPackage
        if (target != null) {
            awaitingPackage = null
            verifyReturned(target, awaitingBatch)
        } else if (Permissions.hasUsageAccess(requireContext()) && apps.isEmpty() && !analyzing) {
            scan()
        }
        render()
    }

    override fun refresh() = render()

    private fun cacheCandidates(): List<AppEntry> =
        apps.filter { it.isQueryable && (it.cacheBytes ?: 0L) > 0 }

    private fun scan() {
        if (analyzing) return
        if (!Permissions.hasUsageAccess(requireContext())) {
            render()
            return
        }
        analyzing = true
        binding.progress.visibility = View.VISIBLE
        binding.progress.progress = 0
        binding.statusText.text = "Analisando aplicativos instalados..."
        render()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                analyzer.analyzeAllApps { current, total ->
                    activity?.runOnUiThread {
                        if (_binding != null) {
                            binding.progress.progress = if (total > 0) current * 100 / total else 0
                            binding.statusText.text = "$current de $total apps analisados"
                        }
                    }
                }
            }
            analyzing = false
            apps = result
            analyzedAt = System.currentTimeMillis()
            selected.clear()
            statuses.clear()
            val total = apps.filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L }
            SmartStore.saveCacheSnapshot(
                CacheSnapshot(
                    timestamp = analyzedAt,
                    totalCacheBytes = total,
                    analyzedApps = apps.size,
                    appsWithCache = apps.count { (it.cacheBytes ?: 0L) > 0 }
                )
            )
            binding.progress.visibility = View.GONE
            binding.statusText.text = "${apps.size} apps • ${apps.count { (it.cacheBytes ?: 0L) > 0 }} com cache mensurável"
            render()
        }
    }

    private fun render() {
        if (_binding == null) return
        val hasAccess = Permissions.hasUsageAccess(requireContext())
        binding.permissionCard.visibility = if (hasAccess) View.GONE else View.VISIBLE
        val total = apps.filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L }
        binding.totalCache.text = formatBytes(total)
        binding.appCount.text = apps.size.toString()
        binding.withCacheCount.text = apps.count { (it.cacheBytes ?: 0L) > 0 }.toString()
        binding.lastAnalysis.text = if (analyzedAt > 0L)
            "Última análise: ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(analyzedAt))}"
        else "Nenhuma análise realizada ainda"
        binding.scanButton.text = if (apps.isEmpty()) "Analisar todos os apps" else "Atualizar análise"
        binding.scanButton.isEnabled = hasAccess && !analyzing && !batchActive
        binding.scanButton.alpha = if (binding.scanButton.isEnabled) 1f else 0.5f
        val selectedBytes = apps.filter { selected.contains(it.packageName) }.sumOf { it.cacheBytes ?: 0L }
        binding.selectedCount.text = if (selected.isEmpty()) "0 selecionado(s)"
        else "${selected.size} selecionado(s) • ${formatBytes(selectedBytes)}"
        binding.selectAll.isEnabled = cacheCandidates().isNotEmpty() && !analyzing
        binding.deselectAll.isEnabled = selected.isNotEmpty() && !analyzing
        binding.cleanAllButton.isEnabled = cacheCandidates().isNotEmpty() && !analyzing && !batchActive
        binding.cleanSelectedButton.isEnabled = selected.isNotEmpty() && !analyzing && !batchActive
        binding.cleanAllButton.alpha = if (binding.cleanAllButton.isEnabled) 1f else 0.5f
        binding.cleanSelectedButton.alpha = if (binding.cleanSelectedButton.isEnabled) 1f else 0.5f
        binding.emptyState.visibility = if (apps.isEmpty() && !analyzing) View.VISIBLE else View.GONE
        binding.list.visibility = if (apps.isEmpty() && !analyzing) View.GONE else View.VISIBLE
        binding.emptyTitle.text = if (!hasAccess) "Permissão pendente" else "Nenhum app analisado"
        binding.list.adapter = CacheAppAdapter(
            apps,
            selected,
            if (analyzedAt > 0L) analyzedAt else System.currentTimeMillis(),
            statuses,
            onToggle = ::toggleSelection,
            onClean = ::cleanOne
        )
    }

    private fun selectAll() {
        cacheCandidates().forEach { selected.add(it.packageName) }
        render()
    }

    private fun deselectAll() {
        selected.clear()
        render()
    }

    private fun toggleSelection(app: AppEntry) {
        if (!app.isQueryable || (app.cacheBytes ?: 0L) <= 0) return
        if (selected.contains(app.packageName)) selected.remove(app.packageName) else selected.add(app.packageName)
        render()
    }

    private fun cleanOne(app: AppEntry) {
        if (batchActive) return
        awaitingBatch = false
        awaitingPackage = app.packageName
        awaitingName = app.label
        beforeCache = app.cacheBytes ?: 0L
        statuses[app.packageName] = "Aguardando configurações"
        render()
        openDetails(app)
    }

    private fun openDetails(app: AppEntry) {
        val details = runCatching { startActivity(Permissions.appDetailsIntent(app.packageName)) }
        if (details.isSuccess) return
        val fallback = runCatching {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS))
        }
        if (fallback.isFailure) {
            statuses[app.packageName] = "Configurações indisponíveis"
            awaitingPackage = null
            render()
            Toast.makeText(requireContext(), "Não foi possível abrir as configurações deste app", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmBatch(entries: List<AppEntry>, all: Boolean) {
        val candidates = entries.filter { it.isQueryable && (it.cacheBytes ?: 0L) > 0 }
        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum app com cache mensurável selecionado", Toast.LENGTH_SHORT).show()
            return
        }
        val estimated = candidates.sumOf { it.cacheBytes ?: 0L }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (all) "Esvaziar cache de todos?" else "Esvaziar cache dos selecionados?")
            .setMessage(
                "${candidates.size} app(s) • ${formatBytes(estimated)} estimados.\n\n" +
                    "O Android não permite que um app apague o cache de outro diretamente. " +
                    "O Smartphone Top abrirá as configurações oficiais de cada app, uma por vez. " +
                    "Toque em Armazenamento > Limpar cache e volte: o app mede o delta e continua.\n\n" +
                    "Nenhum dado pessoal, foto, vídeo, documento ou conversa é apagado."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Iniciar") { _, _ -> startBatch(candidates) }
            .show()
    }

    private fun startBatch(entries: List<AppEntry>) {
        batchActive = true
        batchQueue = entries
        batchIndex = 0
        batchEstimated = entries.sumOf { it.cacheBytes ?: 0L }
        batchFreed = 0L
        batchOperations = 0
        batchNotPermitted = 0
        statuses.clear()
        openBatchItem()
    }

    private fun openBatchItem() {
        if (!batchActive) return
        if (batchIndex >= batchQueue.size) {
            finishBatch()
            return
        }
        val app = batchQueue[batchIndex]
        awaitingBatch = true
        awaitingPackage = app.packageName
        awaitingName = app.label
        beforeCache = app.cacheBytes ?: 0L
        statuses[app.packageName] = "Aguardando configurações"
        binding.progress.visibility = View.VISIBLE
        binding.progress.progress = (batchIndex * 100) / batchQueue.size.coerceAtLeast(1)
        binding.statusText.text = "Lote ${batchIndex + 1} de ${batchQueue.size}: ${app.label}"
        render()
        val opened = runCatching { startActivity(Permissions.appDetailsIntent(app.packageName)) }
        if (opened.isFailure) {
            statuses[app.packageName] = "Configurações indisponíveis"
            batchNotPermitted++
            awaitingPackage = null
            batchIndex++
            openBatchItem()
        }
    }

    private fun verifyReturned(packageName: String, wasBatch: Boolean) {
        lifecycleScope.launch {
            val fresh = analyzer.analyzePackage(packageName)
            val after = fresh?.cacheBytes ?: 0L
            val freed = (beforeCache - after).coerceAtLeast(0L)
            apps = apps.map { if (it.packageName == packageName) fresh ?: it else it }
            statuses[packageName] = if (freed > 0) "Limpo agora: ${formatBytes(freed)}" else "Sem delta medido"
            if (freed > 0) {
                SmartStore.recordClean("Cache", freed, 1, "Cache de $awaitingName")
            }
            selected.remove(packageName)
            if (wasBatch) {
                batchOperations++
                batchFreed += freed
                batchIndex++
                binding.progress.progress = (batchIndex * 100) / batchQueue.size.coerceAtLeast(1)
                val total = apps.filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L }
                SmartStore.saveCacheSnapshot(
                    CacheSnapshot(
                        timestamp = analyzedAt,
                        totalCacheBytes = total,
                        analyzedApps = apps.size,
                        appsWithCache = apps.count { (it.cacheBytes ?: 0L) > 0 }
                    )
                )
                render()
                if (batchIndex >= batchQueue.size) {
                    finishBatch()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(awaitingName)
                        .setMessage(
                            "Antes: ${formatBytes(beforeCache)}\n" +
                                "Depois: ${formatBytes(after)}\n" +
                                "Liberado: ${formatBytes(freed)}\n\n" +
                                "Continuar para o próximo app do lote?"
                        )
                        .setNegativeButton("Encerrar e ver relatório") { _, _ -> finishBatch() }
                        .setPositiveButton("Próximo app") { _, _ -> openBatchItem() }
                        .setCancelable(false)
                        .show()
                }
            } else {
                val report = CacheReport(
                    analyzedApps = 1,
                    appsWithCache = if (fresh?.isQueryable == true) 1 else 0,
                    operations = 1,
                    notPermitted = if (fresh?.isQueryable == true) 0 else 1,
                    estimatedBefore = beforeCache,
                    freedBytes = freed,
                    summary = if (freed > 0) awaitingName
                    else "Sem delta medido em $awaitingName. O Android pode atualizar o tamanho depois."
                )
                SmartStore.addCacheReport(report)
                CacheReportDialog.newInstance(report).show(parentFragmentManager, "cache_report")
                render()
            }
        }
    }

    private fun finishBatch() {
        if (!batchActive) return
        batchActive = false
        awaitingPackage = null
        awaitingBatch = false
        binding.progress.visibility = View.GONE
        val total = apps.filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L }
        SmartStore.saveCacheSnapshot(
            CacheSnapshot(
                timestamp = analyzedAt,
                totalCacheBytes = total,
                analyzedApps = apps.size,
                appsWithCache = apps.count { (it.cacheBytes ?: 0L) > 0 }
            )
        )
        val report = CacheReport(
            analyzedApps = apps.size,
            appsWithCache = apps.count { (it.cacheBytes ?: 0L) > 0 },
            operations = batchOperations,
            notPermitted = batchNotPermitted,
            estimatedBefore = batchEstimated,
            freedBytes = batchFreed,
            summary = "${batchOperations} app(s) processado(s) no lote assistido"
        )
        SmartStore.addCacheReport(report)
        statuses.clear()
        selected.clear()
        render()
        CacheReportDialog.newInstance(report).show(parentFragmentManager, "cache_report")
        batchQueue = emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_AUTO_SCAN = "auto_scan"
        fun newInstance(autoScan: Boolean = false): CleanFragment = CleanFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_AUTO_SCAN, autoScan) }
        }
    }
}
