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
        // Main container
        menuContainerView = view.findViewById(R.id.menu_container)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        progressMenu = view.findViewById(R.id.menu_submenu2)
        settingsMenu = view.findViewById(R.id.menu_submenu1)
        aboutMenu = view.findViewById(R.id.menu_about)
        exitMenu = view.findViewById(R.id.menu_exit)

        // Initialize ordered list of menu items
        menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, aboutMenu, exitMenu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        settingsTitle = view.findViewById(R.id.submenu1_title)
        progressTitle = view.findViewById(R.id.submenu2_title)
        aboutTitle = view.findViewById(R.id.about_title)
        exitTitle = view.findViewById(R.id.exit_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2)
        selectionArrowAbout = view.findViewById(R.id.selection_arrow_about)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)

        // Force zero marginStart for all arrows to prevent spacing issues
        listOf(
                        selectionArrowContinue,
                        selectionArrowReset,
                        selectionArrowSettings,
                        selectionArrowProgress,
                        selectionArrowAbout,
                        selectionArrowExit
                )
                .forEach { arrow ->
                    (arrow.layoutParams as? LinearLayout.LayoutParams)?.apply {
                        marginStart = 0
                        marginEnd = 0
                    }
                }

        // Initialize menu item views list
        menuItemViews =
                listOf(
                        MenuItemView(continueTitle, selectionArrowContinue, continueMenu),
                        MenuItemView(resetTitle, selectionArrowReset, resetMenu),
                        MenuItemView(progressTitle, selectionArrowProgress, progressMenu),
                        MenuItemView(settingsTitle, selectionArrowSettings, settingsMenu),
                        MenuItemView(aboutTitle, selectionArrowAbout, aboutMenu),
                        MenuItemView(exitTitle, selectionArrowExit, exitMenu)
                )

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                fragment.requireContext(),
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                aboutTitle,
                exitTitle,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowAbout,
                selectionArrowExit
        )

        // Apply configured capitalization to menu texts (after font)
        FontUtils.applyTextCapitalization(
                fragment.requireContext(),
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                aboutTitle,
                exitTitle
        )

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
