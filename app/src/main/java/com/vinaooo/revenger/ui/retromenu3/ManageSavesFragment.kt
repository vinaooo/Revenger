package com.vinaooo.revenger.ui.retromenu3

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils

/**
 * Fragment for managing save slots (rename, copy, move, delete).
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Context menu for occupied slots with options:
 * - Rename: Change save name
 * - Copy: Copy to another slot
 * - Move: Move to another slot
 * - Delete: Delete save
 * - Empty slots show "No Save" and cannot be managed
 */
class ManageSavesFragment : SaveStateGridFragment() {

    /** The operations, rename and delete dialogs shown over the grid. */
    internal val dialogs = SlotDialogController { view as? ViewGroup }

    // Copy/move target selection state
    private var pendingSlot: SaveSlotData? = null
    private var pendingOperation: Operation? = null
    private var isSelectingTargetSlot = false

    /** The two-step operations that pick a target slot after the source. */
    enum class Operation {
        COPY,
        MOVE
    }

    override fun onDestroyView() {
        dialogs.hide()
        super.onDestroyView()
    }

    override fun getTitleResId(): Int = R.string.manage_saves_title

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (isSelectingTargetSlot) {
            // User is selecting target slot for copy/move operation
            handleTargetSlotSelected(slot)
            return
        }

