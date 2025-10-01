package com.vinaooo.revenger.ui.retromenu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Retro menu fragment that shows "PAUSE" text over the game screen Triggered by gamepad buttons
 * (START, SELECT, or SELECT+START combinations based on configuration)
 */
class RetroMenuFragment : Fragment() {

        companion object {
                private const val TAG = "RetroMenuFragment"

                fun newInstance(): RetroMenuFragment {
                        return RetroMenuFragment()
                }
        }

        // Get ViewModel reference for centralized methods
        private lateinit var viewModel: GameActivityViewModel

        // Callback to notify when retro menu should be dismissed
        var onDismissCallback: (() -> Unit)? = null

        // Note: Reset game callback removed - now using centralized ViewModel method

        // Note: Load/Save state callbacks removed - now using centralized ViewModel methods

        // Retro menu mode (1=START, 2=SELECT, 3=SELECT+START)
        var retroMenuMode: Int = 1

        init {
                Log.d(TAG, "RetroMenuFragment initialized")
        }

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View? {
                Log.d(TAG, "RetroMenuFragment.onCreateView called")
                // Create retro menu with title and Continue button
                val frameLayout =
                        FrameLayout(requireContext()).apply {
                                layoutParams =
                                        ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                setBackgroundColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_background
                                        )
                                )
                                isClickable = true
                                isFocusable = true

