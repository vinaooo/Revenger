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

        // Check if save state exists
        val hasSaveState = viewModel.hasSaveState()

        // CRITICAL: Build menuItems list dynamically
        // Load State sempre VISÍVEL, mas removido da navegação quando desabilitado
        menuItems =
                if (hasSaveState) {
                    // Com save: Load State habilitado e navegável
                    loadState.isEnabled = true
                    loadState.alpha = 1.0f
                    listOf(loadState, saveState, backProgress) // 3 itens navegáveis
                } else {
                    // Sem save: Load State VISÍVEL mas desabilitado e NÃO navegável
                    loadState.isEnabled = false
                    loadState.alpha = 0.5f
                    listOf(saveState, backProgress) // Apenas 2 itens navegáveis
                }

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

        // Set first item as selected (always valid - Load State removed from nav if disabled)
        updateSelectionVisualInternal()

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

    /**
     * Refresh menuItems list dynamically based on current save state. Called after saving to enable
     * Load State immediately.
     */
    private fun refreshMenuItems() {
        val hasSaveState = viewModel.hasSaveState()
        val previousItemCount = menuItems.size

        // Rebuild menuItems list dynamically
        menuItems =
                if (hasSaveState) {
                    // Com save: Load State habilitado e navegável
                    loadState.isEnabled = true
                    loadState.alpha = 1.0f
                    listOf(loadState, saveState, backProgress) // 3 itens navegáveis
                } else {
                    // Sem save: Load State VISÍVEL mas desabilitado e NÃO navegável
                    loadState.isEnabled = false
                    loadState.alpha = 0.5f
                    listOf(saveState, backProgress) // Apenas 2 itens navegáveis
                }

        android.util.Log.d(
                TAG,
                "[REFRESH] menuItems updated: $previousItemCount -> ${menuItems.size} items (hasSaveState=$hasSaveState)"
        )

        // CRITICAL: Update NavigationController with new item count
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            viewModel.navigationController?.registerFragment(this, menuItems.size)
            android.util.Log.d(
                    TAG,
                    "[REFRESH] NavigationController re-registered with ${menuItems.size} items"
            )
        }

        // Update visual to reflect changes
        updateSelectionVisualInternal()
    }

    private fun setupClickListeners() {
        // PHASE 3.3a: Route touch events through feature flag
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    TAG,
                    "[TOUCH] Using new navigation system - touch routed through NavigationController"
            )
            setupTouchNavigationSystem()
        } else {
            android.util.Log.d(TAG, "[TOUCH] Using old navigation system - direct onClick")
            setupLegacyClickListeners()
        }
    }

    /**
     * PHASE 3.3: New touch navigation system using NavigationController. Touch events create
     * SelectItem + ActivateSelected after 100ms delay.
     */
    private fun setupTouchNavigationSystem() {
        // menuItems list is dynamic (2 or 3 items depending on save state)
        menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                android.util.Log.d(
                        TAG,
                        "[TOUCH] Progress item $index clicked - routing through NavigationController"
                )

                // PHASE 3.3b: Focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                viewModel.navigationController?.selectItem(index)

                // 2. After 100ms delay, activate item
                it.postDelayed(
                        {
                            android.util.Log.d(
                                    TAG,
                                    "[TOUCH] Activating Progress item $index after delay"
                            )
                            viewModel.navigationController?.activateItem()
                        },
                        100L
                ) // 100ms = focus-then-activate delay
            }
        }
    }

    /** Legacy click listeners - direct action execution (old system). */
    private fun setupLegacyClickListeners() {
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

                        // CRITICAL: Update menuItems list to include Load State now that save
                        // exists
                        refreshMenuItems()
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

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        navigateUpCircular(menuItems.size)
        android.util.Log.d(
                TAG,
                "[NAV] Progress menu: UP navigation - now at index ${getCurrentSelectedIndex()}"
        )
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        navigateDownCircular(menuItems.size)
        android.util.Log.d(
                TAG,
                "[NAV] Progress menu: DOWN navigation - now at index ${getCurrentSelectedIndex()}"
        )
        updateSelectionVisualInternal()
    }

    /** Confirm current selection - Execute actions DIRECTLY (não usar performClick) */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] Progress menu: CONFIRM on index $selectedIndex")

        // CRITICAL: Use menuItems list to identify which item is selected
        // This handles both cases: with Load State (3 items) and without (2 items)
        if (selectedIndex >= 0 && selectedIndex < menuItems.size) {
            val selectedItem = menuItems[selectedIndex]
            android.util.Log.d(TAG, "[ACTION] Progress menu: Selected item = ${selectedItem.id}")

            when (selectedItem) {
                loadState -> {
                    // Load State - Execute action directly
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Load State selected")
                    viewModel.loadStateCentralized {
                        android.util.Log.d(TAG, "[ACTION] Progress menu: State loaded successfully")
                    }
                }
                saveState -> {
                    // Save State - Execute action directly with refreshMenuItems callback
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Save State selected")
                    viewModel.saveStateCentralized(
                            keepPaused = true,
                            onComplete = {
                                android.util.Log.d(
                                        TAG,
                                        "[ACTION] Progress menu: State saved successfully"
                                )
                                refreshMenuItems()
                            }
                    )
                }
                backProgress -> {
                    // Back to main menu - Execute action directly
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Back to main menu selected")
                    performBack()
                }
                else -> {
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Progress menu: Unknown item at index $selectedIndex"
                    )
                }
            }
        } else {
            android.util.Log.w(
                    TAG,
                    "[ACTION] Progress menu: Invalid selection index $selectedIndex (menuItems.size=${menuItems.size})"
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
        val currentIndex = getCurrentSelectedIndex()

        // CRITICAL: Get the currently selected item from menuItems (dynamic list)
        val selectedItem =
                if (currentIndex >= 0 && currentIndex < menuItems.size) {
                    menuItems[currentIndex]
                } else {
                    null
                }

        // Update each menu item based on selection state
        menuItems.forEachIndexed { index, menuItem ->
            if (index == currentIndex) {
                // Item selecionado - usar estado SELECTED do RetroCardView
                menuItem.setState(RetroCardView.State.SELECTED)
            } else {
                // Item não selecionado - usar estado NORMAL do RetroCardView
                menuItem.setState(RetroCardView.State.NORMAL)
            }
        }

        // Control text colors and arrows based on which ACTUAL item is selected
        // Load State
        if (selectedItem == loadState) {
            loadStateTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowLoadState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowLoadState.visibility = View.VISIBLE
            (selectionArrowLoadState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0
                marginEnd = 0
            }
        } else {
            loadStateTitle.setTextColor(
                    if (!loadState.isEnabled)
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
            selectionArrowLoadState.visibility = View.GONE
        }

        // Save State
        if (selectedItem == saveState) {
            saveStateTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowSaveState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowSaveState.visibility = View.VISIBLE
            (selectionArrowSaveState.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0
                marginEnd = 0
            }
        } else {
            saveStateTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_normal_color
                    )
            )
            selectionArrowSaveState.visibility = View.GONE
        }

        // Back
        if (selectedItem == backProgress) {
            backTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowBack.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowBack.visibility = View.VISIBLE
            (selectionArrowBack.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0
                marginEnd = 0
            }
        } else {
            backTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_normal_color
                    )
            )
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
