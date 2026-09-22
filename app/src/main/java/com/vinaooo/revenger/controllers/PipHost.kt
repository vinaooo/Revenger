package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.managers.GameLifecycleObserver

/**
 * Activity-only platform primitives and Activity-scoped collaborators [PipController] needs but
 * cannot exercise (or safely reference at construction time) from a plain unit test: entering
 * PiP, registering the PiP action receiver, bringing the task back to the foreground, and the
 * conditionally-initialized [GameLifecycleObserver]. [GameActivity] implements this by
 * delegating to its own inherited Activity/Context methods.
 */
interface PipHost {
        /** The Activity itself, for the few call sites that need a raw Activity reference. */
        val activity: FragmentActivity

        fun isCurrentlyInPip(): Boolean

        fun enterPip(params: PictureInPictureParams): Boolean

        fun updatePipParams(params: PictureInPictureParams)

        fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter)

        fun unregisterPipReceiver(receiver: BroadcastReceiver)

        /** Brings the task back to the foreground (used by the PiP action buttons). */
        fun bringTaskToFront()

        fun finishPipTask()

        fun postToUiThread(action: () -> Unit)

        fun restoreFloatingButtonVisibility()

        /** Null before [GameLifecycleObserver] is created (retroView not ready yet). */
        fun gameLifecycleObserverOrNull(): GameLifecycleObserver?
}
