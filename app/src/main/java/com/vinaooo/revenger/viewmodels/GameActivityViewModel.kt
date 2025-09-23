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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libretro.frontend.gamepad.padkit.PadKitGamepadSystem
import com.vinaooo.revenger.R
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    // PadKit System - Sistema universal de gamepad
    private var padKitSystem: PadKitGamepadSystem? = null
    private var leftComposeView: ComposeView? = null
    private var rightComposeView: ComposeView? = null

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

    /** Setup PadKit Universal Gamepad System */
    fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val context = getApplication<Application>().applicationContext

        // Inicializar sistema PadKit universal
        padKitSystem = PadKitGamepadSystem(context)

        // Criar ComposeViews para renderizar gamepad PadKit
        leftComposeView =
                ComposeView(context).apply {
                    setViewCompositionStrategy(
                            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        retroView?.let { retroView ->
                            padKitSystem?.CreateUniversalGamepad(
                                    retroView = retroView.view,
                                    inputListener =
                                            object : PadKitGamepadSystem.GamepadInputListener {
                                                override fun onInputReceived() {
                                                    // Inputs são mapeados automaticamente pelo
                                                    // sistema
                                                }

                                                override fun onMenuRequested() {
                                                    showMenu()
                                                }
                                            },
                                    port = 0 // Player 1
                            )
                        }
                    }
                }

        // Por enquanto, usar apenas o lado esquerdo para o gamepad universal
        // O layout PadKit já contém todos os controles necessários
        leftContainer.removeAllViews()
        leftContainer.addView(leftComposeView)

        // Limpar container direito (não usado no sistema universal)
        rightContainer.removeAllViews()
    }

    /** Update PadKit Universal Gamepad visibility */
    fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val visibility = if (GamePad.shouldShowGamePads(activity)) View.VISIBLE else View.GONE

        // Sistema PadKit usa apenas container esquerdo
        leftContainer.visibility = visibility
        // Container direito fica sempre oculto no sistema universal
        rightContainer.visibility = View.GONE
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

    /** PadKit System Management Functions */

    /** Obtém configuração atual do sistema PadKit */
    fun getPadKitConfiguration() = padKitSystem?.getCurrentConfiguration()

    /** Recarrega configuração PadKit (útil se config.xml mudou) */
    fun reloadPadKitConfiguration() = padKitSystem?.reloadConfiguration()

    /** Reseta estado do sistema PadKit */
    fun resetPadKitSystem() = padKitSystem?.reset()

    /** Limpa recursos PadKit na destruição */
    override fun onCleared() {
        super.onCleared()
        padKitSystem = null
        leftComposeView = null
        rightComposeView = null
    }
}
