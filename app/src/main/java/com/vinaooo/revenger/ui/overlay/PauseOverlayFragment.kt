package com.vinaooo.revenger.ui.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R

/**
 * Simple overlay fragment that shows "PAUSE" text over the game screen
 * Triggered when START button is pressed alone (not in combination with SELECT)
 */
class PauseOverlayFragment : Fragment() {

    companion object {
        private const val TAG = "PauseOverlayFragment"

        fun newInstance(): PauseOverlayFragment {
            return PauseOverlayFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pause_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dismiss overlay when touched anywhere
        view.setOnClickListener {
            dismissOverlay()
        }
    }

    /**
     * Dismiss the pause overlay
     */
    fun dismissOverlay() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
}