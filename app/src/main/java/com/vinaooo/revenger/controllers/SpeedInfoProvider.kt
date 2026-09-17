package com.vinaooo.revenger.controllers

import android.content.Context
import com.vinaooo.revenger.R

/**
 * User-facing description/icon/multiplier for the current speed state, used by [SpeedController].
 * Split out purely to keep that class under the project's function-count threshold; exposed back
 * on it via Kotlin interface delegation (`by`) since tests call these methods directly.
 */
interface SpeedInfo {
    /**
     * Gets textual description of the current speed state.
     * @return String with current state ("Fast Forward Active" or "Normal Speed")
     */
    fun getSpeedStateDescription(): String

    /**
     * Gets the appropriate icon ID for the current speed state.
     * @return Resource ID of the icon
     */
    fun getSpeedIconResource(): Int

    /**
     * Gets the configured fast forward speed.
     * @return speed multiplier for fast forward
     */
    fun getFastForwardSpeed(): Int
}

class SpeedInfoProvider(
        private val context: Context,
        private val fastForwardSpeed: Int,
        private val isFastForwardActive: () -> Boolean
) : SpeedInfo {

    override fun getSpeedStateDescription(): String {
        return if (isFastForwardActive()) {
            context.getString(R.string.fast_forward_active)
        } else {
            context.getString(R.string.fast_forward_inactive)
        }
    }

    override fun getSpeedIconResource(): Int {
        // For now uses the same icon, but can be differentiated in the future
        return R.drawable.ic_fast_forward_24
    }

    override fun getFastForwardSpeed(): Int {
        return fastForwardSpeed
    }
}
