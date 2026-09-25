package com.vinaooo.revenger.viewmodels.menu

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.gamepad.GamePadConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView

/** [GameActivityViewModel]'s virtual-GamePad wiring/visibility surface. */
interface GamePadInputFacade {
    fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    )
    fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            floatingButton: android.view.View? = null
    )
}

/**
 * Implementation of [GamePadInputFacade], plus the GamePad-event routing shared by the left/right
 * pad callbacks. Owns [leftGamePad]/[rightGamePad] itself, since nothing outside this class reads
 * them; [clear] drops both references on cleanup. [retroView], [controllerInput] and
 * [gamePadContainerView] are read via provider lambdas rather than captured, since all three are
 * mutated on the owning ViewModel after construction.
 */
class GamePadInputController(
        private val applicationContext: Context,
        private val appConfig: AppConfig,
        private val retroView: () -> RetroView?,
        private val isAnyMenuActive: () -> Boolean,
        private val controllerInput: () -> ControllerInput,
        private val gamePadContainerView: () -> android.widget.LinearLayout?
) : GamePadInputFacade {

    private var leftGamePad: GamePad? = null
    private var rightGamePad: GamePad? = null

    override fun setupGamePads(
            activity: ComponentActivity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ) {
        val gamePadConfig = GamePadConfig(applicationContext, appConfig)
        leftGamePad =
                GamePad(applicationContext, gamePadConfig.left) { event: Event ->
                    handleGamePadEvent(event)
                }
        rightGamePad =
                GamePad(applicationContext, gamePadConfig.right) { event: Event ->
                    handleGamePadEvent(event)
                }

        leftGamePad?.let {
            leftContainer.addView(it.pad)
            retroView()?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }

        rightGamePad?.let {
            rightContainer.addView(it.pad)
            retroView()?.let { retroView -> it.subscribe(activity.lifecycleScope, retroView.view) }
        }
    }

    override fun updateGamePadVisibility(
            activity: Activity,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            floatingButton: android.view.View?
    ) {
        val shouldShow = GamePad.shouldShowGamePads(activity, appConfig)
        val visibility = if (shouldShow) android.view.View.VISIBLE else android.view.View.GONE

        gamePadContainerView()?.visibility = visibility
        leftContainer.visibility = visibility
        rightContainer.visibility = visibility

        floatingButton?.let {
            val configValue = appConfig.getMenuModeFab().lowercase()
            if (configValue != "disabled" && !shouldShow) {
                it.visibility = android.view.View.VISIBLE
            } else {
                it.visibility = android.view.View.GONE
            }
        }
    }

    /** Drops both GamePad references on cleanup. */
    fun clear() {
        leftGamePad = null
        rightGamePad = null
    }

    /**
     * Routes a virtual GamePad event to [controllerInput], the same way for both the left and
     * right pad (their callbacks used to carry two verbatim copies of this logic).
     */
    private fun handleGamePadEvent(event: Event): Boolean =
            when (event) {
                is Event.Button ->
                        controllerInput().processGamePadButtonEvent(event.id, event.action)
                is Event.Direction -> handleGamePadDirectionEvent(event)
                else -> false // Other event types are not intercepted
            }

    /**
     * While a menu is open, DPAD/analog direction events are converted into a synthetic
     * [MotionEvent] and routed through [ControllerInput]'s menu-navigation path instead of being
     * dispatched natively by the GamePad.
     */
    private fun handleGamePadDirectionEvent(event: Event.Direction): Boolean {
        if (!isAnyMenuActive()) {
            // Menu is closed. Do not intercept. Let GamePad natively dispatch its axes directly.
            return false
        }
        // Process motion and return true to intercept the directional event while the menu is open
        controllerInput().processMotionEvent(buildDpadMotionEvent(event), retroView()!!)
        return true
    }

    /** Create a synthetic MotionEvent for DPAD/analog direction, using PointerCoords. */
    private fun buildDpadMotionEvent(event: Event.Direction): MotionEvent {
        val pointerCoords = MotionEvent.PointerCoords()
        pointerCoords.x = 0f
        pointerCoords.y = 0f
        pointerCoords.pressure = 1f
        pointerCoords.size = 1f
        pointerCoords.setAxisValue(MotionEvent.AXIS_HAT_X, event.xAxis)
        pointerCoords.setAxisValue(MotionEvent.AXIS_HAT_Y, event.yAxis)

        val pointerProperties = MotionEvent.PointerProperties()
        pointerProperties.id = 0
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER

        return MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                1,
                arrayOf(pointerProperties),
                arrayOf(pointerCoords),
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_JOYSTICK,
                0
        )
    }
}
