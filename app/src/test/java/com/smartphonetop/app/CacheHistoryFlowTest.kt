package com.smartphonetop.app

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.smartphonetop.app.data.CacheReport
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.ui.CacheHistoryActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-xxhdpi")
class CacheHistoryFlowTest {
    @Test fun historyOpensWithReport() {
        val context = ApplicationProvider.getApplicationContext<SmartphoneTopApplication>()
        SmartStore.init(context)
        SmartStore.resetForTests()
        SmartStore.addCacheReport(
            CacheReport(analyzedApps = 10, appsWithCache = 3, operations = 2, notPermitted = 0,
                estimatedBefore = 1000L, freedBytes = 500L, summary = "teste")
        )
        val activity = Robolectric.buildActivity(CacheHistoryActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(activity)
        assertEqualsText(activity, R.id.total, "500 B")
    }

    private fun assertEqualsText(activity: CacheHistoryActivity, id: Int, expected: String) {
        val text = activity.findViewById<android.widget.TextView>(id).text.toString()
        org.junit.Assert.assertEquals(expected, text)
    }
}
