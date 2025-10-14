package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
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
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application),
        SettingsMenuFragment.SettingsMenuListener,
        MenuManager.MenuManagerListener {

    private val resources = application.resources

    // ===== SPECIALIZED VIEWMODELS =====
    // Using composition pattern to separate concerns

    /** Menu management ViewModel */
    private lateinit var menuViewModel: MenuViewModel

    /** Game state management ViewModel */
    private lateinit var gameStateViewModel: GameStateViewModel

    /** Input management ViewModel */
    private lateinit var inputViewModel: InputViewModel

    /** Audio management ViewModel */
    private lateinit var audioViewModel: AudioViewModel

    /** Shader management ViewModel */
    private lateinit var shaderViewModel: ShaderViewModel

    /** Speed management ViewModel */
    private lateinit var speedViewModel: SpeedViewModel

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

    // ===== CENTRALIZED STATE MANAGEMENT =====
    // Estado distribuído migrado para MenuStateManager

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

    /** Set dismissing all menus flag */
    private fun setDismissingAllMenus(dismissing: Boolean) {
        menuStateManager.setDismissingAllMenus(dismissing)
    }

    // Unified Menu Manager for centralized menu navigation
    private lateinit var menuManager: MenuManager

    // Centralized Menu State Manager
    private lateinit var menuStateManager: com.vinaooo.revenger.ui.retromenu3.MenuStateManager

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
        // Initialize centralized Menu State Manager
        menuStateManager = com.vinaooo.revenger.ui.retromenu3.MenuStateManager()

        // Initialize unified Menu Manager
        menuManager = MenuManager(this, menuStateManager)

        // Initialize specialized ViewModels using composition pattern
        menuViewModel = MenuViewModel(application)
        gameStateViewModel = GameStateViewModel(application)
        inputViewModel = InputViewModel(application)
        audioViewModel = AudioViewModel(application)
        shaderViewModel = ShaderViewModel(application)
        speedViewModel = SpeedViewModel(application)

        // Set the callback to check if SELECT+START combo should work
        controllerInput.shouldHandleSelectStartCombo = { shouldHandleSelectStartCombo() }
    }

    /** Configure menu callback with activity reference */
    fun setupMenuCallback(activity: FragmentActivity) {
        // Configure RetroMenu3 callback for SELECT+START combo
        controllerInput.selectStartComboCallback = { showRetroMenu3(activity) }

        // Configure START button callback to close ALL menus (submenus first, then main menu)
        controllerInput.startButtonCallback = { dismissAllMenus() }

        // Configure navigation callbacks for RetroMenu3
        controllerInput.menuNavigateUpCallback = { menuManager.sendNavigateUp() }
        controllerInput.menuNavigateDownCallback = { menuManager.sendNavigateDown() }
        controllerInput.menuConfirmCallback = { menuManager.sendConfirm() }
        controllerInput.menuBackCallback = {
            // Always delegate BACK handling to MenuManager - it will handle submenus properly
            menuManager.sendBack()
        }

        // Control when to intercept DPAD for menu
        controllerInput.shouldInterceptDpadForMenu = { isAnyMenuActive() }

        // Control when START button alone should work (only when RetroMenu3 or SettingsMenu
        // is REALLY open)
        controllerInput.shouldHandleStartButton = { isAnyMenuActive() }

        // Control when to block ALL gamepad inputs (when RetroMenu3 or SettingsMenu
        // is open)
        controllerInput.shouldBlockAllGamepadInput = { isAnyMenuActive() }

        // Control if RetroMenu3 or SettingsMenu is open for combo reset
        controllerInput.isRetroMenu3Open = { isAnyMenuActive() }
    }

    /** Create an instance of the RetroMenu3 overlay (activated by SELECT+START) */
    fun prepareRetroMenu3(activity: ComponentActivity) {
        android.util.Log.e(
                "GAME_ACTIVITY_VIEWMODEL",
                "🚨🚨🚨 PREPARE_RETRO_MENU3 CALLED - NEW APK VERSION 🚨🚨🚨"
        )
        android.util.Log.e("GAME_ACTIVITY_VIEWMODEL", "📅 TIMESTAMP: ${java.util.Date()}")
        android.util.Log.e(
                "GAME_ACTIVITY_VIEWMODEL",
                "🔧 APK VERSION: DEBUG WITH EXTENSIVE LOGGING"
        )

        if (retroMenu3Fragment != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[PREPARE] ⚠️ prepareRetroMenu3: RetroMenu3Fragment already exists, skipping"
            )
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[PREPARE] 🎯 prepareRetroMenu3: ========== PREPARATION SKIPPED =========="
            )
            return
        }

        android.util.Log.d(
                "GameActivityViewModel",
                "[PREPARE] 🆕 prepareRetroMenu3: Creating new RetroMenu3Fragment"
        )
        retroMenu3Fragment =
                RetroMenu3Fragment.newInstance().apply {
                    // REMOVED: setMenuListener - migrated to unified MenuAction/MenuEvent system
                    // setMenuListener(this@GameActivityViewModel)
                }

        android.util.Log.d(
                "GameActivityViewModel",
                "[PREPARE] 📝 prepareRetroMenu3: Registering RetroMenu3Fragment with MenuManager"
        )
        // Register RetroMenu3Fragment with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU,
                retroMenu3Fragment!!
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[PREPARE] ✅ prepareRetroMenu3: RetroMenu3Fragment registered with MenuManager"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[PREPARE] 🎯 prepareRetroMenu3: ========== PREPARATION COMPLETED =========="
        )
    }

    /** Force recreation of RetroMenu3Fragment (used after configuration changes) */
    fun recreateRetroMenu3(activity: ComponentActivity) {
        android.util.Log.d(
                "GameActivityViewModel",
                "[RECREATE] 🔄 recreateRetroMenu3: Forcing recreation of RetroMenu3Fragment"
        )

        // Clean up existing fragment reference
        retroMenu3Fragment = null

        // Recreate the fragment
        prepareRetroMenu3(activity)

        android.util.Log.d(
                "GameActivityViewModel",
                "[RECREATE] ✅ recreateRetroMenu3: RetroMenu3Fragment recreated successfully"
        )
    }

    /** Set menu container reference from activity layout */
    fun setMenuContainer(container: FrameLayout) {
        menuContainerView = container
        menuViewModel.setMenuContainer(container)
        android.util.Log.d("GameActivityViewModel", "Menu container set: $container")
    }

    /** Set GamePad container reference to force it on top when menu opens */
    fun setGamePadContainer(container: android.widget.LinearLayout) {
        gamePadContainerView = container
        android.util.Log.d("GameActivityViewModel", "GamePad container set: $container")
    }

    /** Show the RetroMenu3 (activated by SELECT+START combo) */
    fun showRetroMenu3(activity: FragmentActivity) {
        android.util.Log.d("GameActivityViewModel", "[SHOW_RETRO_MENU] showRetroMenu3 called!")
        android.util.Log.d(
                "GameActivityViewModel",
                "[SHOW_RETRO_MENU] Current back stack count: ${activity.supportFragmentManager.backStackEntryCount}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[SHOW_RETRO_MENU] isDismissingAllMenus: ${isDismissingAllMenus()}"
        )
        android.util.Log.d(
                "GameActivityViewModel",
                "[SHOW_RETRO_MENU] isRetroMenu3Open: ${isRetroMenu3Open()}"
        )
        android.util.Log.d("GameActivityViewModel", "[SHOW_RETRO_MENU] Stack trace:", Exception())

        // CRITICAL: Prevent multiple calls if menu is already open
        if (isRetroMenu3Open()) {
            android.util.Log.d("GameActivityViewModel", "RetroMenu3 already open, ignoring call")
            return
        }

        // Verificar se temos o container do menu
        val containerId = menuContainerView?.id ?: R.id.menu_container
        android.util.Log.d("GameActivityViewModel", "Using menu container ID: $containerId")

        if (retroView?.frameRendered?.value == true) {
            // CRITICAL: Capture currently pressed keys BEFORE showing menu
            controllerInput.captureKeysOnMenuOpen()

            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // PAUSE the game when menu opens
            retroView?.let { speedController?.pause(it.view) }

            // Show RetroMenu3
            retroMenu3Fragment?.let { menu ->
                android.util.Log.d(
                        "GameActivityViewModel",
                        "RetroMenu3Fragment is available, showing it"
                )
                if (!menu.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(containerId, menu, RetroMenu3Fragment::class.java.simpleName)
                            .commitAllowingStateLoss()
                    android.util.Log.d(
                            "GameActivityViewModel",
                            "RetroMenu3 fragment added successfully"
                    )
                } else {
                    android.util.Log.d("GameActivityViewModel", "RetroMenu3Fragment already added!")
                }

                // Set MenuManager to MAIN_MENU state when RetroMenu3 is shown
                menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU)
            }
                    ?: android.util.Log.e("GameActivityViewModel", "RetroMenu3Fragment is NULL!")
        } else {
            android.util.Log.e(
                    "GameActivityViewModel",
                    "retroView not ready or frameRendered = false"
            )
        }
    }

    /** Dismiss the RetroMenu3 */
    fun dismissRetroMenu3() {
        android.util.Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Starting")
        retroMenu3Fragment?.dismissMenuPublic()
        // CRITICAL: Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // CRITICAL: Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
        android.util.Log.d("GameActivityViewModel", "[DISMISS_MAIN] dismissRetroMenu3: Completed")
    }

    /**
     * Clears only controller states without closing the fragment. Used when the fragment closes on
     * its own (e.g.: Continue button)
     */
    fun clearControllerInputState() {
        inputViewModel.clearControllerInputState()
        controllerInput.clearKeyLog()
        controllerInput.clearBlockedKeysDelayed()
    }

    /** Check if the RetroMenu3 is currently open */
    fun isRetroMenu3Open(): Boolean {
        return retroMenu3Fragment?.isAdded == true
    }

    /** Check if any menu is currently active */
    private fun isAnyMenuActive(): Boolean {
        android.util.Log.d(
                "GameActivityViewModel",
                "[ACTIVE] 🔍 isAnyMenuActive: ========== CHECKING MENU ACTIVITY =========="
        )

        val retroMenu3Open = isRetroMenu3Open()
        val settingsActive = isSettingsMenuActive()
        val progressActive = isProgressActive()
        val exitActive = isExitActive()
        val result = retroMenu3Open || settingsActive || progressActive || exitActive

        android.util.Log.d("GameActivityViewModel", "[ACTIVE] 📊 Menu states:")
        android.util.Log.d("GameActivityViewModel", "[ACTIVE]   🎮 retroMenu3Open=$retroMenu3Open")
        android.util.Log.d("GameActivityViewModel", "[ACTIVE]   ⚙️ settingsActive=$settingsActive")
        android.util.Log.d("GameActivityViewModel", "[ACTIVE]   📊 progressActive=$progressActive")
        android.util.Log.d("GameActivityViewModel", "[ACTIVE]   🚪 exitActive=$exitActive")
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
        val activity = fragment?.activity as? androidx.fragment.app.FragmentActivity
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

        // Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
        // Clear the fragment reference and flag
        activeFlagSetter()

        // CRITICAL FIX: After dismissing submenu, ensure main menu is visible
        // BUT only if we're NOT in the middle of dismissing ALL menus (START button case)
        if (isRetroMenu3Open() && !isDismissingAllMenus()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: Showing main menu after individual submenu dismissal"
            )
            // Find the RetroMenu3Fragment and show it
            val retroMenu3Fragment =
                    activity?.supportFragmentManager?.findFragmentByTag(
                            RetroMenu3Fragment::class.java.simpleName
                    ) as?
                            RetroMenu3Fragment
            retroMenu3Fragment?.showMainMenu()
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismiss${fragmentName}: NOT showing main menu (dismissingAll=${isDismissingAllMenus()}, retroMenu3Open=${isRetroMenu3Open()})"
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
        }
    }

    /** Dismiss the Progress submenu */
    fun dismissProgress() {
        dismissSubmenuFragment(progressFragment, "Progress") {
            progressFragment = null
            deactivateProgressMenu()
        }
    }

    /** Dismiss the Exit submenu */
    fun dismissExit() {
        dismissSubmenuFragment(exitFragment, "Exit") {
            exitFragment = null
            deactivateExitMenu()
        }
    }

    /** Dismiss ALL menus in cascade order (submenus first, then main menu) */
    fun dismissAllMenus() {
        android.util.Log.d(
                "GameActivityViewModel",
                "[DISMISS_ALL] dismissAllMenus: Starting cascade dismissal"
        )

        // Set flag to prevent showing main menu when submenus are dismissed
        setDismissingAllMenus(true)
        android.util.Log.d(
                "GameActivityViewModel",
                "[DISMISS_ALL] dismissAllMenus: Set isDismissingAllMenus = true"
        )

        // Dismiss submenus first (in reverse order of opening)
        if (isExitActive()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Dismissing Exit submenu"
            )
            dismissExit()
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Exit submenu dismissed"
            )
        }
        if (isProgressActive()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Dismissing Progress submenu"
            )
            dismissProgress()
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Progress submenu dismissed"
            )
        }
        if (isSettingsMenuActive()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Dismissing Settings submenu"
            )
            dismissSettingsMenu()
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Settings submenu dismissed"
            )
        }

        // Finally dismiss the main RetroMenu3
        if (isRetroMenu3Open()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Dismissing main RetroMenu3"
            )
            dismissRetroMenu3()
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[DISMISS_ALL] dismissAllMenus: Main RetroMenu3 dismissed"
            )
        }

        // Restore game speed from sharedpreferences when exiting menu with Start
        restoreGameSpeedFromPreferences()

        // Reset flag after all menus are dismissed
        setDismissingAllMenus(false)
        android.util.Log.d(
                "GameActivityViewModel",
                "[DISMISS_ALL] dismissAllMenus: Reset isDismissingAllMenus = false"
        )

        android.util.Log.d(
                "GameActivityViewModel",
                "[DISMISS_ALL] dismissAllMenus: Cascade dismissal completed"
        )
    }

    /** Restore game speed from sharedpreferences when exiting menu with Start */
    fun restoreGameSpeedFromPreferences() {
        retroView?.let { retroView ->
            // Get saved game speed from sharedpreferences
            val savedSpeed =
                    sharedPreferences?.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1) ?: 1

            android.util.Log.d(
                    "GameActivityViewModel",
                    "restoreGameSpeedFromPreferences: Setting frameSpeed to $savedSpeed"
            )

            // Apply the saved speed (1 for Normal, 2+ for Fast)
            retroView.view.frameSpeed = savedSpeed
        }
    }

    /** Register the SettingsMenuFragment when it's created */
    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        settingsMenuFragment = fragment
        menuViewModel.registerSettingsMenuFragment(fragment)
        activateSettingsMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU,
                fragment
        )
    }

    /** Register the ProgressFragment when it's created */
    fun registerProgressFragment(fragment: ProgressFragment) {
        progressFragment = fragment
        menuViewModel.registerProgressFragment(fragment)
        activateProgressMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU,
                fragment
        )
    }

    /** Register the ExitFragment when it's created */
    fun registerExitFragment(fragment: ExitFragment) {
        exitFragment = fragment
        menuViewModel.registerExitFragment(fragment)
        activateExitMenu()
        // Register with MenuManager
        menuManager.registerFragment(
                com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU,
                fragment
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
    fun saveStateCentralized(onComplete: (() -> Unit)? = null) {
        val currentRetroView = retroView
        val utils = retroViewUtils

        if (currentRetroView == null || utils == null) {
            onComplete?.invoke()
            return
        }

        val savedFrameSpeed = currentRetroView.view.frameSpeed
        if (savedFrameSpeed == 0) {
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
        return speedController?.getFastForwardState() ?: false
    }

    /** Toggle shader for visual effects */
    fun onToggleShader(): String {
        return shaderViewModel.toggleShader()
    }

    /** Get current shader state for UI management */
    fun getShaderState(): String {
        return shaderViewModel.getShaderState()
    }

    /** Hide the system bars */
    @Suppress("DEPRECATION")
    fun immersive(window: Window) {
        /* Check if the config permits it */
        if (!resources.getBoolean(R.bool.config_fullscreen)) return

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
                GamePad(context, gamePadConfig.left) { event ->
                    when (event) {
                        is Event.Button ->
                                controllerInput.processGamePadButtonEvent(event.id, event.action)
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
                            controllerInput.processMotionEvent(motionEvent, retroView!!)
                        }
                    }
                }
        rightGamePad =
                GamePad(context, gamePadConfig.right) { event ->
                    when (event) {
                        is Event.Button ->
                                controllerInput.processGamePadButtonEvent(event.id, event.action)
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
                            controllerInput.processMotionEvent(motionEvent, retroView!!)
                        }
                    }
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
        // Process normally via ControllerInput
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
        when (resources.getInteger(R.integer.config_orientation)) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            3 -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else -> return
        }.also { activity.requestedOrientation = it }
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

    /** Check if menu is enabled based on config_menu_mode */
    private fun isMenuEnabled(): Boolean {
        val menuMode = resources.getInteger(R.integer.config_menu_mode)
        return menuMode != 0 // 0 = disabled, 1,2,3 = enabled in different ways
    }

    /** Check if menu should respond to back button based on config_menu_mode */
    fun shouldHandleBackButton(): Boolean {
        val menuMode = resources.getInteger(R.integer.config_menu_mode)
        return menuMode == 1 || menuMode == 3 // 1 = back button only, 3 = both
    }

    /** Check if menu should respond to SELECT+START combo based on config_menu_mode */
    fun shouldHandleSelectStartCombo(): Boolean {
        val menuMode = resources.getInteger(R.integer.config_menu_mode)
        return menuMode == 2 || menuMode == 3 // 2 = combo only, 3 = both
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
                        saveStateCentralized {
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
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
                        "Menu state changed: ${event.from} -> ${event.to}"
                )
                // Additional state change handling can be added here if needed
            }
            com.vinaooo.revenger.ui.retromenu3.MenuEvent.MenuClosed -> {
                // Handle complete menu closure
                dismissAllMenus()
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
}
