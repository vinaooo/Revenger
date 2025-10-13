package com.vinaooo.revenger.ui.retromenu3

/**
 * Data class representing the complete configuration for a menu. Allows dynamic menu creation
 * without hardcoded values.
 */
data class MenuConfiguration(
        val menuId: String,
        val title: String,
        val items: List<MenuItem>,
        val defaultSelectedIndex: Int = 0,
        val allowNavigation: Boolean = true,
        val showBackButton: Boolean = true
) {
    init {
        require(items.isNotEmpty()) { "Menu configuration must have at least one item" }
        require(defaultSelectedIndex in items.indices) {
            "Default selected index must be within valid range"
        }
    }

    /** Get the default selected menu item */
    fun getDefaultSelectedItem(): MenuItem = items[defaultSelectedIndex]

    /** Find a menu item by its ID */
    fun findItemById(id: String): MenuItem? = items.find { it.id == id }

    /** Get all enabled items */
    fun getEnabledItems(): List<MenuItem> = items.filter { it.isEnabled }

    /** Check if the configuration is valid */
    fun isValid(): Boolean {
        return menuId.isNotBlank() &&
                title.isNotBlank() &&
                items.isNotEmpty() &&
                items.all { it.id.isNotBlank() && it.title.isNotBlank() }
    }
}
