package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Fragment for saving game state during the "Save and Exit" flow.
 *
 * This grid is shown when the user selects "Save and Exit" from the Exit menu
 * but has not previously saved or loaded during the current session. It allows
 * the user to pick a slot to save before exiting.
 *
 * Key differences from SaveSlotsFragment:
 * - The bottom button shows "EXIT" instead of "BACK"
 * - The EXIT button is DISABLED until a save is completed
 * - After saving, the EXIT button becomes enabled and triggers app shutdown
 * - The Back (B button) navigates back to the Exit menu, not Progress
 *
 * @see SaveStateGridFragment Base class with 3x3 grid navigation
 * @see ExitFragment Parent menu that navigates here
 * @see SessionSlotTracker Tracks last used slot for auto-save
 */
class ExitSaveGridFragment : SaveStateGridFragment() {

    /** Whether the EXIT button is enabled (only after a save is completed) */
    private var exitEnabled = false

    // Dialog state (reused from SaveSlotsFragment pattern)
    private var dialogOverlay: View? = null
    private var isDialogVisible = false
    private var pendingSlotNumber: Int = 0

    // Dialog navigation state
    private var dialogSelectedIndex = 0
    private var dialogButtons: List<RetroCardView> = emptyList()

    // Keyboard for naming dialog
    private var retroKeyboard: RetroKeyboard? = null
    private var isKeyboardActive = false

