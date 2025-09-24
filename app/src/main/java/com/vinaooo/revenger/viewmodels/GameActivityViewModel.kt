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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.R
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.menu.ExpressiveGameMenuBottomSheet
import com.vinaooo.revenger.utils.RetroViewUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val resources = application.resources

    var retroView: RetroView? = null
    private var retroViewUtils: RetroViewUtils? = null

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    private var menuDialog: AlertDialog? = null
    // Sprint 2: New menu system
    private var newMenuBottomSheet: ExpressiveGameMenuBottomSheet? = null
    private var currentActivity: FragmentActivity? = null
    private val useNewMenu: Boolean by lazy { resources.getBoolean(R.bool.config_use_new_menu) }

    private var compositeDisposable = CompositeDisposable()
    private val controllerInput = ControllerInput()

    init {
        controllerInput.menuCallback = { showMenu(currentActivity) }
    }

    /** Create an instance of a menu dialog Sprint 2: Support both old and new menu systems */
    fun prepareMenu(context: Context) {
        Log.d("GameActivityViewModel", "prepareMenu called - useNewMenu: $useNewMenu")
        // Store activity reference for controller callback
        if (context is FragmentActivity) {
            currentActivity = context
            Log.d("GameActivityViewModel", "Activity reference stored: $currentActivity")
        }

        if (useNewMenu) {
            Log.d("GameActivityViewModel", "Preparing new menu...")
            prepareNewMenu()
        } else {
            Log.d("GameActivityViewModel", "Preparing old menu...")
            prepareOldMenu(context)
        }
    }

    /** Prepare traditional AlertDialog menu */
    private fun prepareOldMenu(context: Context) {
        if (menuDialog != null) return

        val menuOnClickListener = MenuOnClickListener()
        menuDialog =
                AlertDialog.Builder(context)
                        .setItems(menuOnClickListener.menuOptions, menuOnClickListener)
                        .create()
    }

    /** Prepare new Material You Expressive menu */
    private fun prepareNewMenu() {
        if (newMenuBottomSheet != null) return

        newMenuBottomSheet =
                ExpressiveGameMenuBottomSheet.newInstance().apply {
                    setMenuActionListener(
                            object : ExpressiveGameMenuBottomSheet.MenuActionListener {
                                override fun onResetClicked() {
                                    retroView?.view?.reset()
                                }

                                override fun onSaveStateClicked() {
                                    retroView?.let { retroViewUtils?.saveState(it) }
                                }

                                override fun onLoadStateClicked() {
                                    retroView?.let { retroViewUtils?.loadState(it) }
                                }

                                override fun onMuteClicked() {
                                    retroView?.let { it.view.audioEnabled = !it.view.audioEnabled }
                                }

                                override fun onFastForwardClicked() {
                                    retroView?.let { retroViewUtils?.fastForward(it) }
                                }

                                override fun onMenuDismissed() {
                                    // Optional: Handle menu dismiss event
                                }
                            }
                    )
                }
    }

    /** Show the menu Sprint 2: Support both menu systems */
    fun showMenu(activity: FragmentActivity? = null) {
        Log.d(
                "GameActivityViewModel",
                "showMenu called - useNewMenu: $useNewMenu, activity: $activity"
        )
        if (retroView?.frameRendered?.value == true) {
            retroView?.let { retroViewUtils?.preserveEmulatorState(it) }

            if (useNewMenu && activity != null) {
                Log.d("GameActivityViewModel", "Using new menu system")
                showNewMenu(activity)
            } else {
                Log.d("GameActivityViewModel", "Using old menu system - menuDialog: $menuDialog")
                menuDialog?.show()
            }
        }
    }

    /** Show new Material You Expressive menu */
    private fun showNewMenu(activity: FragmentActivity) {
        Log.d(
                "GameActivityViewModel",
                "showNewMenu called - newMenuBottomSheet: $newMenuBottomSheet"
        )
        newMenuBottomSheet?.let { bottomSheet ->
            Log.d("GameActivityViewModel", "BottomSheet found, isAdded: ${bottomSheet.isAdded}")
            if (!bottomSheet.isAdded) {
                Log.d("GameActivityViewModel", "Showing BottomSheet...")
                bottomSheet.show(activity.supportFragmentManager, "ExpressiveGameMenu")
            } else {
                Log.d("GameActivityViewModel", "BottomSheet already added, skipping")
            }
        }
                ?: Log.e("GameActivityViewModel", "newMenuBottomSheet is null!")
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
