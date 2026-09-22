package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.utils.FontUtils

/**
 * Runs a single save-slot operation (rename/copy/move/delete) against [saveStateManager] and
 * reports the outcome the same way for all four: on success, invokes the caller's `onSuccess`
 * callback (so it can refresh its grid) and shows a success toast; on failure, shows an error
 * toast and leaves the caller's state untouched.
 *
 * Extracted from [ManageSavesFragment] to keep that fragment's function count within detekt's
 * `TooManyFunctions` threshold -- this is a plain, view-lifecycle-free operation runner, so it
 * carries no fragment/view state of its own.
 */
class SaveSlotOperationRunner(private val saveStateManager: SaveStateManager) {

        fun rename(context: Context, slotNumber: Int, newName: String, onSuccess: () -> Unit) {
                run(
                        context,
                        { saveStateManager.renameSlot(slotNumber, newName) },
                        R.string.rename_success,
                        R.string.rename_error,
                        onSuccess
                )
        }

        fun copy(context: Context, fromSlot: Int, toSlot: Int, onSuccess: () -> Unit) {
                run(
                        context,
                        { saveStateManager.copySlot(fromSlot, toSlot) },
                        R.string.copy_success,
                        R.string.copy_error,
                        onSuccess
                )
        }

        fun move(context: Context, fromSlot: Int, toSlot: Int, onSuccess: () -> Unit) {
                run(
                        context,
                        { saveStateManager.moveSlot(fromSlot, toSlot) },
                        R.string.move_success,
                        R.string.move_error,
                        onSuccess
                )
        }

        fun delete(context: Context, slotNumber: Int, onSuccess: () -> Unit) {
                run(
                        context,
                        { saveStateManager.deleteSlot(slotNumber) },
                        R.string.delete_success,
                        R.string.delete_error,
                        onSuccess
                )
        }

        private fun run(
                context: Context,
                operation: () -> Boolean,
                successMessageRes: Int,
                errorMessageRes: Int,
                onSuccess: () -> Unit
        ) {
                val success = operation()
                if (success) {
                        onSuccess()
                        Toast.makeText(
                                        context,
                                        FontUtils.getCapitalizedString(context, successMessageRes),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                } else {
                        Toast.makeText(
                                        context,
                                        FontUtils.getCapitalizedString(context, errorMessageRes),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                }
        }
}
