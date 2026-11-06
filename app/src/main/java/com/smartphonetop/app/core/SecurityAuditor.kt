package com.smartphonetop.app.core

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.smartphonetop.app.data.SecurityCheck
import com.smartphonetop.app.data.SecurityReport
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SecurityAuditor {

    fun audit(context: Context): SecurityReport {
        val checks = mutableListOf<SecurityCheck>()
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val secureLock = keyguard?.isDeviceSecure == true
        checks.add(
            SecurityCheck(
                "Bloqueio de tela",
                if (secureLock) "PIN, senha ou biometria configurados." else "Nenhum bloqueio de tela seguro configurado.",
                secureLock,
                warning = !secureLock
            )
        )
        val adb = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1 }.getOrDefault(false)
        checks.add(SecurityCheck("Depuração USB", if (adb) "Ativada nas opções do desenvolvedor." else "Desativada.", !adb, warning = adb))
        val dev = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 }.getOrDefault(false)
        checks.add(SecurityCheck("Opções do desenvolvedor", if (dev) "Ativadas." else "Desativadas.", !dev, warning = dev))
        val unknown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else false
        checks.add(SecurityCheck("Instalação de fontes desconhecidas", if (unknown) "O Smartphone Top pode instalar APKs." else "Bloqueada para este app.", !unknown, warning = unknown))
        val patch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else ""
        val patchCurrent = runCatching {
            val date = LocalDate.parse(patch, DateTimeFormatter.ISO_LOCAL_DATE)
            date.isAfter(LocalDate.now().minusMonths(18))
        }.getOrDefault(false)
        checks.add(
            SecurityCheck(
                "Atualização de segurança",
                if (patch.isBlank()) "O fabricante não informou a data do patch." else "Patch de $patch.",
                patchCurrent,
                warning = !patchCurrent
            )
        )
        val sideloadedApps = sideloadedApps(context)
        val sideloaded = sideloadedApps.size
        checks.add(
            SecurityCheck(
                "Apps instalados fora da loja",
                if (sideloaded == 0) "Nenhum app de usuário fora da Play Store foi encontrado."
                else "$sideloaded app(s) fora da Play Store: ${sideloadedApps.take(3).joinToString { it.first }}${if (sideloaded > 3) "…" else ""}.",
                sideloaded == 0,
                warning = sideloaded > 0
            )
        )
        val score = (checks.count { it.passed } * 100) / checks.size.coerceAtLeast(1)
        return SecurityReport(checks, score)
    }

    fun sideloadedApps(context: Context): List<Pair<String, String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return runCatching {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .mapNotNull { info ->
                    val installer = context.packageManager.getInstallSourceInfo(info.packageName).installingPackageName
                    if (installer != null && installer != "com.android.vending" && installer != "com.google.android.packageinstaller") {
                        val label = context.packageManager.getApplicationLabel(info).toString()
                        label to (installer ?: "desconhecido")
                    } else null
                }.toList()
        }.getOrDefault(emptyList())
    }

    fun sensitiveAppSummary(context: Context): String {
        val dangerous = setOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_CALL_LOG
        )
        val rows = runCatching {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                .mapNotNull { info ->
                    val pkg = context.packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
                    val requested = (pkg.requestedPermissions ?: emptyArray())
                        .filter { it in dangerous }
                    if (requested.isEmpty()) null else context.packageManager.getApplicationLabel(info).toString() to requested.size
                }.sortedByDescending { it.second }.take(5).toList()
        }.getOrDefault(emptyList())
        return if (rows.isEmpty()) "Nenhum app de usuário solicita câmera, microfone, localização, contatos, SMS ou chamadas."
        else rows.joinToString(" • ") { "${it.first} (${it.second})" }
    }
}
