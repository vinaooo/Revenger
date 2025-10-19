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

/** ExitFragment - Exit options menu with visual identical to RetroMenu3 */
class ExitFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var exitMenuContainer: LinearLayout
    private lateinit var saveAndExit: RetroCardView
    private lateinit var exitWithoutSave: RetroCardView
    private lateinit var backExitMenu: RetroCardView

    // Menu title
    private lateinit var exitMenuTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Menu option titles for color control
    private lateinit var saveAndExitTitle: TextView
    private lateinit var exitWithoutSaveTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSaveAndExit: TextView
    private lateinit var selectionArrowExitWithoutSave: TextView
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface ExitListener {
        fun onBackToMainMenu()
    }

    private var exitListener: ExitListener? = null

    fun setExitListener(listener: ExitListener) {
        this.exitListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.exit_menu, container, false)
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
    }

    private fun setupViews(view: View) {
        // Main container
        exitMenuContainer = view.findViewById(R.id.exit_menu_container)

        // Menu title
        exitMenuTitle = view.findViewById(R.id.exit_menu_title)

        // Menu items
        saveAndExit = view.findViewById(R.id.exit_menu_option_a)
        exitWithoutSave = view.findViewById(R.id.exit_menu_option_b)
        backExitMenu = view.findViewById(R.id.exit_menu_option_c)

        // Initialize ordered list of menu items
        menuItems = listOf(saveAndExit, exitWithoutSave, backExitMenu)

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        saveAndExit.setUseBackgroundColor(false)
        exitWithoutSave.setUseBackgroundColor(false)
        backExitMenu.setUseBackgroundColor(false)

        // Initialize menu option titles
        saveAndExitTitle = view.findViewById(R.id.option_a_title)
        exitWithoutSaveTitle = view.findViewById(R.id.option_b_title)
        backTitle = view.findViewById(R.id.option_c_title)

        // Initialize selection arrows
        selectionArrowSaveAndExit = view.findViewById(R.id.selection_arrow_option_a)
        selectionArrowExitWithoutSave = view.findViewById(R.id.selection_arrow_option_b)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_option_c)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                exitMenuTitle,
                saveAndExitTitle,
                exitWithoutSaveTitle,
                backTitle,
                selectionArrowSaveAndExit,
                selectionArrowExitWithoutSave,
                selectionArrowBack
        )

        // Aplicar capitalização configurada aos textos
        FontUtils.applyTextCapitalization(
                requireContext(),
                exitMenuTitle,
                saveAndExitTitle,
                exitWithoutSaveTitle,
                backTitle
        )
    }

    private fun setupClickListeners() {
        saveAndExit.setOnClickListener {
            // Save and Exit - Close menu, restore frameSpeed, then save and exit
            // A) Close menu first
            viewModel.dismissAllMenus()

            // B) Restore frameSpeed to correct value from sharedPreferences
            viewModel.restoreGameSpeedFromPreferences()

            // C) Apply existing functionality (save state and exit)
            viewModel.saveStateCentralized(
                    onComplete = {
                        // After saving, exit the game completely
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
            )
        }

        exitWithoutSave.setOnClickListener {
            // Exit without Save - Close menu, restore frameSpeed, then exit without saving
            // A) Close menu first
            viewModel.dismissAllMenus()

            // B) Restore frameSpeed to correct value from sharedPreferences
            viewModel.restoreGameSpeedFromPreferences()

            // C) Apply existing functionality (exit without saving)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        backExitMenu.setOnClickListener {
            // Return to main menu by calling listener method (same as pressing B)
            exitListener?.onBackToMainMenu()
        }
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateUpCircular(menuItems.size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] Exit menu: UP navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateDownCircular(menuItems.size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] Exit menu: DOWN navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Confirm current selection */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] Exit menu: CONFIRM on index $selectedIndex")
        when (selectedIndex) {
            0 -> {
                android.util.Log.d(TAG, "[ACTION] Exit menu: Save and Exit selected")
                saveAndExit.performClick() // Save and Exit
            }
            1 -> {
                android.util.Log.d(TAG, "[ACTION] Exit menu: Exit without Save selected")
                exitWithoutSave.performClick() // Exit without Save
            }
            2 -> {
                android.util.Log.d(TAG, "[ACTION] Exit menu: Back to main menu selected")
                backExitMenu.performClick() // Back
            }
            else ->
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Exit menu: Invalid selection index $selectedIndex"
                    )
        }
    }

    /** Back action */
    override fun performBack(): Boolean {
        // For exit submenu, back should go to main menu
        backExitMenu.performClick()
        return true
    }

    /** Update selection visual - specific implementation for ExitFragment */
    override fun updateSelectionVisualInternal() {
        val selectedIndex = getCurrentSelectedIndex()

        // Update each menu item state based on selection
        menuItems.forEachIndexed { index, item ->
            if (index == selectedIndex) {
                // Item selecionado - usar estado SELECTED do RetroCardView
                item.setState(RetroCardView.State.SELECTED)
            } else {
                // Item não selecionado - usar estado NORMAL do RetroCardView
                item.setState(RetroCardView.State.NORMAL)
            }
        }

        // Control text colors based on selection
        saveAndExitTitle.setTextColor(
                if (getCurrentSelectedIndex() == 0)
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
        exitWithoutSaveTitle.setTextColor(
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

        // Save and Exit
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowSaveAndExit.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowSaveAndExit.visibility = View.VISIBLE
            (selectionArrowSaveAndExit.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowSaveAndExit.visibility = View.GONE
        }

        // Exit without Save
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowExitWithoutSave.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowExitWithoutSave.visibility = View.VISIBLE
            (selectionArrowExitWithoutSave.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowExitWithoutSave.visibility = View.GONE
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
        exitMenuContainer.requestLayout()
    }
    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            viewModel.clearControllerKeyLog()
        } catch (e: Exception) {
            android.util.Log.w("ExitFragment", "Error resetting combo state in onDestroy", e)
        }
    }

    // ===== MenuFragmentBase Abstract Methods Implementation =====

    override fun getMenuItems(): List<MenuItem> {
        return listOf(
                MenuItem(
                        "save_exit",
                        getString(R.string.exit_menu_option_a),
                        action = MenuAction.SAVE_AND_EXIT
                ),
                MenuItem(
                        "exit_no_save",
                        getString(R.string.exit_menu_option_b),
                        action = MenuAction.EXIT
                ),
                MenuItem("back", getString(R.string.exit_menu_option_c), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Use new MenuAction system, but fallback to old click listeners for compatibility
        when (item.action) {
            MenuAction.SAVE_AND_EXIT -> saveAndExit.performClick()
            MenuAction.EXIT -> exitWithoutSave.performClick()
            MenuAction.BACK -> backExitMenu.performClick()
            else -> {
                /* Ignore other actions */
            }
        }
    }

    companion object {
        private const val TAG = "ExitMenu"

        fun newInstance(): ExitFragment {
            return ExitFragment()
        }
    }
}
