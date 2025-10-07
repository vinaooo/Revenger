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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.R
import com.vinaooo.revenger.controllers.AudioController
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application),
        RetroMenu3Fragment.RetroMenu3Listener,
        SettingsMenuFragment.SettingsMenuListener {

    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    // RetroMenu3 fragment (activated by SELECT+START combo)
    private var retroMenu3Fragment: RetroMenu3Fragment? = null

    // Settings submenu fragment
    private var settingsMenuFragment: SettingsMenuFragment? = null
    private var isSettingsMenuActive = false

    // New submenu fragments
    private var progressFragment: ProgressFragment? = null
    private var isProgressActive = false
    private var exitFragment: ExitFragment? = null
    private var isExitActive = false

    // Flag to prevent showing main menu when dismissing all menus at once
    private var isDismissingAllMenus = false

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput(application.applicationContext)

    // Controllers modulares
    private var audioController: AudioController? = null
    private var speedController: SpeedController? = null
    private var sharedPreferences: android.content.SharedPreferences? = null

    // Flag to prevent tempState from overwriting a manual Load State
    private var skipNextTempStateLoad = false

    init {
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
        controllerInput.menuNavigateUpCallback = {
            when {
                isProgressActive -> progressFragment?.navigateUp()
                isExitActive -> exitFragment?.navigateUp()
                isSettingsMenuActive -> settingsMenuFragment?.navigateUp()
                else -> retroMenu3Fragment?.navigateUp()
            }
        }
        controllerInput.menuNavigateDownCallback = {
            when {
                isProgressActive -> progressFragment?.navigateDown()
                isExitActive -> exitFragment?.navigateDown()
                isSettingsMenuActive -> settingsMenuFragment?.navigateDown()
                else -> retroMenu3Fragment?.navigateDown()
            }
        }
        controllerInput.menuConfirmCallback = {
            when {
                isProgressActive -> progressFragment?.confirmSelection()
                isExitActive -> exitFragment?.confirmSelection()
                isSettingsMenuActive -> settingsMenuFragment?.confirmSelection()
                else -> retroMenu3Fragment?.confirmSelection()
            }
        }
        controllerInput.menuBackCallback = {
            when {
                isProgressActive -> dismissProgress()
                isExitActive -> dismissExit()
                isSettingsMenuActive -> dismissSettingsMenu()
                isRetroMenu3Open() -> {
                    dismissRetroMenu3()
                    // Only restore game speed when closing the main menu (RetroMenu3), not when
                    // going back from submenus
                    restoreGameSpeedFromPreferences()
                }
            }
        }

        // Control when to intercept DPAD for menu
        controllerInput.shouldInterceptDpadForMenu = {
            isRetroMenu3Open() || isSettingsMenuActive || isProgressActive || isExitActive
        }

        // Control when START button alone should work (only when RetroMenu3 or SettingsMenu
        // is REALLY open)
        controllerInput.shouldHandleStartButton = {
            isRetroMenu3Open() || isSettingsMenuActive || isProgressActive || isExitActive
        }

        // Control when to block ALL gamepad inputs (when RetroMenu3 or SettingsMenu
        // is open)
        controllerInput.shouldBlockAllGamepadInput = {
            isRetroMenu3Open() || isSettingsMenuActive || isProgressActive || isExitActive
        }

        // Control if RetroMenu3 or SettingsMenu is open for combo reset
        controllerInput.isRetroMenu3Open = {
            isRetroMenu3Open() || isSettingsMenuActive || isProgressActive || isExitActive
        }
    }

    /** Create an instance of the RetroMenu3 overlay (activated by SELECT+START) */
    fun prepareRetroMenu3(activity: ComponentActivity) {
        if (retroMenu3Fragment != null) return

        retroMenu3Fragment =
                RetroMenu3Fragment.newInstance().apply {
                    setMenuListener(this@GameActivityViewModel)
                }
    }

    /** Create an instance of the RetroMenu3 overlay (activated by SELECT+START) */
    /** Show the RetroMenu3 (activated by SELECT+START combo) */
    fun showRetroMenu3(activity: FragmentActivity) {
        android.util.Log.d("GameActivityViewModel", "showRetroMenu3 called!")

        // CRITICAL: Prevent multiple calls if menu is already open
        if (isRetroMenu3Open()) {
            android.util.Log.d("GameActivityViewModel", "RetroMenu3 already open, ignoring call")
            return
        }

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
                            .add(
                                    android.R.id.content,
                                    menu,
                                    RetroMenu3Fragment::class.java.simpleName
                            )
                            .commit()
                } else {
                    android.util.Log.d("GameActivityViewModel", "RetroMenu3Fragment already added!")
                }
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
        retroMenu3Fragment?.dismissMenuPublic()
        // CRITICAL: Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // CRITICAL: Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
    }

    /**
     * Clears only controller states without closing the fragment. Used when the fragment closes on
     * its own (e.g.: Continue button)
     */
    fun clearControllerInputState() {
        controllerInput.clearKeyLog()
        controllerInput.clearBlockedKeysDelayed()
    }

    /** Check if the RetroMenu3 is currently open */
    fun isRetroMenu3Open(): Boolean {
        return retroMenu3Fragment?.isAdded == true
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

    /** Dismiss the Settings submenu */
    fun dismissSettingsMenu() {
        android.util.Log.d("GameActivityViewModel", "dismissSettingsMenu: Starting")

        // IMPORTANT: Since the SettingsMenuFragment was added to the back stack,
        // we must use popBackStack() instead of manual remove()
        // This ensures FragmentManager properly manages the hierarchy

        // Check if there's anything in the back stack before trying to remove
        val activity = settingsMenuFragment?.activity as? FragmentActivity
        if (activity != null) {
            val fragmentManager = activity.supportFragmentManager
            val backStackCount = fragmentManager.backStackEntryCount

            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissSettingsMenu: backStackCount = $backStackCount"
            )

            if (backStackCount > 0) {
                // Usar popBackStack para remover o SettingsMenuFragment corretamente
                android.util.Log.d(
                        "GameActivityViewModel",
                        "dismissSettingsMenu: Calling popBackStackImmediate()"
                )
                fragmentManager.popBackStackImmediate()
            } else {
                android.util.Log.w(
                        "GameActivityViewModel",
                        "dismissSettingsMenu: Back stack is empty, nothing to pop"
                )
            }
        }

        // Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
        // Clear the fragment reference and flag
        settingsMenuFragment = null
        isSettingsMenuActive = false

        android.util.Log.d("GameActivityViewModel", "dismissSettingsMenu: Completed")
    }

    /** Dismiss the Progress submenu */
    fun dismissProgress() {
        android.util.Log.d("GameActivityViewModel", "dismissProgress: Starting")

        // IMPORTANT: Since the ProgressFragment was added to the back stack,
        // we must use popBackStack() instead of manual remove()
        // This ensures FragmentManager properly manages the hierarchy

        // Check if there's anything in the back stack before trying to remove
        val activity = progressFragment?.activity as? FragmentActivity
        if (activity != null) {
            val fragmentManager = activity.supportFragmentManager
            val backStackCount = fragmentManager.backStackEntryCount

            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissProgress: backStackCount = $backStackCount"
            )

            if (backStackCount > 0) {
                // Usar popBackStack para remover o ProgressFragment corretamente
                android.util.Log.d(
                        "GameActivityViewModel",
                        "dismissProgress: Calling popBackStackImmediate()"
                )
                fragmentManager.popBackStackImmediate()
            } else {
                android.util.Log.w(
                        "GameActivityViewModel",
                        "dismissProgress: Back stack is empty, nothing to pop"
                )
            }
        }

        // Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
        // Clear the fragment reference and flag
        progressFragment = null
        isProgressActive = false

        android.util.Log.d("GameActivityViewModel", "dismissProgress: Completed")
    }

    /** Dismiss the Exit submenu */
    fun dismissExit() {
        android.util.Log.d("GameActivityViewModel", "dismissExit: Starting")

        // IMPORTANT: Since the ExitFragment was added to the back stack,
        // we must use popBackStack() instead of manual remove()
        // This ensures FragmentManager properly manages the hierarchy

        // Check if there's anything in the back stack before trying to remove
        val activity = exitFragment?.activity as? FragmentActivity
        if (activity != null) {
            val fragmentManager = activity.supportFragmentManager
            val backStackCount = fragmentManager.backStackEntryCount

            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissExit: backStackCount = $backStackCount"
            )

            if (backStackCount > 0) {
                // Usar popBackStack para remover o ExitFragment corretamente
                android.util.Log.d(
                        "GameActivityViewModel",
                        "dismissExit: Calling popBackStackImmediate()"
                )
                fragmentManager.popBackStackImmediate()
            } else {
                android.util.Log.w(
                        "GameActivityViewModel",
                        "dismissExit: Back stack is empty, nothing to pop"
                )
            }
        }

        // Clear keyLog to prevent phantom key presses
        controllerInput.clearKeyLog()
        // Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
        // Clear the fragment reference and flag
        exitFragment = null
        isExitActive = false

        android.util.Log.d("GameActivityViewModel", "dismissExit: Completed")
    }

    /** Dismiss ALL menus in cascade order (submenus first, then main menu) */
    fun dismissAllMenus() {
        android.util.Log.d("GameActivityViewModel", "dismissAllMenus: Starting cascade dismissal")

        // Set flag to prevent showing main menu when submenus are dismissed
        isDismissingAllMenus = true

        // Dismiss submenus first (in reverse order of opening)
        if (isExitActive) {
            android.util.Log.d("GameActivityViewModel", "dismissAllMenus: Dismissing Exit submenu")
            dismissExit()
        }
        if (isProgressActive) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissAllMenus: Dismissing Progress submenu"
            )
            dismissProgress()
        }
        if (isSettingsMenuActive) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissAllMenus: Dismissing Settings submenu"
            )
            dismissSettingsMenu()
        }

        // Finally dismiss the main RetroMenu3
        if (isRetroMenu3Open()) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "dismissAllMenus: Dismissing main RetroMenu3"
            )
            dismissRetroMenu3()
        }

        // Restore game speed from sharedpreferences when exiting menu with Start
        restoreGameSpeedFromPreferences()

        // Reset flag after all menus are dismissed
        isDismissingAllMenus = false

        android.util.Log.d("GameActivityViewModel", "dismissAllMenus: Cascade dismissal completed")
    }

    /** Restore game speed from sharedpreferences when exiting menu with Start */
    fun restoreGameSpeedFromPreferences() {
        retroView?.let { retroView ->
            // Get saved game speed from sharedpreferences
            val savedSpeed =
                    sharedPreferences?.getInt(
                            getApplication<Application>().getString(R.string.pref_frame_speed),
                            1
                    )
                            ?: 1

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
        isSettingsMenuActive = true
    }

    /** Register the ProgressFragment when it's created */
    fun registerProgressFragment(fragment: ProgressFragment) {
        progressFragment = fragment
        isProgressActive = true
    }

    /** Register the ExitFragment when it's created */
    fun registerExitFragment(fragment: ExitFragment) {
        exitFragment = fragment
        isExitActive = true
    }

    // Implementation of GameMenuBottomSheet.GameMenuListener interface
    override fun onResetGame() {
        // Legacy method - now using resetGameCentralized() instead
        // Kept for interface compatibility but should not be called
    }

    override fun onSaveState() {
        // Legacy method - now using saveStateCentralized() instead
        // Kept for interface compatibility but should not be called
    }

    override fun onLoadState() {
        // Legacy method - now using loadStateCentralized() instead
        // Kept for interface compatibility but should not be called
    }

    override fun onToggleAudio() {
        retroView?.let { audioController?.toggleAudio(it.view) }
    }

    override fun onFastForward() {
        retroView?.let { speedController?.toggleFastForward(it.view) }
    }

    override fun onExitGame(activity: FragmentActivity) {
        // Show confirmation dialog asking if user wants to save before exiting
        // Create context with the same theme as the menu
        val themedContext =
                android.view.ContextThemeWrapper(activity, R.style.Theme_Revenger_FloatingMenu)

        val dialog =
                AlertDialog.Builder(themedContext)
                        .setTitle(R.string.exit_game_title)
                        .setMessage(R.string.exit_game_message)
                        .setPositiveButton(R.string.exit_game_save_and_exit) {
                                _: android.content.DialogInterface,
                                _: Int ->
                            // Save state and then exit (using centralized method)
                            saveStateCentralized {
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }
                        .setNegativeButton(R.string.exit_game_exit_without_save) {
                                _: android.content.DialogInterface,
                                _: Int ->
                            // Exit without saving
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                        .setNeutralButton(R.string.cancel, null)
                        .create()

        // Apply the same background color as the menu with rounded corners (Material 3 surface
        // color)
        val backgroundColor = android.util.TypedValue()
        themedContext.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface,
                backgroundColor,
                true
        )

        // Create rounded background drawable (same radius as menu: 28dp)
        val cornerRadiusPx = (28 * themedContext.resources.displayMetrics.density).toInt()
        val roundedBackground = android.graphics.drawable.GradientDrawable()
        roundedBackground.setColor(backgroundColor.data)
        roundedBackground.cornerRadius = cornerRadiusPx.toFloat()

        dialog.window?.setBackgroundDrawable(roundedBackground)

        dialog.show()
    }

    override fun getAudioState(): Boolean {
        // If the controller has already been initialized, use it
        audioController?.let {
            return it.getAudioState()
        }

        // If the controller has not been initialized yet, read directly from SharedPreferences
        // This happens when the menu is created before RetroView
        return sharedPreferences?.getBoolean(
                getApplication<Application>().getString(R.string.pref_audio_enabled),
                true
        )
                ?: true
    }

    override fun getFastForwardState(): Boolean {
        // If the controller has already been initialized, use it
        speedController?.let {
            return it.getFastForwardState()
        }

        // If the controller has not been initialized yet, read directly from SharedPreferences
        // This happens when the menu is created before RetroView
        return (sharedPreferences?.getInt(
                getApplication<Application>().getString(R.string.pref_frame_speed),
                1
        )
                ?: 1) > 1
    }

    override fun hasSaveState(): Boolean = retroViewUtils?.hasSaveState() == true

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
                    controllerInput.processGamePadButtonEvent(event.id, event.action)
                }
        rightGamePad =
                GamePad(context, gamePadConfig.right) { event ->
                    controllerInput.processGamePadButtonEvent(event.id, event.action)
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
     * Audio control using modular controller
     * @param enabled true to turn on, false to turn off
     */
    fun setAudioEnabled(enabled: Boolean) {
        retroView?.let { audioController?.setAudioEnabled(it.view, enabled) }
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
        retroView?.let { speedController?.enableFastForward(it.view) }
    }

    /** Clear controller key log (used by RetroMenu3Fragment on destroy) */
    fun clearControllerKeyLog() {
        controllerInput.clearKeyLog()
    }

    /** Check if we are currently dismissing all menus (used by RetroMenu3Fragment) */
    fun isDismissingAllMenus(): Boolean {
        return isDismissingAllMenus
    }

    /** Desativa fast forward usando controller modular */
    fun disableFastForward() {
        retroView?.let { speedController?.setSpeed(it.view, 1) }
    }
}
