package com.vinaooo.revenger.ui.saves

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

class SaveSlotsFragment : Fragment() {

    private val viewModel: GameActivityViewModel by lazy {
        ViewModelProvider(requireActivity()).get(GameActivityViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_save_slots, container, false)

        val grid = root.findViewById<GridLayout>(R.id.grid_slots)

        // Load current slots and inflate item views
        for (i in 1..9) {
            val itemView = inflater.inflate(R.layout.item_save_slot, grid, false)
            val slot = SaveSlotData.empty(i)

            val name = itemView.findViewById<android.widget.TextView>(R.id.tv_slot_name)
            val ts = itemView.findViewById<android.widget.TextView>(R.id.tv_slot_ts)
            name.text = slot.getDisplayName()
            ts.text = slot.getFormattedTimestamp()

            itemView.setOnClickListener {
                viewModel.selectedSlot.value = i
                requireActivity().onBackPressed()
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            grid.addView(itemView, params)
        }

        return root
    }
}
