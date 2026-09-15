package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.input.ControllerInputCallbacks
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.setupMenuCallback]. Written BEFORE the
 * method-decomposition refactor to pin down current behavior: menu-open ordering, menu-close
 * ordering (including the `closingButton` threading into the grace-period intercept), and that
 * every real `ControllerInputCallbacks` field gets wired (nothing silently dropped).
 *
 * `RevengerApplication.appConfig` is a `lateinit var` (companion object, private setter) that is
 * only populated by `RevengerApplication.onCreate()`, which never runs against Robolectric's
 * default test Application. `GameActivityViewModel` reads it directly in its constructor, so it
 * has to be seeded via reflection before each test and cleared afterwards, exactly like
 * `InputViewModel_test.kt` does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    /**
     * Replaces the real (private) `menuViewModel` field with a relaxed mock, so the
     * register-fragment tests below can assert `verify(exactly = ...)` on it instead of relying
     * on an observable side effect -- `MenuViewModel.registerXFragment` only assigns a private
     * field on `MenuViewModel` that is never read back anywhere, so there is no such side effect
     * to assert on.
     */
    private fun mockMenuViewModel(): MenuViewModel {
        val mock = mockk<MenuViewModel>(relaxed = true)
        setPrivateField(viewModel, "menuViewModel", mock)
        return mock
    }

    /**
     * Replaces the real (private) `menuManager` field with a relaxed mock, so the register-
     * fragment tests below can assert `registerFragment(state, fragment)` was invoked for the
     * correct [MenuState] without depending on [MenuManager]'s internal fragment map.
     */
    private fun mockMenuManager(): MenuManager {
        val mock = mockk<MenuManager>(relaxed = true)
        setPrivateField(viewModel, "menuManager", mock)
        return mock
    }

    /**
     * Reads whether [menuType] is active via the REAL (untouched) private `menuStateManager`
     * field. This is the observable side effect of `activateXMenu()`/`deactivateXMenu()`, used to
     * pin down exactly which of the register methods call `activateXMenu()` and which don't.
     */
    private fun isMenuTypeActive(menuType: MenuSystemState.MenuType): Boolean {
        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")
        return menuStateManager.isMenuActive(menuType)
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
        unmockkObject(ScreenshotCaptureUtil)
    }

    /**
     * Test 1: opening the menu must capture the save-state screenshot BEFORE pausing emulation.
     * `ScreenshotCaptureUtil` is a singleton object, mocked via `mockkObject` so its call can
     * take part in the same `verifyOrder` chain as the mocked `speedController`.
     */
    @Test
    fun `abrir o menu captura screenshot antes de pausar o emulador`() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any()) } just Runs
        every { ScreenshotCaptureUtil.capturePipFrame(any(), any()) } just Runs

        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)
        viewModel.retroView = mockk<RetroView>(relaxed = true)

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        viewModel.navigationController?.onMenuOpenedCallback?.invoke()

        verifyOrder {
            ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any())
            speedController.pause(any())
        }
    }

    /**
     * Test 2: closing the menu must, in order, clear the menu action buttons, reset the
     * already-triggered combo flag, clear the key log, update the debounce time, and only then
     * start the post-close grace period -- with the exact `closingButton` value passed through so
     * that only the button that actually closed the menu gets blocked during the grace period.
     *
     * `controllerInput` is swapped for a `spyk()` wrapping the SAME real instance (not a bare
     * mock): every call still runs its real implementation, so ordering is verified without
     * changing behavior.
     */
    @Test
    fun `fechar o menu limpa o estado do combo em ordem e bloqueia so o botao que fechou`() {
        val realControllerInput = getPrivateField<ControllerInput>(viewModel, "controllerInput")
        val spyControllerInput = spyk(realControllerInput)
        setPrivateField(viewModel, "controllerInput", spyControllerInput)

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        val closingButton = KeyEvent.KEYCODE_BUTTON_B

        viewModel.navigationController?.onMenuClosedCallback?.invoke(closingButton)

        verifyOrder {
            spyControllerInput.clearMenuActionButtons()
            spyControllerInput.resetComboAlreadyTriggered()
            spyControllerInput.clearKeyLog()
            spyControllerInput.updateMenuCloseDebounceTime()
            spyControllerInput.keepInterceptingButtons(200, closingButton = closingButton)
        }

        // Prove the exact closingButton value really reached keepInterceptingButtons: during
        // the grace period, only the button that closed the menu is intercepted on ACTION_UP.
        // ACTION_UP is used (rather than ACTION_DOWN) so this check doesn't also invoke
        // menuBackCallback / navigationController as a side effect.
        assertTrue(
                "the button that closed the menu should be intercepted during the grace period",
                spyControllerInput.processGamePadButtonEvent(closingButton, KeyEvent.ACTION_UP)
        )
        assertFalse(
                "a different button should NOT be intercepted during the grace period",
                spyControllerInput.processGamePadButtonEvent(
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.ACTION_UP
                )
        )
    }

    /**
     * Test 3 (regression guard for the bundle-copy consolidation): after `setupMenuCallback` runs
     * once, all 16 real `ControllerInputCallbacks` fields (the 4 set in `init {}` plus the 12 set
     * by `setupMenuCallback`) must be non-default. Only the dead `menuCallback` field (never
     * assigned anywhere) is expected to stay at its default. Comparing by identity, not behavior,
     * because function references from distinct lambda literals are never `===`/`==` to each
     * other -- so this reliably catches "a callback got dropped during the copy() consolidation".
     */
    @Test
    fun `setupMenuCallback substitui todos os 16 callbacks reais e deixa o campo morto no default`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        val controllerInput = getPrivateField<ControllerInput>(viewModel, "controllerInput")
        val callbacks = controllerInput.callbacks
        val defaults = ControllerInputCallbacks()

        // Set in init {} (untouched by this task, but part of the 16 "real" fields).
        assertNotSame(defaults.selectStartComboCallback, callbacks.selectStartComboCallback)
        assertNotSame(defaults.startButtonCallback, callbacks.startButtonCallback)
        assertNotSame(
                defaults.shouldHandleSelectStartCombo,
                callbacks.shouldHandleSelectStartCombo
        )
        assertNotSame(
                defaults.shouldHandleGamepadMenuButton,
                callbacks.shouldHandleGamepadMenuButton
        )

        // Set by setupMenuCallback() (the 12 under test).
        assertNotSame(defaults.gamepadMenuButtonCallback, callbacks.gamepadMenuButtonCallback)
        assertNotSame(defaults.menuNavigateUpCallback, callbacks.menuNavigateUpCallback)
        assertNotSame(defaults.menuNavigateDownCallback, callbacks.menuNavigateDownCallback)
        assertNotSame(defaults.menuNavigateLeftCallback, callbacks.menuNavigateLeftCallback)
        assertNotSame(defaults.menuNavigateRightCallback, callbacks.menuNavigateRightCallback)
        assertNotSame(defaults.menuConfirmCallback, callbacks.menuConfirmCallback)
        assertNotSame(defaults.menuBackCallback, callbacks.menuBackCallback)
        assertNotSame(defaults.shouldInterceptDpadForMenu, callbacks.shouldInterceptDpadForMenu)
        assertNotSame(defaults.shouldHandleStartButton, callbacks.shouldHandleStartButton)
        assertNotSame(defaults.shouldBlockAllGamepadInput, callbacks.shouldBlockAllGamepadInput)
        assertNotSame(defaults.isRetroMenu3Open, callbacks.isRetroMenu3Open)
        assertNotSame(defaults.isMenuOperationSafe, callbacks.isMenuOperationSafe)

        // Dead field, never assigned anywhere -- must stay at the class's own default.
        assertSame(defaults.menuCallback, callbacks.menuCallback)
    }

    // ===== Characterization tests for the 10 registerXFragment[ForRotation] methods =====
    //
    // Written BEFORE consolidating them into a single `registerSubmenuFragment` helper, to pin
    // down the real (and non-uniform) per-method combination of:
    //   (a) setting the fragment field,
    //   (b) calling `menuViewModel.registerXFragment(fragment)`,
    //   (c) calling `activateXMenu()`,
    //   (d) calling `menuManager.registerFragment(MenuState.X, fragment)`.
    // `menuViewModel` and `menuManager` are swapped for relaxed mocks (via the same
    // reflection-based field injection used elsewhere in this file) because
    // `MenuViewModel.registerXFragment` only assigns a private field that's never read back --
    // there is no other observable side effect to assert on. `activateXMenu()`'s effect IS
    // observable through the real (untouched) `menuStateManager` field, so that one is checked
    // directly instead of being mocked.

    @Test
    fun `registerSettingsMenuFragment define o fragmento, notifica o menuViewModel, ativa o menu e registra no menuManager`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)

        viewModel.registerSettingsMenuFragment(fragment)

        assertTrue(viewModel.isSettingsMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify(exactly = 1) { menuViewModelMock.registerSettingsMenuFragment(fragment) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.SETTINGS_MENU, fragment) }
    }

    /**
     * Pinning test: unlike Progress/Exit's rotation variants, Settings' rotation variant calls
     * NEITHER `menuViewModel.registerSettingsMenuFragment` NOR `activateSettingsMenu()`.
     */
    @Test
    fun `registerSettingsMenuFragmentForRotation define o fragmento mas NAO notifica o menuViewModel nem ativa o menu`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)

        viewModel.registerSettingsMenuFragmentForRotation(fragment)

        assertTrue(viewModel.isSettingsMenuOpen())
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify(exactly = 0) { menuViewModelMock.registerSettingsMenuFragment(any()) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.SETTINGS_MENU, fragment) }
    }

    @Test
    fun `registerProgressFragment define o fragmento, notifica o menuViewModel, ativa o menu e registra no menuManager`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<ProgressFragment>(relaxed = true)

        viewModel.registerProgressFragment(fragment)

        assertTrue(viewModel.isProgressMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
        verify(exactly = 1) { menuViewModelMock.registerProgressFragment(fragment) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.PROGRESS_MENU, fragment) }
    }

    /**
     * Pinning test: unlike Settings' rotation variant, Progress' rotation variant DOES still call
     * `menuViewModel.registerProgressFragment` -- it just skips `activateProgressMenu()`. This is
     * the asymmetry most likely to be silently "fixed away" by an over-eager refactor.
     */
    @Test
    fun `registerProgressFragmentForRotation define o fragmento e notifica o menuViewModel mas NAO ativa o menu`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<ProgressFragment>(relaxed = true)

        viewModel.registerProgressFragmentForRotation(fragment)

        assertTrue(viewModel.isProgressMenuOpen())
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
        verify(exactly = 1) { menuViewModelMock.registerProgressFragment(fragment) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.PROGRESS_MENU, fragment) }
    }

    @Test
    fun `registerExitFragment define o fragmento, notifica o menuViewModel, ativa o menu e registra no menuManager`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<ExitFragment>(relaxed = true)

        viewModel.registerExitFragment(fragment)

        assertTrue(viewModel.isExitMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))
        verify(exactly = 1) { menuViewModelMock.registerExitFragment(fragment) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.EXIT_MENU, fragment) }
    }

    /** Pinning test: Exit's rotation variant also still calls `menuViewModel`, like Progress'. */
    @Test
    fun `registerExitFragmentForRotation define o fragmento e notifica o menuViewModel mas NAO ativa o menu`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<ExitFragment>(relaxed = true)

        viewModel.registerExitFragmentForRotation(fragment)

        assertTrue(viewModel.isExitMenuOpen())
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.EXIT_MENU))
        verify(exactly = 1) { menuViewModelMock.registerExitFragment(fragment) }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.EXIT_MENU, fragment) }
    }

    /**
     * Pinning test: About never calls `menuViewModel` at all (it has no `registerAboutFragment`
     * method on `MenuViewModel` to begin with), in either variant -- but its plain variant DOES
     * still call `activateAboutMenu()`.
     */
    @Test
    fun `registerAboutFragment define o fragmento e ativa o menu mas NUNCA chama o menuViewModel`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<AboutFragment>(relaxed = true)

        viewModel.registerAboutFragment(fragment)

        assertSame(fragment, getPrivateField<AboutFragment?>(viewModel, "aboutFragment"))
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
        verify { menuViewModelMock wasNot Called }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.ABOUT_MENU, fragment) }
    }

    @Test
    fun `registerAboutFragmentForRotation define o fragmento mas NAO ativa o menu nem chama o menuViewModel`() {
        val menuViewModelMock = mockMenuViewModel()
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<AboutFragment>(relaxed = true)

        viewModel.registerAboutFragmentForRotation(fragment)

        assertSame(fragment, getPrivateField<AboutFragment?>(viewModel, "aboutFragment"))
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
        verify { menuViewModelMock wasNot Called }
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.ABOUT_MENU, fragment) }
    }

    // ===== Characterization tests for loadStateCentralized / saveStateCentralized /
    // resetGameCentralized =====
    //
    // Written BEFORE extracting these three methods into SaveLoadOrchestrator, to pin down:
    // every early-return guard, the exact frame-speed save/restore sequence around the actual
    // load/save, the postDelayed(200)-based unpause used only when saving a paused emulator that
    // isn't explicitly being kept paused, and that `skipNextTempStateLoad` is only ever set when a
    // load really happened.

    /**
     * Builds a mocked [RetroView] whose [RetroView.frameRendered] value and `view.frameSpeed` are
     * pinned to the given values, plus the mocked [GLRetroView] backing `view`, so tests can
     * verify frame-speed mutations and state save/load calls against one stable instance.
     */
    private fun mockRetroView(
            frameRendered: Boolean?,
            frameSpeed: Int
    ): Pair<RetroView, GLRetroView> {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        every { glRetroView.frameSpeed } returns frameSpeed
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns MutableLiveData(frameRendered)
        return retroView to glRetroView
    }

    /** Injects a mocked [RetroViewUtils] into the private `retroViewUtils` field. */
    private fun mockRetroViewUtils(hasSaveState: Boolean = true): RetroViewUtils {
        val utils = mockk<RetroViewUtils>(relaxed = true)
        every { utils.hasSaveState() } returns hasSaveState
        setPrivateField(viewModel, "retroViewUtils", utils)
        return utils
    }

    // --- loadStateCentralized: no-op guard paths ---

    @Test
    fun `loadStateCentralized nao faz nada quando retroView e nulo`() {
        viewModel.retroView = null
        val utils = mockRetroViewUtils(hasSaveState = true)

        var completed = false
        viewModel.loadStateCentralized { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { utils.loadState(any()) }
        assertFalse(getPrivateField<Boolean>(viewModel, "skipNextTempStateLoad"))
    }

    @Test
    fun `loadStateCentralized nao faz nada quando frameRendered nao e true`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = false, frameSpeed = 1)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils(hasSaveState = true)

        var completed = false
        viewModel.loadStateCentralized { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { utils.loadState(any()) }
        verify(exactly = 0) { glRetroView.frameSpeed = any() }
        assertFalse(getPrivateField<Boolean>(viewModel, "skipNextTempStateLoad"))
    }

    @Test
    fun `loadStateCentralized nao faz nada quando retroViewUtils e nulo`() {
        val (retroView, _) = mockRetroView(frameRendered = true, frameSpeed = 1)
        viewModel.retroView = retroView
        setPrivateField<RetroViewUtils?>(viewModel, "retroViewUtils", null)

        var completed = false
        viewModel.loadStateCentralized { completed = true }

        assertTrue(completed)
        assertFalse(getPrivateField<Boolean>(viewModel, "skipNextTempStateLoad"))
    }

    @Test
    fun `loadStateCentralized nao faz nada quando nao ha save state`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 1)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils(hasSaveState = false)

        var completed = false
        viewModel.loadStateCentralized { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { utils.loadState(any()) }
        verify(exactly = 0) { glRetroView.frameSpeed = any() }
        assertFalse(getPrivateField<Boolean>(viewModel, "skipNextTempStateLoad"))
    }

    // --- loadStateCentralized: the real load path ---

    @Test
    fun `loadStateCentralized com save state disponivel despausa carrega e restaura o frameSpeed em ordem`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 3)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils(hasSaveState = true)

        var completed = false
        viewModel.loadStateCentralized { completed = true }

        verifyOrder {
            glRetroView.frameSpeed = 1
            utils.loadState(retroView)
            glRetroView.frameSpeed = 3
        }
        assertTrue(completed)
        assertTrue(getPrivateField<Boolean>(viewModel, "skipNextTempStateLoad"))
    }

    // --- saveStateCentralized: paused emulator, not explicitly kept paused -> delayed save ---

    @Test
    fun `saveStateCentralized com emulador pausado e keepPaused false despausa salva apos 200ms e restaura o pause`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 0)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils()

        var completed = false
        viewModel.saveStateCentralized(onComplete = { completed = true }, keepPaused = false)

        // Unpausing happens synchronously; the actual save + re-pause are still pending on the
        // main looper's delayed message queue (Robolectric's paused scheduler does not run them
        // until the looper is explicitly idled).
        verify { glRetroView.frameSpeed = 1 }
        verify(exactly = 0) { utils.saveState(any()) }
        verify(exactly = 0) { glRetroView.frameSpeed = 0 }
        assertFalse(completed)

        // idle() alone only runs tasks already due; the save/restore runnable is scheduled 200ms
        // in the future, so the main looper's virtual clock must be advanced past that point.
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))

        verifyOrder {
            glRetroView.frameSpeed = 1
            utils.saveState(retroView)
            glRetroView.frameSpeed = 0
        }
        assertTrue(completed)
    }

    // --- saveStateCentralized: immediate paths (no delay, no frame-speed manipulation) ---

    @Test
    fun `saveStateCentralized com keepPaused true salva imediatamente sem tocar no frameSpeed`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 0)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils()

        var completed = false
        viewModel.saveStateCentralized(onComplete = { completed = true }, keepPaused = true)

        verify(exactly = 1) { utils.saveState(retroView) }
        verify(exactly = 0) { glRetroView.frameSpeed = any() }
        assertTrue(completed)
    }

    @Test
    fun `saveStateCentralized com frameSpeed diferente de zero salva imediatamente sem tocar no frameSpeed`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 1)
        viewModel.retroView = retroView
        val utils = mockRetroViewUtils()

        var completed = false
        viewModel.saveStateCentralized(onComplete = { completed = true }, keepPaused = false)

        verify(exactly = 1) { utils.saveState(retroView) }
        verify(exactly = 0) { glRetroView.frameSpeed = any() }
        assertTrue(completed)
    }

    @Test
    fun `saveStateCentralized nao faz nada quando retroView e nulo mas ainda chama onComplete`() {
        viewModel.retroView = null
        val utils = mockRetroViewUtils()

        var completed = false
        viewModel.saveStateCentralized(onComplete = { completed = true })

        assertTrue(completed)
        verify(exactly = 0) { utils.saveState(any()) }
    }

    // --- resetGameCentralized ---

    @Test
    fun `resetGameCentralized chama reset na view e invoca onComplete`() {
        val (retroView, glRetroView) = mockRetroView(frameRendered = true, frameSpeed = 1)
        viewModel.retroView = retroView

        var completed = false
        viewModel.resetGameCentralized { completed = true }

        verify(exactly = 1) { glRetroView.reset() }
        assertTrue(completed)
    }

    @Test
    fun `resetGameCentralized com retroView nulo nao quebra e ainda invoca onComplete`() {
        viewModel.retroView = null

        var completed = false
        viewModel.resetGameCentralized { completed = true }

        assertTrue(completed)
    }
}
