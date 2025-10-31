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

                // PlayStation button symbols with vector icons
                val BUTTON_PS_TRIANGLE =
                        ButtonConfig(
                                id = KeyEvent.KEYCODE_BUTTON_Y,
                                iconId = R.drawable.ic_ps_triangle
                        )
                val BUTTON_PS_CIRCLE =
                        ButtonConfig(
                                id = KeyEvent.KEYCODE_BUTTON_B,
                                iconId = R.drawable.ic_ps_circle
                        )
                val BUTTON_PS_CROSS =
                        ButtonConfig(
                                id = KeyEvent.KEYCODE_BUTTON_A,
                                iconId = R.drawable.ic_ps_cross
                        )
                val BUTTON_PS_SQUARE =
                        ButtonConfig(
                                id = KeyEvent.KEYCODE_BUTTON_X,
                                iconId = R.drawable.ic_ps_square
                        )

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
                        normalColor = ContextCompat.getColor(context, R.color.gamepad_button_color),
                        pressedColor =
                                ContextCompat.getColor(context, R.color.gamepad_pressed_color)
                )

        private fun getActionButtonsInOrder(): List<ButtonConfig> {
                val actionButtonStyle = resources.getInteger(R.integer.config_action_button)

                return when (actionButtonStyle) {
                        1 -> // Nintendo: Top=B, Bottom=X, Left=Y, Right=A (bottom/top swapped)
                        listOfNotNull(
                                        BUTTON_A, // Right - always visible
                                        BUTTON_X, // Bottom - swapped with top
                                        BUTTON_Y, // Left - always visible
                                        BUTTON_B // Top - swapped with bottom
                                )
                        2 -> // Xbox: Top=A, Bottom=Y, Left=X, Right=B (top/bottom swapped)
                        listOfNotNull(
                                        BUTTON_B, // Right - always visible
                                        BUTTON_Y, // Bottom - swapped with top
                                        BUTTON_X, // Left - always visible
                                        BUTTON_A // Top - swapped with bottom
                                )
                        3 -> // PlayStation: Top=×, Bottom=△, Left=□, Right=○ (Triangle on bottom, X
                                // on top)
                                listOfNotNull(
                                        BUTTON_PS_CIRCLE, // Right (○) - always visible
                                        BUTTON_PS_TRIANGLE, // Bottom (△) - always visible
                                        BUTTON_PS_SQUARE, // Left (□) - always visible
                                        BUTTON_PS_CROSS // Top (×) - always visible
                                )
                        else -> // Default to Nintendo: Top=X, Bottom=B, Left=Y, Right=A
                        listOfNotNull(
                                        BUTTON_A, // Right - always visible
                                        BUTTON_B, // Bottom - always visible
                                        BUTTON_Y, // Left - always visible
                                        BUTTON_X // Top - always visible
                                )
                }
        }

        val left =
                RadialGamePadConfig(
                        haptic =
                                if (resources.getBoolean(R.bool.config_gamepad_haptic))
                                        HapticConfig.PRESS
                                else HapticConfig.OFF,
                        theme = radialGamePadTheme,
                        sockets = 12,
                        primaryDial =
                                if (resources.getBoolean(R.bool.config_left_analog)) LEFT_ANALOG
                                else LEFT_DPAD,
                        secondaryDials =
                                listOfNotNull(
                                        SecondaryDialConfig.SingleButton(
                                                        index = 4,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_L1
                                                )
                                                .takeIf {
                                                        resources.getBoolean(
                                                                R.bool.config_gamepad_l1
                                                        )
                                                },
                                        SecondaryDialConfig.SingleButton(
                                                        index = 3,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_L2
                                                )
                                                .takeIf {
                                                        resources.getBoolean(
                                                                R.bool.config_gamepad_l2
                                                        )
                                                },
                                        SecondaryDialConfig.SingleButton(
                                                        index = 2,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_SELECT
                                                )
                                                .takeIf {
                                                        resources.getBoolean(
                                                                R.bool.config_gamepad_select
                                                        )
                                                },
                                )
                )

        val right =
                RadialGamePadConfig(
                        haptic =
                                if (resources.getBoolean(R.bool.config_gamepad_haptic))
                                        HapticConfig.PRESS
                                else HapticConfig.OFF,
                        theme = radialGamePadTheme,
                        sockets = 12,
                        primaryDial =
                                PrimaryDialConfig.PrimaryButtons(
                                        dials = getActionButtonsInOrder(),
                                        allowMultiplePressesSingleFinger =
                                                resources.getBoolean(
                                                        R.bool.config_gamepad_allow_multiple_presses_action
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
                                                                        R.bool.config_gamepad_r1
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
                                                                        R.bool.config_gamepad_r2
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
                                                                        R.bool.config_gamepad_start
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
                                                                        R.bool.config_show_fake_button_0
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
                                                                        R.bool.config_show_fake_button_1
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
                                                                        R.bool.config_show_fake_button_5
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
                                                                        R.bool.config_show_fake_button_6
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
                                                                        R.bool.config_show_fake_button_7
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
                                                                        R.bool.config_menu_mode_gamepad
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
                                                                        R.bool.config_show_fake_button_9
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
                                                                        R.bool.config_show_fake_button_10
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
                                                                        R.bool.config_show_fake_button_11
                                                                )
                                                        }
                                        )
                                        .filterNotNull()
                )
}
