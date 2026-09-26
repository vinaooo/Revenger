package com.vinaooo.revenger.controllers

import android.view.KeyEvent
import androidx.activity.OnBackPressedDispatcher
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The pure decision table and event mapping for `GameActivity`'s system back. */
class SystemBackRouter_test {

    @Test
    fun `menu open navigates back even when back also opens the menu`() {
        assertEquals(SystemBackAction.NAVIGATE_BACK, SystemBackRouter.decide(true, true))
    }

    @Test
    fun `menu open navigates back when back does not open the menu`() {
        assertEquals(SystemBackAction.NAVIGATE_BACK, SystemBackRouter.decide(true, false))
    }

    @Test
    fun `menu closed and back mode on opens the menu`() {
        assertEquals(SystemBackAction.OPEN_MENU, SystemBackRouter.decide(false, true))
    }

    @Test
    fun `menu closed and back mode off uses the default back`() {
        assertEquals(SystemBackAction.DEFAULT, SystemBackRouter.decide(false, false))
    }

    @Test
    fun `navigate back event carries the system back source and key code`() {
        val event = SystemBackRouter.eventFor(SystemBackAction.NAVIGATE_BACK)

        assertTrue(event is NavigationEvent.NavigateBack)
        event as NavigationEvent.NavigateBack
        assertEquals(InputSource.SYSTEM_BACK, event.inputSource)
        assertEquals(KeyEvent.KEYCODE_BACK, event.keyCode)
    }

    @Test
    fun `open menu event carries the system back source and targets the main menu`() {
        val event = SystemBackRouter.eventFor(SystemBackAction.OPEN_MENU)

        assertTrue(event is NavigationEvent.OpenMenu)
        event as NavigationEvent.OpenMenu
        assertEquals(InputSource.SYSTEM_BACK, event.inputSource)
        assertEquals(MenuType.MAIN, event.targetMenu)
    }

    @Test
    fun `default back has no event`() {
        assertNull(SystemBackRouter.eventFor(SystemBackAction.DEFAULT))
    }
}

/**
 * [SystemBackCallback] on a real [OnBackPressedDispatcher], wired like `GameActivity` wires it: the
 * host's default back re-dispatches to the same dispatcher, whose fallback stands in for the
 * platform's back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SystemBackCallback_test {

    private var menuActive = false
    private var backOpensMenu = false
    private var platformBackCount = 0
    private val sent = mutableListOf<NavigationEvent>()
    private val dispatcher = OnBackPressedDispatcher { platformBackCount++ }

    private val callback =
            SystemBackCallback(
                    object : SystemBackHost {
                        override fun isMenuActive() = menuActive
                        override fun shouldHandleBack() = backOpensMenu
                        override fun sendNavigationEvent(event: NavigationEvent) {
                            sent += event
                        }
                        override fun dispatchDefaultBack() = dispatcher.onBackPressed()
                    }
            )

    init {
        dispatcher.addCallback(callback)
    }

    @Test
    fun `menu open sends navigate back and does not reach the platform`() {
        menuActive = true

        dispatcher.onBackPressed()

        assertEquals(1, sent.size)
        assertTrue(sent.single() is NavigationEvent.NavigateBack)
        assertEquals(0, platformBackCount)
    }

    @Test
    fun `back mode on sends open menu and does not reach the platform`() {
        backOpensMenu = true

        dispatcher.onBackPressed()

        assertTrue(sent.single() is NavigationEvent.OpenMenu)
        assertEquals(0, platformBackCount)
    }

    @Test
    fun `default back reaches the platform once and sends nothing`() {
        dispatcher.onBackPressed()

        assertEquals(1, platformBackCount)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `callback is enabled again after the default back`() {
        dispatcher.onBackPressed()

        assertTrue(callback.isEnabled)
    }

    /**
     * Regression: the default back on a task root only moves the task to the background, so the
     * Activity survives. The callback used to stay disabled after that, and back with the menu
     * open then left the game instead of going back one level.
     */
    @Test
    fun `after a default back, back with the menu open still navigates the menu`() {
        dispatcher.onBackPressed()

        menuActive = true
        dispatcher.onBackPressed()

        assertEquals(1, platformBackCount)
        assertTrue(sent.single() is NavigationEvent.NavigateBack)
    }

    @Test
    fun `callback is enabled again even when the default back throws`() {
        val throwing =
                SystemBackCallback(
                        object : SystemBackHost {
                            override fun isMenuActive() = false
                            override fun shouldHandleBack() = false
                            override fun sendNavigationEvent(event: NavigationEvent) = Unit
                            override fun dispatchDefaultBack() = error("platform back failed")
                        }
                )

        runCatching { throwing.handleOnBackPressed() }

        assertTrue(throwing.isEnabled)
    }
}
