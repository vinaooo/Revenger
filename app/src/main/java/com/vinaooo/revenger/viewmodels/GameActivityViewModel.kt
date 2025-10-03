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
import com.vinaooo.revenger.ui.modernmenu.ModernMenuFragment
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application), ModernMenuFragment.ModernMenuListener {

    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    // Modern menu fragment (activated by Android back button)
    private var modernMenuFragment: ModernMenuFragment? = null

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
    }

    /** Configure menu callback with activity reference */
    fun setupMenuCallback(activity: FragmentActivity) {
        controllerInput.menuCallback = {
            // Check if menu is enabled before showing
            if (isMenuEnabled()) {
                showMenu(activity)
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
        // Usar o controller modular para obter estado do áudio
        return audioController?.getAudioState() ?: true
    }

    override fun getFastForwardState(): Boolean {
        // Usar o controller modular para obter estado da velocidade
        return speedController?.getFastForwardState() ?: false
    }

    override fun hasSaveState(): Boolean = retroViewUtils?.hasSaveState() == true

    /**
     * Centralized load state implementation with improved debugging CORREÇÃO: Despausa
     * temporariamente APENAS durante o load, sem enviar sinais ao core
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
     * Centralized save state implementation with improved debugging CORREÇÃO: Removido delay
     * desnecessário que pode causar problemas de timing
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
     * Centralized reset game implementation with improved debugging CORREÇÃO: Garantir que o reset
     * realmente reinicie o jogo do início
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

        // Inicializar controllers com as mesmas SharedPreferences que RetroViewUtils usa
        initializeControllers(activity)

        retroView?.let { retroView ->
            container.addView(retroView.view)
            activity.lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()

            /* CORREÇÃO: NÃO restaurar state automaticamente no primeiro frame
             * O jogo deve começar do zero e o usuário decide quando carregar o save
             * Isso corrige o bug onde Load State não funcionava após restart porque
             * o save já tinha sido carregado automaticamente na inicialização
             */
            retroView.frameRendered.observe(activity) {
                if (it != true) return@observe

                // IMPORTANTE: Apenas inicializar velocidade, SEM carregar save state
                // O save só deve ser carregado quando usuário clicar em "Load State"
                speedController?.initializeSpeedState(retroView.view)
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
        val sharedPreferences = activity.getPreferences(android.content.Context.MODE_PRIVATE)
        audioController = AudioController(activity.applicationContext, sharedPreferences)
        speedController = SpeedController(activity.applicationContext, sharedPreferences)
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

    /** Desativa fast forward usando controller modular */
    fun disableFastForward() {
        retroView?.let { speedController?.disableFastForward(it.view) }
    }
}
