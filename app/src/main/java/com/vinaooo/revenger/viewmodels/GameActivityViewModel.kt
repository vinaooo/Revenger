package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
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
import com.vinaooo.revenger.ui.CustomGameMenuDialog
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GameActivityViewModel"
    }
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private var menuDialog: AlertDialog? = null
    private var customMenuDialog: CustomGameMenuDialog? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    // Sistema de detecção de combo compartilhado entre os gamepads
    private val globalPressedButtons = mutableSetOf<Int>()
    private val MENU_COMBO = setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START)

    init {
        controllerInput.menuCallback = {
            if (isComboMenuEnabled()) {
                showMenu()
            }
        }
    }

    /** Verifica se o menu via combo (SELECT+START) está habilitado */
    private fun isComboMenuEnabled(): Boolean {
        val menuMode = resources.getInteger(R.integer.config_menu_mode)
        return menuMode == 2 || menuMode == 3 // Modo 2 ou 3 permitem combo
    }

    /** Verifica se o menu via botão voltar está habilitado */
    fun isBackButtonMenuEnabled(): Boolean {
        val menuMode = resources.getInteger(R.integer.config_menu_mode)
        val enabled = menuMode == 1 || menuMode == 3 // Modo 1 ou 3 permitem botão voltar
        Log.d(TAG, "isBackButtonMenuEnabled() - menuMode: $menuMode, enabled: $enabled")
        return enabled
    }

    /** Método para receber eventos de botões dos gamepads e detectar combo global */
    fun onGamePadButtonEvent(buttonId: Int, action: Int) {
        android.util.Log.d("ViewModel", "GamePad button event - ID: $buttonId, Action: $action")
        android.util.Log.d("ViewModel", "Global pressed buttons before: $globalPressedButtons")

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                globalPressedButtons.add(buttonId)
                Log.d(TAG, "Button DOWN - Added $buttonId, global pressed: $globalPressedButtons")

                // Verificar se o combo do menu foi acionado
                val hasCombo = globalPressedButtons.containsAll(MENU_COMBO)
                val comboEnabled = isComboMenuEnabled()
                Log.d(
                        TAG,
                        "Combo check - hasCombo: $hasCombo, comboEnabled: $comboEnabled, MENU_COMBO: $MENU_COMBO"
                )

                if (hasCombo && comboEnabled) {
                    Log.i(TAG, "🎮 GLOBAL MENU COMBO DETECTED! Calling showMenu()")
                    showMenu()
                }
            }
            KeyEvent.ACTION_UP -> {
                globalPressedButtons.remove(buttonId)
                Log.d(TAG, "Button UP - Removed $buttonId, global pressed: $globalPressedButtons")
            }
        }
    }

    /** Create an instance of a menu dialog */
    fun prepareMenu(context: Context) {
        if (menuDialog != null) return

        // Criar MenuActions para integração com CustomGameMenuDialog
        val menuActions =
                object : CustomGameMenuDialog.MenuActions {
                    override fun onReset() {
                        retroView?.view?.reset()
                    }

                    override fun onSaveState() {
                        retroView?.let { retroViewUtils?.saveState(it) }
                    }

                    override fun onLoadState() {
                        retroView?.let { retroViewUtils?.loadState(it) }
                    }

                    override fun onMute() {
                        retroView?.let { it.view.audioEnabled = !it.view.audioEnabled }
                    }

                    override fun onFastForward() {
                        retroView?.let { retroViewUtils?.fastForward(it) }
                    }

                    override fun onClose() {
                        dismissMenu()
                    }
                }

        // Armazenar referência para o Material You dialog
        customMenuDialog = CustomGameMenuDialog(context, menuActions)

        // Manter compatibilidade com código existente através de AlertDialog fake
        menuDialog = AlertDialog.Builder(context).create()
    }

    /** Show the menu */
    fun showMenu() {
        Log.d(TAG, "showMenu() chamado")

        val frameRendered = retroView?.frameRendered?.value == true
        Log.d(TAG, "Frame rendered status: $frameRendered")
        Log.d(TAG, "retroView exists: ${retroView != null}")
        Log.d(TAG, "frameRendered LiveData: ${retroView?.frameRendered?.value}")

        if (frameRendered) {
            Log.d(TAG, "Frame renderizado, prosseguindo com menu")
            retroView?.let {
                Log.d(TAG, "Preservando estado do emulador")
                retroViewUtils?.preserveEmulatorState(it)
            }

            val audioEnabled = retroView?.view?.audioEnabled == true
            Log.d(TAG, "Audio enabled: $audioEnabled")

            // Mostrar o novo CustomGameMenuDialog Material You
            Log.d(TAG, "Chamando customMenuDialog.show()")
            customMenuDialog?.show(audioEnabled)
        } else {
            Log.w(
                    TAG,
                    "⚠️ Frame não renderizado ainda, mas vou mostrar menu mesmo assim para debug"
            )

            // TEMPORÁRIO: Permitir menu mesmo sem frame renderizado para debug
            val audioEnabled = retroView?.view?.audioEnabled ?: false
            Log.d(TAG, "Audio enabled (default): $audioEnabled")

            Log.d(TAG, "Chamando customMenuDialog.show() - modo debug")
            customMenuDialog?.show(audioEnabled)
        }
    }

    /** Dismiss the menu */
    fun dismissMenu() {
        Log.d(TAG, "dismissMenu() chamado")
        customMenuDialog?.dismiss()
        if (menuDialog?.isShowing == true) {
            Log.d(TAG, "Dismissing legacy AlertDialog")
            menuDialog?.dismiss()
        }
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
        Log.i(TAG, "🚀 SETTING UP RetroView...")

        try {
            Log.d(TAG, "🔨 Creating RetroView instance...")
            retroView = RetroView(activity, viewModelScope)
            Log.d(TAG, "✅ RetroView instance created")

            Log.d(TAG, "🛠️ Creating RetroViewUtils...")
            retroViewUtils = RetroViewUtils(activity)
            Log.d(TAG, "✅ RetroViewUtils created")

            retroView?.let { retroView ->
                Log.d(TAG, "📺 Adding RetroView to container...")
                container.addView(retroView.view)
                Log.d(TAG, "✅ RetroView added to container")

                Log.d(TAG, "🔄 Adding lifecycle observer...")
                activity.lifecycle.addObserver(retroView.view)
                Log.d(TAG, "✅ Lifecycle observer added")

                Log.d(TAG, "🎬 Registering frame rendered listener...")
                retroView.registerFrameRenderedListener()
                Log.d(TAG, "✅ Frame rendered listener registered")

                /* Restore state after first frame loaded */
                Log.d(TAG, "👁️ Setting up frameRendered observer...")
                retroView.frameRendered.observe(activity) { frameRendered ->
                    Log.i(
                            TAG,
                            "🎯 FRAME RENDERED OBSERVER TRIGGERED! frameRendered: $frameRendered"
                    )

                    if (frameRendered != true) {
                        Log.d(TAG, "⏳ Frame not rendered yet, waiting...")
                        return@observe
                    }

                    Log.i(TAG, "🎮 FIRST FRAME RENDERED! Restoring emulator state...")
                    try {
                        retroViewUtils?.restoreEmulatorState(retroView)
                        Log.i(TAG, "✅ Emulator state restored successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 ERROR restoring emulator state: ${e.message}", e)
                    }
                }
                Log.d(TAG, "✅ frameRendered observer set up")
            }

            Log.i(TAG, "🎯 RetroView setup COMPLETED!")
        } catch (e: Exception) {
            Log.e(TAG, "💥 FATAL ERROR setting up RetroView: ${e.message}", e)
            throw e
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
        leftGamePad = GamePad(context, gamePadConfig.left)
        rightGamePad = GamePad(context, gamePadConfig.right)

        // Configurar callback de eventos de botões para ambos os gamepads
        leftGamePad?.setButtonEventCallback { buttonId, action ->
            onGamePadButtonEvent(buttonId, action)
        }
        rightGamePad?.setButtonEventCallback { buttonId, action ->
            onGamePadButtonEvent(buttonId, action)
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
