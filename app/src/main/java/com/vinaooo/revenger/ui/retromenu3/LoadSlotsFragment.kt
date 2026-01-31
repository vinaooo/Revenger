package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for Load State slots grid.
 * Allows user to select a slot to load a saved game state.
 */
class LoadSlotsFragment : SaveStateGridFragment() {

    override fun getTitleResId(): Int = R.string.menu_load_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Cannot load from empty slot
            Toast.makeText(requireContext(), "Cannot load from empty slot", Toast.LENGTH_SHORT).show()
        } else {
            // Show confirmation dialog before loading
            showLoadConfirmationDialog(slot)
        }
    }

    override fun onBackConfirmed() {
        // Navigate back to main menu
        viewModel.navigationController?.navigateBack()
    }

    private fun showLoadConfirmationDialog(slot: SaveSlotData) {
        val context = requireContext()
        val dialog = AlertDialog.Builder(context)
            .setTitle("Load Game State")
            .setMessage("Load game state from ${slot.getDisplayName()}? Current progress will be lost.")
            .setPositiveButton("Load") { _, _ ->
                performLoad(slot)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun performLoad(slot: SaveSlotData) {
        try {
            // Load the state using SaveStateManager
            val success = saveStateManager.loadState(slot.slotNumber)

            if (success) {
                // Show success message
                Toast.makeText(requireContext(), "Game loaded from ${slot.getDisplayName()}", Toast.LENGTH_SHORT).show()

                // Navigate back to game (close menu)
                viewModel.navigationController?.closeMenuExternal()
            } else {
                Toast.makeText(requireContext(), "Failed to load game state", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load state", e)
            Toast.makeText(requireContext(), "Failed to load game state", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "LoadSlotsFragment"
    }
}