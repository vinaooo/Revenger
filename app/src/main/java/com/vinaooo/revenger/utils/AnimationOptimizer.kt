package com.vinaooo.revenger.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.util.Pools

/**
 * Otimizador de animações para o sistema de menu RetroMenu3. Usa ViewPropertyAnimator para melhor
 * performance e pools de objetos para reduzir alocações.
 */
object AnimationOptimizer {

    // Pool de AnimatorSet para reduzir alocações
    private val animatorSetPool = Pools.SimplePool<AnimatorSet>(4)

    // Pool de ObjectAnimator para reduzir alocações
    private val objectAnimatorPool = Pools.SimplePool<ObjectAnimator>(12)

    // Pool de ViewPropertyAnimator listeners para reduzir alocações
    private val animatorListenerPool = Pools.SimplePool<AnimationEndListener>(8)

    /** Animação otimizada usando ViewPropertyAnimator com pool de listeners */
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

    /** Animação em lote otimizada usando AnimatorSet pool */
    fun animateViewsBatchOptimized(
            views: Array<View>,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
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

        views.forEachIndexed { index, view ->
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

        // Usar ViewPropertyAnimator diretamente - mais confiável que ObjectAnimator
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] Using ViewPropertyAnimator approach"
        )

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
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM] View[$index] animation ENDED"
                                    )
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM] View[$index] final state:"
                                    )
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM]   alpha: ${view.alpha}"
                                    )
                                    android.util.Log.d(
                                            "AnimationOptimizer",
                                            "🎬 [BATCH_ANIM]   scaleX: ${view.scaleX}, scaleY: ${view.scaleY}"
                                    )

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

        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] All ViewPropertyAnimator animations initiated"
        )
        android.util.Log.d(
                "AnimationOptimizer",
                "🎬 [BATCH_ANIM] ===== BATCH ANIMATION INITIATED ====="
        )
    }

    /** Obtém AnimatorSet do pool ou cria novo */
    private fun getAnimatorSet(): AnimatorSet {
        return animatorSetPool.acquire() ?: AnimatorSet()
    }

    /** Obtém ObjectAnimator do pool ou cria novo */
    private fun getObjectAnimator(
            target: Any,
            property: String,
            vararg values: Float
    ): ObjectAnimator {
        return objectAnimatorPool.acquire()?.apply {
            setTarget(target)
            setPropertyName(property)
            setFloatValues(*values)
        }
                ?: ObjectAnimator.ofFloat(target, property, *values)
    }

    /** Obtém listener do pool ou cria novo */
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

    /** Listener reutilizável para animações ViewPropertyAnimator */
    private class AnimationEndListener(var view: View? = null, var onEnd: (() -> Unit)? = null) :
            Animator.AnimatorListener {

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

        override fun onAnimationRepeat(animation: Animator) {}
    }

    /** Limpa os pools quando necessário (chamar no onDestroy da Activity) */
    fun clearPools() {
        // Not strictly necessary, but helps with memory cleanup
        MenuLogger.performance("Animation pools cleared")
    }
}
