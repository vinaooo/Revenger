package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment

/**
 * Holds the current instance of each submenu fragment, shared between
 * [SubmenuFragmentRegistrar] (which sets these on registration) and [SubmenuFragmentDismisser]
 * (which reads and clears them on dismiss).
 */
class SubmenuFragmentState {
    var settingsMenuFragment: SettingsMenuFragment? = null
    var progressFragment: ProgressFragment? = null
    var exitFragment: ExitFragment? = null
    var aboutFragment: AboutFragment? = null

    fun clearAll() {
        settingsMenuFragment = null
        progressFragment = null
        exitFragment = null
        aboutFragment = null
    }
}
