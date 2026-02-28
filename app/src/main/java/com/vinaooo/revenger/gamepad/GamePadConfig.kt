package com.vinaooo.revenger.gamepad

import android.content.Context
import android.content.res.Resources
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.config.*
import com.swordfish.radialgamepad.library.haptics.HapticConfig
import com.vinaooo.revenger.R

class GamePadConfig(context: Context, private val resources: Resources) {
        companion object {

                val BUTTON_START = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_START, label = "+")

                val BUTTON_SELECT = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_SELECT, label = "-")

                val BUTTON_L1 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_L1, label = "L1")

                val BUTTON_R1 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_R1, label = "R1")

                val BUTTON_L2 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_L2, label = "L2")

                val BUTTON_R2 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_R2, label = "R2")

                val BUTTON_A = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_A, label = "A")

                val BUTTON_B = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_B, label = "B")

                val BUTTON_X = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_X, label = "X")

                val BUTTON_Y = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_Y, label = "Y")

                // Fake buttons for filling empty sockets
                val BUTTON_F1 = ButtonConfig(id = -1, label = "0")
                val BUTTON_F2 = ButtonConfig(id = -2, label = "1")
                val BUTTON_F3 = ButtonConfig(id = -3, label = "3")
                val BUTTON_F4 = ButtonConfig(id = -4, label = "5")
                val BUTTON_F5 = ButtonConfig(id = -5, label = "7")
                val BUTTON_F6 = ButtonConfig(id = -6, label = "☰")
                val BUTTON_F7 = ButtonConfig(id = -7, label = "9")
                val BUTTON_F8 = ButtonConfig(id = -8, label = "10")
                val BUTTON_F9 = ButtonConfig(id = -9, label = "11")
                val BUTTON_F10 = ButtonConfig(id = -10, label = "6")

                val LEFT_DPAD = PrimaryDialConfig.Cross(CrossConfig(GLRetroView.MOTION_SOURCE_DPAD))
                val LEFT_ANALOG = PrimaryDialConfig.Stick(GLRetroView.MOTION_SOURCE_ANALOG_LEFT)
        }

        private val radialGamePadTheme =
                RadialGamePadTheme(
                        textColor = ContextCompat.getColor(context, android.R.color.white),
                        normalColor = ContextCompat.getColor(context, R.color.gp_button_color),
                        pressedColor = ContextCompat.getColor(context, R.color.gp_pressed_color)
                )

        private fun getActionButtonsInOrder(): List<ButtonConfig> {
                return listOfNotNull( // ordem antihoraria
                        BUTTON_B.takeIf { resources.getBoolean(R.bool.conf_gp_b) }, // Right (3h)
                        BUTTON_Y.takeIf { resources.getBoolean(R.bool.conf_gp_y) }, // Top (12h)
                        BUTTON_X.takeIf { resources.getBoolean(R.bool.conf_gp_x) }, // Left (9h)
                        BUTTON_A.takeIf { resources.getBoolean(R.bool.conf_gp_a) } // Bottom (6h)
                )
        }

        /**
         * Helper: returns a SingleButton at the given index if the condition is true, otherwise
         * returns an Empty dial at the same index to preserve socket layout. This guarantees both
         * sides always occupy the same socket positions, keeping their bounding boxes identical and
         * centers perfectly aligned.
         */
        private fun buttonOrEmpty(
                index: Int,
                buttonConfig: ButtonConfig,
                visible: Boolean
        ): SecondaryDialConfig {
                return if (visible) {
                        SecondaryDialConfig.SingleButton(
                                index = index,
                                scale = 1f,
                                distance = 0f,
                                buttonConfig = buttonConfig
                        )
                } else {
                        SecondaryDialConfig.Empty(
                                index = index,
                                spread = 1,
                                scale = 1f,
                                distance = 0f
                        )
                }
        }

        // ===================================================================
        // ALIGNMENT STRATEGY:
        // 1. Build each side's button map: index → (ButtonConfig, visible?)
        // 2. Compute UNION of all visible indices from either side
        // 3. For each visible index, ALSO include its opposite ((i+6)%12)
        //    to balance the bounding box symmetrically around the center.
        //    Without this, dials on one side shift the circle center, causing
        //    the LEFT pad to clip against the left edge while the RIGHT pad
        //    has extra margin from the right edge.
        // 4. For each index in the balanced set:
        //    - If that side has a visible button → SingleButton
        //    - Otherwise → Empty dial (invisible placeholder)
        // ===================================================================

        // --- LEFT side button definitions (index → button, isVisible) ---
        private val leftButtons =
                mapOf(
                        2 to Pair(BUTTON_SELECT, resources.getBoolean(R.bool.conf_gp_select)),
                        3 to Pair(BUTTON_L2, resources.getBoolean(R.bool.conf_gp_l2)),
                        4 to Pair(BUTTON_L1, resources.getBoolean(R.bool.conf_gp_l1)),
                )

        // --- RIGHT side button definitions (index → button, isVisible) ---
        private val rightButtons =
                mapOf(
                        0 to Pair(BUTTON_F1, resources.getBoolean(R.bool.conf_show_fake_button_0)),
                        1 to Pair(BUTTON_F2, resources.getBoolean(R.bool.conf_show_fake_button_1)),
                        2 to Pair(BUTTON_R1, resources.getBoolean(R.bool.conf_gp_r1)),
                        3 to Pair(BUTTON_R2, resources.getBoolean(R.bool.conf_gp_r2)),
                        4 to Pair(BUTTON_START, resources.getBoolean(R.bool.conf_gp_start)),
                        5 to Pair(BUTTON_F4, resources.getBoolean(R.bool.conf_show_fake_button_5)),
                        6 to Pair(BUTTON_F10, resources.getBoolean(R.bool.conf_show_fake_button_6)),
                        7 to Pair(BUTTON_F5, resources.getBoolean(R.bool.conf_show_fake_button_7)),
                        8 to Pair(BUTTON_F6, resources.getBoolean(R.bool.conf_menu_mode_gamepad)),
                        9 to Pair(BUTTON_F7, resources.getBoolean(R.bool.conf_show_fake_button_9)),
                        10 to
                                Pair(
                                        BUTTON_F8,
                                        resources.getBoolean(R.bool.conf_show_fake_button_10)
                                ),
                        11 to
                                Pair(
                                        BUTTON_F9,
                                        resources.getBoolean(R.bool.conf_show_fake_button_11)
                                ),
                )

        // Balanced union: for each visible button, include its opposite index
        // to keep the bounding box centered. E.g. Select at 2 → also add 8.
        private val allIndices: List<Int> = run {
                val leftVisible = leftButtons.filter { it.value.second }.keys
                val rightVisible = rightButtons.filter { it.value.second }.keys
                val visibleUnion = leftVisible + rightVisible
                val balanced = visibleUnion + visibleUnion.map { (it + 6) % 12 }
                balanced.distinct().sorted()
        }

        /**
         * Builds the secondary dials list for one side. For each index in the union:
         * - If this side has a visible button → SingleButton
         * - Otherwise → Empty dial (keeps bounding box consistent)
         */
        private fun buildSecondaryDials(
                ownButtons: Map<Int, Pair<ButtonConfig, Boolean>>
        ): List<SecondaryDialConfig> {
                return allIndices.map { index ->
                        val entry = ownButtons[index]
                        if (entry != null && entry.second) {
                                SecondaryDialConfig.SingleButton(
                                        index = index,
                                        scale = 1f,
                                        distance = 0f,
                                        buttonConfig = entry.first
                                )
                        } else {
                                SecondaryDialConfig.Empty(
                                        index = index,
                                        spread = 1,
                                        scale = 1f,
                                        distance = 0f
                                )
                        }
                }
        }

        val left =
                RadialGamePadConfig(
                        haptic =
                                if (resources.getBoolean(R.bool.conf_gp_haptic)) HapticConfig.PRESS
                                else HapticConfig.OFF,
                        theme = radialGamePadTheme,
                        sockets = 12,
                        primaryDial =
                                if (resources.getBoolean(R.bool.conf_left_analog)) LEFT_ANALOG
                                else LEFT_DPAD,
                        secondaryDials = buildSecondaryDials(leftButtons)
                )

        val right =
                RadialGamePadConfig(
                        haptic =
                                if (resources.getBoolean(R.bool.conf_gp_haptic)) HapticConfig.PRESS
                                else HapticConfig.OFF,
                        theme = radialGamePadTheme,
                        sockets = 12,
                        primaryDial =
                                PrimaryDialConfig.PrimaryButtons(
                                        dials = getActionButtonsInOrder(),
                                        allowMultiplePressesSingleFinger =
                                                resources.getBoolean(
                                                        R.bool.conf_gp_allow_multiple_presses_action
                                                )
                                ),
                        secondaryDials = buildSecondaryDials(rightButtons)
                )
}
