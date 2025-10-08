package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** ProgressFragment - Progress submenu with visual identical to RetroMenu3 */
class ProgressFragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var progressContainer: LinearLayout
    private lateinit var saveState: MaterialCardView
    private lateinit var loadState: MaterialCardView
    private lateinit var backProgress: MaterialCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Start with "Save State"

    // Menu option titles for color control
    private lateinit var saveStateTitle: TextView
    private lateinit var loadStateTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSaveState: TextView
    private lateinit var selectionArrowLoadState: TextView
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface ProgressListener {
        fun onBackToMainMenu()
    }

    private var progressListener: ProgressListener? = null

    fun setProgressListener(listener: ProgressListener) {
        this.progressListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("ProgressFragment", "onViewCreated: ProgressFragment view created")

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        android.util.Log.d("ProgressFragment", "onViewCreated: ProgressFragment setup completed")
        // REMOVED: No longer closes when touching the sides
        // Menu only closes when selecting Back
    }
    
    /**
     * Recursively set z=0 and elevation=0 on all views to ensure menu stays below gamepad.
     */
    private fun forceZeroElevationRecursively(view: View) {
        view.z = 0f
        view.elevation = 0f
        view.translationZ = 0f
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                forceZeroElevationRecursively(view.getChildAt(i))
            }
        }
    }

    private fun setupViews(view: View) {
        // Main container
        progressContainer = view.findViewById(R.id.progress_container)

        // Menu items
        saveState = view.findViewById(R.id.progress_save_state)
        loadState = view.findViewById(R.id.progress_load_state)
        backProgress = view.findViewById(R.id.progress_back)

        // Initialize ordered list of menu items
        menuItems = listOf(saveState, loadState, backProgress)

        // Initialize menu option titles
        saveStateTitle = view.findViewById(R.id.save_state_title)
        loadStateTitle = view.findViewById(R.id.load_state_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowSaveState = view.findViewById(R.id.selection_arrow_save_state)
        selectionArrowLoadState = view.findViewById(R.id.selection_arrow_load_state)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Check if save state exists and disable Load State if not available
        val hasSaveState = viewModel.hasSaveState()
        loadState.isEnabled = hasSaveState
        loadState.alpha = if (hasSaveState) 1.0f else 0.5f

        // Set first item as selected
        updateSelectionVisual()
    }

    private fun setupClickListeners() {

        saveState.setOnClickListener {
            // Save State - First close menus, then set correct frameSpeed, then save
            // Close menus first
            viewModel.dismissAllMenus()

            // A) Set frameSpeed to correct value (1 or 2) from Game Speed sharedPreference
            viewModel.restoreGameSpeedFromPreferences()

            // B) Take existing actions to save the state
            viewModel.saveStateCentralized { /* Menus already closed */}
        }

        loadState.setOnClickListener {
            // Load State - First close menus, then set correct frameSpeed, then load
            // A) Close all menus first
            viewModel.dismissAllMenus()

            // B) Set frameSpeed to correct value (1 or 2) from Game Speed sharedPreference
            viewModel.restoreGameSpeedFromPreferences()

            // C) Load the saved game state
            viewModel.loadStateCentralized { /* Menus already closed */}
        }

        backProgress.setOnClickListener {
            // Return to main menu by calling ViewModel method
            viewModel.dismissProgress()
        }
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    /** Navigate up in the menu */
    fun navigateUp() {
        do {
            currentSelectedIndex = (currentSelectedIndex - 1 + menuItems.size) % menuItems.size
        } while (currentSelectedIndex == 1 && !loadState.isEnabled)
        updateSelectionVisual()
    }

    /** Navigate down in the menu */
    fun navigateDown() {
        do {
            currentSelectedIndex = (currentSelectedIndex + 1) % menuItems.size
        } while (currentSelectedIndex == 1 && !loadState.isEnabled)
        updateSelectionVisual()
    }

    /** Confirm current selection */
    fun confirmSelection() {
        when (currentSelectedIndex) {
            0 -> saveState.performClick() // Save State
            1 -> if (loadState.isEnabled) loadState.performClick() // Load State (only if enabled)
            2 -> backProgress.performClick() // Back
        }
    }

    /** Update selection visual */
    private fun updateSelectionVisual() {
        menuItems.forEach { item ->
            // Removed: background color of individual cards
            // Selection now indicated only by yellow text and arrows
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Control text colors based on selection
        saveStateTitle.setTextColor(
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        loadStateTitle.setTextColor(
                if (currentSelectedIndex == 1 && loadState.isEnabled) android.graphics.Color.YELLOW
                else if (!loadState.isEnabled) android.graphics.Color.GRAY
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Save State
        if (currentSelectedIndex == 0) {
            selectionArrowSaveState.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSaveState.visibility = View.VISIBLE
            (selectionArrowSaveState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSaveState.visibility = View.GONE
        }

        // Load State
        if (currentSelectedIndex == 1) {
            selectionArrowLoadState.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowLoadState.visibility = View.VISIBLE
            (selectionArrowLoadState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowLoadState.visibility = View.GONE
        }

        // Back
        if (currentSelectedIndex == 2) {
            selectionArrowBack.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowBack.visibility = View.VISIBLE
            (selectionArrowBack.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowBack.visibility = View.GONE
        }

        // Force layout update
        progressContainer.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            (progressListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Call clearKeyLog through ViewModel to reset combo state
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w("ProgressFragment", "Error resetting combo state in onDestroy", e)
        }
    }

    companion object {
        fun newInstance(): ProgressFragment {
            return ProgressFragment()
        }
    }
}
