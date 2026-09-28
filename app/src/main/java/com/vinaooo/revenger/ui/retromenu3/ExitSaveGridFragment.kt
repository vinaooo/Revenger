package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils

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
 * @see com.vinaooo.revenger.managers.SessionSlotTracker Tracks last used slot for auto-save
 */
class ExitSaveGridFragment : SaveStateGridFragment() {

    /** Whether the EXIT button is enabled (only after a save is completed) */
    private var exitEnabled = false

    /** The naming and overwrite dialogs shown over the grid. */
    internal val dialogs = SlotDialogController { view as? ViewGroup }

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
        if (dialogs.isVisible) {
            dialogs.hide()
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
        if (dialogs.isVisible) {
            dialogs.hide()
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
            backButton.alpha = DISABLED_EXIT_BUTTON_ALPHA
        }
    }

    // Override selection visual to handle disabled EXIT button
    override fun updateSelectionVisualInternal() {
        super.updateSelectionVisualInternal()

        // Override the back/exit button visual when it's disabled
        if (isBackButtonSelected && !exitEnabled) {
            backButton.setTextColor(resources.getColor(R.color.rm_disabled_color, null))
            backButton.alpha = DISABLED_EXIT_BUTTON_ALPHA
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

        if (!dialogs.confirm()) super.performConfirm()
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

    // ========== DIALOGS ==========

    /** Naming dialog for an empty slot; cancelling still saves, under the default name. */
    private fun showNamingDialog(slotNumber: Int) {
        val defaultName = "Slot $slotNumber"
        dialogs.showKeyboardDialog(
                titleRes = R.string.name_save_dialog_title,
                initialText = defaultName,
                onConfirm = { name -> performSave(slotNumber, name.ifBlank { defaultName }) },
                onCancel = { performSave(slotNumber, defaultName) }
        )
    }

    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        val text =
                ConfirmDialogText(
                        title = getString(R.string.overwrite_dialog_title),
                        message = getString(R.string.overwrite_dialog_message, slot.name),
                        confirmLabel = getString(R.string.dialog_overwrite),
                        cancelLabel = getString(R.string.dialog_cancel)
                )
        dialogs.showConfirmDialog(text) { performSave(slot.slotNumber, slot.overwriteName()) }
    }

    override fun performNavigateUp() {
        if (!dialogs.navigateVertical(-1)) super.performNavigateUp()
    }

    override fun performNavigateDown() {
        if (!dialogs.navigateVertical(1)) super.performNavigateDown()
    }

    override fun onNavigateLeft(): Boolean = dialogs.navigateHorizontal(toLeft = true) || super.onNavigateLeft()

    override fun onNavigateRight(): Boolean = dialogs.navigateHorizontal(toLeft = false) || super.onNavigateRight()

    // ========== SAVE OPERATION ==========

    /** Saves, and on success enables the EXIT button. */
    private fun performSave(slotNumber: Int, name: String) {
        if (CurrentGameSlotSaver(requireContext(), viewModel, saveStateManager).save(slotNumber, name)) {
            refreshGrid()
            exitEnabled = true
            updateExitButtonState()
        }
    }

    // ========== CLEANUP ==========

    override fun onDestroyView() {
        dialogs.hide()
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "ExitSaveGridFragment"
        private const val DISABLED_EXIT_BUTTON_ALPHA = 0.5f

        fun newInstance(): ExitSaveGridFragment {
            return ExitSaveGridFragment()
        }
    }
}
