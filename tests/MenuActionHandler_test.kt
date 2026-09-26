package com.vinaooo.revenger.ui.retromenu3

import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.LogSaver
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

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

    /**
     * A RetroMenu3Fragment mock whose dismissMenuPublic immediately runs its callback, as the real
     * implementation eventually does once the exit animation completes.
     */
    private fun retroMenu3FragmentThatDismissesImmediately(): RetroMenu3Fragment {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        val callback = slot<() -> Unit>()
        every { fragment.dismissMenuPublic(capture(callback)) } answers { callback.captured.invoke() }
        return fragment
    }

    // Unconfined runs the log I/O inline, so the SAVE_LOG tests below finish in one looper idle.
    private fun handlerWith(fragment: Fragment, ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined) =
            MenuActionHandler(fragment, viewModel, submenuCoordinator, ioDispatcher)

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

    // --- SAVE_LOG ---

    /** A plain Fragment attached to a started activity, so it has a context and a lifecycleScope. */
    private fun attachedFragment(): Fragment {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()
        return fragment
    }

    private fun saveLogWith(result: () -> String?): String? {
        mockkObject(LogSaver)
        try {
            every { LogSaver.saveCompleteLog(any()) } answers { result() }
            handlerWith(attachedFragment()).executeAction(MenuAction.SAVE_LOG)
            shadowOf(Looper.getMainLooper()).idle()
            return ShadowToast.getTextOfLatestToast()
        } finally {
            unmockkObject(LogSaver)
        }
    }

    @Test
    fun `SAVE_LOG salvo mostra o nome do arquivo`() {
        assertEquals("Log saved: revenger_log.txt", saveLogWith { "/some/dir/revenger_log.txt" })
    }

    @Test
    fun `SAVE_LOG sem arquivo mostra erro`() {
        assertEquals("Error saving log", saveLogWith { null })
    }

    @Test
    fun `SAVE_LOG com excecao mostra o motivo`() {
        assertEquals("Error saving log: disk full", saveLogWith { throw IllegalStateException("disk full") })
    }

    @Test
    // The exception must have no message: that is the case under test.
    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    fun `SAVE_LOG com excecao sem mensagem mostra o tipo da excecao`() {
        assertEquals("Error saving log: IllegalStateException", saveLogWith { throw IllegalStateException() })
    }

    @Test
    fun `SAVE_LOG grava o arquivo fora da thread principal e mostra o toast na principal`() {
        val mainThread = Thread.currentThread()
        var saveThread: Thread? = null
        val executor = Executors.newSingleThreadExecutor()
        mockkObject(LogSaver)
        try {
            every { LogSaver.saveCompleteLog(any()) } answers {
                saveThread = Thread.currentThread()
                "/some/dir/revenger_log.txt"
            }
            handlerWith(attachedFragment(), executor.asCoroutineDispatcher()).executeAction(MenuAction.SAVE_LOG)

            // The save hops to the executor and back; idle the main looper until the toast shows.
            val deadline = System.currentTimeMillis() + 5_000
            while (ShadowToast.getTextOfLatestToast() == null && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.yield()
            }

            assertEquals("Log saved: revenger_log.txt", ShadowToast.getTextOfLatestToast())
            assertNotEquals(mainThread, saveThread)
        } finally {
            unmockkObject(LogSaver)
            executor.shutdownNow()
        }
    }
}
