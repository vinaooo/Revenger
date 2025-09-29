package com.vinaooo.revenger.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.vinaooo.revenger.R

/**
 * RecyclerView adapter for the Material You game menu items
 */
class MenuItemAdapter(
    private val clickListener: MenuItemClickListener
) : ListAdapter<GameMenuItem, MenuItemViewHolder>(MenuItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_option, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    /**
     * Update a specific menu item and refresh only that item
     */
    fun updateMenuItem(item: GameMenuItem) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentList[index] = item
            submitList(currentList)
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
class MenuItemDiffCallback : DiffUtil.ItemCallback<GameMenuItem>() {
    override fun areItemsTheSame(oldItem: GameMenuItem, newItem: GameMenuItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GameMenuItem, newItem: GameMenuItem): Boolean {
        return oldItem == newItem
    }
}
