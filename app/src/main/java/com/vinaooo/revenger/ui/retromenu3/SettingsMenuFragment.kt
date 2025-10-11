package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** SettingsMenuFragment - Settings submenu with visual identical to RetroMenu3 */
class SettingsMenuFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var settingsMenuContainer: LinearLayout
    private lateinit var soundSettings: MaterialCardView
    private lateinit var shaderSettings: MaterialCardView
    private lateinit var gameSpeedSettings: MaterialCardView
    private lateinit var backSettings: MaterialCardView

    // Menu title
    private lateinit var settingsMenuTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>

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
        forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation
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
        settingsMenuContainer = view.findViewById(R.id.settings_menu_container)

        // Menu title
        settingsMenuTitle = view.findViewById(R.id.settings_menu_title)

        // Menu items
        soundSettings = view.findViewById(R.id.settings_sound)
        shaderSettings = view.findViewById(R.id.settings_shader)
        gameSpeedSettings = view.findViewById(R.id.settings_game_speed)
        backSettings = view.findViewById(R.id.settings_back)

        // Initialize ordered list of menu items
        menuItems = listOf(soundSettings, shaderSettings, gameSpeedSettings, backSettings)

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
        applyArcadeFontToViews()
    }

    private fun applyArcadeFontToViews() {
        val context = requireContext()

        // Apply font to all text views in the settings menu
        FontUtils.applyArcadeFont(
                context,
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
            // Game Speed - First close all menus, then apply the functionality
            // A) Close all menus first
            viewModel.dismissAllMenus()

            // B) Apply existing functionality (toggle fast forward)
            val currentFastForwardState = viewModel.getFastForwardState()
            if (currentFastForwardState) {
                viewModel.disableFastForward()
            } else {
                viewModel.enableFastForward()
            }
            // Note: No need to call updateMenuState() since menus are being dismissed
        }

        backSettings.setOnClickListener {
            // Return to main menu
            // Just notify the listener, animation will be done by dismissSettingsMenu()
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
        android.util.Log.d(TAG, "[ACTION] Settings menu: CONFIRM on index $selectedIndex")
        when (selectedIndex) {
            0 -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Sound toggle selected")
                soundSettings.performClick() // Sound
            }
            1 -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Shader toggle selected")
                shaderSettings.performClick() // Shader
            }
            2 -> {
                android.util.Log.d(TAG, "[ACTION] Settings menu: Game speed toggle selected")
                gameSpeedSettings.performClick() // Game Speed
            }
            3 -> {
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
        menuItems.forEach { item ->
            // Removed: background color of individual cards
            // Selection now indicated only by yellow text and arrows
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Control text colors based on selection
        soundTitle.setTextColor(
                if (getCurrentSelectedIndex() == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        shaderTitle.setTextColor(
                if (getCurrentSelectedIndex() == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        gameSpeedTitle.setTextColor(
                if (getCurrentSelectedIndex() == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (getCurrentSelectedIndex() == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Sound
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowSound.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSound.visibility = View.VISIBLE
            (selectionArrowSound.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSound.visibility = View.GONE
        }

        // Shader
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowShader.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowShader.visibility = View.VISIBLE
            (selectionArrowShader.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowShader.visibility = View.GONE
        }

        // Game Speed
        if (getCurrentSelectedIndex() == 2) {
            selectionArrowGameSpeed.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowGameSpeed.visibility = View.VISIBLE
            (selectionArrowGameSpeed.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowGameSpeed.visibility = View.GONE
        }

        // Back
        if (getCurrentSelectedIndex() == 3) {
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
        parentFragmentManager.beginTransaction().remove(this).commit()
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
        return listOf(
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
                MenuItem(
                        "shader",
                        getString(R.string.settings_shader),
                        action = MenuAction.TOGGLE_SHADER
                ),
                MenuItem("back", getString(R.string.settings_back), action = MenuAction.BACK)
        )
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

    companion object {
        private const val TAG = "SettingsMenu"

        fun newInstance(): SettingsMenuFragment {
            return SettingsMenuFragment()
        }
    }
}
