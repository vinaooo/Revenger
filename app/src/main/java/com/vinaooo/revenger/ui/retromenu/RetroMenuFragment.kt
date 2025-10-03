package com.vinaooo.revenger.ui.retromenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Retro menu fragment that shows "PAUSE" text over the game screen Triggered by gamepad buttons
 * (START, SELECT, or SELECT+START combinations based on configuration)
 */
class RetroMenuFragment : Fragment() {

        companion object {
                fun newInstance(): RetroMenuFragment {
                        return RetroMenuFragment()
                }
        }

        // Get ViewModel reference for centralized methods
        private lateinit var viewModel: GameActivityViewModel

        // Callback to notify when retro menu should be dismissed
        var onDismissCallback: (() -> Unit)? = null

        // Note: Reset game callback removed - now using centralized ViewModel method

        // Note: Load/Save state callbacks removed - now using centralized ViewModel methods

        // Retro menu mode (1=START, 2=SELECT, 3=SELECT+START)
        var retroMenuMode: Int = 1

        // Navigation state
        private var selectedOptionIndex = 0
        private val menuOptions = mutableListOf<TextView>()
        private val menuActions = mutableListOf<() -> Unit>()

        // Submenu state
        private var isInSubmenu = false
        private var menuContainer: LinearLayout? = null
        private var menuTitle: TextView? = null

        /** Reset selection to first option (called when menu appears) */
        fun resetSelectionToFirst() {
                selectedOptionIndex = 0
                updateSelection()
        }

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View? {
                // Create retro menu with title and Continue button
                val frameLayout =
                        FrameLayout(requireContext()).apply {
                                layoutParams =
                                        ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                setBackgroundColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_background
                                        )
                                )
                                isClickable = true
                                isFocusable = true

                                // Create 3-column layout: 25% - 50% - 25%
                                addView(
                                        LinearLayout(requireContext()).apply {
                                                layoutParams =
                                                        FrameLayout.LayoutParams(
                                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                                ViewGroup.LayoutParams.MATCH_PARENT
                                                        )
                                                orientation = LinearLayout.HORIZONTAL

                                                // Left column (25% - empty spacer)
                                                addView(
                                                        View(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.25f // Left column
                                                                                // weight -
                                                                                // hardcoded
                                                                                // for stability
                                                                                )
                                                        }
                                                )

                                                // Center column (50% - contains the menu)
                                                addView(
                                                        LinearLayout(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.50f // Center
                                                                                // column
                                                                                // weight -
                                                                                // hardcoded for
                                                                                // stability
                                                                                )
                                                                orientation = LinearLayout.VERTICAL
                                                                gravity =
                                                                        android.view.Gravity
                                                                                .CENTER_VERTICAL or
                                                                                android.view.Gravity
                                                                                        .LEFT

                                                                // Menu container aligned to left of
                                                                // center column
                                                                addView(
                                                                        LinearLayout(
                                                                                        requireContext()
                                                                                )
                                                                                .apply {
                                                                                        layoutParams =
                                                                                                LinearLayout
                                                                                                        .LayoutParams(
                                                                                                                ViewGroup
                                                                                                                        .LayoutParams
                                                                                                                        .WRAP_CONTENT,
                                                                                                                ViewGroup
                                                                                                                        .LayoutParams
                                                                                                                        .WRAP_CONTENT
                                                                                                        )
                                                                                        orientation =
                                                                                                LinearLayout
                                                                                                        .VERTICAL
                                                                                        gravity =
                                                                                                android.view
                                                                                                        .Gravity
                                                                                                        .LEFT

                                                                                        // Store
                                                                                        // reference
                                                                                        // to menu
                                                                                        // container
                                                                                        menuContainer =
                                                                                                this

                                                                                        // Menu
                                                                                        // title
                                                                                        menuTitle =
                                                                                                createMenuTitle(
                                                                                                        getString(
                                                                                                                R.string
                                                                                                                        .retro_menu_title
                                                                                                        )
                                                                                                )
                                                                                        addView(
                                                                                                menuTitle
                                                                                        )

                                                                                        // Menu
                                                                                        // options -
                                                                                        // using
                                                                                        // navigation system
                                                                                        createAllMenuOptions(
                                                                                                this
                                                                                        )
                                                                                }
                                                                ) // Close menu container
                                                        }
                                                ) // Close center column

