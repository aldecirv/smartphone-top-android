package com.smartphonetop.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.databinding.ActivityBatteryBinding
import com.smartphonetop.app.util.ThemeUtils

class BatteryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBatteryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivityBatteryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        binding.back.setOnClickListener { finish() }
        binding.openBatterySettings.setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
        }
        val stats = DeviceStatsReader.read(this)
        binding.level.text = "${stats.batteryLevel}%"
        binding.levelBar.progress = stats.batteryLevel
        binding.status.text = stats.batteryStatus
        binding.summary.text = "Leitura direta do BatteryManager • ${stats.batteryTechnology}"
        binding.temperature.text = "%.1f °C".format(stats.batteryTemperatureC)
        binding.voltage.text = "%.2f V".format(stats.batteryVoltageV)
        binding.technology.text = stats.batteryTechnology
        binding.health.text = stats.batteryHealth
    }
}
