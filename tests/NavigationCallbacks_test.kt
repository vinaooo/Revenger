package com.vinaooo.revenger.ui.retromenu3.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [NavigationCallbacks] bundles [NavigationController]'s two menu open/close callbacks into one
 * object purely to keep [NavigationController]'s constructor under the project's
 * parameter-count threshold. [NavigationController_test] already exercises it indirectly through
 * `onMenuOpenedCallback`/`onMenuClosedCallback`; this file covers the holder itself in isolation.
 */
class NavigationCallbacks_test {

    @Test
    fun `callbacks comecam nulos`() {
        val callbacks = NavigationCallbacks()

        assertNull(callbacks.onMenuOpened)
        assertNull(callbacks.onMenuClosed)
    }

    @Test
    fun `onMenuOpened e settable e invocavel`() {
        val callbacks = NavigationCallbacks()
        var calls = 0

        callbacks.onMenuOpened = { calls++ }
        callbacks.onMenuOpened?.invoke()

        assertEquals(1, calls)
    }

    @Test
    fun `onMenuClosed e settable e recebe o botao de fechamento`() {
        val callbacks = NavigationCallbacks()
        val received = mutableListOf<Int?>()

        callbacks.onMenuClosed = { received.add(it) }
        callbacks.onMenuClosed?.invoke(42)
        callbacks.onMenuClosed?.invoke(null)

        assertEquals(listOf(42, null), received)
    }
}
