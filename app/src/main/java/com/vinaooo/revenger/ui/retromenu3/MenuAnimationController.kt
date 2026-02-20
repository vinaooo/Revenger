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

                // Update title colors based on selection
                menuViews.continueTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_CONTINUE)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.continueTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.continueTitle.context,
                                        R.color.rm_normal_color
                                )
                )
                menuViews.resetTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_RESET)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.resetTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.resetTitle.context,
                                        R.color.rm_normal_color
                                )
                )
                menuViews.progressTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_PROGRESS)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.progressTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.progressTitle.context,
                                        R.color.rm_normal_color
                                )
                )
                menuViews.settingsTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_SETTINGS)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.settingsTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.settingsTitle.context,
                                        R.color.rm_normal_color
                                )
                )
                menuViews.aboutTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_ABOUT)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.aboutTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.aboutTitle.context,
                                        R.color.rm_normal_color
                                )
                )
                menuViews.exitTitle.setTextColor(
                        if (selectedIndex == MENU_ITEM_EXIT)
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.exitTitle.context,
                                        R.color.rm_selected_color
                                )
                        else
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.exitTitle.context,
                                        R.color.rm_normal_color
                                )
                )

                // Update selection arrow visibility and colors
                // Continue
                if (selectedIndex == MENU_ITEM_CONTINUE) {
                        menuViews.selectionArrowContinue.setTextColor(
                                androidx.core.content.ContextCompat.getColor(
                                        menuViews.selectionArrowContinue.context,
                                        R.color.rm_selected_color
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
                                        R.color.rm_selected_color
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
                                        R.color.rm_selected_color
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
                                        R.color.rm_selected_color
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
                                        R.color.rm_selected_color
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
                                        R.color.rm_selected_color
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
