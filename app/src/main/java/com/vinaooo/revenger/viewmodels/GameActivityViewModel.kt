package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.R
import com.vinaooo.revenger.controllers.AudioController
import com.vinaooo.revenger.controllers.ShaderController
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.*
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application),
        SettingsMenuListener,
        AboutListener,
        MenuManager.MenuManagerListener {

    private val resources = application.resources

    // ===== SPECIALIZED VIEWMODELS =====
    // Using composition pattern to separate concerns

    /** Menu management ViewModel */
    private val menuViewModel: MenuViewModel = MenuViewModel(application)

    /** Game state management ViewModel */
    private val gameStateViewModel: GameStateViewModel = GameStateViewModel(application)

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

    /** Check if settings menu is active */
    private fun isSettingsMenuActive(): Boolean =
            menuStateManager.isMenuActive(
                    com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.SETTINGS_MENU
            )

    /** Check if progress menu is active */
    private fun isProgressActive(): Boolean =
            menuStateManager.isMenuActive(
                    com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.PROGRESS_MENU
            )

    /** Check if exit menu is active */
    private fun isExitActive(): Boolean =
            menuStateManager.isMenuActive(
                    com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.EXIT_MENU
            )

    /** Check if about menu is active */
    private fun isAboutActive(): Boolean =
            menuStateManager.isMenuActive(
                    com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.ABOUT_MENU
            )

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
    private val controllerInput = ControllerInput(application.applicationContext)

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
        // PHASE 3.1a: Initialize NavigationController (permanently enabled after Phase 4
        // validation)
        if (navigationController == null) {
            navigationController = NavigationController(activity)

            // PHASE 4.1c: Initialize KeyboardInputAdapter
            keyboardInputAdapter =
                    com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter(
                            navigationController!!,
                            { isAnyMenuActive() }
                    )

            // PHASE 3.2b: Configurar callbacks para pausar/resumir o jogo
            navigationController?.onMenuOpenedCallback = {
                try {
                    Log.d(
                            "GameActivityViewModel",
                            "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} thread=${Thread.currentThread().name} - menu opened callback start"
                    )
                    Log.d(
                            "GameActivityViewModel",
                            "[ON_MENU_OPENED] Fragment in container=${activity.supportFragmentManager.findFragmentById(R.id.menu_container)?.javaClass?.simpleName} backStack=${activity.supportFragmentManager.backStackEntryCount}"
                    )
                } catch (t: Throwable) {
                    Log.w(
                            "GameActivityViewModel",
                            "[ON_MENU_OPENED] failed to log fragment manager state",
                            t
                    )
                }

                // Capturar screenshot ANTES de pausar para save states
                captureScreenshotForSaveState()
                // Preservar estado do emulador
                retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
                // PAUSAR o jogo quando menu abre
                retroView?.let { speedController?.pause(it.view) }

                try {
                    Log.d(
                            "GameActivityViewModel",
                            "[ON_MENU_OPENED] ts=${System.currentTimeMillis()} - menu opened callback completed"
                    )
                } catch (t: Throwable) {
                    Log.w("GameActivityViewModel", "[ON_MENU_OPENED] failed to log completion", t)
                }
            }

            navigationController?.onMenuClosedCallback = { closingButton: Int? ->
                android.util.Log.d(
                        "GameActivityViewModel",
                        "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED ====="
                )
                try {
                    Log.d(
                            "GameActivityViewModel",
                            "🔥 [ON_MENU_CLOSED_CALLBACK] ts=${System.currentTimeMillis()} thread=${Thread.currentThread().name} closingButton=$closingButton"
                    )
                    Log.d(
                            "GameActivityViewModel",
                            "🔥 [ON_MENU_CLOSED_CALLBACK] Fragment in container=${activity.supportFragmentManager.findFragmentById(R.id.menu_container)?.javaClass?.simpleName} backStack=${activity.supportFragmentManager.backStackEntryCount}"
                    )
                } catch (t: Throwable) {
                    Log.w(
                            "GameActivityViewModel",
                            "🔥 [ON_MENU_CLOSED_CALLBACK] failed to log fragment manager state",
                            t
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
                controllerInput.clearMenuActionButtons()

                // Reset combo state to allow SELECT+START to work again after menu closes
                controllerInput.resetComboAlreadyTriggered()

                // Clear keyLog immediately to prevent residual button states from causing combo
                // detection issues
                controllerInput.clearKeyLog()

                // Update menu close debounce time to prevent immediate combo detection
                controllerInput.updateMenuCloseDebounceTime()

                android.util.Log.d(
                        "GameActivityViewModel",
                        "🔥 [ON_MENU_CLOSED_CALLBACK] comboAlreadyTriggered reset, keyLog cleared, debounce updated"
                )

                // Grace period: keep interception active for 200ms after menu closes
                // 200ms covers the ~150ms hardware delay between ACTION_DOWN and ACTION_UP
                // Identified via logs: UP arrives 150ms later; 50ms was insufficient
                // Block only the button that actually closed the menu
                controllerInput.keepInterceptingButtons(200, closingButton = closingButton)

                // Limpar screenshot cacheado quando menu fecha
                clearCachedScreenshot()

                // RESUMIR o jogo quando menu fecha - aplicar velocidade salva nas preferences
                retroView?.let { speedController?.restoreSpeedFromPreferences(it.view) }

                // Hide load preview overlay AFTER game resumes with delay.
                // The GL surface needs time to render the loaded state frame;
                // hiding immediately causes a brief flash of the old game frame.
                Handler(Looper.getMainLooper()).postDelayed({ hideLoadPreview() }, 300)

                android.util.Log.d(
                        "GameActivityViewModel",
                        "🔥 [ON_MENU_CLOSED_CALLBACK] ===== MENU CLOSED COMPLETED ====="
                )
            }
        }

        // REMOVED: Legacy callback configurations - NavigationController handles all navigation now

        // Configure gamepad menu button callback to toggle menu
        controllerInput.gamepadMenuButtonCallback = {
            if (isAnyMenuActive()) {
                // PHASE 3.4a: Use CloseAllMenus to exit directly to game (permanently enabled)
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
                // PHASE 3: Use NavigationController to open menu (permanently enabled)
                navigationController?.handleNavigationEvent(
                        com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.OpenMenu(
                                inputSource =
                                        com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                                .PHYSICAL_GAMEPAD
                        )
                )
            }
        }

        // PHASE 3.1b: Configure navigation callbacks (permanently enabled)
        controllerInput.menuNavigateUpCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.Navigate(
                            direction = com.vinaooo.revenger.ui.retromenu3.navigation.Direction.UP,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        controllerInput.menuNavigateDownCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.Navigate(
                            direction =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.Direction.DOWN,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        controllerInput.menuNavigateLeftCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.Navigate(
                            direction =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.Direction.LEFT,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        controllerInput.menuNavigateRightCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.Navigate(
                            direction =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.Direction.RIGHT,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        controllerInput.menuConfirmCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.ActivateSelected(
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        controllerInput.menuBackCallback = {
            navigationController?.handleNavigationEvent(
                    com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.NavigateBack(
                            keyCode = KeyEvent.KEYCODE_BUTTON_B,
                            inputSource =
                                    com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
                                            .PHYSICAL_GAMEPAD
                    )
            )
        }

        // Control when to intercept DPAD for menu
        // CRITICAL: DO NOT check isDismissingAllMenus() here!
        // We need to keep intercepting buttons even during closing
        // to prevent ACTION_UP from leaking into the game
        controllerInput.shouldInterceptDpadForMenu = {
            val result = isAnyMenuActive() // Removido: && !isDismissingAllMenus()
            result
        }

        // Control when START button alone should work (only when RetroMenu3 or SettingsMenu
        // is REALLY open)
        controllerInput.shouldHandleStartButton = { isAnyMenuActive() && !isDismissingAllMenus() }

        // Control when to block ALL gamepad inputs (when RetroMenu3 or SettingsMenu
        // is open)
        controllerInput.shouldBlockAllGamepadInput = { isAnyMenuActive() }

        // Control if RetroMenu3 or SettingsMenu is open for combo reset
        controllerInput.isRetroMenu3Open = { isAnyMenuActive() }

        // Control if it's safe to execute menu callbacks (no critical operations in progress)
        controllerInput.isMenuOperationSafe = {
            val dismissingAll = isDismissingAllMenus()
            val fragmentDismissing = retroMenu3Fragment?.isDismissingMenu() == true
            val result = !dismissingAll && !fragmentDismissing

            result
        }
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

        retroMenu3Fragment?.dismissMenuPublic(onAnimationEnd)

        // CRITICAL: Add small delay before clearing keyLog to ensure fragment is fully removed
        // This prevents comboAlreadyTriggered from staying true when menu closes
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[DISMISS_MAIN] dismissRetroMenu3: DELAYED - isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
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
                        200
                ) // 200ms delay to ensure fragment removal is complete

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
                "[CLEAR_STATE] clearControllerInputState: comboAlreadyTriggered before: ${controllerInput.getComboAlreadyTriggered()}"
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
                                    "[CLEAR_STATE] clearControllerInputState: isRetroMenu3Open after delay: ${isRetroMenu3Open()}"
                            )
                            inputViewModel.clearControllerInputState()
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: comboAlreadyTriggered after: ${controllerInput.getComboAlreadyTriggered()}"
                            )
                            android.util.Log.d(
                                    "GameActivityViewModel",
                                    "[CLEAR_STATE] clearControllerInputState: COMPLETED"
                            )
                        },
                        200
                ) // 200ms delay to ensure fragment destruction is complete
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

        val retroMenu3Open = isRetroMenu3Open()

        // CRITICAL FIX: Remove isResumed requirement - isAdded is enough
        // This eliminates the race condition where Fragment is visible but not yet resumed
        val aboutFragmentActive = aboutFragment != null && aboutFragment?.isAdded == true
        val settingsFragmentActive =
                settingsMenuFragment != null && settingsMenuFragment?.isAdded == true
        val progressFragmentActive = progressFragment != null && progressFragment?.isAdded == true
        val exitFragmentActive = exitFragment != null && exitFragment?.isAdded == true

        // CRITICAL: If we're in the middle of dismissing submenus but the main menu should still be
        // active,
        // ensure we don't lose gamepad blocking. Check if retroMenu3Fragment exists and was
        // recently active
        val dismissingSubmenu = isDismissingAllMenus()
        val retroMenu3FragmentExists = retroMenu3Fragment != null
        val retroMenu3FragmentAdded = retroMenu3Fragment?.isAdded == true

        // If we're dismissing a submenu but the main menu fragment still exists, keep blocking
        val forceMainMenuActive =
                dismissingSubmenu && retroMenu3FragmentExists && retroMenu3FragmentAdded

        // CRITICAL FIX: If RetroMenu3 is not added but exists (replaced by submenu),
        // and there's an active submenu, the menu system should still be considered active
        val hasActiveSubmenu =
                settingsFragmentActive ||
                        progressFragmentActive ||
                        aboutFragmentActive ||
                        exitFragmentActive
        val menuSystemActive = retroMenu3Open || (retroMenu3FragmentExists && hasActiveSubmenu)

        // SIMPLIFIED: Menu is active if any of these is true
        val result =
                retroMenu3Open ||
                        menuSystemActive ||
                        settingsFragmentActive ||
                        progressFragmentActive ||
                        aboutFragmentActive ||
                        exitFragmentActive ||
                        forceMainMenuActive

        android.util.Log.d("GameActivityViewModel", "[ACTIVE] 📊 Menu states:")
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🎮 retroMenu3Open=$retroMenu3Open (isAdded=${retroMenu3Fragment?.isAdded})"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🔧 retroMenu3FragmentExists=$retroMenu3FragmentExists"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   📱 hasActiveSubmenu=$hasActiveSubmenu"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🎯 menuSystemActive=$menuSystemActive"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🎯 menuSystemActive=$menuSystemActive"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   📋 aboutFragmentActive=$aboutFragmentActive (ref=${aboutFragment != null}, added=${aboutFragment?.isAdded}, resumed=${aboutFragment?.isResumed})"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   ⚙️ settingsFragmentActive=$settingsFragmentActive (ref=${settingsMenuFragment != null}, added=${settingsMenuFragment?.isAdded}, resumed=${settingsMenuFragment?.isResumed})"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   💾 progressFragmentActive=$progressFragmentActive (ref=${progressFragment != null}, added=${progressFragment?.isAdded}, resumed=${progressFragment?.isResumed})"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🚪 exitFragmentActive=$exitFragmentActive (ref=${exitFragment != null}, added=${exitFragment?.isAdded}, resumed=${exitFragment?.isResumed})"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE]   🔄 dismissingSubmenu=$dismissingSubmenu, forceMainMenuActive=$forceMainMenuActive"
        )
        android.util.Log.d("GameActivityViewModel", "[ACTIVE] ✅ RESULT: isAnyMenuActive=$result")

        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE] 🔍 isAnyMenuActive: ========== CHECK COMPLETED =========="
        )
        return result
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
                    "dismiss${fragmentName}: Main menu restoration handled by BackStackChangeListener (retroMenu3Open=$retroMenu3OpenBefore)"
            )
            // REMOVED: retroMenu3Fragment?.restoreMainMenu()
            // The BackStackChangeListener in RetroMenu3Fragment will handle menu restoration
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: NOT showing main menu (dismissingAll=${isDismissingAllMenus()}, retroMenu3Open=$retroMenu3OpenBefore)"
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
                    "isSettingsMenuOpen check: fragment=${settingsMenuFragment}, isAdded=${settingsMenuFragment?.isAdded}, result=$isOpen"
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

    /** Dismiss ALL menus in cascade order (submenus first, then main menu) */
    // REMOVED: dismissAllMenus() - NavigationController handles menu dismissal now

    /** Register the SettingsMenuFragment when it's created */
    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] ⚙️ registerSettingsMenuFragment: Registering SettingsMenuFragment - isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
        )
        settingsMenuFragment = fragment
        menuViewModel.registerSettingsMenuFragment(fragment)
        activateSettingsMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] ⚙️ registerSettingsMenuFragment: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
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
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] ⚙️ registerSettingsMenuFragmentForRotation: Registering without state activation"
        )
        settingsMenuFragment = fragment
        // Register with MenuManager only (no activation)
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] ⚙️ registerSettingsMenuFragmentForRotation: Completed (state NOT changed)"
        )
    }

    /** Register the ProgressFragment when it's created */
    fun registerProgressFragment(fragment: ProgressFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 💾 registerProgressFragment: Registering ProgressFragment - isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
        )
        progressFragment = fragment
        menuViewModel.registerProgressFragment(fragment)
        activateProgressMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 💾 registerProgressFragment: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
        )
    }

    /** Register ProgressFragment for rotation recreation (without activating state) */
    fun registerProgressFragmentForRotation(fragment: ProgressFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 💾 registerProgressFragmentForRotation: Registering without state activation"
        )
        progressFragment = fragment
        menuViewModel.registerProgressFragment(fragment)
        // Register with MenuManager only (no activation)
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 💾 registerProgressFragmentForRotation: Completed (state NOT changed)"
        )
    }

    /** Register the ExitFragment when it's created */
    fun registerExitFragment(fragment: ExitFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 🚪 registerExitFragment: Registering ExitFragment - isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
        )
        exitFragment = fragment
        menuViewModel.registerExitFragment(fragment)
        activateExitMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 🚪 registerExitFragment: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
        )
    }

    /** Register ExitFragment for rotation recreation (without activating state) */
    fun registerExitFragmentForRotation(fragment: ExitFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 🚪 registerExitFragmentForRotation: Registering without state activation"
        )
        exitFragment = fragment
        menuViewModel.registerExitFragment(fragment)
        // Register with MenuManager only (no activation)
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 🚪 registerExitFragmentForRotation: Completed (state NOT changed)"
        )
    }

    /** Register the AboutFragment when it's created */
    fun registerAboutFragment(fragment: AboutFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 📋 registerAboutFragment: Registering AboutFragment - isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
        )
        aboutFragment = fragment
        activateAboutMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 📋 registerAboutFragment: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
        )
    }

    /** Register AboutFragment for rotation recreation (without activating state) */
    fun registerAboutFragmentForRotation(fragment: AboutFragment) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 📋 registerAboutFragmentForRotation: Registering without state activation"
        )
        aboutFragment = fragment
        // Register with MenuManager only (no activation)
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU,
                fragment
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[REGISTER] 📋 registerAboutFragmentForRotation: Completed (state NOT changed)"
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
        val currentRetroView = retroView
        val utils = retroViewUtils

        if (currentRetroView?.frameRendered?.value != true || utils == null) {
            onComplete?.invoke()
            return
        }

        if (utils.hasSaveState() != true) {
            onComplete?.invoke()
            return
        }

        val savedFrameSpeed = currentRetroView.view.frameSpeed
        currentRetroView.view.frameSpeed = 1
        utils.loadState(currentRetroView)
        currentRetroView.view.frameSpeed = savedFrameSpeed
        skipNextTempStateLoad = true
        onComplete?.invoke()
    }

    /**
     * Centralized save state implementation with improved debugging FIX: Removed unnecessary delay
     * that could cause timing issues
     */
    fun saveStateCentralized(onComplete: (() -> Unit)? = null, keepPaused: Boolean = false) {
        val currentRetroView = retroView
        val utils = retroViewUtils

        if (currentRetroView == null || utils == null) {
            onComplete?.invoke()
            return
        }

        val savedFrameSpeed = currentRetroView.view.frameSpeed
        if (savedFrameSpeed == 0 && !keepPaused) {
            // Only temporarily unpause if not explicitly keeping paused (menu context)
            currentRetroView.view.frameSpeed = 1
            Handler(Looper.getMainLooper())
                    .postDelayed(
                            {
                                utils.saveState(currentRetroView)
                                currentRetroView.view.frameSpeed = savedFrameSpeed
                                onComplete?.invoke()
                            },
                            200
                    )
        } else {
            // Keep current frameSpeed (including 0 for paused state in menu)
            utils.saveState(currentRetroView)
            onComplete?.invoke()
        }
    }

    /**
     * Centralized reset game implementation with improved debugging FIX: Ensure that reset really
     * restarts the game from the beginning
     */
    fun resetGameCentralized(onComplete: (() -> Unit)? = null) {
        retroView?.view?.reset()
        onComplete?.invoke()
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

    // ========== SCREENSHOT CAPTURE FOR SAVE STATES ==========

    /**
     * Capture screenshot when menu opens. Called from showRetroMenu3() before pausing the game.
     *
     * @param onCaptured Optional callback when capture completes
     */
    fun captureScreenshotForSaveState(onCaptured: ((Boolean) -> Unit)? = null) {
        retroView?.view?.let { glRetroView ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.vinaooo.revenger.utils.ScreenshotCaptureUtil.captureAndCacheScreenshot(
                        glRetroView,
                        onCaptured
                )
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
        if (!resources.getBoolean(R.bool.conf_fullscreen)) return

        with(window.insetsController!!) {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** Hook the RetroView with the GLRetroView instance */
    fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
        retroView = RetroView(activity, viewModelScope)
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

    /** Subscribe the GamePads to the RetroView */
    fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val context = getApplication<Application>().applicationContext

        val gamePadConfig = GamePadConfig(context, resources)
        leftGamePad =
                GamePad(context, gamePadConfig.left) { event: Event ->
                    val intercepted =
                            when (event) {
                                is Event.Button ->
                                        controllerInput.processGamePadButtonEvent(
                                                event.id,
                                                event.action
                                        )
                                is Event.Direction -> {
                                    // Create synthetic MotionEvent for DPAD using PointerCoords
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

                                    val motionEvent =
                                            MotionEvent.obtain(
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
                                    // Process motion and return false (directions are never
                                    // intercepted)
                                    controllerInput.processMotionEvent(motionEvent, retroView!!)
                                    false
                                }
                                else -> false // Other event types are not intercepted
                            }
                    intercepted // Return the boolean
                }
        rightGamePad =
                GamePad(context, gamePadConfig.right) { event: Event ->
                    val intercepted =
                            when (event) {
                                is Event.Button ->
                                        controllerInput.processGamePadButtonEvent(
                                                event.id,
                                                event.action
                                        )
                                is Event.Direction -> {
                                    // Create synthetic MotionEvent for DPAD using PointerCoords
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

                                    val motionEvent =
                                            MotionEvent.obtain(
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
                                    // Process motion and return false (directions are never
                                    // intercepted)
                                    controllerInput.processMotionEvent(motionEvent, retroView!!)
                                    false
                                }
                                else -> false // Other event types are not intercepted
                            }
                    intercepted // Return the boolean
                }

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }
    }

    /** Hide the on-screen GamePads */
    fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val visibility = if (GamePad.shouldShowGamePads(activity)) View.VISIBLE else View.GONE

        leftContainer.visibility = visibility
        rightContainer.visibility = visibility
    }

    /** Process a key event and return the result */
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        // DEBUG: Log ALL key events to diagnose Backspace issue
        android.util.Log.d(
                "GameActivityViewModel",
                "[KEY-EVENT] keyCode=$keyCode, action=${event.action}, navigationSystemActive=true"
        )

        // PHASE 4.1c: Check for keyboard input (permanently enabled)
        if (keyboardInputAdapter != null) {
            // Check if this is a navigation key
            if (keyboardInputAdapter!!.isNavigationKey(keyCode)) {
                // PHASE 4.2c: Allow F12 even when menu is closed (to open menu)
                // But Backspace (DEL) only works when menu is OPEN (to navigate back)
                val isMenuActive = isAnyMenuActive()
                val shouldProcessKeyboard = isMenuActive || keyCode == KeyEvent.KEYCODE_F12

                android.util.Log.d(
                        "GameActivityViewModel",
                        "[PHASE4] Navigation key check: keyCode=$keyCode, action=${event.action}, isMenuActive=$isMenuActive, shouldProcess=$shouldProcessKeyboard"
                )

                if (shouldProcessKeyboard) {
                    android.util.Log.d(
                            "GameActivityViewModel",
                            "[PHASE4] Routing key event to KeyboardInputAdapter: keyCode=$keyCode, action=${event.action}"
                    )
                    // Route to keyboard adapter based on action type
                    val consumed =
                            when (event.action) {
                                KeyEvent.ACTION_DOWN ->
                                        keyboardInputAdapter!!.onKeyDown(keyCode, event)
                                KeyEvent.ACTION_UP -> keyboardInputAdapter!!.onKeyUp(keyCode, event)
                                else -> false
                            }
                    if (consumed) {
                        return true // Event was consumed by menu navigation
                    }
                }
            }
        }

        // Process normally via ControllerInput (for game inputs)
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
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
        val configOrientation = resources.getInteger(R.integer.conf_orientation)
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

    /** Check if menu is enabled based on menu mode configs */
    private fun isMenuEnabled(): Boolean {
        return resources.getBoolean(R.bool.conf_menu_mode_back) ||
                resources.getBoolean(R.bool.conf_menu_mode_combo) ||
                resources.getBoolean(R.bool.conf_menu_mode_gamepad)
    }

    /** Check if menu should respond to back button based on conf_menu_mode_back */
    fun shouldHandleBackButton(): Boolean {
        return resources.getBoolean(R.bool.conf_menu_mode_back)
    }

    /** Check if menu should respond to SELECT+START combo based on conf_menu_mode_combo */
    fun shouldHandleSelectStartCombo(): Boolean {
        return resources.getBoolean(R.bool.conf_menu_mode_combo)
    }

    /** Check if menu should respond to gamepad menu button based on conf_menu_mode_gamepad */
    fun shouldHandleGamepadMenuButton(): Boolean {
        return resources.getBoolean(R.bool.conf_menu_mode_gamepad)
    }

    /**
     * Inicializa os controllers modulares com as mesmas SharedPreferences do RetroViewUtils Garante
     * compatibilidade com o sistema existente
     */
    private fun initializeControllers(activity: Activity) {
        val sharedPrefs = activity.getPreferences(android.content.Context.MODE_PRIVATE)
        sharedPreferences = sharedPrefs
        audioController = AudioController(activity.applicationContext, sharedPrefs)
        speedController = SpeedController(activity.applicationContext, sharedPrefs)
        shaderController = ShaderController(activity.applicationContext, sharedPrefs)

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
        controllerInput.clearKeyLog()
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

    override fun onMenuEvent(event: com.vinaooo.revenger.ui.retromenu3.MenuEvent) {
        when (event) {
            is com.vinaooo.revenger.ui.retromenu3.MenuEvent.Action -> {
                // Handle menu actions
                when (event.action) {
                    com.vinaooo.revenger.ui.retromenu3.MenuAction.SAVE_STATE ->
                            saveStateCentralized()
                    com.vinaooo.revenger.ui.retromenu3.MenuAction.LOAD_STATE ->
                            loadStateCentralized()
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
                                onComplete = {
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                }
                        )
                    }
                    com.vinaooo.revenger.ui.retromenu3.MenuAction.EXIT -> {
                        // Exit without save
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    com.vinaooo.revenger.ui.retromenu3.MenuAction.BACK -> {
                        // Handle back navigation based on current state
                        when (menuManager.getCurrentState()) {
                            com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU ->
                                    dismissRetroMenu3()
                            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU ->
                                    dismissSettingsMenu()
                            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU ->
                                    dismissProgress()
                            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU ->
                                    dismissAboutMenu()
                            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU -> dismissExit()
                        }
                    }
                    is com.vinaooo.revenger.ui.retromenu3.MenuAction.NAVIGATE -> {
                        // Navigate to different menu state
                        menuManager.navigateToState(event.action.targetMenu)
                    }
                    else -> {
                        // Ignore other actions
                    }
                }
            }
            is com.vinaooo.revenger.ui.retromenu3.MenuEvent.StateChanged -> {
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
