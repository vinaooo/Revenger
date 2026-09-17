package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [MenuState] and [NavigationStack]'s Bundle round-trip, in particular the
 * `IllegalArgumentException` catches in `MenuState.Companion.fromBundle` and
 * `NavigationStack.fromBundle` around `MenuType.valueOf()` on a corrupted/stale saved-instance
 * Bundle (e.g. from an older APK version with a since-removed [MenuType] enum constant). Had zero
 * dedicated test coverage before this file - [NavigationStateManager_test] only exercises the
 * equivalent path in [NavigationStateManager], not these two classes directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NavigationState_test {

    // ========== MenuState.toBundle / fromBundle round-trip ==========

    @Test
    fun `MenuState toBundle e fromBundle preservam tipo e indice`() {
        val original = MenuState(MenuType.SETTINGS, 3)

        val restored = MenuState.fromBundle(original.toBundle())

        assertEquals(original, restored)
    }

    @Test
    fun `MenuState fromBundle com bundle nulo retorna nulo`() {
        assertNull(MenuState.fromBundle(null))
    }

    @Test
    fun `MenuState fromBundle sem chave de tipo retorna nulo`() {
        assertNull(MenuState.fromBundle(Bundle()))
    }

    // Regression test for the narrowed IllegalArgumentException catch in
    // MenuState.Companion.fromBundle: MenuType.valueOf() on a name that no longer exists in the
    // enum (simulating a stale Bundle from an older APK version) must be swallowed and logged
    // (with the exception itself, fixing the prior SwallowedException finding) instead of
    // propagating or crashing the restore flow.
    @Test
    fun `MenuState fromBundle com tipo de menu invalido retorna nulo sem lancar excecao`() {
        val bundle = Bundle().apply {
            putString("menu_type", "NOT_A_REAL_MENU_TYPE")
            putInt("menu_index", 2)
        }

        val restored = MenuState.fromBundle(bundle)

        assertNull(restored)
    }

    // ========== NavigationStack.toBundle / fromBundle round-trip ==========

    @Test
    fun `NavigationStack toBundle e fromBundle preservam a pilha em ordem`() {
        val original = NavigationStack()
        original.push(MenuState(MenuType.MAIN, 0))
        original.push(MenuState(MenuType.PROGRESS, 1))
        original.push(MenuState(MenuType.SAVE_SLOTS, 2))

        val restored = NavigationStack()
        restored.fromBundle(original.toBundle())

        assertEquals(3, restored.size())
        assertEquals(MenuState(MenuType.SAVE_SLOTS, 2), restored.pop())
        assertEquals(MenuState(MenuType.PROGRESS, 1), restored.pop())
        assertEquals(MenuState(MenuType.MAIN, 0), restored.pop())
    }

    @Test
    fun `NavigationStack fromBundle com bundle nulo esvazia a pilha`() {
        val stack = NavigationStack()
        stack.push(MenuState(MenuType.MAIN, 0))

        stack.fromBundle(null)

        assertTrue(stack.isEmpty())
    }

    // Regression test for the narrowed IllegalArgumentException catch in
    // NavigationStack.fromBundle: one corrupted entry (invalid MenuType name) among otherwise
    // valid ones must be skipped and logged (with the exception, fixing the prior
    // SwallowedException finding), without aborting the rest of the deserialization.
    @Test
    fun `NavigationStack fromBundle pula entradas com tipo de menu invalido mas mantem as validas`() {
        val bundle = Bundle().apply {
            putInt("stack_size", 3)
            putString("stack_0_type", MenuType.MAIN.name)
            putInt("stack_0_index", 0)
            putString("stack_1_type", "NOT_A_REAL_MENU_TYPE")
            putInt("stack_1_index", 0)
            putString("stack_2_type", MenuType.SETTINGS.name)
            putInt("stack_2_index", 5)
        }

        val stack = NavigationStack()
        stack.fromBundle(bundle)

        assertEquals(2, stack.size())
        assertEquals(MenuState(MenuType.SETTINGS, 5), stack.pop())
        assertEquals(MenuState(MenuType.MAIN, 0), stack.pop())
    }
}
