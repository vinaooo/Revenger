package com.vinaooo.revenger.viewmodels

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.view.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.R
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.menu.GameMenuFullscreenFragment
import com.vinaooo.revenger.ui.overlay.PauseOverlayFragment
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) :
        AndroidViewModel(application), GameMenuFullscreenFragment.GameMenuListener {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    // Replace with new GameMenuFullscreenFragment
    private var gameMenuFragment: GameMenuFullscreenFragment? = null

    // Pause overlay fragment
    private var pauseOverlayFragment: PauseOverlayFragment? = null

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    init {
        // Set the callback to check if SELECT+START combo should work
        controllerInput.shouldHandleSelectStartCombo = { shouldHandleSelectStartCombo() }
        // Set the callback to check if START alone should trigger pause
        controllerInput.shouldHandleStartPause = { shouldHandleStartPause() }
    }

    /** Configure menu callback with activity reference */
    fun setupMenuCallback(activity: FragmentActivity) {
        controllerInput.menuCallback = {
            // Check if menu is enabled before showing
            if (isMenuEnabled()) {
                showMenu(activity)
            }
        }
        controllerInput.pauseCallback = {
            // Check if pause overlay is enabled before showing
            if (isPauseOverlayEnabled()) {
                showPauseOverlay(activity)
            }
        }
    }

    /** Create an instance of the fullscreen game menu overlay */
    fun prepareMenu(activity: ComponentActivity) {
        if (gameMenuFragment != null) return

        val fragmentActivity = activity as? FragmentActivity ?: return
        setupMenuCallback(fragmentActivity)

        gameMenuFragment =
                GameMenuFullscreenFragment.newInstance().apply {
                    setMenuListener(this@GameActivityViewModel)
                }
    }

    /** Create an instance of the pause overlay */
    fun preparePauseOverlay(activity: ComponentActivity) {
        if (pauseOverlayFragment != null) return

        pauseOverlayFragment = PauseOverlayFragment.newInstance()
    }

    /** Show the pause overlay */
    fun showPauseOverlay(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            // Show pause overlay
            pauseOverlayFragment?.let { overlay ->
                if (!overlay.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(
                                    android.R.id.content,
                                    overlay,
                                    PauseOverlayFragment::class.java.simpleName
                            )
                            .commit()
                }
            }
        }
    }

    /** Dismiss the pause overlay */
    fun dismissPauseOverlay() {
        pauseOverlayFragment?.dismissOverlay()
    }

    /** Check if the pause overlay is currently visible */
    fun isPauseOverlayVisible(): Boolean {
        return pauseOverlayFragment?.isAdded == true
    }

    /** Show the fullscreen game menu */
    fun showMenu(activity: FragmentActivity) {
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            // Show new fullscreen game menu
            gameMenuFragment?.let { menu ->
                if (!menu.isAdded) {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .add(
                                    android.R.id.content,
                                    menu,
                                    GameMenuFullscreenFragment::class.java.simpleName
                            )
                            .commit()
                }
            }
        }
    }

    /** Dismiss the fullscreen menu */
    fun dismissMenu() {
        gameMenuFragment?.dismissMenuPublic()
    }

    /** Check if the menu is currently open */
    fun isMenuOpen(): Boolean {
        return gameMenuFragment?.isAdded == true
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
            // Save the new audio state immediately
            retroViewUtils?.preserveEmulatorState(it)
        }
    }

    override fun onFastForward() {
        retroView?.let {
            retroViewUtils?.fastForward(it)
            // Save the new speed state immediately
            retroViewUtils?.preserveEmulatorState(it)
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
                            // Save state and then exit
                            onSaveState()
                            android.os.Process.killProcess(android.os.Process.myPid())
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
        return retroViewUtils?.getAudioState() ?: true
    }

    override fun getFastForwardState(): Boolean {
        return retroViewUtils?.getFastForwardState() ?: false
    }

    override fun hasSaveState(): Boolean {
        return retroViewUtils?.hasSaveState() ?: false
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
        return resources.getBoolean(R.bool.config_pause_overlay)
    }

    /** Check if START alone should trigger pause overlay */
    fun shouldHandleStartPause(): Boolean {
        return isPauseOverlayEnabled()
    }
}
