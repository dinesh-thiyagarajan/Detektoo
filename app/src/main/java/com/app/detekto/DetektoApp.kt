package com.app.detekto

import android.app.Application
import com.app.detekto.core.ads.AdManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DetektoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (AdManager.showAds) {
            MobileAds.initialize(this)
        }
    }
}
