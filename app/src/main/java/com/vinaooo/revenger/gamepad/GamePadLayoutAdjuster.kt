package com.vinaooo.revenger.gamepad

import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication

/**
 * Adjusts the on-screen virtual gamepad's position, size, and left/right symmetry in response to
 * orientation changes.
 *
 * Extracted from `GameActivity` (Task 9 of the split-god-classes refactor): all logic here works
 * purely on the passed-in view hierarchy plus the global [RevengerApplication.appConfig] facade,
 * so it needs zero `Activity` reference.
 */
class GamePadLayoutAdjuster {

    companion object {
        private const val TAG = "GamePadLayoutAdjuster"
    }

    /** Adjust gamepad position based on screen orientation */
    fun adjustPositionForOrientation(gamepadContainer: LinearLayout) {
        val layoutParams = gamepadContainer.layoutParams as FrameLayout.LayoutParams

        // Reset margins and minimumHeight from previous orientation
        layoutParams.topMargin = 0
        layoutParams.bottomMargin = 0

        // Reset minimumHeight of child containers
        val leftContainer =
                gamepadContainer.findViewById<android.widget.FrameLayout>(
                        R.id.left_container
                )
        val rightContainer =
                gamepadContainer.findViewById<android.widget.FrameLayout>(
                        R.id.right_container
                )
        leftContainer?.minimumHeight = 0
        rightContainer?.minimumHeight = 0

        Log.d(TAG, "Reset margins and minimumHeight for orientation change")

        // Check current orientation
        val isPortrait =
                gamepadContainer.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT

        Log.d(TAG, "Current orientation: ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}")
        Log.d(TAG, "Current layout gravity before: ${layoutParams.gravity}")

        if (isPortrait) {
            // Force bottom positioning in portrait
            layoutParams.gravity = android.view.Gravity.BOTTOM
            Log.d(TAG, "GamePad positioned at BOTTOM for portrait mode")

            // Increase gamepad sizes for portrait (40% each instead of 25%)
            adjustGamePadSizes(gamepadContainer, 0.40f, 0.2f)

            // Equalize heights and apply portrait offset
            gamepadContainer.post {
                equalizeGamePadHeights(gamepadContainer)
                applyPortraitOffset(gamepadContainer)
            }
        } else {
            // Keep top positioning in landscape
            layoutParams.gravity = android.view.Gravity.TOP
            Log.d(TAG, "GamePad positioned at TOP for landscape mode")

            // Keep original sizes for landscape (25% each)
            adjustGamePadSizes(gamepadContainer, 0.25f, 0.5f)

            // Equalize heights in landscape to fix alignment issues
            gamepadContainer.post {
                equalizeGamePadHeights(gamepadContainer)
                applyLandscapeOffset(gamepadContainer)
            }
        }

        Log.d(TAG, "Final layout gravity: ${layoutParams.gravity}")
        gamepadContainer.layoutParams = layoutParams
        gamepadContainer.requestLayout()
    }

