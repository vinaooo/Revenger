package com.vinaooo.revenger.ui.retromenu3


import android.view.View
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Represents a menu item composed of a title, selection arrow, and card view.
 *
 * **Value Object Pattern**: Encapsulates the three visual components of each menu item.
 *
 * @property titleTextView TextView for the item's title
 * @property arrowTextView TextView for the selection arrow (→)
 * @property cardView RetroCardView that contains the entire item
 */
data class MenuItemView(
        val titleTextView: TextView,
        val arrowTextView: TextView,
        val cardView: RetroCardView
)

/**
 * Finds all the raw menu views and derived collections [MenuViewManager.setupViews] needs, in one
 * shot. Split out of [MenuViewManager] (used only from that one call site) so the class stays
 * under the project's function-count threshold.
 */
private object MenuViewSetupBuilder {

    /** Everything [build] finds/derives from the inflated menu layout. */
    data class Result(
            val menuContainerView: LinearLayout,
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
            val menuItemViews: List<MenuItemView>
    )

    /** The main container plus each menu item card, found by [findCards]. */
    private data class Cards(
            val menuContainerView: LinearLayout,
            val continueMenu: RetroCardView,
            val resetMenu: RetroCardView,
            val progressMenu: RetroCardView,
            val settingsMenu: RetroCardView,
            val aboutMenu: RetroCardView,
            val exitMenu: RetroCardView,
            val menuItems: List<RetroCardView>
    )

    /** Every menu option title and selection arrow [TextView], found by [findTitlesAndArrows]. */
    private data class TitlesAndArrows(
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
            val selectionArrowExit: TextView
    ) {
        val titles: List<TextView>
            get() = listOf(continueTitle, resetTitle, progressTitle, settingsTitle, aboutTitle, exitTitle)
        val arrows: List<TextView>
            get() =
                    listOf(
                            selectionArrowContinue,
                            selectionArrowReset,
                            selectionArrowProgress,
                            selectionArrowSettings,
                            selectionArrowAbout,
                            selectionArrowExit
                    )
    }

    fun build(view: View, fragment: Fragment): Result {
        val cards = findCards(view)
        val titlesAndArrows = findTitlesAndArrows(view)
        resetArrowMargins(titlesAndArrows.arrows)
        applyFontsAndCapitalization(fragment, titlesAndArrows.titles, titlesAndArrows.arrows)

        return Result(
                menuContainerView = cards.menuContainerView,
                continueMenu = cards.continueMenu,
                resetMenu = cards.resetMenu,
                progressMenu = cards.progressMenu,
                settingsMenu = cards.settingsMenu,
                aboutMenu = cards.aboutMenu,
                exitMenu = cards.exitMenu,
                menuItems = cards.menuItems,
                continueTitle = titlesAndArrows.continueTitle,
                resetTitle = titlesAndArrows.resetTitle,
                progressTitle = titlesAndArrows.progressTitle,
                settingsTitle = titlesAndArrows.settingsTitle,
                aboutTitle = titlesAndArrows.aboutTitle,
                exitTitle = titlesAndArrows.exitTitle,
                selectionArrowContinue = titlesAndArrows.selectionArrowContinue,
                selectionArrowReset = titlesAndArrows.selectionArrowReset,
                selectionArrowProgress = titlesAndArrows.selectionArrowProgress,
                selectionArrowSettings = titlesAndArrows.selectionArrowSettings,
                selectionArrowAbout = titlesAndArrows.selectionArrowAbout,
                selectionArrowExit = titlesAndArrows.selectionArrowExit,
                menuItemViews = buildMenuItemViews(cards, titlesAndArrows)
        )
    }

    /** Finds the main container and each menu item card, plus the ordered [Cards.menuItems] list. */
    private fun findCards(view: View): Cards {
        val continueMenu = view.findViewById<RetroCardView>(R.id.menu_continue)
        val resetMenu = view.findViewById<RetroCardView>(R.id.menu_reset)
        val progressMenu = view.findViewById<RetroCardView>(R.id.menu_submenu2)
        val settingsMenu = view.findViewById<RetroCardView>(R.id.menu_submenu1)
        val aboutMenu = view.findViewById<RetroCardView>(R.id.menu_about)
        val exitMenu = view.findViewById<RetroCardView>(R.id.menu_exit)

        return Cards(
                menuContainerView = view.findViewById(R.id.menu_container),
                continueMenu = continueMenu,
                resetMenu = resetMenu,
                progressMenu = progressMenu,
                settingsMenu = settingsMenu,
                aboutMenu = aboutMenu,
                exitMenu = exitMenu,
                menuItems =
                        listOf(
                                continueMenu,
                                resetMenu,
                                progressMenu,
                                settingsMenu,
                                aboutMenu,
                                exitMenu
                        )
        )
    }

