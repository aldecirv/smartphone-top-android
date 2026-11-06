package com.smartphonetop.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.databinding.ActivityBatteryBinding
import com.smartphonetop.app.ui.Refreshable

class BatteryFragment : Fragment(), Refreshable {
    private var _binding: ActivityBatteryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.visibility = View.VISIBLE
        binding.back.visibility = View.GONE
        binding.openBatterySettings.setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val stats = DeviceStatsReader.read(requireContext())
        binding.level.text = "${stats.batteryLevel}%"
        binding.levelBar.progress = stats.batteryLevel
        binding.status.text = stats.batteryStatus
        binding.summary.text = "Leitura direta do BatteryManager • ${stats.batteryTechnology}"
        binding.temperature.text = "%.1f °C".format(stats.batteryTemperatureC)
        binding.voltage.text = "%.2f V".format(stats.batteryVoltageV)
        binding.technology.text = stats.batteryTechnology
        binding.health.text = stats.batteryHealth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
