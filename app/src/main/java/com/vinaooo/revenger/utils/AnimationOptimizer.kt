package com.vinaooo.revenger.utils

import android.animation.Animator
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.util.Pools

/**
 * Animation optimizer for the RetroMenu3 menu system. Uses ViewPropertyAnimator for better
 * performance and object pools to reduce allocations.
 */
object AnimationOptimizer {

    // Max number of pooled listener instances kept for reuse; sized to comfortably cover the
    // menu's simultaneous animations without unbounded growth.
    private const val ANIMATOR_LISTENER_POOL_MAX_SIZE = 8

    // Pool of ViewPropertyAnimator listeners to reduce allocations
    private val animatorListenerPool =
            Pools.SimplePool<AnimationEndListener>(ANIMATOR_LISTENER_POOL_MAX_SIZE)

    /** Optimized animation using ViewPropertyAnimator with listener pool */
    fun animateViewOptimized(
            view: View,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
    ): ViewPropertyAnimator {
        // Usar hardware layer para melhor performance
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        return view.animate()
                .alpha(toAlpha)
                .scaleX(toScale)
                .scaleY(toScale)
                .setDuration(duration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setListener(getAnimationEndListener(view, onEnd))
    }

    /** Optimized batch animation using ViewPropertyAnimator */
    fun animateViewsBatchOptimized(
            views: Array<View>,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
    ) {
        logBatchAnimationStart(views, toAlpha, toScale, duration)

        val animationsCompleted = mutableListOf<Boolean>()
        var completedCount = 0

        views.forEachIndexed { index, view ->
            android.util.Log.d(
                    "AnimationOptimizer",
                    "🎬 [BATCH_ANIM] Starting animation for view[$index]"
            )

            // Usar hardware layer para melhor performance
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            view.animate()
                    .alpha(toAlpha)
                    .scaleX(toScale)
                    .scaleY(toScale)
                    .setDuration(duration)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .setListener(
                            object : android.animation.Animator.AnimatorListener {
                                override fun onAnimationStart(
                                        animation: android.animation.Animator
                                ) {
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM] View[$index] animation STARTED"
                                    )
                                }

                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    logViewFinalState(index, view)

                                    // Restaurar layer type
                                    view.setLayerType(View.LAYER_TYPE_NONE, null)

                                    synchronized(animationsCompleted) {
                                        completedCount++
                                        android.util.Log.d(
                                                "AnimationOptimizer",
                                                "🎬 [BATCH_ANIM] Completed count: $completedCount/${views.size}"
                                        )

                                        if (completedCount >= views.size) {
                                            android.util.Log.d(
                                                    "AnimationOptimizer",
                                                    "🎬 [BATCH_ANIM] ALL ANIMATIONS COMPLETED - calling onEnd"
                                            )
                                            onEnd?.invoke()
                                        }
                                    }
                                }

                                override fun onAnimationCancel(
                                        animation: android.animation.Animator
                                ) {
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM] View[$index] animation CANCELLED"
                                    )
                                    // Restaurar layer type mesmo se cancelado
                                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                                }

                                // Animator.AnimatorListener requires overriding every callback;
                                // only start/end/cancel matter for this batch animation.
                                @Suppress("EmptyFunctionBlock")
                                override fun onAnimationRepeat(
                                        animation: android.animation.Animator
                                ) {}
                            }
                    )
                    .start()

            android.util.Log.d(
                    "AnimationOptimizer",
                    "🎬 [BATCH_ANIM] View[$index] animate().start() called"
            )
        }

        logBatchAnimationInitiated()
    }

    /** Logs the batch animation header plus each view's pre-animation state. */
    private fun logBatchAnimationStart(
            views: Array<View>,
            toAlpha: Float,
            toScale: Float,
            duration: Long
    ) {
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] ===== STARTING BATCH ANIMATION ====="
        )
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] Timestamp: ${System.currentTimeMillis()}"
        )
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM] Views count: ${views.size}")
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] Target alpha: $toAlpha, scale: $toScale"
        )
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM] Duration: ${duration}ms")

        views.forEachIndexed { index, view -> logViewInitialState(index, view) }

        // Use ViewPropertyAnimator directly - more reliable than ObjectAnimator
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] Using ViewPropertyAnimator approach"
        )
    }

    /** Logs a single view's alpha/scale/visibility state before its animation starts. */
    private fun logViewInitialState(index: Int, view: View) {
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM] View[$index] initial state:")
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM]   alpha: ${view.alpha}")
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM]   scaleX: ${view.scaleX}, scaleY: ${view.scaleY}"
        )
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM]   visibility: ${view.visibility}"
        )
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM]   isShown: ${view.isShown}")
    }

    /** Logs a single view's alpha/scale state once its animation ends. */
    private fun logViewFinalState(index: Int, view: View) {
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM] View[$index] animation ENDED")
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM] View[$index] final state:")
        android.util.Log.d("AnimationOptimizer", "🎬 [BATCH_ANIM]   alpha: ${view.alpha}")
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM]   scaleX: ${view.scaleX}, scaleY: ${view.scaleY}"
        )
    }

    /** Logs the batch-animation-initiated footer. */
    private fun logBatchAnimationInitiated() {
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] All ViewPropertyAnimator animations initiated"
        )
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] ===== BATCH ANIMATION INITIATED ====="
        )
    }

    /** Obtains a listener from the pool or creates a new one */
    private fun getAnimationEndListener(
            view: View,
            onEnd: (() -> Unit)?
    ): Animator.AnimatorListener {
        return animatorListenerPool.acquire()?.apply {
            this.view = view
            this.onEnd = onEnd
        }
                ?: AnimationEndListener(view, onEnd)
    }

    /** Reusable listener for ViewPropertyAnimator animations */
    private class AnimationEndListener(var view: View? = null, var onEnd: (() -> Unit)? = null) :
            Animator.AnimatorListener {

        // Animator.AnimatorListener requires overriding every callback; this reusable
        // listener only cares about end/cancel to restore layer type and return to the pool.
        @Suppress("EmptyFunctionBlock")
        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {
            // Restaurar layer type
            view?.setLayerType(View.LAYER_TYPE_NONE, null)
            onEnd?.invoke()
            // Limpar referências e devolver ao pool
            view = null
            onEnd = null
            animatorListenerPool.release(this)
        }

        override fun onAnimationCancel(animation: Animator) {
            // Restaurar layer type mesmo se cancelado
            view?.setLayerType(View.LAYER_TYPE_NONE, null)
            // Limpar referências e devolver ao pool
            view = null
            onEnd = null
            animatorListenerPool.release(this)
        }

        @Suppress("EmptyFunctionBlock")
        override fun onAnimationRepeat(animation: Animator) {}
    }

    /** Clears the pools when needed (call from Activity onDestroy) */
    fun clearPools() {
        // Not strictly necessary, but helps with memory cleanup
        MenuLogger.performance("Animation pools cleared")
    }
}
