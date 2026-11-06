package com.smartphonetop.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.FragmentMoreBinding
import com.smartphonetop.app.ui.BatteryActivity
import com.smartphonetop.app.ui.HistoryActivity
import com.smartphonetop.app.ui.SecurityActivity
import com.smartphonetop.app.ui.SettingsActivity
import com.smartphonetop.app.ui.TopAiActivity

class MoreFragment : Fragment() {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stats = DeviceStatsReader.read(requireContext())
        binding.deviceName.text = "${stats.manufacturer} ${stats.model}"
        binding.deviceAndroid.text = stats.androidVersion
        binding.devicePatch.text = "Patch de segurança: ${stats.securityPatch}"
        binding.deviceStorage.text = "Armazenamento: ${formatBytes(stats.usedStorage)} de ${formatBytes(stats.totalStorage)}"
        binding.batteryCard.setOnClickListener { startActivity(Intent(requireContext(), BatteryActivity::class.java)) }
        binding.securityCard.setOnClickListener { startActivity(Intent(requireContext(), SecurityActivity::class.java)) }
        binding.aiCard.setOnClickListener { startActivity(Intent(requireContext(), TopAiActivity::class.java)) }
        binding.historyCard.setOnClickListener { startActivity(Intent(requireContext(), HistoryActivity::class.java)) }
        binding.settingsCard.setOnClickListener { startActivity(Intent(requireContext(), SettingsActivity::class.java)) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
