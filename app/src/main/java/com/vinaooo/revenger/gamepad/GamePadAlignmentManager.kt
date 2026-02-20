package com.vinaooo.revenger.gamepad

import android.content.res.Resources
import android.util.Log
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.vinaooo.revenger.R

/**
 * Centralized manager for virtual GamePad alignment and positioning.
 *
 * Responsibilities:
 * 1. Calculate Empty Dials required to maintain symmetry between LEFT and RIGHT
 * 2. Calculate margin based on vertical offset (0-100%) configured in XML
 * 3. Provide methods for dynamic application at runtime
 *
 * Goal: Ensure that the centers of LEFT and RIGHT GamePads are always horizontally
 * aligned, even when they have different secondary button configurations.
 */
class GamePadAlignmentManager(private val resources: Resources) {

    companion object {
        private const val TAG = "GamePadAlignmentManager"

        // Default indices that need to be mirrored
        private const val MENU_INDEX = 8 // Index of MENU button (RIGHT)
    }

    /**
     * Calculates bottom margin in pixels based on vertical offset (0-100%) and screen height.
     *
     * @param screenHeight Total screen height in pixels (excluding status bar, nav bar, etc)
     * @param offsetPercent Vertical offset as percentage (0-100). 100 = bottom edge, 50 =
     * centered
     * @return Bottom margin in pixels. Negative values move the GamePad up.
     *
     * Formula: screenHeight * (100 - offsetPercent) / 100 Example: screenHeight=1000, offset=100 →
     * margin = 0px (at edge) Example: screenHeight=1000, offset=50 → margin = 500px (centered)
     */
    fun calculateBottomMarginPortrait(screenHeight: Int, offsetPercent: Int): Int {
        val clampedOffset = offsetPercent.coerceIn(0, 100)
        val calculatedMargin = (screenHeight * (100 - clampedOffset) / 100.0).toInt()

        Log.d(
                TAG,
                "calculateBottomMarginPortrait: screenHeight=$screenHeight, " +
                        "offset=$clampedOffset%, margin=$calculatedMargin px"
        )

        return calculatedMargin
    }

    /**
     * Calculates top margin in pixels for landscape. In landscape, the GamePad is positioned at
     * the top, so we use marginTop.
     *
     * @param screenHeight Screen height in pixels
     * @param offsetPercent Vertical offset as percentage (0-100). 100 = bottom edge, 0 = top
     * @return Top margin in pixels. Positive = moves downward
     */
    fun calculateTopMarginLandscape(screenHeight: Int, offsetPercent: Int): Int {
        val clampedOffset = offsetPercent.coerceIn(0, 100)

        // Em landscape, queremos inverter: offset 100 = abaixo (long margin), offset 0 = topo (0
        // margin)
        val calculatedMargin = (screenHeight * clampedOffset / 100.0).toInt()

        Log.d(
                TAG,
                "calculateTopMarginLandscape: screenHeight=$screenHeight, " +
                        "offset=$clampedOffset%, margin=$calculatedMargin px"
        )

        return calculatedMargin
    }

    /**
     * Returns the indices of Empty Dials required for the LEFT side to mirror the RIGHT
     * side configuration.
     *
     * Note: This version is simplified because the Empty Dial has already been added directly
     * in GamePadConfig.kt for index 8 (MENU). This method exists for future documentation and
     * extensibility.
     *
     * @return List containing index 8 (MENU)
     */
    fun getEmptyDialIndicesForLeft(): List<Int> {
        return listOf(MENU_INDEX)
    }

    /**
     * Returns information about current alignment for logging/debug.
     *
     * @param leftConfig LEFT GamePad configuration
     * @param rightConfig RIGHT GamePad configuration
     * @return String summarizing both configurations
     */
    fun getAlignmentDebugInfo(
            leftConfig: RadialGamePadConfig?,
            rightConfig: RadialGamePadConfig?
    ): String {
        return buildString {
            append("LEFT: ")
            if (leftConfig != null) {
                append("sockets=${leftConfig.sockets}, ")
                append("secondaryDials=${leftConfig.secondaryDials.size}")
            } else {
                append("null")
            }
            append(" | RIGHT: ")
            if (rightConfig != null) {
                append("sockets=${rightConfig.sockets}, ")
                append("secondaryDials=${rightConfig.secondaryDials.size}")
            } else {
                append("null")
            }
        }
    }

    /**
     * Validates that XML offsets are within the correct range.
     *
     * @return Pair<Boolean, String> with validation result and error message (if any)
     */
    fun validateOffsets(): Pair<Boolean, String> {
        return try {
            val portraitOffset = resources.getInteger(R.integer.gp_offset_portrait)
            val landscapeOffset = resources.getInteger(R.integer.gp_offset_landscape)

            val portraitValid = portraitOffset in 0..100
            val landscapeValid = landscapeOffset in 0..100

            return when {
                !portraitValid ->
                        Pair(
                                false,
                                "gp_offset_portrait deve estar entre 0-100, encontrado: $portraitOffset"
                        )
                !landscapeValid ->
                        Pair(
                                false,
                                "gp_offset_landscape deve estar entre 0-100, encontrado: $landscapeOffset"
                        )
                else -> {
                    Log.d(
                            TAG,
                            "Offsets valid - Portrait: $portraitOffset%, Landscape: $landscapeOffset%"
                    )
                    Pair(true, "")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Erro ao validar offsets: ${e.message}")
        }
    }
}
