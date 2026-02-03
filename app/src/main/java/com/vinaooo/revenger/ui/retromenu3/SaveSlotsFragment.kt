package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.EditText
import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for saving game state to one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Optional naming dialog when saving
 * - Confirmation when overwriting existing save
 * - Automatic screenshot capture from cached bitmap
 */
class SaveSlotsFragment : SaveStateGridFragment() {

    interface SaveSlotsListener {
        fun onSaveCompleted(slotNumber: Int)
        fun onBackToProgressMenu()
    }

    private var listener: SaveSlotsListener? = null

    fun setListener(listener: SaveSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: show naming dialog directly
            showNamingDialog(slot.slotNumber)
        } else {
            // Occupied slot: confirm overwrite
            showOverwriteConfirmation(slot)
        }
    }

    override fun onBackConfirmed() {
        listener?.onBackToProgressMenu()
    }

    private fun showNamingDialog(slotNumber: Int) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.save_name_hint)
            setText("Slot $slotNumber")
            selectAll()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_name_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val name = editText.text.toString().ifBlank { "Slot $slotNumber" }
                performSave(slotNumber, name)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.overwrite_dialog_title)
            .setMessage(getString(R.string.overwrite_dialog_message, slot.name))
            .setPositiveButton(R.string.dialog_overwrite) { _, _ ->
                showNamingDialog(slot.slotNumber)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun performSave(slotNumber: Int, name: String) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            Log.e("SaveSlotsFragment", "RetroView is null, cannot save")
            return
        }

        try {
            // Get serialized state
            val stateBytes = retroView.view.serializeState()

            // Get ROM name from config
            val romName = getString(R.string.conf_name)

            // Capture screenshot using ScreenshotCaptureUtil
            val screenshot: android.graphics.Bitmap? = com.vinaooo.revenger.utils.ScreenshotCaptureUtil.getCachedScreenshot()
            if (screenshot == null) {
                Log.w("SaveSlotsFragment", "No cached screenshot available, saving without screenshot")
            }

            // Save to slot
            val success = saveStateManager.saveToSlot(
                slotNumber = slotNumber,
                stateBytes = stateBytes,
                screenshot = screenshot,
                name = name,
                romName = romName
            )

            if (success) {
                Log.d("SaveSlotsFragment", "Save successful to slot $slotNumber")
                refreshGrid()
                listener?.onSaveCompleted(slotNumber)
            } else {
                Log.e("SaveSlotsFragment", "Save failed to slot $slotNumber")
                // TODO: Show error toast
            }
        } catch (e: Exception) {
            Log.e("SaveSlotsFragment", "Error saving state", e)
        }
    }

    companion object {
        fun newInstance(): SaveSlotsFragment {
            return SaveSlotsFragment()
        }
    }
}
