package com.smartphonetop.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.smartphonetop.app.R
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.core.SecurityAuditor
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.FragmentHomeBinding
import com.smartphonetop.app.ui.MainActivity
import com.smartphonetop.app.ui.Refreshable
import com.smartphonetop.app.ui.DeepScanDialog
import com.smartphonetop.app.ui.SecurityActivity
import com.smartphonetop.app.ui.BatteryActivity
import com.smartphonetop.app.ui.TopAiActivity
import java.util.Calendar

class HomeFragment : Fragment(), Refreshable {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var unsubscribe: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scanButton.setOnClickListener { DeepScanDialog().show(parentFragmentManager, "deep_scan") }
        binding.quickClean.setOnClickListener { (activity as? MainActivity)?.openTab(R.id.navClean) }
        binding.quickApps.setOnClickListener { (activity as? MainActivity)?.openTab(R.id.navApps) }
        binding.quickFiles.setOnClickListener { (activity as? MainActivity)?.openTab(R.id.navFiles) }
        binding.quickSecurity.setOnClickListener { startActivity(android.content.Intent(requireContext(), SecurityActivity::class.java)) }
        binding.quickBattery.setOnClickListener { (activity as? MainActivity)?.openTab(R.id.navBattery) }
        binding.quickAi.setOnClickListener { startActivity(android.content.Intent(requireContext(), TopAiActivity::class.java)) }
        binding.cacheAnalyze.setOnClickListener { (activity as? MainActivity)?.openCacheScan() }
        binding.cacheCard.setOnClickListener { (activity as? MainActivity)?.openCacheScan() }
        unsubscribe = SmartStore.subscribe { activity?.runOnUiThread { refresh() } }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val stats = DeviceStatsReader.read(requireContext())
        val security = SecurityAuditor.audit(requireContext())
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.greeting.text = when {
            hour < 12 -> "Bom dia!"
            hour < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }
        binding.deviceLine.text = "${stats.manufacturer} ${stats.model}  •  dados reais do Android"
        binding.savedTotal.text = formatBytes(SmartStore.totalFreedBytes())
        binding.gauge.bind(stats.storagePercent, "Armazenamento", stats.storageLabel)
        binding.usedStorage.text = formatBytes(stats.usedStorage)
        binding.freeStorage.text = formatBytes(stats.freeStorage)
        binding.deviceStorage.text = formatBytes(stats.totalStorage)
        binding.ramValue.text = stats.ramLabel
        binding.ramBar.progress = stats.ramPercent
        binding.batteryValue.text = "${stats.batteryLevel}%  ${stats.batteryStatus}"
        binding.batteryBar.progress = stats.batteryLevel
        binding.tempValue.text = "%.1f °C".format(stats.batteryTemperatureC)
        binding.securityValue.text = "${security.score}/100"
        binding.securityValue.setTextColor(
            ContextCompat.getColor(requireContext(), if (security.score >= 80) R.color.emerald else R.color.amber)
        )
        val threshold = SmartStore.storageThreshold()
        binding.warningCard.visibility = if (stats.storagePercent >= threshold) View.VISIBLE else View.GONE
        binding.warningText.text = "Armazenamento em ${stats.storagePercent}% (limite de $threshold%). Libere espaço quando puder."
        val cache = SmartStore.lastCacheSnapshot()
        binding.cacheIndicator.text = cache?.let { formatBytes(it.totalCacheBytes) } ?: "0 B"
        binding.cacheLast.text = cache?.let { "Última análise: ${it.dateLabel}" } ?: "Nunca analisado"
        binding.cacheCount.text = cache?.let { "${it.analyzedApps} apps analisados • ${it.appsWithCache} com cache" } ?: "0 apps analisados"
        val last = SmartStore.history().firstOrNull()
        binding.lastClean.text = last?.let {
            "Última limpeza: ${it.dateLabel}  •  ${formatBytes(it.freedBytes)} liberados"
        } ?: "Dados reais do Android. Nada é inventado."
    }

    override fun onDestroyView() {
        unsubscribe?.invoke()
        unsubscribe = null
        super.onDestroyView()
        _binding = null
    }
}
