package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.MenuLogger

/**
 * Data class representing all views of the RetroMenu3 menu.
 *
 * **Value Object Pattern**: Centralizes all view references to avoid multiple
 * findViewById calls.
 *
 * **Benefits**:
 * - Performance: findViewById done only once
 * - Type-safety: References guaranteed at compile time
 * - Maintainability: Layout changes reflected in a single place
 *
 * @property menuContainer Main menu container
 * @property continueMenu, resetMenu, progressMenu, settingsMenu, aboutMenu, exitMenu Cards for each
 * option
 * @property menuItems Ordered list of all cards for navigation
 * @property continueTitle, resetTitle, etc TextViews for titles of each option
 * @property selectionArrowContinue, selectionArrowReset, etc TextViews for selection arrows (→)
 * @property titleTextView TextView for the main menu title
 */
data class MenuViews(
        val menuContainer: LinearLayout,
        val continueMenu: RetroCardView,
        val resetMenu: RetroCardView,
        val progressMenu: RetroCardView,
        val settingsMenu: RetroCardView,
        val aboutMenu: RetroCardView,
        val exitMenu: RetroCardView,
        val menuItems: List<RetroCardView>,
        val continueTitle: TextView,
        val resetTitle: TextView,
        val progressTitle: TextView,
        val settingsTitle: TextView,
        val aboutTitle: TextView,
        val exitTitle: TextView,
        val selectionArrowContinue: TextView,
        val selectionArrowReset: TextView,
        val selectionArrowProgress: TextView,
        val selectionArrowSettings: TextView,
        val selectionArrowAbout: TextView,
        val selectionArrowExit: TextView,
        val titleTextView: TextView
)

/**
 * Interface for menu view initialization.
 *
 * **Strategy Pattern**: Defines a contract for different UI initialization strategies.
 *
 * **Responsibilities**:
 * - `initializeViews()`: Performs findViewById for all menu views
 * - `setupClickListeners()`: Configures touch navigation system (Phase 3.3)
 * - `setupDynamicTitle()`: Sets up dynamic menu title
 * - `configureInitialViewStates()`: Sets initial view states
 *
 * **Phase 3.3**: setupClickListeners integrated with NavigationController for unified touch
 * navigation. **Phase 3.3 Cleanup**: Legacy system removed, only NavigationController routes.
 *
 * @see MenuViewInitializerImpl Concrete implementation
 * @see NavigationController Unified navigation system
 */
interface MenuViewInitializer {
    fun initializeViews(view: View): MenuViews
    fun setupClickListeners(
            views: MenuViews,
            actionHandler: MenuActionHandler,
            navigationController: NavigationController? = null
    )
    fun setupDynamicTitle(views: MenuViews)
    fun configureInitialViewStates(views: MenuViews)
}

/**
 * Implementation of MenuViewInitializer. Responsible for initializing all view references and
 * configuring listeners.
 */
class MenuViewInitializerImpl(private val fragment: Fragment) : MenuViewInitializer {

