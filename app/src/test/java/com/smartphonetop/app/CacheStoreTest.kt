package com.smartphonetop.app

import androidx.test.core.app.ApplicationProvider
import com.smartphonetop.app.data.CacheReport
import com.smartphonetop.app.data.CacheSnapshot
import com.smartphonetop.app.data.SmartStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CacheStoreTest {
    private val context = ApplicationProvider.getApplicationContext<SmartphoneTopApplication>()

    @Before fun setup() {
        SmartStore.init(context)
        SmartStore.resetForTests()
    }

    @Test fun snapshotAndReportsPersist() {
        SmartStore.saveCacheSnapshot(CacheSnapshot(1_700_000_000_000L, 123_456_789L, 42, 7))
        val snapshot = SmartStore.lastCacheSnapshot()
        assertEquals(42, snapshot?.analyzedApps)
        assertEquals(7, snapshot?.appsWithCache)
        assertEquals(123_456_789L, snapshot?.totalCacheBytes)

        SmartStore.addCacheReport(
            CacheReport(
                analyzedApps = 40, appsWithCache = 6, operations = 3,
                notPermitted = 1, estimatedBefore = 100_000L, freedBytes = 75_000L,
                summary = "teste"
            )
        )
        assertEquals(1, SmartStore.cacheReports().size)
        assertEquals(75_000L, SmartStore.cacheReports().first().freedBytes)
        SmartStore.clearCacheReports()
        assertEquals(0, SmartStore.cacheReports().size)
    }
}
