package com.vinaooo.revenger.models

import com.google.gson.annotations.SerializedName

data class GamepadConfigJson(
    @SerializedName("colors") val colors: GamepadColorsConfig = GamepadColorsConfig(),
    @SerializedName("dimensions") val dimensions: GamepadDimensionsConfig = GamepadDimensionsConfig(),
    @SerializedName("offsets") val offsets: GamepadOffsetsConfig = GamepadOffsetsConfig()
)

data class GamepadColorsConfig(
    @SerializedName("button_color") val buttonColor: String = "#88ffffff",
    @SerializedName("pressed_color") val pressedColor: String = "#66ffffff"
)

data class GamepadDimensionsConfig(
    @SerializedName("padding_vertical_dp") val paddingVerticalDp: Int = 20
)

data class GamepadOffsetsConfig(
    @SerializedName("portrait_percent") val portraitPercent: Int = 100,
    @SerializedName("landscape_percent") val landscapePercent: Int = 50
)
