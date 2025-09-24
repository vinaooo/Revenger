package com.vinaooo.revenger.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vinaooo.revenger.R

/**
 * Adapter for menu actions in the ExpressiveGameMenuBottomSheet Sprint 2: Basic functional adapter
 * for menu actions
 */
class MenuActionsAdapter(
        private val actions: List<MenuAction>,
        private val onActionClick: (MenuAction) -> Unit
) : RecyclerView.Adapter<MenuActionsAdapter.ActionViewHolder>() {

    /** Data class representing a menu action */
    data class MenuAction(
            val id: MenuActionId,
            val titleResId: Int,
            val subtitleResId: Int,
            val iconResId: Int,
            val isEnabled: Boolean = true
    )

    /** Enum for menu action identifiers */
    enum class MenuActionId {
        RESET,
        SAVE_STATE,
        LOAD_STATE,
        MUTE,
        FAST_FORWARD
    }

    /** ViewHolder for individual menu action cards */
    inner class ActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView: ImageView = view.findViewById(R.id.action_icon)
        private val titleView: TextView = view.findViewById(R.id.action_title)

        fun bind(action: MenuAction) {
            titleView.setText(action.titleResId)
            iconView.setImageResource(action.iconResId)

            itemView.setOnClickListener { onActionClick(action) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.menu_action_card_simple, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount(): Int = actions.size

    companion object {
        /** Create default menu actions matching original AlertDialog functionality */
        fun createDefaultActions(): List<MenuAction> {
            return listOf(
                    MenuAction(
                            MenuActionId.RESET,
                            android.R.string.cancel, // "Reset"
                            android.R.string.cancel, // Placeholder subtitle
                            android.R.drawable.ic_menu_revert
                    ),
                    MenuAction(
                            MenuActionId.SAVE_STATE,
                            android.R.string.copy, // "Save"
                            android.R.string.copy,
                            android.R.drawable.ic_menu_save
                    ),
                    MenuAction(
                            MenuActionId.LOAD_STATE,
                            android.R.string.paste, // "Load"
                            android.R.string.paste,
                            android.R.drawable.ic_menu_upload
                    ),
                    MenuAction(
                            MenuActionId.MUTE,
                            android.R.string.yes, // "Mute"
                            android.R.string.yes,
                            android.R.drawable.ic_lock_silent_mode
                    ),
                    MenuAction(
                            MenuActionId.FAST_FORWARD,
                            android.R.string.ok, // "Fast Forward"
                            android.R.string.ok,
                            android.R.drawable.ic_media_ff
                    )
            )
        }
    }
}
