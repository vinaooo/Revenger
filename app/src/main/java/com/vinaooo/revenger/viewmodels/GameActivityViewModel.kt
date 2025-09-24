package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import com.vinaooo.revenger.R
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.menu.GameMenuBottomSheet
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application), GameMenuBottomSheet.GameMenuListener {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    // Replace old AlertDialog with Material You BottomSheet
    private var gameMenuBottomSheet: GameMenuBottomSheet? = null
    private var currentActivity: FragmentActivity? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    init {
        controllerInput.menuCallback = { showMenu() }
    }

    /**
     * Create an instance of the Material You game menu
     */
    fun prepareMenu(activity: ComponentActivity) {
        if (gameMenuBottomSheet != null) return

        currentActivity = activity as? FragmentActivity
        gameMenuBottomSheet = GameMenuBottomSheet.newInstance().apply {
            setMenuListener(this@GameActivityViewModel)
        }
    }

    /**
     * Show the Material You menu
     */
    fun showMenu() {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // Show Material You BottomSheet instead of AlertDialog
            currentActivity?.let { activity ->
                gameMenuBottomSheet?.show(activity.supportFragmentManager, GameMenuBottomSheet.TAG)
            }
        }
    }

    /**
     * Dismiss the menu
     */
    fun dismissMenu() {
        gameMenuBottomSheet?.dismiss()
    }

    // Implementation of GameMenuBottomSheet.GameMenuListener interface
    override fun onResetGame() {
        retroView?.view?.reset()
    }

    override fun onSaveState() {
        retroView?.let { retroViewUtils?.saveState(it) }
    }

    override fun onLoadState() {
        retroView?.let { retroViewUtils?.loadState(it) }
    }

    override fun onToggleAudio() {
        retroView?.let {
            it.view.audioEnabled = !it.view.audioEnabled
        }
    }

    override fun onFastForward() {
        retroView?.let { retroViewUtils?.fastForward(it) }
    }

    override fun getAudioState(): Boolean {
        return retroView?.view?.audioEnabled == true
    }

    override fun getFastForwardState(): Boolean {
        return retroView?.view?.frameSpeed ?: 1 > 1
    }

    override fun hasSaveState(): Boolean {
        return retroViewUtils?.hasSaveState() ?: false
    }

    /**
     * Hide the system bars
     */
    @Suppress("DEPRECATION")
    fun immersive(window: Window) {
        /* Check if the config permits it */
        if (!resources.getBoolean(R.bool.config_fullscreen))
            return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with (window.insetsController!!) {
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

    /**
     * Hook the RetroView with the GLRetroView instance
     */
    fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
        retroView = RetroView(activity, viewModelScope)
        retroViewUtils = RetroViewUtils(activity)

        retroView?.let { retroView ->
            container.addView(retroView.view)
            activity.lifecycle.addObserver(retroView.view)
            retroView.registerFrameRenderedListener()

            /* Restore state after first frame loaded */
            retroView.frameRendered.observe(activity) {
                if (it != true)
                    return@observe

                retroViewUtils?.restoreEmulatorState(retroView)
            }
        }
    }

    /**
     * Subscribe the GamePads to the RetroView
     */
    fun setupGamePads(activity: ComponentActivity, leftContainer: FrameLayout, rightContainer: FrameLayout) {
        val context = getApplication<Application>().applicationContext

        val gamePadConfig = GamePadConfig(context, resources)
        leftGamePad = GamePad(context, gamePadConfig.left)
        rightGamePad = GamePad(context, gamePadConfig.right)

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView?.let { retroView -> 
                it.subscribe(activity.lifecycleScope, retroView.view)
            }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView?.let { retroView -> 
                it.subscribe(activity.lifecycleScope, retroView.view)
            }
        }
    }

    /**
     * Hide the on-screen GamePads
     */
    fun updateGamePadVisibility(activity: Activity, leftContainer: FrameLayout, rightContainer: FrameLayout) {
        val visibility = if (GamePad.shouldShowGamePads(activity))
            View.VISIBLE
        else
            View.GONE

        leftContainer.visibility = visibility
        rightContainer.visibility = visibility
    }

    /**
     * Process a key event and return the result
     */
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean? {
        retroView?.let {
            return controllerInput.processKeyEvent(keyCode, event, it)
        }

        return false
    }

    /**
     * Process a motion event and return the result
     */
    fun processMotionEvent(event: MotionEvent): Boolean? {
        retroView?.let {
            return controllerInput.processMotionEvent(event, it)
        }

        return false
    }

    /**
     * Deallocate the old RetroView
     */
    fun detachRetroView(activity: ComponentActivity) {
        retroView?.let { activity.lifecycle.removeObserver(it.view) }
        retroView = null
    }

    /**
     * Set the screen orientation based on the config
     */
    fun setConfigOrientation(activity: Activity) {
        when (resources.getInteger(R.integer.config_orientation)) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            3 -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else -> return
        }.also {
            activity.requestedOrientation = it
        }
    }

    /**
     * Dispose the composite disposable; call on onDestroy
     */
    fun dispose() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    /**
     * Save the state of the emulator
     */
    fun preserveState() {
        if (retroView?.frameRendered?.value == true)
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }
    }
}