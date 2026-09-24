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
import com.swordfish.radialgamepad.library.event.Event
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
import com.vinaooo.revenger.viewmodels.menu.MenuCloseHandler
import com.vinaooo.revenger.viewmodels.menu.MenuNavigationCallbackWiring
import com.vinaooo.revenger.viewmodels.menu.MenuNavigationCallbackWiringFacade
import com.vinaooo.revenger.viewmodels.menu.MenuOpenHandler
import com.vinaooo.revenger.viewmodels.menu.NavigationControllerInitializer
import com.vinaooo.revenger.viewmodels.menu.PlaybackStateController
import com.vinaooo.revenger.viewmodels.menu.PlaybackStateFacade
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3ContainerConfig
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3ContainerConfigFacade
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3FragmentLifecycle
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3FragmentLifecycleFacade
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3ToggleController
import com.vinaooo.revenger.viewmodels.menu.RetroMenu3ToggleFacade
import com.vinaooo.revenger.viewmodels.menu.SaveLoadCentralizedController
import com.vinaooo.revenger.viewmodels.menu.SaveLoadCentralizedFacade
import com.vinaooo.revenger.viewmodels.menu.SaveLoadOrchestrator
import com.vinaooo.revenger.viewmodels.menu.ScreenshotPreviewController
import com.vinaooo.revenger.viewmodels.menu.ScreenshotPreviewFacade
import com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentDismisser
import com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentDismissal
import com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentRegistrar
import com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentRegistration
import com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentState
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application),
        SettingsMenuListener,
        AboutListener,
        MenuManager.MenuManagerListener,
        SubmenuFragmentRegistration,
        SubmenuFragmentDismissal,
        ScreenshotPreviewFacade,
        SaveLoadCentralizedFacade,
        PlaybackStateFacade,
        RetroMenu3FragmentLifecycleFacade,
        RetroMenu3ContainerConfigFacade,
        RetroMenu3ToggleFacade,
        MenuNavigationCallbackWiringFacade {

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
    override fun showLoadPreview(bitmap: android.graphics.Bitmap) =
            screenshotPreviewController.showLoadPreview(bitmap)

    /**
     * Hide the load preview overlay. Called when navigating away from Load State or when menu
     * closes.
     */
    override fun hideLoadPreview() = screenshotPreviewController.hideLoadPreview()

    /**
     * Get the cached full-screen screenshot (with black bars) for preview overlay. Used when saving
     * to slot — the full screenshot is saved alongside the cropped one.
     */
    override fun getCachedFullScreenshot(): android.graphics.Bitmap? =
            screenshotPreviewController.getCachedFullScreenshot()

    // ===== CENTRALIZED STATE MANAGEMENT =====
    // Distributed state migrated to MenuStateManager

    // Centralized Menu State Manager (must be initialized first for MenuManager)
    private val menuStateManager: com.vinaooo.revenger.ui.retromenu3.MenuStateManager =
            com.vinaooo.revenger.ui.retromenu3.MenuStateManager()

    // Unified Menu Manager for centralized menu navigation
    private val menuManager: MenuManager = MenuManager(this, menuStateManager)

    /** Get the MenuManager instance */
    fun getMenuManager(): MenuManager = menuManager

    // menuManager/menuViewModel are passed as providers, not direct references, because tests
    // replace those fields by reflection after this ViewModel (and these delegates) are built.
    private val submenuFragmentState = SubmenuFragmentState()
    private val submenuFragmentRegistrar =
            SubmenuFragmentRegistrar(
                    state = submenuFragmentState,
                    menuManager = { menuManager },
                    menuStateManager = menuStateManager,
                    menuViewModel = { menuViewModel },
                    isAnyMenuActive = { isAnyMenuActive() }
            )
    private val submenuFragmentDismisser =
            SubmenuFragmentDismisser(
                    state = submenuFragmentState,
                    menuManager = { menuManager },
                    menuStateManager = menuStateManager,
                    isRetroMenu3Open = { isRetroMenu3Open() },
                    isDismissingAllMenus = { isDismissingAllMenus() }
            )
    private val screenshotPreviewController =
            ScreenshotPreviewController(
                    retroView = { retroView },
                    loadPreviewCallback = { loadPreviewCallback },
                    isScreenshotCaptureSuppressed = { suppressNextScreenshotCapture },
                    clearScreenshotCaptureSuppression = { suppressNextScreenshotCapture = false }
            )
    private val saveLoadCentralizedController =
            SaveLoadCentralizedController(
                    saveLoadOrchestrator = saveLoadOrchestrator,
                    retroView = { retroView },
                    retroViewUtils = { retroViewUtils },
                    markSkipNextTempStateLoad = { skipNextTempStateLoad = true }
            )
    private val playbackStateController =
            PlaybackStateController(
                    audioViewModel = audioViewModel,
                    speedViewModel = speedViewModel,
                    shaderViewModel = shaderViewModel
            )
    private val retroMenu3FragmentLifecycle =
            RetroMenu3FragmentLifecycle(
                    retroMenu3Fragment = { retroMenu3Fragment },
                    setRetroMenu3Fragment = { retroMenu3Fragment = it },
                    menuManager = { menuManager }
            )
    private val retroMenu3ContainerConfig =
            RetroMenu3ContainerConfig(
                    menuContainerView = { menuContainerView },
                    setMenuContainerView = { menuContainerView = it },
                    setGamePadContainerView = { gamePadContainerView = it },
                    menuViewModel = { menuViewModel }
            )
    private val retroMenu3ToggleController =
            RetroMenu3ToggleController(
                    navigationController = { navigationController },
                    retroMenu3Fragment = { retroMenu3Fragment },
                    menuStateManager = menuStateManager,
                    inputViewModel = inputViewModel,
                    controllerInput = { controllerInput },
                    isRetroMenu3Open = { isRetroMenu3Open() }
            )
    private val menuOpenHandler =
            MenuOpenHandler(
                    retroView = { retroView },
                    retroViewUtils = { retroViewUtils },
                    speedController = { speedController },
                    captureScreenshotForSaveState = { captureScreenshotForSaveState() }
            )
    private val menuCloseHandler =
            MenuCloseHandler(
                    retroView = { retroView },
                    speedController = { speedController },
                    controllerInput = { controllerInput },
                    clearCachedScreenshot = { clearCachedScreenshot() },
                    hideLoadPreview = { hideLoadPreview() },
                    viewModelScope = viewModelScope
            )
    private val navigationControllerInitializer =
            NavigationControllerInitializer(
                    navigationController = { navigationController },
                    setNavigationController = { navigationController = it },
                    setKeyboardInputAdapter = { keyboardInputAdapter = it },
                    isAnyMenuActive = { isAnyMenuActive() },
                    onMenuOpened = { activity -> menuOpenHandler.handleMenuOpened(activity) },
                    onMenuClosed = { activity, closingButton ->
                        menuCloseHandler.handleMenuClosed(activity, closingButton)
                    }
            )
    private val menuNavigationCallbackWiring =
            MenuNavigationCallbackWiring(
                    controllerInput = { controllerInput },
                    navigationController = { navigationController },
                    isAnyMenuActive = { isAnyMenuActive() },
                    isDismissingAllMenus = { isDismissingAllMenus() },
                    retroMenu3Fragment = { retroMenu3Fragment },
                    initializeNavigationController = { activity ->
                        navigationControllerInitializer.initializeNavigationControllerIfNeeded(
                                activity
                        )
                    }
            )

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
    override fun setupMenuCallback(activity: FragmentActivity) =
            menuNavigationCallbackWiring.setupMenuCallback(activity)

    /** Create an instance of the RetroMenu3 overlay (activated by SELECT+START) */
    override fun prepareRetroMenu3() = retroMenu3FragmentLifecycle.prepareRetroMenu3()

    /** Force recreation of RetroMenu3Fragment (used after configuration changes) */
    override fun recreateRetroMenu3() = retroMenu3FragmentLifecycle.recreateRetroMenu3()

    /** Set menu container reference from activity layout */
    override fun setMenuContainer(container: FrameLayout) =
            retroMenu3ContainerConfig.setMenuContainer(container)

    /** Get menu container ID for consistent fragment placement */
    override fun getMenuContainerId(): Int = retroMenu3ContainerConfig.getMenuContainerId()

    /** Update RetroMenu3Fragment reference after recreation (e.g., after rotation) */
    override fun updateRetroMenu3FragmentReference(fragment: RetroMenu3Fragment) =
            retroMenu3FragmentLifecycle.updateRetroMenu3FragmentReference(fragment)

    /** Set GamePad container reference to force it on top when menu opens */
    override fun setGamePadContainer(container: android.widget.LinearLayout) =
            retroMenu3ContainerConfig.setGamePadContainer(container)

    /** Toggles the Retro Menu 3 open/closed state using the NavigationController */
    override fun toggleMainMenu() = retroMenu3ToggleController.toggleMainMenu()

    /** Dismiss the RetroMenu3 */
    override fun dismissRetroMenu3(onAnimationEnd: (() -> Unit)?) =
            retroMenu3ToggleController.dismissRetroMenu3(onAnimationEnd)

    /**
     * Clears only controller states without closing the fragment. Used when the fragment closes on
     * its own (e.g.: Continue button)
     */
    override fun clearControllerInputState() = retroMenu3ToggleController.clearControllerInputState()

    /** Check if the RetroMenu3 is currently open */
    override fun isRetroMenu3Open(): Boolean = retroMenu3FragmentLifecycle.isRetroMenu3Open()

    /** Check if any menu is currently active */
    override fun isAnyMenuActive(): Boolean = retroMenu3ToggleController.isAnyMenuActive()

    /** Check if the Settings submenu is currently open */
    override fun isSettingsMenuOpen(): Boolean = submenuFragmentDismisser.isSettingsMenuOpen()

    /** Check if the Progress submenu is currently open */
    override fun isProgressMenuOpen(): Boolean = submenuFragmentDismisser.isProgressMenuOpen()

    /** Check if the Exit submenu is currently open */
    override fun isExitMenuOpen(): Boolean = submenuFragmentDismisser.isExitMenuOpen()

    /** Dismiss the Settings submenu */
    override fun dismissSettingsMenu() = submenuFragmentDismisser.dismissSettingsMenu()

    /** Dismiss the Progress submenu */
    override fun dismissProgress() = submenuFragmentDismisser.dismissProgress()

    /** Dismiss the Exit submenu */
    override fun dismissExit() = submenuFragmentDismisser.dismissExit()

    /** Dismiss the About submenu */
    override fun dismissAboutMenu() = submenuFragmentDismisser.dismissAboutMenu()

    /** Dismiss ALL menus in cascade order (submenus first, then main menu) */
    // REMOVED: dismissAllMenus() - NavigationController handles menu dismissal now

    /** Register the SettingsMenuFragment when it's created */
    override fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) =
            submenuFragmentRegistrar.registerSettingsMenuFragment(fragment)

    /** Unregister SettingsMenuFragment when closing via BACK */
    override fun unregisterSettingsMenuFragment() =
            submenuFragmentRegistrar.unregisterSettingsMenuFragment()

    /** Register SettingsMenuFragment for rotation recreation (without activating state) */
    override fun registerSettingsMenuFragmentForRotation(fragment: SettingsMenuFragment) =
            submenuFragmentRegistrar.registerSettingsMenuFragmentForRotation(fragment)

    /** Register the ProgressFragment when it's created */
    override fun registerProgressFragment(fragment: ProgressFragment) =
            submenuFragmentRegistrar.registerProgressFragment(fragment)

    /** Register ProgressFragment for rotation recreation (without activating state) */
    override fun registerProgressFragmentForRotation(fragment: ProgressFragment) =
            submenuFragmentRegistrar.registerProgressFragmentForRotation(fragment)

    /** Register the ExitFragment when it's created */
    override fun registerExitFragment(fragment: ExitFragment) =
            submenuFragmentRegistrar.registerExitFragment(fragment)

    /** Register ExitFragment for rotation recreation (without activating state) */
    override fun registerExitFragmentForRotation(fragment: ExitFragment) =
            submenuFragmentRegistrar.registerExitFragmentForRotation(fragment)

    /** Register the AboutFragment when it's created */
    override fun registerAboutFragment(fragment: AboutFragment) =
            submenuFragmentRegistrar.registerAboutFragment(fragment)

    /** Register AboutFragment for rotation recreation (without activating state) */
    override fun registerAboutFragmentForRotation(fragment: AboutFragment) =
            submenuFragmentRegistrar.registerAboutFragmentForRotation(fragment)


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
                "onBackToMainMenu: settingsMenuFragment = ${submenuFragmentState.settingsMenuFragment}"
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
    override fun loadStateCentralized(onComplete: (() -> Unit)?) =
            saveLoadCentralizedController.loadStateCentralized(onComplete)

    /**
     * Centralized save state implementation with improved debugging FIX: Removed unnecessary delay
     * that could cause timing issues
     */
    override fun saveStateCentralized(onComplete: (() -> Unit)?, keepPaused: Boolean) =
            saveLoadCentralizedController.saveStateCentralized(onComplete, keepPaused)

    /**
     * Centralized reset game implementation with improved debugging FIX: Ensure that reset really
     * restarts the game from the beginning
     */
    override fun resetGameCentralized(onComplete: (() -> Unit)?) =
            saveLoadCentralizedController.resetGameCentralized(onComplete)

    /** Check if save state exists for UI state management */
    override fun hasSaveState(): Boolean = saveLoadCentralizedController.hasSaveState()

    /** Get current audio state for UI management */
    override fun getAudioState(): Boolean = playbackStateController.getAudioState()

    /** Get current fast forward state for UI management */
    override fun getFastForwardState(): Boolean = playbackStateController.getFastForwardState()

    /** Toggle shader for visual effects */
    override fun onToggleShader(): String = playbackStateController.onToggleShader()

    /** Get current shader state for UI management */
    override fun getShaderState(): String = playbackStateController.getShaderState()

    override fun getShaderDisplayName(): String = playbackStateController.getShaderDisplayName()

    // ========== SCREENSHOT CAPTURE FOR SAVE STATES ==========

    /** Property to skip next screenshot. Used by PiP. */
    var suppressNextScreenshotCapture: Boolean = false

    /**
     * Capture screenshot when menu opens. Called from showRetroMenu3() before pausing the game.
     *
     * @param onCaptured Optional callback when capture completes
     */
    override fun captureScreenshotForSaveState(onCaptured: ((Boolean) -> Unit)?) =
            screenshotPreviewController.captureScreenshotForSaveState(onCaptured)

    /** Get cached screenshot for save operation. Returns null if no screenshot was captured. */
    override fun getCachedScreenshot(): android.graphics.Bitmap? =
            screenshotPreviewController.getCachedScreenshot()

    /**
     * Clear cached screenshot when menu closes without saving. Frees memory used by the cached
     * bitmap.
     */
    override fun clearCachedScreenshot() = screenshotPreviewController.clearCachedScreenshot()

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

    /** Clear controller key log (used by RetroMenu3Fragment on destroy) */
    fun clearControllerKeyLog() {
        controllerInput.comboTracker.clearKeyLog()
    }

    /** Check if we are currently dismissing all menus (used by RetroMenu3Fragment) */
    fun isDismissingAllMenus(): Boolean {
        return menuStateManager.isDismissingAllMenus()
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
            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU ->
                    menuStateManager.activateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.SETTINGS_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU ->
                    menuStateManager.activateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.PROGRESS_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU ->
                    menuStateManager.activateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.ABOUT_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU ->
                    menuStateManager.activateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.EXIT_MENU)
            else -> {}
        }

        // Deactivate previous menu if it was a submenu
        when (event.from) {
            com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU ->
                    menuStateManager.deactivateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.SETTINGS_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU ->
                    menuStateManager.deactivateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.PROGRESS_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU ->
                    menuStateManager.deactivateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.ABOUT_MENU)
            com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU ->
                    menuStateManager.deactivateMenu(
                            com.vinaooo.revenger.ui.retromenu3.MenuSystemState.MenuType.EXIT_MENU)
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
        submenuFragmentState.clearAll()

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
    override fun onRetroMenu3FragmentDestroyed() = retroMenu3FragmentLifecycle.onRetroMenu3FragmentDestroyed()
}
