package com.smartphonetop.app

import androidx.test.core.app.ApplicationProvider
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartStoreTest {

    private val context = ApplicationProvider.getApplicationContext<SmartphoneTopApplication>()

    @Before
    fun setup() {
        SmartStore.init(context)
        SmartStore.resetForTests()
    }

    @Test
    fun formatsBytes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1,0 KB", formatBytes(1024))
        assertEquals("1,0 MB", formatBytes(1024 * 1024))
        assertEquals("Indisponível", formatBytes(null))
    }

    @Test
    fun recordsRealCleanAndHistory() {
        SmartStore.recordClean("Cache", 2_000_000, 2, "2 apps")
        assertEquals(2_000_000L, SmartStore.totalFreedBytes())
        assertEquals(1, SmartStore.history().size)
        assertTrue(SmartStore.history().first().summary.contains("2 apps"))
        SmartStore.recordClean("Cache", 0, 0, "nada")
        assertEquals(1, SmartStore.history().size)
        SmartStore.clearHistory()
        assertEquals(0, SmartStore.history().size)
        assertEquals(0L, SmartStore.totalFreedBytes())
    }

    @Test
    fun thresholdIsClamped() {
        SmartStore.setStorageThreshold(120)
        assertEquals(99, SmartStore.storageThreshold())
        SmartStore.setStorageThreshold(10)
        assertEquals(50, SmartStore.storageThreshold())
    }
}
