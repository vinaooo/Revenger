package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.R
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private var menuDialog: AlertDialog? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    init {
        controllerInput.menuCallback = { showMenu() }
    }

    /** Create an instance of a menu dialog */
    fun prepareMenu(context: Context) {
        if (menuDialog != null) return

        val menuOnClickListener = MenuOnClickListener()
        menuDialog =
                AlertDialog.Builder(context)
                        .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
                        .create()
    }

    /** Show the menu */
    fun showMenu() {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
            menuDialog?.show()
        }
    }

    /** Dismiss the menu */
    fun dismissMenu() {
        if (menuDialog?.isShowing == true) menuDialog?.dismiss()
    }

    /** Save the state of the emulator */
    fun preserveState() {
        if (retroView?.frameRendered?.value == true)
                retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
    }

    /** Hide the system bars */
    @Suppress("DEPRECATION")
    fun immersive(window: Window) {
        /* Check if the config permits it */
        if (!resources.getBoolean(R.bool.config_fullscreen)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with(window.insetsController!!) {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    /** Hook the RetroView with the GLRetroView instance */
    fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
        retroView = RetroView(activity, viewModelScope)
        retroViewUtils = RetroViewUtils(activity)

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

    /** Subscribe the GamePads to the RetroView - NOVA VERSÃO COM SISTEMA HÍBRIDO */
    fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val context = getApplication<Application>().applicationContext

        try {
            // ETAPA 5: Detecção automática do melhor sistema
            val configId = resources.getString(com.vinaooo.revenger.R.string.config_id)
            val virtualPreferredGames = listOf("sak", "sth", "rrr", "loz")

            if (configId in virtualPreferredGames) {
                android.util.Log.d("GameActivityViewModel", "Usando VirtualJoystick para $configId")
                setupVirtualGamePads(activity, leftContainer, rightContainer, configId)
            } else {
                android.util.Log.d(
                        "GameActivityViewModel",
                        "Usando RadialGamePad tradicional para $configId"
                )
                setupTraditionalGamePads(activity, leftContainer, rightContainer)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                    "GameActivityViewModel",
                    "Erro no sistema híbrido, usando fallback",
                    e
            )
            setupTraditionalGamePads(activity, leftContainer, rightContainer)
        }
    }

    /** Setup Sistema Híbrido: VirtualJoystick (direcional) + RadialGamePad (botões) */
    private fun setupVirtualGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            configId: String
    ) {
        val context = getApplication<Application>().applicationContext

        try {
            // Verificar se deve usar joystick analógico ou D-Pad digital
            val useAnalogStick = resources.getBoolean(R.bool.config_left_analog)
            
            if (useAnalogStick) {
                // MODO ANALÓGICO: CustomJoystickView (cores personalizadas)
                setupAnalogJoystick(activity, leftContainer, rightContainer, configId)
            } else {
                // MODO DIGITAL: RadialGamePad tradicional (D-Pad visual correto)
                setupDigitalDPad(activity, leftContainer, rightContainer)
            }

            android.util.Log.d(
                    "GameActivityViewModel",
                    "Sistema Híbrido configurado - Modo: ${if (useAnalogStick) "ANALÓGICO (CustomJoystick)" else "DIGITAL (RadialGamePad)"}"
            )

            val customJoystick =
                    com.vinaooo.revenger.gamepad.CustomJoystickView(context).apply {
                        applyConfig(config)

                        // CONEXÃO CRÍTICA: Conectar movimento ao RetroView
                        setOnMoveListener(
                                object :
                                        com.vinaooo.revenger.gamepad.CustomJoystickView.OnMoveListener {
                                    override fun onMove(
                                            angle: Int,
                                            strength: Int,
                                            xAxis: Float,
                                            yAxis: Float
                                    ) {
                                        retroView?.let { retroView ->
                                            // Escolher tipo de movimento baseado no config.xml
                                            val motionSource =
                                                    if (useAnalogStick) {
                                                        com.swordfish.libretrodroid.GLRetroView
                                                                .MOTION_SOURCE_ANALOG_LEFT
                                                    } else {
                                                        com.swordfish.libretrodroid.GLRetroView
                                                                .MOTION_SOURCE_DPAD
                                                    }

                                            retroView.view.sendMotionEvent(
                                                    motionSource,
                                                    xAxis,
                                                    yAxis
                                            )
                                        }
                                    }
                                }
                        )
                    }

            leftContainer.addView(customJoystick)

            // PARTE 2: RadialGamePad (botões do lado direito) RESPEITANDO config.xml
            val gamePadConfig = GamePadConfig(context, resources) // Já respeita as configurações
            rightGamePad = GamePad(context, gamePadConfig.right)

            rightGamePad?.let {
                rightContainer.addView(it.pad)
                retroView?.let { retroView ->
                    it.subscribe(activity.lifecycleScope, retroView.view)
                }
            }

            android.util.Log.d(
                    "GameActivityViewModel",
                    "Sistema Híbrido conectado - VirtualJoystick: ${config.name} + RadialGamePad (Analógico: $useAnalogStick)"
            )
        } catch (e: Exception) {
            android.util.Log.e("GameActivityViewModel", "Erro Sistema Híbrido, usando fallback", e)
            setupTraditionalGamePads(activity, leftContainer, rightContainer)
        }
    }

    /** Fallback para sistema tradicional RadialGamePad */
    private fun setupTraditionalGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val context = getApplication<Application>().applicationContext

        val gamePadConfig = GamePadConfig(context, resources)
        leftGamePad = GamePad(context, gamePadConfig.left)
        rightGamePad = GamePad(context, gamePadConfig.right)

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }

        android.util.Log.d(
                "GameActivityViewModel",
                "Sistema tradicional RadialGamePad inicializado"
        )
    }

    /** Hide the on-screen GamePads - VERSÃO SIMPLIFICADA */
    fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val shouldShow = GamePad.shouldShowGamePads(activity)
        val visibility = if (shouldShow) View.VISIBLE else View.GONE

        leftContainer.visibility = visibility
        rightContainer.visibility = visibility

        android.util.Log.d("GameActivityViewModel", "GamePad visibility: $shouldShow")
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

    /** Class to handle the menu dialog actions */
    inner class MenuOnClickListener : DialogInterface.OnClickListener {
        private val context = getApplication<Application>().applicationContext

        val menuOptions =
                arrayOf(
                        context.getString(R.string.menu_reset),
                        context.getString(R.string.menu_save_state),
                        context.getString(R.string.menu_load_state),
                        context.getString(R.string.menu_mute),
                        context.getString(R.string.menu_fast_forward)
                )

        override fun onClick(dialog: DialogInterface?, which: Int) {
            when (menuOptions[which]) {
                context.getString(R.string.menu_reset) -> retroView?.view?.reset()
                context.getString(R.string.menu_save_state) ->
                        retroView?.let { retroViewUtils?.saveState(it) }
                context.getString(R.string.menu_load_state) ->
                        retroView?.let { retroViewUtils?.loadState(it) }
                context.getString(R.string.menu_mute) ->
                        retroView?.let { it.view.audioEnabled = !it.view.audioEnabled }
                context.getString(R.string.menu_fast_forward) ->
                        retroView?.let { retroViewUtils?.fastForward(it) }
            }
        }
    }
}
