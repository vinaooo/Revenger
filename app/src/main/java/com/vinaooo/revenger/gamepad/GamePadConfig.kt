package com.vinaooo.revenger.gamepad

import android.content.Context
import android.content.res.Resources
import android.view.KeyEvent
import androidx.core.content.ContextCompat
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

                val LEFT_DPAD = PrimaryDialConfig.Cross(CrossConfig(0))
                val LEFT_ANALOG = PrimaryDialConfig.Stick(0)
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
        // Socket map (12 sockets, clock positions):
        //   0=5h  1=4h  2=3h  3=2h  4=1h  5=12h
        //   6=11h 7=10h 8=9h  9=8h  10=7h 11=6h
        //
        // LEFT side real buttons:  L1(4) L2(3) Select(2)  Menu-mirror(8)
        // RIGHT side real buttons: R1(2) R2(3) Start(4)   Menu(8) + Fakes
        //
        // ALIGNMENT RULE: Every socket that is occupied on one side must also
        // be occupied on the other side (with a real button OR an Empty dial).
        // This keeps both RadialGamePad bounding boxes identical.
        // ===================================================================

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
                        secondaryDials =
                                listOf(
                                        // Mirror RIGHT's fake button slots with Empty dials
                                        SecondaryDialConfig.Empty(
                                                index = 0,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 1,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        // Real buttons (or Empty if hidden)
                                        buttonOrEmpty(
                                                2,
                                                BUTTON_SELECT,
                                                resources.getBoolean(R.bool.conf_gp_select)
                                        ),
                                        buttonOrEmpty(
                                                3,
                                                BUTTON_L2,
                                                resources.getBoolean(R.bool.conf_gp_l2)
                                        ),
                                        buttonOrEmpty(
                                                4,
                                                BUTTON_L1,
                                                resources.getBoolean(R.bool.conf_gp_l1)
                                        ),
                                        // Mirror RIGHT's remaining slots
                                        SecondaryDialConfig.Empty(
                                                index = 5,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 6,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 7,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        // Menu mirror (Empty always — LEFT never shows a button
                                        // here)
                                        SecondaryDialConfig.Empty(
                                                index = 8,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 9,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 10,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                        SecondaryDialConfig.Empty(
                                                index = 11,
                                                spread = 1,
                                                scale = 1f,
                                                distance = 0f
                                        ),
                                )
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
                        secondaryDials =
                                listOf(
                                        // Fake buttons (or Empty if hidden)
                                        buttonOrEmpty(
                                                0,
                                                BUTTON_F1,
                                                resources.getBoolean(R.bool.conf_show_fake_button_0)
                                        ),
                                        buttonOrEmpty(
                                                1,
                                                BUTTON_F2,
                                                resources.getBoolean(R.bool.conf_show_fake_button_1)
                                        ),
                                        // Real buttons (or Empty if hidden)
                                        buttonOrEmpty(
                                                2,
                                                BUTTON_R1,
                                                resources.getBoolean(R.bool.conf_gp_r1)
                                        ),
                                        buttonOrEmpty(
                                                3,
                                                BUTTON_R2,
                                                resources.getBoolean(R.bool.conf_gp_r2)
                                        ),
                                        buttonOrEmpty(
                                                4,
                                                BUTTON_START,
                                                resources.getBoolean(R.bool.conf_gp_start)
                                        ),
                                        // Fake buttons (or Empty if hidden)
                                        buttonOrEmpty(
                                                5,
                                                BUTTON_F4,
                                                resources.getBoolean(R.bool.conf_show_fake_button_5)
                                        ),
                                        buttonOrEmpty(
                                                6,
                                                BUTTON_F10,
                                                resources.getBoolean(R.bool.conf_show_fake_button_6)
                                        ),
                                        buttonOrEmpty(
                                                7,
                                                BUTTON_F5,
                                                resources.getBoolean(R.bool.conf_show_fake_button_7)
                                        ),
                                        // Menu button (or Empty if hidden)
                                        buttonOrEmpty(
                                                8,
                                                BUTTON_F6,
                                                resources.getBoolean(R.bool.conf_menu_mode_gamepad)
                                        ),
                                        buttonOrEmpty(
                                                9,
                                                BUTTON_F7,
                                                resources.getBoolean(R.bool.conf_show_fake_button_9)
                                        ),
                                        buttonOrEmpty(
                                                10,
                                                BUTTON_F8,
                                                resources.getBoolean(
                                                        R.bool.conf_show_fake_button_10
                                                )
                                        ),
                                        buttonOrEmpty(
                                                11,
                                                BUTTON_F9,
                                                resources.getBoolean(
                                                        R.bool.conf_show_fake_button_11
                                                )
                                        ),
                                )
                )
}
