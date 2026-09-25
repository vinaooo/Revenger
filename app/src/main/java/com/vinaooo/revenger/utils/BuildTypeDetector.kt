package com.vinaooo.revenger.utils

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Tells debug builds from release builds using the installed app's `FLAG_DEBUGGABLE`, which
 * the build type controls.
 *
 * Don't infer the build type from the package name: the `applicationId` includes the sanitized
 * game name, so a release build of a game whose name contains "debug" would be misreported.
 */
object BuildTypeDetector {
    /** True when [appInfo] has [ApplicationInfo.FLAG_DEBUGGABLE] set. */
    fun isDebuggable(appInfo: ApplicationInfo): Boolean =
        (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** True when the app that [context] belongs to is a debuggable (debug) build. */
    fun isDebuggable(context: Context): Boolean = isDebuggable(context.applicationInfo)
}
