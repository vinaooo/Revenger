package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Fragment do submenu Progress (Save/Load States).
 *
 * **Funcionalidades (Multi-Slot System)**:
 * - Load State: Abre grid de 9 slots para carregar
 * - Save State: Abre grid de 9 slots para salvar
 * - Manage Saves: Abre grid para gerenciar (rename, copy, move, delete)
 * - Back: Volta ao menu principal
 *
 * **Navegação**:
 * - Cada opção navega para um submenu com grid 3x3
 * - Suporte a navegação 2D (UP/DOWN/LEFT/RIGHT) nos grids
 *
 * **Arquitetura Multi-Input**:
 * - Gamepad: DPAD UP/DOWN para navegar, A para confirmar, B para voltar
 * - Teclado: Arrow keys para navegar, Enter para confirmar, Backspace para voltar
 * - Touch: Toque com highlight imediato + delay de 100ms para ativação
 *
 * @see SaveSlotsFragment Grid para salvar em 9 slots
 * @see LoadSlotsFragment Grid para carregar de 9 slots
 * @see ManageSavesFragment Grid para gerenciar saves
 */
class ProgressFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var progressContainer: LinearLayout
    private lateinit var loadState: RetroCardView
    private lateinit var saveState: RetroCardView
    private lateinit var manageSaves: RetroCardView
    private lateinit var backProgress: RetroCardView

    // Menu title
    private lateinit var progressTitle: TextView

    // Ordered list of menu items for navigation (always 4 items)
    private lateinit var menuItems: List<RetroCardView>

    // Menu option titles for color control
    private lateinit var loadStateTitle: TextView
    private lateinit var saveStateTitle: TextView
    private lateinit var manageSavesTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowLoadState: TextView
    private lateinit var selectionArrowSaveState: TextView
    private lateinit var selectionArrowManageSaves: TextView
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

        // Aplicar proporções de layout configuráveis
        applyLayoutProportions(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()

        // Register with NavigationController
        viewModel.navigationController?.registerFragment(this, menuItems.size)
        android.util.Log.d(
                TAG,
                "[NAVIGATION] ProgressFragment registered with ${menuItems.size} items"
        )
    }

    override fun onDestroyView() {
        android.util.Log.d(
                TAG,
                "[NAVIGATION] ProgressFragment onDestroyView - keeping registration for next fragment"
        )
        super.onDestroyView()
    }

    private fun setupViews(view: View) {
        // Main container
        progressContainer = view.findViewById(R.id.progress_container)

        // Menu title
        progressTitle = view.findViewById(R.id.progress_title)

        // Menu items (order: Load, Save, Manage, Back)
        loadState = view.findViewById(R.id.progress_load_state)
        saveState = view.findViewById(R.id.progress_save_state)
        manageSaves = view.findViewById(R.id.progress_manage_saves)
        backProgress = view.findViewById(R.id.progress_back)

        // Build menuItems list (always 4 items now)
        menuItems = listOf(loadState, saveState, manageSaves, backProgress)

        // Configure to not use background colors on cards
        loadState.setUseBackgroundColor(false)
        saveState.setUseBackgroundColor(false)
        manageSaves.setUseBackgroundColor(false)
        backProgress.setUseBackgroundColor(false)

        // Initialize menu option titles
        loadStateTitle = view.findViewById(R.id.load_state_title)
        saveStateTitle = view.findViewById(R.id.save_state_title)
        manageSavesTitle = view.findViewById(R.id.manage_saves_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowLoadState = view.findViewById(R.id.selection_arrow_load_state)
        selectionArrowSaveState = view.findViewById(R.id.selection_arrow_save_state)
        selectionArrowManageSaves = view.findViewById(R.id.selection_arrow_manage_saves)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                progressTitle,
                loadStateTitle,
                saveStateTitle,
                manageSavesTitle,
                backTitle,
                selectionArrowLoadState,
                selectionArrowSaveState,
                selectionArrowManageSaves,
                selectionArrowBack
        )

        // Aplicar capitalização configurada aos textos
        FontUtils.applyTextCapitalization(
                requireContext(),
                progressTitle,
                loadStateTitle,
                saveStateTitle,
                manageSavesTitle,
                backTitle
        )
    }

    private fun setupClickListeners() {
        // Route touch events through NavigationController
        android.util.Log.d(
                TAG,
                "[TOUCH] Using navigation system - touch routed through NavigationController"
        )
        setupTouchNavigationSystem()
    }

    /**
     * Touch navigation system using NavigationController.
     * Touch events create SelectItem + ActivateSelected after 100ms delay.
     */
    private fun setupTouchNavigationSystem() {
        menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                android.util.Log.d(
                        TAG,
                        "[TOUCH] Progress item $index clicked - routing through NavigationController"
                )

                // Focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                viewModel.navigationController?.selectItem(index)

                // 2. After delay, activate item
                it.postDelayed(
                        {
                            android.util.Log.d(
                                    TAG,
                                    "[TOUCH] Activating Progress item $index after delay"
                            )
                            viewModel.navigationController?.activateItem()
                        },
                        MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS
                )
            }
        }
    }

    private fun dismissMenu() {
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

    /** Confirm current selection - Navigate to grid submenus */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] Progress menu: CONFIRM on index $selectedIndex")

        if (selectedIndex >= 0 && selectedIndex < menuItems.size) {
            val selectedItem = menuItems[selectedIndex]
            android.util.Log.d(TAG, "[ACTION] Progress menu: Selected item = ${selectedItem.id}")

            when (selectedItem) {
                loadState -> {
                    // Navigate to Load Slots grid
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Opening Load Slots grid")
                    navigateToSubmenu(MenuType.LOAD_SLOTS)
                }
                saveState -> {
                    // Navigate to Save Slots grid
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Opening Save Slots grid")
                    navigateToSubmenu(MenuType.SAVE_SLOTS)
                }
                manageSaves -> {
                    // Navigate to Manage Saves grid
                    android.util.Log.d(TAG, "[ACTION] Progress menu: Opening Manage Saves grid")
                    navigateToSubmenu(MenuType.MANAGE_SAVES)
                }
                backProgress -> {
                    // Back to main menu
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
                    "[ACTION] Progress menu: Invalid selection index $selectedIndex"
            )
        }
    }

    /**
     * Navigate to a submenu using FragmentNavigationAdapter.
     */
    private fun navigateToSubmenu(menuType: MenuType) {
        android.util.Log.d(TAG, "[NAV] Navigating to submenu: $menuType")
        
        // Show the submenu fragment directly
        val fragmentAdapter = com.vinaooo.revenger.ui.retromenu3.navigation.FragmentNavigationAdapter(requireActivity())
        fragmentAdapter.showMenu(menuType)
    }

    /** Back action */
    override fun performBack(): Boolean {
        android.util.Log.d(
                TAG,
                "[BACK] 🚀 performBack ENTRY POINT - isAdded=$isAdded, hasContext=${context != null}"
        )

        try {
            android.util.Log.d(TAG, "[BACK] performBack called - navigating to main menu")

            // PHASE 3: Use NavigationController (permanently enabled)
            android.util.Log.d(
                    TAG,
                    "[BACK] Using new navigation system - calling viewModel.navigationController.navigateBack()"
            )
            val success = viewModel.navigationController?.navigateBack() ?: false
            android.util.Log.d(TAG, "[BACK] NavigationController.navigateBack() returned: $success")
            return success
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
                            R.color.rm_selected_color
                    )
            )
            selectionArrowLoadState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
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
                                    R.color.rm_disabled_color
                            )
                    else
                            androidx.core.content.ContextCompat.getColor(
                                    requireContext(),
                                    R.color.rm_normal_color
                            )
            )
            selectionArrowLoadState.visibility = View.GONE
        }

        // Save State
        if (selectedItem == saveState) {
            saveStateTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowSaveState.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
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
                            R.color.rm_normal_color
                    )
            )
            selectionArrowSaveState.visibility = View.GONE
        }

        // Manage Saves
        if (selectedItem == manageSaves) {
            manageSavesTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowManageSaves.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowManageSaves.visibility = View.VISIBLE
            (selectionArrowManageSaves.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0
                marginEnd = 0
            }
        } else {
            manageSavesTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_normal_color
                    )
            )
            selectionArrowManageSaves.visibility = View.GONE
        }

        // Back
        if (selectedItem == backProgress) {
            backTitle.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowBack.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
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
                            R.color.rm_normal_color
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
            val activity = requireActivity()
            val viewModel = ViewModelProvider(activity)[GameActivityViewModel::class.java]
            // Call clearKeyLog through ViewModel to reset combo state
            viewModel.clearControllerKeyLog()
        } catch (e: Exception) {
            android.util.Log.w("ProgressFragment", "Error resetting combo state in onDestroy", e)
        }
    }

    // ===== MenuFragmentBase Abstract Methods Implementation =====

    override fun getMenuItems(): List<MenuItem> {
        return listOf(
                MenuItem(
                        "load",
                        getString(R.string.menu_load_state),
                        action = MenuAction.LOAD_STATE
                ),
                MenuItem(
                        "save",
                        getString(R.string.menu_save_state),
                        action = MenuAction.SAVE_STATE
                ),
                MenuItem(
                        "manage",
                        getString(R.string.manage_saves_title),
                        action = MenuAction.MANAGE_SAVES
                ),
                MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        when (item.action) {
            MenuAction.LOAD_STATE -> loadState.performClick()
            MenuAction.SAVE_STATE -> saveState.performClick()
            MenuAction.MANAGE_SAVES -> manageSaves.performClick()
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

        // PHASE 3: Skip old navigation system logic (permanently enabled)
        android.util.Log.d(
                "ProgressFragment",
                "[RESUME] ✅ New navigation system active - skipping old MenuState checks"
        )
        return
    }

    companion object {
        private const val TAG = "ProgressMenu"

        fun newInstance(): ProgressFragment {
            return ProgressFragment()
        }
    }
}