    /** Finds every menu option title [TextView] and selection arrow [TextView]. */
    private fun findTitlesAndArrows(view: View): TitlesAndArrows =
            TitlesAndArrows(
                    continueTitle = view.findViewById(R.id.continue_title),
                    resetTitle = view.findViewById(R.id.reset_title),
                    settingsTitle = view.findViewById(R.id.submenu1_title),
                    progressTitle = view.findViewById(R.id.submenu2_title),
                    aboutTitle = view.findViewById(R.id.about_title),
                    exitTitle = view.findViewById(R.id.exit_title),
                    selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue),
                    selectionArrowReset = view.findViewById(R.id.selection_arrow_reset),
                    selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1),
                    selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2),
                    selectionArrowAbout = view.findViewById(R.id.selection_arrow_about),
                    selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)
            )

    /** Forces zero marginStart/marginEnd on all selection [arrows] to prevent spacing issues. */
    private fun resetArrowMargins(arrows: List<TextView>) {
        arrows.forEach { arrow ->
            (arrow.layoutParams as? LinearLayout.LayoutParams)?.apply {
                marginStart = 0
                marginEnd = 0
            }
        }
    }

    /** Builds the ordered [MenuItemView] list pairing each title, arrow, and card. */
    private fun buildMenuItemViews(cards: Cards, titlesAndArrows: TitlesAndArrows): List<MenuItemView> =
            listOf(
                    MenuItemView(
                            titlesAndArrows.continueTitle,
                            titlesAndArrows.selectionArrowContinue,
                            cards.continueMenu
                    ),
                    MenuItemView(
                            titlesAndArrows.resetTitle,
                            titlesAndArrows.selectionArrowReset,
                            cards.resetMenu
                    ),
                    MenuItemView(
                            titlesAndArrows.progressTitle,
                            titlesAndArrows.selectionArrowProgress,
                            cards.progressMenu
                    ),
                    MenuItemView(
                            titlesAndArrows.settingsTitle,
                            titlesAndArrows.selectionArrowSettings,
                            cards.settingsMenu
                    ),
                    MenuItemView(
                            titlesAndArrows.aboutTitle,
                            titlesAndArrows.selectionArrowAbout,
                            cards.aboutMenu
                    ),
                    MenuItemView(
                            titlesAndArrows.exitTitle,
                            titlesAndArrows.selectionArrowExit,
                            cards.exitMenu
                    )
            )

    /**
     * Applies the arcade font to [titles] + [arrows], then the configured text capitalization to
     * [titles] only, matching the prior (pre-extraction) behavior.
     */
    private fun applyFontsAndCapitalization(
            fragment: Fragment,
            titles: List<TextView>,
            arrows: List<TextView>
    ) {
        ViewUtils.applySelectedFontToViews(fragment.requireContext(), titles + arrows)
        FontUtils.applyTextCapitalization(fragment.requireContext(), titles)
    }
}

/**
 * Show/hide/dim visual-state transitions of the RetroMenu3 main menu container. Split out of
 * [MenuViewManager] so it stays under the project's function-count threshold, and exposed back on
 * it unchanged via interface delegation. Call [attachContainer]/[attachTitle] whenever
 * [MenuViewManager] (re)initializes the underlying views (`setupViews()`/`setupDynamicTitle()`) --
 * both may be called independently and in either order.
 */
interface MainMenuVisibility {
    fun dimMainMenu()
    fun restoreMainMenu()
    fun hideMainMenu()
    fun hideMainMenuCompletely()
    fun hideMainMenuTexts()
    fun showMainMenuTexts()
    fun showMainMenu(preserveSelection: Boolean = false)
}

class MainMenuVisibilityController : MainMenuVisibility {

    companion object {
        private const val TAG = "MenuViewManager"
        private const val MAIN_MENU_DIM_ALPHA = 0.3f
    }

    private var menuContainerView: LinearLayout? = null
    private var menuItemViews: List<MenuItemView>? = null
    private var menuTitleTextView: TextView? = null

    /** Called from `setupViews()` once the container and per-item views exist. */
    fun attachContainer(containerView: LinearLayout, itemViews: List<MenuItemView>) {
        menuContainerView = containerView
        menuItemViews = itemViews
    }

    /** Called from `setupDynamicTitle()` once the title view exists. */
    fun attachTitle(titleTextView: TextView?) {
        menuTitleTextView = titleTextView
    }

    /** Put the main menu in the background (when a submenu opens) */
    override fun dimMainMenu() {
        val container =
                menuContainerView
                        ?: run {
                            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot dim main menu")
                            return
                        }
        container.alpha = MAIN_MENU_DIM_ALPHA
    }

    /** Return the main menu to normal (when submenu is closed) */
    override fun restoreMainMenu() {
        val container =
                menuContainerView
                        ?: run {
                            Log.e(
                                    TAG,
                                    "[VIEW] MenuContainer not initialized, cannot restore main menu"
                            )
                            return
                        }
        container.alpha = 1.0f
    }