                                // Create 3-column layout: 25% - 50% - 25%
                                addView(
                                        LinearLayout(requireContext()).apply {
                                                layoutParams =
                                                        FrameLayout.LayoutParams(
                                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                                ViewGroup.LayoutParams.MATCH_PARENT
                                                        )
                                                orientation = LinearLayout.HORIZONTAL

                                                // Left column (25% - empty spacer)
                                                addView(
                                                        View(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.25f // Left column
                                                                                // weight -
                                                                                // hardcoded
                                                                                // for stability
                                                                                )
                                                        }
                                                )

                                                // Center column (50% - contains the menu)
                                                addView(
                                                        LinearLayout(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.50f // Center
                                                                                // column
                                                                                // weight -
                                                                                // hardcoded for
                                                                                // stability
                                                                                )
                                                                orientation = LinearLayout.VERTICAL
                                                                gravity =
                                                                        android.view.Gravity
                                                                                .CENTER_VERTICAL or
                                                                                android.view.Gravity
                                                                                        .LEFT

                                                                // Menu container aligned to left of
                                                                // center column
                                                                addView(
                                                                        LinearLayout(
                                                                                        requireContext()
                                                                                )
                                                                                .apply {
                                                                                        layoutParams =
                                                                                                LinearLayout
                                                                                                        .LayoutParams(
                                                                                                                ViewGroup
                                                                                                                        .LayoutParams
                                                                                                                        .WRAP_CONTENT,
                                                                                                                ViewGroup
                                                                                                                        .LayoutParams
                                                                                                                        .WRAP_CONTENT
                                                                                                        )
                                                                                        orientation =
                                                                                                LinearLayout
                                                                                                        .VERTICAL
                                                                                        gravity =
                                                                                                android.view
                                                                                                        .Gravity
                                                                                                        .LEFT

                                                                                        // Menu
                                                                                        // title
                                                                                        addView(
                                                                                                createMenuTitle(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_title
                                                                                                        )
                                                                                                )
                                                                                        )

                                                                                        // Menu
                                                                                        // options
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_continue_game
                                                                                                        ),
                                                                                                        true
                                                                                                ) {
                                                                                                        continueGame()
                                                                                                }
                                                                                        )
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_restart_game
                                                                                                        ),
                                                                                                        false
                                                                                                ) {
                                                                                                        restartGame()
                                                                                                }
                                                                                        )
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_save_state
                                                                                                        ),
                                                                                                        false
                                                                                                ) {
                                                                                                        saveState()
                                                                                                }
                                                                                        )
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_load_state
                                                                                                        ),
                                                                                                        false
                                                                                                ) {
                                                                                                        loadStateSafe()
                                                                                                }
                                                                                        )
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_settings
                                                                                                        ),
                                                                                                        false
                                                                                                ) {
                                                                                                        openSettings()
                                                                                                }
                                                                                        )
                                                                                        addView(
                                                                                                createMenuOption(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_exit_to_menu
                                                                                                        ),
                                                                                                        false
                                                                                                ) {
                                                                                                        exitToMenu()
                                                                                                }
                                                                                        )
                                                                                }
                                                                ) // Close menu container
                                                        }
                                                ) // Close center column

                                                // Right column (25% - empty spacer)
                                                addView(
                                                        View(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.25f // Right
                                                                                // column
                                                                                // weight -
                                                                                // hardcoded
                                                                                // for stability
                                                                                )
                                                        }
                                                )
                                        } // Close 3-column layout
                                )

                                // Keep background click behavior (don't dismiss)
                                setOnClickListener {
                                        Log.d(
                                                TAG,
                                                "Retro menu background touched - use Continue button or configured gamepad button"
                                        )
                                }
                        }
                Log.d(TAG, "RetroMenuFragment view created programmatically with arcade font")
                return frameLayout
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                // Initialize ViewModel
                viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

                // Also dismiss when appropriate button(s) are pressed
                view.isFocusableInTouchMode = true
                view.requestFocus()
                view.setOnKeyListener { _, keyCode, event ->
                        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                val shouldDismiss =
                                        when (retroMenuMode) {
                                                1 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_START // START only
                                                2 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_SELECT // SELECT only
                                                3 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_START ||
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

        /** Create menu title with arcade styling */
        private fun createMenuTitle(text: String): TextView {
                return TextView(requireContext()).apply {
                        this.text = text
                        textSize =
                                resources.getDimension(R.dimen.retro_menu_title_text_size) /
                                        resources.displayMetrics.density
                        setTextColor(
                                ContextCompat.getColor(
                                        requireContext(),
                                        R.color.retro_menu_text_default
                                )
                        )
                        setShadowLayer(
                                resources.getDimension(R.dimen.retro_menu_title_shadow_radius),
                                resources.getDimension(R.dimen.retro_menu_title_shadow_dx),
                                resources.getDimension(R.dimen.retro_menu_title_shadow_dy),
                                ContextCompat.getColor(requireContext(), R.color.retro_menu_shadow)
                        )

                        // Apply arcade font
                        try {
                                typeface =
                                        ResourcesCompat.getFont(
                                                requireContext(),
                                                R.font.arcade_normal
                                        )
                        } catch (e: Exception) {
                                Log.w(TAG, "Could not load arcade font for title, using default", e)
                        }

                        // Margin below title
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        .apply {
                                                bottomMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_title_bottom_margin
                                                        )
                                        }

                        gravity = android.view.Gravity.LEFT
                }
        }

        /** Create menu option with arcade styling */
        private fun createMenuOption(
                text: String,
                isSelected: Boolean,
                action: () -> Unit
        ): TextView {
                return TextView(requireContext()).apply {
                        this.text = text
                        textSize =
                                resources.getDimension(R.dimen.retro_menu_option_text_size) /
                                        resources.displayMetrics.density
                        setTextColor(
                                if (isSelected)
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_selected
                                        )
                                else
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_default
                                        )
                        )
                        setShadowLayer(
                                resources.getDimension(R.dimen.retro_menu_option_shadow_radius),
                                resources.getDimension(R.dimen.retro_menu_option_shadow_dx),
                                resources.getDimension(R.dimen.retro_menu_option_shadow_dy),
                                ContextCompat.getColor(requireContext(), R.color.retro_menu_shadow)
                        )

                        // Apply arcade font
                        try {
                                typeface =
                                        ResourcesCompat.getFont(
                                                requireContext(),
                                                R.font.arcade_normal
                                        )
                        } catch (e: Exception) {
                                Log.w(
                                        TAG,
                                        "Could not load arcade font for option, using default",
                                        e
                                )
                        }

                        // Add generous padding for better touch area
                        setPadding(
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_horizontal
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_vertical
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_horizontal
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_vertical
                                )
                        )

                        // Make it clickable with background for visual feedback
                        isClickable = true
                        isFocusable = true

                        // Add background using color from XML config
                        setBackgroundColor(
                                ContextCompat.getColor(
                                        requireContext(),
                                        R.color.retro_menu_option_background
                                )
                        )

                        // Add click listener
                        setOnClickListener {
                                Log.d(TAG, "$text option clicked!")

                                // Enhanced visual feedback
                                val originalColor = currentTextColor
                                val originalBackground = background

                                // Immediate feedback
                                setTextColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_default
                                        )
                                ) // White when pressed
                                setBackgroundColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_option_background_pressed
                                        )
                                ) // Background when pressed

                                postDelayed(
                                        {
                                                setTextColor(
                                                        originalColor
                                                ) // Back to original color
                                                background = originalBackground // Back to original
                                                // background
                                        },
                                        resources
                                                .getInteger(R.integer.retro_menu_feedback_duration)
                                                .toLong()
                                ) // Feedback duration from XML

                                action()
                        }

                        // Larger margin between options for easier targeting
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams
                                                        .WRAP_CONTENT, // Wrap content to align left
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        .apply {
                                                bottomMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_bottom
                                                        )
                                                leftMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_horizontal
                                                        )
                                                rightMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_horizontal
                                                        )
                                        }

                        gravity = android.view.Gravity.CENTER
                }
        }

        /** Game actions */
        private fun restartGame() {
                Log.d(TAG, "Restart game requested - using centralized implementation")

                // Use centralized implementation from ViewModel
                viewModel.resetGameCentralized {
                        Log.d(TAG, "Centralized reset game completed, dismissing overlay")
                        dismissOverlay()
                }
        }

        private fun saveState() {
                Log.d(TAG, "Save state requested - using centralized implementation")

                // Use centralized implementation from ViewModel
                viewModel.saveStateCentralized {
                        Log.d(TAG, "Centralized save state completed, dismissing overlay")
                        dismissOverlay()
                }
        }

        private fun loadState() {
                Log.d(TAG, "Load state requested - using centralized implementation")

                // Use centralized implementation from ViewModel
                viewModel.loadStateCentralized {
                        Log.d(TAG, "Centralized load state completed, dismissing overlay")
                        dismissOverlay()
                }
        }

        private fun loadStateSafe() {
                Log.d(TAG, "Load state safe requested - using centralized implementation")

                // Use centralized implementation from ViewModel (already includes save state check)
                viewModel.loadStateCentralized {
                        Log.d(TAG, "Centralized load state safe completed, dismissing overlay")
                        dismissOverlay()
                }
        }

        private fun openSettings() {
                Log.d(TAG, "Settings requested")
                // TODO: Implement settings functionality
                dismissOverlay()
        }

        private fun exitToMenu() {
                Log.d(TAG, "Exit to menu requested")
                // TODO: Implement exit to menu functionality
                dismissOverlay()
        }

        private fun continueGame() {
                Log.d(TAG, "Continue game requested - using centralized implementation")

                // Use centralized implementation from ViewModel
                viewModel.continueGameCentralized {
                        Log.d(TAG, "Centralized continue game completed, dismissing overlay")
                        dismissOverlay()
                }
        }

        /** Dismiss the pause overlay */
        fun dismissOverlay() {
                Log.d(TAG, "dismissOverlay() called")
                Log.d(TAG, "onDismissCallback is: ${onDismissCallback}")
                if (onDismissCallback != null) {
                        Log.d(TAG, "Calling onDismissCallback")
                        onDismissCallback?.invoke()
                        Log.d(TAG, "onDismissCallback called successfully")
                } else {
                        Log.e(TAG, "onDismissCallback is null!")
                }
        }
}
