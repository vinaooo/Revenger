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

        // Core variables display - TEMP: Comentado para testes de alinhamento
        // coreVariablesLabel = view.findViewById(R.id.core_variables_label)
        // coreVariablesValue = view.findViewById(R.id.core_variables_value)

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
        // TEMP: Core variables display comentado
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                coreVariablesMenuTitle,
                // coreVariablesLabel,
                // coreVariablesValue,
                backTitle,
                selectionArrowBack
        )

        // Apply configured capitalization to menu title and back button
        applyConfiguredCapitalization(coreVariablesMenuTitle)
        applyConfiguredCapitalization(backTitle)
    }

    private fun setupClickListeners() {
        backVariables.setOnClickListener {
            // Return to settings menu by calling listener method
            coreVariablesListener?.onBackToSettings()
        }
    }

    private fun updateCoreVariablesDisplay() {
        // TEMP: Core variables display comentado para testes de alinhamento
        /*
        // Set labels with configured capitalization
        coreVariablesLabel.text = "core variables:\u00A0"
        applyConfiguredCapitalization(coreVariablesLabel)

        // Get core variables from config and replace commas with newlines for better readability
        val coreVariables = resources.getString(R.string.config_variables)
        val formattedVariables =
                if (coreVariables.isNotEmpty()) {
                    coreVariables.replace(",", "\n")
                } else {
                    "None"
                }
        coreVariablesValue.text = formattedVariables
        applyConfiguredCapitalization(coreVariablesValue)
        */

        // Apply capitalization to menu title
        applyConfiguredCapitalization(coreVariablesMenuTitle)
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
            0 -> {
                // Return to settings menu by calling listener method
                coreVariablesListener?.onBackToSettings()
            }
        }
    }

    override fun performBack(): Boolean {
        android.util.Log.d("CoreVariablesFragment", "[BACK] performBack called - calling listener")
        // Return to settings menu - listener handles popBackStack
        coreVariablesListener?.onBackToSettings()
        android.util.Log.d("CoreVariablesFragment", "[BACK] performBack completed")
        return true
    }

    override fun updateSelectionVisualInternal() {
        val currentIndex = getCurrentSelectedIndex()

        // Update selection arrows visibility and color
        val context = requireContext()
        selectionArrowBack.visibility = if (currentIndex == 0) View.VISIBLE else View.GONE
        selectionArrowBack.setTextColor(
                if (currentIndex == 0) {
                    FontUtils.getSelectedTextColor(context)
                } else {
                    FontUtils.getUnselectedTextColor(context)
                }
        )

        // Update title colors based on selection
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

    override fun onResume() {
        super.onResume()
        // Ensure fragment is fully resumed before re-registering
        // This prevents timing issues when returning from back stack navigation
        view?.post {
            if (isAdded && isResumed) {
                android.util.Log.d(
                        "CoreVariablesFragment",
                        "[RESUME] 🔧 onResume: Re-registering CoreVariablesFragment after back stack return"
                )

                // CRITICAL: Re-configure listener after rotation
                android.util.Log.d(
                        "CoreVariablesFragment",
                        "[RESUME] 🔗 Reconfiguring listener after recreation"
                )
                try {
                    val parentFragment = parentFragment
                    if (parentFragment is CoreVariablesListener) {
                        setCoreVariablesListener(parentFragment)
                        android.util.Log.d(
                                "CoreVariablesFragment",
                                "[RESUME] ✅ Listener configured successfully"
                        )
                    } else {
                        android.util.Log.e(
                                "CoreVariablesFragment",
                                "[RESUME] ❌ Parent fragment is not CoreVariablesListener!"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                            "CoreVariablesFragment",
                            "[RESUME] ❌ Error configuring listener",
                            e
                    )
                }

                viewModel.registerCoreVariablesFragment(this)

                // Restaurar foco no primeiro item
                val firstFocusable = view?.findViewById<android.view.View>(R.id.core_variables_back)
                firstFocusable?.requestFocus()
                android.util.Log.d(
                        "CoreVariablesFragment",
                        "[FOCUS] Foco restaurado no primeiro item"
                )
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
