package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.ui.retromenu3.callbacks.ManageSavesListener
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

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

    private var listener: ManageSavesListener? = null

    // Dialog/Operation state
    private var dialogOverlay: View? = null
    private var isDialogVisible = false
    private var pendingSlot: SaveSlotData? = null
    private var pendingOperation: Operation? = null
    private var isSelectingTargetSlot = false

    // Dialog navigation state
    private var dialogSelectedIndex = 0
    private var dialogButtons: List<RetroCardView> = emptyList()

    enum class Operation {
        RENAME,
        COPY,
        MOVE,
        DELETE
    }

    fun setListener(listener: ManageSavesListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        // Clean up any visible dialog
        dialogOverlay?.let { dialog ->
            (dialog.parent as? ViewGroup)?.removeView(dialog)
        }
        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0
        retroKeyboard = null
        isKeyboardActive = false
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
            Toast.makeText(requireContext(), getString(R.string.slot_is_empty), Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(
                                requireContext(),
                                getString(R.string.operation_cancelled),
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
            isDialogVisible -> {
                hideDialog()
                true
            }
            isSelectingTargetSlot -> {
                isSelectingTargetSlot = false
                pendingOperation = null
                updateTitle()
                Toast.makeText(
                                requireContext(),
                                getString(R.string.operation_cancelled),
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
        if (isKeyboardActive && retroKeyboard != null) {
            retroKeyboard?.navigateUp()
            return
        }
        if (isDialogVisible && dialogButtons.isNotEmpty()) {
            if (dialogSelectedIndex > 0) {
                dialogSelectedIndex--
                updateDialogSelection()
            }
            return
        }
        super.performNavigateUp()
    }

    override fun performNavigateDown() {
        if (isKeyboardActive && retroKeyboard != null) {
            retroKeyboard?.navigateDown()
            return
        }
        if (isDialogVisible && dialogButtons.isNotEmpty()) {
            if (dialogSelectedIndex < dialogButtons.size - 1) {
                dialogSelectedIndex++
                updateDialogSelection()
            }
            return
        }
        super.performNavigateDown()
    }

    override fun onNavigateLeft(): Boolean {
        if (isKeyboardActive && retroKeyboard != null) {
            retroKeyboard?.navigateLeft()
            return true
        }
        return super.onNavigateLeft()
    }

    override fun onNavigateRight(): Boolean {
        if (isKeyboardActive && retroKeyboard != null) {
            retroKeyboard?.navigateRight()
            return true
        }
        return super.onNavigateRight()
    }

    override fun performConfirm() {
        if (isKeyboardActive && retroKeyboard != null) {
            retroKeyboard?.pressCurrentKey()
            return
        }
        if (isDialogVisible && dialogButtons.isNotEmpty()) {
            dialogButtons.getOrNull(dialogSelectedIndex)?.performClick()
            return
        }
        super.performConfirm()
    }

    private fun updateDialogSelection() {
        dialogButtons.forEachIndexed { index, button ->
            val arrow = button.findViewById<TextView>(
                when (button.id) {
                    R.id.operation_rename -> R.id.rename_arrow
                    R.id.operation_copy -> R.id.copy_arrow
                    R.id.operation_move -> R.id.move_arrow
                    R.id.operation_delete -> R.id.delete_arrow
                    R.id.operation_cancel -> R.id.cancel_arrow
                    R.id.dialog_confirm_button -> R.id.confirm_button_arrow
                    R.id.dialog_cancel_button -> R.id.cancel_button_arrow
                    else -> return@forEachIndexed
                }
            )
            val textView = button.findViewById<TextView>(
                when (button.id) {
                    R.id.operation_rename -> R.id.rename_text
                    R.id.operation_copy -> R.id.copy_text
                    R.id.operation_move -> R.id.move_text
                    R.id.operation_delete -> R.id.delete_text
                    R.id.operation_cancel -> R.id.cancel_text
                    R.id.dialog_confirm_button -> R.id.confirm_button_text
                    R.id.dialog_cancel_button -> R.id.cancel_button_text
                    else -> return@forEachIndexed
                }
            )

            if (index == dialogSelectedIndex) {
                button.setState(RetroCardView.State.SELECTED)
                arrow?.visibility = View.VISIBLE
                textView?.setTextColor(resources.getColor(R.color.rm_selected_color, null))
            } else {
                button.setState(RetroCardView.State.NORMAL)
                arrow?.visibility = View.GONE
                textView?.setTextColor(resources.getColor(R.color.rm_text_color, null))
            }
        }
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
        isDialogVisible = true

        val container = view as? ViewGroup ?: return

        // Create dialog overlay
        dialogOverlay =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.retro_operations_menu, container, false)

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

            cancelButton.setOnClickListener { hideDialog() }

            // Setup gamepad navigation for dialog
            dialogButtons = listOf(renameButton, copyButton, moveButton, deleteButton, cancelButton)
            dialogSelectedIndex = 0
            updateDialogSelection()

            // Add to fragment's root view
            container.addView(dialog)

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

    // Keyboard instance for rename dialog
    private var retroKeyboard: RetroKeyboard? = null
    private var isKeyboardActive = false

    private fun showRenameDialog(slot: SaveSlotData) {
        pendingSlot = slot
        isDialogVisible = true
        isKeyboardActive = true

        val container = view as? ViewGroup ?: return

        dialogOverlay =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.retro_rename_keyboard_dlg, container, false)

        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val retroEditText = dialog.findViewById<RetroEditText>(R.id.rename_edit_text)

            titleView.text = getString(R.string.rename_dialog_title)
            
            // Set hint text for RetroEditText
            retroEditText.setHintText(getString(R.string.save_name_hint))
            retroEditText.setRetroHintColor(0x88888888.toInt())
            
            // Apply retro font to RetroEditText
            FontUtils.getSelectedTypeface(requireContext())?.let { typeface ->
                retroEditText.applyTypeface(typeface)
            }

            // Apply fonts to other TextViews (excluding RetroEditText)
            val textViews = mutableListOf<TextView>()
            findAllTextViews(dialog, textViews)
            // Remove RetroEditText from list since we handle it separately
            textViews.removeAll { it is RetroEditText }
            ViewUtils.applySelectedFontToViews(requireContext(), *textViews.toTypedArray())

            // Initialize RetroKeyboard
            retroKeyboard = RetroKeyboard(
                context = requireContext(),
                retroEditText = retroEditText,
                onConfirm = { newName ->
                    val finalName = newName.ifBlank { "Slot ${slot.slotNumber}" }
                    hideDialog()
                    performRename(slot.slotNumber, finalName)
                },
                onCancel = {
                    hideDialog()
                }
            )

            // Set initial text
            retroKeyboard?.setText(slot.name ?: "")

            container.addView(dialog)
            
            // Apply same proportions as other RetroMenu3 menus
            com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig.applyAllProportionsToMenuLayout(dialog)
            
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()

            // Setup keyboard click listeners and initial selection
            retroKeyboard?.setupKeyboardInView(dialog)
        }
    }

    private fun showDeleteConfirmation(slot: SaveSlotData) {
        pendingSlot = slot
        isDialogVisible = true

        val container = view as? ViewGroup ?: return

        dialogOverlay =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.retro_confirm_dlg, container, false)

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

            cancelButton.setOnClickListener { hideDialog() }

            // Setup gamepad navigation for dialog (cancel selected by default for safety)
            dialogButtons = listOf(confirmButton, cancelButton)
            dialogSelectedIndex = 1  // Cancel selected by default
            updateDialogSelection()

            container.addView(dialog)
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()
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
                    else -> ""
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
                            getString(R.string.same_slot_error),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        // Check if target is occupied
        if (!targetSlot.isEmpty) {
            Toast.makeText(
                            requireContext(),
                            getString(R.string.slot_occupied_error),
                            Toast.LENGTH_SHORT
                    )
                    .show()
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
        val dialog = dialogOverlay ?: return

        try {
            Log.d(TAG, "[DIALOG] hideDialog() called - dialogOverlay present")
            // Cancel any ongoing animations
            dialog.animate().cancel()

            // Log parent before removal
            val parentBefore = dialog.parent
            Log.d(TAG, "[DIALOG] parent before remove=${parentBefore?.javaClass?.simpleName}")

            // Immediately hide and remove
            dialog.visibility = View.GONE
            (dialog.parent as? ViewGroup)?.removeView(dialog)

            // Verify removal
            val parentAfter = dialog.parent
            Log.d(TAG, "[DIALOG] parent after remove=${parentAfter?.javaClass?.simpleName} isDialogVisible=${isDialogVisible}")
        } catch (t: Throwable) {
            Log.e(TAG, "[DIALOG] Exception while hiding dialog", t)
        }

        // Reset state
        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0

        // Reset keyboard state
        retroKeyboard = null
        isKeyboardActive = false
    }

    // ========== OPERATIONS ==========

    private fun performRename(slotNumber: Int, newName: String) {
        val success = saveStateManager.renameSlot(slotNumber, newName)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.rename_success), Toast.LENGTH_SHORT)
                    .show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.rename_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private fun performCopy(fromSlot: Int, toSlot: Int) {
        val success = saveStateManager.copySlot(fromSlot, toSlot)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT)
                    .show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.copy_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private fun performMove(fromSlot: Int, toSlot: Int) {
        val success = saveStateManager.moveSlot(fromSlot, toSlot)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.move_success), Toast.LENGTH_SHORT)
                    .show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.move_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private fun performDelete(slotNumber: Int) {
        val success = saveStateManager.deleteSlot(slotNumber)
        if (success) {
            refreshGrid()
            Toast.makeText(requireContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT)
                    .show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.delete_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        private const val TAG = "ManageSavesFragment"

        fun newInstance(): ManageSavesFragment {
            return ManageSavesFragment()
        }
    }
}
