package com.vinaooo.revenger.utils

import android.os.Build

/** Android version checks for code that behaves differently per API level. */
object AndroidCompatibility {

    private const val ANDROID_16_API_LEVEL = 36

    /** Check if running on Android 12+ (API 31) */
    fun isAndroid12Plus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Check if running on Android 16+ (API 36) */
    fun isAndroid16Plus(): Boolean = Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL
}
