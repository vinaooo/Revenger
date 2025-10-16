package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.MenuLogger

/**
 * Data class que representa todas as views do menu RetroMenu3. Centraliza as referências para
 * evitar múltiplas findViewById.
 */
data class MenuViews(
        val menuContainer: LinearLayout,
        val continueMenu: RetroCardView,
        val resetMenu: RetroCardView,
        val progressMenu: RetroCardView,
        val settingsMenu: RetroCardView,
        val exitMenu: RetroCardView,
        val menuItems: List<RetroCardView>,
        val continueTitle: TextView,
        val resetTitle: TextView,
        val progressTitle: TextView,
        val settingsTitle: TextView,
        val exitTitle: TextView,
        val selectionArrowContinue: TextView,
        val selectionArrowReset: TextView,
        val selectionArrowProgress: TextView,
        val selectionArrowSettings: TextView,
        val selectionArrowExit: TextView,
        val controlsHint: TextView,
        val titleTextView: TextView
)

/** Interface para inicialização de views do menu. */
interface MenuViewInitializer {
    fun initializeViews(view: View): MenuViews
    fun setupClickListeners(views: MenuViews, onItemClick: (MenuItem) -> Unit)
    fun setupDynamicTitle(views: MenuViews)
    fun configureInitialViewStates(views: MenuViews)
}

/**
 * Implementação do MenuViewInitializer. Responsável por inicializar todas as referências de views e
 * configurar listeners.
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
                        exitMenu = view.findViewById(R.id.menu_exit),
                        menuItems =
                                listOf(
                                        view.findViewById(R.id.menu_continue),
                                        view.findViewById(R.id.menu_reset),
                                        view.findViewById(R.id.menu_submenu2),
                                        view.findViewById(R.id.menu_submenu1),
                                        view.findViewById(R.id.menu_exit)
                                ),
                        continueTitle = view.findViewById(R.id.continue_title),
                        resetTitle = view.findViewById(R.id.reset_title),
                        progressTitle = view.findViewById(R.id.submenu2_title),
                        settingsTitle = view.findViewById(R.id.submenu1_title),
                        exitTitle = view.findViewById(R.id.exit_title),
                        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue),
                        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset),
                        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2),
                        selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1),
                        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit),
                        controlsHint = view.findViewById(R.id.retro_menu3_controls_hint),
                        titleTextView = view.findViewById(R.id.menu_title)
                )

        MenuLogger.lifecycle("MenuViewInitializer: initializeViews COMPLETED")
        return menuViews
    }

    override fun setupClickListeners(views: MenuViews, onItemClick: (MenuItem) -> Unit) {
        MenuLogger.lifecycle("MenuViewInitializer: setupClickListeners START")

        views.continueMenu.setOnClickListener {
            onItemClick(MenuItem("continue", "Continue", action = MenuAction.CONTINUE))
        }
        views.resetMenu.setOnClickListener {
            onItemClick(MenuItem("reset", "Reset", action = MenuAction.RESET))
        }
        views.progressMenu.setOnClickListener {
            onItemClick(MenuItem("progress", "Progress", action = MenuAction.SAVE_STATE))
        }
        views.settingsMenu.setOnClickListener {
            onItemClick(MenuItem("settings", "Settings", action = MenuAction.TOGGLE_AUDIO))
        }
        views.exitMenu.setOnClickListener {
            onItemClick(MenuItem("exit", "Exit", action = MenuAction.EXIT))
        }

        MenuLogger.lifecycle("MenuViewInitializer: setupClickListeners COMPLETED")
    }

    override fun setupDynamicTitle(views: MenuViews) {
        MenuLogger.lifecycle("MenuViewInitializer: setupDynamicTitle START")

        val titleStyle = fragment.resources.getInteger(R.integer.retro_menu3_title_style)
        val titleText =
                when (titleStyle) {
                    1 -> fragment.resources.getString(R.string.config_name)
                    else -> fragment.resources.getString(R.string.retro_menu3_title)
                }

        views.titleTextView.text = titleText

        // Garantir fonte Arcade no título
        FontUtils.applyArcadeFont(fragment.requireContext(), views.titleTextView)

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
        views.exitMenu.setUseBackgroundColor(false)

        // Force zero marginStart for all arrows to prevent spacing issues
        listOf(
                        views.selectionArrowContinue,
                        views.selectionArrowReset,
                        views.selectionArrowSettings,
                        views.selectionArrowProgress,
                        views.selectionArrowExit
                )
                .forEach { arrow ->
                    (arrow.layoutParams as? android.widget.LinearLayout.LayoutParams)?.apply {
                        marginStart = 0
                        marginEnd = 0
                    }
                }

        // Configure controls hint
        views.controlsHint.apply {
            text = fragment.getString(R.string.retro_menu3_controls_hint)
            visibility = android.view.View.VISIBLE
            alpha = 1.0f
        }

        MenuLogger.lifecycle("MenuViewInitializer: configureInitialViewStates COMPLETED")
    }
}
