package com.vinaooo.revenger.ui.retromenu3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.MenuLogger

/** Interface para controle de animações do menu. */
interface MenuAnimationController {
        fun setMenuViews(menuViews: MenuViews)
        fun animateMenuIn(onComplete: (() -> Unit)? = null)
        fun animateMenuOut(onComplete: (() -> Unit)? = null)
        fun animateItemSelection(fromIndex: Int, toIndex: Int, onComplete: (() -> Unit)? = null)
        fun updateSelectionVisual(selectedIndex: Int)
        fun dismissMenu(onAnimationEnd: (() -> Unit)? = null)
}

/**
 * Implementação do MenuAnimationController. Gerencia animações de entrada/saída do menu usando
 * ViewPropertyAnimator.
 */
class MenuAnimationControllerImpl : MenuAnimationController {

        private lateinit var menuViews: MenuViews

        override fun setMenuViews(menuViews: MenuViews) {
                this.menuViews = menuViews
        }

        override fun animateMenuIn(onComplete: (() -> Unit)?) {
                MenuLogger.lifecycle("MenuAnimationController: animateMenuIn START")

                // Configurar estado inicial
                menuViews.menuContainer.alpha = 0f

                // Animar entrada
                menuViews
                        .menuContainer
                        .animate()
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION_IN)
                        .setListener(
                                object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                                MenuLogger.lifecycle(
                                                        "MenuAnimationController: animateMenuIn COMPLETED"
                                                )
                                                onComplete?.invoke()
                                        }
                                }
                        )
        }

        override fun animateMenuOut(onComplete: (() -> Unit)?) {
                MenuLogger.lifecycle("MenuAnimationController: animateMenuOut START")

                // Animar saída
                menuViews
                        .menuContainer
                        .animate()
                        .alpha(0f)
                        .setDuration(ANIMATION_DURATION_OUT)
                        .setListener(
                                object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                                MenuLogger.lifecycle(
                                                        "MenuAnimationController: animateMenuOut COMPLETED"
                                                )
                                                onComplete?.invoke()
                                        }
                                }
                        )
        }

        override fun animateItemSelection(fromIndex: Int, toIndex: Int, onComplete: (() -> Unit)?) {
                MenuLogger.lifecycle(
                        "MenuAnimationController: animateItemSelection from $fromIndex to $toIndex"
                )

                // Simple fade transition between selections
                // Could be enhanced with more sophisticated animations in the future
                updateSelectionVisual(toIndex)
                onComplete?.invoke()
        }

        override fun updateSelectionVisual(selectedIndex: Int) {
                MenuLogger.lifecycle(
                        "MenuAnimationController: updateSelectionVisual for index $selectedIndex"
                )

                // Update title colors based on selection
                menuViews.continueTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_CONTINUE)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.continueTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.continueTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )
                menuViews.resetTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_RESET)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.resetTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.resetTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )
                menuViews.progressTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_PROGRESS)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.progressTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.progressTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )
                menuViews.settingsTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_SETTINGS)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.settingsTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.settingsTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )
                menuViews.aboutTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_ABOUT)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.aboutTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.aboutTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )
                menuViews.exitTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_EXIT)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.exitTitle.context,
                                        R.color.retro_menu3_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.exitTitle.context,
                                        R.color.retro_menu3_normal_color
                                )
                )

                // Update selection arrow visibility and colors
                // Continue
                if (selectedIndex == MENU_ITEM_CONTINUE) {
                        menuViews.selectionArrowContinue.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowContinue.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowContinue.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowContinue.visibility = View.GONE
                }

                // Reset
                if (selectedIndex == MENU_ITEM_RESET) {
                        menuViews.selectionArrowReset.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowReset.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowReset.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowReset.visibility = View.GONE
                }

                // Progress
                if (selectedIndex == MENU_ITEM_PROGRESS) {
                        menuViews.selectionArrowProgress.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowProgress.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowProgress.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowProgress.visibility = View.GONE
                }

                // Settings
                if (selectedIndex == MENU_ITEM_SETTINGS) {
                        menuViews.selectionArrowSettings.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowSettings.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowSettings.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowSettings.visibility = View.GONE
                }

                // About
                if (selectedIndex == MENU_ITEM_ABOUT) {
                        menuViews.selectionArrowAbout.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowAbout.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowAbout.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowAbout.visibility = View.GONE
                }

                // Exit
                if (selectedIndex == MENU_ITEM_EXIT) {
                        menuViews.selectionArrowExit.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowExit.context,
                                        R.color.retro_menu3_selected_color
                                )
                        )
                        menuViews.selectionArrowExit.visibility = View.VISIBLE
                } else {
                        menuViews.selectionArrowExit.visibility = View.GONE
                }
        }

        override fun dismissMenu(onAnimationEnd: (() -> Unit)?) {
                MenuLogger.lifecycle("MenuAnimationController: dismissMenu START")

                animateMenuOut {
                        menuViews.menuContainer.visibility = View.GONE
                        MenuLogger.lifecycle("MenuAnimationController: dismissMenu COMPLETED")
                        onAnimationEnd?.invoke()
                }
        }

        companion object {
                /** Duração da animação de entrada do menu em milissegundos */
                const val ANIMATION_DURATION_IN = 300L

                /** Duração da animação de saída do menu em milissegundos */
                const val ANIMATION_DURATION_OUT = 200L

                /** Índice do item Continue no menu */
                const val MENU_ITEM_CONTINUE = 0

                /** Índice do item Reset no menu */
                const val MENU_ITEM_RESET = 1

                /** Índice do item Progress no menu */
                const val MENU_ITEM_PROGRESS = 2

                /** Índice do item Settings no menu */
                const val MENU_ITEM_SETTINGS = 3

                /** Índice do item About no menu */
                const val MENU_ITEM_ABOUT = 4

                /** Índice do item Exit no menu */
                const val MENU_ITEM_EXIT = 5
        }
}
