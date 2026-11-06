package com.smartphonetop.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.ui.BatteryActivity
import com.smartphonetop.app.ui.HistoryActivity
import com.smartphonetop.app.ui.MainActivity
import com.smartphonetop.app.ui.SecurityActivity
import com.smartphonetop.app.ui.SettingsActivity
import com.smartphonetop.app.ui.TopAiActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private val dir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun captureScreens() {
        val context = ApplicationProvider.getApplicationContext<SmartphoneTopApplication>()
        SmartStore.init(context)
        SmartStore.resetForTests()
        val main = Robolectric.buildActivity(MainActivity::class.java).setup().visible().get()
        idle(main)
        capture(main.window.decorView, "40_home")

        main.openTab(R.id.navClean)
        idle(main)
        capture(main.window.decorView, "41_clean")

        main.openTab(R.id.navFiles)
        idle(main)
        capture(main.window.decorView, "43_files")

        main.openTab(R.id.navBattery)
        idle(main)
        capture(main.window.decorView, "44_battery")

        main.openTab(R.id.navApps)
        idle(main)
        capture(main.window.decorView, "45_apps")

        val battery = Robolectric.buildActivity(BatteryActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(battery.window.decorView, "45_battery")

        val security = Robolectric.buildActivity(SecurityActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(security.window.decorView, "46_security")

        val settings = Robolectric.buildActivity(SettingsActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(settings.window.decorView, "47_settings")

        val history = Robolectric.buildActivity(HistoryActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(history.window.decorView, "48_history")

        val ai = Robolectric.buildActivity(TopAiActivity::class.java).setup().visible().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        capture(ai.window.decorView, "49_top_ai")
    }

    private fun idle(activity: MainActivity) {
        activity.supportFragmentManager.executePendingTransactions()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun capture(view: View, name: String) {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2280, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 2280)
        val bitmap = Bitmap.createBitmap(1080, 2280, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
