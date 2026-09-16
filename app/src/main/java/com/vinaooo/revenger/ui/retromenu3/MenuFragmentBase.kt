package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.viewmodels.InputViewModel

/**
 * Abstract base class for all menu fragments in the RetroMenu3 system. Eliminates duplicate code
 * by providing default implementations for navigation and selection state management.
 *
 * **Multi-Input Architecture (Phase 3+)**:
 * - Supports gamepad, keyboard and touch simultaneously
 * - Navigation via unified MenuFragment interface
 * - Touch with 100ms delay to prevent accidental activation (TOUCH_ACTIVATION_DELAY_MS)
 *
 * **Fragments extending this class must implement**:
 * - `getMenuItems()`: Returns standardized list of items
 * - `performNavigateUp()`: Fragment-specific up navigation logic
 * - `performNavigateDown()`: Fragment-specific down navigation logic
 * - `performConfirm()`: Fragment-specific confirm logic
 * - `performBack()`: Fragment-specific back logic
 * - `updateSelectionVisualInternal()`: Updates selection visuals
 *
 * **Phase 3.3**: Integrated touch system with immediate highlight + activation delay.
 *
 * **FIX ERROR 1 - Phase 4.2**: onPause() clears input state to prevent event leakage
 * during fragment transitions (B/BackSpace residual after popBackStack).
 *
 * @see MenuFragment Unified navigation interface
 * @see MenuItem Data model for menu items
 * @see MenuAction Standardized menu actions
 */
abstract class MenuFragmentBase : Fragment(), MenuFragment {

    /** Currently selected index */
    private var _currentSelectedIndex = 0

    /** Retorna a lista padronizada de itens do menu */
    abstract override fun getMenuItems(): List<MenuItem>

    /** Abstract method to handle menu item selection */
    abstract override fun onMenuItemSelected(item: MenuItem)

    /** Abstract method for fragment-specific up navigation */
    protected abstract fun performNavigateUp()

    /** Abstract method for fragment-specific down navigation */
    protected abstract fun performNavigateDown()

    /** Abstract method for fragment-specific confirm */
    protected abstract fun performConfirm()

    /** Abstract method for fragment-specific back */
    protected abstract fun performBack(): Boolean

    /** Abstract method to update selection visual */
    protected abstract fun updateSelectionVisualInternal()

    // ========== LIFECYCLE HOOKS ==========

    /**
     * FIX ERROR 1: Clears input state when pausing fragment to avoid event leakage.
     *
     * Problem: Holding B/BackSpace in a submenu, the KEY_DOWN event closes the submenu via
     * popBackStack(), but the corresponding KEY_UP was processed in the main menu, causing
     * unintended closure.
     *
     * Solution: Clear all debounce timestamps, keyLog, and flags when pausing a fragment,
     * ensuring the next fragment starts with a clean state.
     */
    override fun onPause() {
        super.onPause()
        try {
            // Acessar InputViewModel e limpar estado de input de forma segura
            val inputViewModel = ViewModelProvider(requireActivity())[InputViewModel::class.java]
            inputViewModel.getControllerInput().clearPendingInputsPreserveHeld()

            Log.d(
                    "MenuFragmentBase",
                    "[LIFECYCLE] onPause() - clearPendingInputsPreserveHeld() for ${this::class.simpleName}"
            )
        } catch (e: Exception) {
            Log.e(
                    "MenuFragmentBase",
                    "[LIFECYCLE] Failed to clear pending inputs in onPause()",
                    e
            )
        }
    }

    // ========== MenuFragment INTERFACE IMPLEMENTATION ==========

    override fun onNavigateUp(): Boolean {
        Log.d("MenuBase", "[NAV] ↑ Navigate Up triggered")
        performNavigateUp()
        return true
    }

    override fun onNavigateDown(): Boolean {
        Log.d(
                "MenuBase",
                "[NAV] ↓ onNavigateDown triggered - calling performNavigateDown"
        )
        val result = performNavigateDown()
        Log.d("MenuBase", "[NAV] ↓ onNavigateDown completed - result=$result")
        return true
    }

    override fun onConfirm(): Boolean {
        Log.d("MenuBase", "[ACTION] ✓ Confirm triggered")
        performConfirm()
        return true
    }

