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
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** ExitFragment - Exit options menu with visual identical to RetroMenu3 */
class ExitFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var exitMenuContainer: LinearLayout
    private lateinit var saveAndExit: MaterialCardView
    private lateinit var exitWithoutSave: MaterialCardView
    private lateinit var backExitMenu: MaterialCardView

    // Menu title
    private lateinit var exitMenuTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>

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
        android.util.Log.d("ExitFragment", "onViewCreated: ExitFragment view created")

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        android.util.Log.d("ExitFragment", "onViewCreated: ExitFragment setup completed")
        // REMOVED: No longer closes when touching the sides
        // Menu only closes when selecting Back
    }

    /** Recursively set z=0 and elevation=0 on all views to ensure menu stays below gamepad. */
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
        exitMenuContainer = view.findViewById(R.id.exit_menu_container)

        // Menu title
        exitMenuTitle = view.findViewById(R.id.exit_menu_title)

        // Menu items
        saveAndExit = view.findViewById(R.id.exit_menu_option_a)
        exitWithoutSave = view.findViewById(R.id.exit_menu_option_b)
        backExitMenu = view.findViewById(R.id.exit_menu_option_c)

        // Initialize ordered list of menu items
        menuItems = listOf(saveAndExit, exitWithoutSave, backExitMenu)

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
        applyArcadeFontToViews()
    }

    private fun applyArcadeFontToViews() {
        val context = requireContext()

        // Apply font to all text views in the exit menu
        FontUtils.applyArcadeFont(
                context,
                exitMenuTitle,
                saveAndExitTitle,
                exitWithoutSaveTitle,
                backTitle,
                selectionArrowSaveAndExit,
                selectionArrowExitWithoutSave,
                selectionArrowBack
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
            viewModel.saveStateCentralized {
                // After saving, exit the game completely
                android.os.Process.killProcess(android.os.Process.myPid())
            }
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
            // Back - Return to main menu
            viewModel.dismissExit()
        }
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        navigateUpCircular(menuItems.size)
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        navigateDownCircular(menuItems.size)
        updateSelectionVisualInternal()
    }

    /** Confirm current selection */
    override fun performConfirm() {
        when (getCurrentSelectedIndex()) {
            0 -> saveAndExit.performClick() // Save and Exit
            1 -> exitWithoutSave.performClick() // Exit without Save
            2 -> backExitMenu.performClick() // Back
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
        menuItems.forEach { item ->
            // Removed: background color of individual cards
            // Selection now indicated only by yellow text and arrows
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Control text colors based on selection
        saveAndExitTitle.setTextColor(
                if (getCurrentSelectedIndex() == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        exitWithoutSaveTitle.setTextColor(
                if (getCurrentSelectedIndex() == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (getCurrentSelectedIndex() == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Save and Exit
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowSaveAndExit.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSaveAndExit.visibility = View.VISIBLE
            (selectionArrowSaveAndExit.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSaveAndExit.visibility = View.GONE
        }

        // Exit without Save
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowExitWithoutSave.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowExitWithoutSave.visibility = View.VISIBLE
            (selectionArrowExitWithoutSave.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowExitWithoutSave.visibility = View.GONE
        }

        // Back
        if (getCurrentSelectedIndex() == 2) {
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
        exitMenuContainer.requestLayout()
    }    /** Public method to dismiss the menu from outside */
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
        fun newInstance(): ExitFragment {
            return ExitFragment()
        }
    }
}
