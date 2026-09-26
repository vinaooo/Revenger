package com.vinaooo.revenger.utils

import android.content.res.Configuration
import android.view.WindowInsetsController

/**
 * Status/navigation bar icon appearance for a UI mode: light icons in the dark theme, dark icons
 * in the light theme. `APPEARANCE_LIGHT_*_BARS` means a light bar with **dark** icons, so it is set
 * only in the light theme.
 */
object SystemBarsAppearance {

    /** The appearance bits this class controls; pass as the `mask` of `setSystemBarsAppearance`. */
    const val MASK =
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS

    /** The `appearance` value for [uiMode] (a `Configuration.uiMode`). */
    fun forUiMode(uiMode: Int): Int =
        if (uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 0
        else MASK
}
