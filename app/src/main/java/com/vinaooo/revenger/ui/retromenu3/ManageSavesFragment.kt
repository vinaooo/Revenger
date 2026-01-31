package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for managing save state slots.
 * Provides operations like copy, move, delete, and rename.
 */
class ManageSavesFragment : SaveStateGridFragment() {

    override fun getTitleResId(): Int = R.string.menu_manage_saves

    override fun onSlotConfirmed(slot: SaveSlotData) {
        // Show management options for the selected slot
        showSlotOperationsDialog(slot)
    }

    override fun onBackConfirmed() {
        // Navigate back to main menu
        viewModel.navigationController?.navigateBack()
    }

    private fun showSlotOperationsDialog(slot: SaveSlotData) {
        val operations = arrayOf("Copy", "Move", "Delete", "Rename", "Cancel")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Manage ${slot.getDisplayName()}")
            .setItems(operations) { _, which ->
                when (which) {
                    0 -> showCopyDialog(slot)
                    1 -> showMoveDialog(slot)
                    2 -> showDeleteConfirmation(slot)
                    3 -> showRenameDialog(slot)
                    4 -> {} // Cancel
                }
            }
            .create()

        dialog.show()
    }

    private fun showCopyDialog(slot: SaveSlotData) {
        // Show dialog to select destination slot
        val slotNames = (1..9).map { "Slot $it" }.toTypedArray()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Copy ${slot.getDisplayName()} to:")
            .setItems(slotNames) { _, which ->
                val destSlot = which + 1
                performCopy(slot, destSlot)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showMoveDialog(slot: SaveSlotData) {
        // Show dialog to select destination slot
        val slotNames = (1..9).map { "Slot $it" }.toTypedArray()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Move ${slot.getDisplayName()} to:")
            .setItems(slotNames) { _, which ->
                val destSlot = which + 1
                performMove(slot, destSlot)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDeleteConfirmation(slot: SaveSlotData) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete Save")
            .setMessage("Delete ${slot.getDisplayName()}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(slot)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showRenameDialog(slot: SaveSlotData) {
        val input = android.widget.EditText(requireContext())
        input.setText(slot.name)
        input.selectAll()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Rename ${slot.getDisplayName()}")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    performRename(slot, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun performCopy(sourceSlot: SaveSlotData, destSlotNumber: Int) {
        try {
            val success = saveStateManager.copySlot(sourceSlot.slotNumber, destSlotNumber)
            if (success) {
                Toast.makeText(requireContext(), "Save copied successfully", Toast.LENGTH_SHORT).show()
                // Refresh the grid
                populateGrid()
            } else {
                Toast.makeText(requireContext(), "Failed to copy save", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to copy slot", e)
            Toast.makeText(requireContext(), "Failed to copy save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performMove(sourceSlot: SaveSlotData, destSlotNumber: Int) {
        try {
            val success = saveStateManager.moveSlot(sourceSlot.slotNumber, destSlotNumber)
            if (success) {
                Toast.makeText(requireContext(), "Save moved successfully", Toast.LENGTH_SHORT).show()
                // Refresh the grid
                populateGrid()
            } else {
                Toast.makeText(requireContext(), "Failed to move save", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to move slot", e)
            Toast.makeText(requireContext(), "Failed to move save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDelete(slot: SaveSlotData) {
        try {
            val success = saveStateManager.deleteSlot(slot.slotNumber)
            if (success) {
                Toast.makeText(requireContext(), "Save deleted successfully", Toast.LENGTH_SHORT).show()
                // Refresh the grid
                populateGrid()
            } else {
                Toast.makeText(requireContext(), "Failed to delete save", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to delete slot", e)
            Toast.makeText(requireContext(), "Failed to delete save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRename(slot: SaveSlotData, newName: String) {
        try {
            val success = saveStateManager.renameSlot(slot.slotNumber, newName)
            if (success) {
                Toast.makeText(requireContext(), "Save renamed successfully", Toast.LENGTH_SHORT).show()
                // Refresh the grid
                populateGrid()
            } else {
                Toast.makeText(requireContext(), "Failed to rename save", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to rename slot", e)
            Toast.makeText(requireContext(), "Failed to rename save", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ManageSavesFragment"
    }
}