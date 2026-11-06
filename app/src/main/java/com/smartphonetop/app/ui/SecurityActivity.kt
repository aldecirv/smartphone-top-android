package com.smartphonetop.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartphonetop.app.core.SecurityAuditor
import com.smartphonetop.app.databinding.ActivitySecurityBinding
import com.smartphonetop.app.ui.adapters.SecurityAdapter
import com.smartphonetop.app.util.ThemeUtils

class SecurityActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySecurityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        binding.back.setOnClickListener { finish() }
        val report = SecurityAuditor.audit(this)
        binding.score.text = "${report.score}/100"
        binding.summary.text = "${report.passed} de ${report.total} verificações em conformidade"
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = SecurityAdapter(report.checks)
        val sideloaded = SecurityAuditor.sideloadedApps(this)
        binding.sideloadDetails.text = if (sideloaded.isEmpty()) "Apps fora da Play Store: nenhum encontrado."
        else "Apps fora da Play Store: " + sideloaded.take(4).joinToString { it.first } + if (sideloaded.size > 4) "…" else ""
        binding.permissionDetails.text = "Permissões sensíveis solicitadas: " + SecurityAuditor.sensitiveAppSummary(this)
    }
}
