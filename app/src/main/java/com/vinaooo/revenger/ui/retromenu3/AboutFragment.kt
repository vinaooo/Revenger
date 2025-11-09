package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** AboutFragment - About submenu showing project information */
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

    // Callback interface
    interface AboutListener {
        fun onAboutBackToMainMenu()
    }

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

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        // PHASE 3: Register with NavigationController for new navigation system
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            viewModel.navigationController?.registerFragment(this, getMenuItems().size)
            android.util.Log.d(
                    TAG,
                    "[NAVIGATION] AboutFragment registered with ${getMenuItems().size} items"
            )
        }
    }

    override fun onDestroyView() {
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    TAG,
                    "[NAVIGATION] AboutFragment onDestroyView - keeping registration"
            )
        }
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

        // Aplicar capitalização configurada a TODOS os textos (título, labels, títulos dos botões)
        val capitalizationStyle =
                resources.getInteger(com.vinaooo.revenger.R.integer.retro_menu3_text_capitalization)

        // Aplicar capitalização ao título
        val titleText = aboutTitle.text.toString()
        val capitalizedTitle =
                when (capitalizationStyle) {
                    1 -> {
                        // Primeira letra maiúscula
                        if (titleText.isNotEmpty()) {
                            titleText.substring(0, 1).uppercase() + titleText.substring(1)
                        } else {
                            titleText
                        }
                    }
                    2 -> titleText.uppercase() // Tudo maiúsculo
                    else -> titleText // Normal (padrão)
                }
        if (capitalizedTitle != titleText) {
            aboutTitle.text = capitalizedTitle
        }

        // Aplicar capitalização aos textos informativos (já incluem label + value)
        val infoViews = arrayOf(projectNameInfo, romNameInfo, coreNameInfo)
        infoViews.forEach { infoView ->
            val infoText = infoView.text.toString()
            val capitalizedInfo =
                    when (capitalizationStyle) {
                        1 -> {
                            // Primeira letra maiúscula
                            if (infoText.isNotEmpty()) {
                                infoText.substring(0, 1).uppercase() + infoText.substring(1)
                            } else {
                                infoText
                            }
                        }
                        2 -> infoText.uppercase() // Tudo maiúsculo
                        else -> infoText // Normal (padrão)
                    }
            if (capitalizedInfo != infoText) {
                infoView.text = capitalizedInfo
            }
        }

        // Aplicar capitalização aos títulos dos botões do menu
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

    /** Aplica a capitalização configurada a um TextView */
    private fun applyConfiguredCapitalization(textView: android.widget.TextView) {
        val capitalizationStyle =
                resources.getInteger(com.vinaooo.revenger.R.integer.retro_menu3_text_capitalization)
        val originalText = textView.text.toString()
        val capitalizedText =
                when (capitalizationStyle) {
                    1 -> {
                        // Primeira letra maiúscula
                        if (originalText.isNotEmpty()) {
                            originalText.substring(0, 1).uppercase() + originalText.substring(1)
                        } else {
                            originalText
                        }
                    }
                    2 -> originalText.uppercase() // Tudo maiúsculo
                    else -> originalText // Normal (padrão)
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
        val romName = resources.getString(R.string.config_rom)
        romNameInfo.text = "$romLabel $romName"
        applyConfiguredCapitalization(romNameInfo)

        // Core name from config - combine label and value
        val coreLabel = resources.getString(R.string.about_core_name)
        val coreName = resources.getString(R.string.config_core)
        coreNameInfo.text = "$coreLabel $coreName"
        applyConfiguredCapitalization(coreNameInfo)
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
        menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                android.util.Log.d(
                        TAG,
                        "[TOUCH] About item $index clicked - routing through NavigationController"
                )

                // PHASE 3.3b: Focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                viewModel.navigationController?.selectItem(index)

                // 2. After 100ms delay, activate item
                it.postDelayed(
                        {
                            android.util.Log.d(
                                    TAG,
                                    "[TOUCH] Activating About item $index after delay"
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
        backAbout.setOnClickListener {
            // Return to main menu by calling listener method (same as pressing B)
            aboutListener?.onAboutBackToMainMenu()
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

    /** Confirm selection - Execute actions DIRECTLY (não usar performClick) */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] About menu: CONFIRM on index $selectedIndex")

        when (selectedIndex) {
            0 -> {
                // Back to main menu - Execute action directly
                android.util.Log.d(TAG, "[ACTION] About menu: Back to main menu selected")
                performBack()
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
        // PHASE 3: Use NavigationController when new system is active
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    "AboutFragment",
                    "[BACK] Using new navigation system - calling viewModel.navigationController.navigateBack()"
            )
            val success = viewModel.navigationController?.navigateBack() ?: false
            android.util.Log.d(
                    "AboutFragment",
                    "[BACK] NavigationController.navigateBack() returned: $success"
            )
            return success
        } else {
            // Old system: Return to main menu
            viewModel.dismissAboutMenu()
            return true
        }
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
                                    R.color.retro_menu3_selected_color
                            )
                    else
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.retro_menu3_normal_color
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
                                    R.color.retro_menu3_selected_color
                            )
                    else
                            androidx.core.content.ContextCompat.getColor(
                                    context,
                                    R.color.retro_menu3_normal_color
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

        // PHASE 3: Skip old navigation system logic when new system is active
        if (com.vinaooo.revenger.FeatureFlags.USE_NEW_NAVIGATION_SYSTEM) {
            android.util.Log.d(
                    "AboutFragment",
                    "[RESUME] ✅ New navigation system active - skipping old MenuState checks"
            )
            return
        }

        // Check 1: Don't register if being removed
        if (isRemoving) {
            android.util.Log.d(
                    "AboutFragment",
                    "[RESUME] ⚠️ Fragment is being removed - skipping registration"
            )
            return
        }

        // Check 2: Don't register if MenuState is not ABOUT_MENU
        val currentState = viewModel.getMenuManager().getCurrentState()
        if (currentState != MenuState.ABOUT_MENU) {
            android.util.Log.d(
                    "AboutFragment",
                    "[RESUME] ⚠️ MenuState is $currentState (not ABOUT) - skipping registration"
            )
            return
        }

        // Ensure fragment is fully resumed before re-registering
        // This prevents timing issues when returning from back stack navigation
        view?.post {
            if (isAdded && isResumed) {
                android.util.Log.d(
                        "AboutFragment",
                        "[RESUME] 📋 Registering immediately (isAdded=true, state=ABOUT_MENU)"
                )
                viewModel.registerAboutFragment(this)

                // Restaurar foco no primeiro item
                val firstFocusable = view?.findViewById<android.view.View>(R.id.about_back)
                firstFocusable?.requestFocus()
                android.util.Log.d("AboutFragment", "[FOCUS] Foco restaurado no primeiro item")

                android.util.Log.d(
                        "AboutFragment",
                        "[RESUME] 📋 ========== ABOUT FRAGMENT ON RESUME END =========="
                )
            } else {
                android.util.Log.d(
                        "AboutFragment",
                        "[RESUME] 📋 Fragment not ready: isAdded=$isAdded, isResumed=$isResumed"
                )
            }
        }
    }
    companion object {
        private const val TAG = "AboutFragment"

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}
