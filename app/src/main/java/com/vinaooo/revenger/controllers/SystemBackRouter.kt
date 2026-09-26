package com.vinaooo.revenger.controllers

import android.view.KeyEvent
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/** What a system back press should do in `GameActivity`. */
enum class SystemBackAction {
        /** A menu is open: go back one level in it. */
        NAVIGATE_BACK,

        /** No menu is open and back is configured to open it (`menu_mode` `back`). */
        OPEN_MENU,

        /** Neither: let the platform handle back (leaves the game). */
        DEFAULT
}

/**
 * Pure decision for the system back press, extracted out of `GameActivity`'s
 * `OnBackPressedCallback`. An open menu always wins over the `back` menu mode.
 */
object SystemBackRouter {

        fun decide(isMenuActive: Boolean, shouldHandleBack: Boolean): SystemBackAction =
                when {
                        isMenuActive -> SystemBackAction.NAVIGATE_BACK
                        shouldHandleBack -> SystemBackAction.OPEN_MENU
                        else -> SystemBackAction.DEFAULT
                }

        /** The navigation event to send for [action], or null for [SystemBackAction.DEFAULT]. */
        fun eventFor(action: SystemBackAction): NavigationEvent? =
                when (action) {
                        SystemBackAction.NAVIGATE_BACK ->
                                NavigationEvent.NavigateBack(
                                        keyCode = KeyEvent.KEYCODE_BACK,
                                        inputSource = InputSource.SYSTEM_BACK
                                )
                        SystemBackAction.OPEN_MENU ->
                                NavigationEvent.OpenMenu(inputSource = InputSource.SYSTEM_BACK)
                        SystemBackAction.DEFAULT -> null
                }
}