                                                // Right column (25% - empty spacer)
                                                addView(
                                                        View(requireContext()).apply {
                                                                layoutParams =
                                                                        LinearLayout.LayoutParams(
                                                                                0,
                                                                                ViewGroup
                                                                                        .LayoutParams
                                                                                        .MATCH_PARENT,
                                                                                0.25f // Right
                                                                                // column
                                                                                // weight -
                                                                                // hardcoded
                                                                                // for stability
                                                                                )
                                                        }
                                                )
                                        } // Close 3-column layout
                                )

                                // Keep background click behavior (don't dismiss)
                                setOnClickListener {
                                        // Intencionalmente não faz nada para evitar fechamento
                                        // acidental
                                }
                        }
                return frameLayout
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                // Initialize ViewModel
                viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

                // Always reset selection to first option when menu appears
                resetSelectionToFirst()

                // Also dismiss when appropriate button(s) are pressed
                view.isFocusableInTouchMode = true
                view.requestFocus()
                view.setOnKeyListener { _, keyCode, event ->
                        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                val shouldDismiss =
                                        when (retroMenuMode) {
                                                1 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_START // START only
                                                2 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_SELECT // SELECT only
                                                3 ->
                                                        keyCode ==
                                                                android.view.KeyEvent
                                                                        .KEYCODE_BUTTON_START ||
                                                                keyCode ==
                                                                        android.view.KeyEvent
                                                                                .KEYCODE_BUTTON_SELECT // Either for
                                                // combo
                                                else -> false
                                        }

                                if (shouldDismiss) {
                                        dismissOverlay()
                                        true
                                } else {
                                        false
                                }
                        } else {
                                false
                        }
                }
        }

        /** Create all menu options with navigation support */
        private fun createAllMenuOptions(container: LinearLayout) {
                // Clear previous options
                menuOptions.clear()
                menuActions.clear()

                // Define all menu options
                val options =
                        listOf(
                                Pair(getString(R.string.retro_menu_continue_game)) {
                                        continueGame()
                                },
                                Pair(getString(R.string.retro_menu_restart_game)) { restartGame() },
                                Pair(getString(R.string.retro_menu_save_state)) { saveState() },
                                Pair(getString(R.string.retro_menu_load_state)) { loadStateSafe() },
                                Pair(getString(R.string.retro_menu_settings)) { openSettings() },
                                Pair(getString(R.string.retro_menu_exit_game)) { showExitSubmenu() }
                        )

                // Create options and add to lists
                options.forEachIndexed { index, (text, action) ->
                        val option = createMenuOption(text, index == selectedOptionIndex)
                        container.addView(option)
                        menuOptions.add(option)
                        menuActions.add(action)
                }
        }

        /** Handle DPAD navigation input */
        fun handleNavigationInput(keyCode: Int): Boolean {
                return when (keyCode) {
                        // Standard DPAD codes (now sent via converted analog motion)
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                navigateUp()
                                true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                navigateDown()
                                true
                        }
                        android.view.KeyEvent.KEYCODE_BUTTON_A -> {
                                confirmSelection()
                                true
                        }
                        android.view.KeyEvent.KEYCODE_BUTTON_B -> {
                                exitMenu()
                                true
                        }
                        else -> false
                }
        }

        /** Navigate to previous option */
        private fun navigateUp() {
                if (menuOptions.isEmpty()) return

                selectedOptionIndex =
                        if (selectedOptionIndex > 0) {
                                selectedOptionIndex - 1
                        } else {
                                menuOptions.size - 1 // Wrap to last option
                        }
                updateSelection()
        }

        /** Navigate to next option */
        private fun navigateDown() {
                if (menuOptions.isEmpty()) return

                selectedOptionIndex =
                        if (selectedOptionIndex < menuOptions.size - 1) {
                                selectedOptionIndex + 1
                        } else {
                                0 // Wrap to first option
                        }
                updateSelection()
        }

        /** Confirm current selection */
        private fun confirmSelection() {
                if (selectedOptionIndex < menuActions.size) {
                        menuActions[selectedOptionIndex].invoke()
                }
        }

        /** Exit menu (B button) */
        private fun exitMenu() {
                if (isInSubmenu) {
                        returnToMainMenu()
                } else {
                        continueGame()
                }
        }

        /** Update visual selection highlighting */
        private fun updateSelection() {
                menuOptions.forEachIndexed { index, option ->
                        val isSelected = index == selectedOptionIndex

                        // Update text color based on selection
                        option.setTextColor(
                                if (isSelected) {
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_selected
                                        )
                                } else {
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_default
                                        )
                                }
                        )

                        // Update background for better visual feedback
                        if (isSelected) {
                                option.setBackgroundColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_option_background
                                        )
                                )
                        } else {
                                option.setBackgroundColor(
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_option_background
                                        )
                                )
                        }
                }
        }

        /** Create menu title with arcade styling */
        private fun createMenuTitle(text: String): TextView {
                return TextView(requireContext()).apply {
                        this.text = text
                        textSize =
                                resources.getDimension(R.dimen.retro_menu_title_text_size) /
                                        resources.displayMetrics.density
                        setTextColor(
                                ContextCompat.getColor(
                                        requireContext(),
                                        R.color.retro_menu_text_default
                                )
                        )
                        setShadowLayer(
                                resources.getDimension(R.dimen.retro_menu_title_shadow_radius),
                                resources.getDimension(R.dimen.retro_menu_title_shadow_dx),
                                resources.getDimension(R.dimen.retro_menu_title_shadow_dy),
                                ContextCompat.getColor(requireContext(), R.color.retro_menu_shadow)
                        )

                        // Apply arcade font
                        try {
                                typeface =
                                        ResourcesCompat.getFont(
                                                requireContext(),
                                                R.font.arcade_normal
                                        )
                        } catch (e: Exception) {
                                // Mantém fonte padrão quando arcade_normal não estiver disponível
                        }

                        // Margin below title
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        .apply {
                                                bottomMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_title_bottom_margin
                                                        )
                                        }

                        gravity = android.view.Gravity.LEFT
                }
        }

        /** Create menu option with arcade styling for DPAD navigation */
        private fun createMenuOption(text: String, isSelected: Boolean): TextView {
                return TextView(requireContext()).apply {
                        this.text = text
                        textSize =
                                resources.getDimension(R.dimen.retro_menu_option_text_size) /
                                        resources.displayMetrics.density
                        setTextColor(
                                if (isSelected)
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_selected
                                        )
                                else
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.retro_menu_text_default
                                        )
                        )
                        setShadowLayer(
                                resources.getDimension(R.dimen.retro_menu_option_shadow_radius),
                                resources.getDimension(R.dimen.retro_menu_option_shadow_dx),
                                resources.getDimension(R.dimen.retro_menu_option_shadow_dy),
                                ContextCompat.getColor(requireContext(), R.color.retro_menu_shadow)
                        )

                        // Apply arcade font
                        try {
                                typeface =
                                        ResourcesCompat.getFont(
                                                requireContext(),
                                                R.font.arcade_normal
                                        )
                        } catch (e: Exception) {
                                // Mantém fonte padrão quando arcade_normal não estiver disponível
                        }

                        // Add generous padding for better touch area
                        setPadding(
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_horizontal
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_vertical
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_horizontal
                                ),
                                resources.getDimensionPixelSize(
                                        R.dimen.retro_menu_option_padding_vertical
                                )
                        )

                        // Background for visual feedback (controlled by navigation)
                        setBackgroundColor(
                                ContextCompat.getColor(
                                        requireContext(),
                                        R.color.retro_menu_option_background
                                )
                        )

                        // Make it clickable for touch support alongside DPAD navigation
                        isClickable = true
                        isFocusable = true

                        // Add click listener for touch support
                        setOnClickListener {
                                // Find which option was clicked and update selection
                                val clickedIndex = menuOptions.indexOf(this)
                                if (clickedIndex != -1) {
                                        selectedOptionIndex = clickedIndex
                                        updateSelection()

                                        // Execute the action after a brief delay for visual
                                        // feedback
                                        postDelayed(
                                                {
                                                        if (clickedIndex < menuActions.size) {
                                                                menuActions[clickedIndex].invoke()
                                                        }
                                                },
                                                100
                                        )
                                }
                        }

                        // Larger margin between options for easier targeting
                        layoutParams =
                                LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams
                                                        .WRAP_CONTENT, // Wrap content to align left
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        .apply {
                                                bottomMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_bottom
                                                        )
                                                leftMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_horizontal
                                                        )
                                                rightMargin =
                                                        resources.getDimensionPixelSize(
                                                                R.dimen
                                                                        .retro_menu_option_margin_horizontal
                                                        )
                                        }

                        gravity = android.view.Gravity.CENTER
                }
        }

        /** Game actions */
        private fun restartGame() {
                // CORREÇÃO: Usar dismissOverlay para restaurar frameSpeed = 1 (retomar emulação)
                viewModel.resetGameCentralized { dismissOverlay() }
        }

        private fun saveState() {
                // CORREÇÃO: Usar dismissOverlay para restaurar frameSpeed = 1 (retomar emulação)
                viewModel.saveStateCentralized { dismissOverlay() }
        }

        private fun loadState() {
                // CORREÇÃO: Usar dismissOverlay para restaurar frameSpeed = 1 (retomar emulação)
                viewModel.loadStateCentralized { dismissOverlay() }
        }

        private fun loadStateSafe() {
                // CORREÇÃO: Usar dismissOverlay para restaurar frameSpeed = 1 (retomar emulação)
                viewModel.loadStateCentralized { dismissOverlay() }
        }

        private fun openSettings() {
                showSettingsSubmenu()
        }

        /** Show exit game submenu with 3 options */
        private fun showExitSubmenu() {
                isInSubmenu = true

                // Update title
                menuTitle?.text = "EXIT GAME?"

                // Clear current menu options
                menuContainer?.let { container ->
                        // Remove all menu option views (keep only title)
                        for (i in container.childCount - 1 downTo 1) {
                                container.removeViewAt(i)
                        }
                }

                // Create submenu options
                createExitSubmenuOptions()
        }

        /** Create exit submenu options */
        private fun createExitSubmenuOptions() {
                menuOptions.clear()
                menuActions.clear()

                val submenuOptions =
                        listOf(
                                Pair("SAVE AND EXIT") { saveAndExit() },
                                Pair("CANCEL") { returnToMainMenu() },
                                Pair("EXIT WITHOUT SAVE") { exitWithoutSave() }
                        )

                menuContainer?.let { container ->
                        submenuOptions.forEachIndexed { index, (text, action) ->
                                val option = createMenuOption(text, index == selectedOptionIndex)
                                container.addView(option)
                                menuOptions.add(option)
                                menuActions.add(action)
                        }
                }

                // Reset selection to first option
                resetSelectionToFirst()
        }

        /** Show settings submenu with audio and speed options */
        private fun showSettingsSubmenu() {
                isInSubmenu = true

                // Update title
                menuTitle?.text = getString(R.string.retro_menu_settings_submenu_title)

                // Clear current menu options
                menuContainer?.let { container ->
                        // Remove all menu option views (keep only title)
                        for (i in container.childCount - 1 downTo 1) {
                                container.removeViewAt(i)
                        }
                }

                // Create submenu options
                createSettingsSubmenuOptions()
        }

        /** Create settings submenu options */
        private fun createSettingsSubmenuOptions() {
                menuOptions.clear()
                menuActions.clear()

                // Get current states for display using string resources
                val audioState =
                        if (viewModel.getAudioController()?.getAudioState() == true) {
                                getString(R.string.retro_menu_settings_sound_on)
                        } else {
                                getString(R.string.retro_menu_settings_sound_off)
                        }

                val speedState =
                        if (viewModel.getSpeedController()?.getFastForwardState() == true) {
                                getString(R.string.retro_menu_settings_speed_fast)
                        } else {
                                getString(R.string.retro_menu_settings_speed_normal)
                        }

                val submenuOptions =
                        listOf(
                                Pair(getString(R.string.retro_menu_settings_sound, audioState)) {
                                        toggleAudioSetting()
                                },
                                Pair(getString(R.string.retro_menu_settings_speed, speedState)) {
                                        toggleSpeedSetting()
                                },
                                Pair(getString(R.string.retro_menu_settings_back)) {
                                        returnToMainMenu()
                                }
                        )

                menuContainer?.let { container ->
                        submenuOptions.forEachIndexed { index, (text, action) ->
                                val option = createMenuOption(text, index == selectedOptionIndex)
                                container.addView(option)
                                menuOptions.add(option)
                                menuActions.add(action)
                        }
                }

                // Reset selection to first option
                resetSelectionToFirst()
        }

        /** Return to main menu from submenu */
        private fun returnToMainMenu() {
                isInSubmenu = false

                // Restore main menu title
                menuTitle?.text = getString(R.string.retro_menu_title)

                // Clear submenu options
                menuContainer?.let { container ->
                        // Remove all submenu option views (keep only title)
                        for (i in container.childCount - 1 downTo 1) {
                                container.removeViewAt(i)
                        }
                }

                // Recreate main menu options
                menuContainer?.let { createAllMenuOptions(it) }
        }

        /** Exit without saving */
        private fun exitWithoutSave() {
                // Close the app immediately
                android.os.Process.killProcess(android.os.Process.myPid())
        }

        /** Save and exit */
        private fun saveAndExit() {
                // Save state first, then exit
                viewModel.saveStateCentralized {
                        android.os.Process.killProcess(android.os.Process.myPid())
                }
        }

        private fun continueGame() {
                // Simply dismiss the overlay - dismissPauseOverlay() will handle resume with
                // frameSpeed = 1
                onDismissCallback?.invoke()
        }

        /** Toggle audio setting and update submenu display */
        private fun toggleAudioSetting() {
                // Use ViewModel's audio controller
                viewModel.onToggleAudio()

                // Refresh the settings submenu to show updated state
                showSettingsSubmenu()
        }

        /** Toggle speed setting and update submenu display */
        private fun toggleSpeedSetting() {
                // Use ViewModel's speed controller
                viewModel.onFastForward()

                // Refresh the settings submenu to show updated state
                showSettingsSubmenu()
        }

        /** Dismiss the pause overlay */
        fun dismissOverlay() {
                onDismissCallback?.invoke()
        }

        /** Simple menu dismiss replicating Modern Menu behavior - just removes fragment */
        private fun dismissMenuSimple() {
                parentFragmentManager.beginTransaction().remove(this).commit()
        }
}
