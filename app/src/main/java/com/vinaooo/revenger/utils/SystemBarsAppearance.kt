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

    /**
     * Applies the appearance for [uiMode] to [controller] (a window's insets controller; null
     * before the window is attached, in which case nothing happens). Called at startup and again
     * on every configuration change, since `GameActivity` handles `uiMode` changes itself and is
     * not recreated when the theme switches.
     */
    fun apply(controller: WindowInsetsController?, uiMode: Int) {
        controller?.setSystemBarsAppearance(forUiMode(uiMode), MASK)
    }

    /** The `appearance` value for [uiMode] (a `Configuration.uiMode`). */
    fun forUiMode(uiMode: Int): Int =
        if (uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 0
        else MASK
}
