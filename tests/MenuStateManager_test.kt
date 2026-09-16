package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuSystemState] is the immutable state object backing the whole RetroMenu3 orchestration
 * layer, and [MenuStateManager] is the single mutable holder for it (Single Source of Truth,
 * replacing distributed boolean flags per the class doc). Neither had any test coverage before
 * this file. Covers the `with*` transformation methods on the data class and the manager's
 * delegation/notification behavior around them.
 */
class MenuSystemState_test {

    @Test
    fun `estado inicial comeca em MAIN_MENU sem menus ativos, pilha vazia e flags desligadas`() {
        val state = MenuSystemState()

        assertEquals(MenuState.MAIN_MENU, state.currentState)
        assertFalse(state.hasActiveMenus())
        assertTrue(state.navigationStack.isEmpty())
        assertFalse(state.isRetroMenu3Open)
        assertFalse(state.isDismissingAllMenus)
    }

    @Test
    fun `withMenuActivated adiciona o tipo ao conjunto de menus ativos`() {
        val state = MenuSystemState().withMenuActivated(MenuSystemState.MenuType.SETTINGS_MENU)

        assertTrue(state.hasActiveMenus())
        assertTrue(state.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertFalse(state.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `withMenuDeactivated remove apenas o tipo indicado, preservando os demais`() {
        val state =
                MenuSystemState()
                        .withMenuActivated(MenuSystemState.MenuType.SETTINGS_MENU)
                        .withMenuActivated(MenuSystemState.MenuType.ABOUT_MENU)
                        .withMenuDeactivated(MenuSystemState.MenuType.SETTINGS_MENU)

        assertFalse(state.isMenuActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertTrue(state.isMenuActive(MenuSystemState.MenuType.ABOUT_MENU))
    }

    @Test
    fun `withState altera apenas currentState`() {
        val state = MenuSystemState().withState(MenuState.SETTINGS_MENU)

        assertEquals(MenuState.SETTINGS_MENU, state.currentState)
    }

    @Test
    fun `withStatePushed empilha e withStatePopped desempilha o ultimo estado`() {
        val pushed =
                MenuSystemState()
                        .withStatePushed(MenuState.MAIN_MENU)
                        .withStatePushed(MenuState.SETTINGS_MENU)

        assertEquals(listOf(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU), pushed.navigationStack)

        val popped = pushed.withStatePopped()

        assertEquals(listOf(MenuState.MAIN_MENU), popped.navigationStack)
    }

    @Test
    fun `withStatePopped em pilha vazia nao lanca excecao e mantem a pilha vazia`() {
        val popped = MenuSystemState().withStatePopped()

        assertTrue(popped.navigationStack.isEmpty())
    }

    @Test
    fun `withRetroMenu3Open e withDismissingAllMenus alteram apenas a flag correspondente`() {
        val opened = MenuSystemState().withRetroMenu3Open(true)
        assertTrue(opened.isRetroMenu3Open)
        assertFalse(opened.isDismissingAllMenus)

        val dismissing = MenuSystemState().withDismissingAllMenus(true)
        assertFalse(dismissing.isRetroMenu3Open)
        assertTrue(dismissing.isDismissingAllMenus)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuStateManager_test {

    @Test
    fun `updateState aplica a transformacao e notifica o callback com o novo estado`() {
        val observed = mutableListOf<MenuSystemState>()
        val manager = MenuStateManager(onStateChanged = { observed.add(it) })

        manager.updateState { it.withState(MenuState.ABOUT_MENU) }

        assertEquals(MenuState.ABOUT_MENU, manager.currentState.currentState)
        assertEquals(1, observed.size)
        assertEquals(MenuState.ABOUT_MENU, observed[0].currentState)
    }

    @Test
    fun `updateState sem callback nao lanca excecao`() {
        val manager = MenuStateManager()

        manager.updateState { it.withState(MenuState.EXIT_MENU) }

        assertEquals(MenuState.EXIT_MENU, manager.getCurrentState())
    }

    @Test
    fun `activateMenu e deactivateMenu delegam para o estado imutavel`() {
        val manager = MenuStateManager()

        manager.activateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
        assertTrue(manager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
        assertTrue(manager.hasActiveMenus())

        manager.deactivateMenu(MenuSystemState.MenuType.PROGRESS_MENU)
        assertFalse(manager.isMenuActive(MenuSystemState.MenuType.PROGRESS_MENU))
        assertFalse(manager.hasActiveMenus())
    }

    @Test
    fun `changeState atualiza getCurrentState`() {
        val manager = MenuStateManager()

        manager.changeState(MenuState.SETTINGS_MENU)

        assertEquals(MenuState.SETTINGS_MENU, manager.getCurrentState())
    }

    @Test
    fun `pushToNavigationStack e popFromNavigationStack delegam corretamente`() {
        val manager = MenuStateManager()

        manager.pushToNavigationStack(MenuState.MAIN_MENU)
        manager.pushToNavigationStack(MenuState.SETTINGS_MENU)
        assertEquals(
                listOf(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU),
                manager.currentState.navigationStack
        )

        manager.popFromNavigationStack()
        assertEquals(listOf(MenuState.MAIN_MENU), manager.currentState.navigationStack)
    }

    @Test
    fun `setRetroMenu3Open e setDismissingAllMenus atualizam as flags correspondentes`() {
        val manager = MenuStateManager()

        manager.setRetroMenu3Open(true)
        assertTrue(manager.isRetroMenu3Open())

        manager.setDismissingAllMenus(true)
        assertTrue(manager.isDismissingAllMenus())

        manager.setRetroMenu3Open(false)
        assertFalse(manager.isRetroMenu3Open())
    }
}
