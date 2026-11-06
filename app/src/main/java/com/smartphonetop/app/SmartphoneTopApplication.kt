package com.smartphonetop.app

import android.app.Application
import com.smartphonetop.app.data.SmartStore

class SmartphoneTopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SmartStore.init(this)
    }
}