    /** Make main menu invisible (when submenu is opened) */
    override fun hideMainMenu() {
        val container =
                menuContainerView
                        ?: run {
                            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot hide main menu")
                            return
                        }
        container.visibility = View.INVISIBLE
    }

    /** Oculta completamente o menu principal de uma vez (para evitar piscada visual) */
    override fun hideMainMenuCompletely() {
        val container =
                menuContainerView
                        ?: run {
                            Log.e(
                                    TAG,
                                    "[VIEW] MenuContainer not initialized, cannot hide main menu completely"
                            )
                            return
                        }
        container.visibility = View.INVISIBLE
        menuItemViews?.forEach { menuItemView ->
            menuItemView.titleTextView.visibility = View.INVISIBLE
            menuItemView.arrowTextView.visibility = View.INVISIBLE
        }
        menuTitleTextView?.visibility = View.INVISIBLE
    }

    /** Hide main menu texts completely (when a submenu is active) */
    override fun hideMainMenuTexts() {
        val items =
                menuItemViews
                        ?: run {
                            Log.e(
                                    TAG,
                                    "[VIEW] MenuItemViews not initialized, cannot hide main menu texts"
                            )
                            return
                        }
        items.forEach { menuItemView ->
            menuItemView.titleTextView.visibility = View.INVISIBLE
            menuItemView.arrowTextView.visibility = View.INVISIBLE
        }
        menuTitleTextView?.visibility = View.INVISIBLE
    }

    /** Show main menu texts again (when submenu is closed) */
    override fun showMainMenuTexts() {
        val items =
                menuItemViews
                        ?: run {
                            Log.e(
                                    TAG,
                                    "[VIEW] MenuItemViews not initialized, cannot show main menu texts"
                            )
                            return
                        }
        items.forEach { menuItemView ->
            menuItemView.titleTextView.visibility = View.VISIBLE
            menuItemView.arrowTextView.visibility = View.VISIBLE
        }
        menuTitleTextView?.visibility = View.VISIBLE
    }

    /** Make main menu visible again (when submenu is closed) */
    override fun showMainMenu(preserveSelection: Boolean) {
        Log.d(TAG, "[VIEW] showMainMenu called with preserveSelection=$preserveSelection")
        val container =
                menuContainerView
                        ?: run {
                            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot show main menu")
                            return
                        }
        container.visibility = View.VISIBLE
        container.alpha = 1.0f
    }
}

/**
 * Applies the selected/unselected color, arrow, and highlight state to a single [MenuItemView].
 * Pure per-item styling extracted out of [MenuViewManager.updateSelectionVisual].
 */
class MenuItemSelectionStyler(private val fragment: Fragment) {

    /** Define um item de menu como selecionado */
    fun markSelected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.rm_selected_color
                )
        )
        menuItemView.arrowTextView.apply {
            setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            fragment.requireContext(),
                            R.color.rm_selected_color
                    )
            )
            visibility = View.VISIBLE
            (layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Force zero margin - critical fix
                marginEnd =
                        fragment.resources.getDimensionPixelSize(
                                R.dimen.rm_arrow_margin_end
                        )
                leftMargin = 0 // Additional force for left margin
            }
        }
        // RetroCardView usa estados internos para visual
        menuItemView.cardView.setState(RetroCardView.State.SELECTED)
    }

    /** Mark a menu item as unselected */
    fun markUnselected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.rm_normal_color
                )
        )
        menuItemView.arrowTextView.visibility = View.GONE
        // RetroCardView volta ao estado normal
        menuItemView.cardView.setState(RetroCardView.State.NORMAL)
    }
}

/**
 * RetroMenu3 menu views manager.
 *
 * **Responsibilities**:
 * - Initial configuration of all menu views
 * - Visual update of selection (colors, arrows, highlight)
 * - Management of entry/exit animations
 * - Dynamic visual state (enabled/disabled items)
 *
 * **Manager Pattern**: Centralizes all menu UI logic in a dedicated class.
 *
 * **Integration**:
 * - Works with MenuViewInitializer for initial setup
 * - Uses FontUtils and ViewUtils for styling
 * - Coordinates with MenuAnimationController for transitions
 *
 * **Phase 3**: Supports multi-input (gamepad, keyboard, touch) with unified visual feedback.
 *
 * @param fragment Fragment that contains the views (RetroMenu3Fragment)
 *
 * @see MenuViewInitializer Initializes touch navigation system
 * @see MenuAnimationController Controls menu animations
 * @see RetroCardView Componente customizado de card
 * @see MainMenuVisibilityController Show/hide/dim of the main menu container (delegated)
 * @see MenuItemSelectionStyler Per-item selected/unselected styling
 */
