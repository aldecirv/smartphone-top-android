package com.smartphonetop.app

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.smartphonetop.app.R
import com.smartphonetop.app.ui.MainActivity
import com.smartphonetop.app.ui.fragments.BatteryFragment
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-xxhdpi")
class NavigationFlowTest {
    @Test fun batteryTabAndMoreSheetOpen() {
        ApplicationProvider.getApplicationContext<SmartphoneTopApplication>()
        val main = Robolectric.buildActivity(MainActivity::class.java).setup().visible().get()
        main.findViewById<android.view.View>(R.id.navBattery).performClick()
        main.supportFragmentManager.executePendingTransactions()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(main.supportFragmentManager.findFragmentById(R.id.content) is BatteryFragment)

        main.findViewById<android.view.View>(R.id.navMore).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(main.supportFragmentManager.findFragmentByTag("more"))
    }
}
