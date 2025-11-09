package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** ProgressFragment - Progress submenu with visual identical to RetroMenu3 */
class ProgressFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var progressContainer: LinearLayout
    private lateinit var saveState: RetroCardView
    private lateinit var loadState: RetroCardView
    private lateinit var backProgress: RetroCardView

    // Menu title
    private lateinit var progressTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

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

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        // REMOVED: No longer closes when touching the sides
        // Menu only closes when selecting Back

        // PHASE 3: Register with NavigationController for new navigation system
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            viewModel.navigationController?.registerFragment(this, getMenuItems().size)
            android.util.Log.d(
                    TAG,
                    "[NAVIGATION] ProgressFragment registered with ${getMenuItems().size} items"
            )
        }
    }

    override fun onDestroyView() {
        // PHASE 3: DON'T unregister - let next fragment override registration
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    TAG,
                    "[NAVIGATION] ProgressFragment onDestroyView - keeping registration for next fragment"
            )
        }
        super.onDestroyView()
    }

    private fun setupViews(view: View) {
        // Main container
        progressContainer = view.findViewById(R.id.progress_container)

        // Menu title
        progressTitle = view.findViewById(R.id.progress_title)

        // Menu items
        saveState = view.findViewById(R.id.progress_save_state)
        loadState = view.findViewById(R.id.progress_load_state)
        backProgress = view.findViewById(R.id.progress_back)

        // Initialize ordered list of menu items
        menuItems = listOf(loadState, saveState, backProgress)

        // Configure ProgressFragment to not use background colors on cards
        // (unlike main menu which uses yellow background for selection)
        saveState.setUseBackgroundColor(false)
        loadState.setUseBackgroundColor(false)
        backProgress.setUseBackgroundColor(false)

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
        updateSelectionVisualInternal()

        // If Load State is disabled (no save state available), skip to Save State
        if (getCurrentSelectedIndex() == 0 && !loadState.isEnabled) {
            setSelectedIndex(1) // Move to Save State
            updateSelectionVisualInternal()
        }

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                progressTitle,
                saveStateTitle,
                loadStateTitle,
                backTitle,
                selectionArrowSaveState,
                selectionArrowLoadState,
                selectionArrowBack
        )

        // Aplicar capitalização configurada aos textos
        FontUtils.applyTextCapitalization(
                requireContext(),
                progressTitle,
                saveStateTitle,
                loadStateTitle,
                backTitle
        )
    }

    private fun setupClickListeners() {

        saveState.setOnClickListener {
            // Save State - Keep menu open and game paused
            android.util.Log.d(
                    TAG,
                    "[ACTION] Progress menu: Saving state (menu stays open, game stays paused)"
            )

            // NOTE: Don't restore game speed - keep game paused while menu is open
            // viewModel.restoreGameSpeedFromPreferences() // ← REMOVED

            // Save the game state without closing menus and without unpausing
            viewModel.saveStateCentralized(
                    keepPaused = true,
                    onComplete = {
                        android.util.Log.d(TAG, "[ACTION] Progress menu: State saved successfully")
                        // Could add visual feedback here if needed
                    }
            )
        }

        loadState.setOnClickListener {
            // Load State - Keep menu open and game paused
            android.util.Log.d(
                    TAG,
                    "[ACTION] Progress menu: Loading state (menu stays open, game stays paused)"
            )

            // NOTE: Don't restore game speed - keep game paused while menu is open
            // viewModel.restoreGameSpeedFromPreferences() // ← REMOVED

            // Load the saved game state without closing menus
            viewModel.loadStateCentralized {
                android.util.Log.d(TAG, "[ACTION] Progress menu: State loaded successfully")
                // Could add visual feedback here if needed
            }
        }

        backProgress.setOnClickListener {
            // Return to main menu - direct ViewModel call (survives rotation)
            android.util.Log.d(
                    TAG,
                    "[BACK] backProgress onClick - calling viewModel.dismissProgress()"
            )
            viewModel.dismissProgress()
            android.util.Log.d(TAG, "[BACK] backProgress onClick completed")
        }
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    /** Navigate up in the menu - with special logic to skip disabled Load State */
    override fun performNavigateUp() {
        val beforeIndex = getCurrentSelectedIndex()
        do {
            navigateUpCircular(menuItems.size)
        } while (getCurrentSelectedIndex() == 0 && !loadState.isEnabled)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(
                TAG,
                "[NAV] Progress menu: UP navigation - $beforeIndex -> $afterIndex (skipping disabled Load State)"
        )
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu - with special logic to skip disabled Load State */
    override fun performNavigateDown() {
        val beforeIndex = getCurrentSelectedIndex()
        do {
            navigateDownCircular(menuItems.size)
        } while (getCurrentSelectedIndex() == 0 && !loadState.isEnabled)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(
                TAG,
                "[NAV] Progress menu: DOWN navigation - $beforeIndex -> $afterIndex (skipping disabled Load State)"
        )
        updateSelectionVisualInternal()
    }

    /** Confirm current selection */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] Progress menu: CONFIRM on index $selectedIndex")
        when (selectedIndex) {
            0 -> {
                if (loadState.isEnabled) {
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Load State selected")
                    loadState.performClick() // Load State (only if enabled)
                } else {
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Progress menu: Load State selected but disabled"
                    )
                }
            }
            1 -> {
                android.util.Log.d(TAG, "[ACTION] Progress menu: Save State selected")
                saveState.performClick() // Save State
            }
            2 -> {
                android.util.Log.d(TAG, "[ACTION] Progress menu: Back to main menu selected")
                // PHASE 3: Use NavigationController when new system is active
                if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
                    android.util.Log.d(
                            TAG,
                            "[ACTION] Using new navigation system - calling performBack()"
                    )
                    performBack()
                } else {
                    backProgress.performClick() // Old system
                }
            }
            else ->
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Progress menu: Invalid selection index $selectedIndex"
                    )
        }
    }

    /** Back action */
    override fun performBack(): Boolean {
        android.util.Log.d(
                TAG,
                "[BACK] 🚀 performBack ENTRY POINT - isAdded=$isAdded, hasContext=${context != null}"
        )

        try {
            android.util.Log.d(TAG, "[BACK] performBack called - navigating to main menu")

            // PHASE 3: Use NavigationController when new system is active
            if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
                android.util.Log.d(
                        TAG,
                        "[BACK] Using new navigation system - calling viewModel.navigationController.navigateBack()"
                )
                val success = viewModel.navigationController?.navigateBack() ?: false
                android.util.Log.d(
                        TAG,
                        "[BACK] NavigationController.navigateBack() returned: $success"
                )
                return success
            } else {
                // Old system: Use viewModel directly like AboutFragment does
                android.util.Log.d(TAG, "[BACK] Calling viewModel.dismissProgressMenu()")
                viewModel.dismissProgress()
                android.util.Log.d(
                        TAG,
                        "[BACK] performBack completed - viewModel handled the dismissal"
                )
                return true
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[BACK] ❌ EXCEPTION in performBack: ${e.message}", e)
            return false
        }
    }

    /** Update selection visual - specific implementation for ProgressFragment */
    override fun updateSelectionVisualInternal() {
        // Update each menu item based on selection state
        menuItems.forEachIndexed { index, menuItem ->
            if (index == getCurrentSelectedIndex()) {
                // Item selecionado - usar estado SELECTED do RetroCardView
                menuItem.setState(RetroCardView.State.SELECTED)
            } else {
                // Item não selecionado - usar estado NORMAL do RetroCardView
                menuItem.setState(RetroCardView.State.NORMAL)
            }
        }

        // Control text colors based on selection and state
        loadStateTitle.setTextColor(
                if (getCurrentSelectedIndex() == 0 && loadState.isEnabled)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_selected_color
                        )
                else if (!loadState.isEnabled)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_disabled_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_normal_color
                        )
        )
        saveStateTitle.setTextColor(
                if (getCurrentSelectedIndex() == 1)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_selected_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_normal_color
                        )
        )
        backTitle.setTextColor(
                if (getCurrentSelectedIndex() == 2)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_selected_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.retro_menu3_normal_color
                        )
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        // val arrowMarginEnd =
        // resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Load State
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowLoadState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowLoadState.visibility = View.VISIBLE
            (selectionArrowLoadState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowLoadState.visibility = View.GONE
        }

        // Save State
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowSaveState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowSaveState.visibility = View.VISIBLE
            (selectionArrowSaveState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowSaveState.visibility = View.GONE
        }

        // Back
        if (getCurrentSelectedIndex() == 2) {
            selectionArrowBack.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowBack.visibility = View.VISIBLE
            (selectionArrowBack.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
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

    // ===== MenuFragmentBase Abstract Methods Implementation =====

    override fun getMenuItems(): List<MenuItem> {
        return listOf(
                MenuItem(
                        "save",
                        getString(R.string.menu_save_state),
                        action = MenuAction.SAVE_STATE
                ),
                MenuItem(
                        "load",
                        getString(R.string.menu_load_state),
                        action = MenuAction.LOAD_STATE,
                        isEnabled = viewModel.hasSaveState()
                ),
                MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Use new MenuAction system, but fallback to old click listeners for compatibility
        when (item.action) {
            MenuAction.SAVE_STATE -> saveState.performClick()
            MenuAction.LOAD_STATE -> loadState.performClick()
            MenuAction.BACK -> backProgress.performClick()
            else -> {
                /* Ignore other actions */
            }
        }
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d(
                "ProgressFragment",
                "[RESUME] 🏁 onResume() CALLED - isAdded=$isAdded, isResumed=$isResumed, isRemoving=$isRemoving"
        )

        // PHASE 3: Skip old navigation system logic when new system is active
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    "ProgressFragment",
                    "[RESUME] ✅ New navigation system active - skipping old MenuState checks"
            )
            return
        }

        // Check 1: Don't register if being removed
        if (isRemoving) {
            android.util.Log.d(
                    "ProgressFragment",
                    "[RESUME] ⚠️ Fragment is being removed - skipping registration"
            )
            return
        }

        // Check 2: Don't register if MenuState is not PROGRESS_MENU
        val currentState = viewModel.getMenuManager().getCurrentState()
        if (currentState != MenuState.PROGRESS_MENU) {
            android.util.Log.d(
                    "ProgressFragment",
                    "[RESUME] ⚠️ MenuState is $currentState (not PROGRESS) - skipping registration"
            )
            return
        }

        // Ensure fragment is fully resumed before re-registering
        // This prevents timing issues when returning from back stack navigation
        android.util.Log.d(
                "ProgressFragment",
                "[RESUME] 📝 Posting view runnable - view=${view != null}"
        )
        view?.post {
            android.util.Log.d(
                    "ProgressFragment",
                    "[RESUME] 🏃 view.post EXECUTED - isAdded=$isAdded, isResumed=$isResumed"
            )
            if (isAdded && isResumed) {
                android.util.Log.d(
                        "ProgressFragment",
                        "[RESUME] 💾 Registering immediately (isAdded=true, state=PROGRESS_MENU)"
                )

                // CRITICAL: Re-configure listener after rotation
                // The listener is lost when Fragment is recreated by Android
                android.util.Log.d(
                        "ProgressFragment",
                        "[RESUME] 🔗 Reconfiguring listener after recreation"
                )
                try {
                    val parentFrag = parentFragment
                    android.util.Log.d(
                            "ProgressFragment",
                            "[RESUME] 🔍 parentFragment = ${parentFrag?.javaClass?.simpleName ?: "NULL"}"
                    )
                    android.util.Log.d(
                            "ProgressFragment",
                            "[RESUME] 🔍 parentFragment.isAdded = ${parentFrag?.isAdded}"
                    )
                    android.util.Log.d(
                            "ProgressFragment",
                            "[RESUME] 🔍 parentFragment is ProgressListener? ${parentFrag is ProgressListener}"
                    )

                    if (parentFrag == null) {
                        android.util.Log.e("ProgressFragment", "[RESUME] ❌ parentFragment is NULL!")
                    } else if (parentFrag !is ProgressListener) {
                        android.util.Log.e(
                                "ProgressFragment",
                                "[RESUME] ❌ Parent fragment (${parentFrag.javaClass.simpleName}) is not ProgressListener!"
                        )
                    } else {
                        setProgressListener(parentFrag)
                        android.util.Log.d(
                                "ProgressFragment",
                                "[RESUME] ✅ Listener configured successfully - progressListener=${if (progressListener != null) "NOT NULL" else "NULL"}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                            "ProgressFragment",
                            "[RESUME] ❌ Error configuring listener",
                            e
                    )
                }

                viewModel.registerProgressFragment(this)

                // Restaurar foco no primeiro item
                val firstFocusable = view?.findViewById<android.view.View>(R.id.progress_load_state)
                firstFocusable?.requestFocus()
                android.util.Log.d("ProgressFragment", "[FOCUS] Foco restaurado no primeiro item")
            }
        }
    }

    companion object {
        private const val TAG = "ProgressMenu"

        fun newInstance(): ProgressFragment {
            return ProgressFragment()
        }
    }
}
