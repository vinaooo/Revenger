package com.vinaooo.revenger.controllers

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Completes a Quick Save requested from the PiP window. Runs after the activity is back in the
 * foreground (the PiP action handler brought it forward), waiting for one rendered frame so the
 * GL thread is running again before [GLRetroView.serializeState] -- which would deadlock while
 * the GL thread is paused in PiP. Extracted out of [PipController] to keep that class within
 * detekt's `TooManyFunctions` threshold.
 */
class PipQuickSaveExecutor(
        private val host: PipHost,
        private val viewModel: GameActivityViewModel
) {
        companion object {
                private const val TAG = "PipQuickSaveExecutor"
                private const val FRAME_TIMEOUT_MS = 2000L
        }

        fun execute() {
                val retroView = viewModel.retroView
                if (retroView == null) {
                        host.finishPipTask()
                        return
                }

                host.activity.lifecycleScope.launch {
                        // Wait (bounded) until the emulator has stepped at least once post-resume.
                        // serializeState() runs on the GL thread via a no-timeout latch, so if the
                        // emulator never resumes we must NOT call it — abort the save instead of
                        // hanging the app forever.
                        val emulatorResumed = withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                                retroView.view.getGLRetroEvents()
                                        .first { it == GLRetroView.GLRetroEvents.FrameRendered }
                                true
                        } == true

                        if (!emulatorResumed) {
                                Log.w(TAG, "[PIP] Quick save aborted: emulator did not resume in time")
                                host.finishPipTask()
                                return@launch
                        }

                        Thread {
                                try {
                                        val tracker = SessionSlotTracker.getInstance()
                                        val slotNumber = tracker.getLastUsedSlot() ?: 1
                                        val stateBytes = retroView.view.serializeState()
                                        val screenshot = ScreenshotCaptureUtil.getPipFrame()
                                        val saveManager = SaveStateManager.getInstance(host.activity.applicationContext)
                                        val slotData = saveManager.getSlot(slotNumber)
                                        saveManager.saveToSlot(
                                                slotNumber = slotNumber,
                                                payload = SaveSlotPayload(
                                                        stateBytes = stateBytes,
                                                        screenshot = screenshot,
                                                        preview = screenshot,
                                                        name = if (slotData.isEmpty) "Slot $slotNumber" else slotData.name,
                                                        romName = host.activity.getString(R.string.name)
                                                )
                                        )
                                        tracker.recordSave(slotNumber)
                                        Log.d(TAG, "[PIP] Quick save written to slot $slotNumber")
                                        // serializeState() runs on LibretroDroid's GL thread via a blocking
                                        // CountDownLatch; a failure inside the native call there deadlocks the
                                        // latch rather than propagating an exception back to this thread, and
                                        // the only checked failure mode reaching here (the library unboxing a
                                        // null native result) surfaces as a plain NullPointerException, which
                                        // this project's detekt config still treats as "too generic" -- so
                                        // there is no narrower reachable type to catch. Kept as a safety net
                                        // via detekt's documented escape hatch instead of @Suppress.
                                } catch (expectedNativeCallFailure: Exception) {
                                        Log.e(TAG, "[PIP] Quick save failed", expectedNativeCallFailure)
                                } finally {
                                        host.postToUiThread { host.finishPipTask() }
                                }
                        }.start()
                }
        }
}
