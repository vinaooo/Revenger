package com.vinaooo.revenger.ui.retromenu3

/**
 * Builder class for creating MenuConfiguration instances dynamically. Provides a fluent API for
 * menu construction.
 */
class MenuConfigurationBuilder {

    private var menuId: String = ""
    private var title: String = ""
    private var items: MutableList<MenuItem> = mutableListOf()
    private var defaultSelectedIndex: Int = 0
    private var allowNavigation: Boolean = true
    private var showBackButton: Boolean = true

    /** Set the menu ID */
    fun menuId(id: String): MenuConfigurationBuilder {
        this.menuId = id
        return this
    }

    /** Set the menu title */
    fun title(title: String): MenuConfigurationBuilder {
        this.title = title
        return this
    }

    /** Add a single menu item */
    fun addItem(item: MenuItem): MenuConfigurationBuilder {
        this.items.add(item)
        return this
    }

    /** Add multiple menu items */
    fun addItems(vararg items: MenuItem): MenuConfigurationBuilder {
        this.items.addAll(items)
        return this
    }

    /** Add multiple menu items from a list */
    fun addItems(items: List<MenuItem>): MenuConfigurationBuilder {
        this.items.addAll(items)
        return this
    }

    /** Set the default selected index */
    fun defaultSelectedIndex(index: Int): MenuConfigurationBuilder {
        this.defaultSelectedIndex = index
        return this
    }

    /** Set whether navigation is allowed */
    fun allowNavigation(allow: Boolean): MenuConfigurationBuilder {
        this.allowNavigation = allow
        return this
    }

    /** Set whether to show back button */
    fun showBackButton(show: Boolean): MenuConfigurationBuilder {
        this.showBackButton = show
        return this
    }

    /** Build the MenuConfiguration instance */
    fun build(): MenuConfiguration {
        require(menuId.isNotBlank()) { "Menu ID must be set" }
        require(title.isNotBlank()) { "Menu title must be set" }
        require(items.isNotEmpty()) { "At least one menu item must be added" }

        return MenuConfiguration(
                menuId = menuId,
                title = title,
                items = items.toList(), // Create immutable copy
                defaultSelectedIndex = defaultSelectedIndex,
                allowNavigation = allowNavigation,
                showBackButton = showBackButton
        )
    }

    companion object {
        /** Create a new builder instance */
        fun create(): MenuConfigurationBuilder = MenuConfigurationBuilder()

        /** Create a builder with default main menu configuration */
        fun createMainMenu(): MenuConfigurationBuilder {
            return create().menuId("main_menu")
                    .title("RetroMenu3")
                    .addItem(MenuItem("continue", "Continuar", action = MenuAction.CONTINUE))
                    .addItem(MenuItem("reset", "Restart", action = MenuAction.RESET))
                    .addItem(
                            MenuItem(
                                    "progress",
                                    "Progress",
                                    action = MenuAction.NAVIGATE(MenuState.PROGRESS_MENU)
                            )
                    )
                    .addItem(
                            MenuItem(
                                    "settings",
                                    "Settings",
                                    action = MenuAction.NAVIGATE(MenuState.SETTINGS_MENU)
                            )
                    )
                    .addItem(
                            MenuItem(
                                    "exit",
                                    "Exit",
                                    action = MenuAction.NAVIGATE(MenuState.EXIT_MENU)
                            )
                    )
                    .addItem(MenuItem("save_log", "Save Log", action = MenuAction.SAVE_LOG))
        }
    }
}