    override fun getTitleResId(): Int = R.string.exit_save_grid_title

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: show naming dialog
            showNamingDialog(slot.slotNumber)
        } else {
            // Occupied slot: show overwrite confirmation
            showOverwriteConfirmation(slot)
        }
    }

    override fun onBackConfirmed() {
        if (isDialogVisible) {
            hideDialog()
        } else if (exitEnabled) {
            // EXIT button confirmed - perform shutdown
            performExitWithShutdown()
        } else {
            // Navigate back to Exit menu
            Log.d(TAG, "[BACK] ExitSaveGridFragment onBackConfirmed - navigating back to Exit menu")
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

    // ========== VIEW SETUP ==========

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Change the back button text to "EXIT" and disable it initially
        backButton.text = FontUtils.getCapitalizedString(requireContext(), R.string.exit_button_label)
        updateExitButtonState()
    }

    /**
     * Updates the EXIT button visual state based on whether a save has been completed.
     */
    private fun updateExitButtonState() {
        if (exitEnabled) {
            backButton.setTextColor(resources.getColor(R.color.rm_text_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background)
            backButton.alpha = 1.0f
        } else {
            backButton.setTextColor(resources.getColor(R.color.rm_disabled_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background)
            backButton.alpha = 0.5f
        }
    }

    // Override selection visual to handle disabled EXIT button
    override fun updateSelectionVisualInternal() {
        super.updateSelectionVisualInternal()

        // Override the back/exit button visual when it's disabled
        if (isBackButtonSelected && !exitEnabled) {
            backButton.setTextColor(resources.getColor(R.color.rm_disabled_color, null))
            backButton.alpha = 0.5f
        } else if (isBackButtonSelected && exitEnabled) {
            backButton.setTextColor(resources.getColor(R.color.rm_selected_color, null))
            backButton.alpha = 1.0f
        }
    }

    // Override confirm to block EXIT when disabled
    override fun performConfirm() {
        if (isBackButtonSelected) {
            if (exitEnabled) {
                Log.d(TAG, "[ACTION] EXIT button confirmed - performing shutdown")
                performExitWithShutdown()
            } else {
                Log.d(TAG, "[ACTION] EXIT button is disabled - save first")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.exit_button_disabled_hint),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

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

    // ========== SHUTDOWN ==========

    /**
     * Performs the save-and-exit shutdown sequence:
     * 1. Dismiss menu
     * 2. Start shutdown animation
     * 3. Kill process
     */
    private fun performExitWithShutdown() {
        // Dismiss RetroMenu3
        viewModel.dismissRetroMenu3()

        // Start shutdown animation, then kill process
        (requireActivity() as? com.vinaooo.revenger.views.GameActivity)
            ?.startShutdownAnimation {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
    }

    // ========== DIALOG: NAMING ==========

    private fun showNamingDialog(slotNumber: Int) {
        pendingSlotNumber = slotNumber
        isDialogVisible = true
        isKeyboardActive = true

        val container = view as? ViewGroup ?: return

        dialogOverlay =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.retro_rename_keyboard_dlg, container, false)

        dialogOverlay?.let { dialog ->
            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val retroEditText = dialog.findViewById<RetroEditText>(R.id.rename_edit_text)

            titleView.text = FontUtils.getCapitalizedString(requireContext(), R.string.name_save_dialog_title)

            // Set hint text
            retroEditText.setHintText(FontUtils.getCapitalizedString(requireContext(), R.string.save_name_hint))
            retroEditText.setRetroHintColor(0x88888888.toInt())

            // Apply retro font
            FontUtils.getSelectedTypeface(requireContext())?.let { typeface ->
                retroEditText.applyTypeface(typeface)
            }

            // Apply fonts to other TextViews
            val textViews = mutableListOf<TextView>()
            findAllTextViews(dialog, textViews)
            textViews.removeAll { it is RetroEditText }
            ViewUtils.applySelectedFontToViews(requireContext(), *textViews.toTypedArray())

            // Initialize RetroKeyboard
            retroKeyboard = RetroKeyboard(
                context = requireContext(),
                retroEditText = retroEditText,
                onConfirm = { newName ->
                    val finalName = newName.ifBlank { "Slot $slotNumber" }
                    hideNamingDialog()
                    performSave(slotNumber, finalName)
                },
                onCancel = {
                    hideNamingDialog()
                    // Save with default name if canceled
                    performSave(slotNumber, "Slot $slotNumber")
                }
            )

            // Set default text (slot number)
            retroKeyboard?.setText("Slot $slotNumber")

            container.addView(dialog)

            // Apply menu proportions
            com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig
                .applyAllProportionsToMenuLayout(dialog)

            dialog.alpha = 0f
            dialog.animate().alpha(1f).setDuration(150).start()

            // Setup keyboard
            retroKeyboard?.setupKeyboardInView(dialog)
        }
    }

    private fun hideNamingDialog() {
        val dialog = dialogOverlay ?: return

        dialog.animate().cancel()
        dialog.visibility = View.GONE
        (dialog.parent as? ViewGroup)?.removeView(dialog)

        dialogOverlay = null
        isDialogVisible = false
        isKeyboardActive = false
        retroKeyboard = null
    }

    private fun findAllTextViews(view: View, result: MutableList<TextView>) {
        if (view is TextView) {
            result.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAllTextViews(view.getChildAt(i), result)
            }
        }
    }

    // ========== DIALOG: OVERWRITE CONFIRMATION ==========

    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        pendingSlotNumber = slot.slotNumber
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

            titleView.text = getString(R.string.overwrite_dialog_title)
            messageView.text = FontUtils.getCapitalizedString(requireContext(), R.string.overwrite_dialog_message, slot.name)
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
            // Apply configured capitalization also to the dialog message
            FontUtils.applyTextCapitalization(requireContext(), titleView, messageView, confirmText, cancelText)

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

        dialog.animate().cancel()
        dialog.visibility = View.GONE
        (dialog.parent as? ViewGroup)?.removeView(dialog)

        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0
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

    // ========== SAVE OPERATION ==========

    private fun performSave(slotNumber: Int, name: String) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            Log.e(TAG, "RetroView is null, cannot save")
            Toast.makeText(requireContext(), FontUtils.getCapitalizedString(requireContext(), R.string.save_error), Toast.LENGTH_SHORT)
                .show()
            return
        }

        try {
            // Get serialized state
            val stateBytes = retroView.view.serializeState()

            // Get cached screenshot
            val screenshot = viewModel.getCachedScreenshot()

            // Get cached full-screen screenshot for load preview overlay
            val preview = viewModel.getCachedFullScreenshot()

            // Get ROM name from config
            val romName = try {
                getString(R.string.conf_name)
            } catch (e: Exception) {
                "Unknown Game"
            }

            // Save to slot
            val success = saveStateManager.saveToSlot(
                slotNumber = slotNumber,
                stateBytes = stateBytes,
                screenshot = screenshot,
                preview = preview,
                name = name,
                romName = romName
            )

            if (success) {
                Log.d(TAG, "Save successful to slot $slotNumber")

                // Record in session tracker
                SessionSlotTracker.getInstance().recordSave(slotNumber)

                // Refresh grid to show updated slot
                refreshGrid()

                // Enable the EXIT button
                exitEnabled = true
                updateExitButtonState()

                Toast.makeText(
                    requireContext(),
                    FontUtils.getCapitalizedString(requireContext(), R.string.save_success, slotNumber),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.e(TAG, "Save failed to slot $slotNumber")
                Toast.makeText(requireContext(), FontUtils.getCapitalizedString(requireContext(), R.string.save_error), Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving state", e)
            Toast.makeText(requireContext(), FontUtils.getCapitalizedString(requireContext(), R.string.save_error), Toast.LENGTH_SHORT)
                .show()
        }
    }

    // ========== CLEANUP ==========

    override fun onDestroyView() {
        retroKeyboard = null
        isKeyboardActive = false

        dialogOverlay?.let { dialog ->
            (dialog.parent as? ViewGroup)?.removeView(dialog)
        }
        dialogOverlay = null
        isDialogVisible = false
        dialogButtons = emptyList()
        dialogSelectedIndex = 0
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "ExitSaveGridFragment"

        fun newInstance(): ExitSaveGridFragment {
            return ExitSaveGridFragment()
        }
    }
}
