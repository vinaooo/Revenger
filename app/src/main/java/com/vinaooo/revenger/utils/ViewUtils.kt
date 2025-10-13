package com.vinaooo.revenger.utils

import android.view.View
import android.view.ViewGroup

/**
 * Utility class for common view operations used across menu fragments. Eliminates code duplication
 * and provides centralized view management.
 */
object ViewUtils {

    /**
     * Recursively sets z=0, elevation=0, and translationZ=0 on all views to ensure menu stays below
     * gamepad layer. This is necessary because Material Design components have default elevation
     * that overrides XML attributes.
     *
     * @param view The root view to process recursively
     */
    fun forceZeroElevationRecursively(view: View) {
        view.z = 0f
        view.elevation = 0f
        view.translationZ = 0f

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                forceZeroElevationRecursively(view.getChildAt(i))
            }
        }
    }

    /**
     * Applies arcade font to multiple text views at once. Convenience method to reduce code
     * duplication.
     *
     * @param context The context to use for font loading
     * @param views Variable number of TextViews to apply font to
     */
    fun applyArcadeFontToViews(
            context: android.content.Context,
            vararg views: android.widget.TextView
    ) {
        views.forEach { view -> FontUtils.applyArcadeFont(context, view) }
    }

    /**
     * Batch update for view visibility to reduce layout passes. Sets multiple views to the same
     * visibility state efficiently.
     *
     * @param visibility The visibility state (VISIBLE, INVISIBLE, GONE)
     * @param views Variable number of views to update
     */
    fun setVisibilityBatch(visibility: Int, vararg views: View) {
        views.forEach { it.visibility = visibility }
    }

    /**
     * Safely performs layout operations by batching them to reduce unnecessary layout passes.
     *
     * @param action The action to perform that may trigger layout
     */
    fun performLayoutBatch(action: () -> Unit) {
        action()
    }

    /**
     * Optimized menu animation that uses hardware acceleration and reduces overdraw. Animates alpha
     * and scale simultaneously for better performance.
     *
     * @param view The view to animate
     * @param toAlpha Target alpha value (0f to 1f)
     * @param toScale Target scale value (typically 0.8f to 1f)
     * @param duration Animation duration in milliseconds
     * @param onEnd Optional callback when animation ends
     */
    fun animateMenuView(
            view: View,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
    ) {
        // Use hardware layer for better performance
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        view.animate()
                .alpha(toAlpha)
                .scaleX(toScale)
                .scaleY(toScale)
                .setDuration(duration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setListener(
                        object : android.animation.Animator.AnimatorListener {
                            override fun onAnimationStart(animation: android.animation.Animator) {}
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                // Restore layer type to prevent memory leaks
                                view.setLayerType(View.LAYER_TYPE_NONE, null)
                                onEnd?.invoke()
                            }
                            override fun onAnimationCancel(animation: android.animation.Animator) {
                                // Restore layer type even if cancelled
                                view.setLayerType(View.LAYER_TYPE_NONE, null)
                            }
                            override fun onAnimationRepeat(animation: android.animation.Animator) {}
                        }
                )
                .start()
    }

    /**
     * Batch animation for multiple views to reduce animation overhead. Useful for animating menu
     * containers and hints together.
     *
     * @param views Array of views to animate
     * @param toAlpha Target alpha value
     * @param toScale Target scale value
     * @param duration Animation duration
     * @param onEnd Callback when all animations complete
     */
    fun animateMenuViewsBatch(
            views: Array<View>,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
    ) {
        val animatorSet = android.animation.AnimatorSet()
        val animators = mutableListOf<android.animation.Animator>()

        views.forEach { view ->
            // Use hardware layer for each view
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Create ObjectAnimator for better control
            val alphaAnimator = android.animation.ObjectAnimator.ofFloat(view, "alpha", toAlpha)
            val scaleXAnimator = android.animation.ObjectAnimator.ofFloat(view, "scaleX", toScale)
            val scaleYAnimator = android.animation.ObjectAnimator.ofFloat(view, "scaleY", toScale)

            // Set common properties
            alphaAnimator.duration = duration
            scaleXAnimator.duration = duration
            scaleYAnimator.duration = duration
            alphaAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            scaleXAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            scaleYAnimator.interpolator = android.view.animation.DecelerateInterpolator()

            animators.add(alphaAnimator)
            animators.add(scaleXAnimator)
            animators.add(scaleYAnimator)
        }

        animatorSet.playTogether(animators)
        animatorSet.addListener(
                object : android.animation.Animator.AnimatorListener {
                    override fun onAnimationStart(animation: android.animation.Animator) {}
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Restore layer types
                        views.forEach { it.setLayerType(View.LAYER_TYPE_NONE, null) }
                        onEnd?.invoke()
                    }
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        // Restore layer types even if cancelled
                        views.forEach { it.setLayerType(View.LAYER_TYPE_NONE, null) }
                    }
                    override fun onAnimationRepeat(animation: android.animation.Animator) {}
                }
        )
        animatorSet.start()
    }
}
