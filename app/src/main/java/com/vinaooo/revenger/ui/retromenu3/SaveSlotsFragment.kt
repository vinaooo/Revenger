package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Fragment for saving game state to one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Retro-styled confirmation dialogs
 * - Confirmation when overwriting existing save
 * - Automatic screenshot capture from cached bitmap
 *
 * Dialog Flow:
 * - Empty slot: Direct save with default name "Slot X"
 * - Occupied slot: Show overwrite confirmation
 */
class SaveSlotsFragment : SaveStateGridFragment() {

    interface SaveSlotsListener {
        fun onSaveCompleted(slotNumber: Int)
        fun onBackToProgressMenu()
    }

    private var listener: SaveSlotsListener? = null

    // Dialog state
    private var dialogOverlay: View? = null
    private var isDialogVisible = false
    private var pendingSlotNumber: Int = 0

    // Dialog navigation state
    private var dialogSelectedIndex = 0
    private var dialogButtons: List<RetroCardView> = emptyList()

    fun setListener(listener: SaveSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: save directly with default name
            performSave(slot.slotNumber, "Slot ${slot.slotNumber}")
        } else {
            // Occupied slot: show overwrite confirmation
            showOverwriteConfirmation(slot)
        }
    }

    override fun onBackConfirmed() {
        if (isDialogVisible) {
            hideDialog()
        } else {
            // Navigate back using NavigationController - this will pop the PROGRESS state from
            // stack
            android.util.Log.d(
                    TAG,
                    "[BACK] SaveSlotsFragment onBackConfirmed - using NavigationController"
            )
            viewModel.navigationController?.navigateBack()
        }
    }

    override fun performBack(): Boolean {
        if (isDialogVisible) {
            hideDialog()
            return true
        }
        // Return false to let NavigationEventProcessor handle the back navigation
        return false
    }

    // ========== DIALOG NAVIGATION ==========

    override fun performNavigateUp() {
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
        if (isDialogVisible && dialogButtons.isNotEmpty()) {
            if (dialogSelectedIndex < dialogButtons.size - 1) {
                dialogSelectedIndex++
                updateDialogSelection()
            }
            return
        }
        super.performNavigateDown()
    }

    override fun performConfirm() {
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
                    R.id.dialog_confirm_button -> R.id.confirm_button_arrow
                    R.id.dialog_cancel_button -> R.id.cancel_button_arrow
                    else -> return@forEachIndexed
                }
            )
            val textView = button.findViewById<TextView>(
                when (button.id) {
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

    override fun onDestroyView() {
        // Clean up any visible dialog
        dialogOverlay?.let { dialog ->
            (dialog.parent as? ViewGroup)?.removeView(dialog)
        }
        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0
        super.onDestroyView()
    }

    /** Shows a retro-styled overwrite confirmation dialog. */
    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        pendingSlotNumber = slot.slotNumber
        isDialogVisible = true

        val container = view as? ViewGroup ?: return

        // Create dialog overlay
        dialogOverlay =
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.retro_confirm_dlg, container, false)

        // Setup dialog content
        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val messageView = dialog.findViewById<TextView>(R.id.dialog_message)
            val confirmButton = dialog.findViewById<RetroCardView>(R.id.dialog_confirm_button)
            val cancelButton = dialog.findViewById<RetroCardView>(R.id.dialog_cancel_button)
            val confirmText = dialog.findViewById<TextView>(R.id.confirm_button_text)
            val cancelText = dialog.findViewById<TextView>(R.id.cancel_button_text)
            val confirmArrow = dialog.findViewById<TextView>(R.id.confirm_button_arrow)
            val cancelArrow = dialog.findViewById<TextView>(R.id.cancel_button_arrow)

            titleView.text = getString(R.string.overwrite_dialog_title)
            messageView.text = getString(R.string.overwrite_dialog_message, slot.name)
            confirmText.text = getString(R.string.dialog_overwrite)
            cancelText.text = getString(R.string.dialog_cancel)

            // Configure buttons
            confirmButton.setUseBackgroundColor(false)
            cancelButton.setUseBackgroundColor(false)

            // Default selection on confirm
            confirmButton.setState(RetroCardView.State.SELECTED)
            cancelButton.setState(RetroCardView.State.NORMAL)
            confirmArrow.visibility = View.VISIBLE
            cancelArrow.visibility = View.GONE
            confirmText.setTextColor(resources.getColor(R.color.rm_selected_color, null))
            cancelText.setTextColor(resources.getColor(R.color.rm_text_color, null))

            // Apply fonts
            ViewUtils.applySelectedFontToViews(
                    requireContext(),
                    titleView,
                    messageView,
                    confirmText,
                    cancelText,
                    confirmArrow,
                    cancelArrow
            )
            FontUtils.applyTextCapitalization(requireContext(), titleView, confirmText, cancelText)

            // Click listeners
            confirmButton.setOnClickListener {
                hideDialog()
                performSave(pendingSlotNumber, "Slot $pendingSlotNumber")
            }

            cancelButton.setOnClickListener { hideDialog() }

            // Setup gamepad navigation for dialog
            dialogButtons = listOf(confirmButton, cancelButton)
            dialogSelectedIndex = 0
            updateDialogSelection()

            // Add to fragment's root view
            container.addView(dialog)

            // Animate in
            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun hideDialog() {
        val dialog = dialogOverlay ?: return
        
        // Cancel any ongoing animations
        dialog.animate().cancel()
        
        // Immediately hide and remove
        dialog.visibility = View.GONE
        (dialog.parent as? ViewGroup)?.removeView(dialog)
        
        // Reset state
        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0
    }

    private fun performSave(slotNumber: Int, name: String) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            android.util.Log.e("SaveSlotsFragment", "RetroView is null, cannot save")
            Toast.makeText(requireContext(), getString(R.string.save_error), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        try {
            // Get serialized state
            val stateBytes = retroView.view.serializeState()

            // Get cached screenshot
            val screenshot = viewModel.getCachedScreenshot()

            // Get ROM name from config
            val romName =
                    try {
                        getString(R.string.conf_name)
                    } catch (e: Exception) {
                        "Unknown Game"
                    }

            // Save to slot
            val success =
                    saveStateManager.saveToSlot(
                            slotNumber = slotNumber,
                            stateBytes = stateBytes,
                            screenshot = screenshot,
                            name = name,
                            romName = romName
                    )

            if (success) {
                android.util.Log.d("SaveSlotsFragment", "Save successful to slot $slotNumber")
                refreshGrid()
                Toast.makeText(
                                requireContext(),
                                getString(R.string.save_success, slotNumber),
                                Toast.LENGTH_SHORT
                        )
                        .show()
                listener?.onSaveCompleted(slotNumber)
            } else {
                android.util.Log.e("SaveSlotsFragment", "Save failed to slot $slotNumber")
                Toast.makeText(requireContext(), getString(R.string.save_error), Toast.LENGTH_SHORT)
                        .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("SaveSlotsFragment", "Error saving state", e)
            Toast.makeText(requireContext(), getString(R.string.save_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        private const val TAG = "SaveSlotsFragment"

        fun newInstance(): SaveSlotsFragment {
            return SaveSlotsFragment()
        }
    }
}
