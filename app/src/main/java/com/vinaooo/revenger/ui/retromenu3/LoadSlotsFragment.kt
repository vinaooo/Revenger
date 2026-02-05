package com.vinaooo.revenger.ui.retromenu3

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.ui.retromenu3.callbacks.LoadSlotsListener

/**
 * Fragment for loading game state from one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Empty slots show visual feedback but cannot be loaded
 * - Loads state and resumes game on confirmation
 * - Shows full-screen preview overlay behind menu when slot is selected
 */
class LoadSlotsFragment : SaveStateGridFragment() {

    private var listener: LoadSlotsListener? = null

    fun setListener(listener: LoadSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_load_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            Log.d(TAG, "Cannot load from empty slot ${slot.slotNumber}")
            Toast.makeText(requireContext(), getString(R.string.slot_is_empty), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        // Show full-screen preview behind menu before loading for visual fluidity.
        // The preview bridges the transition: confirm → preview visible → menu closes → game matches.
        // Old saves without preview.webp gracefully fall back (no preview shown).
        showPreviewForSlot(slot)

        performLoad(slot.slotNumber)
    }

    /**
     * Show full-screen preview overlay for the given slot.
     * If the slot has a preview.webp file, the image is displayed behind the menu
     * and on top of the game surface, creating a smooth visual transition on load.
     */
    private fun showPreviewForSlot(slot: SaveSlotData) {
        if (!slot.hasPreview()) return

        try {
            val bitmap = BitmapFactory.decodeFile(slot.previewFile!!.absolutePath)
            if (bitmap != null) {
                viewModel.showLoadPreview(bitmap)
                Log.d(TAG, "Load preview shown for slot ${slot.slotNumber}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preview for slot ${slot.slotNumber}", e)
        }
    }

    override fun onBackConfirmed() {
        Log.d(TAG, "[BACK] LoadSlotsFragment onBackConfirmed - using NavigationController")
        viewModel.navigationController?.navigateBack()
    }

    private fun performLoad(slotNumber: Int) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            Log.e(TAG, "RetroView is null, cannot load")
            Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT)
                    .show()
            return
        }

        try {
            // Load state bytes from slot
            val stateBytes = saveStateManager.loadFromSlot(slotNumber)

            if (stateBytes == null) {
                Log.e(TAG, "Failed to load state from slot $slotNumber")
                Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT)
                        .show()
                return
            }

            // Deserialize state into emulator
            val success = retroView.view.unserializeState(stateBytes)

            if (success) {
                Log.d(TAG, "Load successful from slot $slotNumber")
                com.vinaooo.revenger.managers.SessionSlotTracker.getInstance().recordLoad(slotNumber)
                Toast.makeText(
                                requireContext(),
                                getString(R.string.load_success, slotNumber),
                                Toast.LENGTH_SHORT
                        )
                        .show()
                listener?.onLoadCompleted(slotNumber)
            } else {
                Log.e(TAG, "Failed to unserialize state from slot $slotNumber")
                Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT)
                        .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading state", e)
            Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        private const val TAG = "LoadSlotsFragment"

        fun newInstance(): LoadSlotsFragment {
            return LoadSlotsFragment()
        }
    }
}
