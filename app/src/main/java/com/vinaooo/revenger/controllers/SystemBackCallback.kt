package com.vinaooo.revenger.controllers

import androidx.activity.OnBackPressedCallback
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/** The narrow slice of `GameActivity` that [SystemBackCallback] needs. */
interface SystemBackHost {
        fun isMenuActive(): Boolean
        fun shouldHandleBack(): Boolean
        fun sendNavigationEvent(event: NavigationEvent)

        /** Re-dispatches the back press; with this callback disabled it reaches the platform. */
        fun dispatchDefaultBack()
}

/**
 * `GameActivity`'s system back handler: routes through [SystemBackRouter] and, for
 * [SystemBackAction.DEFAULT], lets the platform handle the press.
 *
 * The callback disables itself only for the length of that re-dispatch and then re-enables. It
 * used to stay disabled for good, but the platform's default back on a task root (Android 12+)
 * moves the task to the background instead of finishing the Activity. So after returning to the
 * game, back with the menu open would leave the game instead of going back one level.
 */
class SystemBackCallback(private val host: SystemBackHost) : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
                val action = SystemBackRouter.decide(host.isMenuActive(), host.shouldHandleBack())
                val event = SystemBackRouter.eventFor(action)
                if (event != null) {
                        host.sendNavigationEvent(event)
                        return
                }
                isEnabled = false
                try {
                        host.dispatchDefaultBack()
                } finally {
                        isEnabled = true
                }
        }
}
