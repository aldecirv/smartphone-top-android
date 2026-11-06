package com.smartphonetop.app.ui

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.databinding.ActivitySettingsBinding
import com.smartphonetop.app.util.Permissions
import com.smartphonetop.app.util.ThemeUtils

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        binding.back.setOnClickListener { finish() }
        binding.usageButton.setOnClickListener { startActivity(Permissions.usageAccessIntent(this)) }
        binding.mediaButton.setOnClickListener { mediaPermission.launch(Permissions.mediaPermissions()) }
        binding.openAppSettings.setOnClickListener { startActivity(Permissions.appDetailsIntent(packageName)) }
        binding.clearHistoryRow.setOnClickListener { confirmClearHistory() }
        binding.thresholdSlider.value = SmartStore.storageThreshold().toFloat()
        binding.thresholdSlider.addOnChangeListener { _, value, _ ->
            SmartStore.setStorageThreshold(value.toInt())
            binding.thresholdLabel.text = "Avisar a partir de ${value.toInt()}%"
        }
        binding.thresholdLabel.text = "Avisar a partir de ${SmartStore.storageThreshold()}%"
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val usage = Permissions.hasUsageAccess(this)
        binding.usageStatus.text = if (usage) "Permitido — leitura real de cache" else "Pendente — o cache fica indisponível"
        binding.usageButton.text = if (usage) "Revisar" else "Abrir"
        val media = Permissions.hasMediaPermission(this)
        binding.mediaStatus.text = if (media) "Permitido" else "Pendente — sem leitura de fotos/vídeos"
        binding.mediaButton.text = if (media) "Ativo" else "Permitir"
        binding.mediaButton.isEnabled = !media
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar histórico?")
            .setMessage("O total liberado será zerado. Suas permissões não mudam.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ -> SmartStore.clearHistory() }
            .show()
    }
}
