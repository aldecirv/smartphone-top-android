package com.smartphonetop.app.data

import android.graphics.drawable.Drawable
import java.util.Locale

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val appBytes: Long? = null,
    val dataBytes: Long? = null,
    val cacheBytes: Long? = null,
    val isQueryable: Boolean = true,
    val unavailableReason: String? = null,
    val lastUsed: Long? = null,
    val versionName: String = "",
    val installer: String? = null,
    val permissions: List<String> = emptyList()
) {
    val formattedCache: String get() = formatBytes(cacheBytes)
    val formattedAppSize: String get() = formatBytes(appBytes)
    val formattedData: String get() = formatBytes(dataBytes)
    val lastUsedLabel: String
        get() {
            val used = lastUsed ?: return "Uso não informado"
            if (used <= 0L) return "Nunca usado"
            val days = ((System.currentTimeMillis() - used) / 86_400_000L).toInt()
            return if (days <= 0) "Usado hoje" else "Sem uso há $days dia(s)"
        }
    val isUnused: Boolean
        get() = lastUsed != null && (lastUsed <= 0L || (System.currentTimeMillis() - lastUsed) > 30L * 86_400_000L)
}

data class MediaEntry(
    val uri: android.net.Uri,
    val name: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val mime: String,
    val kind: MediaKind
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
    val dateLabel: String get() = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        .format(java.util.Date(dateAdded * 1000L))
}

enum class MediaKind(val label: String) {
    IMAGE("Imagem"), VIDEO("Vídeo"), AUDIO("Áudio"), DOCUMENT("Documento"), OTHER("Arquivo")
}

data class HistoryRecord(
    val id: String,
    val timestamp: Long,
    val type: String,
    val freedBytes: Long,
    val itemsCount: Int,
    val summary: String
) {
    val dateLabel: String get() = java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        .format(java.util.Date(timestamp))
}

data class DeviceStats(
    val totalStorage: Long,
    val freeStorage: Long,
    val usedStorage: Long,
    val totalRam: Long,
    val availableRam: Long,
    val batteryLevel: Int,
    val charging: Boolean,
    val batteryTemperatureC: Float,
    val batteryVoltageV: Float,
    val batteryTechnology: String,
    val batteryHealth: String,
    val batteryStatus: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val securityPatch: String
) {
    val storagePercent: Int
        get() = if (totalStorage <= 0) 0 else ((usedStorage * 100) / totalStorage).toInt().coerceIn(0, 100)
    val ramPercent: Int
        get() = if (totalRam <= 0) 0 else (((totalRam - availableRam) * 100) / totalRam).toInt().coerceIn(0, 100)
    val usedRam: Long get() = (totalRam - availableRam).coerceAtLeast(0)
    val storageLabel: String get() = "${formatBytes(usedStorage)} de ${formatBytes(totalStorage)}"
    val freeLabel: String get() = formatBytes(freeStorage)
    val ramLabel: String get() = "${formatBytes(usedRam)} / ${formatBytes(totalRam)}"
    val deviceName: String get() = "$manufacturer $model".trim()
}

data class SecurityCheck(
    val title: String,
    val description: String,
    val passed: Boolean,
    val warning: Boolean = false,
    val detail: String = ""
)

data class SecurityReport(
    val checks: List<SecurityCheck>,
    val score: Int
) {
    val passed: Int get() = checks.count { it.passed }
    val total: Int get() = checks.size
    val warnings: Int get() = checks.count { !it.passed }
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "Indisponível"
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes < kb -> "$bytes B"
        bytes < mb -> String.format(Locale("pt", "BR"), "%.1f KB", bytes / kb)
        bytes < gb -> String.format(Locale("pt", "BR"), "%.1f MB", bytes / mb)
        else -> String.format(Locale("pt", "BR"), "%.2f GB", bytes / gb)
    }
}
