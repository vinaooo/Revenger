package com.vinaooo.revenger.input

/**
 * Bundles [ControllerInput]'s external wiring surface -- the callbacks and predicates
 * that other classes assign to react to menu-combo detection, START handling, and
 * RetroMenu3 navigation -- into a single value object.
 *
 * Extracted as part of the God-class split-up so `ControllerInput`'s 17 previously
 * independent `var` callback fields are represented as one cohesive, copyable state
 * object instead of scattered mutable properties. `ControllerInput` exposes each field
 * as a delegate property reading/writing through a `callbacks` instance of this class,
 * so existing external assignment call sites keep compiling and behaving unchanged.
 */
data class ControllerInputCallbacks(
        /** The callback for when the user inputs the menu key-combination */
        val menuCallback: () -> Unit = {},
        /** The callback for when the user inputs the SELECT+START combo (RetroMenu3) */
        val selectStartComboCallback: () -> Unit = {},
        /** The callback for when the user presses START alone (to close RetroMenu3) */
        val startButtonCallback: () -> Unit = {},
        /** Function to check if SELECT+START combo should trigger menu */
        val shouldHandleSelectStartCombo: () -> Boolean = { true },
        /** Function to check if START button alone should trigger callback */
        val shouldHandleStartButton: () -> Boolean = { false },
        /** Function to check if gamepad menu button should trigger menu */
        val shouldHandleGamepadMenuButton: () -> Boolean = { false },
        /** The callback for when the user presses the gamepad menu button */
        val gamepadMenuButtonCallback: () -> Unit = {},
        /** Function to check if all gamepad input should be blocked */
        val shouldBlockAllGamepadInput: () -> Boolean = { false },
        /** Function to check if RetroMenu3 is currently open */
        val isRetroMenu3Open: () -> Boolean = { false },
        /** Callback for RetroMenu3 "navigate up" */
        val menuNavigateUpCallback: () -> Unit = {},
        /** Callback for RetroMenu3 "navigate down" */
        val menuNavigateDownCallback: () -> Unit = {},
        /** Callback for RetroMenu3 "navigate left" */
        val menuNavigateLeftCallback: () -> Unit = {},
        /** Callback for RetroMenu3 "navigate right" */
        val menuNavigateRightCallback: () -> Unit = {},
        /** Callback for RetroMenu3 "confirm" */
        val menuConfirmCallback: () -> Unit = {},
        /** Callback for RetroMenu3 "back" */
        val menuBackCallback: () -> Unit = {},
        /** Function to check if we should intercept DPAD for menu */
        val shouldInterceptDpadForMenu: () -> Boolean = { false },
        /**
         * Function to check if it's safe to execute menu callbacks (no critical
         * operations in progress)
         */
        val isMenuOperationSafe: () -> Boolean = { true }
)
