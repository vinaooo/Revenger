package com.vinaooo.revenger.ui.retromenu3

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker

/**
 * Owns the pulsing "last used slot" glow indicator's animation state (which view is currently
 * animating, and the [ValueAnimator] driving it), extracted from [SaveStateGridFragment] to keep
 * that fragment's function count within detekt's `TooManyFunctions` threshold.
 *
 * [apply] is called once per slot view on every selection-visual refresh; the
 * `lastAnimatedGlowView != glowView` ordering (cancel the previous animator, reset the previous
 * view's alpha, THEN start the new one) is deliberate -- it's what stops a slot that just lost
 * "last used, unselected" status from being left stuck mid-pulse.
 */
class GlowAnimationController {

        private var activeGlowAnimator: ValueAnimator? = null
        private var lastAnimatedGlowView: View? = null

        /**
         * Applies the last-used-slot glow visibility/animation to [glowView]. The glow is
         * visible ONLY when the slot is [isLastUsed] and NOT [isSelected] (selection takes
         * visual precedence).
         */
        fun apply(glowView: View, isLastUsed: Boolean, isSelected: Boolean) {
                if (isLastUsed && !isSelected) {
                        glowView.visibility = View.VISIBLE

                        // Only start animation if this is a different slot than currently
                        // animating -- this prevents restarting the animation on every refresh.
                        if (lastAnimatedGlowView != glowView) {
                                if (lastAnimatedGlowView != null) {
                                        activeGlowAnimator?.cancel()
                                        activeGlowAnimator = null
                                        lastAnimatedGlowView?.alpha = 1.0f
                                }
                                startAnimation(glowView)
                                lastAnimatedGlowView = glowView
                        }
                } else {
                        glowView.visibility = View.GONE

                        // Only stop animation if this was the animated slot.
                        if (lastAnimatedGlowView == glowView) {
                                activeGlowAnimator?.cancel()
                                activeGlowAnimator = null
                                lastAnimatedGlowView = null
                        }
                        glowView.alpha = 1.0f
                }
        }

        private fun startAnimation(glowView: View) {
                activeGlowAnimator?.cancel()

                activeGlowAnimator =
                        ObjectAnimator.ofFloat(
                                        glowView,
                                        "alpha",
                                        GLOW_DIM_ALPHA,
                                        GLOW_BRIGHT_ALPHA,
                                        GLOW_DIM_ALPHA
                                )
                                .apply {
                                        duration = GLOW_ANIMATION_DURATION_MS
                                        interpolator = AccelerateDecelerateInterpolator()
                                        repeatCount = ValueAnimator.INFINITE
                                        repeatMode = ValueAnimator.RESTART
                                        start()
                                }
        }

        /** Stops the active glow animation, if any. Safe to call when nothing is animating. */
        fun stop() {
                if (activeGlowAnimator != null) {
                        activeGlowAnimator?.cancel()
                        activeGlowAnimator = null
                }
        }

        /**
         * Invokes [onValid] only when [SessionSlotTracker] reports a last-used slot that falls
         * within the valid slot range -- the caller is expected to pass a refresh callback that
         * re-renders the grid (which is what actually triggers [apply] to show the glow).
         */
        fun refreshIfLastUsedSlotValid(onValid: () -> Unit) {
                val lastSlot = SessionSlotTracker.getInstance().getLastUsedSlot() ?: return
                if (lastSlot !in 1..SaveStateManager.TOTAL_SLOTS) {
                        return
                }
                onValid()
        }

        companion object {
                private const val GLOW_DIM_ALPHA = 0.3f
                private const val GLOW_BRIGHT_ALPHA = 1.0f
                private const val GLOW_ANIMATION_DURATION_MS = 1500L
        }
}
