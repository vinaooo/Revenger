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
    private lateinit var coreVariablesAbout: RetroCardView
    private lateinit var backAbout: RetroCardView

    // Menu title
    private lateinit var aboutTitle: TextView

    // Information display views
    private lateinit var projectNameLabel: TextView
    private lateinit var projectNameValue: TextView
    private lateinit var romNameLabel: TextView
    private lateinit var romNameValue: TextView
    private lateinit var coreNameLabel: TextView
    private lateinit var coreNameValue: TextView

    // Menu option titles for color control
    private lateinit var coreVariablesTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowCoreVariables: TextView
    private lateinit var selectionArrowBack: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Callback interface
    interface AboutListener {
        fun onAboutCoreVariablesSelected()
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
    }

    private fun setupViews(view: View) {
        // Main container
        aboutContainer = view.findViewById(R.id.about_container)

        // Menu title
        aboutTitle = view.findViewById(R.id.about_title)

        // Information display views
        projectNameLabel = view.findViewById(R.id.project_name_label)
        projectNameValue = view.findViewById(R.id.project_name_value)
        romNameLabel = view.findViewById(R.id.rom_name_label)
        romNameValue = view.findViewById(R.id.rom_name_value)
        coreNameLabel = view.findViewById(R.id.core_name_label)
        coreNameValue = view.findViewById(R.id.core_name_value)

        // Menu items
        coreVariablesAbout = view.findViewById(R.id.about_core_variables)
        backAbout = view.findViewById(R.id.about_back)

        // Initialize ordered list of menu items
        menuItems = listOf(coreVariablesAbout, backAbout)

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        coreVariablesAbout.setUseBackgroundColor(false)
        backAbout.setUseBackgroundColor(false)

        // Initialize menu option titles
        coreVariablesTitle = view.findViewById(R.id.core_variables_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowCoreVariables = view.findViewById(R.id.selection_arrow_core_variables)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views FIRST
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                aboutTitle,
                projectNameLabel,
                projectNameValue,
                romNameLabel,
                romNameValue,
                coreNameLabel,
                coreNameValue,
                backTitle,
                selectionArrowBack,
                coreVariablesTitle,
                selectionArrowCoreVariables
        )

        // Populate information
        populateAboutInfo()

        // Set label texts programmatically AFTER font application to ensure spacing is preserved
        // Use direct text assignment to avoid any resource loading issues
        projectNameLabel.text = "project:\u00A0"
        romNameLabel.text = "rom:\u00A0"
        coreNameLabel.text = "core:\u00A0"

        // Aplicar capitalização configurada a TODOS os textos (título e labels)
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

        // Aplicar capitalização aos labels (preservando os espaços não-quebráveis)
        val labels = arrayOf(projectNameLabel, romNameLabel, coreNameLabel)
        labels.forEach { label ->
            val labelText = label.text.toString()
            val capitalizedLabel =
                    when (capitalizationStyle) {
                        1 -> {
                            // Primeira letra maiúscula
                            if (labelText.isNotEmpty()) {
                                labelText.substring(0, 1).uppercase() + labelText.substring(1)
                            } else {
                                labelText
                            }
                        }
                        2 -> labelText.uppercase() // Tudo maiúsculo
                        else -> labelText // Normal (padrão)
                    }
            if (capitalizedLabel != labelText) {
                label.text = capitalizedLabel
            }
        }

        // DEBUG: Log labels after all processing
        android.util.Log.d(TAG, "[DEBUG] After all processing:")
        android.util.Log.d(TAG, "[DEBUG] projectNameLabel: '${projectNameLabel.text}'")
        android.util.Log.d(TAG, "[DEBUG] romNameLabel: '${romNameLabel.text}'")
        android.util.Log.d(TAG, "[DEBUG] coreNameLabel: '${coreNameLabel.text}'")

        // Force layout update to ensure text rendering is correct
        projectNameLabel.invalidate()
        romNameLabel.invalidate()
        coreNameLabel.invalidate()
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
        // Project name
        projectNameValue.text = "Revenger"
        applyConfiguredCapitalization(projectNameValue)

        // ROM name from config
        val romName = resources.getString(R.string.config_rom)
        romNameValue.text = romName
        applyConfiguredCapitalization(romNameValue)

        // Core name from config
        val coreName = resources.getString(R.string.config_core)
        coreNameValue.text = coreName
        applyConfiguredCapitalization(coreNameValue)
    }

    private fun setupClickListeners() {
        coreVariablesAbout.setOnClickListener {
            // Navigate to Core Variables submenu
            aboutListener?.onAboutCoreVariablesSelected()
        }

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

    /** Confirm selection */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] About menu: CONFIRM on index $selectedIndex")

        when (selectedIndex) {
            0 -> {
                // Core Variables selected
                android.util.Log.d(TAG, "[ACTION] About menu: Core Variables selected")
                aboutListener?.onAboutCoreVariablesSelected()
            }
            1 -> {
                // Back to main menu
                android.util.Log.d(TAG, "[ACTION] About menu: Back to main menu selected")
                viewModel.dismissAboutMenu()
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
        // Return to main menu
        viewModel.dismissAboutMenu()
        return true
    }

    /** Update selection visuals */
    override fun updateSelectionVisualInternal() {
        val selectedIndex = getCurrentSelectedIndex()
        val context = requireContext()

        // Update title colors
        val titles = arrayOf(coreVariablesTitle, backTitle)
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
        val arrows = arrayOf(selectionArrowCoreVariables, selectionArrowBack)
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
                MenuItem(
                        "core_variables",
                        resources.getString(R.string.core_variables_menu_title),
                        action =
                                MenuAction.NAVIGATE(
                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                .CORE_VARIABLES_MENU
                                )
                ),
                MenuItem("back", resources.getString(R.string.about_back), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        when (item.action) {
            MenuAction.BACK -> {
                aboutListener?.onAboutBackToMainMenu()
            }
            is MenuAction.NAVIGATE -> {
                // Handle navigation to Core Variables submenu
                viewModel.updateMenuState(item.action.targetMenu)
            }
            else -> {
                // Handle other actions if needed
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
