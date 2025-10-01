package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.util.Log
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
import com.vinaooo.revenger.ui.modernmenu.ModernMenuFragment
import com.vinaooo.revenger.ui.retromenu.RetroMenuFragment
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application), ModernMenuFragment.ModernMenuListener {

    companion object {
        private const val TAG = "GameActivityViewModel"
    }

    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    // Modern menu fragment (activated by Android back button)
    private var modernMenuFragment: ModernMenuFragment? = null

    // Retro menu fragment (activated by gamepad buttons)
    private var retroMenuFragment: RetroMenuFragment? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput(application.applicationContext)

    // Controllers modulares para áudio e velocidade (inicializados no setupRetroView)
    private var audioController: AudioController? = null
    private var speedController: SpeedController? = null

    init {
        // Set the callback to check if SELECT+START combo should work
        controllerInput.shouldHandleSelectStartCombo = { shouldHandleSelectStartCombo() }
        // Set the callbacks to check pause overlay modes
        controllerInput.shouldHandleStartPause = { shouldHandleStartPause() }
        controllerInput.shouldHandleSelectPause = { shouldHandleSelectPause() }
        controllerInput.shouldHandleSelectStartPause = { shouldHandleSelectStartPause() }
    }

    /** Configure menu callback with activity reference */
    fun setupMenuCallback(activity: FragmentActivity) {
        controllerInput.menuCallback = {
            // Check if menu is enabled before showing
            if (isMenuEnabled()) {
                showMenu(activity)
            }
        }

        // Configure navigation callback for retro menu
        controllerInput.retroMenuNavigationCallback = { keyCode ->
            retroMenuFragment?.handleNavigationInput(keyCode) ?: false
        }

        // Configure function to check if retro menu is visible
        controllerInput.isRetroMenuVisible = { isPauseOverlayVisible() }

        // Configure pause overlay callbacks based on mode
        controllerInput.pauseCallback = {
            // START button (mode 1)
            if (isPauseOverlayEnabled()) {
                showPauseOverlay(activity)
            }
        }

        controllerInput.selectPauseCallback = {
            // SELECT button (mode 2)
            if (isPauseOverlayEnabled()) {
                showPauseOverlay(activity)
            }
        }

        controllerInput.selectStartPauseCallback = {
            // SELECT + START together (mode 3)
            if (isPauseOverlayEnabled()) {
                showPauseOverlay(activity)
            }
        }
    }

    /** Create an instance of the modern menu overlay (activated by back button) */
    fun prepareMenu(activity: ComponentActivity) {
        if (modernMenuFragment != null) return

        val fragmentActivity = activity as? FragmentActivity ?: return
        setupMenuCallback(fragmentActivity)

        modernMenuFragment =
                ModernMenuFragment.newInstance().apply {
                    setMenuListener(this@GameActivityViewModel)
                }
    }

    /** Create an instance of the retro menu (activated by gamepad buttons) */
    fun preparePauseOverlay(activity: ComponentActivity) {
        if (retroMenuFragment != null) return

        retroMenuFragment =
                RetroMenuFragment.newInstance().apply { retroMenuMode = getPauseOverlayMode() }
    }

    /** Show the retro menu overlay */
    fun showPauseOverlay(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            // Preserve emulator state (same as Modern Menu does)
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // Show retro menu overlay
            retroMenuFragment?.let { overlay ->
                if (!overlay.isAdded) {
                    // Set callbacks to handle actions
                    overlay.onDismissCallback = { dismissPauseOverlay() }
                    // onResetGameCallback removed - now using centralized resetGameCentralized()
                    // All callbacks removed - now using centralized methods:
                    // - continueGameCentralized()
                    // - resetGameCentralized()
                    // - loadStateCentralized()
                    // - saveStateCentralized()
                    // - hasSaveState() from ViewModel interface

                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(
                                    android.R.id.content,
                                    overlay,
                                    RetroMenuFragment::class.java.simpleName
                            )
                            .commit()
                }
            }
        }
    }

    /** Dismiss the pause overlay */
    fun dismissPauseOverlay() {
        Log.d(TAG, "Dismissing pause overlay")

        // Send appropriate signal(s) to unpause the game before dismissing overlay
        retroView?.view?.let { view ->
            val mode = getPauseOverlayMode()
            Log.d(TAG, "Sending unpause signal(s) for mode: $mode")

            when (mode) {
                1 -> { // START button
                    Log.d(TAG, "Sending START signal to unpause game")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, 0)
                }
                2 -> { // SELECT button
                    Log.d(TAG, "Sending SELECT signal to unpause game")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                }
                3 -> { // SELECT + START together
                    Log.d(TAG, "Sending SELECT+START signals to unpause game")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, 0)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                }
            }

            Thread.sleep(100) // Additional delay before dismissing overlay
            Log.d(TAG, "Unpause signals sent, now dismissing overlay")
        }

        // Remove the retro menu fragment
        retroMenuFragment?.let { overlay ->
            overlay.parentFragmentManager.beginTransaction().remove(overlay).commit()
            Log.d(TAG, "Retro menu fragment removed")
        }
        Log.d(TAG, "Retro menu dismissed")
    }

    /** Check if the retro menu is currently visible */
    fun isPauseOverlayVisible(): Boolean {
        return retroMenuFragment?.isAdded == true
    }

    /** Show the fullscreen game menu */
    fun showMenu(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // Show new modern menu
            modernMenuFragment?.let { menu ->
                if (!menu.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(
                                    android.R.id.content,
                                    menu,
                                    ModernMenuFragment::class.java.simpleName
                            )
                            .commit()
                }
            }
        }
    }

    /** Dismiss the modern menu */
    fun dismissMenu() {
        modernMenuFragment?.dismissMenuPublic()
    }

    /** Check if the modern menu is currently open */
    fun isMenuOpen(): Boolean {
        return modernMenuFragment?.isAdded == true
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
        retroView?.let {
            // Usar o controller modular para áudio
            audioController?.toggleAudio(it.view)
            Log.d(TAG, "Audio toggled using AudioController")
        }
    }

    override fun onFastForward() {
        retroView?.let {
            // Usar o controller modular para velocidade
            speedController?.toggleFastForward(it.view)
            Log.d(TAG, "Fast forward toggled using SpeedController")
        }
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
        // Usar o controller modular para obter estado do áudio
        return audioController?.getAudioState() ?: true
    }

    override fun getFastForwardState(): Boolean {
        // Usar o controller modular para obter estado da velocidade
        return speedController?.getFastForwardState() ?: false
    }

    override fun hasSaveState(): Boolean {
        return retroViewUtils?.hasSaveState() ?: false
    }

    /**
     * Centralized load state implementation based on Modern Menu behavior This method includes the
     * necessary 150ms delay that matches animateMenuOut() timing Both Modern Menu and Retro Menu
     * should use this method for consistency
     */
    fun loadStateCentralized(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "Central load state called")

        // Check if save state exists first (same as Modern Menu)
        if (hasSaveState()) {
            Log.d(TAG, "Save state exists, proceeding with load")

            // Apply the same 150ms delay as Modern Menu's animateMenuOut()
            android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(
                            {
                                retroView?.let { retroView ->
                                    if (retroViewUtils != null) {
                                        Log.d(TAG, "Loading state from storage (centralized)")
                                        retroViewUtils?.loadState(retroView)
                                        Log.d(TAG, "Load state executed successfully (centralized)")
                                    } else {
                                        Log.e(TAG, "retroViewUtils is null - cannot load state!")
                                    }
                                }
                                        ?: Log.e(TAG, "retroView is null - cannot load state!")

                                onComplete?.invoke()
                            },
                            150
                    )
        } else {
            Log.d(TAG, "No save state available")
            onComplete?.invoke()
        }
    }

    /**
     * Centralized save state implementation based on Modern Menu behavior This method includes the
     * necessary 150ms delay that matches animateMenuOut() timing Both Modern Menu and Retro Menu
     * should use this method for consistency
     */
    fun saveStateCentralized(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "Central save state called")

        // Apply the same 150ms delay as Modern Menu's animateMenuOut()
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            retroView?.let { retroView ->
                                if (retroViewUtils != null) {
                                    Log.d(TAG, "Saving state to storage (centralized)")
                                    retroViewUtils?.saveState(retroView)
                                    Log.d(TAG, "Save state executed successfully (centralized)")
                                    Log.d(
                                            TAG,
                                            "Save state now exists: ${retroViewUtils?.hasSaveState()}"
                                    )
                                } else {
                                    Log.e(TAG, "retroViewUtils is null - cannot save state!")
                                }
                            }
                                    ?: Log.e(TAG, "retroView is null - cannot save state!")

                            onComplete?.invoke()
                        },
                        150
                )
    }

    /**
     * Centralized reset game implementation Both Modern Menu and Retro Menu should use this method
     * for consistency
     */
    fun resetGameCentralized(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "Central reset game called")

        retroView?.view?.let { view ->
            Log.d(TAG, "Resetting game (centralized)")
            view.reset()
            Log.d(TAG, "Game reset executed successfully (centralized)")
        }
                ?: Log.e(TAG, "retroView.view is null - cannot reset game!")

        onComplete?.invoke()
    }

    /**
     * Centralized continue game implementation Handles unpause signals based on configured mode and
     * dismisses overlay Currently used by Retro Menu but modularized for future reuse
     */
    fun continueGameCentralized(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "Central continue game called")

        // Send appropriate unpause signal(s) based on configured mode
        retroView?.view?.let { view ->
            val mode = getPauseOverlayMode()
            Log.d(TAG, "Sending unpause signal(s) for mode: $mode (centralized)")

            when (mode) {
                1 -> { // START button
                    Log.d(TAG, "Sending START signal to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, 0)
                }
                2 -> { // SELECT button
                    Log.d(TAG, "Sending SELECT signal to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                }
                3 -> { // SELECT + START together
                    Log.d(TAG, "Sending SELECT+START signals to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    Thread.sleep(200)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, 0)
                    view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                }
            }

            Thread.sleep(100) // Additional delay before completing
            Log.d(TAG, "Unpause signals sent successfully (centralized)")
        }
                ?: Log.e(TAG, "retroView.view is null - cannot send unpause signals!")

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
        
        // Inicializar controllers com as mesmas SharedPreferences que RetroViewUtils usa
        initializeControllers(activity)

        retroView?.let { retroView ->
            container.addView(retroView.view)
            activity.lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()

            /* Restore state after first frame loaded */
            retroView.frameRendered.observe(activity) {
                if (it != true) return@observe

                retroViewUtils?.restoreEmulatorState(retroView)
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
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
    }

    /** Process a motion event and return the result */
    fun processMotionEvent(event: MotionEvent): Boolean? {
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

    /** Check if pause overlay is enabled based on config_pause_overlay */
    fun isPauseOverlayEnabled(): Boolean {
        return getPauseOverlayMode() != 0
    }

    /** Get the pause overlay mode (0=disabled, 1=START, 2=SELECT, 3=SELECT+START) */
    fun getPauseOverlayMode(): Int {
        return resources.getInteger(R.integer.config_pause_overlay)
    }

    /** Check if START alone should trigger pause overlay */
    fun shouldHandleStartPause(): Boolean {
        val mode = getPauseOverlayMode()
        return mode == 1 // START button alone
    }

    /** Check if SELECT alone should trigger pause overlay */
    fun shouldHandleSelectPause(): Boolean {
        val mode = getPauseOverlayMode()
        return mode == 2 // SELECT button alone
    }

    /** Check if SELECT+START combo should trigger pause overlay */
    fun shouldHandleSelectStartPause(): Boolean {
        val mode = getPauseOverlayMode()
        return mode == 3 // SELECT + START together
    }

    /** 
     * Inicializa os controllers modulares com as mesmas SharedPreferences do RetroViewUtils
     * Garante compatibilidade com o sistema existente
     */
    private fun initializeControllers(activity: Activity) {
        val sharedPreferences = activity.getPreferences(android.content.Context.MODE_PRIVATE)
        audioController = AudioController(activity.applicationContext, sharedPreferences)
        speedController = SpeedController(activity.applicationContext, sharedPreferences)
        Log.d(TAG, "Controllers modulares inicializados com compatibilidade RetroViewUtils")
    }

    // MÉTODOS PÚBLICOS PARA ACESSO AOS CONTROLLERS MODULARES

    /**
     * Obtém referência ao AudioController para uso em outros componentes
     * Permite acesso modular às funcionalidades de áudio
     */
    fun getAudioController(): AudioController? {
        return audioController
    }

    /**
     * Obtém referência ao SpeedController para uso em outros componentes
     * Permite acesso modular às funcionalidades de velocidade
     */
    fun getSpeedController(): SpeedController? {
        return speedController
    }

    /**
     * Controle de áudio usando controller modular
     * @param enabled true para ligar, false para desligar
     */
    fun setAudioEnabled(enabled: Boolean) {
        retroView?.let {
            audioController?.setAudioEnabled(it.view, enabled)
            Log.d(TAG, "Audio set to ${if (enabled) "enabled" else "disabled"} via AudioController")
        }
    }

    /**
     * Controle de velocidade usando controller modular
     * @param speed velocidade desejada (1 = normal, > 1 = fast forward)
     */
    fun setGameSpeed(speed: Int) {
        retroView?.let {
            speedController?.setSpeed(it.view, speed)
            Log.d(TAG, "Game speed set to ${speed}x via SpeedController")
        }
    }

    /**
     * Ativa fast forward usando controller modular
     */
    fun enableFastForward() {
        retroView?.let {
            speedController?.enableFastForward(it.view)
            Log.d(TAG, "Fast forward enabled via SpeedController")
        }
    }

    /**
     * Desativa fast forward usando controller modular
     */
    fun disableFastForward() {
        retroView?.let {
            speedController?.disableFastForward(it.view)
            Log.d(TAG, "Fast forward disabled via SpeedController")
        }
    }
}
