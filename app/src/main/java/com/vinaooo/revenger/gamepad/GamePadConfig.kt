package com.vinaooo.revenger.gamepad

import android.content.Context
import android.content.res.Resources
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.swordfish.radialgamepad.library.config.*
import com.swordfish.radialgamepad.library.haptics.HapticConfig
import com.vinaooo.revenger.R

/** Modern gamepad button definitions using object pattern */
object GamePadButtons {
        val BUTTON_START = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_START, label = "+")
        val BUTTON_SELECT = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_SELECT, label = "-")
        val BUTTON_L1 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_L1, label = "L")
        val BUTTON_R1 = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_R1, label = "R")
        val BUTTON_A = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_A, label = "A")
        val BUTTON_B = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_B, label = "B")
        val BUTTON_X = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_X, label = "X")
        val BUTTON_Y = ButtonConfig(id = KeyEvent.KEYCODE_BUTTON_Y, label = "Y")

        val LEFT_DPAD = PrimaryDialConfig.Cross(CrossConfig(0))
        val LEFT_ANALOG = PrimaryDialConfig.Stick(0)
}

class GamePadConfig(context: Context, private val resources: Resources) {
        companion object {
                // Import buttons from GamePadButtons object for backward compatibility
                val BUTTON_START = GamePadButtons.BUTTON_START
                val BUTTON_SELECT = GamePadButtons.BUTTON_SELECT
                val BUTTON_L1 = GamePadButtons.BUTTON_L1
                val BUTTON_R1 = GamePadButtons.BUTTON_R1
                val BUTTON_A = GamePadButtons.BUTTON_A
                val BUTTON_B = GamePadButtons.BUTTON_B
                val BUTTON_X = GamePadButtons.BUTTON_X
                val BUTTON_Y = GamePadButtons.BUTTON_Y
                val LEFT_DPAD = GamePadButtons.LEFT_DPAD
                val LEFT_ANALOG = GamePadButtons.LEFT_ANALOG
        }

        private val radialGamePadTheme =
                RadialGamePadTheme(
                        textColor = ContextCompat.getColor(context, R.color.gamepad_icon_color),
                        normalColor = ContextCompat.getColor(context, R.color.gamepad_button_color),
                        pressedColor =
                                ContextCompat.getColor(context, R.color.gamepad_pressed_color)
                )

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
                                                        index = 10,
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
                                        dials =
                                                listOfNotNull(
                                                        BUTTON_A.takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.config_gamepad_a
                                                                )
                                                        },
                                                        BUTTON_X.takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.config_gamepad_x
                                                                )
                                                        },
                                                        BUTTON_Y.takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.config_gamepad_y
                                                                )
                                                        },
                                                        BUTTON_B.takeIf {
                                                                resources.getBoolean(
                                                                        R.bool.config_gamepad_b
                                                                )
                                                        }
                                                )
                                ),
                        secondaryDials =
                                listOfNotNull(
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
                                                        index = 8,
                                                        scale = 1f,
                                                        distance = 0f,
                                                        buttonConfig = BUTTON_START
                                                )
                                                .takeIf {
                                                        resources.getBoolean(
                                                                R.bool.config_gamepad_start
                                                        )
                                                },
                                )
                )
}
