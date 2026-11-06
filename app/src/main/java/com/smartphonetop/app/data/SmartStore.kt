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

    lateinit var appContext: Context
        private set
    private lateinit var prefs: SharedPreferences
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    private val history = mutableListOf<HistoryRecord>()

    @Synchronized
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        read()
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
    fun resetForTests() {
        prefs.edit().clear().apply()
        history.clear()
    }
}