class MenuViewManager(
        private val fragment: Fragment,
        private val visibilityController: MainMenuVisibilityController = MainMenuVisibilityController(),
        private val itemStyler: MenuItemSelectionStyler = MenuItemSelectionStyler(fragment)
) : MainMenuVisibility by visibilityController {

    companion object {
        private const val TAG = "MenuViewManager"
    }

    // Menu item views
    private lateinit var menuContainerView: LinearLayout
    private lateinit var continueMenu: RetroCardView
    private lateinit var resetMenu: RetroCardView
    private lateinit var progressMenu: RetroCardView
    private lateinit var settingsMenu: RetroCardView
    private lateinit var aboutMenu: RetroCardView
    private lateinit var exitMenu: RetroCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Ordered list of menu item views for unified selection handling
    private lateinit var menuItemViews: List<MenuItemView>

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var aboutTitle: TextView
    private lateinit var exitTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowAbout: TextView
    private lateinit var selectionArrowExit: TextView

    // Main menu title
    private var menuTitleTextView: TextView? = null

    /** Configures the dynamic menu title based on the configured style */
    fun setupDynamicTitle(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.menu_title)
        menuTitleTextView = titleTextView // Store reference for hiding/showing
        visibilityController.attachTitle(titleTextView)
        Log.d(
                TAG,
                "[VIEW] setupDynamicTitle - menuTitleTextView initialized: " +
                        "${menuTitleTextView != null}, id: ${titleTextView?.id}"
        )
        val titleStyle = fragment.resources.getInteger(R.integer.rm_title_style)

        val titleText =
                when (titleStyle) {
                    1 -> fragment.resources.getString(R.string.name)
                    else -> fragment.resources.getString(R.string.rm_title)
                }

        titleTextView?.text = titleText

        // Ensure Arcade font on the title
        titleTextView?.let { FontUtils.applySelectedFont(fragment.requireContext(), it) }
    }

    /** Inicializa todas as views do menu e estruturas de dados relacionadas */
    fun setupViews(view: View) {
        val setup = MenuViewSetupBuilder.build(view, fragment)

        menuContainerView = setup.menuContainerView
        continueMenu = setup.continueMenu
        resetMenu = setup.resetMenu
        progressMenu = setup.progressMenu
        settingsMenu = setup.settingsMenu
        aboutMenu = setup.aboutMenu
        exitMenu = setup.exitMenu
        menuItems = setup.menuItems

        continueTitle = setup.continueTitle
        resetTitle = setup.resetTitle
        progressTitle = setup.progressTitle
        settingsTitle = setup.settingsTitle
        aboutTitle = setup.aboutTitle
        exitTitle = setup.exitTitle

        selectionArrowContinue = setup.selectionArrowContinue
        selectionArrowReset = setup.selectionArrowReset
        selectionArrowProgress = setup.selectionArrowProgress
        selectionArrowSettings = setup.selectionArrowSettings
        selectionArrowAbout = setup.selectionArrowAbout
        selectionArrowExit = setup.selectionArrowExit

        menuItemViews = setup.menuItemViews

        // Set first item as selected
        updateSelectionVisual(0)

        visibilityController.attachContainer(menuContainerView, menuItemViews)
    }

    /** Atualiza o estado visual do menu (itens dinâmicos, estados, etc.) */
    fun updateMenuState() {
        // Main menu no longer has dynamic options - everything was moved to submenus
    }

    /** Anima a entrada do menu na tela */
    fun animateMenuIn() {
        // Use optimized batch animation for better performance
        ViewUtils.animateMenuViewsBatchOptimized(
                arrayOf(menuContainerView),
                toAlpha = 1f,
                toScale = 1f,
                duration = 200
        )
    }

    /** Animate the menu exiting the screen */
    fun animateMenuOut(onEnd: () -> Unit) {
        // Use optimized batch animation with callback
        ViewUtils.animateMenuViewsBatchOptimized(
                arrayOf(menuContainerView),
                toAlpha = 0f,
                toScale = 0.8f,
                duration = 150
        ) { onEnd() }
    }

    /** Update the selection visual based on the current index */
    fun updateSelectionVisual(currentIndex: Int) {
        // Update each menu item view based on selection state
        menuItemViews.forEachIndexed { index, menuItemView ->
            if (index == currentIndex) {
                itemStyler.markSelected(menuItemView)
            } else {
                itemStyler.markUnselected(menuItemView)
            }
        }

        // Layout will be updated automatically when visibility changes
    }

    // Getters for accessing views when needed
    fun getMenuItems(): List<RetroCardView> = menuItems
    fun getMenuItemViews(): List<MenuItemView> = menuItemViews

    /** Checks if the menu views have been initialized */
    fun isViewsInitialized(): Boolean = ::menuItemViews.isInitialized
}
