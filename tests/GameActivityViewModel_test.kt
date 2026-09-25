package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.input.ControllerInputCallbacks
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuAction
import com.vinaooo.revenger.ui.retromenu3.MenuEvent
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
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
     * Reads the currently registered `aboutFragment` from the private `submenuFragmentState`
     * field -- there is no public `isAboutMenuOpen()` accessor (unlike Settings/Progress/Exit),
     * so this is the only way to assert on it from outside `GameActivityViewModel`.
     */
    private fun registeredAboutFragment(): AboutFragment? {
        val state =
                getPrivateField<
                        com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentState>(
                        viewModel, "submenuFragmentState"
                )
        return state.aboutFragment
    }

    /** Invokes a private, single-overload method by name -- used for the GamePad-event routing
     * helpers, which have no public entry point of their own. */
    private fun <T> invokePrivate(target: Any, methodName: String, vararg args: Any?): T {
        val method = target.javaClass.declaredMethods.first { it.name == methodName }
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST") return method.invoke(target, *args) as T
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
     * `suppressNextScreenshotCapture` is set directly by `PipController` (`viewModel.
     * suppressNextScreenshotCapture = true`) to skip the next save-state screenshot. Pins that
     * contract through the ViewModel's public API: the capture is skipped, the callback still
     * reports success, and the flag resets so the capture after that runs normally again.
     */
    @Test
    fun `captureScreenshotForSaveState suprimido pula a captura e reseta a flag`() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any()) } just Runs
        every { ScreenshotCaptureUtil.capturePipFrame(any(), any()) } just Runs
        viewModel.retroView = mockk<RetroView>(relaxed = true)
        viewModel.suppressNextScreenshotCapture = true

        var firstResult: Boolean? = null
        viewModel.captureScreenshotForSaveState { firstResult = it }

        assertTrue(firstResult == true)
        assertFalse(viewModel.suppressNextScreenshotCapture)
        verify { ScreenshotCaptureUtil wasNot Called }

        viewModel.captureScreenshotForSaveState()

        verify(exactly = 1) { ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any()) }
    }

    /**
     * Test 2: closing the menu must, in order, clear the menu action buttons, reset the
     * already-triggered combo flag, clear the key log, update the debounce time, and only then
     * start the post-close grace period -- with the exact `closingButton` value passed through so
     * that only the button that actually closed the menu gets blocked during the grace period.
     *
     * `comboTracker` and `callbackDebouncer` (the two collaborators `ControllerInput` delegates
     * this state to) are each swapped for a `spyk()` wrapping the SAME real instance (not a bare
     * mock): every call still runs its real implementation, so ordering is verified without
     * changing behavior. `mockk`'s `verifyOrder` accepts calls on more than one spy in a single
     * block, so the cross-object order is still checked exactly as before this state was split
     * out of `ControllerInput` itself.
     */
    @Test
    fun `fechar o menu limpa o estado do combo em ordem e bloqueia so o botao que fechou`() {
        val realControllerInput = getPrivateField<ControllerInput>(viewModel, "controllerInput")
        val spyComboTracker = spyk(realControllerInput.comboTracker)
        setPrivateField(realControllerInput, "comboTracker", spyComboTracker)
        val spyCallbackDebouncer = spyk(realControllerInput.callbackDebouncer)
        setPrivateField(realControllerInput, "callbackDebouncer", spyCallbackDebouncer)

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        val closingButton = KeyEvent.KEYCODE_BUTTON_B

        viewModel.navigationController?.onMenuClosedCallback?.invoke(closingButton)

        verifyOrder {
            spyComboTracker.clearMenuActionButtons()
            spyComboTracker.resetComboAlreadyTriggered()
            spyComboTracker.clearKeyLog()
            spyComboTracker.updateMenuCloseDebounceTime()
            spyCallbackDebouncer.keepInterceptingButtons(200, closingButton = closingButton)
        }

        // Prove the exact closingButton value really reached keepInterceptingButtons: during
        // the grace period, only the button that closed the menu is intercepted on ACTION_UP.
        // ACTION_UP is used (rather than ACTION_DOWN) so this check doesn't also invoke
        // menuBackCallback / navigationController as a side effect.
        assertTrue(
                "the button that closed the menu should be intercepted during the grace period",
                realControllerInput.processGamePadButtonEvent(closingButton, KeyEvent.ACTION_UP)
        )
        assertFalse(
                "a different button should NOT be intercepted during the grace period",
                realControllerInput.processGamePadButtonEvent(
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.ACTION_UP
                )
        )
    }

    /**
     * Test 3 (regression guard for the bundle-copy consolidation): after `setupMenuCallback` runs
     * once, all 16 `ControllerInputCallbacks` fields (the 4 set in `init {}` plus the 12 set
     * by `setupMenuCallback`) must be non-default. Comparing by identity, not behavior,
     * because function references from distinct lambda literals are never `===`/`==` to each
     * other -- so this reliably catches "a callback got dropped during the copy() consolidation".
     */
    @Test
    fun `setupMenuCallback substitui todos os 16 callbacks reais`() {
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
    }

    // ===== Characterization tests for the 8 registerXFragment[ForRotation] methods =====
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
     * Asserts `unregisterSettingsMenuFragment` deactivates the Settings menu type in
     * `menuStateManager`, not just that it gets called (`SubmenuCoordinator_test.kt` only checks
     * the latter, against a mocked `viewModel`).
     */
    @Test
    fun `unregisterSettingsMenuFragment desativa o menu de settings`() {
        mockMenuViewModel()
        mockMenuManager()
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        viewModel.registerSettingsMenuFragment(fragment)
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))

        viewModel.unregisterSettingsMenuFragment()

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        assertFalse(viewModel.isSettingsMenuOpen())
    }

    // -------------------------------------------------------------------------------------
    // Characterization tests for the private `dismissSubmenuFragment` helper, pinned via its
    // public callers (`dismissSettingsMenu`/`dismissProgress`/`dismissExit`/`dismissAboutMenu`)
    // before any refactor touches it.
    // -------------------------------------------------------------------------------------

    /**
     * A registered fragment that is no longer added (e.g. already detached) makes
     * `dismissSubmenuFragment` return early: the field, menu-active state and current menu are
     * all left untouched.
     */
    @Test
    fun `dismissSettingsMenu nao faz nada quando o fragmento nao esta mais added`() {
        val menuManagerMock = mockMenuManager()
        mockMenuViewModel()
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns false
        viewModel.registerSettingsMenuFragment(fragment)

        viewModel.dismissSettingsMenu()

        assertTrue(viewModel.isSettingsMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify(exactly = 0) { menuManagerMock.navigateToState(any()) }
    }

    /**
     * An added fragment with no attached `Activity` skips the back-stack pop entirely (the
     * `activity != null` guard), but still clears its field, deactivates its menu type and
     * navigates back to the main menu.
     */
    @Test
    fun `dismissSettingsMenu sem activity anexada ainda limpa o estado mas nao tenta pop`() {
        val menuManagerMock = mockMenuManager()
        mockMenuViewModel()
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        every { fragment.activity } returns null
        viewModel.registerSettingsMenuFragment(fragment)

        viewModel.dismissSettingsMenu()

        assertFalse(viewModel.isSettingsMenuOpen())
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
        verify(exactly = 1) { menuManagerMock.navigateToState(MenuState.MAIN_MENU) }
    }

    /** A non-empty back stack is popped exactly once. */
    @Test
    fun `dismissSettingsMenu com back stack nao vazio chama popBackStackImmediate`() {
        mockMenuManager()
        mockMenuViewModel()
        val fragmentManager = mockk<androidx.fragment.app.FragmentManager>(relaxed = true)
        every { fragmentManager.backStackEntryCount } returns 1
        val activity = mockk<FragmentActivity>(relaxed = true)
        every { activity.supportFragmentManager } returns fragmentManager
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        every { fragment.activity } returns activity
        viewModel.registerSettingsMenuFragment(fragment)

        viewModel.dismissSettingsMenu()

        verify(exactly = 1) { fragmentManager.popBackStackImmediate() }
    }

    /** An empty back stack is left alone -- `popBackStackImmediate` is never called. */
    @Test
    fun `dismissSettingsMenu com back stack vazio nao chama popBackStackImmediate`() {
        mockMenuManager()
        mockMenuViewModel()
        val fragmentManager = mockk<androidx.fragment.app.FragmentManager>(relaxed = true)
        every { fragmentManager.backStackEntryCount } returns 0
        val activity = mockk<FragmentActivity>(relaxed = true)
        every { activity.supportFragmentManager } returns fragmentManager
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        every { fragment.activity } returns activity
        viewModel.registerSettingsMenuFragment(fragment)

        viewModel.dismissSettingsMenu()

        verify(exactly = 0) { fragmentManager.popBackStackImmediate() }
        assertFalse(viewModel.isSettingsMenuOpen())
    }

    /**
     * Each `dismissX` must clear only its own fragment field and menu type -- guards against a
     * copy-paste mistake in the eventual extraction touching the wrong one.
     */
    @Test
    fun `dismissSettingsMenu nao afeta o estado do menu de Progress`() {
        mockMenuManager()
        mockMenuViewModel()
        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        every { settingsFragment.activity } returns null
        viewModel.registerSettingsMenuFragment(settingsFragment)
        val progressFragment = mockk<ProgressFragment>(relaxed = true)
        every { progressFragment.isAdded } returns true
        every { progressFragment.activity } returns null
        viewModel.registerProgressFragment(progressFragment)

        viewModel.dismissSettingsMenu()

        assertFalse(viewModel.isSettingsMenuOpen())
        assertTrue(viewModel.isProgressMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.PROGRESS_MENU))
    }

    @Test
    fun `dismissProgress nao afeta o estado do menu de Settings`() {
        mockMenuManager()
        mockMenuViewModel()
        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        every { settingsFragment.activity } returns null
        viewModel.registerSettingsMenuFragment(settingsFragment)
        val progressFragment = mockk<ProgressFragment>(relaxed = true)
        every { progressFragment.isAdded } returns true
        every { progressFragment.activity } returns null
        viewModel.registerProgressFragment(progressFragment)

        viewModel.dismissProgress()

        assertFalse(viewModel.isProgressMenuOpen())
        assertTrue(viewModel.isSettingsMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `dismissExit nao afeta o estado do menu de Settings`() {
        mockMenuManager()
        mockMenuViewModel()
        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        every { settingsFragment.activity } returns null
        viewModel.registerSettingsMenuFragment(settingsFragment)
        val exitFragment = mockk<ExitFragment>(relaxed = true)
        every { exitFragment.isAdded } returns true
        every { exitFragment.activity } returns null
        viewModel.registerExitFragment(exitFragment)

        viewModel.dismissExit()

        assertFalse(viewModel.isExitMenuOpen())
        assertTrue(viewModel.isSettingsMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `dismissAboutMenu nao afeta o estado do menu de Settings`() {
        mockMenuManager()
        mockMenuViewModel()
        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        every { settingsFragment.activity } returns null
        viewModel.registerSettingsMenuFragment(settingsFragment)
        val aboutFragment = mockk<AboutFragment>(relaxed = true)
        every { aboutFragment.isAdded } returns true
        every { aboutFragment.activity } returns null
        viewModel.registerAboutFragment(aboutFragment)

        viewModel.dismissAboutMenu()

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.ABOUT_MENU))
        assertTrue(viewModel.isSettingsMenuOpen())
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
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

        assertSame(fragment, registeredAboutFragment())
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

        assertSame(fragment, registeredAboutFragment())
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

    // --- handleGamePadEvent / setupGamePads ---
    //
    // Characterization coverage for the GamePad-event routing that used to be duplicated verbatim
    // inline in setupGamePads's left/right GamePad callbacks (detekt LongMethod), now shared via
    // GamePadInputController's handleGamePadEvent/handleGamePadDirectionEvent/buildDpadMotionEvent.

    @Test
    fun `handleGamePadEvent com Button delega para controllerInput e retorna o resultado`() {
        val controllerInput = mockk<ControllerInput>(relaxed = true)
        every { controllerInput.processGamePadButtonEvent(99, KeyEvent.ACTION_DOWN) } returns true
        setPrivateField(viewModel, "controllerInput", controllerInput)
        val gamePadInputController = getPrivateField<Any>(viewModel, "gamePadInputController")

        val result =
                invokePrivate<Boolean>(
                        gamePadInputController,
                        "handleGamePadEvent",
                        Event.Button(99, KeyEvent.ACTION_DOWN, 0)
                )

        assertTrue(result)
        verify { controllerInput.processGamePadButtonEvent(99, KeyEvent.ACTION_DOWN) }
    }

    @Test
    fun `handleGamePadEvent com Direction e menu fechado nao intercepta`() {
        val controllerInput = mockk<ControllerInput>(relaxed = true)
        setPrivateField(viewModel, "controllerInput", controllerInput)
        val navigationController = mockk<NavigationController>(relaxed = true)
        every { navigationController.isMenuActive() } returns false
        viewModel.navigationController = navigationController
        val gamePadInputController = getPrivateField<Any>(viewModel, "gamePadInputController")

        val result =
                invokePrivate<Boolean>(
                        gamePadInputController,
                        "handleGamePadEvent",
                        Event.Direction(0, 1f, 0f, 0)
                )

        assertFalse(result)
        verify(exactly = 0) { controllerInput.processMotionEvent(any(), any()) }
    }

    @Test
    fun `handleGamePadEvent com Direction e menu aberto converte para MotionEvent e intercepta`() {
        val controllerInput = mockk<ControllerInput>(relaxed = true)
        setPrivateField(viewModel, "controllerInput", controllerInput)
        val navigationController = mockk<NavigationController>(relaxed = true)
        every { navigationController.isMenuActive() } returns true
        viewModel.navigationController = navigationController
        val (retroView, _) = mockRetroView(frameRendered = true, frameSpeed = 1)
        viewModel.retroView = retroView
        val gamePadInputController = getPrivateField<Any>(viewModel, "gamePadInputController")

        val motionEventSlot = slot<MotionEvent>()
        every { controllerInput.processMotionEvent(capture(motionEventSlot), any()) } returns true

        val result =
                invokePrivate<Boolean>(
                        gamePadInputController,
                        "handleGamePadEvent",
                        Event.Direction(0, 0.7f, -0.4f, 0)
                )

        assertTrue(result)
        val captured = motionEventSlot.captured
        assertEquals(0.7f, captured.getAxisValue(MotionEvent.AXIS_HAT_X))
        assertEquals(-0.4f, captured.getAxisValue(MotionEvent.AXIS_HAT_Y))
    }

    @Test
    fun `handleGamePadEvent com outro tipo de evento nao intercepta`() {
        val gamePadInputController = getPrivateField<Any>(viewModel, "gamePadInputController")

        val result =
                invokePrivate<Boolean>(
                        gamePadInputController,
                        "handleGamePadEvent",
                        com.swordfish.radialgamepad.library.event.Event.Gesture(
                                0,
                                com.swordfish.radialgamepad.library.event.GestureType.SINGLE_TAP
                        )
                )

        assertFalse(result)
    }

    // --- onMenuEvent: Action/StateChanged dispatch to MenuActionDispatcher/MenuStateChangeHandler
    // ---
    //
    // Characterization coverage for onMenuEvent's top-level branches: one test per branch
    // dispatched to its handler, plus representative MenuAction sub-branches and the BACK
    // action's per-state dismiss routing.

    @Test
    fun `onMenuEvent com Action NAVIGATE delega para menuManager navigateToState`() {
        val menuManagerMock = mockMenuManager()

        viewModel.onMenuEvent(MenuEvent.Action(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU)))

        verify(exactly = 1) { menuManagerMock.navigateToState(MenuState.SETTINGS_MENU) }
    }

    @Test
    fun `onMenuEvent com StateChanged para SETTINGS_MENU ativa o menu de configuracoes`() {
        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU))

        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `onMenuEvent com StateChanged saindo de SETTINGS_MENU desativa o menu de configuracoes`() {
        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.MAIN_MENU, MenuState.SETTINGS_MENU))
        assertTrue(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))

        viewModel.onMenuEvent(MenuEvent.StateChanged(MenuState.SETTINGS_MENU, MenuState.MAIN_MENU))

        assertFalse(isMenuTypeActive(MenuSystemState.MenuType.SETTINGS_MENU))
    }

    @Test
    fun `onMenuEvent com Action EXIT mata o processo sem salvar`() {
        mockkStatic(android.os.Process::class)
        every { android.os.Process.killProcess(any()) } just Runs
        try {
            viewModel.retroView = null
            val utils = mockRetroViewUtils()

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.EXIT))

            verify(exactly = 1) { android.os.Process.killProcess(android.os.Process.myPid()) }
            verify(exactly = 0) { utils.saveState(any()) }
        } finally {
            unmockkStatic(android.os.Process::class)
        }
    }

    @Test
    fun `onMenuEvent com Action SAVE_AND_EXIT salva e entao mata o processo`() {
        mockkStatic(android.os.Process::class)
        every { android.os.Process.killProcess(any()) } just Runs
        try {
            // retroView permanece nulo (default do fixture): saveStateCentralized nao salva nada
            // mas ainda chama onComplete de forma sincrona, o que basta para exercitar a ordem
            // "salva, depois mata o processo" sem precisar mockar o RetroView inteiro.
            viewModel.retroView = null

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.SAVE_AND_EXIT))

            verify(exactly = 1) { android.os.Process.killProcess(android.os.Process.myPid()) }
        } finally {
            unmockkStatic(android.os.Process::class)
        }
    }

    @Test
    fun `onMenuEvent com Action BACK no MAIN_MENU chama dismissRetroMenu3`() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        try {
            val menuManagerMock = mockMenuManager()
            every { menuManagerMock.getCurrentState() } returns MenuState.MAIN_MENU

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.BACK))

            verify {
                android.util.Log.d(
                        "GameActivityViewModel",
                        "[DISMISS_MAIN] dismissRetroMenu3: Starting"
                )
            }
        } finally {
            unmockkStatic(android.util.Log::class)
        }
    }

    @Test
    fun `onMenuEvent com Action BACK no SETTINGS_MENU chama dismissSettingsMenu`() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        try {
            val menuManagerMock = mockMenuManager()
            every { menuManagerMock.getCurrentState() } returns MenuState.SETTINGS_MENU

            viewModel.onMenuEvent(MenuEvent.Action(MenuAction.BACK))

            verify { android.util.Log.d("GameActivityViewModel", "dismissSettingsMenu: Starting") }
        } finally {
            unmockkStatic(android.util.Log::class)
        }
    }
}
