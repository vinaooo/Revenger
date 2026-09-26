package com.vinaooo.revenger.utils

import android.content.res.Configuration
import android.view.InputDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The text of [LogSaver]'s report: file name, section layout and the per-line formatting of the
 * values [LogSaver] reads from the device. Pure (no I/O, no Android services), so every rule here
 * is unit-testable; [LogSaver] keeps only the calls into the framework.
 */
object LogReport {

    /** How many of the most recent logcat lines the report keeps. */
    const val MAX_LOG_LINES = 1000

    private const val BYTES_PER_MEGABYTE = 1024L * 1024L
    private const val RULE = "========================================"

    /** The report's file name for a report generated at [at], e.g. `revenger_log_20260926_213000.txt`. */
    fun fileName(at: Date, locale: Locale = Locale.getDefault()): String =
            "revenger_log_${SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(at)}.txt"

    /** A section banner: [title] between two rules, followed by a blank line. */
    fun section(title: String): String = "$RULE\n        $title\n$RULE\n\n"

    /** The bodies of the report's four sections. */
    data class Sections(
            val deviceInfo: String,
            val appInfo: String,
            val configurationInfo: String,
            val systemLogs: String
    )

    /**
     * Assembles the whole report: the title banner and generation time, then the device, app,
     * configuration and system-log sections, in that order.
     */
    fun build(generatedAt: Date, sections: Sections, locale: Locale = Locale.getDefault()): String = buildString {
        append(section("REVENGER LOG REPORT"))
        append("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(generatedAt)}\n\n")
        append(section("DEVICE INFORMATION"))
        append(sections.deviceInfo).append("\n")
        append(section("APPLICATION INFORMATION"))
        append(sections.appInfo).append("\n")
        append(section("CONFIGURATION INFORMATION"))
        append(sections.configurationInfo).append("\n")
        append(section("SYSTEM LOGS"))
        append(sections.systemLogs)
    }

    /** Human name of a [Configuration.orientation] value. */
    fun orientationName(orientation: Int): String =
            when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                else -> "Unknown"
            }

    /** The memory line, with each byte count rounded down to whole megabytes. */
    fun memoryLine(totalBytes: Long, freeBytes: Long, maxBytes: Long): String =
            "Memory - Total: ${totalBytes / BYTES_PER_MEGABYTE}MB, " +
                    "Free: ${freeBytes / BYTES_PER_MEGABYTE}MB, Max: ${maxBytes / BYTES_PER_MEGABYTE}MB\n"

    /** A connected input device, as far as the input-method line cares: its name and source bits. */
    data class InputDeviceSummary(val name: String, val sources: Int)

    /** Whether an [InputDevice.getSources] bitmask reports a gamepad or joystick. */
    fun isGamepadSource(sources: Int): Boolean =
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    /**
     * Describes the input method from the connected devices: gamepads whose name contains
     * "virtual" (any case) count as virtual, other gamepads as physical, non-gamepads are ignored.
     */
    fun describeInputMethod(devices: List<InputDeviceSummary>): String {
        val gamepads = devices.filter { isGamepadSource(it.sources) }
        val hasVirtual = gamepads.any { it.name.contains("virtual", ignoreCase = true) }
        val hasPhysical = gamepads.any { !it.name.contains("virtual", ignoreCase = true) }
        return when {
            hasPhysical && hasVirtual -> "Physical + Virtual Gamepad"
            hasPhysical -> "Physical Gamepad"
            hasVirtual -> "Virtual Gamepad"
            else -> "Touch/Other Input"
        }
    }

    /** The last [max] lines of [logs], joined back with `\n`. */
    fun lastLines(logs: String, max: Int = MAX_LOG_LINES): String = logs.lines().takeLast(max).joinToString("\n")
}
