package com.vinaooo.revenger.utils

import android.content.res.Configuration
import android.view.InputDevice
import com.vinaooo.revenger.utils.LogReport.InputDeviceSummary
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure text rules of [LogSaver]'s report. */
class LogReport_test {

    // 2026-09-26 21:30:05 in the default time zone, which both SimpleDateFormats use.
    private val at: Date =
            Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 26, 21, 30, 5) }.time

    @Test
    fun `nome do arquivo usa data e hora da geracao`() {
        assertEquals("revenger_log_20260926_213005.txt", LogReport.fileName(at, Locale.US))
    }

    @Test
    fun `secao e o titulo entre duas reguas seguido de linha em branco`() {
        val rule = "=".repeat(40)

        assertEquals("$rule\n        TITLE\n$rule\n\n", LogReport.section("TITLE"))
    }

    @Test
    fun `relatorio tem cabecalho, data e as quatro secoes em ordem`() {
        val report = LogReport.build(at, LogReport.Sections("DEVICE-BODY", "APP-BODY", "CONFIG-BODY", "LOGS-BODY"), Locale.US)

        assertTrue(report.startsWith(LogReport.section("REVENGER LOG REPORT") + "Generated at: 2026-09-26 21:30:05\n\n"))
        val order =
                listOf(
                        "DEVICE INFORMATION",
                        "DEVICE-BODY",
                        "APPLICATION INFORMATION",
                        "APP-BODY",
                        "CONFIGURATION INFORMATION",
                        "CONFIG-BODY",
                        "SYSTEM LOGS",
                        "LOGS-BODY",
                ).map(report::indexOf)
        assertTrue("$order", order.all { it >= 0 } && order == order.sorted())
        assertTrue(report.endsWith(LogReport.section("SYSTEM LOGS") + "LOGS-BODY"))
    }

    @Test
    fun `orientacao retrato, paisagem e desconhecida`() {
        assertEquals("Portrait", LogReport.orientationName(Configuration.ORIENTATION_PORTRAIT))
        assertEquals("Landscape", LogReport.orientationName(Configuration.ORIENTATION_LANDSCAPE))
        assertEquals("Unknown", LogReport.orientationName(Configuration.ORIENTATION_UNDEFINED))
    }

    @Test
    fun `memoria arredonda para baixo em megabytes`() {
        val mb = 1024L * 1024L

        assertEquals(
                "Memory - Total: 3MB, Free: 0MB, Max: 512MB\n",
                LogReport.memoryLine(totalBytes = 3 * mb + mb / 2, freeBytes = mb - 1, maxBytes = 512 * mb)
        )
    }

    @Test
    fun `gamepad e joystick contam como gamepad`() {
        assertTrue(LogReport.isGamepadSource(InputDevice.SOURCE_GAMEPAD))
        assertTrue(LogReport.isGamepadSource(InputDevice.SOURCE_JOYSTICK))
        assertTrue(LogReport.isGamepadSource(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD))
        assertFalse(LogReport.isGamepadSource(0))
    }

    @Test
    fun `teclado nao conta como gamepad`() {
        // Regression: the old check tested only the shared "button" class bit, so every keyboard
        // (SOURCE_KEYBOARD also has it) was classified as a gamepad.
        assertFalse(LogReport.isGamepadSource(InputDevice.SOURCE_KEYBOARD))
        assertFalse(LogReport.isGamepadSource(InputDevice.SOURCE_DPAD))
    }

    @Test
    fun `metodo de entrada conforme os gamepads conectados`() {
        val physical = InputDeviceSummary("Pad", InputDevice.SOURCE_GAMEPAD)
        val virtual = InputDeviceSummary("Some VIRTUAL pad", InputDevice.SOURCE_JOYSTICK)

        assertEquals("Touch/Other Input", LogReport.describeInputMethod(emptyList()))
        assertEquals("Physical Gamepad", LogReport.describeInputMethod(listOf(physical)))
        assertEquals("Virtual Gamepad", LogReport.describeInputMethod(listOf(virtual)))
        assertEquals("Physical + Virtual Gamepad", LogReport.describeInputMethod(listOf(virtual, physical)))
    }

    @Test
    fun `teclado virtual do sistema nao vira gamepad virtual`() {
        // Every device has Android's built-in "Virtual" key-character-map device, a keyboard.
        val systemVirtualKeys = InputDeviceSummary("Virtual", InputDevice.SOURCE_KEYBOARD)

        assertEquals("Touch/Other Input", LogReport.describeInputMethod(listOf(systemVirtualKeys)))
    }

    @Test
    fun `log do sistema mantem so as ultimas linhas`() {
        val logs = (1..1005).joinToString("\n")

        val kept = LogReport.lastLines(logs)

        assertEquals(LogReport.MAX_LOG_LINES, kept.lines().size)
        assertTrue(kept.startsWith("6\n"))
        assertTrue(kept.endsWith("\n1005"))
        assertEquals("a\nb", LogReport.lastLines("a\nb"))
    }
}
