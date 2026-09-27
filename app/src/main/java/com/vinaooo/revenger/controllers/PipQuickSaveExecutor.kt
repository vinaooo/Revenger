package com.vinaooo.revenger.controllers

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.QuickSaveSlotPicker
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Completes a Quick Save requested from the PiP window. The slot is chosen by
 * [QuickSaveSlotPicker.target]: the slot last used this session, else the first empty one, else
 * the oldest save.
 *
 * Runs after the activity is back in the foreground (the PiP action handler brought it
 * forward), waiting for one rendered frame so the GL thread is running again before
 * [GLRetroView.serializeState] -- which would deadlock while the GL thread is paused in PiP.
 * Extracted out of [PipController] to keep that class within detekt's `TooManyFunctions`
 * threshold.
 *
 * The last four parameters are seams for tests; production uses the defaults.
 *
 * @param runInBackground runs the save off the main thread (the save blocks on the GL thread).
 * @param saveManager supplies the slot store the save is written to.
 * @param pipFrame supplies the frame saved as the slot's screenshot and preview.
 * @param frameTimeoutMs how long to wait for the emulator to render before giving up.
 */
class PipQuickSaveExecutor(
        private val host: PipHost,
        private val viewModel: GameActivityViewModel,
        private val runInBackground: (Runnable) -> Unit = { Thread(it).start() },
        private val saveManager: () -> SaveStateManager = {
                SaveStateManager.getInstance(host.activity.applicationContext)
        },
        private val pipFrame: () -> Bitmap? = { ScreenshotCaptureUtil.getPipFrame() },
        private val frameTimeoutMs: Long = FRAME_TIMEOUT_MS
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
                        val emulatorResumed = withTimeoutOrNull(frameTimeoutMs) {
                                retroView.view.getGLRetroEvents()
                                        .first { it == GLRetroView.GLRetroEvents.FrameRendered }
                                true
                        } == true

                        if (!emulatorResumed) {
                                Log.w(TAG, "[PIP] Quick save aborted: emulator did not resume in time")
                                host.finishPipTask()
                                return@launch
                        }

                        runInBackground {
                                try {
                                        val tracker = SessionSlotTracker.getInstance()
                                        val slots = saveManager()
                                        // Undated slots get their file date first, so "oldest
                                        // save" is well defined when every slot is full.
                                        slots.backfillMissingTimestamps()
                                        val slotNumber = QuickSaveSlotPicker.target(
                                                tracker.getLastUsedSlot(),
                                                slots.getAllSlots()
                                        )
                                        val stateBytes = retroView.view.serializeState()
                                        val screenshot = pipFrame()
                                        val slotData = slots.getSlot(slotNumber)
                                        val saved = slots.saveToSlot(
                                                slotNumber = slotNumber,
                                                payload = SaveSlotPayload(
                                                        stateBytes = stateBytes,
                                                        screenshot = screenshot,
                                                        preview = screenshot,
                                                        name = if (slotData.isEmpty) "Slot $slotNumber" else slotData.name,
                                                        romName = host.activity.getString(R.string.name)
                                                )
                                        )
                                        if (saved) {
                                                tracker.recordSave(slotNumber)
                                                Log.d(TAG, "[PIP] Quick save written to slot $slotNumber")
                                        } else {
                                                Log.w(TAG, "[PIP] Quick save to slot $slotNumber failed")
                                        }
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
                        }
                }
        }
}
