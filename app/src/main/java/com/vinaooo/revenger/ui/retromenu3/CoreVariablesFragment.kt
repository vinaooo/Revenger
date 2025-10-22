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

/** CoreVariablesFragment - Dedicated submenu for displaying core variables */
class CoreVariablesFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var coreVariablesMenuContainer: LinearLayout
    private lateinit var backVariables: RetroCardView

    // Menu title
    private lateinit var coreVariablesMenuTitle: TextView

    // Core variables display
    private lateinit var coreVariablesLabel: TextView
    private lateinit var coreVariablesValue: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Menu option titles for color control
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface CoreVariablesListener {
        fun onBackToSettings()
    }

    private var coreVariablesListener: CoreVariablesListener? = null

    fun setCoreVariablesListener(listener: CoreVariablesListener) {
        this.coreVariablesListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.core_variables_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        updateCoreVariablesDisplay()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation
    }

    private fun setupViews(view: View) {
        // Main container
        coreVariablesMenuContainer = view.findViewById(R.id.core_variables_menu_container)

        // Menu title
        coreVariablesMenuTitle = view.findViewById(R.id.core_variables_menu_title)

        // Core variables display
        coreVariablesLabel = view.findViewById(R.id.core_variables_label)
        coreVariablesValue = view.findViewById(R.id.core_variables_value)

        // Menu items
        backVariables = view.findViewById(R.id.core_variables_back)

        // Initialize ordered list of menu items
        menuItems = listOf(backVariables)

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        backVariables.setUseBackgroundColor(false)

        // Initialize menu option titles
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                coreVariablesMenuTitle,
                coreVariablesLabel,
                coreVariablesValue,
                backTitle,
                selectionArrowBack
        )
    }

    private fun setupClickListeners() {
        backVariables.setOnClickListener {
            // Return to settings menu by calling listener method
            coreVariablesListener?.onBackToSettings()
        }
    }

    private fun updateCoreVariablesDisplay() {
        // Set labels
        coreVariablesLabel.text = "core variables:\u00A0"

        // Get core variables from config
        val coreVariables = resources.getString(R.string.config_variables)
        coreVariablesValue.text = if (coreVariables.isNotEmpty()) coreVariables else "None"

        // Apply configured capitalization
        applyConfiguredCapitalization(coreVariablesValue)
    }

    private fun applyConfiguredCapitalization(textView: TextView) {
        FontUtils.applyTextCapitalization(requireContext(), textView)
    }

    override fun performNavigateUp() {
        navigateUpCircular(menuItems.size)
    }

    override fun performNavigateDown() {
        navigateDownCircular(menuItems.size)
    }

    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        when (selectedIndex) {
            0 -> backVariables.performClick() // Back
        }
    }

    override fun performBack(): Boolean {
        // Return to settings menu
        coreVariablesListener?.onBackToSettings()
        return true
    }

    override fun updateSelectionVisualInternal() {
        val currentIndex = getCurrentSelectedIndex()

        // Update selection arrows visibility
        selectionArrowBack.visibility = if (currentIndex == 0) View.VISIBLE else View.GONE

        // Update title colors based on selection
        val context = requireContext()
        backTitle.setTextColor(
                if (currentIndex == 0) {
                    FontUtils.getSelectedTextColor(context)
                } else {
                    FontUtils.getUnselectedTextColor(context)
                }
        )
    }

    override fun getMenuItems(): List<MenuItem> {
        return listOf(MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK))
    }

    override fun onMenuItemSelected(item: MenuItem) {
        when (item.action) {
            MenuAction.BACK -> backVariables.performClick()
            else -> {
                /* Ignore other actions */
            }
        }
    }

    companion object {
        private const val TAG = "CoreVariablesMenu"

        fun newInstance(): CoreVariablesFragment {
            return CoreVariablesFragment()
        }
    }
}
