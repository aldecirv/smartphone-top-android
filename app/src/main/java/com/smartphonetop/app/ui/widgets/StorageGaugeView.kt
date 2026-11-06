package com.smartphonetop.app.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.smartphonetop.app.R

class StorageGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var percent = 0
    private var label = "Armazenamento"
    private var sublabel = "0 B"
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1E2D4A")
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = ResourcesCompat.getFont(context, R.font.poppins_extrabold)
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8FA3C0")
        typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
        textAlign = Paint.Align.CENTER
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C7D2E5")
        typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    fun bind(percent: Int, label: String, sublabel: String) {
        this.percent = percent.coerceIn(0, 100)
        this.label = label
        this.sublabel = sublabel
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat()
        val stroke = size * 0.075f
        val pad = stroke * 1.4f
        rect.set((width - size) / 2f + pad, (height - size) / 2f + pad, (width + size) / 2f - pad, (height + size) / 2f - pad)
        track.strokeWidth = stroke
        arc.strokeWidth = stroke
        canvas.drawArc(rect, 135f, 270f, false, track)
        arc.color = when {
            percent >= 90 -> Color.parseColor("#F43F5E")
            percent >= 75 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#3B82F6")
        }
        canvas.drawArc(rect, 135f, 270f * percent / 100f, false, arc)
        valuePaint.textSize = size * 0.24f
        canvas.drawText("$percent%", width / 2f, height / 2f + valuePaint.textSize * 0.25f, valuePaint)
        labelPaint.textSize = size * 0.072f
        canvas.drawText(label, width / 2f, height / 2f - size * 0.26f, labelPaint)
        subPaint.textSize = size * 0.075f
        canvas.drawText(sublabel, width / 2f, height / 2f + size * 0.23f, subPaint)
    }
}
