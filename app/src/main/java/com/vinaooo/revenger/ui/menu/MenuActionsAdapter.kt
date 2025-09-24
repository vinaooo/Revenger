package com.vinaooo.revenger.ui.menu

import android.animation.AnimatorInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vinaooo.revenger.R

/**
 * Adapter for menu actions in the ExpressiveGameMenuBottomSheet 
 * Material 3 Expressive implementation with micro-interactions
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
        private val subtitleView: TextView = view.findViewById(R.id.action_subtitle)

        fun bind(action: MenuAction) {
            titleView.setText(action.titleResId)
            subtitleView.setText(action.subtitleResId)
            iconView.setImageResource(action.iconResId)

            itemView.setOnClickListener { onActionClick(action) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.menu_action_card_material3, parent, false)
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
                            R.string.menu_reset_expressive,
                            R.string.menu_reset_subtitle,
                            R.drawable.ic_restart_alt_expressive
                    ),
                    MenuAction(
                            MenuActionId.SAVE_STATE,
                            R.string.menu_save_expressive,
                            R.string.menu_save_state_subtitle,
                            R.drawable.ic_save_expressive
                    ),
                    MenuAction(
                            MenuActionId.LOAD_STATE,
                            R.string.menu_load_expressive,
                            R.string.menu_load_state_subtitle,
                            R.drawable.ic_folder_open_expressive
                    ),
                    MenuAction(
                            MenuActionId.MUTE,
                            R.string.menu_settings_expressive,
                            R.string.menu_settings_subtitle,
                            R.drawable.ic_settings_expressive
                    ),
                    MenuAction(
                            MenuActionId.FAST_FORWARD,
                            R.string.menu_exit_expressive,
                            R.string.menu_exit_subtitle,
                            R.drawable.ic_exit_to_app_expressive
                    )
            )
        }
    }
}
