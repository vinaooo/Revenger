package com.vinaooo.revenger.ui.overlay

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Simple overlay fragment that shows "PAUSE" text over the game screen Triggered when START button
 * is pressed alone (not in combination with SELECT)
 */
class PauseOverlayFragment : Fragment() {

    companion object {
        private const val TAG = "PauseOverlayFragment"

        fun newInstance(): PauseOverlayFragment {
            return PauseOverlayFragment()
        }
    }

    // Callback to notify when overlay should be dismissed
    var onDismissCallback: (() -> Unit)? = null

    // Pause overlay mode (1=START, 2=SELECT, 3=SELECT+START)
    var pauseMode: Int = 1

    init {
        Log.d(TAG, "PauseOverlayFragment initialized")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "PauseOverlayFragment.onCreateView called")
        // Temporarily create view programmatically for testing
        val frameLayout =
                FrameLayout(requireContext()).apply {
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    setBackgroundColor(0x80000000.toInt()) // Semi-transparent black
                    isClickable = true
                    isFocusable = true

                    // Add TextView programmatically
                    addView(
                            TextView(requireContext()).apply {
                                layoutParams =
                                        FrameLayout.LayoutParams(
                                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                                )
                                                .apply { gravity = android.view.Gravity.CENTER }
                                text = "PAUSE"
                                textSize = 48f
                                setTextColor(0xFFFFFFFF.toInt()) // White
                                setShadowLayer(4f, 2f, 2f, 0xFF000000.toInt()) // Shadow
                            }
                    )

                    setOnClickListener {
                        // Don't dismiss automatically - user should press START again
                        Log.d(TAG, "Overlay touched - press START to unpause")
                    }
                }
        Log.d(TAG, "PauseOverlayFragment view created programmatically")
        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dismiss overlay when touched anywhere and send START to unpause
        view.setOnClickListener { dismissOverlay() }

        // Also dismiss when appropriate button(s) are pressed
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val shouldDismiss =
                        when (pauseMode) {
                            1 -> keyCode == android.view.KeyEvent.KEYCODE_BUTTON_START // START only
                            2 ->
                                    keyCode ==
                                            android.view.KeyEvent
                                                    .KEYCODE_BUTTON_SELECT // SELECT only
                            3 ->
                                    keyCode == android.view.KeyEvent.KEYCODE_BUTTON_START ||
                                            keyCode ==
                                                    android.view.KeyEvent
                                                            .KEYCODE_BUTTON_SELECT // Either for
                            // combo
                            else -> false
                        }

                if (shouldDismiss) {
                    dismissOverlay()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    /** Dismiss the pause overlay */
    fun dismissOverlay() {
        Log.d(TAG, "Dismissing pause overlay")
        onDismissCallback?.invoke()
        Log.d(TAG, "Pause overlay dismiss callback called")
    }
}
