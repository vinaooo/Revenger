package com.vinaooo.revenger.ui.retromenu3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
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
                .setDuration(300)
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
                .setDuration(200)
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
                if (selectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        menuViews.resetTitle.setTextColor(
                if (selectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        menuViews.progressTitle.setTextColor(
                if (selectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        menuViews.settingsTitle.setTextColor(
                if (selectedIndex == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        menuViews.exitTitle.setTextColor(
                if (selectedIndex == 4) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Update selection arrow visibility and colors
        // Continue
        if (selectedIndex == 0) {
            menuViews.selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
            menuViews.selectionArrowContinue.visibility = View.VISIBLE
        } else {
            menuViews.selectionArrowContinue.visibility = View.GONE
        }

        // Reset
        if (selectedIndex == 1) {
            menuViews.selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
            menuViews.selectionArrowReset.visibility = View.VISIBLE
        } else {
            menuViews.selectionArrowReset.visibility = View.GONE
        }

        // Progress
        if (selectedIndex == 2) {
            menuViews.selectionArrowProgress.setTextColor(android.graphics.Color.YELLOW)
            menuViews.selectionArrowProgress.visibility = View.VISIBLE
        } else {
            menuViews.selectionArrowProgress.visibility = View.GONE
        }

        // Settings
        if (selectedIndex == 3) {
            menuViews.selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
            menuViews.selectionArrowSettings.visibility = View.VISIBLE
        } else {
            menuViews.selectionArrowSettings.visibility = View.GONE
        }

        // Exit
        if (selectedIndex == 4) {
            menuViews.selectionArrowExit.setTextColor(android.graphics.Color.YELLOW)
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
}
