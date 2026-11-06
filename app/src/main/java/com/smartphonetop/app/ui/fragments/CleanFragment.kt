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
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.FragmentCleanBinding
import com.smartphonetop.app.ui.Refreshable
import com.smartphonetop.app.ui.adapters.CacheAppAdapter
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CleanFragment : Fragment(), Refreshable {

    private var _binding: FragmentCleanBinding? = null
    private val binding get() = _binding!!
    private lateinit var analyzer: CacheAnalyzer
    private var apps: List<AppEntry> = emptyList()
    private var analyzing = false
    private var awaitingPackage: String? = null
    private var beforeCache: Long = 0L
    private var awaitingName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCleanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyzer = CacheAnalyzer(requireContext())
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.grantUsage.setOnClickListener {
            startActivity(Permissions.usageAccessIntent(requireContext()))
        }
        binding.mediaShortcut.setOnClickListener { (activity as? com.smartphonetop.app.ui.MainActivity)?.openTab(R.id.navFiles) }
        binding.scanButton.setOnClickListener { scan() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        val target = awaitingPackage
        if (target != null) {
            awaitingPackage = null
            verifyCleaned(target)
        } else if (Permissions.hasUsageAccess(requireContext()) && apps.isEmpty()) {
            scan()
        }
        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val hasAccess = Permissions.hasUsageAccess(requireContext())
        binding.permissionCard.visibility = if (hasAccess) View.GONE else View.VISIBLE
        binding.scanButton.isEnabled = hasAccess && !analyzing
        binding.scanButton.alpha = if (hasAccess) 1f else 0.45f
        binding.totalCache.text = formatBytes(apps.filter { it.isQueryable }.sumOf { it.cacheBytes ?: 0L })
        binding.appCount.text = apps.size.toString()
        binding.emptyState.visibility = if (apps.isEmpty() && !analyzing) View.VISIBLE else View.GONE
        binding.list.visibility = if (apps.isEmpty() && !analyzing) View.GONE else View.VISIBLE
        binding.list.adapter = CacheAppAdapter(apps) { app -> openAppSettings(app) }
        binding.emptyTitle.text = if (!hasAccess) "Permissão pendente" else "Nada analisado ainda"
    }

    private fun scan() {
        if (analyzing) return
        if (!Permissions.hasUsageAccess(requireContext())) {
            refresh()
            return
        }
        analyzing = true
        binding.progress.visibility = View.VISIBLE
        binding.progress.progress = 0
        binding.statusText.text = "Analisando..."
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { analyzer.analyzeAllApps { current, total ->
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.progress.progress = if (total > 0) current * 100 / total else 0
                        binding.statusText.text = "$current/$total"
                    }
                }
            } }
            analyzing = false
            apps = result
            binding.progress.visibility = View.GONE
            binding.statusText.text = "Concluído"
            binding.list.adapter = CacheAppAdapter(apps) { app -> openAppSettings(app) }
            refresh()
        }
    }

    private fun openAppSettings(app: AppEntry) {
        awaitingPackage = app.packageName
        awaitingName = app.label
        beforeCache = app.cacheBytes ?: 0L
        startActivity(Permissions.appDetailsIntent(app.packageName))
    }

    private fun verifyCleaned(packageName: String) {
        lifecycleScope.launch {
            val fresh = analyzer.analyzePackage(packageName)
            val after = fresh?.cacheBytes ?: 0L
            val freed = (beforeCache - after).coerceAtLeast(0L)
            apps = apps.map { if (it.packageName == packageName) fresh ?: it else it }
            if (freed > 0) {
                SmartStore.recordClean("Cache", freed, 1, "Cache de $awaitingName")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cache liberado")
                    .setMessage("${formatBytes(freed)} liberados em $awaitingName.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Nenhuma alteração medida")
                    .setMessage("Se você tocou em “Limpar cache” nas configurações, o Android pode demorar a atualizar o tamanho. Toque em Analisar cache para atualizar.\n\nO Smartphone Top não apaga dados de outros apps diretamente.")
                    .setPositiveButton("Entendi", null)
                    .show()
            }
            binding.list.adapter = CacheAppAdapter(apps) { app -> openAppSettings(app) }
            refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
