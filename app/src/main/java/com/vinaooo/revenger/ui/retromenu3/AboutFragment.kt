package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Submenu fragment for About (Information).
 *
 * **Features**:
 * - Displays project information (name, version)
 * - Shows current ROM name
 * - Shows LibRetro core in use
 * - Back: Returns to main menu
 *
 * **Multi-Input Architecture (Phase 3+)**:
 * - Gamepad/Keyboard: Simplified navigation (only 1 Back button)
 * - Touch: Highlight + 100ms activation delay
 *
 * **Visual**:
 * - Informational design with Material Design 3
 * - Reads config.xml for dynamic data
 * - Typography optimized for readability
 *
 * **Phase 3.3**: Cleaned up 8 lines of legacy code.
 *
 * @see MenuFragmentBase Base class with unified navigation
 * @see GameActivityViewModel ViewModel para dados da ROM/Core
 */
class AboutFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var aboutContainer: LinearLayout
    private lateinit var backAbout: RetroCardView

    // Menu title
    private lateinit var aboutTitle: TextView

    // Information display views
    private lateinit var projectNameInfo: TextView
    private lateinit var romNameInfo: TextView
    private lateinit var coreNameInfo: TextView

    // Menu option titles for color control
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowBack: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    private var aboutListener: AboutListener? = null

    fun setAboutListener(listener: AboutListener) {
        this.aboutListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        android.util.Log.d(TAG, "[DEBUG] AboutFragment.onCreateView called")
        return inflater.inflate(R.layout.about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply configurable layout proportions
        applyLayoutProportions(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        // PHASE 3: Register with NavigationController for new navigation system
        viewModel.navigationController?.registerFragment(this, getMenuItems().size)
        android.util.Log.d(
                TAG,
                "[NAVIGATION] AboutFragment registered with ${getMenuItems().size} items"
        )
    }

    override fun onDestroyView() {
        android.util.Log.d(TAG, "[NAVIGATION] AboutFragment onDestroyView - keeping registration")
        super.onDestroyView()
    }

    private fun setupViews(view: View) {
        // Main container
        aboutContainer = view.findViewById(R.id.about_container)

        // Menu title
        aboutTitle = view.findViewById(R.id.about_title)

        // Information display views
        projectNameInfo = view.findViewById(R.id.project_name_info)
        romNameInfo = view.findViewById(R.id.rom_name_info)
        coreNameInfo = view.findViewById(R.id.core_name_info)

        // Menu items
        backAbout = view.findViewById(R.id.about_back)

        // Initialize ordered list of menu items
        menuItems = listOf(backAbout)

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        backAbout.setUseBackgroundColor(false)

        // Initialize menu option titles
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views FIRST
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                aboutTitle,
                projectNameInfo,
                romNameInfo,
                coreNameInfo,
                backTitle,
                selectionArrowBack
        )

        // Populate information with combined label and value
        populateAboutInfo()

        // Apply configured capitalization to ALL texts (title, labels, button titles)
        val capitalizationStyle =
                resources.getInteger(com.vinaooo.revenger.R.integer.rm_text_capitalization)

        // Apply capitalization to the title
        val titleText = aboutTitle.text.toString()
        val capitalizedTitle =
                when (capitalizationStyle) {
                    1 -> {
                        // First letter uppercase
                        if (titleText.isNotEmpty()) {
                            titleText.substring(0, 1).uppercase() + titleText.substring(1)
                        } else {
                            titleText
                        }
                    }
                    2 -> titleText.uppercase() // All uppercase
                    else -> titleText // Normal (default)
                }
        if (capitalizedTitle != titleText) {
            aboutTitle.text = capitalizedTitle
        }

        // Apply capitalization to informational texts (already include label + value)
        val infoViews = arrayOf(projectNameInfo, romNameInfo, coreNameInfo)
        infoViews.forEach { infoView ->
            val infoText = infoView.text.toString()
            val capitalizedInfo =
                    when (capitalizationStyle) {
                        1 -> {
                            // First letter uppercase
                            if (infoText.isNotEmpty()) {
                                infoText.substring(0, 1).uppercase() + infoText.substring(1)
                            } else {
                                infoText
                            }
                        }
                        2 -> infoText.uppercase() // All uppercase
                        else -> infoText // Normal (default)
                    }
            if (capitalizedInfo != infoText) {
                infoView.text = capitalizedInfo
            }
        }

        // Apply capitalization to menu button titles
        applyConfiguredCapitalization(backTitle)

        // DEBUG: Log info views after all processing
        android.util.Log.d(TAG, "[DEBUG] After all processing:")
        android.util.Log.d(TAG, "[DEBUG] projectNameInfo: '${projectNameInfo.text}'")
        android.util.Log.d(TAG, "[DEBUG] romNameInfo: '${romNameInfo.text}'")
        android.util.Log.d(TAG, "[DEBUG] coreNameInfo: '${coreNameInfo.text}'")
        android.util.Log.d(TAG, "[DEBUG] backTitle: '${backTitle.text}'")

        // Force layout update to ensure text rendering is correct
        projectNameInfo.invalidate()
        romNameInfo.invalidate()
        coreNameInfo.invalidate()
        backTitle.invalidate()
    }

    /** Applies configured capitalization to a TextView */
    private fun applyConfiguredCapitalization(textView: android.widget.TextView) {
        val capitalizationStyle =
                resources.getInteger(com.vinaooo.revenger.R.integer.rm_text_capitalization)
        val originalText = textView.text.toString()
        val capitalizedText =
                when (capitalizationStyle) {
                    1 -> {
                        // First letter uppercase
                        if (originalText.isNotEmpty()) {
                            originalText.substring(0, 1).uppercase() + originalText.substring(1)
                        } else {
                            originalText
                        }
                    }
                    2 -> originalText.uppercase() // All uppercase
                    else -> originalText // Normal (default)
                }
        if (capitalizedText != originalText) {
            textView.text = capitalizedText
        }
    }

    private fun populateAboutInfo() {
        // Project name - combine label and value
        val projectLabel = resources.getString(R.string.about_project_name)
        projectNameInfo.text = "$projectLabel Revenger"
        applyConfiguredCapitalization(projectNameInfo)

        // ROM name from config - combine label and value
        val romLabel = resources.getString(R.string.about_rom_name)
        val romName = resources.getString(R.string.conf_rom)
        romNameInfo.text = "$romLabel $romName"
        applyConfiguredCapitalization(romNameInfo)

        // Core name from config - combine label and value
        val coreLabel = resources.getString(R.string.about_core_name)
        val coreName = resources.getString(R.string.conf_core)
        coreNameInfo.text = "$coreLabel $coreName"
        applyConfiguredCapitalization(coreNameInfo)
    }

    private fun setupClickListeners() {
        // PHASE 3.3a: Route touch events through NavigationController
        android.util.Log.d(
                TAG,
                "[TOUCH] Using new navigation system - touch routed through NavigationController"
        )
        setupTouchNavigationSystem()
    }

    /**
     * PHASE 3.3: New touch navigation system using NavigationController. Touch events create
     * SelectItem + ActivateSelected after 100ms delay.
     */
    private fun setupTouchNavigationSystem() {
        menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                android.util.Log.d(
                        TAG,
                        "[TOUCH] About item $index clicked - routing through NavigationController"
                )

                // PHASE 3.3b: Focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                viewModel.navigationController?.selectItem(index)

                // 2. After TOUCH_ACTIVATION_DELAY_MS delay, activate item
                it.postDelayed(
                        {
                            android.util.Log.d(
                                    TAG,
                                    "[TOUCH] Activating About item $index after delay"
                            )
                            viewModel.navigationController?.activateItem()
                        },
                        MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS
                ) // MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS = focus-then-activate delay
            }
        }
    }

    private fun updateMenuState() {
        // No dynamic state to update for About menu
    }

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateUpCircular(getMenuItems().size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] About menu: UP navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateDownCircular(getMenuItems().size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] About menu: DOWN navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Confirm selection - Execute actions DIRECTLY (do not use performClick) */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] About menu: CONFIRM on index $selectedIndex")

        when (selectedIndex) {
            0 -> {
                // Back to main menu - Execute action directly
                android.util.Log.d(TAG, "[ACTION] About menu: Back to main menu selected")
                // Use NavigationController to navigate back (don't call performBack which returns false)
                viewModel.navigationController?.navigateBack()
            }
            else -> {
                android.util.Log.w(
                        TAG,
                        "[ACTION] About menu: Invalid selection index $selectedIndex"
                )
            }
        }
    }

    /** Handle back action */
    override fun performBack(): Boolean {
        // Return false to let NavigationEventProcessor handle the back navigation
        // Don't call navigateBack() here as it causes infinite recursion
        return false
    }

    /** Update selection visuals */
    override fun updateSelectionVisualInternal() {
        val selectedIndex = getCurrentSelectedIndex()
        val context = requireContext()

        // Update title colors
        val titles = arrayOf(backTitle)
        titles.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            title.setTextColor(
                    if (isSelected)
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.rm_selected_color
                            )
                    else
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.rm_normal_color
                            )
            )
        }

        // Update arrow visibility and color
        val arrows = arrayOf(selectionArrowBack)
        arrows.forEachIndexed { index, arrow ->
            val isSelected = index == selectedIndex
            arrow.visibility = if (isSelected) View.VISIBLE else View.GONE
            arrow.setTextColor(
                    if (isSelected)
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.rm_selected_color
                            )
                    else
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.rm_normal_color
                            )
            )
        }
    }

    override fun getMenuItems(): List<MenuItem> {
        return listOf(
                MenuItem("back", resources.getString(R.string.about_back), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        when (item.action) {
            MenuAction.BACK -> {
                aboutListener?.onAboutBackToMainMenu()
            }
            else -> {
                // Handle other actions if needed
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // PHASE 3: New navigation system active - skipping old MenuState checks
        android.util.Log.d(
                "AboutFragment",
                "[RESUME] ✅ New navigation system active - skipping old MenuState checks"
        )
    }
    companion object {
        private const val TAG = "AboutFragment"

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}
