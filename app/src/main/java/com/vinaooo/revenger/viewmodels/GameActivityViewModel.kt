package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.AudioController
import com.vinaooo.revenger.controllers.ShaderController
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.viewmodels.menu.SaveLoadOrchestrator
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application),
        SettingsMenuListener,
        AboutListener,
        MenuManager.MenuManagerListener {

    companion object {
        // Grace period after the menu closes during which button interception stays active.
        // Covers the ~150ms hardware delay observed between ACTION_DOWN and ACTION_UP; a
        // shorter window (50ms) was found insufficient.
        private const val MENU_CLOSE_BUTTON_INTERCEPT_GRACE_MS = 200L

        // Delay before clearing state after dismissing the RetroMenu3 fragment, to let the
        // pending fragment removal complete first.
        private const val RETRO_MENU3_FRAGMENT_REMOVAL_SETTLE_DELAY_MS = 200L

        // Delay before clearing controller input state, to let the pending fragment
        // destruction complete first.
        private const val CONTROLLER_STATE_CLEAR_FRAGMENT_DESTROY_SETTLE_DELAY_MS = 200L
    }

    private val resources = application.resources
    private val appConfig = RevengerApplication.appConfig

    // ===== SPECIALIZED VIEWMODELS =====
    // Using composition pattern to separate concerns

    /** Menu management ViewModel */
    private val menuViewModel: MenuViewModel = MenuViewModel(application)

    /** Input management ViewModel */
    private val inputViewModel: InputViewModel = InputViewModel(application)

    /** Audio management ViewModel */
    private val audioViewModel: AudioViewModel = AudioViewModel(application)

    /** Shader management ViewModel */
    private val shaderViewModel: ShaderViewModel = ShaderViewModel(application)

    /** Speed management ViewModel */
    private val speedViewModel: SpeedViewModel = SpeedViewModel(application)

    /**
     * Navigation controller for multi-input navigation system (Phase 3+). Internal visibility
     * allows fragments to register/unregister themselves.
     */
    internal var navigationController: NavigationController? = null

    /**
     * Keyboard input adapter for physical keyboard navigation (Phase 4). Translates KeyEvents into
     * NavigationEvents for unified input handling.
     */
    internal var keyboardInputAdapter:
            com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter? =
            null

    // ===== SHARED REFERENCES =====
    // These are shared between specialized ViewModels

    var retroView: RetroView? = null
        set(value) {
            field = value
            // FUTURE: gameStateViewModel.setRetroView(value)
        }

    private var retroViewUtils: RetroViewUtils? = null
        set(value) {
            field = value
            // FUTURE: gameStateViewModel.setRetroViewUtils(value)
        }

    private val saveLoadOrchestrator = SaveLoadOrchestrator()

    // Legacy references for backward compatibility
    private var leftGamePad: GamePad? = null
    // FUTURE: get() = inputViewModel.getLeftGamePad()
    // FUTURE: set(value) { value?.let { inputViewModel.setLeftGamePad(it) } }

    private var rightGamePad: GamePad? = null
    // FUTURE: get() = inputViewModel.getRightGamePad()
    // FUTURE: set(value) { value?.let { inputViewModel.setRightGamePad(it) } }

    // Menu container reference (from activity layout) - delegated to MenuViewModel
    private var menuContainerView: FrameLayout? = null
        set(value) {
            field = value
            // FUTURE: value?.let { menuViewModel.setMenuContainer(container = it) }
        }

    // GamePad container reference (needed to force it on top of menu)
    private var gamePadContainerView: android.widget.LinearLayout? = null

    // RetroMenu3 fragment (activated by SELECT+START combo)
    private var retroMenu3Fragment: RetroMenu3Fragment? = null

    // Settings submenu fragment
    private var settingsMenuFragment: SettingsMenuFragment? = null

    // New submenu fragments
    private var progressFragment: ProgressFragment? = null
    private var exitFragment: ExitFragment? = null
    private var aboutFragment: AboutFragment? = null
    private var coreVariablesFragment: com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment? = null

    // ===== LOAD PREVIEW OVERLAY =====

    /**
     * Callback to show/hide the full-screen preview overlay in GameActivity. Set by GameActivity
     * during initialization, called by fragments on slot selection.
     */
    var loadPreviewCallback: ((android.graphics.Bitmap?) -> Unit)? = null

    /**
     * Show the load preview overlay with the given bitmap. Used when user navigates between slots
     * in Load State grid.
     */
    fun showLoadPreview(bitmap: android.graphics.Bitmap) {
        loadPreviewCallback?.invoke(bitmap)
    }

    /**
     * Hide the load preview overlay. Called when navigating away from Load State or when menu
     * closes.
     */
    fun hideLoadPreview() {
        loadPreviewCallback?.invoke(null)
    }

    /**
     * Get the cached full-screen screenshot (with black bars) for preview overlay. Used when saving
     * to slot — the full screenshot is saved alongside the cropped one.
     */
    fun getCachedFullScreenshot(): android.graphics.Bitmap? {
        return com.vinaooo.revenger.utils.ScreenshotCaptureUtil.getCachedFullScreenshot()
    }

    // ===== CENTRALIZED STATE MANAGEMENT =====
    // Distributed state migrated to MenuStateManager

    /** Activate settings menu */
    private fun activateSettingsMenu() {
        menuStateManager.activateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.SETTINGS_MENU
        )
    }

    /** Deactivate settings menu */
    private fun deactivateSettingsMenu() {
        menuStateManager.deactivateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.SETTINGS_MENU
        )
    }

    /** Activate progress menu */
    private fun activateProgressMenu() {
        menuStateManager.activateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.PROGRESS_MENU
        )
    }

    /** Deactivate progress menu */
    private fun deactivateProgressMenu() {
        menuStateManager.deactivateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.PROGRESS_MENU
        )
    }

    /** Activate exit menu */
    private fun activateExitMenu() {
        menuStateManager.activateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.EXIT_MENU
        )
    }

    /** Deactivate exit menu */
    private fun deactivateExitMenu() {
        menuStateManager.deactivateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.EXIT_MENU
        )
    }

    /** Activate about menu */
    private fun activateAboutMenu() {
        menuStateManager.activateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.ABOUT_MENU
        )
    }

    /** Deactivate about menu */
    private fun deactivateAboutMenu() {
        menuStateManager.deactivateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.ABOUT_MENU
        )
    }


    /** Deactivate core variables menu */
    private fun deactivateCoreVariablesMenu() {
        menuStateManager.deactivateMenu(
                com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.CORE_VARIABLES_MENU
        )
    }

    /** Set dismissing all menus flag */
    private fun setDismissingAllMenus(dismissing: Boolean) {
        menuStateManager.setDismissingAllMenus(dismissing)
    }

    // Centralized Menu State Manager (must be initialized first for MenuManager)
    private val menuStateManager: com.vinaooo.revenger.ui.retromenu3.MenuStateManager =
            com.vinaooo.revenger.ui.retromenu3.MenuStateManager()

    // Unified Menu Manager for centralized menu navigation
    private val menuManager: MenuManager = MenuManager(this, menuStateManager)

    /** Get the MenuManager instance */
    fun getMenuManager(): MenuManager = menuManager

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    // Controllers modulares
    private var audioController: AudioController? = null
    private var speedController: SpeedController? = null
    private var shaderController: ShaderController? = null
    private var sharedPreferences: android.content.SharedPreferences? = null

    // Flag to prevent tempState from overwriting a manual Load State
    private var skipNextTempStateLoad = false

    init {
        // All ViewModels and managers are now initialized as val at declaration

        // Set the callback to check if SELECT+START combo should work
        controllerInput.shouldHandleSelectStartCombo = { shouldHandleSelectStartCombo() }

        // Set the callback for SELECT+START combo to open menu via NavigationController
        controllerInput.selectStartComboCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.OpenMenu(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        // Set the callback for START button to close all menus via NavigationController
        controllerInput.startButtonCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.CloseAllMenus(
                            keyCode = KeyEvent.KEYCODE_BUTTON_START,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        // Set the callback to check if gamepad menu button should work
        controllerInput.shouldHandleGamepadMenuButton = { shouldHandleGamepadMenuButton() }
    }

    /** Configure menu callback with activity reference */
    fun setupMenuCallback(activity: FragmentActivity) {
        initializeNavigationControllerIfNeeded(activity)
        wireControllerInputMenuCallbacks()
    }

    /**
     * PHASE 3.1a/4.1c: one-time initialization of the [NavigationController] and
     * [com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter], and wiring of the
     * pause/resume-on-menu-open/close callbacks. Guarded so it only runs once per ViewModel
     * instance -- [setupMenuCallback] itself may be called more than once (e.g. on
     * Activity recreation).
     */
    private fun initializeNavigationControllerIfNeeded(activity: FragmentActivity) {
        // PHASE 3.1a: Initialize NavigationController (permanently enabled after Phase 4
        // validation)
        if (navigationController != null) return

        navigationController = NavigationController(activity)

        // PHASE 4.1c: Initialize KeyboardInputAdapter
        keyboardInputAdapter =
                com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter(
                        navigationController!!,
                        { isAnyMenuActive() }
                )

        // PHASE 3.2b: Configurar callbacks para pausar/resumir o jogo
        navigationController?.onMenuOpenedCallback = { handleMenuOpened(activity) }

        navigationController?.onMenuClosedCallback = { closingButton: Int? ->
            handleMenuClosed(activity, closingButton)
        }
    }

    /**
     * Runs when the menu opens: captures the save-state screenshot, preserves emulator state,
     * pauses emulation, and restores floating-button visibility. Exact body of the previous
     * `onMenuOpenedCallback` lambda inside `setupMenuCallback()`, unchanged.
     */
    private fun handleMenuOpened(activity: FragmentActivity) {
        try {
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} " +
                            "thread=${Thread.currentThread().name} - menu opened callback start"
            )
            val menuFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.menu_container)
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] Fragment in container=${menuFragment?.javaClass?.simpleName} " +
                            "backStack=${activity.supportFragmentManager.backStackEntryCount}"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // This block only formats debug strings from trivial fragment-manager getters; there
            // is no narrower reachable type, and Throwable is itself on detekt's generic-exception
            // list, so the name-based escape hatch is used instead of guessing one.
            Log.w(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] failed to log fragment manager state",
                    expectedDiagnosticLoggingFailure
            )
        }

        // Capturar screenshot ANTES de pausar para save states
        captureScreenshotForSaveState()
        // Preservar estado do emulador
        retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
        // PAUSAR o jogo quando menu abre
        retroView?.let { speedController?.pause(it.view) }

        (activity as? FloatingButtonVisibilityHost)?.restoreFloatingButtonVisibility()

        try {
            Log.d(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} - menu opened callback completed"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // Same rationale as the catch above: purely diagnostic logging with no narrower
            // reachable type.
            Log.w(
                    "GameActivityViewModel",
                    "[ON_MENU_OPENED] failed to log completion",
                    expectedDiagnosticLoggingFailure
            )
        }
    }

    /**
     * Runs when the menu closes: fades the floating button, resets combo/key-log state, starts
     * the post-close grace period for the button that closed the menu, promotes the cached
     * screenshot to the PiP frame, resumes emulation, and hides the load preview overlay once the
     * next frame renders. Exact body of the previous `onMenuClosedCallback` lambda inside
     * `setupMenuCallback()`, unchanged.
     */
    private fun handleMenuClosed(activity: FragmentActivity, closingButton: Int?) {
        (activity as? FloatingButtonVisibilityHost)?.fadeFloatingButtonImmediately()

        android.util.Log.d("GameActivityViewModel", "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED =====")
        try {
            Log.d(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] ts=${System.currentTimeMillis()} " +
                            "thread=${Thread.currentThread().name} closingButton=$closingButton"
            )
            val menuFragment = activity.supportFragmentManager.findFragmentById(R.id.menu_container)
            Log.d(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] Fragment in container=" +
                            "${menuFragment?.javaClass?.simpleName} " +
                            "backStack=${activity.supportFragmentManager.backStackEntryCount}"
            )
        } catch (expectedDiagnosticLoggingFailure: Throwable) {
            // Same rationale as handleMenuOpened()'s diagnostic-logging catches: purely debug
            // string formatting with no narrower reachable type.
            Log.w(
                    "GameActivityViewModel",
                    "🔥 [ON_MENU_CLOSED_CALLBACK] failed to log fragment manager state",
                    expectedDiagnosticLoggingFailure
            )
        }
        android.util.Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] Timestamp: ${System.currentTimeMillis()}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] closingButton: $closingButton"
        )

        // Limpar botões de menu do keyLog para evitar "wasAlreadyPressed" bugs
        controllerInput.comboTracker.clearMenuActionButtons()

        // Reset combo state to allow SELECT+START to work again after menu closes
        controllerInput.comboTracker.resetComboAlreadyTriggered()

        // Clear keyLog immediately to prevent residual button states from causing combo
        // detection issues
        controllerInput.comboTracker.clearKeyLog()

        // Update menu close debounce time to prevent immediate combo detection
        controllerInput.comboTracker.updateMenuCloseDebounceTime()

        android.util.Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] comboAlreadyTriggered reset, keyLog cleared, debounce updated"
        )

        // Grace period: keep interception active for 200ms after menu closes
        // 200ms covers the ~150ms hardware delay between ACTION_DOWN and ACTION_UP
        // Identified via logs: UP arrives 150ms later; 50ms was insufficient
        // Block only the button that actually closed the menu
        controllerInput.callbackDebouncer.keepInterceptingButtons(
                MENU_CLOSE_BUTTON_INTERCEPT_GRACE_MS,
                closingButton = closingButton
        )

        // Keep the freshest known frame as the PiP still before dropping the menu caches.
        com.vinaooo.revenger.utils.ScreenshotCaptureUtil.promoteCachedFullToPipFrame()

        // Limpar screenshot cacheado quando menu fecha
        clearCachedScreenshot()

        // RESUMIR o jogo quando menu fecha - aplicar velocidade salva nas preferences
        retroView?.let { speedController?.restoreSpeedFromPreferences(it.view) }

        // Hide load preview overlay AFTER game resumes and the first new frame is rendered
        retroView?.view?.getGLRetroEvents()?.let { events ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                events.first { it == com.swordfish.libretrodroid.GLRetroView.GLRetroEvents.FrameRendered }
                hideLoadPreview()
            }
        } ?: run {
            hideLoadPreview()
        }

        android.util.Log.d(
                "GameActivityViewModel",
                "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED COMPLETED ====="
        )
    }

    /**
     * Wires the 12 `ControllerInput` callback/predicate properties that
     * [setupMenuCallback] re-runs unconditionally on every call (unlike the one-time
     * NavigationController setup above). Collapsed into a single `copy()` of
     * [com.vinaooo.revenger.input.ControllerInputCallbacks] so every other field --
     * the 4 set in `init {}` -- is preserved as-is.
     */
    private fun wireControllerInputMenuCallbacks() {
        controllerInput.callbacks =
                controllerInput.callbacks.copy(
                        gamepadMenuButtonCallback = ::handleGamepadMenuButtonCallback,
                        menuNavigateUpCallback =
                                menuNavigateCallback(
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction.UP
                                ),
                        menuNavigateDownCallback =
                                menuNavigateCallback(
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .DOWN
                                ),
                        menuNavigateLeftCallback =
                                menuNavigateCallback(
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .LEFT
                                ),
                        menuNavigateRightCallback =
                                menuNavigateCallback(
                                        com.vinaooo.revenger.ui.retromenu3.navigation.Direction
                                                .RIGHT
                                ),
                        menuConfirmCallback = {
                            navigationController?.handleNavigationEvent(
                                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
                                            .ActivateSelected(
                                            inputSource =
                                                    com.vinaooo.revenger.ui.retromenu3.navigation
                                                            .InputSource.PHYSICAL_GAMEPAD
                                    )
                            )
                        },
                        menuBackCallback = {
                            navigationController?.handleNavigationEvent(
                                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
                                            .NavigateBack(
                                            keyCode = KeyEvent.KEYCODE_BUTTON_B,
                                            inputSource =
                                                    com.vinaooo.revenger.ui.retromenu3.navigation
                                                            .InputSource.PHYSICAL_GAMEPAD
                                    )
                            )
                        },

                        // CRITICAL: DO NOT check isDismissingAllMenus() here! We need to keep
                        // intercepting buttons even during closing to prevent ACTION_UP from
                        // leaking into the game.
                        shouldInterceptDpadForMenu = { isAnyMenuActive() },

                        // Only when RetroMenu3 or SettingsMenu is REALLY open.
                        shouldHandleStartButton = {
                            isAnyMenuActive() && !isDismissingAllMenus()
                        },
                        shouldBlockAllGamepadInput = { isAnyMenuActive() },
                        isRetroMenu3Open = { isAnyMenuActive() },
                        isMenuOperationSafe = {
                            !isDismissingAllMenus() &&
                                    retroMenu3Fragment?.isDismissingMenu() != true
                        }
                )
    }

    /** Toggle the menu open/closed in response to the physical gamepad's menu button. */
    private fun handleGamepadMenuButtonCallback() {
        if (isAnyMenuActive()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[MENU_BUTTON] Closing ALL menus directly with NavigationController"
            )
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.CloseAllMenus(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        } else {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.OpenMenu(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }
    }

    /**
     * Builds a menu-navigate callback for [direction]; the four `menuNavigateXCallback` fields
     * used to carry four verbatim copies of this same dispatch with only the direction changed.
     */
    private fun menuNavigateCallback(
            direction: com.vinaooo.revenger.ui.retromenu3.navigation.Direction
    ): () -> Unit = {
        navigationController?.handleNavigationEvent(
                com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.Navigate(
                        direction = direction,
                        inputSource =
                                com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                        .PHYSICAL_GAMEPAD
                )
        )
    }

    /** Create an instance of the RetroMenu3 overlay (activated by SELECT+START) */
    fun prepareRetroMenu3() {
        // Skip if fragment already exists
        if (retroMenu3Fragment != null) {
            return
        }

        retroMenu3Fragment =
                RetroMenu3Fragment.newInstance().apply {
                    // REMOVED: setMenuListener - migrated to unified MenuAction/MenuEvent system
                    // setMenuListener(this@GameActivityViewModel)
                }

        // Register RetroMenu3Fragment with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU,
                retroMenu3Fragment!!
        )
    }

    /** Force recreation of RetroMenu3Fragment (used after configuration changes) */
    fun recreateRetroMenu3() {
        // Clean up existing fragment reference
        retroMenu3Fragment = null

        // Recreate the fragment
        prepareRetroMenu3()
    }

    /** Set menu container reference from activity layout */
    fun setMenuContainer(container: FrameLayout) {
        menuContainerView = container
        menuViewModel.setMenuContainer(container)
    }

    /** Get menu container ID for consistent fragment placement */
    fun getMenuContainerId(): Int = menuContainerView?.id ?: R.id.menu_container

    /** Update RetroMenu3Fragment reference after recreation (e.g., after rotation) */
    fun updateRetroMenu3FragmentReference(fragment: RetroMenu3Fragment) {
        retroMenu3Fragment = fragment
        // Re-register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU,
                fragment
        )
    }

    /** Set GamePad container reference to force it on top when menu opens */
    fun setGamePadContainer(container: android.widget.LinearLayout) {
        gamePadContainerView = container
    }

    /** Toggles the Retro Menu 3 open/closed state using the NavigationController */
    fun toggleMainMenu() {
        if (isAnyMenuActive()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[MENU_TOGGLE] Closing ALL menus directly with NavigationController via FloatingButton"
            )
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.CloseAllMenus(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD // Treat floating button like a
                            // physical button for behavior
                            )
            )
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[MENU_TOGGLE] Opening menu via FloatingButton"
            )
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.OpenMenu(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD // Treat floating button like a
                            // physical button for behavior
                            )
            )
        }
    }
    /** Dismiss the RetroMenu3 */
    fun dismissRetroMenu3(onAnimationEnd: (() -> Unit)? = null) {
        android.util.Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Starting")
        android.util.Log.d(
                "GameActivityViewModel",
                "[DISMISS_MAIN] dismissRetroMenu3: isRetroMenu3Open before dismiss: ${isRetroMenu3Open()}"
        )

        retroMenu3Fragment?.dismissMenuPublic {
            // Ensure NavigationController is synchronized and its state is reset
            navigationController?.closeMenuExternal()
            
            // Explicitly tell MenuManager that we are closed
            menuStateManager.setRetroMenu3Open(false)
            
            onAnimationEnd?.invoke()
        }

        // CRITICAL: Add small delay before clearing keyLog to ensure fragment is fully removed
        // This prevents comboAlreadyTriggered from staying true when menu closes
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: DELAYED - " +
                                            "isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
                            )
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: DELAYED - clearing keyLog now"
                            )

                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: Menu dismissed"
                            )
                        },
                        RETRO_MENU3_FRAGMENT_REMOVAL_SETTLE_DELAY_MS
                ) // Delay to ensure fragment removal is complete

        android.util.Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Completed")
    }

    /**
     * Clears only controller states without closing the fragment. Used when the fragment closes on
     * its own (e.g.: Continue button)
     */
    fun clearControllerInputState() {
        android.util.Log.d(
                "GameActivityViewModel",
                "[CLEAR_STATE] clearControllerInputState: STARTING"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[CLEAR_STATE] clearControllerInputState: comboAlreadyTriggered before: " +
                        "${controllerInput.comboTracker.getComboAlreadyTriggered()}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[CLEAR_STATE] clearControllerInputState: isRetroMenu3Open: ${isRetroMenu3Open()}"
        )

        // Add small delay to ensure fragment is fully destroyed before clearing combo state
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: DELAYED - clearing now"
                            )
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: " +
                                            "isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
                            )
                            inputViewModel.clearControllerInputState()
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: " +
                                            "comboAlreadyTriggered after: " +
                                            "${controllerInput.comboTracker.getComboAlreadyTriggered()}"
                            )
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: COMPLETED"
                            )
                        },
                        CONTROLLER_STATE_CLEAR_FRAGMENT_DESTROY_SETTLE_DELAY_MS
                ) // Delay to ensure fragment destruction is complete
    }

    /** Check if the RetroMenu3 is currently open */
    fun isRetroMenu3Open(): Boolean {
        return retroMenu3Fragment?.isAdded == true
    }

    /** Check if any menu is currently active */
    fun isAnyMenuActive(): Boolean {
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE] 🔍 isAnyMenuActive: ========== CHECKING MENU ACTIVITY =========="
        )

        // PHASE 3: Use NavigationController for menu detection (permanently enabled)
        if (navigationController != null) {
            val navControllerActive = navigationController!!.isMenuActive()
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[ACTIVE] ✅ Using NavigationController: isMenuActive=$navControllerActive"
            )
            return navControllerActive
        }

        // navigationController is set once, at the end of onCreate() (setupMenuCallback()), and
        // never cleared afterward -- every real caller of isAnyMenuActive() already runs after
        // that. Confirmed on-device (menu open/navigate/close/background/foreground) that this
        // branch is never reached; it's a safe default for the narrow window before that.
        return false
    }

    /** Helper method to dismiss submenu fragments with common cleanup logic */
    private fun dismissSubmenuFragment(
            fragment: androidx.fragment.app.Fragment?,
            fragmentName: String,
            activeFlagSetter: () -> Unit
    ) {
        android.util.Log.d("GameActivityViewModel", "dismiss${fragmentName}: Starting")

        // Check if fragment is still valid and added
        if (fragment == null || !fragment.isAdded) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: Fragment is null or not added, skipping dismiss"
            )
            return
        }

        // IMPORTANT: Since submenu fragments were added to the back stack,
        // we must use popBackStack() instead of manual remove()
        // This ensures FragmentManager properly manages the hierarchy

        // Check if there's anything in the back stack before trying to remove
        val activity = fragment.activity
        if (activity != null) {
            val fragmentManager = activity.supportFragmentManager
            val backStackCount = fragmentManager.backStackEntryCount

            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: backStackCount = $backStackCount"
            )

            if (backStackCount > 0) {
                // Use popBackStack to remove the fragment correctly
                android.util.Log.d(
                        "GameActivityViewModel",
                        "dismiss${fragmentName}: Calling popBackStackImmediate()"
                )
                fragmentManager.popBackStackImmediate()
            } else {
                android.util.Log.w(
                        "GameActivityViewModel",
                        "dismiss${fragmentName}: Back stack is empty, nothing to pop"
                )
            }
        }

        // Clear the fragment reference and flag
        activeFlagSetter()

        // CRITICAL FIX: After dismissing submenu, ensure main menu is visible
        // BUT only if we're NOT in the middle of dismissing ALL menus (START button case)
        val retroMenu3OpenBefore = isRetroMenu3Open()
        if (isRetroMenu3Open() && !isDismissingAllMenus()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: Main menu restoration handled by " +
                            "BackStackChangeListener (retroMenu3Open=$retroMenu3OpenBefore)"
            )
            // REMOVED: retroMenu3Fragment?.restoreMainMenu()
            // The BackStackChangeListener in RetroMenu3Fragment will handle menu restoration
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: NOT showing main menu " +
                            "(dismissingAll=${isDismissingAllMenus()}, " +
                            "retroMenu3Open=$retroMenu3OpenBefore)"
            )
        }

        android.util.Log.d("GameActivityViewModel", "dismiss${fragmentName}: Completed")
    }

    /** Check if the Settings submenu is currently open */
    fun isSettingsMenuOpen(): Boolean {
        val isOpen = settingsMenuFragment != null
        if (settingsMenuFragment != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "isSettingsMenuOpen check: fragment=${settingsMenuFragment}, " +
                            "isAdded=${settingsMenuFragment?.isAdded}, result=$isOpen"
            )
        }
        return isOpen
    }

    /** Check if the Progress submenu is currently open */
    fun isProgressMenuOpen(): Boolean {
        return progressFragment != null
    }

    /** Check if the Exit submenu is currently open */
    fun isExitMenuOpen(): Boolean {
        return exitFragment != null
    }

    /** Dismiss the Settings submenu */
    fun dismissSettingsMenu() {
        dismissSubmenuFragment(settingsMenuFragment, "SettingsMenu") {
            settingsMenuFragment = null
            deactivateSettingsMenu()
            // Navigate back to main menu when dismissing Settings submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
        }
    }

    /** Dismiss the Progress submenu */
    fun dismissProgress() {
        dismissSubmenuFragment(progressFragment, "Progress") {
            progressFragment = null
            deactivateProgressMenu()
            // Navigate back to main menu when dismissing Progress submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
        }
    }

    /** Dismiss the Exit submenu */
    fun dismissExit() {
        dismissSubmenuFragment(exitFragment, "Exit") {
            exitFragment = null
            deactivateExitMenu()
            // Navigate back to main menu when dismissing Exit submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
        }
    }

    /** Dismiss the About submenu */
    fun dismissAboutMenu() {
        dismissSubmenuFragment(aboutFragment, "About") {
            aboutFragment = null
            deactivateAboutMenu()
            // Navigate back to main menu when dismissing About submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
        }
    }


    /** Dismiss the Core Variables submenu */
    fun dismissCoreVariablesMenu() {
        dismissSubmenuFragment(coreVariablesFragment, "CoreVariables") {
            coreVariablesFragment = null
            deactivateCoreVariablesMenu()
            // Navigate back to main menu when dismissing CoreVariables submenu
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
        }
    }

    /** Dismiss ALL menus in cascade order (submenus first, then main menu) */
    // REMOVED: dismissAllMenus() - NavigationController handles menu dismissal now

    /**
     * Shared implementation for the 8 `registerXFragment`/`registerXFragmentForRotation`
     * methods below. Each one does some subset of: set the fragment field, notify `menuViewModel`,
     * activate the corresponding menu state, and register with `menuManager` -- always in that
     * order. The exact combination of [notifyMenuViewModel] and [activate] is NOT uniform across
     * fragment types (e.g. Settings' rotation variant passes both as null, while Progress/Exit's
     * still pass a non-null [notifyMenuViewModel]); callers must reproduce their original
     * per-method combination exactly, not "clean it up".
     *
     * The two log lines' wording is driven by whether [activate] is null, matching the original
     * per-method log text: methods that activate a state log "Registering X - isAdded=...,
     * isResumed=..." / "Registration completed - isAnyMenuActive=...", while the `ForRotation`
     * methods (which never activate) log "Registering without state activation" / "Completed
     * (state NOT changed)".
     */
    private data class SubmenuRegistrationMeta(
            val menuState: com.vinaooo.revenger.ui.retromenu3.MenuState,
            val methodLabel: String,
            val emoji: String,
            val fragmentClassName: String
    )

    private fun <F> registerSubmenuFragment(
            fragment: F,
            meta: SubmenuRegistrationMeta,
            setFragmentRef: (F) -> Unit,
            notifyMenuViewModel: (() -> Unit)?,
            activate: (() -> Unit)?
    ) where F : androidx.fragment.app.Fragment, F : com.vinaooo.revenger.ui.retromenu3.MenuFragment {
        if (activate != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registering ${meta.fragmentClassName} - " +
                            "isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
            )
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registering without state activation"
            )
        }
        setFragmentRef(fragment)
        notifyMenuViewModel?.invoke()
        activate?.invoke()
        // Register with MenuManager
        menuManager.registerFragment(meta.menuState, fragment)
        if (activate != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
            )
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Completed (state NOT changed)"
            )
        }
    }

    /** Register the SettingsMenuFragment when it's created */
    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU,
                        "registerSettingsMenuFragment",
                        "⚙️",
                        "SettingsMenuFragment"
                ),
                setFragmentRef = { settingsMenuFragment = it },
                notifyMenuViewModel = { menuViewModel.registerSettingsMenuFragment(fragment) },
                activate = { activateSettingsMenu() }
        )
    }

    /** Unregister SettingsMenuFragment when closing via BACK */
    fun unregisterSettingsMenuFragment() {
        android.util.Log.d(
                "GameActivityViewModel",
                "[UNREGISTER] ⚙️ unregisterSettingsMenuFragment: Clearing SettingsMenuFragment reference"
        )
        settingsMenuFragment = null
        deactivateSettingsMenu()
        // Unregister from MenuManager
        menuManager.unregisterFragment(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)
        android.util.Log.d(
                "GameActivityViewModel",
                "[UNREGISTER] ⚙️ unregisterSettingsMenuFragment: Unregistration completed"
        )
    }

    /** Register SettingsMenuFragment for rotation recreation (without activating state) */
    fun registerSettingsMenuFragmentForRotation(fragment: SettingsMenuFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU,
                        "registerSettingsMenuFragmentForRotation",
                        "⚙️",
                        "SettingsMenuFragment"
                ),
                setFragmentRef = { settingsMenuFragment = it },
                notifyMenuViewModel = null,
                activate = null
        )
    }

    /** Register the ProgressFragment when it's created */
    fun registerProgressFragment(fragment: ProgressFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU,
                        "registerProgressFragment",
                        "💾",
                        "ProgressFragment"
                ),
                setFragmentRef = { progressFragment = it },
                notifyMenuViewModel = { menuViewModel.registerProgressFragment(fragment) },
                activate = { activateProgressMenu() }
        )
    }

    /** Register ProgressFragment for rotation recreation (without activating state) */
    fun registerProgressFragmentForRotation(fragment: ProgressFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU,
                        "registerProgressFragmentForRotation",
                        "💾",
                        "ProgressFragment"
                ),
                setFragmentRef = { progressFragment = it },
                notifyMenuViewModel = { menuViewModel.registerProgressFragment(fragment) },
                activate = null
        )
    }

    /** Register the ExitFragment when it's created */
    fun registerExitFragment(fragment: ExitFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU,
                        "registerExitFragment",
                        "🚪",
                        "ExitFragment"
                ),
                setFragmentRef = { exitFragment = it },
                notifyMenuViewModel = { menuViewModel.registerExitFragment(fragment) },
                activate = { activateExitMenu() }
        )
    }

    /** Register ExitFragment for rotation recreation (without activating state) */
    fun registerExitFragmentForRotation(fragment: ExitFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU,
                        "registerExitFragmentForRotation",
                        "🚪",
                        "ExitFragment"
                ),
                setFragmentRef = { exitFragment = it },
                notifyMenuViewModel = { menuViewModel.registerExitFragment(fragment) },
                activate = null
        )
    }

    /** Register the AboutFragment when it's created */
    fun registerAboutFragment(fragment: AboutFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU,
                        "registerAboutFragment",
                        "📋",
                        "AboutFragment"
                ),
                setFragmentRef = { aboutFragment = it },
                notifyMenuViewModel = null,
                activate = { activateAboutMenu() }
        )
    }

    /** Register AboutFragment for rotation recreation (without activating state) */
    fun registerAboutFragmentForRotation(fragment: AboutFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU,
                        "registerAboutFragmentForRotation",
                        "📋",
                        "AboutFragment"
                ),
                setFragmentRef = { aboutFragment = it },
                notifyMenuViewModel = null,
                activate = null
        )
    }

    // Implementation of GameMenuBottomSheet.GameMenuListener interface
    // REMOVED: RetroMenu3Listener implementation - migrated to unified MenuAction/MenuEvent system
    // Implementation of SettingsMenuFragment.SettingsMenuListener interface
    override fun onBackToMainMenu() {
        android.util.Log.d("GameActivityViewModel", "onBackToMainMenu: User wants to go back")
        android.util.Log.d(
                "GameActivityViewModel",
                "onBackToMainMenu: retroMenu3Fragment = $retroMenu3Fragment"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "onBackToMainMenu: retroMenu3Fragment.isAdded = ${retroMenu3Fragment?.isAdded}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "onBackToMainMenu: settingsMenuFragment = $settingsMenuFragment"
        )

        // Simply close the submenu using popBackStack
        // The OnBackStackChangedListener in RetroMenu3Fragment will handle showing the main menu
        dismissSettingsMenu()

        android.util.Log.d(
                "GameActivityViewModel",
                "onBackToMainMenu: dismissSettingsMenu() called"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "onBackToMainMenu: Back stack listener will handle showing main menu"
        )
    }

    override fun onAboutBackToMainMenu() {
        android.util.Log.d(
                "GameActivityViewModel",
                "onAboutBackToMainMenu: User wants to go back from About menu"
        )
        dismissAboutMenu()
    }

    /**
     * Centralized load state implementation with improved debugging FIX: Temporarily unpause ONLY
     * during load, without sending signals to core
     */
    fun loadStateCentralized(onComplete: (() -> Unit)? = null) {
        if (saveLoadOrchestrator.loadState(retroView, retroViewUtils, onComplete)) {
            skipNextTempStateLoad = true
        }
    }

    /**
     * Centralized save state implementation with improved debugging FIX: Removed unnecessary delay
     * that could cause timing issues
     */
    fun saveStateCentralized(onComplete: (() -> Unit)? = null, keepPaused: Boolean = false) {
        saveLoadOrchestrator.saveState(retroView, retroViewUtils, keepPaused, onComplete)
    }

    /**
     * Centralized reset game implementation with improved debugging FIX: Ensure that reset really
     * restarts the game from the beginning
     */
    fun resetGameCentralized(onComplete: (() -> Unit)? = null) {
        saveLoadOrchestrator.resetGame(retroView, onComplete)
    }

    /** Check if save state exists for UI state management */
    fun hasSaveState(): Boolean {
        return retroViewUtils?.hasSaveState() ?: false
    }

    /** Get current audio state for UI management */
    fun getAudioState(): Boolean {
        return audioViewModel.getAudioState()
    }

    /** Get current fast forward state for UI management */
    fun getFastForwardState(): Boolean {
        return speedViewModel.getFastForwardState()
    }

    /** Toggle shader for visual effects */
    fun onToggleShader(): String {
        return shaderViewModel.toggleShader()
    }

    /** Get current shader state for UI management */
    fun getShaderState(): String {
        return shaderViewModel.getShaderState()
    }
    
    fun getShaderDisplayName(): String {
        return shaderViewModel.getCurrentShaderDisplayName()
    }

    // ========== SCREENSHOT CAPTURE FOR SAVE STATES ==========

    /** Property to skip next screenshot. Used by PiP. */
    var suppressNextScreenshotCapture: Boolean = false

    /**
     * Capture screenshot when menu opens. Called from showRetroMenu3() before pausing the game.
     *
     * @param onCaptured Optional callback when capture completes
     */
    fun captureScreenshotForSaveState(onCaptured: ((Boolean) -> Unit)? = null) {
        if (suppressNextScreenshotCapture) {
            suppressNextScreenshotCapture = false
            onCaptured?.invoke(true)
            return
        }

        retroView?.view?.let { glRetroView ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.vinaooo.revenger.utils.ScreenshotCaptureUtil.captureAndCacheScreenshot(
                        glRetroView,
                        onCaptured
                )
                // Menu-open is a clean pause point with a valid surface — refresh the PiP still too.
                com.vinaooo.revenger.utils.ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = true)
            } else {
                onCaptured?.invoke(false)
            }
        }
                ?: onCaptured?.invoke(false)
    }

    /** Get cached screenshot for save operation. Returns null if no screenshot was captured. */
    fun getCachedScreenshot(): android.graphics.Bitmap? {
        return com.vinaooo.revenger.utils.ScreenshotCaptureUtil.getCachedScreenshot()
    }

    /**
     * Clear cached screenshot when menu closes without saving. Frees memory used by the cached
     * bitmap.
     */
    fun clearCachedScreenshot() {
        com.vinaooo.revenger.utils.ScreenshotCaptureUtil.clearCachedScreenshot()
    }

    /** Hide the system bars */
    fun immersive(window: Window) {
        /* Check if the config permits it */
        if (!appConfig.getFullscreen()) return

        with(window.insetsController!!) {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** Hook the RetroView with the GLRetroView instance */
    fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
        retroView = RetroView(activity, viewModelScope, appConfig)
        retroViewUtils = RetroViewUtils(activity)

        // Initialize controllers with the same SharedPreferences that RetroViewUtils uses
        initializeControllers(activity)

        retroView?.let { retroView ->
            container.addView(retroView.view)
            activity.lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()
            retroView.registerFrameCallback()

            /* FIX: DO NOT restore state automatically on first frame
             * The game should start from zero and the user decides when to load the save
             * This fixes the bug where Load State didn't work after restart because
             * the save had already been loaded automatically during initialization
             */
            retroView.frameRendered.observe(activity) {
                if (it != true) return@observe

                // IMPORTANT: Initialize speed AND audio, WITHOUT loading save state
                // Save state should only be loaded when user clicks "Load State"
                speedController?.initializeSpeedState(retroView.view)
                audioController?.initializeAudioState(retroView.view)

                // Connect ShaderController to RetroView for real-time shader switching
                shaderController?.connect(retroView)
            }
        }
    }

    /**
     * Routes a virtual GamePad event to [controllerInput], the same way for both the left and
     * right pad (their callbacks used to carry two verbatim copies of this logic).
     */
    private fun handleGamePadEvent(event: Event): Boolean =
            when (event) {
                is Event.Button -> controllerInput.processGamePadButtonEvent(event.id, event.action)
                is Event.Direction -> handleGamePadDirectionEvent(event)
                else -> false // Other event types are not intercepted
            }

    /**
     * While a menu is open, DPAD/analog direction events are converted into a synthetic
     * [MotionEvent] and routed through [ControllerInput]'s menu-navigation path instead of being
     * dispatched natively by the GamePad.
     */
    private fun handleGamePadDirectionEvent(event: Event.Direction): Boolean {
        if (!isAnyMenuActive()) {
            // Menu is closed. Do not intercept. Let GamePad natively dispatch its axes directly.
            return false
        }
        // Process motion and return true to intercept the directional event while the menu is open
        controllerInput.processMotionEvent(buildDpadMotionEvent(event), retroView!!)
        return true
    }

    /** Create a synthetic MotionEvent for DPAD/analog direction, using PointerCoords. */
    private fun buildDpadMotionEvent(event: Event.Direction): MotionEvent {
        val pointerCoords = MotionEvent.PointerCoords()
        pointerCoords.x = 0f
        pointerCoords.y = 0f
        pointerCoords.pressure = 1f
        pointerCoords.size = 1f
        pointerCoords.setAxisValue(MotionEvent.AXIS_HAT_X, event.xAxis)
        pointerCoords.setAxisValue(MotionEvent.AXIS_HAT_Y, event.yAxis)

        val pointerProperties = MotionEvent.PointerProperties()
        pointerProperties.id = 0
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER

        return MotionEvent.obtain(
                android.os.SystemClock.uptimeMillis(),
                android.os.SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                1,
                arrayOf(pointerProperties),
                arrayOf(pointerCoords),
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_JOYSTICK,
                0
        )
    }

    /** Subscribe the GamePads to the RetroView */
    fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val context = getApplication<Application>().applicationContext

        val gamePadConfig = GamePadConfig(context, appConfig)
        leftGamePad = GamePad(context, gamePadConfig.left) { event: Event -> handleGamePadEvent(event) }
        rightGamePad =
                GamePad(context, gamePadConfig.right) { event: Event -> handleGamePadEvent(event) }

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }
    }

    /** Hide the on-screen GamePads and toggle Floating Menu Button if applicable */
    fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            floatingButton: android.view.View? = null
    ) {
        val shouldShow = com.vinaooo.revenger.gamepad.GamePad.shouldShowGamePads(activity, appConfig)
        val visibility = if (shouldShow) android.view.View.VISIBLE else android.view.View.GONE

        gamePadContainerView?.visibility = visibility
        leftContainer.visibility = visibility
        rightContainer.visibility = visibility

        floatingButton?.let {
            val configValue = appConfig.getMenuModeFab().lowercase()
            if (configValue != "disabled" && !shouldShow) {
                it.visibility = android.view.View.VISIBLE
            } else {
                it.visibility = android.view.View.GONE
            }
        }
    }

    /** Process a key event and return the result */
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        // DEBUG: Log ALL key events to diagnose Backspace issue
        android.util.Log.d(
                "GameActivityViewModel",
                "[KEY-EVENT] keyCode=$keyCode, action=${event.action}, navigationSystemActive=true"
        )

        if (tryConsumeKeyboardNavigation(keyCode, event) == true) {
            return true // Event was consumed by menu navigation
        }

        // Process normally via ControllerInput (for game inputs)
        val retroView = retroView
        return if (retroView != null) {
            controllerInput.processKeyEvent(keyCode, event, retroView)
        } else {
            false
        }
    }

    /**
     * PHASE 4.1c: routes [keyCode]/[event] to [keyboardInputAdapter] when it's a navigation key
     * the keyboard path should currently handle, returning whether it consumed the event.
     * Returns `null` when the keyboard path doesn't apply at all (no adapter, not a navigation
     * key, or the menu-active/F12 gate says not to route it there) and `false` when it applied
     * but the adapter didn't consume the event; [processKeyEvent] treats both the same way
     * (falls through to `ControllerInput`), so the distinction only matters to callers that care
     * why. Extracted out of [processKeyEvent] to keep that function within detekt's
     * `NestedBlockDepth`/`ReturnCount` thresholds.
     */
    private fun tryConsumeKeyboardNavigation(keyCode: Int, event: KeyEvent): Boolean? {
        val adapter = keyboardInputAdapter ?: return null
        if (!adapter.isNavigationKey(keyCode)) return null

        // PHASE 4.2c: Allow F12 even when menu is closed (to open menu)
        // But Backspace (DEL) only works when menu is OPEN (to navigate back)
        val isMenuActive = isAnyMenuActive()
        val shouldProcessKeyboard = isMenuActive || keyCode == KeyEvent.KEYCODE_F12

        android.util.Log.d(
                "GameActivityViewModel",
                "[PHASE4] Navigation key check: keyCode=$keyCode, " +
                        "action=${event.action}, isMenuActive=$isMenuActive, " +
                        "shouldProcess=$shouldProcessKeyboard"
        )

        if (!shouldProcessKeyboard) return null

        android.util.Log.d(
                "GameActivityViewModel",
                "[PHASE4] Routing key event to KeyboardInputAdapter: " +
                        "keyCode=$keyCode, action=${event.action}"
        )
        // Route to keyboard adapter based on action type
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> adapter.onKeyDown(keyCode, event)
            KeyEvent.ACTION_UP -> adapter.onKeyUp(keyCode, event)
            else -> false
        }
    }

    /** Process a motion event and return the result */
    fun processMotionEvent(event: MotionEvent): Boolean? {
        // Process normally via ControllerInput
        retroView?.let {
            return controllerInput.processMotionEvent(event, it)
        }

        return false
    }

    /** Deallocate the old RetroView */
    fun detachRetroView(activity: ComponentActivity) {
        retroView?.let { activity.lifecycle.removeObserver(it.view) }
        retroView = null
    }

    /** Set the screen orientation based on the config */
    fun setConfigOrientation(activity: Activity) {
        val configOrientation = appConfig.getOrientation()
        com.vinaooo.revenger.utils.OrientationManager.applyConfigOrientation(
                activity,
                configOrientation
        )
    }

    /** Dispose the composite disposable; call on onDestroy */
    fun dispose() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    /** Save the state of the emulator */
    fun preserveState() {
        if (retroView?.frameRendered?.value == true)
                retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
    }

    /** Check if menu should respond to back button based on menu_mode_back */
    fun shouldHandleBackButton(): Boolean {
        return appConfig.getMenuModeBack()
    }

    /** Check if menu should respond to SELECT+START combo based on menu_mode_combo */
    fun shouldHandleSelectStartCombo(): Boolean {
        return appConfig.getMenuModeCombo()
    }

    /** Check if menu should respond to gamepad menu button based on menu_mode_gamepad */
    fun shouldHandleGamepadMenuButton(): Boolean {
        return appConfig.getMenuModeGamepad()
    }

    /**
     * Inicializa os controllers modulares com as mesmas SharedPreferences do RetroViewUtils Garante
     * compatibilidade com o sistema existente
     */
    private fun initializeControllers(activity: Activity) {
        val sharedPrefs = activity.getPreferences(android.content.Context.MODE_PRIVATE)
        sharedPreferences = sharedPrefs
        audioController = AudioController(activity.applicationContext, sharedPrefs)
        speedController = SpeedController(activity.applicationContext, sharedPrefs, appConfig)
        shaderController = ShaderController(sharedPrefs, appConfig)

        // Set controllers in ViewModels
        audioController?.let { audioViewModel.setAudioController(it) }
        speedController?.let { speedViewModel.setSpeedController(it) }
        shaderController?.let { shaderViewModel.setShaderController(it) }
    }

    // PUBLIC METHODS FOR ACCESS TO MODULAR CONTROLLERS

    /**
     * Gets reference to AudioController for use in other components. Allows modular access to audio
     * functionalities
     */
    fun getAudioController(): AudioController? {
        return audioController
    }

    /**
     * Gets reference to SpeedController for use in other components. Allows modular access to speed
     * functionalities
     */
    fun getSpeedController(): SpeedController? {
        return speedController
    }

    /**
     * Gets reference to ShaderController for use in other components. Allows modular access to
     * shader functionalities
     */
    fun getShaderController(): ShaderController? {
        return shaderController
    }

    /**
     * Audio control using modular controller
     * @param enabled true to turn on, false to turn off
     */
    fun setAudioEnabled(enabled: Boolean) {
        audioViewModel.setAudioEnabled(retroView?.view, enabled)
    }

    /**
     * Controle de velocidade usando controller modular
     * @param speed velocidade desejada (1 = normal, > 1 = fast forward)
     */
    fun setGameSpeed(speed: Int) {
        retroView?.let { speedController?.setSpeed(it.view, speed) }
    }

    /** Ativa fast forward usando controller modular */
    fun enableFastForward() {
        speedViewModel.enableFastForward(retroView?.view)
    }

    /** Clear controller key log (used by RetroMenu3Fragment on destroy) */
    fun clearControllerKeyLog() {
        controllerInput.comboTracker.clearKeyLog()
    }

    /** Check if we are currently dismissing all menus (used by RetroMenu3Fragment) */
    fun isDismissingAllMenus(): Boolean {
        return menuStateManager.isDismissingAllMenus()
    }

    /** Update the current menu state in MenuManager */
    fun updateMenuState(newState: com.vinaooo.revenger.ui.retromenu3.MenuState) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE] 🔄 updateMenuState: Changing to $newState"
        )
        menuManager.navigateToState(newState)
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE] ✅ updateMenuState: State changed to $newState"
        )
    }

    fun unregisterFragment(state: com.vinaooo.revenger.ui.retromenu3.MenuState) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[FRAGMENT] unregisterFragment: Unregistering fragment for state $state"
        )
        menuManager.unregisterFragment(state)
        android.util.Log.d(
                "GameActivityViewModel",
                "[FRAGMENT] unregisterFragment: Fragment unregistered"
        )
    }

    fun getCurrentMenuState(): com.vinaooo.revenger.ui.retromenu3.MenuState {
        return menuManager.getCurrentState()
    }

    fun getCurrentFragment(): com.vinaooo.revenger.ui.retromenu3.MenuFragment? {
        return menuManager.getCurrentFragment()
    }

    // ===== MenuManagerListener Implementation =====

    /** Handles the "back" action's per-state dismissal, extracted out of [handleMenuAction]. */
    private fun dismissCurrentMenuState() {
        when (menuManager.getCurrentState()) {
            com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU -> dismissRetroMenu3()
            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU -> dismissSettingsMenu()
            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU -> dismissProgress()
            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU -> dismissAboutMenu()
            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU -> dismissExit()
            else -> { }
        }
    }

    /** Handles [com.vinaooo.revenger.ui.retromenu3.MenuEvent.Action], extracted from [onMenuEvent]. */
    private fun handleMenuAction(action: com.vinaooo.revenger.ui.retromenu3.MenuAction) {
        when (action) {
            com.vinaooo.revenger.ui.retromenu3.MenuAction.SAVE_STATE -> saveStateCentralized()
            com.vinaooo.revenger.ui.retromenu3.MenuAction.LOAD_STATE -> loadStateCentralized()
            com.vinaooo.revenger.ui.retromenu3.MenuAction.RESET -> resetGameCentralized()
            com.vinaooo.revenger.ui.retromenu3.MenuAction.TOGGLE_AUDIO -> {
                retroView?.let { audioViewModel.toggleAudio(it.view) }
            }
            com.vinaooo.revenger.ui.retromenu3.MenuAction.TOGGLE_SPEED -> {
                retroView?.let { speedController?.toggleFastForward(it.view) }
            }
            com.vinaooo.revenger.ui.retromenu3.MenuAction.TOGGLE_SHADER -> {
                shaderViewModel.toggleShader()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuAction.SAVE_AND_EXIT -> {
                // Save and exit - same logic as in ExitFragment
                saveStateCentralized(
                        onComplete = { android.os.Process.killProcess(android.os.Process.myPid()) }
                )
            }
            com.vinaooo.revenger.ui.retromenu3.MenuAction.EXIT -> {
                // Exit without save
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            com.vinaooo.revenger.ui.retromenu3.MenuAction.BACK -> dismissCurrentMenuState()
            is com.vinaooo.revenger.ui.retromenu3.MenuAction.NAVIGATE -> {
                // Navigate to different menu state
                menuManager.navigateToState(action.targetMenu)
            }
            else -> {
                // Ignore other actions
            }
        }
    }

    /**
     * Handles [com.vinaooo.revenger.ui.retromenu3.MenuEvent.StateChanged], extracted from
     * [onMenuEvent].
     */
    private fun handleMenuStateChanged(
            event: com.vinaooo.revenger.ui.retromenu3.MenuEvent.StateChanged
    ) {
        // Handle menu state transitions
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 ========== MENU STATE CHANGED =========="
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 From: ${event.from} -> To: ${event.to}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 isAnyMenuActive before=${isAnyMenuActive()}"
        )

        // Activate/deactivate menus based on state changes
        when (event.to) {
            com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU -> {
                android.util.Log.d(
                        "GameActivityViewModel",
                        "[STATE_CHANGE] 🎮 State changed to MAIN_MENU - retroMenu3Open=${isRetroMenu3Open()}"
                )
                // Main menu is always active when RetroMenu3 is open
                // No need to activate/deactivate here
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU -> {
                activateSettingsMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU -> {
                activateProgressMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU -> {
                activateAboutMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU -> {
                activateExitMenu()
            }
            else -> {}
        }

        // Deactivate previous menu if it was a submenu
        when (event.from) {
            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU -> {
                deactivateSettingsMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU -> {
                deactivateProgressMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU -> {
                deactivateAboutMenu()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU -> {
                deactivateExitMenu()
            }
            else -> {
                // No deactivation needed for MAIN_MENU or other states
            }
        }

        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 isAnyMenuActive after=${isAnyMenuActive()}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[STATE_CHANGE] 🔄 ========== MENU STATE CHANGED END =========="
        )
    }

    override fun onMenuEvent(event: com.vinaooo.revenger.ui.retromenu3.MenuEvent) {
        when (event) {
            is com.vinaooo.revenger.ui.retromenu3.MenuEvent.Action -> handleMenuAction(event.action)
            is com.vinaooo.revenger.ui.retromenu3.MenuEvent.StateChanged ->
                    handleMenuStateChanged(event)
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.MenuClosed -> {
                // Handle complete menu closure - delegate to NavigationController
                navigationController?.handleNavigationEvent(
                        com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.CloseAllMenus(
                                inputSource =
                                        com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                                .PHYSICAL_GAMEPAD
                        )
                )
            }
            // Navigation events are handled by the fragments themselves, not by the ViewModel
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.NavigateUp -> {
                menuManager.navigateUp()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.NavigateDown -> {
                menuManager.navigateDown()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.Confirm -> {
                menuManager.confirm()
            }
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.Back -> {
                menuManager.back()
            }
        }
    }

    /** Desativa fast forward usando controller modular */
    fun disableFastForward() {
        speedViewModel.disableFastForward(retroView?.view)
    }

    /** Define fast forward enabled/disabled sem aplicar imediatamente (usado pelo menu Settings) */
    fun setFastForwardEnabled(enabled: Boolean) {
        if (enabled) {
            speedViewModel.enableFastForward(null) // Pass null to avoid immediate application
            // Also save the speed value to preferences for menu closure restoration
            sharedPreferences?.edit()?.putInt(PreferencesConstants.PREF_FRAME_SPEED, 2)?.apply()
        } else {
            speedViewModel.disableFastForward(null) // Pass null to avoid immediate application
            // Also save the speed value to preferences for menu closure restoration
            sharedPreferences?.edit()?.putInt(PreferencesConstants.PREF_FRAME_SPEED, 1)?.apply()
        }
    }

    /**
     * Cleanup method called when ViewModel is being destroyed. Prevents memory leaks by clearing
     * references and disposing resources.
     */
    override fun onCleared() {
        android.util.Log.d("GameActivityViewModel", "onCleared: Starting cleanup")

        // Dispose RxJava subscriptions to prevent memory leaks
        compositeDisposable.dispose()

        // Cancel a save-state restore that might still be pending (see saveState()'s 200ms
        // delayed restore) so it doesn't fire later against a torn-down RetroView.
        saveLoadOrchestrator.cancelPendingSave()

        // Clear fragment references to prevent memory leaks
        retroMenu3Fragment = null
        settingsMenuFragment = null
        progressFragment = null
        aboutFragment = null
        exitFragment = null

        // Clear container references
        menuContainerView = null
        gamePadContainerView = null

        // Clear other references
        retroView = null
        retroViewUtils = null
        leftGamePad = null
        rightGamePad = null

        // Clear controllers
        audioController = null
        speedController = null
        shaderController = null
        sharedPreferences = null

        android.util.Log.d("GameActivityViewModel", "onCleared: Cleanup completed")
        super.onCleared()
    }

    /** Called when RetroMenu3Fragment is destroyed to clean up the reference */
    fun onRetroMenu3FragmentDestroyed() {
        android.util.Log.d(
                "GameActivityViewModel",
                "[FRAGMENT_DESTROYED] onRetroMenu3FragmentDestroyed: Clearing retroMenu3Fragment reference"
        )
        retroMenu3Fragment = null
    }
}
