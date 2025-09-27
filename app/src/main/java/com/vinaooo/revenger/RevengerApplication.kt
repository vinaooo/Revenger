package com.vinaooo.revenger

import android.app.Application

class RevengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dynamic theme is now handled automatically by Material 3 theme inheritance
    }
}
