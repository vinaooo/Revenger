package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Saves the running game into a slot for the save grids ([SaveSlotsFragment],
 * [ExitSaveGridFragment]): the emulator state, the screenshots cached when the menu opened, the
 * slot name and the game name. Shows the success or error toast, and records a successful save as
 * this session's last used slot.
 */
class CurrentGameSlotSaver(
        private val context: Context,
        private val viewModel: GameActivityViewModel,
        private val saveStateManager: SaveStateManager
) {

    /** Saves into [slotNumber] under [name]. Returns true if the save was written. */
    fun save(slotNumber: Int, name: String): Boolean {
        val retroView = viewModel.retroView
        if (retroView == null) {
            Log.e(TAG, "RetroView is null, cannot save")
            showToast(FontUtils.getCapitalizedString(context, R.string.save_error))
            return false
        }

        val saved =
                try {
                    saveStateManager.saveToSlot(
                            slotNumber = slotNumber,
                            payload =
                                    SaveSlotPayload(
                                            stateBytes = retroView.view.serializeState(),
                                            screenshot = viewModel.getCachedScreenshot(),
                                            preview = viewModel.getCachedFullScreenshot(),
                                            name = name,
                                            romName = romName()
                                    )
                    )
                    // serializeState() runs on LibretroDroid's GL thread via a blocking
                    // CountDownLatch; a failure inside the native call there deadlocks the latch
                    // rather than propagating an exception back to this thread, and the only
                    // failure reaching here (the library unboxing a null native result) is a plain
                    // NullPointerException, which this project's detekt config still treats as "too
                    // generic" -- so there is no narrower type to catch. Kept as a safety net via
                    // detekt's documented escape hatch instead of @Suppress.
                } catch (expectedNativeCallFailure: Exception) {
                    Log.e(TAG, "Error saving state", expectedNativeCallFailure)
                    false
                }

        if (saved) {
            Log.d(TAG, "Save successful to slot $slotNumber")
            SessionSlotTracker.getInstance().recordSave(slotNumber)
            showToast(FontUtils.getCapitalizedString(context, R.string.save_success, slotNumber))
        } else {
            Log.e(TAG, "Save failed to slot $slotNumber")
            showToast(FontUtils.getCapitalizedString(context, R.string.save_error))
        }
        return saved
    }

    // The configured game name, or a generic label if the resource is missing.
    private fun romName(): String =
            try {
                context.getString(R.string.name)
            } catch (e: Resources.NotFoundException) {
                Log.w(TAG, "R.string.name not found, using fallback name", e)
                FALLBACK_ROM_NAME
            }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "CurrentGameSlotSaver"
        const val FALLBACK_ROM_NAME = "Unknown Game"
    }
}