    override fun initializeViews(view: View): MenuViews {
        MenuLogger.lifecycle("MenuViewInitializer: initializeViews START")

        val menuViews =
                MenuViews(
                        menuContainer = view.findViewById(R.id.menu_container),
                        continueMenu = view.findViewById(R.id.menu_continue),
                        resetMenu = view.findViewById(R.id.menu_reset),
                        progressMenu = view.findViewById(R.id.menu_submenu2),
                        settingsMenu = view.findViewById(R.id.menu_submenu1),
                        aboutMenu = view.findViewById(R.id.menu_about),
                        exitMenu = view.findViewById(R.id.menu_exit),
                        menuItems =
                                listOf(
                                        view.findViewById(R.id.menu_continue),
                                        view.findViewById(R.id.menu_reset),
                                        view.findViewById(R.id.menu_submenu2),
                                        view.findViewById(R.id.menu_submenu1),
                                        view.findViewById(R.id.menu_about),
                                        view.findViewById(R.id.menu_exit)
                                ),
                        continueTitle = view.findViewById(R.id.continue_title),
                        resetTitle = view.findViewById(R.id.reset_title),
                        progressTitle = view.findViewById(R.id.submenu2_title),
                        settingsTitle = view.findViewById(R.id.submenu1_title),
                        aboutTitle = view.findViewById(R.id.about_title),
                        exitTitle = view.findViewById(R.id.exit_title),
                        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue),
                        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset),
                        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2),
                        selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1),
                        selectionArrowAbout = view.findViewById(R.id.selection_arrow_about),
                        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit),
                        titleTextView = view.findViewById(R.id.menu_title)
                )

        MenuLogger.lifecycle("MenuViewInitializer: initializeViews COMPLETED")
        return menuViews
    }

    override fun setupClickListeners(
            views: MenuViews,
            actionHandler: MenuActionHandler,
            navigationController: NavigationController?
    ) {
        MenuLogger.lifecycle("MenuViewInitializer: setupClickListeners START")

        // PHASE 3.3a: Touch events routed through NavigationController (permanently enabled)
        Log.d(
                TAG,
                "[TOUCH] Using new navigation system - touch routed through NavigationController"
        )
        if (navigationController == null) {
            MenuLogger.lifecycle("MenuViewInitializer: navigationController missing - listeners not configured")
            return
        }

        setupTouchNavigationSystem(views, navigationController)

        MenuLogger.lifecycle("MenuViewInitializer: setupClickListeners COMPLETED")
    }

    /**
     * PHASE 3: New touch navigation system using NavigationController. Touch events create
     * SelectItem + ActivateSelected after 100ms delay.
     */
    private fun setupTouchNavigationSystem(
            views: MenuViews,
            navigationController: NavigationController
    ) {
        // For each menu item, setup touch listener that routes through NavigationController
        views.menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                Log.d(
                        TAG,
                        "[TOUCH] Menu item $index clicked - routing through NavigationController"
                )

                // PHASE 3.3b: Implement focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                navigationController.selectItem(index)

                // 2. After TOUCH_ACTIVATION_DELAY_MS delay, activate item
                it.postDelayed(
                        {
                            Log.d(TAG, "[TOUCH] Activating item $index after delay")
                            navigationController.activateItem()
                        },
                        MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS
                ) // MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS = focus-then-activate delay
            }
        }
    }

    companion object {
        private const val TAG = "MenuViewInitializer"
    }

    override fun setupDynamicTitle(views: MenuViews) {
        MenuLogger.lifecycle("MenuViewInitializer: setupDynamicTitle START")

        val titleStyle = fragment.resources.getInteger(R.integer.rm_title_style)
        val titleText =
                when (titleStyle) {
                    1 -> fragment.resources.getString(R.string.conf_name)
                    else -> fragment.resources.getString(R.string.rm_title)
                }

        views.titleTextView.text = titleText

        // Apply configured capitalization to the title
        FontUtils.applyTextCapitalization(fragment.requireContext(), views.titleTextView)

        // Ensure Arcade font on the title
        FontUtils.applySelectedFont(fragment.requireContext(), views.titleTextView)

        MenuLogger.lifecycle("MenuViewInitializer: setupDynamicTitle COMPLETED")
    }

    override fun configureInitialViewStates(views: MenuViews) {
        MenuLogger.lifecycle("MenuViewInitializer: configureInitialViewStates START")

        // Configure RetroCardViews to not use background colors - selection shown only by text
        // color and arrows
        views.continueMenu.setUseBackgroundColor(false)
        views.resetMenu.setUseBackgroundColor(false)
        views.progressMenu.setUseBackgroundColor(false)
        views.settingsMenu.setUseBackgroundColor(false)
        views.aboutMenu.setUseBackgroundColor(false)
        views.exitMenu.setUseBackgroundColor(false)

        // Force zero marginStart for all arrows to prevent spacing issues
        listOf(
                        views.selectionArrowContinue,
                        views.selectionArrowReset,
                        views.selectionArrowSettings,
                        views.selectionArrowProgress,
                        views.selectionArrowAbout,
                        views.selectionArrowExit
                )
                .forEach { arrow ->
                    (arrow.layoutParams as? android.widget.LinearLayout.LayoutParams)?.apply {
                        marginStart = 0
                        marginEnd = 0
                    }
                }

        MenuLogger.lifecycle("MenuViewInitializer: configureInitialViewStates COMPLETED")
    }
}
