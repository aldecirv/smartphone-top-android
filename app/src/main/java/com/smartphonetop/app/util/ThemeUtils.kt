package com.smartphonetop.app.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat

object ThemeUtils {
    fun darkSystemBars(activity: Activity) {
        val window: Window = activity.window
        window.statusBarColor = Color.parseColor("#060B16")
        window.navigationBarColor = Color.parseColor("#060B16")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}
