package com.smartphonetop.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.smartphonetop.app.data.DeviceStats

object DeviceStatsReader {

    fun read(context: Context): DeviceStats {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        memory.getMemoryInfo(memInfo)

        val batteryIntent = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Não informado"
        val health = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Desgastada"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            else -> "Não informada"
        }
        val batteryStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Cheia"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Sem carga"
            else -> "Desconhecido"
        }
        return DeviceStats(
            totalStorage = total,
            freeStorage = free,
            usedStorage = (total - free).coerceAtLeast(0),
            totalRam = memInfo.totalMem,
            availableRam = memInfo.availMem,
            batteryLevel = percent,
            charging = charging,
            batteryTemperatureC = temperature,
            batteryVoltageV = voltage,
            batteryTechnology = technology,
            batteryHealth = health,
            batteryStatus = batteryStatus,
            model = if (Build.MODEL.equals("robolectric", true) || Build.MODEL.equals("unknown", true)) "Android" else Build.MODEL,
            manufacturer = if (Build.MANUFACTURER.equals("unknown", true)) "Dispositivo" else Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH.ifBlank { "Não informado" } else "Não informado"
        )
    }
}
