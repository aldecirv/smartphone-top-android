package com.smartphonetop.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object SmartStore {

    private const val PREFS = "smartphone_top"
    private const val KEY_HISTORY = "history"
    private const val KEY_TOTAL_FREED = "total_freed"
    private const val KEY_THRESHOLD = "storage_threshold"
    private const val KEY_LAST_FREED = "last_freed"
    private const val KEY_CACHE_SNAPSHOT = "cache_snapshot"
    private const val KEY_CACHE_REPORTS = "cache_reports"

    lateinit var appContext: Context
        private set
    private lateinit var prefs: SharedPreferences
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    private val history = mutableListOf<HistoryRecord>()
    private val cacheReports = mutableListOf<CacheReport>()

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        read()
        loadCacheReports()
    }

    @Synchronized
    fun history(): List<HistoryRecord> = history.sortedByDescending { it.timestamp }

    @Synchronized
    fun addHistory(record: HistoryRecord) {
        history.add(0, record)
        while (history.size > 80) history.removeAt(history.lastIndex)
        saveHistory()
        notifyChanged()
    }

    @Synchronized
    fun recordClean(type: String, freedBytes: Long, items: Int, summary: String) {
        if (freedBytes <= 0L) return
        val total = totalFreedBytes() + freedBytes
        prefs.edit().putLong(KEY_TOTAL_FREED, total).putLong(KEY_LAST_FREED, freedBytes).commit()
        addHistory(
            HistoryRecord(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                type = type,
                freedBytes = freedBytes,
                itemsCount = items,
                summary = summary
            )
        )
    }

    fun totalFreedBytes(): Long = prefs.getLong(KEY_TOTAL_FREED, 0L)
    fun lastFreedBytes(): Long = prefs.getLong(KEY_LAST_FREED, 0L)

    @Synchronized
    fun clearHistory() {
        history.clear()
        prefs.edit().putLong(KEY_TOTAL_FREED, 0L).putLong(KEY_LAST_FREED, 0L).commit()
        saveHistory()
        notifyChanged()
    }

    fun storageThreshold(): Int = prefs.getInt(KEY_THRESHOLD, 85)
    fun setStorageThreshold(value: Int) {
        prefs.edit().putInt(KEY_THRESHOLD, value.coerceIn(50, 99)).apply()
    }

    fun subscribe(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    private fun notifyChanged() {
        listeners.toList().forEach { runCatching { it() } }
    }

    private fun read() {
        history.clear()
        runCatching {
            val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                history.add(
                    HistoryRecord(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        type = o.optString("type", "Limpeza"),
                        freedBytes = o.optLong("freedBytes", 0L),
                        itemsCount = o.optInt("itemsCount", 0),
                        summary = o.optString("summary", "")
                    )
                )
            }
        }
    }

    private fun saveHistory() {
        val arr = JSONArray()
        history.forEach { record ->
            arr.put(JSONObject().apply {
                put("id", record.id); put("timestamp", record.timestamp); put("type", record.type)
                put("freedBytes", record.freedBytes); put("itemsCount", record.itemsCount); put("summary", record.summary)
            })
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }


    @Synchronized
    fun saveCacheSnapshot(snapshot: CacheSnapshot) {
        val json = JSONObject().apply {
            put("timestamp", snapshot.timestamp)
            put("totalCacheBytes", snapshot.totalCacheBytes)
            put("analyzedApps", snapshot.analyzedApps)
            put("appsWithCache", snapshot.appsWithCache)
        }
        prefs.edit().putString(KEY_CACHE_SNAPSHOT, json.toString()).commit()
        notifyChanged()
    }

    fun lastCacheSnapshot(): CacheSnapshot? {
        val raw = prefs.getString(KEY_CACHE_SNAPSHOT, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            CacheSnapshot(
                timestamp = o.optLong("timestamp"),
                totalCacheBytes = o.optLong("totalCacheBytes"),
                analyzedApps = o.optInt("analyzedApps"),
                appsWithCache = o.optInt("appsWithCache")
            )
        }.getOrNull()
    }

    @Synchronized
    fun addCacheReport(report: CacheReport) {
        cacheReports.add(0, report)
        while (cacheReports.size > 50) cacheReports.removeAt(cacheReports.lastIndex)
        saveCacheReports()
        notifyChanged()
    }

    fun cacheReports(): List<CacheReport> = cacheReports.toList()

    @Synchronized
    fun clearCacheReports() {
        cacheReports.clear()
        saveCacheReports()
        notifyChanged()
    }

    private fun saveCacheReports() {
        val arr = JSONArray()
        cacheReports.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("timestamp", r.timestamp)
                put("analyzedApps", r.analyzedApps); put("appsWithCache", r.appsWithCache)
                put("operations", r.operations); put("notPermitted", r.notPermitted)
                put("estimatedBefore", r.estimatedBefore); put("freedBytes", r.freedBytes)
                put("summary", r.summary)
            })
        }
        prefs.edit().putString(KEY_CACHE_REPORTS, arr.toString()).apply()
    }

    private fun loadCacheReports() {
        cacheReports.clear()
        runCatching {
            val arr = JSONArray(prefs.getString(KEY_CACHE_REPORTS, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cacheReports.add(
                    CacheReport(
                        id = o.optString("id"), timestamp = o.optLong("timestamp"),
                        analyzedApps = o.optInt("analyzedApps"), appsWithCache = o.optInt("appsWithCache"),
                        operations = o.optInt("operations"), notPermitted = o.optInt("notPermitted"),
                        estimatedBefore = o.optLong("estimatedBefore"), freedBytes = o.optLong("freedBytes"),
                        summary = o.optString("summary")
                    )
                )
            }
        }
    }

    @Synchronized
    fun resetForTests() {
        prefs.edit().clear().apply()
        history.clear()
        cacheReports.clear()
    }
}
