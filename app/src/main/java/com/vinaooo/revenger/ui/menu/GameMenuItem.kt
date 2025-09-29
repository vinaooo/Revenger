package com.vinaooo.revenger.ui.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Data class representing a menu item in the Material You game menu
 */
data class GameMenuItem(
    val id: MenuItemId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val isToggleable: Boolean = false,
    var isToggled: Boolean = false,
    var statusText: String? = null,
    var isEnabled: Boolean = true
)

/**
 * Enum representing all available menu items
 */
enum class MenuItemId {
    RESET,
    SAVE_STATE,
    LOAD_STATE,
    TOGGLE_AUDIO,
    FAST_FORWARD
}

/**
 * Interface for handling menu item clicks
 */
interface MenuItemClickListener {
    fun onMenuItemClick(item: GameMenuItem)
}
