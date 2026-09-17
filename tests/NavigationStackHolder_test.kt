package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [NavigationStackHolder] was extracted out of [NavigationStateManager] (the navigation history
 * stack: push/pop/clear/empty/size), which had this behavior covered only indirectly through
 * [NavigationStateManager_test] before this file. It adapts [NavigationStack]'s own method names
 * (`push`/`pop`/`clear`/`isEmpty`/`size`) to the names [NavigationStateManager] exposes
 * (`pushState`/`popState`/`clearStack`/`isStackEmpty`/`getStackSize`), which is why a plain
 * `by navigationStack` wasn't possible and this adapter exists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigationStackHolder_test {

    private lateinit var holder: NavigationStackHolder

    @Before
    fun setUp() {
        holder = NavigationStackHolder()
    }

    @Test
    fun `pilha comeca vazia`() {
        assertTrue(holder.isStackEmpty())
        assertEquals(0, holder.getStackSize())
        assertNull(holder.popState())
    }

    @Test
    fun `pushState e popState seguem ordem LIFO`() {
        holder.pushState(MenuState(MenuType.MAIN, 2))
        holder.pushState(MenuState(MenuType.SETTINGS, 0))

        assertEquals(2, holder.getStackSize())
        assertEquals(MenuState(MenuType.SETTINGS, 0), holder.popState())
        assertEquals(MenuState(MenuType.MAIN, 2), holder.popState())
        assertTrue(holder.isStackEmpty())
    }

    @Test
    fun `clearStack esvazia a pilha`() {
        holder.pushState(MenuState(MenuType.MAIN, 0))
        holder.pushState(MenuState(MenuType.SETTINGS, 1))

        holder.clearStack()

        assertTrue(holder.isStackEmpty())
        assertEquals(0, holder.getStackSize())
    }

    @Test
    fun `stack exposto permite round-trip via Bundle`() {
        holder.pushState(MenuState(MenuType.MAIN, 3))

        val bundle = Bundle()
        bundle.putBundle("stack", holder.stack.toBundle())

        val restored = NavigationStackHolder()
        restored.stack.fromBundle(bundle.getBundle("stack"))

        assertEquals(1, restored.getStackSize())
        assertEquals(MenuState(MenuType.MAIN, 3), restored.popState())
    }
}
