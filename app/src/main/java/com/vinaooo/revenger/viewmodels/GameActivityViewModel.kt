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
import com.vinaooo.revenger.retromenu2.RetroMenu2Fragment
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

    // RetroMenu2 fragment (new menu system)
    private var retroMenu2Fragment: RetroMenu2Fragment? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput(application.applicationContext)

    // Controllers for modular audio and speed control
    private var audioController: AudioController? = null
    private var speedController: SpeedController? = null

    // Flag to prevent tempState from overwriting a manual Load State
    private var skipNextTempStateLoad = false

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
                Log.d(TAG, "pauseCallback TRIGGERED - calling showPauseOverlay (START)")
                showPauseOverlay(activity)
            }
        }

        controllerInput.selectPauseCallback = {
            // SELECT button (mode 2)
            if (isPauseOverlayEnabled()) {
                Log.d(TAG, "selectPauseCallback TRIGGERED - calling showPauseOverlay (SELECT)")
                showPauseOverlay(activity)
            }
        }

        controllerInput.selectStartPauseCallback = {
            // SELECT + START together (mode 3 or RetroMenu2)
            if (isRetroMenu2Enabled()) {
                // RetroMenu2 usa SELECT+START como trigger exclusivo
                Log.d(TAG, "selectStartPauseCallback TRIGGERED - opening RetroMenu2")
                showRetroMenu2(activity)
            } else if (isPauseOverlayEnabled()) {
                // RetroMenu1 original
                Log.d(
                        TAG,
                        "selectStartPauseCallback TRIGGERED - calling showPauseOverlay (SELECT+START)"
                )
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

    /** Create an instance of RetroMenu2 (new menu system) */
    fun prepareRetroMenu2(activity: ComponentActivity) {
        if (retroMenu2Fragment != null) return

        retroMenu2Fragment = RetroMenu2Fragment.newInstance()
        Log.d(TAG, "RetroMenu2 preparado")
    }

    /** Show RetroMenu2 overlay */
    fun showRetroMenu2(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            // RetroMenu2 irá pausar o emulador no onResume()
            // usando frameSpeed = 0

            retroMenu2Fragment?.let { menu ->
                if (!menu.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(
                                    android.R.id.content,
                                    menu,
                                    RetroMenu2Fragment::class.java.simpleName
                            )
                            .commit()

                    Log.d(TAG, "RetroMenu2 exibido")
                }
            }
        }
    }

    /** Show the retro menu overlay */
    fun showPauseOverlay(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            // CRITICAL: Capture currently pressed keys BEFORE pausing
            // Their ACTION_UP will be blocked after menu closes to prevent partial signals
            controllerInput.captureKeysOnMenuOpen()

            // PAUSE emulation using LibretroDroid's native frameSpeed API
            retroView?.view?.frameSpeed = 0
            Log.d(TAG, "🛑 Emulator PAUSED using frameSpeed = 0 (native API)")

            // Preserve emulator state (audio, speed settings)
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // Show retro menu overlay
            retroMenuFragment?.let { overlay ->
                if (!overlay.isAdded) {
                    // Set callback to dismiss overlay and resume emulation
                    overlay.onDismissCallback = { dismissPauseOverlay() }
                    // Using centralized methods:
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
        Log.d(TAG, "Dismissing pause overlay and resuming emulation")

        retroView?.let { retroView ->
            // Restore emulator state (audio, speed settings)
            // Pass skipTempStateLoad flag to avoid overwriting manual Load State
            retroViewUtils?.restoreEmulatorState(retroView, skipNextTempStateLoad)
            Log.d(TAG, "Emulator settings restored (skipTempStateLoad=$skipNextTempStateLoad)")

            // Reset flag after use
            skipNextTempStateLoad = false

            // RESUME emulation using LibretroDroid's native frameSpeed API
            retroView.view.frameSpeed = 1
            Log.d(TAG, "▶️ Emulator RESUMED using frameSpeed = 1 (native API)")
        }

        // Remove the retro menu fragment
        retroMenuFragment?.let { overlay ->
            overlay.parentFragmentManager.beginTransaction().remove(overlay).commit()
            Log.d(TAG, "Retro menu fragment removed")
        }
        Log.d(TAG, "Retro menu dismissed")

        // CRITICAL: Clear blocked keys after a delay
        // This gives time for any pending ACTION_UP events to be blocked first
        controllerInput.clearBlockedKeysDelayed()
    }

    /** Check if the retro menu is currently visible */
    fun isPauseOverlayVisible(): Boolean {
        return retroMenuFragment?.isAdded == true
    }

    /** Show the fullscreen game menu */
    fun showMenu(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            // CRITICAL: Capture currently pressed keys BEFORE showing menu
            controllerInput.captureKeysOnMenuOpen()

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
        // CRITICAL: Clear blocked keys after menu dismissal
        controllerInput.clearBlockedKeysDelayed()
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
     * Centralized load state implementation with improved debugging CORREÇÃO: Despausa
     * temporariamente APENAS durante o load, sem enviar sinais ao core
     */
    fun loadStateCentralized(onComplete: (() -> Unit)? = null) {
        Log.w(TAG, "🔵 Central load state called")

        // CORREÇÃO: Verificar se o jogo está completamente inicializado
        if (retroView?.frameRendered?.value != true) {
            Log.w(
                    TAG,
                    "⏳ Game is still initializing - Load State blocked to prevent tempState override"
            )
            Log.w(TAG, "💡 Try Load State again after the game finishes loading")
            onComplete?.invoke()
            return
        }

        // Check if save state exists first
        if (hasSaveState()) {
            Log.w(TAG, "💾 Save state exists, proceeding with load")

            retroView?.let { retroView ->
                if (retroViewUtils != null) {
                    Log.w(TAG, "📂 Loading state from storage (centralized)")

                    // DIAGNÓSTICO: Verificar se arquivo de save state existe
                    Log.w(TAG, "🔍 DIAGNÓSTICO: hasSaveState = ${retroViewUtils?.hasSaveState()}")

                    // CORREÇÃO CRÍTICA: Salvar frameSpeed atual e temporariamente forçar = 1
                    // para o load funcionar, mas NÃO enviar keyevents que ativariam pause do core
                    val savedFrameSpeed = retroView.view.frameSpeed
                    Log.w(TAG, "⚙️ Saving current frameSpeed: $savedFrameSpeed")

                    // Temporariamente despausar SILENCIOSAMENTE (apenas frameSpeed, sem keyevents)
                    retroView.view.frameSpeed = 1
                    Log.w(
                            TAG,
                            "⏸️ Temporarily set frameSpeed = 1 for load (silent, no core signals)"
                    )

                    // Executar load state COM emulador rodando
                    retroViewUtils?.loadState(retroView)

                    // Restaurar frameSpeed pausado imediatamente após load
                    retroView.view.frameSpeed = savedFrameSpeed
                    Log.w(TAG, "⏸️ Restored frameSpeed to $savedFrameSpeed after load")

                    // CORREÇÃO CRÍTICA: Ativar flag para evitar que tempState sobrescreva o load
                    skipNextTempStateLoad = true
                    Log.w(
                            TAG,
                            "🚩 Flag set: skipNextTempStateLoad = true (prevent tempState override)"
                    )

                    Log.w(TAG, "✅ Load state executed successfully (centralized)")
                    Log.w(TAG, "🎮 State loaded, dismissPauseOverlay() will resume to frameSpeed=1")
                } else {
                    Log.e(TAG, "❌ retroViewUtils is null - cannot load state!")
                }
            }
                    ?: Log.e(TAG, "❌ retroView is null - cannot load state!")
        } else {
            Log.w(TAG, "❌ No save state available")
        }

        onComplete?.invoke()
    }

    /**
     * Centralized save state implementation with improved debugging CORREÇÃO: Removido delay
     * desnecessário que pode causar problemas de timing
     */
    fun saveStateCentralized(onComplete: (() -> Unit)? = null) {
        Log.w(TAG, "🔵 Central save state called")

        retroView?.let { retroView ->
            if (retroViewUtils != null) {
                Log.w(TAG, "💾 Saving current state to storage (centralized)")

                // CORREÇÃO: Executar save imediatamente sem delay artificial
                retroViewUtils?.saveState(retroView)

                Log.w(TAG, "✅ Save state executed successfully (centralized)")
                Log.w(TAG, "📁 Save state now exists: ${retroViewUtils?.hasSaveState()}")
            } else {
                Log.e(TAG, "❌ retroViewUtils is null - cannot save state!")
            }
        }
                ?: Log.e(TAG, "❌ retroView is null - cannot save state!")

        onComplete?.invoke()
    }

    /**
     * Centralized reset game implementation with improved debugging CORREÇÃO: Garantir que o reset
     * realmente reinicie o jogo do início
     */
    fun resetGameCentralized(onComplete: (() -> Unit)? = null) {
        Log.w(TAG, "🔵 Central reset game called")

        retroView?.view?.let { view ->
            Log.w(TAG, "🔄 Resetting game to beginning (centralized)")

            // CORREÇÃO: Usar reset() do LibRetroDroid que deve reiniciar o jogo
            view.reset()

            Log.w(TAG, "✅ Game reset executed successfully (centralized)")
            Log.w(TAG, "🎮 Game should now be at beginning and running automatically")
        }
                ?: Log.e(TAG, "❌ retroView.view is null - cannot reset game!")

        onComplete?.invoke()
    }

    /**
     * Centralized continue game implementation Handles unpause signals based on configured mode and
     * dismisses overlay Currently used by Retro Menu but modularized for future reuse
     *
     * @param sendUnpauseSignal If true, sends the configured unpause signal (START/SELECT/combo).
     * ```
     *                          If false, just calls onComplete without sending signals.
     * ```
     */
    fun continueGameCentralized(
            sendUnpauseSignal: Boolean = true,
            onComplete: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Central continue game called - sendUnpauseSignal=$sendUnpauseSignal")

        if (!sendUnpauseSignal) {
            Log.d(TAG, "Skipping unpause signal - will be sent by dismissPauseOverlay()")
            onComplete?.invoke()
            return
        }

        // Send appropriate unpause signal(s) based on configured mode
        retroView?.view?.let { view ->
            val mode = getPauseOverlayMode()
            Log.d(TAG, "Sending unpause signal(s) for mode: $mode (centralized)")

            val handler = android.os.Handler(android.os.Looper.getMainLooper())

            when (mode) {
                1 -> { // START button
                    Log.d(TAG, "Sending START signal to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    handler.postDelayed(
                            {
                                view.sendKeyEvent(
                                        KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_BUTTON_START,
                                        0
                                )
                                // Additional delay before completing
                                handler.postDelayed(
                                        {
                                            Log.d(
                                                    TAG,
                                                    "Unpause signals sent successfully (centralized)"
                                            )
                                            onComplete?.invoke()
                                        },
                                        100
                                )
                            },
                            200
                    )
                }
                2 -> { // SELECT button
                    Log.d(TAG, "Sending SELECT signal to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    handler.postDelayed(
                            {
                                view.sendKeyEvent(
                                        KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_BUTTON_SELECT,
                                        0
                                )
                                // Additional delay before completing
                                handler.postDelayed(
                                        {
                                            Log.d(
                                                    TAG,
                                                    "Unpause signals sent successfully (centralized)"
                                            )
                                            onComplete?.invoke()
                                        },
                                        100
                                )
                            },
                            200
                    )
                }
                3 -> { // SELECT + START together
                    Log.d(TAG, "Sending SELECT+START signals to unpause game (centralized)")
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT, 0)
                    view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                    handler.postDelayed(
                            {
                                view.sendKeyEvent(
                                        KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_BUTTON_START,
                                        0
                                )
                                view.sendKeyEvent(
                                        KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_BUTTON_SELECT,
                                        0
                                )
                                // Additional delay before completing
                                handler.postDelayed(
                                        {
                                            Log.d(
                                                    TAG,
                                                    "Unpause signals sent successfully (centralized)"
                                            )
                                            onComplete?.invoke()
                                        },
                                        100
                                )
                            },
                            200
                    )
                }
                else -> {
                    Log.w(
                            TAG,
                            "Invalid pause overlay mode: $mode - completing without unpause signal"
                    )
                    onComplete?.invoke()
                }
            }
        }
                ?: run {
                    Log.e(TAG, "retroView.view is null - cannot send unpause signals!")
                    onComplete?.invoke()
                }
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
        // PRIORITY: Se RetroMenu2 está visível, encaminhar para ele
        retroMenu2Fragment?.let { menu ->
            if (menu.isAdded && menu.isVisible) {
                val handled = menu.handleKeyEvent(keyCode, event)
                if (handled) {
                    Log.d(TAG, "RetroMenu2 consumiu KeyEvent: $keyCode")
                    return true
                }
            }
        }
        
        // Processar normalmente via ControllerInput
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
    }

    /** Process a motion event and return the result */
    fun processMotionEvent(event: MotionEvent): Boolean? {
        // PRIORITY: Se RetroMenu2 está visível, encaminhar para ele
        retroMenu2Fragment?.let { menu ->
            if (menu.isAdded && menu.isVisible) {
                val handled = menu.handleMotionEvent(event)
                if (handled) {
                    Log.d(TAG, "RetroMenu2 consumiu MotionEvent")
                    return true
                }
            }
        }
        
        // Processar normalmente via ControllerInput
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

    /** Check if RetroMenu2 is enabled (new menu system) */
    fun isRetroMenu2Enabled(): Boolean {
        return resources.getBoolean(R.bool.config_use_retromenu2)
    }

    /** Get the pause overlay mode (0=disabled, 1=START, 2=SELECT, 3=SELECT+START) */
    fun getPauseOverlayMode(): Int {
        return resources.getInteger(R.integer.config_pause_overlay)
    }

    /** Check if START alone should trigger pause overlay */
    fun shouldHandleStartPause(): Boolean {
        // RetroMenu2 NÃO usa START sozinho (apenas SELECT+START)
        if (isRetroMenu2Enabled()) {
            return false
        }
        
        // RetroMenu1 original: só se config_pause_overlay == 1
        val mode = getPauseOverlayMode()
        return mode == 1 // START button alone
    }

    /** Check if SELECT alone should trigger pause overlay */
    fun shouldHandleSelectPause(): Boolean {
        // RetroMenu2 NÃO usa SELECT sozinho (apenas SELECT+START)
        if (isRetroMenu2Enabled()) {
            return false
        }
        
        // RetroMenu1 original: só se config_pause_overlay == 2
        val mode = getPauseOverlayMode()
        return mode == 2 // SELECT button alone
    }

    /** Check if SELECT+START combo should trigger pause overlay */
    fun shouldHandleSelectStartPause(): Boolean {
        // RetroMenu2 SEMPRE usa SELECT+START, independente do config_pause_overlay
        if (isRetroMenu2Enabled()) {
            return true
        }
        
        // RetroMenu1 original: só se config_pause_overlay == 3
        val mode = getPauseOverlayMode()
        return mode == 3 // SELECT + START together
    }

    /**
     * Inicializa os controllers modulares com as mesmas SharedPreferences do RetroViewUtils Garante
     * compatibilidade com o sistema existente
     */
    private fun initializeControllers(activity: Activity) {
        val sharedPreferences = activity.getPreferences(android.content.Context.MODE_PRIVATE)
        audioController = AudioController(activity.applicationContext, sharedPreferences)
        speedController = SpeedController(activity.applicationContext, sharedPreferences)
        Log.d(TAG, "Controllers modulares inicializados com compatibilidade RetroViewUtils")
    }

    // MÉTODOS PÚBLICOS PARA ACESSO AOS CONTROLLERS MODULARES

    /**
     * Obtém referência ao AudioController para uso em outros componentes Permite acesso modular às
     * funcionalidades de áudio
     */
    fun getAudioController(): AudioController? {
        return audioController
    }

    /**
     * Obtém referência ao SpeedController para uso em outros componentes Permite acesso modular às
     * funcionalidades de velocidade
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

    /** Ativa fast forward usando controller modular */
    fun enableFastForward() {
        retroView?.let {
            speedController?.enableFastForward(it.view)
            Log.d(TAG, "Fast forward enabled via SpeedController")
        }
    }

    /** Desativa fast forward usando controller modular */
    fun disableFastForward() {
        retroView?.let {
            speedController?.disableFastForward(it.view)
            Log.d(TAG, "Fast forward disabled via SpeedController")
        }
    }

    // ============================================================
    // RETROMENU2 SUPPORT METHODS
    // ============================================================

    /** Pausa o emulador usando frameSpeed = 0 (RetroMenu2) */
    fun pauseEmulator() {
        retroView?.view?.frameSpeed = 0
        Log.d(TAG, "🛑 RetroMenu2: Emulator PAUSED (frameSpeed = 0)")
    }

    /** Retoma o emulador usando frameSpeed = 1 (RetroMenu2) */
    fun resumeEmulator() {
        retroView?.view?.frameSpeed = 1
        Log.d(TAG, "▶️ RetroMenu2: Emulator RESUMED (frameSpeed = 1)")
    }

    /** 
     * Limpa o keyLog do ControllerInput para evitar detecção de combos após fechar menu.
     * CRÍTICO: Previne que SELECT+START reabre menu imediatamente após fechar.
     */
    fun clearInputKeyLog() {
        controllerInput.clearKeyLog()
        Log.d(TAG, "🧹 RetroMenu2: Input keyLog cleared")
    }

    /** Reinicia o jogo usando reset() (RetroMenu2) */
    fun resetGame() {
        resetGameCentralized()
    }

    /** Salva o estado do jogo (RetroMenu2) */
    fun saveGameState() {
        saveStateCentralized()
    }

    /** Carrega o estado do jogo (RetroMenu2) */
    fun loadGameState() {
        loadStateCentralized()
    }
}