        if (slot.isEmpty) {
            // Empty slot: show feedback
            android.util.Log.d(TAG, "Cannot manage empty slot ${slot.slotNumber}")
            Toast.makeText(
                            requireContext(),
                            FontUtils.getCapitalizedString(requireContext(), R.string.slot_is_empty),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        // Show operations menu for this slot
        showOperationsMenu(slot)
    }

    override fun onBackConfirmed() {
        when {
            dialogs.isVisible -> dialogs.hide()
            isSelectingTargetSlot -> {
                isSelectingTargetSlot = false
                pendingOperation = null
                updateTitle()
                Toast.makeText(
                                requireContext(),
                                FontUtils.getCapitalizedString(requireContext(), R.string.operation_cancelled),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
            else -> {
                // Navigate back using NavigationController - this will pop the PROGRESS state from
                // stack
                android.util.Log.d(
                        TAG,
                        "[BACK] ManageSavesFragment onBackConfirmed - using NavigationController"
                )
                viewModel.navigationController?.navigateBack()
            }
        }
    }

    override fun performBack(): Boolean {
        return when {
            dialogs.isVisible -> {
                dialogs.hide()
                true
            }
            isSelectingTargetSlot -> {
                isSelectingTargetSlot = false
                pendingOperation = null
                updateTitle()
                Toast.makeText(
                                requireContext(),
                                FontUtils.getCapitalizedString(requireContext(), R.string.operation_cancelled),
                                Toast.LENGTH_SHORT
                        )
                        .show()
                true
            }
            // Return false to let NavigationEventProcessor handle the back navigation
            // Don't call super.performBack() as it causes infinite recursion
            else -> false
        }
    }

    // ========== DIALOG NAVIGATION ==========

    override fun performNavigateUp() {
        if (!dialogs.navigateVertical(-1)) super.performNavigateUp()
    }

    override fun performNavigateDown() {
        if (!dialogs.navigateVertical(1)) super.performNavigateDown()
    }

    override fun onNavigateLeft(): Boolean = dialogs.navigateHorizontal(toLeft = true) || super.onNavigateLeft()

    override fun onNavigateRight(): Boolean = dialogs.navigateHorizontal(toLeft = false) || super.onNavigateRight()

    override fun performConfirm() {
        if (!dialogs.confirm()) super.performConfirm()
    }

    private fun updateTitle() {
        if (isSelectingTargetSlot) {
            val operationName =
                    when (pendingOperation) {
                        Operation.COPY -> getString(R.string.select_target_copy)
                        Operation.MOVE -> getString(R.string.select_target_move)
                        else -> getString(R.string.manage_saves_title)
                    }
            gridTitle.text = operationName
        } else {
            gridTitle.setText(getTitleResId())
        }
    }

    /** Shows a retro-styled operations menu for the selected slot. */
    private fun showOperationsMenu(slot: SaveSlotData) {
        pendingSlot = slot
        dialogs.showDialog(R.layout.retro_operations_menu) { dialog ->
            dialog.findViewById<TextView>(R.id.dialog_title).text =
                    getString(R.string.operations_dialog_title, slot.getDisplayName())
            dialog.findViewById<LinearLayout>(R.id.operations_container)?.let {
                SlotDialogViews.styleTexts(SlotDialogViews.textViewsIn(it))
            }

            val actions =
                    listOf<Pair<Int, () -> Unit>>(
                            R.id.operation_rename to { showRenameDialog(slot) },
                            R.id.operation_copy to { startTargetSlotSelection(slot, Operation.COPY) },
                            R.id.operation_move to { startTargetSlotSelection(slot, Operation.MOVE) },
                            R.id.operation_delete to { showDeleteConfirmation(slot) },
                            R.id.operation_cancel to {}
                    )
            actions.map { (id, action) ->
                dialog.findViewById<RetroCardView>(id).apply {
                    setUseBackgroundColor(false)
                    setOnClickListener {
                        dialogs.hide()
                        action()
                    }
                }
            }
        }
    }

    private fun showRenameDialog(slot: SaveSlotData) {
        pendingSlot = slot
        dialogs.showKeyboardDialog(
                titleRes = R.string.rename_dialog_title,
                initialText = slot.name,
                onConfirm = { newName ->
                    val finalName = newName.ifBlank { "Slot ${slot.slotNumber}" }
                    SaveSlotOperationRunner(saveStateManager)
                            .rename(requireContext(), slot.slotNumber, finalName) { refreshGrid() }
                },
                onCancel = {}
        )
    }

    /** Delete confirmation, with Cancel selected by default for safety. */
    private fun showDeleteConfirmation(slot: SaveSlotData) {
        pendingSlot = slot
        val text =
                ConfirmDialogText(
                        title = getString(R.string.delete_dialog_title),
                        message = getString(R.string.delete_dialog_message, slot.name),
                        confirmLabel = getString(R.string.dialog_delete),
                        cancelLabel = getString(R.string.dialog_cancel)
                )
        dialogs.showConfirmDialog(text, selectCancel = true) {
            SaveSlotOperationRunner(saveStateManager)
                    .delete(requireContext(), slot.slotNumber) { refreshGrid() }
        }
    }

    private fun startTargetSlotSelection(slot: SaveSlotData, operation: Operation) {
        pendingSlot = slot
        pendingOperation = operation
        isSelectingTargetSlot = true
        updateTitle()

        val message =
                when (operation) {
                    Operation.COPY -> getString(R.string.select_slot_to_copy)
                    Operation.MOVE -> getString(R.string.select_slot_to_move)
                }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun handleTargetSlotSelected(targetSlot: SaveSlotData) {
        val sourceSlot = pendingSlot ?: return
        val operation = pendingOperation ?: return

        // Cannot select same slot
        if (targetSlot.slotNumber == sourceSlot.slotNumber) {
            Toast.makeText(
                            requireContext(),
                            FontUtils.getCapitalizedString(requireContext(), R.string.same_slot_error),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        // Check if target is occupied
        if (!targetSlot.isEmpty) {
            Toast.makeText(
                            requireContext(),
                            FontUtils.getCapitalizedString(requireContext(), R.string.slot_occupied_error),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        isSelectingTargetSlot = false
        updateTitle()

        val operationRunner = SaveSlotOperationRunner(saveStateManager)
        when (operation) {
            Operation.COPY ->
                    operationRunner.copy(
                            requireContext(),
                            sourceSlot.slotNumber,
                            targetSlot.slotNumber
                    ) { refreshGrid() }
            Operation.MOVE ->
                    operationRunner.move(
                            requireContext(),
                            sourceSlot.slotNumber,
                            targetSlot.slotNumber
                    ) { refreshGrid() }
            else -> {}
        }

        pendingSlot = null
        pendingOperation = null
    }

    companion object {
        private const val TAG = "ManageSavesFragment"

        fun newInstance(): ManageSavesFragment {
            return ManageSavesFragment()
        }
    }
}
