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

/** SettingsMenuFragment - Settings submenu with visual identical to RetroMenu3 */
class SettingsMenuFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var settingsMenuContainer: LinearLayout
    private lateinit var soundSettings: RetroCardView
    private lateinit var shaderSettings: RetroCardView
    private lateinit var gameSpeedSettings: RetroCardView
    private lateinit var backSettings: RetroCardView

    // Menu title
    private lateinit var settingsMenuTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Menu option titles for color control
    private lateinit var soundTitle: TextView
    private lateinit var shaderTitle: TextView
    private lateinit var gameSpeedTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSound: TextView
    private lateinit var selectionArrowShader: TextView
    private lateinit var selectionArrowGameSpeed: TextView
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface SettingsMenuListener {
        fun onBackToMainMenu()
    }

    private var settingsListener: SettingsMenuListener? = null

    fun setSettingsListener(listener: SettingsMenuListener) {
        this.settingsListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings_menu, container, false)
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
        settingsMenuContainer = view.findViewById(R.id.settings_menu_container)

        // Menu title
        settingsMenuTitle = view.findViewById(R.id.settings_menu_title)

        // Menu items
        soundSettings = view.findViewById(R.id.settings_sound)
        shaderSettings = view.findViewById(R.id.settings_shader)
        gameSpeedSettings = view.findViewById(R.id.settings_game_speed)
        backSettings = view.findViewById(R.id.settings_back)

        // Check if shader selection is enabled (config_shader == "settings")
        val isShaderSelectionEnabled = isShaderSelectionEnabled()

        // Conditionally show/hide shader settings based on config
        shaderSettings.visibility = if (isShaderSelectionEnabled) View.VISIBLE else View.GONE

        // Initialize ordered list of menu items (dynamic based on shader visibility)
        menuItems =
                if (isShaderSelectionEnabled) {
                    listOf(soundSettings, shaderSettings, gameSpeedSettings, backSettings)
                } else {
                    listOf(soundSettings, gameSpeedSettings, backSettings)
                }

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        soundSettings.setUseBackgroundColor(false)
        shaderSettings.setUseBackgroundColor(false)
        gameSpeedSettings.setUseBackgroundColor(false)
        backSettings.setUseBackgroundColor(false)

        // Initialize menu option titles
        soundTitle = view.findViewById(R.id.sound_title)
        shaderTitle = view.findViewById(R.id.shader_title)
        gameSpeedTitle = view.findViewById(R.id.game_speed_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowSound = view.findViewById(R.id.selection_arrow_sound)
        selectionArrowShader = view.findViewById(R.id.selection_arrow_shader)
        selectionArrowGameSpeed = view.findViewById(R.id.selection_arrow_game_speed)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                settingsMenuTitle,
                soundTitle,
                shaderTitle,
                gameSpeedTitle,
                backTitle,
                selectionArrowSound,
                selectionArrowShader,
                selectionArrowGameSpeed,
                selectionArrowBack
        )
    }

    /** Check if shader selection is enabled based on config_shader setting */
    private fun isShaderSelectionEnabled(): Boolean {
        val configShader = resources.getString(R.string.config_shader)
        return configShader == "settings"
    }

    private fun setupClickListeners() {
        soundSettings.setOnClickListener {
            // Toggle audio
            val currentAudioState = viewModel.getAudioState()
            viewModel.setAudioEnabled(!currentAudioState)
            updateMenuState()
        }

        shaderSettings.setOnClickListener {
            // Toggle shader
            viewModel.onToggleShader()
            updateMenuState()
        }

        gameSpeedSettings.setOnClickListener {
            // Game Speed - Toggle fast forward without closing the menu and without applying
            // immediately
            val currentFastForwardState = viewModel.getFastForwardState()
            viewModel.setFastForwardEnabled(
                    !currentFastForwardState
            ) // Toggle state without immediate application
            // Update menu state to reflect the change
            updateMenuState()
        }

        backSettings.setOnClickListener {
            // Return to main menu by calling listener method (same as pressing B)
            settingsListener?.onBackToMainMenu()
        }
    }

    private fun updateMenuState() {
        val isAudioEnabled = viewModel.getAudioState()
        val isFastForwardEnabled = viewModel.getFastForwardState()
        val currentShader = viewModel.getShaderState()

        // Update sound title
        soundTitle.text = getString(if (isAudioEnabled) R.string.audio_on else R.string.audio_off)

        // Update shader title
        shaderTitle.text = getString(R.string.settings_shader) + ": $currentShader"

        // Update game speed title
        gameSpeedTitle.text =
                getString(
                        if (isFastForwardEnabled) R.string.fast_forward_active
                        else R.string.fast_forward_inactive
                )

        // Aplicar capitalização configurada aos textos
        FontUtils.applyTextCapitalization(
                requireContext(),
                settingsMenuTitle,
                soundTitle,
                shaderTitle,
                gameSpeedTitle,
                backTitle
        )
    }

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateUpCircular(getMenuItems().size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] Settings menu: UP navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateDownCircular(getMenuItems().size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(
                TAG,
                "[NAV] Settings menu: DOWN navigation - $beforeIndex -> $afterIndex"
        )
        updateSelectionVisualInternal()
    }

    /** Confirm current selection */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        val isShaderEnabled = isShaderSelectionEnabled()
        android.util.Log.d(
                TAG,
                "[ACTION] Settings menu: CONFIRM on index $selectedIndex (shader enabled: $isShaderEnabled)"
        )

        when {
            selectedIndex == 0 -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Sound toggle selected")
                soundSettings.performClick() // Sound
            }
            selectedIndex == 1 && isShaderEnabled -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Shader toggle selected")
                shaderSettings.performClick() // Shader
            }
            (selectedIndex == 1 && !isShaderEnabled) || (selectedIndex == 2 && isShaderEnabled) -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Game speed toggle selected")
                gameSpeedSettings.performClick() // Game Speed
            }
            (selectedIndex == 2 && !isShaderEnabled) || (selectedIndex == 3 && isShaderEnabled) -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Back to main menu selected")
                backSettings.performClick() // Back
            }
            else ->
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Settings menu: Invalid selection index $selectedIndex"
                    )
        }
    }

    /** Back action */
    override fun performBack(): Boolean {
        // For settings submenu, back should go to main menu
        backSettings.performClick()
        return true
    }

    /** Update selection visual - specific implementation for SettingsMenuFragment */
    override fun updateSelectionVisualInternal() {
        val isShaderEnabled = isShaderSelectionEnabled()
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

        // Control text colors based on selection (dynamic based on shader visibility)
        soundTitle.setTextColor(
                if (selectedIndex == 0)
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

        if (isShaderEnabled) {
            shaderTitle.setTextColor(
                    if (selectedIndex == 1)
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
            gameSpeedTitle.setTextColor(
                    if (selectedIndex == 2)
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
                    if (selectedIndex == 3)
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
        } else {
            gameSpeedTitle.setTextColor(
                    if (selectedIndex == 1)
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
                    if (selectedIndex == 2)
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
        }

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        // val arrowMarginEnd =
        // resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Sound
        if (selectedIndex == 0) {
            selectionArrowSound.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowSound.visibility = View.VISIBLE
            (selectionArrowSound.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowSound.visibility = View.GONE
        }

        // Shader (only if enabled)
        if (isShaderEnabled && selectedIndex == 1) {
            selectionArrowShader.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowShader.visibility = View.VISIBLE
            (selectionArrowShader.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowShader.visibility = View.GONE
        }

        // Game Speed
        val gameSpeedIndex = if (isShaderEnabled) 2 else 1
        if (selectedIndex == gameSpeedIndex) {
            selectionArrowGameSpeed.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.retro_menu3_selected_color
                    )
            )
            selectionArrowGameSpeed.visibility = View.VISIBLE
            (selectionArrowGameSpeed.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowGameSpeed.visibility = View.GONE
        }

        // Back
        val backIndex = if (isShaderEnabled) 3 else 2
        if (selectedIndex == backIndex) {
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
        settingsMenuContainer.requestLayout()
    }

    /** Make main menu invisible (when submenu is opened) */
    fun hideMainMenu() {
        // Hide only menu content, keeping background for submenu
        settingsMenuContainer.visibility = View.INVISIBLE
    }

    /** Make main menu visible again (when submenu is closed) */
    fun showMainMenu() {
        // Make visible
        settingsMenuContainer.visibility = View.VISIBLE

        // Ensure alpha is at 1.0 (fully visible)
        settingsMenuContainer.alpha = 1.0f

        // Ensure visual selection is updated when menu becomes visible again
        updateSelectionVisualInternal()

        // Force complete redraw
        settingsMenuContainer.invalidate()
        settingsMenuContainer.requestLayout()

        // REMOVED: bringToFront() causes problem with layout_weight
        // The SettingsMenuFragment has already been completely removed with popBackStack()
        // so there is no need to bring to front
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            (settingsListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Call clearKeyLog through ViewModel to reset combo state
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w(
                    "SettingsMenuFragment",
                    "Error resetting combo state in onDestroy",
                    e
            )
        }
    }

    // ===== MenuFragmentBase Abstract Methods Implementation =====

    override fun getMenuItems(): List<MenuItem> {
        val isShaderEnabled = isShaderSelectionEnabled()

        return if (isShaderEnabled) {
            listOf(
                    MenuItem(
                            "sound",
                            getString(R.string.settings_audio),
                            action = MenuAction.TOGGLE_AUDIO
                    ),
                    MenuItem(
                            "shader",
                            getString(R.string.settings_shader),
                            action = MenuAction.TOGGLE_SHADER
                    ),
                    MenuItem(
                            "speed",
                            getString(R.string.menu_fast_forward),
                            action = MenuAction.TOGGLE_SPEED
                    ),
                    MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK)
            )
        } else {
            listOf(
                    MenuItem(
                            "sound",
                            getString(R.string.settings_audio),
                            action = MenuAction.TOGGLE_AUDIO
                    ),
                    MenuItem(
                            "speed",
                            getString(R.string.menu_fast_forward),
                            action = MenuAction.TOGGLE_SPEED
                    ),
                    MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK)
            )
        }
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Use new MenuAction system, but fallback to old click listeners for compatibility
        when (item.action) {
            MenuAction.TOGGLE_AUDIO -> soundSettings.performClick()
            MenuAction.TOGGLE_SPEED -> gameSpeedSettings.performClick()
            MenuAction.BACK -> backSettings.performClick()
            else -> {
                /* Ignore other actions */
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // CRITICAL FIX: Register immediately without delays
        // isAdded is enough - no need to wait for isResumed or view?.post
        // This eliminates race condition where user navigates before registration completes
        if (isAdded && context != null) {
            android.util.Log.d(
                    "SettingsMenuFragment",
                    "[RESUME] ⚙️ Registering immediately (isAdded=$isAdded)"
            )

            // CRITICAL: Re-configure listener after rotation
            android.util.Log.d(
                    "SettingsMenuFragment",
                    "[RESUME] 🔗 Reconfiguring listener after recreation"
            )
            try {
                val parentFragment = parentFragment
                if (parentFragment is SettingsMenuListener) {
                    setSettingsListener(parentFragment)
                    android.util.Log.d(
                            "SettingsMenuFragment",
                            "[RESUME] ✅ Listener configured successfully"
                    )
                } else {
                    android.util.Log.e(
                            "SettingsMenuFragment",
                            "[RESUME] ❌ Parent fragment is not SettingsMenuListener!"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                        "SettingsMenuFragment",
                        "[RESUME] ❌ Error configuring listener",
                        e
                )
            }

            viewModel.registerSettingsMenuFragment(this)

            // Restore focus (can still be delayed)
            view?.post {
                val firstFocusable = view?.findViewById<android.view.View>(R.id.settings_sound)
                firstFocusable?.requestFocus()
                android.util.Log.d("SettingsMenuFragment", "[FOCUS] Focus restored")
            }
        } else {
            android.util.Log.w(
                    "SettingsMenuFragment",
                    "[RESUME] Fragment not ready (isAdded=$isAdded, context=$context)"
            )
        }
    }

    companion object {
        private const val TAG = "SettingsMenu"

        fun newInstance(): SettingsMenuFragment {
            return SettingsMenuFragment()
        }
    }
}
