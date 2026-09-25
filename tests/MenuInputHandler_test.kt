package com.vinaooo.revenger.ui.retromenu3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [MenuInputHandlerImpl]: selection moves are delegated to [MenuStateController],
 * and every confirmed item becomes a [MenuAction] executed by [MenuActionHandler]. Runs under
 * Robolectric only because [com.vinaooo.revenger.utils.MenuLogger] calls `android.util.Log`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuInputHandler_test {

    private lateinit var stateController: MenuStateController
    private lateinit var actionHandler: MenuActionHandler
    private lateinit var handler: MenuInputHandlerImpl

    @Before
    fun setUp() {
        stateController = mockk(relaxed = true)
        actionHandler = mockk(relaxed = true)
        handler = MenuInputHandlerImpl(stateController, actionHandler)
    }

    private fun item(action: MenuAction) = MenuItem("id", "Title", action = action)

    @Test
    fun `handleNavigateUp e handleNavigateDown delegam ao stateController`() {
        every { stateController.selectPreviousItem() } returns true
        every { stateController.selectNextItem() } returns false

        assertTrue(handler.handleNavigateUp())
        assertFalse(handler.handleNavigateDown())
        verify(exactly = 1) { stateController.selectPreviousItem() }
        verify(exactly = 1) { stateController.selectNextItem() }
    }

    @Test
    fun `handleBack deixa o MenuManager tratar o fechamento`() {
        assertFalse(handler.handleBack())
    }

    @Test
    fun `handleConfirm executa a acao do item atualmente selecionado`() {
        every { stateController.getCurrentSelection() } returns item(MenuAction.RESET)

        assertTrue(handler.handleConfirm())

        verify(exactly = 1) { actionHandler.executeAction(MenuAction.RESET) }
    }

    @Test
    fun `CONTINUE e RESET sao executados diretamente`() {
        handler.handleMenuItemSelected(item(MenuAction.CONTINUE))
        handler.handleMenuItemSelected(item(MenuAction.RESET))

        verify(exactly = 1) { actionHandler.executeAction(MenuAction.CONTINUE) }
        verify(exactly = 1) { actionHandler.executeAction(MenuAction.RESET) }
    }

    @Test
    fun `SAVE_STATE abre o menu de progresso`() {
        handler.handleMenuItemSelected(item(MenuAction.SAVE_STATE))

        verify(exactly = 1) {
            actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.PROGRESS_MENU))
        }
    }

    @Test
    fun `TOGGLE_AUDIO abre o menu de configuracoes`() {
        handler.handleMenuItemSelected(item(MenuAction.TOGGLE_AUDIO))

        verify(exactly = 1) {
            actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU))
        }
    }

    @Test
    fun `EXIT abre o menu de saida`() {
        handler.handleMenuItemSelected(item(MenuAction.EXIT))

        verify(exactly = 1) {
            actionHandler.executeAction(MenuAction.NAVIGATE(MenuState.EXIT_MENU))
        }
    }

    @Test
    fun `NAVIGATE executa a propria acao de navegacao`() {
        val navigate = MenuAction.NAVIGATE(MenuState.ABOUT_MENU)

        handler.handleMenuItemSelected(item(navigate))

        verify(exactly = 1) { actionHandler.executeAction(navigate) }
    }

    @Test
    fun `acao sem tratamento nao executa nada`() {
        handler.handleMenuItemSelected(item(MenuAction.NONE))

        verify(exactly = 0) { actionHandler.executeAction(any()) }
    }
}
