package com.vinaooo.revenger.models

import org.json.JSONObject

/**
 * Platform-specific aspect ratio configurations for Picture-in-Picture mode.
 */
data class PipConfigProfile(
    val platformId: String,
    val ratioW: Int,
    val ratioH: Int
) {
    companion object {
        fun fromJson(platformId: String, json: JSONObject): PipConfigProfile {
            return PipConfigProfile(
                platformId = platformId,
                ratioW = json.getInt("ratio_w"),
                ratioH = json.getInt("ratio_h")
            )
        }
    }
}
