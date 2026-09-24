package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuAction
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuActionDispatcher]'s [MenuManager] dependency is read through a provider lambda backed by a
 * local var here, standing in for `GameActivityViewModel`'s own `menuManager` field (which tests
 * replace by reflection). [saveLoad]/[submenuDismissal] stand in for the owning ViewModel itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuActionDispatcher_test {

    private lateinit var saveLoad: SaveLoadCentralizedFacade
    private lateinit var submenuDismissal: SubmenuFragmentDismissal
    private var dismissRetroMenu3Calls = 0
    private lateinit var currentMenuManager: MenuManager
    private lateinit var menuToggleActions: MenuToggleActions
    private lateinit var dispatcher: MenuActionDispatcher

    @Before
    fun setUp() {
        saveLoad = mockk(relaxed = true)
        submenuDismissal = mockk(relaxed = true)
        dismissRetroMenu3Calls = 0
        currentMenuManager = mockk(relaxed = true)
        menuToggleActions = mockk(relaxed = true)
        dispatcher =
                MenuActionDispatcher(
                        saveLoad = saveLoad,
                        submenuDismissal = submenuDismissal,
                        dismissRetroMenu3 = { dismissRetroMenu3Calls++ },
                        menuManager = { currentMenuManager },
                        menuToggleActions = menuToggleActions
                )
    }

    @After
    fun tearDown() {
        unmockkStatic(android.os.Process::class)
    }

    @Test
    fun `SAVE_STATE chama saveStateCentralized sem argumentos`() {
        dispatcher.handleAction(MenuAction.SAVE_STATE)

        verify(exactly = 1) { saveLoad.saveStateCentralized(null, false) }
    }

    @Test
    fun `LOAD_STATE chama loadStateCentralized`() {
        dispatcher.handleAction(MenuAction.LOAD_STATE)

        verify(exactly = 1) { saveLoad.loadStateCentralized(null) }
    }

    @Test
    fun `RESET chama resetGameCentralized`() {
        dispatcher.handleAction(MenuAction.RESET)

        verify(exactly = 1) { saveLoad.resetGameCentralized(null) }
    }

    @Test
    fun `TOGGLE_AUDIO delega para menuToggleActions`() {
        dispatcher.handleAction(MenuAction.TOGGLE_AUDIO)

        verify(exactly = 1) { menuToggleActions.toggleAudio() }
    }

    @Test
    fun `TOGGLE_SPEED delega para menuToggleActions`() {
        dispatcher.handleAction(MenuAction.TOGGLE_SPEED)

        verify(exactly = 1) { menuToggleActions.toggleSpeed() }
    }

    @Test
    fun `TOGGLE_SHADER delega para menuToggleActions`() {
        dispatcher.handleAction(MenuAction.TOGGLE_SHADER)

        verify(exactly = 1) { menuToggleActions.toggleShader() }
    }

    @Test
    fun `SAVE_AND_EXIT salva e o onComplete mata o processo`() {
        mockkStatic(android.os.Process::class)
        every { android.os.Process.killProcess(any()) } just Runs
        val onCompleteSlot = slot<() -> Unit>()
        every { saveLoad.saveStateCentralized(capture(onCompleteSlot), any()) } just Runs

        dispatcher.handleAction(MenuAction.SAVE_AND_EXIT)
        onCompleteSlot.captured.invoke()

        verify(exactly = 1) { android.os.Process.killProcess(android.os.Process.myPid()) }
    }

    @Test
    fun `EXIT mata o processo sem chamar saveLoad`() {
        mockkStatic(android.os.Process::class)
        every { android.os.Process.killProcess(any()) } just Runs

        dispatcher.handleAction(MenuAction.EXIT)

        verify(exactly = 1) { android.os.Process.killProcess(android.os.Process.myPid()) }
        verify(exactly = 0) { saveLoad.saveStateCentralized(any(), any()) }
    }

    @Test
    fun `BACK no MAIN_MENU chama dismissRetroMenu3`() {
        every { currentMenuManager.getCurrentState() } returns MenuState.MAIN_MENU

        dispatcher.handleAction(MenuAction.BACK)

        assertEquals(1, dismissRetroMenu3Calls)
    }

    @Test
    fun `BACK no SETTINGS_MENU chama dismissSettingsMenu`() {
        every { currentMenuManager.getCurrentState() } returns MenuState.SETTINGS_MENU

        dispatcher.handleAction(MenuAction.BACK)

        verify(exactly = 1) { submenuDismissal.dismissSettingsMenu() }
    }

    @Test
    fun `BACK no PROGRESS_MENU chama dismissProgress`() {
        every { currentMenuManager.getCurrentState() } returns MenuState.PROGRESS_MENU

        dispatcher.handleAction(MenuAction.BACK)

        verify(exactly = 1) { submenuDismissal.dismissProgress() }
    }

    @Test
    fun `BACK no ABOUT_MENU chama dismissAboutMenu`() {
        every { currentMenuManager.getCurrentState() } returns MenuState.ABOUT_MENU

        dispatcher.handleAction(MenuAction.BACK)

        verify(exactly = 1) { submenuDismissal.dismissAboutMenu() }
    }

    @Test
    fun `BACK no EXIT_MENU chama dismissExit`() {
        every { currentMenuManager.getCurrentState() } returns MenuState.EXIT_MENU

        dispatcher.handleAction(MenuAction.BACK)

        verify(exactly = 1) { submenuDismissal.dismissExit() }
    }

    @Test
    fun `NAVIGATE chama menuManager navigateToState com o menu alvo`() {
        dispatcher.handleAction(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU))

        verify(exactly = 1) { currentMenuManager.navigateToState(MenuState.SETTINGS_MENU) }
    }

    @Test
    fun `acoes ignoradas nao chamam nenhuma dependencia`() {
        dispatcher.handleAction(MenuAction.CONTINUE)
        dispatcher.handleAction(MenuAction.MANAGE_SAVES)
        dispatcher.handleAction(MenuAction.SAVE_LOG)
        dispatcher.handleAction(MenuAction.NONE)

        verify(exactly = 0) { saveLoad.saveStateCentralized(any(), any()) }
        verify(exactly = 0) { saveLoad.loadStateCentralized(any()) }
        verify(exactly = 0) { saveLoad.resetGameCentralized(any()) }
        verify(exactly = 0) { menuToggleActions.toggleAudio() }
        verify(exactly = 0) { menuToggleActions.toggleSpeed() }
        verify(exactly = 0) { menuToggleActions.toggleShader() }
        verify(exactly = 0) { currentMenuManager.navigateToState(any()) }
        assertEquals(0, dismissRetroMenu3Calls)
    }
}
