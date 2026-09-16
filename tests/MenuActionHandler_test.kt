package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuActionHandler] translates a [MenuAction] into concrete effects: closing the menu (and
 * running follow-up work through the callback `RetroMenu3Fragment.dismissMenuPublic` takes),
 * resetting the game, and delegating submenu navigation to [SubmenuCoordinator]. It had zero
 * test coverage before this file.
 *
 * `executeContinue`/`executeReset` only produce their real effects when `fragment` is actually a
 * `RetroMenu3Fragment` (the `as? RetroMenu3Fragment` cast silently no-ops otherwise) -- both
 * branches are covered here since that's a real, easy-to-miss behavioral fork.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuActionHandler_test {

    private lateinit var viewModel: GameActivityViewModel
    private lateinit var navigationController: NavigationController
    private lateinit var submenuCoordinator: SubmenuCoordinator

    @Before
    fun setUp() {
        navigationController = mockk(relaxed = true)
        viewModel = mockk(relaxed = true)
        every { viewModel.navigationController } returns navigationController
        submenuCoordinator = mockk(relaxed = true)
    }

    /** A RetroMenu3Fragment mock whose dismissMenuPublic immediately runs its callback, as the real implementation eventually does once the exit animation completes. */
    private fun retroMenu3FragmentThatDismissesImmediately(): RetroMenu3Fragment {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        val callback = slot<() -> Unit>()
        every { fragment.dismissMenuPublic(capture(callback)) } answers { callback.captured.invoke() }
        return fragment
    }

    private fun handlerWith(fragment: Fragment) =
            MenuActionHandler(fragment, viewModel, submenuCoordinator)

    // --- CONTINUE ---

    @Test
    fun `CONTINUE em RetroMenu3Fragment fecha o menu via dismissMenuPublic e chama closeMenuExternal`() {
        val fragment = retroMenu3FragmentThatDismissesImmediately()
        val handler = handlerWith(fragment)

        handler.executeAction(MenuAction.CONTINUE)

        verify { fragment.dismissMenuPublic(any()) }
        verify { navigationController.closeMenuExternal() }
    }

    @Test
    fun `CONTINUE em um Fragment que nao e RetroMenu3Fragment nao produz nenhum efeito`() {
        val fragment = mockk<Fragment>(relaxed = true)
        val handler = handlerWith(fragment)

        handler.executeAction(MenuAction.CONTINUE)

        verify(inverse = true) { navigationController.closeMenuExternal() }
    }

    // --- RESET ---

    @Test
    fun `RESET em RetroMenu3Fragment zera a velocidade, fecha o menu e reseta o jogo, nessa ordem`() {
        val fragment = retroMenu3FragmentThatDismissesImmediately()
        val handler = handlerWith(fragment)

        handler.executeAction(MenuAction.RESET)

        verifyOrder {
            viewModel.setGameSpeed(1)
            navigationController.closeMenuExternal()
            viewModel.resetGameCentralized()
        }
    }

    @Test
    fun `RESET em um Fragment que nao e RetroMenu3Fragment ainda assim normaliza a velocidade do jogo mas nao reseta`() {
        val fragment = mockk<Fragment>(relaxed = true)
        val handler = handlerWith(fragment)

        handler.executeAction(MenuAction.RESET)

        verify { viewModel.setGameSpeed(1) }
        verify(inverse = true) { viewModel.resetGameCentralized() }
        verify(inverse = true) { navigationController.closeMenuExternal() }
    }

    // --- NAVIGATE: abertura de submenus ---

    @Test
    fun `NAVIGATE para PROGRESS_MENU abre o submenu de progresso`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.NAVIGATE(MenuState.PROGRESS_MENU))

        verify { submenuCoordinator.openSubmenu(MenuState.PROGRESS_MENU) }
    }

    @Test
    fun `NAVIGATE para SETTINGS_MENU abre o submenu de configuracoes`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU))

        verify { submenuCoordinator.openSubmenu(MenuState.SETTINGS_MENU) }
    }

    @Test
    fun `NAVIGATE para ABOUT_MENU abre o submenu sobre`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.NAVIGATE(MenuState.ABOUT_MENU))

        verify { submenuCoordinator.openSubmenu(MenuState.ABOUT_MENU) }
    }

    @Test
    fun `NAVIGATE para EXIT_MENU abre o submenu de saida`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.NAVIGATE(MenuState.EXIT_MENU))

        verify { submenuCoordinator.openSubmenu(MenuState.EXIT_MENU) }
    }

    @Test
    fun `NAVIGATE para um estado sem submenu conhecido nao abre nada`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.NAVIGATE(MenuState.MAIN_MENU))

        verify(inverse = true) { submenuCoordinator.openSubmenu(any()) }
    }

    // --- Acoes nao tratadas ---

    @Test
    fun `acoes sem handler explicito nao produzem nenhum efeito colateral`() {
        val handler = handlerWith(mockk<Fragment>(relaxed = true))

        handler.executeAction(MenuAction.TOGGLE_AUDIO)

        verify(inverse = true) { submenuCoordinator.openSubmenu(any()) }
        verify(inverse = true) { viewModel.setGameSpeed(any()) }
        verify(inverse = true) { navigationController.closeMenuExternal() }
    }
}
