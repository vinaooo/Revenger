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
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** SettingsMenuFragment - Settings submenu with visual identical to RetroMenu3 */
class SettingsMenuFragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var settingsMenuContainer: LinearLayout
    private lateinit var soundSettings: MaterialCardView
    private lateinit var gameSpeedSettings: MaterialCardView
    private lateinit var backSettings: MaterialCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Start with "Sound"

    // Menu option titles for color control
    private lateinit var soundTitle: TextView
    private lateinit var gameSpeedTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSound: TextView
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
        android.util.Log.d(
                "SettingsMenuFragment",
                "onViewCreated: SettingsMenuFragment view created"
        )

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        android.util.Log.d(
                "SettingsMenuFragment",
                "onViewCreated: SettingsMenuFragment setup completed"
        )
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
        settingsMenuContainer = view.findViewById(R.id.settings_menu_container)

        // Menu items
        soundSettings = view.findViewById(R.id.settings_sound)
        gameSpeedSettings = view.findViewById(R.id.settings_game_speed)
        backSettings = view.findViewById(R.id.settings_back)

        // Initialize ordered list of menu items
        menuItems = listOf(soundSettings, gameSpeedSettings, backSettings)

        // Initialize menu option titles
        soundTitle = view.findViewById(R.id.sound_title)
        gameSpeedTitle = view.findViewById(R.id.game_speed_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowSound = view.findViewById(R.id.selection_arrow_sound)
        selectionArrowGameSpeed = view.findViewById(R.id.selection_arrow_game_speed)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Set first item as selected
        updateSelectionVisual()
    }

    private fun setupClickListeners() {
        soundSettings.setOnClickListener {
            // Toggle audio
            val currentAudioState = viewModel.getAudioState()
            viewModel.setAudioEnabled(!currentAudioState)
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

        // Update sound title
        soundTitle.text = getString(if (isAudioEnabled) R.string.audio_on else R.string.audio_off)

        // Update game speed title
        gameSpeedTitle.text =
                getString(
                        if (isFastForwardEnabled) R.string.fast_forward_active
                        else R.string.fast_forward_inactive
                )
    }

    /** Navigate up in the menu */
    fun navigateUp() {
        currentSelectedIndex = (currentSelectedIndex - 1 + menuItems.size) % menuItems.size
        updateSelectionVisual()
    }

    /** Navigate down in the menu */
    fun navigateDown() {
        currentSelectedIndex = (currentSelectedIndex + 1) % menuItems.size
        updateSelectionVisual()
    }

    /** Confirm current selection */
    fun confirmSelection() {
        when (currentSelectedIndex) {
            0 -> soundSettings.performClick() // Sound
            1 -> gameSpeedSettings.performClick() // Game Speed
            2 -> backSettings.performClick() // Back
        }
    }

    /** Update selection visual */
    private fun updateSelectionVisual() {
        menuItems.forEach { item ->
            // Removed: background color of individual cards
            // Selection now indicated only by yellow text and arrows
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Control text colors based on selection
        soundTitle.setTextColor(
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        gameSpeedTitle.setTextColor(
                if (currentSelectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Sound
        if (currentSelectedIndex == 0) {
            selectionArrowSound.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSound.visibility = View.VISIBLE
            (selectionArrowSound.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSound.visibility = View.GONE
        }

        // Game Speed
        if (currentSelectedIndex == 1) {
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
        if (currentSelectedIndex == 2) {
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
        android.util.Log.d(
                "SettingsMenuFragment",
                "hideMainMenu: Hiding menu content but keeping background"
        )
        // Hide only menu content, keeping background for submenu
        settingsMenuContainer.visibility = View.INVISIBLE
        android.util.Log.d(
                "SettingsMenuFragment",
                "hideMainMenu: Menu content hidden, background should remain visible"
        )
    }

    /** Make main menu visible again (when submenu is closed) */
    fun showMainMenu() {
        android.util.Log.d("SettingsMenuFragment", "showMainMenu: Showing menu content again")
        android.util.Log.d(
                "SettingsMenuFragment",
                "showMainMenu: BEFORE - visibility = ${settingsMenuContainer.visibility}"
        )
        android.util.Log.d(
                "SettingsMenuFragment",
                "showMainMenu: BEFORE - alpha = ${settingsMenuContainer.alpha}"
        )

        // Make visible
        settingsMenuContainer.visibility = View.VISIBLE

        // Ensure alpha is at 1.0 (fully visible)
        settingsMenuContainer.alpha = 1.0f

        // Ensure visual selection is updated when menu becomes visible again
        updateSelectionVisual()

        // Force complete redraw
        settingsMenuContainer.invalidate()
        settingsMenuContainer.requestLayout()

        // REMOVED: bringToFront() causes problem with layout_weight
        // The SettingsMenuFragment has already been completely removed with popBackStack()
        // so there is no need to bring to front

        android.util.Log.d(
                "SettingsMenuFragment",
                "showMainMenu: AFTER - visibility = ${settingsMenuContainer.visibility}"
        )
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

    companion object {
        fun newInstance(): SettingsMenuFragment {
            return SettingsMenuFragment()
        }
    }
}