    override fun onBack(): Boolean {
        Log.d("MenuBase", "[ACTION] ← Back triggered")
        return performBack()
    }
    override fun getCurrentSelectedIndex(): Int = _currentSelectedIndex

    override fun setSelectedIndex(index: Int) {
        val menuItems = getMenuItems()
        if (index in 0 until menuItems.size) {
            _currentSelectedIndex = index

            // PHASE 4: Log when a menu item is selected (yellow)
            val itemTitle = if (index < menuItems.size) menuItems[index].title else "UNKNOWN"
            Log.d(
                    "MenuBase",
                    "[MENU-SELECTION] ✅ Item selected (YELLOW): index=$index, title='$itemTitle'"
            )

            updateSelectionVisualInternal()
        } else {
            Log.w(
                    "MenuFragmentBase",
                    "Invalid index $index, valid range: 0..${menuItems.size-1}"
            )
        }
    }

    // ========== HELPER METHODS ==========

    /** Circular navigation up (last item wraps to first) */
    protected fun navigateUpCircular(itemsCount: Int) {
        _currentSelectedIndex =
                if (_currentSelectedIndex > 0) {
                    _currentSelectedIndex - 1
                } else {
                    itemsCount - 1
                }
        updateSelectionVisualInternal()
    }

    /** Circular navigation down (first item wraps to last) */
    protected fun navigateDownCircular(itemsCount: Int) {
        _currentSelectedIndex =
                if (_currentSelectedIndex < itemsCount - 1) {
                    _currentSelectedIndex + 1
                } else {
                    0
                }
        updateSelectionVisualInternal()
    }

    /** Validates that the current index is valid */
    protected fun isValidSelection(itemsCount: Int): Boolean {
        return _currentSelectedIndex in 0 until itemsCount
    }

    /** Resets selection to the first item */
    protected fun resetSelection() {
        _currentSelectedIndex = 0
        updateSelectionVisualInternal()
    }

    /**
     * Applies per-item selected/unselected visuals to a fixed list of item views.
     *
     * Iterates [items] and invokes [onSelected] for the view at [selectedIndex] and
     * [onUnselected] for every other view. This replaces the iterate-and-branch loop that
     * subclasses previously duplicated in each `updateSelectionVisualInternal()` override;
     * subclasses keep their own selected/unselected visual treatment (color, glow, scale,
     * margins, etc.) in the lambdas passed here.
     *
     * @param items the menu item views, in display order
     * @param selectedIndex the currently selected index
     * @param onSelected applied to the view at [selectedIndex]
     * @param onUnselected applied to every other view
     */
    protected fun <T : View> applySelectionVisuals(
            items: List<T>,
            selectedIndex: Int,
            onSelected: (T) -> Unit,
            onUnselected: (T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            if (index == selectedIndex) onSelected(item) else onUnselected(item)
        }
    }

    /**
     * Indexed variant of [applySelectionVisuals] for fragments whose per-item visual treatment
     * needs the item's index (e.g. to derive a 1-based slot number, or to look up related state
     * keyed by position). Behaves identically otherwise: [onSelected] is invoked for the item at
     * [selectedIndex], [onUnselected] for every other item, both receiving the item's index.
     *
     * @param items the menu item views, in display order
     * @param selectedIndex the currently selected index
     * @param onSelected applied to the view at [selectedIndex], with its index
     * @param onUnselected applied to every other view, with its index
     */
    protected fun <T : View> applySelectionVisuals(
            items: List<T>,
            selectedIndex: Int,
            onSelected: (T, Int) -> Unit,
            onUnselected: (T, Int) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            if (index == selectedIndex) onSelected(item, index) else onUnselected(item, index)
        }
    }

    /**
     * Applies configurable layout proportions to the menu (horizontal and vertical). Should be
     * called in onViewCreated of submenus.
     *
     * @param view The root view of the inflated menu
     */
    protected fun applyLayoutProportions(view: android.view.View) {
        com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig.applyAllProportionsToMenuLayout(
                view
        )
    }

    companion object {
        /** Delay for touch item activation in milliseconds */
        const val TOUCH_ACTIVATION_DELAY_MS = 100L
    }
}
