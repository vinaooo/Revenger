package com.vinaooo.revenger.ui.menu

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd

/**
 * Menu Animation Controller Sprint 1: Base animation system for Material You Expressive menu
 *
 * Handles all animations and micro-interactions for the menu system Will be enhanced with more
 * sophisticated animations in Sprint 3
 */
class MenuAnimationController {

    companion object {
        // Sprint 1: Basic animation durations
        private const val DURATION_FADE = 200L
        private const val DURATION_SLIDE = 300L
        private const val DURATION_SCALE = 250L
        private const val DURATION_STAGGER = 50L
    }

    /** Animate menu entrance Sprint 1: Basic fade and slide in */
    fun animateMenuEntry(menuContainer: View, cards: List<View>, onComplete: (() -> Unit)? = null) {
        // Sprint 1: Simple implementation
        // Will be enhanced with Material You Expressive animations in Sprint 3

        // Initial state
        menuContainer.alpha = 0f
        menuContainer.translationY = 100f
        cards.forEach { card ->
            card.alpha = 0f
            card.scaleX = 0.8f
            card.scaleY = 0.8f
        }

        // Container animation
        val containerAnimator =
                AnimatorSet().apply {
                    playTogether(
                            ObjectAnimator.ofFloat(menuContainer, "alpha", 0f, 1f),
                            ObjectAnimator.ofFloat(menuContainer, "translationY", 100f, 0f)
                    )
                    duration = DURATION_SLIDE
                    interpolator = DecelerateInterpolator()
                }

        // Staggered card animations
        val cardAnimators = mutableListOf<Animator>()
        cards.forEachIndexed { index, card ->
            val cardAnimator =
                    AnimatorSet().apply {
                        playTogether(
                                ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
                                ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f),
                                ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f)
                        )
                        duration = DURATION_SCALE
                        startDelay = index * DURATION_STAGGER
                        interpolator = OvershootInterpolator(0.3f)
                    }
            cardAnimators.add(cardAnimator)
        }

        // Play animations
        val finalAnimator =
                AnimatorSet().apply {
                    play(containerAnimator)
                            .before(AnimatorSet().apply { playTogether(cardAnimators) })
                    doOnEnd { onComplete?.invoke() }
                }

        finalAnimator.start()
    }

    /** Animate menu exit Sprint 1: Basic fade and slide out */
    fun animateMenuExit(menuContainer: View, cards: List<View>, onComplete: (() -> Unit)? = null) {
        // Sprint 1: Simple reverse animation

        val containerAnimator =
                AnimatorSet().apply {
                    playTogether(
                            ObjectAnimator.ofFloat(menuContainer, "alpha", 1f, 0f),
                            ObjectAnimator.ofFloat(menuContainer, "translationY", 0f, 50f)
                    )
                    duration = DURATION_FADE
                    interpolator = DecelerateInterpolator()
                }

        containerAnimator.doOnEnd { onComplete?.invoke() }
        containerAnimator.start()
    }

    /** Animate card press feedback Sprint 1: Basic scale animation */
    fun animateCardPress(card: View, onComplete: (() -> Unit)? = null) {
        val scaleDown =
                AnimatorSet().apply {
                    playTogether(
                            ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.95f),
                            ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.95f)
                    )
                    duration = 100L
                }

        val scaleUp =
                AnimatorSet().apply {
                    playTogether(
                            ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f),
                            ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f)
                    )
                    duration = 150L
                    interpolator = OvershootInterpolator(0.2f)
                }

        val pressAnimation =
                AnimatorSet().apply {
                    play(scaleDown).before(scaleUp)
                    doOnEnd { onComplete?.invoke() }
                }

        pressAnimation.start()
    }

    /** Cancel all running animations Sprint 1: Basic cleanup */
    fun cancelAllAnimations() {
        // Sprint 1: Simple implementation
        // Will be enhanced with proper animation management in Sprint 3
    }
}
