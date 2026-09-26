package com.vinaooo.revenger.controllers

import android.util.Log
import com.vinaooo.revenger.managers.AudioRoutingManager

/** What `GameActivity.onDestroy()` cleans up. A part that was never created is null. */
interface GameSessionParts {
        val inputDeviceWatcher: InputDeviceWatcher?
        val audioRoutingManager: AudioRoutingManager?

        fun disposeRotation()
        fun disposePip()
        fun disposeFloatingButton()
        fun stopProfiling()
        fun hideDebugOverlay()
        fun disposeViewModel()
        fun detachRetroView()
        fun clearPipFrame()
}

/**
 * The ordered cleanup that `GameActivity.onDestroy()` runs before `super.onDestroy()`.
 *
 * Every step runs even when an earlier one throws: a failure is logged and the rest go on. Before
 * this, the first failure skipped the remaining steps (audio focus release among them) and
 * `super.onDestroy()`, which makes the platform crash with `SuperNotCalledException`.
 */
class GameSessionTeardown(
        private val logFailure: (step: String, error: Throwable) -> Unit = { step, error ->
                Log.e(TAG, "Teardown step '$step' failed", error)
        }
) {
        companion object {
                private const val TAG = "GameSessionTeardown"

                /** `onDestroy`'s cleanup for [parts], in order. */
                fun stepsFor(parts: GameSessionParts): List<Step> =
                        listOf(
                                Step("rotation", parts::disposeRotation),
                                // InputManager is process-wide: a registered listener would keep
                                // the Activity.
                                Step("input devices", parts.inputDeviceWatcher?.let { it::dispose }),
                                Step("pip", parts::disposePip),
                                Step("floating button", parts::disposeFloatingButton),
                                Step("profiler", parts::stopProfiling),
                                Step("debug overlay", parts::hideDebugOverlay),
                                Step("view model", parts::disposeViewModel),
                                Step("retro view", parts::detachRetroView),
                                Step("pip frame", parts::clearPipFrame),
                                Step(
                                        "audio focus",
                                        parts.audioRoutingManager?.let { it::abandonFocus }
                                )
                        )
        }

        /** One named cleanup step; a null [action] means there is nothing to clean up. */
        class Step(val name: String, val action: (() -> Unit)?)

        /** Runs [parts]' cleanup in order and returns the failed step names. Never throws. */
        fun run(parts: GameSessionParts): List<String> = run(stepsFor(parts))

        /** Runs [steps] in order and returns the names of the ones that failed. Never throws. */
        fun run(steps: List<Step>): List<String> =
                steps.mapNotNull { step ->
                        val error = step.action?.let { runCatching(it).exceptionOrNull() }
                        error?.let {
                                logFailure(step.name, it)
                                step.name
                        }
                }
}
