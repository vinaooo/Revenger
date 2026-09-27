package com.vinaooo.revenger

import android.app.Application
import com.vinaooo.revenger.repositories.DefaultSettingsRepository

class RevengerApplication : Application() {

    companion object {
        lateinit var appConfig: AppConfig
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize default settings repository
        DefaultSettingsRepository.initialize(this)
        
        // Initialize PiP aspect ratio configurations
        com.vinaooo.revenger.repositories.PipConfigRepository.initialize(this)

        // Initialize application config facade
        appConfig = AppConfig(this)
    }
}
