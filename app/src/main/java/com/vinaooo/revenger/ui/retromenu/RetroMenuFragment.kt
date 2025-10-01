package com.vinaooo.revenger.ui.retromenu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
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
    
    // Callback to notify when game reset is requested
    var onResetGameCallback: (() -> Unit)? = null
    
    // Callback to notify when load state is requested
    var onLoadStateCallback: (() -> Unit)? = null
    
    // Callback to notify when save state is requested
    var onSaveStateCallback: (() -> Unit)? = null
    
    // Callback to check if save state exists
    var onHasSaveStateCallback: (() -> Boolean)? = null

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
                    setBackgroundColor(0x80000000.toInt()) // Semi-transparent black
                    isClickable = true
                    isFocusable = true

                    // Create retro menu with full options list
                    addView(
                            LinearLayout(requireContext()).apply {
                                layoutParams =
                                        FrameLayout.LayoutParams(
                                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                                )
                                                .apply { 
                                                    gravity = android.view.Gravity.CENTER
                                                }
                                orientation = LinearLayout.VERTICAL
                                gravity = android.view.Gravity.CENTER_HORIZONTAL

                                // Menu title
                                addView(createMenuTitle("REVENGER MENU"))
                                
                                // Menu options
                                addView(createMenuOption("CONTINUE GAME", true) { dismissOverlay() })
                                addView(createMenuOption("RESTART GAME", false) { restartGame() })
                                addView(createMenuOption("SAVE STATE", false) { saveState() })
                                addView(createMenuOption("LOAD STATE", false) { loadStateSafe() })
                                addView(createMenuOption("SETTINGS", false) { openSettings() })
                                addView(createMenuOption("EXIT TO MENU", false) { exitToMenu() })
                            }
                    )

                    // Keep background click behavior (don't dismiss)
                    setOnClickListener {
                        Log.d(TAG, "Retro menu background touched - use Continue button or configured gamepad button")
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

    /** Create menu title with arcade styling */
    private fun createMenuTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt()) // White
            setShadowLayer(4f, 2f, 2f, 0xFF000000.toInt()) // Shadow
            
            // Apply arcade font
            try {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.arcade_normal)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load arcade font for title, using default", e)
            }
            
            // Margin below title
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
            
            gravity = android.view.Gravity.CENTER
        }
    }
    
    /** Create menu option with arcade styling */
    private fun createMenuOption(text: String, isSelected: Boolean, action: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 20f
            setTextColor(if (isSelected) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()) // Yellow if selected, white otherwise
            setShadowLayer(3f, 1f, 1f, 0xFF000000.toInt()) // Shadow
            
            // Apply arcade font
            try {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.arcade_normal)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load arcade font for option, using default", e)
            }
            
            // Add generous padding for better touch area
            setPadding(60, 25, 60, 25)
            
            // Make it clickable with background for visual feedback
            isClickable = true
            isFocusable = true
            
            // Add subtle background to show touch area
            setBackgroundColor(0x20FFFFFF) // Very transparent white background
            
            // Add click listener
            setOnClickListener {
                Log.d(TAG, "$text option clicked!")
                
                // Enhanced visual feedback
                val originalColor = currentTextColor
                val originalBackground = background
                
                // Immediate feedback
                setTextColor(0xFFFFFFFF.toInt()) // White when pressed
                setBackgroundColor(0x40FFFFFF) // More visible background
                
                postDelayed({
                    setTextColor(originalColor) // Back to original color
                    background = originalBackground // Back to original background
                }, 200) // Slightly longer feedback
                
                action()
            }
            
            // Larger margin between options for easier targeting
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // Full width for easier touch
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 15 // More space between options
                leftMargin = 40
                rightMargin = 40
            }
            
            gravity = android.view.Gravity.CENTER
        }
    }

    /** Game actions */
    private fun restartGame() {
        Log.d(TAG, "Restart game requested")
        Log.d(TAG, "onResetGameCallback is: ${onResetGameCallback}")
        if (onResetGameCallback != null) {
            Log.d(TAG, "Calling onResetGameCallback")
            onResetGameCallback?.invoke()
            Log.d(TAG, "onResetGameCallback called successfully")
        } else {
            Log.e(TAG, "onResetGameCallback is null!")
        }
        dismissOverlay()
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
