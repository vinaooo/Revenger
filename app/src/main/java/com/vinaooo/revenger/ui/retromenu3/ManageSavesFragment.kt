package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.EditText
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for managing save states (copy, move, delete, rename).
 *
 * Workflow:
 * 1. Select a slot
 * 2. Choose operation (Move, Copy, Delete, Rename)
 * 3. For Move/Copy: select destination slot
 * 4. Confirm operation
 */
class ManageSavesFragment : SaveStateGridFragment() {

    interface ManageSavesListener {
        fun onBackToProgressMenu()
    }

    private var listener: ManageSavesListener? = null
    private var pendingOperation: Operation? = null
    private var sourceSlot: SaveSlotData? = null

    private enum class Operation {
        MOVE, COPY, DELETE, RENAME
    }

    fun setListener(listener: ManageSavesListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.manage_saves_title

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (pendingOperation != null && sourceSlot != null) {
            // Selecting destination for move/copy
            handleDestinationSelection(slot)
        } else {
            // Selecting source slot
            if (slot.isEmpty) {
                android.util.Log.d("ManageSavesFragment", "Cannot manage empty slot")
                // TODO: Show toast
                return
            }
            showOperationsDialog(slot)
        }
    }

    override fun onBackConfirmed() {
        if (pendingOperation != null) {
            // Cancel pending operation
            cancelOperation()
        } else {
            listener?.onBackToProgressMenu()
        }
    }

    private fun showOperationsDialog(slot: SaveSlotData) {
        val options = arrayOf(
            getString(R.string.operation_move),
            getString(R.string.operation_copy),
            getString(R.string.operation_delete),
            getString(R.string.operation_rename),
            getString(R.string.dialog_cancel)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.operations_dialog_title, slot.name))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startOperation(Operation.MOVE, slot)
                    1 -> startOperation(Operation.COPY, slot)
                    2 -> confirmDelete(slot)
                    3 -> showRenameDialog(slot)
                    4 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun startOperation(operation: Operation, slot: SaveSlotData) {
        pendingOperation = operation
        sourceSlot = slot

        // Update title to indicate destination selection
        gridTitle.text = getString(R.string.select_destination_title)

        // TODO: Show toast "Select destination slot"
        android.util.Log.d("ManageSavesFragment", "Started $operation from slot ${slot.slotNumber}")
    }

    private fun cancelOperation() {
        pendingOperation = null
        sourceSlot = null
        gridTitle.setText(getTitleResId())
        android.util.Log.d("ManageSavesFragment", "Operation cancelled")
    }

    private fun handleDestinationSelection(targetSlot: SaveSlotData) {
        val source = sourceSlot ?: return
        val operation = pendingOperation ?: return

        if (targetSlot.slotNumber == source.slotNumber) {
            android.util.Log.d("ManageSavesFragment", "Cannot select same slot as destination")
            // TODO: Show toast
            return
        }

        if (!targetSlot.isEmpty) {
            // Confirm overwrite
            showOverwriteConfirmation(operation, source, targetSlot)
        } else {
            executeOperation(operation, source.slotNumber, targetSlot.slotNumber)
        }
    }

    private fun showOverwriteConfirmation(
        operation: Operation,
        source: SaveSlotData,
        target: SaveSlotData
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.overwrite_dialog_title)
            .setMessage(getString(R.string.overwrite_dialog_message, target.name))
            .setPositiveButton(R.string.dialog_overwrite) { _, _ ->
                executeOperation(operation, source.slotNumber, target.slotNumber)
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                cancelOperation()
            }
            .show()
    }

    private fun executeOperation(operation: Operation, sourceSlotNum: Int, targetSlotNum: Int) {
        val success = when (operation) {
            Operation.MOVE -> saveStateManager.moveSlot(sourceSlotNum, targetSlotNum)
            Operation.COPY -> saveStateManager.copySlot(sourceSlotNum, targetSlotNum)
            else -> false
        }

        if (success) {
            android.util.Log.d("ManageSavesFragment", "$operation successful: $sourceSlotNum -> $targetSlotNum")
        } else {
            android.util.Log.e("ManageSavesFragment", "$operation failed")
        }

        cancelOperation()
        refreshGrid()
    }

    private fun confirmDelete(slot: SaveSlotData) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, slot.name))
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                val success = saveStateManager.deleteSlot(slot.slotNumber)
                if (success) {
                    android.util.Log.d("ManageSavesFragment", "Deleted slot ${slot.slotNumber}")
                }
                refreshGrid()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showRenameDialog(slot: SaveSlotData) {
        val editText = EditText(requireContext()).apply {
            setText(slot.name)
            selectAll()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.dialog_rename) { _, _ ->
                val newName = editText.text.toString().ifBlank { "Slot ${slot.slotNumber}" }
                val success = saveStateManager.renameSlot(slot.slotNumber, newName)
                if (success) {
                    android.util.Log.d("ManageSavesFragment", "Renamed slot ${slot.slotNumber} to '$newName'")
                }
                refreshGrid()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    companion object {
        fun newInstance(): ManageSavesFragment {
            return ManageSavesFragment()
        }
    }
}
