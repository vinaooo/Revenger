package com.vinaooo.revenger.ui.retromenu3

import android.view.ViewGroup
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

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
 * - Empty slot: Show naming dialog (optional - can skip)
 * - Occupied slot: Show overwrite confirmation
 */
class SaveSlotsFragment : SaveStateGridFragment() {

    /** The naming and overwrite dialogs shown over the grid. */
    internal val dialogs = SlotDialogController { view as? ViewGroup }

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            showNamingDialog(slot.slotNumber)
        } else {
            showOverwriteConfirmation(slot)
        }
    }

    override fun onBackConfirmed() {
        if (dialogs.isVisible) {
            dialogs.hide()
        } else {
            // Navigate back using NavigationController - this will pop the PROGRESS state from
            // stack
            android.util.Log.d(TAG, "[BACK] SaveSlotsFragment onBackConfirmed - using NavigationController")
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

    override fun onDestroyView() {
        dialogs.hide()
        super.onDestroyView()
    }

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

    private fun performSave(slotNumber: Int, name: String) {
        if (CurrentGameSlotSaver(requireContext(), viewModel, saveStateManager).save(slotNumber, name)) {
            refreshGrid()
        }
    }

    companion object {
        private const val TAG = "SaveSlotsFragment"

        fun newInstance(): SaveSlotsFragment {
            return SaveSlotsFragment()
        }
    }
}
