package com.vinaooo.revenger.managers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vinaooo.revenger.retroview.RetroView

class GameLifecycleObserver(
    private val retroView: RetroView
) : DefaultLifecycleObserver {

    private var isLogicallyPausedForPip = false
    private var isPipTransitionPending = false

    fun prepareForPipTransition() {
        isPipTransitionPending = true
    }

    fun onEnteredPictureInPicture() {
        retroView.view.frameSpeed = 0
        isLogicallyPausedForPip = true
        isPipTransitionPending = false
    }

    fun onExitedPictureInPicture() {
        if (isLogicallyPausedForPip) {
            retroView.view.frameSpeed = 1
            isLogicallyPausedForPip = false
        }
    }

    fun clearPendingPipTransition() {
        isPipTransitionPending = false
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val isPip = (owner as? android.app.Activity)?.isInPictureInPictureMode == true
        
        if (isLogicallyPausedForPip && !isPip) {
            onExitedPictureInPicture()
        } else if (!isPip) {
            retroView.resume()
        }

        isPipTransitionPending = false
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        val isPip = (owner as? android.app.Activity)?.isInPictureInPictureMode == true

        if (isPip || isPipTransitionPending) {
            // Durante a transição de PiP, evitar pausa pesada do OpenGL.
            // O congelamento do frame é aplicado quando o PiP é confirmado.
        } else {
            retroView.pause()
        }
    }

    // No onDestroy override: retroView.view is registered directly as its own lifecycle
    // observer (GameActivityViewModel.setupRetroView) and owns core teardown via its own
    // ON_DESTROY-annotated method. Forwarding retroView.destroy() here too would call
    // view.onDestroy() a second time on the same lifecycle event.
}
