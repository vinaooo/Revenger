package com.vinaooo.revenger.utils

private const val MENU_LOGGER_TAG = "RetroMenu3"

/**
 * Utility for conditional logging in the RetroMenu3 menu system. Allows controlling
 * production logs via a debug flag.
 *
 * The always-on level methods ([i]/[w]/[e]) and the debug gate ([d]/[setDebugEnabled]) are split
 * into [LevelLogger]/[DebugGate] and exposed back here unchanged via interface delegation -- see
 * those classes for why.
 */
object MenuLogger :
        LevelLogging by LevelLogger(MENU_LOGGER_TAG),
        DebugLogging by DebugGate(MENU_LOGGER_TAG) {

    /** Log specifically for lifecycle events */
    fun lifecycle(message: String) {
        d("[LIFECYCLE] $message")
    }

    /** Log specifically for navigation */
    fun navigation(message: String) {
        d("[NAV] $message")
    }

    /** Log specifically for actions */
    fun action(message: String) {
        d("[ACTION] $message")
    }

    /** Log specifically for animations/dismiss */
    fun animation(message: String) {
        d("[ANIMATION] $message")
    }

    /** Log specifically for menu state */
    fun state(message: String) {
        d("[STATE] $message")
    }

    /** Log specifically for performance */
    fun performance(message: String) {
        d("[PERFORMANCE] $message")
    }
}
