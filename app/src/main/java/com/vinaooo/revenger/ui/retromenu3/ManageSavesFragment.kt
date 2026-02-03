package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Fragment for managing save slots (rename, copy, move, delete).
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Context menu for occupied slots with options:
 *   - Rename: Change save name
 *   - Copy: Copy to another slot
 *   - Move: Move to another slot
 *   - Delete: Delete save
 * - Empty slots show "No Save" and cannot be managed
 */
class ManageSavesFragment : SaveStateGridFragment() {

    interface ManageSavesListener {
        fun onBackToProgressMenu()
    }

    private var listener: ManageSavesListener? = null

    // Dialog/Operation state
    private var dialogOverlay: View? = null
    private var isDialogVisible = false
    private var pendingSlot: SaveSlotData? = null
    private var pendingOperation: Operation? = null
    private var isSelectingTargetSlot = false

    enum class Operation {
        RENAME, COPY, MOVE, DELETE
    }

    fun setListener(listener: ManageSavesListener) {
        this.listener = listener
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
                getString(R.string.slot_is_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Show operations menu for this slot
        showOperationsMenu(slot)
    }

    override fun onBackConfirmed() {
        when {
            isDialogVisible -> hideDialog()
            isSelectingTargetSlot -> {
                isSelectingTargetSlot = false
                pendingOperation = null
                updateTitle()
                Toast.makeText(requireContext(), getString(R.string.operation_cancelled), Toast.LENGTH_SHORT).show()
            }
            else -> listener?.onBackToProgressMenu()
        }
    }

    override fun performBack(): Boolean {
        return when {
            isDialogVisible -> {
                hideDialog()
                true
            }
            isSelectingTargetSlot -> {
                isSelectingTargetSlot = false
                pendingOperation = null
                updateTitle()
                Toast.makeText(requireContext(), getString(R.string.operation_cancelled), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.performBack()
        }
    }

    private fun updateTitle() {
        if (isSelectingTargetSlot) {
            val operationName = when (pendingOperation) {
                Operation.COPY -> getString(R.string.select_target_copy)
                Operation.MOVE -> getString(R.string.select_target_move)
                else -> getString(R.string.manage_saves_title)
            }
            gridTitle.text = operationName
        } else {
            gridTitle.setText(getTitleResId())
        }
    }

    /**
     * Shows a retro-styled operations menu for the selected slot.
     */
    private fun showOperationsMenu(slot: SaveSlotData) {
        pendingSlot = slot
        isDialogVisible = true

        val parent = view?.parent as? ViewGroup ?: return

        // Create dialog overlay
        dialogOverlay = LayoutInflater.from(requireContext())
            .inflate(R.layout.retro_operations_menu, parent, false)

        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val renameButton = dialog.findViewById<RetroCardView>(R.id.operation_rename)
            val copyButton = dialog.findViewById<RetroCardView>(R.id.operation_copy)
            val moveButton = dialog.findViewById<RetroCardView>(R.id.operation_move)
            val deleteButton = dialog.findViewById<RetroCardView>(R.id.operation_delete)
            val cancelButton = dialog.findViewById<RetroCardView>(R.id.operation_cancel)

            titleView.text = getString(R.string.operations_dialog_title, slot.getDisplayName())

            // Configure buttons
            listOf(renameButton, copyButton, moveButton, deleteButton, cancelButton).forEach {
                it.setUseBackgroundColor(false)
            }

            // Apply fonts
            dialog.findViewById<LinearLayout>(R.id.operations_container)?.let { container ->
                val textViews = mutableListOf<TextView>()
                findAllTextViews(container, textViews)
                ViewUtils.applySelectedFontToViews(requireContext(), *textViews.toTypedArray())
                FontUtils.applyTextCapitalization(requireContext(), *textViews.toTypedArray())
            }

            // Click listeners
            renameButton.setOnClickListener {
                hideDialog()
                showRenameDialog(slot)
            }

            copyButton.setOnClickListener {
                hideDialog()
                startTargetSlotSelection(slot, Operation.COPY)
            }

            moveButton.setOnClickListener {
                hideDialog()
                startTargetSlotSelection(slot, Operation.MOVE)
            }

            deleteButton.setOnClickListener {
                hideDialog()
                showDeleteConfirmation(slot)
            }

            cancelButton.setOnClickListener {
                hideDialog()
            }

            // Add to parent
            parent.addView(dialog)

            // Animate in
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun findAllTextViews(view: View, list: MutableList<TextView>) {
        if (view is TextView) {
            list.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAllTextViews(view.getChildAt(i), list)
            }
        }
    }

    private fun showRenameDialog(slot: SaveSlotData) {
        pendingSlot = slot
        isDialogVisible = true

        val parent = view?.parent as? ViewGroup ?: return

        dialogOverlay = LayoutInflater.from(requireContext())
            .inflate(R.layout.retro_rename_dlg, parent, false)

        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val editText = dialog.findViewById<EditText>(R.id.rename_edit_text)
            val confirmButton = dialog.findViewById<RetroCardView>(R.id.dialog_confirm_button)
            val cancelButton = dialog.findViewById<RetroCardView>(R.id.dialog_cancel_button)

            titleView.text = getString(R.string.rename_dialog_title)
            editText.setText(slot.name)
            editText.selectAll()

            confirmButton.setUseBackgroundColor(false)
            cancelButton.setUseBackgroundColor(false)

            // Apply fonts (excluding EditText)
            val textViews = mutableListOf<TextView>()
            findAllTextViews(dialog, textViews)
            ViewUtils.applySelectedFontToViews(requireContext(), *textViews.toTypedArray())

            confirmButton.setOnClickListener {
                val newName = editText.text.toString().ifBlank { "Slot ${slot.slotNumber}" }
                hideDialog()
                performRename(slot.slotNumber, newName)
            }

            cancelButton.setOnClickListener {
                hideDialog()
            }

            parent.addView(dialog)
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()

            // Request focus on edit text
            editText.requestFocus()
        }
    }

    private fun showDeleteConfirmation(slot: SaveSlotData) {
        pendingSlot = slot
        isDialogVisible = true

        val parent = view?.parent as? ViewGroup ?: return

        dialogOverlay = LayoutInflater.from(requireContext())
            .inflate(R.layout.retro_confirm_dlg, parent, false)

        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val messageView = dialog.findViewById<TextView>(R.id.dialog_message)
            val confirmButton = dialog.findViewById<RetroCardView>(R.id.dialog_confirm_button)
            val cancelButton = dialog.findViewById<RetroCardView>(R.id.dialog_cancel_button)
            val confirmText = dialog.findViewById<TextView>(R.id.confirm_button_text)
            val cancelText = dialog.findViewById<TextView>(R.id.cancel_button_text)
            val confirmArrow = dialog.findViewById<TextView>(R.id.confirm_button_arrow)
            val cancelArrow = dialog.findViewById<TextView>(R.id.cancel_button_arrow)

            titleView.text = getString(R.string.delete_dialog_title)
            messageView.text = getString(R.string.delete_dialog_message, slot.name)
            confirmText.text = getString(R.string.dialog_delete)
            cancelText.text = getString(R.string.dialog_cancel)

            confirmButton.setUseBackgroundColor(false)
            cancelButton.setUseBackgroundColor(false)

            // Default selection on cancel (safer)
            confirmButton.setState(RetroCardView.State.NORMAL)
            cancelButton.setState(RetroCardView.State.SELECTED)
            confirmArrow.visibility = View.GONE
            cancelArrow.visibility = View.VISIBLE
            confirmText.setTextColor(resources.getColor(R.color.rm_text_color, null))
            cancelText.setTextColor(resources.getColor(R.color.rm_selected_color, null))

            val textViews = mutableListOf<TextView>()
            findAllTextViews(dialog, textViews)
            ViewUtils.applySelectedFontToViews(requireContext(), *textViews.toTypedArray())
            FontUtils.applyTextCapitalization(requireContext(), *textViews.toTypedArray())

            confirmButton.setOnClickListener {
                hideDialog()
                performDelete(slot.slotNumber)
            }

            cancelButton.setOnClickListener {
                hideDialog()
            }

            parent.addView(dialog)
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun startTargetSlotSelection(slot: SaveSlotData, operation: Operation) {
        pendingSlot = slot
        pendingOperation = operation
        isSelectingTargetSlot = true
        updateTitle()

        val message = when (operation) {
            Operation.COPY -> getString(R.string.select_slot_to_copy)
            Operation.MOVE -> getString(R.string.select_slot_to_move)
            else -> ""
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun handleTargetSlotSelected(targetSlot: SaveSlotData) {
        val sourceSlot = pendingSlot ?: return
        val operation = pendingOperation ?: return

        // Cannot select same slot
        if (targetSlot.slotNumber == sourceSlot.slotNumber) {
            Toast.makeText(requireContext(), getString(R.string.same_slot_error), Toast.LENGTH_SHORT).show()
            return
        }

        // Check if target is occupied
        if (!targetSlot.isEmpty) {
            Toast.makeText(requireContext(), getString(R.string.slot_occupied_error), Toast.LENGTH_SHORT).show()
            return
        }

        isSelectingTargetSlot = false
        updateTitle()

        when (operation) {
            Operation.COPY -> performCopy(sourceSlot.slotNumber, targetSlot.slotNumber)
            Operation.MOVE -> performMove(sourceSlot.slotNumber, targetSlot.slotNumber)
            else -> {}
        }

        pendingSlot = null
        pendingOperation = null
    }

    private fun hideDialog() {
        dialogOverlay?.let { dialog ->
            dialog.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction {
                    (dialog.parent as? ViewGroup)?.removeView(dialog)
                    dialogOverlay = null
                    isDialogVisible = false
                }
                .start()
        }
    }

    // ========== OPERATIONS ==========

    private fun performRename(slotNumber: Int, newName: String) {
        val success = saveStateManager.renameSlot(slotNumber, newName)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.rename_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.rename_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performCopy(fromSlot: Int, toSlot: Int) {
        val success = saveStateManager.copySlot(fromSlot, toSlot)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.copy_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performMove(fromSlot: Int, toSlot: Int) {
        val success = saveStateManager.moveSlot(fromSlot, toSlot)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.move_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.move_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDelete(slotNumber: Int) {
        val success = saveStateManager.deleteSlot(slotNumber)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.delete_error), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ManageSavesFragment"

        fun newInstance(): ManageSavesFragment {
            return ManageSavesFragment()
        }
    }
}
