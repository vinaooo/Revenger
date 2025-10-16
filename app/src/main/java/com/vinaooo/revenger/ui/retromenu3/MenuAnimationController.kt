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

    override fun dismissMenu(onAnimationEnd: (() -> Unit)?) {
        MenuLogger.lifecycle("MenuAnimationController: dismissMenu START")

        animateMenuOut {
            menuViews.menuContainer.visibility = View.GONE
            MenuLogger.lifecycle("MenuAnimationController: dismissMenu COMPLETED")
            onAnimationEnd?.invoke()
        }
    }
}
