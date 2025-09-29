package com.vinaooo.revenger.ui.menu

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vinaooo.revenger.R

/**
 * ViewHolder for menu items in the Material You game menu
 */
class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val icon: ImageView = itemView.findViewById(R.id.menu_item_icon)
    private val title: TextView = itemView.findViewById(R.id.menu_item_title)
    private val description: TextView = itemView.findViewById(R.id.menu_item_description)
    private val status: TextView = itemView.findViewById(R.id.menu_item_status)
    private val secondaryIcon: ImageView = itemView.findViewById(R.id.menu_item_secondary_icon)

    fun bind(item: GameMenuItem, clickListener: MenuItemClickListener?) {
        // Set basic content
        title.setText(item.titleRes)
        description.setText(item.descriptionRes)
        icon.setImageResource(item.iconRes)

        // Handle enabled/disabled state
        itemView.isEnabled = item.isEnabled
        itemView.alpha = if (item.isEnabled) 1.0f else 0.6f

        // Show/hide description
        description.visibility = View.VISIBLE

        // Handle status text
        if (item.statusText.isNullOrEmpty()) {
            status.visibility = View.GONE
        } else {
            status.visibility = View.VISIBLE
            status.text = item.statusText
        }

        // Handle secondary icon for toggleable items
        if (item.isToggleable) {
            secondaryIcon.visibility = View.VISIBLE
            updateToggleState(item)
        } else {
            secondaryIcon.visibility = View.GONE
        }

        // Set click listener
        itemView.setOnClickListener {
            if (item.isEnabled) {
                clickListener?.onMenuItemClick(item)
            }
        }
    }

    private fun updateToggleState(item: GameMenuItem) {
        when (item.id) {
            MenuItemId.TOGGLE_AUDIO -> {
                val iconRes = if (item.isToggled) R.drawable.ic_volume_on_24 else R.drawable.ic_volume_off_24
                icon.setImageResource(iconRes)

                val titleRes = if (item.isToggled) R.string.menu_unmute else R.string.menu_mute
                title.setText(titleRes)
            }
            MenuItemId.FAST_FORWARD -> {
                val alpha = if (item.isToggled) 1.0f else 0.6f
                icon.alpha = alpha

                val statusRes = if (item.isToggled) R.string.fast_forward_active else R.string.fast_forward_inactive
                status.setText(statusRes)
                status.visibility = View.VISIBLE
            }
            else -> {
                // Other toggleable items can be handled here
            }
        }
    }
}
