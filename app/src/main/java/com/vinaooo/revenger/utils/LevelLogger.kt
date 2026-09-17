package com.vinaooo.revenger.utils

import android.util.Log

/**
 * Plain, always-on multi-level logging (info/warn/error, with or without a throwable) used by
 * [MenuLogger]. Split out purely to keep that object under the project's function-count
 * threshold; exposed back on it via Kotlin interface delegation (`by`) since these are called
 * from many places across the menu system (`MenuLogger.w(...)`, `MenuLogger.e(...)`, etc).
 */
interface LevelLogging {
    /** Info log (always active) */
    fun i(message: String)

    /** Info log with throwable (always active) */
    fun i(message: String, throwable: Throwable)

    /** Warning log (always active) */
    fun w(message: String)

    /** Warning log with throwable (always active) */
    fun w(message: String, throwable: Throwable)

    /** Error log (always active) */
    fun e(message: String)

    /** Error log with throwable (always active) */
    fun e(message: String, throwable: Throwable)
}

class LevelLogger(private val tag: String) : LevelLogging {
    override fun i(message: String) {
        Log.i(tag, message)
    }

    override fun i(message: String, throwable: Throwable) {
        Log.i(tag, message, throwable)
    }

    override fun w(message: String) {
        Log.w(tag, message)
    }

    override fun w(message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    override fun e(message: String) {
        Log.e(tag, message)
    }

    override fun e(message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}
