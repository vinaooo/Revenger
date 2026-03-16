package com.vinaooo.revenger.managers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vinaooo.revenger.retroview.RetroView

class GameLifecycleObserver(
    private val retroView: RetroView
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        retroView.resume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        retroView.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        retroView.destroy()
    }
}
