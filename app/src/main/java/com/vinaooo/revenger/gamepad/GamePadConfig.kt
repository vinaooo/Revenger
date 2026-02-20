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
                // ===================================================
                // Indices for Empty Dials (gamepad symmetry)
                // Used to align centers between LEFT and RIGHT
                // ===================================================
                private const val EMPTY_DIAL_INDEX_8 = 8 // Para espelhar MENU do RIGHT

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
                        BUTTON_B, // Right (3h)
                        BUTTON_Y, // Top (12h)
                        BUTTON_X, // Left (9h)
                        BUTTON_A // Bottom (6h)
                )
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
                        secondaryDials =
                                listOfNotNull(
                                        SecondaryDialConfig.SingleButton(
                                                        index = 4,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_L1
                                                )
                                                .takeIf { resources.getBoolean(R.bool.conf_gp_l1) },
                                        SecondaryDialConfig.SingleButton(
                                                        index = 3,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_L2
                                                )
                                                .takeIf { resources.getBoolean(R.bool.conf_gp_l2) },
                                        SecondaryDialConfig.SingleButton(
                                                        index = 2,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_SELECT
                                                )
                                                .takeIf {
                                                        resources.getBoolean(R.bool.conf_gp_select)
                                                },
                                        // Empty Dial for symmetry with RIGHT
                                        // Necessary to align the centers of the gamepads
                                        // RIGHT has MENU at index 8, so LEFT needs an Empty
                                        // here
                                        SecondaryDialConfig.Empty(
                                                        index = EMPTY_DIAL_INDEX_8,
                                                        spread = 1,
                                                        scale = 1f,
                                                        distance = 0f
                                                )
                                                .takeIf {
                                                        resources.getBoolean(
                                                                R.bool.conf_menu_mode_gamepad
                                                        )
                                                },
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
                                                // Real buttons (conditional based on config)
                                                SecondaryDialConfig.SingleButton(
                                                                index = 2,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_R1
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_gp_r1
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 3,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_R2
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_gp_r2
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 4,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_START
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_gp_start
                                                                )
                                                        },

                                                // Fake buttons (individual visibility control)
                                                SecondaryDialConfig.SingleButton(
                                                                index = 0,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F1
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_0
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 1,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F2
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_1
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 5,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F4
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_5
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 6,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F10
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_6
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 7,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F5
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_7
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 8,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F6
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_menu_mode_gamepad
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 9,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F7
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_9
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 10,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F8
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_10
                                                                )
                                                        },
                                                SecondaryDialConfig.SingleButton(
                                                                index = 11,
                                                                scale = 1f,
                                                                distance = 0f,
                                                                buttonConfig = BUTTON_F9
                                                        )
                                                        .takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.conf_show_fake_button_11
                                                                )
                                                        }
                                        )
                                        .filterNotNull()
                )
}
