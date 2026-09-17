package com.vinaooo.revenger.ui.retromenu3


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.MenuLogger

/** Interface for controlling menu animations. */
interface MenuAnimationController {
        fun setMenuViews(menuViews: MenuViews)
        fun animateMenuIn(onComplete: (() -> Unit)? = null)
        fun animateMenuOut(onComplete: (() -> Unit)? = null)
        fun animateItemSelection(fromIndex: Int, toIndex: Int, onComplete: (() -> Unit)? = null)
        fun updateSelectionVisual(selectedIndex: Int)
        fun dismissMenu(onAnimationEnd: (() -> Unit)? = null)
}

/**
 * Implementation of MenuAnimationController. Manages menu in/out animations using
 * ViewPropertyAnimator.
 */
class MenuAnimationControllerImpl : MenuAnimationController {

        private lateinit var menuViews: MenuViews

        override fun setMenuViews(menuViews: MenuViews) {
                this.menuViews = menuViews
        }

        override fun animateMenuIn(onComplete: (() -> Unit)?) {
                MenuLogger.lifecycle("MenuAnimationController: animateMenuIn START")

                // Set initial state
                menuViews.menuContainer.alpha = 0f

                // Animate in
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

                // Animate out
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

                updateTitleColors(selectedIndex)
                updateSelectionArrows(selectedIndex)
        }

        /** Updates the text color of every menu item title based on which index is selected. */
        private fun updateTitleColors(selectedIndex: Int) {
                applyTitleColor(menuViews.continueTitle, selectedIndex == MENU_ITEM_CONTINUE)
                applyTitleColor(menuViews.resetTitle, selectedIndex == MENU_ITEM_RESET)
                applyTitleColor(menuViews.progressTitle, selectedIndex == MENU_ITEM_PROGRESS)
                applyTitleColor(menuViews.settingsTitle, selectedIndex == MENU_ITEM_SETTINGS)
                applyTitleColor(menuViews.aboutTitle, selectedIndex == MENU_ITEM_ABOUT)
                applyTitleColor(menuViews.exitTitle, selectedIndex == MENU_ITEM_EXIT)
        }

        /** Updates visibility and color of every selection arrow based on which index is selected. */
        private fun updateSelectionArrows(selectedIndex: Int) {
                applyArrowVisibility(
                        menuViews.selectionArrowContinue,
                        selectedIndex == MENU_ITEM_CONTINUE
                )
                applyArrowVisibility(menuViews.selectionArrowReset, selectedIndex == MENU_ITEM_RESET)
                applyArrowVisibility(
                        menuViews.selectionArrowProgress,
                        selectedIndex == MENU_ITEM_PROGRESS
                )
                applyArrowVisibility(
                        menuViews.selectionArrowSettings,
                        selectedIndex == MENU_ITEM_SETTINGS
                )
                applyArrowVisibility(menuViews.selectionArrowAbout, selectedIndex == MENU_ITEM_ABOUT)
                applyArrowVisibility(menuViews.selectionArrowExit, selectedIndex == MENU_ITEM_EXIT)
        }

        /** Sets [view]'s text color to the selected or normal color depending on [isSelected]. */
        private fun applyTitleColor(view: android.widget.TextView, isSelected: Boolean) {
                view.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                                view.context,
                                if (isSelected) R.color.rm_selected_color else R.color.rm_normal_color
                        )
                )
        }

        /**
         * Shows [arrow] in the selected color when [isSelected] is true, otherwise hides it.
         */
        private fun applyArrowVisibility(arrow: android.widget.TextView, isSelected: Boolean) {
                if (isSelected) {
                        arrow.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        arrow.context,
                                        R.color.rm_selected_color
                                )
                        )
                        arrow.visibility = View.VISIBLE
                } else {
                        arrow.visibility = View.GONE
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
                /** Duration of the menu entrance animation in milliseconds */
                const val ANIMATION_DURATION_IN = 300L

                /** Duration of the menu exit animation in milliseconds */
                const val ANIMATION_DURATION_OUT = 200L

                /** Index of the Continue item in the menu */
                const val MENU_ITEM_CONTINUE = 0

                /** Index of the Reset item in the menu */
                const val MENU_ITEM_RESET = 1

                /** Index of the Progress item in the menu */
                const val MENU_ITEM_PROGRESS = 2

                /** Index of the Settings item in the menu */
                const val MENU_ITEM_SETTINGS = 3

                /** Index of the About item in the menu */
                const val MENU_ITEM_ABOUT = 4

                /** Index of the Exit item in the menu */
                const val MENU_ITEM_EXIT = 5
        }
}
