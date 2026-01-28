package com.vinaooo.revenger.config

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.RectF
import android.util.Log
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Configuration system for game screen viewport using CSS-like inset margins.
 *
 * Allows defining the game display area through percentual margins from each screen edge. The game
 * is automatically centered within the defined area while maintaining its native aspect ratio.
 *
 * Supported formats:
 * - "V" : V% margin on all sides
 * - "V_H" : V% vertical (top/bottom), H% horizontal (left/right)
 * - "T_R_B_L" : top_right_bottom_left (CSS style)
 *
 * Example: "5_25_45_25" = 5% top, 25% right, 45% bottom, 25% left Result: Game area at x=0.25,
 * y=0.05, width=0.50, height=0.50
 */
object GameScreenInsetConfig {
    private const val TAG = "GameScreenInsetConfig"

    /**
     * Represents inset margins in percentual values (0-99)
     *
     * @property top Top margin in percentage (0-99)
     * @property right Right margin in percentage (0-99)
     * @property bottom Bottom margin in percentage (0-99)
     * @property left Left margin in percentage (0-99)
     */
    data class Inset(val top: Int, val right: Int, val bottom: Int, val left: Int) {
        /** Validates that inset values don't exceed screen bounds */
        fun isValid(): Boolean {
            val verticalSum = top + bottom
            val horizontalSum = left + right
            return verticalSum < 100 &&
                    horizontalSum < 100 &&
                    top >= 0 &&
                    right >= 0 &&
                    bottom >= 0 &&
                    left >= 0
        }

        /** Clamps invalid values to valid range */
        fun clamped(): Inset {
            // Clamp individual values to 0-99
            val clampedTop = top.coerceIn(0, 99)
            val clampedRight = right.coerceIn(0, 99)
            val clampedBottom = bottom.coerceIn(0, 99)
            val clampedLeft = left.coerceIn(0, 99)

            // Check if sum exceeds 100
            var finalTop = clampedTop
            var finalBottom = clampedBottom
            var finalLeft = clampedLeft
            var finalRight = clampedRight

            if (finalTop + finalBottom >= 100) {
                val ratio = 99f / (finalTop + finalBottom)
                finalTop = (finalTop * ratio).toInt()
                finalBottom = (finalBottom * ratio).toInt()
            }

            if (finalLeft + finalRight >= 100) {
                val ratio = 99f / (finalLeft + finalRight)
                finalLeft = (finalLeft * ratio).toInt()
                finalRight = (finalRight * ratio).toInt()
            }

            return Inset(finalTop, finalRight, finalBottom, finalLeft)
        }
    }

    /**
     * Parses inset string from XML configuration.
     *
     * Supports three formats:
     * 1. Single value: "10" → 10% on all sides
     * 2. Two values: "10_20" → 10% vertical, 20% horizontal
     * 3. Four values: "10_20_30_40" → top, right, bottom, left
     *
     * @param insetString String in format "V", "V_H", or "T_R_B_L"
     * @return Parsed Inset object
     */
    fun parseInset(insetString: String): Inset {
        try {
            val trimmed = insetString.trim()

            if (trimmed.isEmpty() || trimmed == "0") {
                return Inset(0, 0, 0, 0)
            }

            val parts = trimmed.split("_").map { it.toIntOrNull()?.coerceIn(0, 99) ?: 0 }

            val inset =
                    when (parts.size) {
                        1 -> {
                            // Format: "V" - same margin on all sides
                            val value = parts[0]
                            Inset(value, value, value, value)
                        }
                        2 -> {
                            // Format: "V_H" - vertical and horizontal
                            val vertical = parts[0]
                            val horizontal = parts[1]
                            Inset(vertical, horizontal, vertical, horizontal)
                        }
                        4 -> {
                            // Format: "T_R_B_L" - CSS style (top, right, bottom, left)
                            Inset(parts[0], parts[1], parts[2], parts[3])
                        }
                        else -> {
                            Log.w(TAG, "Invalid inset format: '$insetString'. Using default (0).")
                            Inset(0, 0, 0, 0)
                        }
                    }

            // Validate and clamp if needed
            if (!inset.isValid()) {
                val clamped = inset.clamped()
                Log.w(TAG, "Invalid inset values: $inset. Clamped to: $clamped")
                return clamped
            }

            Log.d(TAG, "Parsed inset '$insetString' → $inset")
            return inset
        } catch (e: Exception) {
            Log.e(
                    TAG,
                    "Error parsing inset string '$insetString': ${e.message}. Using default (0).",
                    e
            )
            return Inset(0, 0, 0, 0)
        }
    }

    /**
     * Converts inset margins to normalized viewport coordinates.
     *
     * The viewport defines the area where the game will be rendered. LibretroDroid automatically
     * centers the game within this area while maintaining aspect ratio.
     *
     * @param inset Inset margins in percentage
     * @return RectF with normalized coordinates (0.0 - 1.0)
     */
    fun insetToViewport(inset: Inset): RectF {
        val x = inset.left / 100f
        val y = inset.top / 100f
        val width = (100 - inset.left - inset.right) / 100f
        val height = (100 - inset.top - inset.bottom) / 100f

        val viewport = RectF(x, y, width, height)

        Log.d(TAG, "Inset $inset → Viewport(x=$x, y=$y, w=$width, h=$height)")

        return viewport
    }

    /**
     * Gets the configured inset based on current orientation.
     *
     * @param resources Android resources to read configuration
     * @param isPortrait True if portrait orientation, false if landscape
     * @return Configured Inset object
     */
    fun getConfiguredInset(resources: Resources, isPortrait: Boolean): Inset {
        val insetString =
                if (isPortrait) {
                    resources.getString(R.string.gs_inset_portrait)
                } else {
                    resources.getString(R.string.gs_inset_landscape)
                }

        val orientation = if (isPortrait) "Portrait" else "Landscape"
        Log.d(TAG, "Reading $orientation inset configuration: '$insetString'")

        return parseInset(insetString)
    }

    /**
     * Detects current orientation from resources.
     *
     * @param resources Android resources
     * @return True if portrait, false if landscape
     */
    fun isPortraitOrientation(resources: Resources): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    /**
     * Applies configured viewport to GLRetroView.
     *
     * Reads inset configuration from XML (gs_inset_portrait/gs_inset_landscape), parses the inset
     * values, converts to normalized viewport coordinates, and applies to the RetroView via
     * LibretroDroid's setViewport() API.
     *
     * The viewport defines the rendering area. LibretroDroid automatically centers the game within
     * this area while maintaining its native aspect ratio.
     *
     * @param retroView GLRetroView instance to configure
     * @param resources Android resources to read configuration
     * @param isPortrait True if portrait orientation, false if landscape
     */
    fun applyToRetroView(retroView: GLRetroView, resources: Resources, isPortrait: Boolean) {
        try {
            val inset = getConfiguredInset(resources, isPortrait)
            val viewportRect = insetToViewport(inset)

            val orientation = if (isPortrait) "Portrait" else "Landscape"
            Log.i(TAG, "✅ Applying $orientation viewport: $viewportRect (inset: $inset)")

            // Apply viewport via LibretroDroid 0.13.1+ API
            retroView.queueEvent {
                com.swordfish.libretrodroid.LibretroDroid.setViewport(
                        viewportRect.left,
                        viewportRect.top,
                        viewportRect.width(),
                        viewportRect.height()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process viewport configuration", e)
        }
    }
}
