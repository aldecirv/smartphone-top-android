package com.smartphonetop.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.content.ContextCompat
import com.smartphonetop.app.R
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.core.SecurityAuditor
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.ActivityTopAiBinding
import com.smartphonetop.app.util.ThemeUtils

class TopAiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopAiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.darkSystemBars(this)
        binding = ActivityTopAiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        binding.back.setOnClickListener { finish() }
        binding.promptClean.setOnClickListener { ask("O que posso limpar?") }
        binding.promptBattery.setOnClickListener { ask("Como está a bateria?") }
        binding.promptSecurity.setOnClickListener { ask("Como está a segurança?") }
        binding.promptStorage.setOnClickListener { ask("Como está o armazenamento?") }
        binding.send.setOnClickListener { ask(binding.input.text?.toString().orEmpty()) }
        binding.input.setOnEditorActionListener { _, _, _ ->
            ask(binding.input.text?.toString().orEmpty())
            true
        }
        addBubble(
            "Olá! Sou o assistente local do Smartphone Top. Uso apenas dados reais do seu Android para explicar armazenamento, bateria e segurança. Não envio nada para a internet.",
            false
        )
    }

    private fun ask(question: String) {
        if (question.isBlank()) return
        binding.input.setText("")
        addBubble(question.trim(), true)
        addBubble(answer(question.lowercase()), false)
    }

    private fun answer(question: String): String {
        val stats = DeviceStatsReader.read(this)
        val security = SecurityAuditor.audit(this)
        return when {
            question.contains("limp") || question.contains("cache") ->
                "O Android não deixa um app apagar o cache de outro diretamente. Na aba Limpeza, o Smartphone Top mostra o cache real de cada aplicativo (com Acesso a Dados de Uso) e abre as configurações oficiais para você tocar em “Limpar cache”. O tamanho liberado é medido antes/depois e registrado no histórico."
            question.contains("bater") ->
                "Bateria em ${stats.batteryLevel}% (${stats.batteryStatus.lowercase()}), ${"%.1f".format(stats.batteryTemperatureC)} °C e ${"%.2f".format(stats.batteryVoltageV)} V. Saúde informada: ${stats.batteryHealth}. Tecnologia: ${stats.batteryTechnology}. Ciclos e capacidade de fábrica não são expostos de forma confiável pela API pública, então não inventamos esses números."
            question.contains("segur") ->
                "Sua pontuação é ${security.score}/100 com ${security.passed} de ${security.total} verificações em conformidade. ${security.warnings} ponto(s) pedem atenção. Veja detalhes em Segurança. Isso não é um antivírus: são verificações de configuração do Android."
            question.contains("armazen") || question.contains("espaço") || question.contains("memória") ->
                "Armazenamento: ${formatBytes(stats.usedStorage)} usados de ${formatBytes(stats.totalStorage)} (${stats.storagePercent}%). Livre: ${formatBytes(stats.freeStorage)}. RAM: ${stats.ramLabel}. Para liberar espaço, comece pelo cache dos apps na aba Limpeza; para mídia, use Arquivos."
            else ->
                "Resumo real: ${stats.storagePercent}% de armazenamento usado, ${stats.batteryLevel}% de bateria e segurança ${security.score}/100. Posso explicar limpeza de cache, bateria, segurança ou armazenamento."
        }
    }

    private fun addBubble(text: String, user: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@TopAiActivity, if (user) R.color.text_primary else R.color.text_secondary))
            textSize = 12.5f
            setLineSpacing(0f, 1.1f)
            setPadding(14, 11, 14, 11)
            background = ContextCompat.getDrawable(
                this@TopAiActivity,
                if (user) R.drawable.bg_bubble_user else R.drawable.bg_bubble_ai
            )
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 8
            if (user) {
                gravity = Gravity.END
                marginStart = 44
            } else {
                gravity = Gravity.START
                marginEnd = 32
            }
        }
        binding.messages.addView(bubble, params)
        binding.scroll.post { binding.scroll.fullScroll(android.view.View.FOCUS_DOWN) }
    }
}
