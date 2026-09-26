package com.vinaooo.revenger.controllers

import android.view.KeyEvent
import android.view.MotionEvent

/** The narrow slice of `GameActivity` that [GameInputRouter] needs. */
interface GameInputHost {
        /** Records the input-event time for the profiler. */
        fun recordFrame()

        /** Restarts the floating menu button's fade. */
        fun triggerButtonFade()

        /** Refreshes the PiP still frame (throttled by `PipController`). */
        fun capturePipFrame()

        /** The ViewModel's result for a key event, or null to fall back to the platform. */
        fun processKey(keyCode: Int, event: KeyEvent): Boolean?

        /** The ViewModel's result for a motion event, or null to fall back to the platform. */
        fun processMotion(event: MotionEvent): Boolean?
}

/**
 * Key and motion routing for `GameActivity`. Key-down and generic motion events record the frame
 * time, restart the button fade and refresh the PiP frame, then go to the ViewModel; key-up goes
 * straight to the ViewModel; touch only refreshes the PiP frame. A null result from the ViewModel
 * falls back to the Activity's `super` implementation, passed in as [superCall].
 */
class GameInputRouter(private val host: GameInputHost) {

        fun onKeyDown(keyCode: Int, event: KeyEvent, superCall: () -> Boolean): Boolean {
                onUserInput()
                return host.processKey(keyCode, event) ?: superCall()
        }

        fun onKeyUp(keyCode: Int, event: KeyEvent, superCall: () -> Boolean): Boolean =
                host.processKey(keyCode, event) ?: superCall()

        fun onGenericMotionEvent(event: MotionEvent, superCall: () -> Boolean): Boolean {
                onUserInput()
                return host.processMotion(event) ?: superCall()
        }

        fun dispatchTouchEvent(superCall: () -> Boolean): Boolean {
                host.capturePipFrame()
                return superCall()
        }

        private fun onUserInput() {
                host.recordFrame()
                host.triggerButtonFade()
                host.capturePipFrame()
        }
}