    /**
     * Apply portrait offset via bottomMargin based on XML configuration. 100% = base at bottom
     * edge, lower % = higher position
     */
    private fun applyPortraitOffset(container: LinearLayout) {
        try {
            val offsetPercent = RevengerApplication.appConfig.gamePadConfigModel.gp_offset_portrait

            // Use parent height (FrameLayout) minus container height to
            // calculate available space
            val parent = container.parent as? android.view.View
            val availableHeight = parent?.height ?: 0
            val containerHeight = container.height

            if (availableHeight <= 0 || containerHeight <= 0) {
                Log.w(
                        TAG,
                        "Portrait: Heights not available yet (available=$availableHeight, container=$containerHeight)"
                )
                return
            }

            // Maximum space to move the gamepad (available height - container height)
            val maxMovement = availableHeight - containerHeight

            // Calculate margin: offset 100% = 0px (bottom edge), offset 0% =
            // maxMovement (top)
            val bottomMargin = (maxMovement * (100 - offsetPercent) / 100.0).toInt()

            val layoutParams = container.layoutParams as FrameLayout.LayoutParams
            layoutParams.bottomMargin = bottomMargin
            container.layoutParams = layoutParams

            Log.d(
                    TAG,
                    "Portrait offset: $offsetPercent% → availableHeight=$availableHeight, containerHeight=$containerHeight, maxMovement=$maxMovement, bottomMargin=$bottomMargin px"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying portrait offset", e)
        }
    }

    /**
     * Apply landscape offset via topMargin based on XML configuration. 100% = base at bottom,
     * 0% = base at top
     */
    private fun applyLandscapeOffset(container: LinearLayout) {
        try {
            val offsetPercent = RevengerApplication.appConfig.gamePadConfigModel.gp_offset_landscape

            // Use parent height (FrameLayout) minus container height to
            // calculate available space
            val parent = container.parent as? android.view.View
            val availableHeight = parent?.height ?: 0
            val containerHeight = container.height

            if (availableHeight <= 0 || containerHeight <= 0) {
                Log.w(
                        TAG,
                        "Landscape: Heights not available yet (available=$availableHeight, container=$containerHeight)"
                )
                return
            }

            // Maximum space to move the gamepad
            val maxMovement = availableHeight - containerHeight

            // Calculate margin: offset 0% = 0px (top), offset 100% = maxMovement
            // (bottom)
            val topMargin = (maxMovement * offsetPercent / 100.0).toInt()

            val layoutParams = container.layoutParams as FrameLayout.LayoutParams
            layoutParams.topMargin = topMargin
            container.layoutParams = layoutParams

            Log.d(
                    TAG,
                    "Landscape offset: $offsetPercent% → availableHeight=$availableHeight, containerHeight=$containerHeight, maxMovement=$maxMovement, topMargin=$topMargin px"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying landscape offset", e)
        }
    }

    /**
     * Equalizes the height of left and right gamepad containers in landscape mode. This ensures
     * that centers remain aligned even when one side has different secondary dial
     * configurations (e.g., MENU button on right only).
     */
    private fun equalizeGamePadHeights(container: LinearLayout) {
        val leftContainer =
                container.findViewById<android.widget.FrameLayout>(R.id.left_container)
        val rightContainer =
                container.findViewById<android.widget.FrameLayout>(R.id.right_container)

        if (leftContainer != null && rightContainer != null) {
            val leftHeight = leftContainer.height
            val rightHeight = rightContainer.height
            val maxHeight = maxOf(leftHeight, rightHeight)

            // Debug: measure actual positions and sizes
            val leftPos = IntArray(2)
            val rightPos = IntArray(2)
            leftContainer.getLocationOnScreen(leftPos)
            rightContainer.getLocationOnScreen(rightPos)
            val screenWidth = container.resources.displayMetrics.widthPixels
            Log.d(TAG, "=== GAMEPAD ALIGNMENT DEBUG ===")
            Log.d(TAG, "Screen width: $screenWidth")
            Log.d(
                    TAG,
                    "LEFT container: x=${leftPos[0]}, width=${leftContainer.width}, height=$leftHeight"
            )
            Log.d(
                    TAG,
                    "RIGHT container: x=${rightPos[0]}, width=${rightContainer.width}, height=$rightHeight"
            )
            Log.d(TAG, "LEFT margin from left edge: ${leftPos[0]}px")
            Log.d(
                    TAG,
                    "RIGHT margin from right edge: ${screenWidth - rightPos[0] - rightContainer.width}px"
            )
            // Check RadialGamePad view sizes
            if (leftContainer.childCount > 0) {
                val leftPad = leftContainer.getChildAt(0)
                Log.d(
                        TAG,
                        "LEFT pad: width=${leftPad.width}, height=${leftPad.height}, x=${leftPad.x}"
                )
            }
            if (rightContainer.childCount > 0) {
                val rightPad = rightContainer.getChildAt(0)
                Log.d(
                        TAG,
                        "RIGHT pad: width=${rightPad.width}, height=${rightPad.height}, x=${rightPad.x}"
                )
            }
            Log.d(TAG, "=== END ALIGNMENT DEBUG ===")

            if (maxHeight > 0) {
                Log.d(
                        TAG,
                        "GamePad heights - LEFT: $leftHeight, RIGHT: $rightHeight, MAX: $maxHeight"
                )

                // Set both to the same height
                if (leftHeight != maxHeight) {
                    leftContainer.minimumHeight = maxHeight
                    Log.d(TAG, "LEFT container minHeight set to $maxHeight")
                }
                if (rightHeight != maxHeight) {
                    rightContainer.minimumHeight = maxHeight
                    Log.d(TAG, "RIGHT container minHeight set to $maxHeight")
                }
            }
        }
    }

    /** Adjust gamepad container sizes programmatically */
    private fun adjustGamePadSizes(
            container: LinearLayout,
            gamePadWeight: Float,
            centerWeight: Float
    ) {
        // Find the child views
        val leftContainer =
                container.findViewById<android.widget.FrameLayout>(R.id.left_container)
        val rightContainer =
                container.findViewById<android.widget.FrameLayout>(R.id.right_container)
        val centerView = container.getChildAt(1) // The View in the middle

        // Adjust weights
        val leftParams =
                leftContainer.layoutParams as android.widget.LinearLayout.LayoutParams
        leftParams.weight = gamePadWeight
        leftContainer.layoutParams = leftParams

        val rightParams =
                rightContainer.layoutParams as android.widget.LinearLayout.LayoutParams
        rightParams.weight = gamePadWeight
        rightContainer.layoutParams = rightParams

        if (centerView != null) {
            val centerParams =
                    centerView.layoutParams as android.widget.LinearLayout.LayoutParams
            centerParams.weight = centerWeight
            centerView.layoutParams = centerParams
        }

        Log.d(
                TAG,
                "GamePad sizes adjusted - GamePads: $gamePadWeight, Center: $centerWeight"
        )
    }
}
