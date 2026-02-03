package com.vinaooo.revenger.ui.retromenu3

import android.widget.Toast
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for loading game state from one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Empty slots show visual feedback but cannot be loaded
 * - Loads state and resumes game on confirmation
 */
class LoadSlotsFragment : SaveStateGridFragment() {

    interface LoadSlotsListener {
        fun onLoadCompleted(slotNumber: Int)
        fun onBackToProgressMenu()
    }

    private var listener: LoadSlotsListener? = null

    fun setListener(listener: LoadSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_load_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: show feedback and do nothing
            android.util.Log.d(TAG, "Cannot load from empty slot ${slot.slotNumber}")
            Toast.makeText(
                requireContext(),
                getString(R.string.slot_is_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        performLoad(slot.slotNumber)
    }

    override fun onBackConfirmed() {
        listener?.onBackToProgressMenu()
    }

    private fun performLoad(slotNumber: Int) {
        val retroView = viewModel.retroView
        if (retroView == null) {
            android.util.Log.e(TAG, "RetroView is null, cannot load")
            Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Load state bytes from slot
            val stateBytes = saveStateManager.loadFromSlot(slotNumber)

            if (stateBytes == null) {
                android.util.Log.e(TAG, "Failed to load state from slot $slotNumber")
                Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT).show()
                return
            }

            // Deserialize state into emulator
            val success = retroView.view.unserializeState(stateBytes)

            if (success) {
                android.util.Log.d(TAG, "Load successful from slot $slotNumber")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.load_success, slotNumber),
                    Toast.LENGTH_SHORT
                ).show()
                listener?.onLoadCompleted(slotNumber)
            } else {
                android.util.Log.e(TAG, "Failed to unserialize state from slot $slotNumber")
                Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading state", e)
            Toast.makeText(requireContext(), getString(R.string.load_error), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "LoadSlotsFragment"

        fun newInstance(): LoadSlotsFragment {
            return LoadSlotsFragment()
        }
    }
}
