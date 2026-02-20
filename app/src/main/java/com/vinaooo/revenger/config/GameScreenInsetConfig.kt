package com.vinaooo.revenger.config

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.RectF
import android.util.Log
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Configuration system for game screen viewport using a simplified preset model.
 *
 * The configuration is now composed of three values:
 *
 *   * `gs_align_horizontal` -- "left" | "center" | "right"
 *   * `gs_align_vertical`   -- "top" | "center" | "bottom"
 *   * `gs_camera_hole_pct`  -- integer 0..99 representing the offset margin
 *       used both to avoid the camera hole and to perform the requested
 *       alignment.
 *
 * The camera margin always applies to the screen edge where the front camera
 * is located (assuming natural orientation with camera at the top). In
 * landscape the edge is inferred from the display rotation. Alignment and
 * camera margins add together if they point to the same side.
 *
 * Legacy numeric formats ("5_25_45_25" etc.) remain supported via
 * [parseInset] purely for backward compatibility; new builds should use the
 * tags listed above.
 */
object GameScreenInsetConfig {
    private const val TAG = "GameScreenInsetConfig"

    data class Inset(val top: Int, val right: Int, val bottom: Int, val left: Int) {
        fun isValid(): Boolean {
            val verticalSum = top + bottom
            val horizontalSum = left + right
            return verticalSum < 100 &&
                    horizontalSum < 100 &&
                    top >= 0 && right >= 0 && bottom >= 0 && left >= 0
        }

        fun clamped(): Inset {
            val clampedTop = top.coerceIn(0, 99)
            val clampedRight = right.coerceIn(0, 99)
            val clampedBottom = bottom.coerceIn(0, 99)
            val clampedLeft = left.coerceIn(0, 99)

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

    enum class AlignH { LEFT, CENTER, RIGHT }
    enum class AlignV { TOP, CENTER, BOTTOM }
    enum class CameraSide { TOP, BOTTOM, LEFT, RIGHT }

    private fun parseAlignH(raw: String): AlignH {
        return when (raw.trim().lowercase()) {
            "left" -> AlignH.LEFT
            "right" -> AlignH.RIGHT
            else -> AlignH.CENTER
        }
    }

    private fun parseAlignV(raw: String): AlignV {
        return when (raw.trim().lowercase()) {
            "top" -> AlignV.TOP
            "bottom" -> AlignV.BOTTOM
            else -> AlignV.CENTER
        }
    }

    private fun detectCameraSide(context: Context): CameraSide {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            val rot = wm?.defaultDisplay?.rotation ?: 0
            when (rot) {
                android.view.Surface.ROTATION_0 -> CameraSide.TOP
                android.view.Surface.ROTATION_90 -> CameraSide.RIGHT
                android.view.Surface.ROTATION_180 -> CameraSide.BOTTOM
                android.view.Surface.ROTATION_270 -> CameraSide.LEFT
                else -> CameraSide.TOP
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect camera side, assuming top", e)
            CameraSide.TOP
        }
    }

    fun calculateInset(
        hAlign: AlignH,
        vAlign: AlignV,
        cameraSide: CameraSide,
        cameraPct: Int,
        alignPct: Int
    ): Inset {
        var top = 0
        var right = 0
        var bottom = 0
        var left = 0

        // apply alignment offset (independent of camera)
        when (vAlign) {
            AlignV.TOP -> top += alignPct
            AlignV.BOTTOM -> bottom += alignPct
            else -> {}
        }
        when (hAlign) {
            AlignH.LEFT -> left += alignPct
            AlignH.RIGHT -> right += alignPct
            else -> {}
        }

        // apply camera margin on the detected side
        when (cameraSide) {
            CameraSide.TOP -> top += cameraPct
            CameraSide.BOTTOM -> bottom += cameraPct
            CameraSide.LEFT -> left += cameraPct
            CameraSide.RIGHT -> right += cameraPct
        }

        val inset = Inset(top, right, bottom, left)
        if (!inset.isValid()) {
            val clamped = inset.clamped()
            Log.w(TAG, "Invalid inset $inset -> clamped $clamped")
            return clamped
        }
        return inset
    }

    fun getConfiguredInset(context: Context, isPortrait: Boolean): Inset {
        val resources = context.resources
        val hRaw = resources.getString(R.string.gs_align_horizontal)
        val vRaw = resources.getString(R.string.gs_align_vertical)
        val cameraPct = try {
            resources.getInteger(R.integer.gs_camera_hole_pct)
        } catch (e: Resources.NotFoundException) {
            0
        }
        val alignPct = try {
            resources.getInteger(R.integer.gs_align_offset_pct)
        } catch (e: Resources.NotFoundException) {
            0
        }

        val hAlign = parseAlignH(hRaw)
        val vAlign = parseAlignV(vRaw)
        val cameraSide = detectCameraSide(context)

        Log.d(
            TAG,
            "hAlign=$hAlign vAlign=$vAlign align=$alignPct% camera=$cameraPct% side=$cameraSide"
        )
        return calculateInset(hAlign, vAlign, cameraSide, cameraPct, alignPct)
    }

    @Deprecated("Legacy numeric format", ReplaceWith("getConfiguredInset(resources, isPortrait)"))
    fun parseInset(insetString: String): Inset {
        try {
            val trimmed = insetString.trim()
            if (trimmed.isEmpty() || trimmed == "0") return Inset(0,0,0,0)
            val parts = trimmed.split("_").map { it.toIntOrNull()?.coerceIn(0,99) ?: 0 }
            val inset = when (parts.size) {
                1 -> Inset(parts[0],parts[0],parts[0],parts[0])
                2 -> Inset(parts[0],parts[1],parts[0],parts[1])
                4 -> Inset(parts[0],parts[1],parts[2],parts[3])
                else -> { Log.w(TAG,"Formato inválido '$insetString'"); Inset(0,0,0,0) }
            }
            if (!inset.isValid()) return inset.clamped()
            return inset
        } catch (e: Exception) {
            Log.e(TAG,"Erro parsing inset '$insetString'",e)
            return Inset(0,0,0,0)
        }
    }

    fun insetToViewport(inset: Inset): RectF {
        val x = inset.left / 100f
        val y = inset.top / 100f
        val width = (100 - inset.left - inset.right) / 100f
        val height = (100 - inset.top - inset.bottom) / 100f
        val viewport = RectF(x,y,width,height)
        Log.d(TAG,"Inset $inset → Viewport(x=$x,y=$y,w=$width,h=$height)")
        return viewport
    }

    fun applyToRetroView(retroView: GLRetroView, isPortrait: Boolean) {
        try {
            val inset = getConfiguredInset(retroView.context, isPortrait)
            val viewportRect = insetToViewport(inset)
            val orientation = if (isPortrait) "Portrait" else "Landscape"
            Log.i(TAG,"✅ Applying $orientation viewport: $viewportRect (inset: $inset)")
            retroView.queueEvent {
                com.swordfish.libretrodroid.LibretroDroid.setViewport(
                    viewportRect.left,
                    viewportRect.top,
                    viewportRect.width(),
                    viewportRect.height()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG,"❌ Failed to process viewport configuration",e)
        }
    }
}
