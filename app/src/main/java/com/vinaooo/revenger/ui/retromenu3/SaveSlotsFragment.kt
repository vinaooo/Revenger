package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for Save State slots grid. Allows user to select a slot to save the current game state.
 */
class SaveSlotsFragment : SaveStateGridFragment() {

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        // Show confirmation dialog before saving
        showSaveConfirmationDialog(slot)
    }

    override fun onBackConfirmed() {
        // Navigate back to main menu
        viewModel.navigationController?.navigateBack()
    }

    private fun showSaveConfirmationDialog(slot: SaveSlotData) {
        val context = requireContext()
        val dialog =
                AlertDialog.Builder(context)
                        .setTitle("Save Game State")
                        .setMessage("Save current game state to ${slot.getDisplayName()}?")
                        .setPositiveButton("Save") { _, _ -> performSave(slot) }
                        .setNegativeButton("Cancel", null)
                        .create()

        dialog.show()
    }

    private fun performSave(slot: SaveSlotData) {
        try {
            // Save the state using SaveStateManager
            saveStateManager.saveState(slot.slotNumber)

            // Show success message
            Toast.makeText(
                            requireContext(),
                            "Game saved to ${slot.getDisplayName()}",
                            Toast.LENGTH_SHORT
                    )
                    .show()

            // Navigate back to main menu
            viewModel.navigationController?.navigateBack()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save state", e)
            Toast.makeText(requireContext(), "Failed to save game state", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "SaveSlotsFragment"

        fun newInstance(): SaveSlotsFragment = SaveSlotsFragment()
    }
}
