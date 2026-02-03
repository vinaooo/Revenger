package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for loading game state from one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Empty slots are not selectable
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
            // Empty slot: do nothing or show toast
            Log.d("LoadSlotsFragment", "Cannot load from empty slot ${slot.slotNumber}")
            // TODO: Show toast "Slot is empty"
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
            Log.e("LoadSlotsFragment", "RetroView is null, cannot load")
            return
        }

        try {
            // Get state bytes from slot
            val stateBytes = saveStateManager.loadFromSlot(slotNumber)
            if (stateBytes == null) {
                Log.e("LoadSlotsFragment", "Failed to load state from slot $slotNumber")
                return
            }

            // Restore state
            retroView.view.unserializeState(stateBytes)

            Log.d("LoadSlotsFragment", "Load successful from slot $slotNumber")
            listener?.onLoadCompleted(slotNumber)
        } catch (e: Exception) {
            Log.e("LoadSlotsFragment", "Error loading state", e)
        }
    }

    companion object {
        fun newInstance(): LoadSlotsFragment {
            return LoadSlotsFragment()
        }
    }
}
