package com.vinaooo.revenger.viewmodels.menu

import android.widget.FrameLayout
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment

/**
 * Holds the menu container view and the collaborating menu fragment references that
 * `MenuViewModel` used to track directly. Split out purely to keep `MenuViewModel` under the
 * project's function-count threshold; exposed back on it unchanged via Kotlin interface
 * delegation (`by`) since [MenuFragmentRegistration]'s methods are called from outside that file
 * (fragments/`GameActivityViewModel` register themselves here).
 *
 * None of the stored references are currently read back by `MenuViewModel` itself -- they exist
 * purely as the "last registered fragment" bookkeeping this class now owns end to end.
 */
interface MenuFragmentRegistration {
    fun setMenuContainer(container: FrameLayout)
    fun registerRetroMenu3Fragment(fragment: RetroMenu3Fragment)
    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment)
    fun registerProgressFragment(fragment: ProgressFragment)
    fun registerExitFragment(fragment: ExitFragment)
}

class MenuFragmentRegistry : MenuFragmentRegistration {

    private var menuContainerView: FrameLayout? = null
    private var retroMenu3Fragment: RetroMenu3Fragment? = null
    private var settingsMenuFragment: SettingsMenuFragment? = null
    private var progressFragment: ProgressFragment? = null
    private var exitFragment: ExitFragment? = null

    override fun setMenuContainer(container: FrameLayout) {
        menuContainerView = container
    }

    override fun registerRetroMenu3Fragment(fragment: RetroMenu3Fragment) {
        retroMenu3Fragment = fragment
    }

    override fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        settingsMenuFragment = fragment
    }

    override fun registerProgressFragment(fragment: ProgressFragment) {
        progressFragment = fragment
    }

    override fun registerExitFragment(fragment: ExitFragment) {
        exitFragment = fragment
    }
}
