package com.smartphonetop.app.core

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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
        val lastUsed = lastUsedMap()
        val result = mutableListOf<AppEntry>()
        apps.forEachIndexed { index, info ->
            result.add(query(info, hasAccess, statsManager, storageManager, lastUsed[info.packageName]))
            onProgress(index + 1, apps.size)
        }
        result.sortedWith(
            compareByDescending<AppEntry> { it.isQueryable }
                .thenByDescending { it.cacheBytes ?: 0L }
        )
    }

    suspend fun analyzePackage(packageName: String): AppEntry? = withContext(Dispatchers.IO) {
        val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull() ?: return@withContext null
        query(
            info,
            Permissions.hasUsageAccess(context),
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager,
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager,
            lastUsedMap()[packageName]
        )
    }

    private fun query(
        info: ApplicationInfo,
        hasAccess: Boolean,
        statsManager: StorageStatsManager?,
        storageManager: StorageManager?,
        lastUsed: Long?
    ): AppEntry {
        val label = runCatching { packageManager.getApplicationLabel(info).toString() }.getOrDefault(info.packageName)
        val icon = runCatching { packageManager.getApplicationIcon(info) }.getOrNull()
        val pkg = runCatching {
            packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
        }.getOrNull()
        val version = pkg?.versionName.orEmpty()
        val permissions = pkg?.requestedPermissions?.toList().orEmpty()
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(info.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(info.packageName)
            }
        }.getOrNull()
        val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val category = categoryLabel(info, system)
        if (!hasAccess) {
            return AppEntry(
                info.packageName, label, icon, isSystem = system,
                isQueryable = false,
                unavailableReason = "Conceda Acesso a Dados de Uso para ler o cache real.",
                lastUsed = lastUsed, versionName = version, installer = installer,
                permissions = permissions, category = category
            )
        }
        if (statsManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return AppEntry(
                info.packageName, label, icon, isSystem = system,
                isQueryable = false, unavailableReason = "StorageStatsManager indisponível neste Android.",
                lastUsed = lastUsed, versionName = version, installer = installer,
                permissions = permissions, category = category
            )
        }
        return try {
            val uuid: UUID = StorageManager.UUID_DEFAULT
            val user = UserHandle.getUserHandleForUid(info.uid)
            val stats = statsManager.queryStatsForPackage(uuid, info.packageName, user)
            AppEntry(
                packageName = info.packageName,
                label = label,
                icon = icon,
                isSystem = system,
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
                isQueryable = true,
                lastUsed = lastUsed,
                versionName = version,
                installer = installer,
                permissions = permissions,
                category = category
            )
        } catch (security: SecurityException) {
            AppEntry(
                info.packageName, label, icon, isSystem = system,
                isQueryable = false, unavailableReason = "O fabricante bloqueou a leitura deste app.",
                lastUsed = lastUsed, versionName = version, installer = installer,
                permissions = permissions, category = category
            )
        } catch (error: Exception) {
            AppEntry(
                info.packageName, label, icon, isSystem = system,
                isQueryable = false, unavailableReason = "O Android não disponibilizou o tamanho deste app.",
                lastUsed = lastUsed, versionName = version, installer = installer,
                permissions = permissions, category = category
            )
        }
    }


    private fun categoryLabel(info: ApplicationInfo, system: Boolean): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return if (system) "Sistema" else "Outros"
        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> "Jogos"
            ApplicationInfo.CATEGORY_AUDIO -> "Áudio"
            ApplicationInfo.CATEGORY_VIDEO -> "Vídeo"
            ApplicationInfo.CATEGORY_IMAGE -> "Imagem"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_NEWS -> "Notícias"
            ApplicationInfo.CATEGORY_MAPS -> "Mapas"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Produtividade"
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Acessibilidade"
            else -> if (system) "Sistema" else "Outros"
        }
    }
    private fun lastUsedMap(): Map<String, Long> {
        if (!Permissions.hasUsageAccess(context)) return emptyMap()
        return runCatching {
            val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
            val now = System.currentTimeMillis()
            usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 90L * 86_400_000L, now)
                .orEmpty()
                .groupBy { it.packageName }
                .mapValues { (_, stats) -> stats.maxOfOrNull { it.lastTimeUsed } ?: 0L }
        }.getOrDefault(emptyMap())
    }
}
