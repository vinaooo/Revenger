package com.vinaooo.revenger

import android.app.Application
import com.vinaooo.revenger.repositories.OptimalSettingsRepository

class RevengerApplication : Application() {

    companion object {
        lateinit var appConfig: AppConfig
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Dynamic theme is now handled automatically by Material 3 theme inheritance

        // Initialize optimal settings repository
        OptimalSettingsRepository.initialize(this)

        // Initialize application config facade
        appConfig = AppConfig(this)
    }
}
