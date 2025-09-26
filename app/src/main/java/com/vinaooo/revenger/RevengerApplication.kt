package com.vinaooo.revenger

import android.app.Application
import com.vinaooo.revenger.ui.theme.DynamicThemeManager

class RevengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic theme as early as possible so activities and dialogs inherit it
        DynamicThemeManager.applyDynamicTheme(this)
    }
}
