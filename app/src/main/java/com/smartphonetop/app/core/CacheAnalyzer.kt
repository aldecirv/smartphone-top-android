package com.smartphonetop.app.core

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class CacheAnalyzer(private val context: Context) {

    private val packageManager = context.packageManager

    suspend fun analyzeAllApps(onProgress: (Int, Int) -> Unit = { _, _ -> }): List<AppEntry> = withContext(Dispatchers.IO) {
        val apps = runCatching {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != context.packageName }
        }.getOrDefault(emptyList())
        val hasAccess = Permissions.hasUsageAccess(context)
        val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val result = mutableListOf<AppEntry>()
        apps.forEachIndexed { index, info ->
            result.add(query(info, hasAccess, statsManager, storageManager))
            onProgress(index + 1, apps.size)
        }
        result.sortedWith(compareByDescending<AppEntry> { it.isQueryable }.thenByDescending { it.cacheBytes ?: 0L })
    }

    suspend fun analyzePackage(packageName: String): AppEntry? = withContext(Dispatchers.IO) {
        val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull() ?: return@withContext null
        query(
            info,
            Permissions.hasUsageAccess(context),
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager,
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        )
    }

    private fun query(
        info: ApplicationInfo,
        hasAccess: Boolean,
        statsManager: StorageStatsManager?,
        storageManager: StorageManager?
    ): AppEntry {
        val label = runCatching { packageManager.getApplicationLabel(info).toString() }.getOrDefault(info.packageName)
        val icon = runCatching { packageManager.getApplicationIcon(info) }.getOrNull()
        if (!hasAccess) {
            return AppEntry(
                info.packageName, label, icon,
                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isQueryable = false,
                unavailableReason = "Conceda Acesso a Dados de Uso para ler o cache real."
            )
        }
        if (statsManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return AppEntry(info.packageName, label, icon, isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isQueryable = false, unavailableReason = "StorageStatsManager indisponível neste Android.")
        }
        return try {
            val uuid: UUID = StorageManager.UUID_DEFAULT
            val user = UserHandle.getUserHandleForUid(info.uid)
            val stats = statsManager.queryStatsForPackage(uuid, info.packageName, user)
            AppEntry(
                packageName = info.packageName,
                label = label,
                icon = icon,
                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
                isQueryable = true
            )
        } catch (security: SecurityException) {
            AppEntry(info.packageName, label, icon, isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isQueryable = false, unavailableReason = "O fabricante bloqueou a leitura deste app.")
        } catch (error: Exception) {
            AppEntry(info.packageName, label, icon, isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isQueryable = false, unavailableReason = "O Android não disponibilizou o tamanho deste app.")
        }
    }

    private fun storageManager() = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
}
