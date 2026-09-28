package com.vinaooo.revenger.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.RevengerApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [LogSaver]: the "Build Type" line of the app info (`getAppInfo` is private, so it is invoked by
 * reflection with a mocked [Context]), and the whole report written through the internal
 * `writeLog` seam into a temp dir, with a fixed clock and fake system log.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LogSaver_test {

    private fun contextWith(flags: Int, packageName: String): Context {
        val info = ApplicationInfo().apply {
            this.flags = flags
            this.packageName = packageName
        }
        val packageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val pm = mockk<PackageManager> {
            every { getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
        }
        return mockk {
            every { packageManager } returns pm
            every { this@mockk.packageName } returns packageName
            every { applicationInfo } returns info
        }
    }

    private fun appInfo(context: Context): String {
        val method = LogSaver.javaClass.getDeclaredMethod("getAppInfo", Context::class.java)
        method.isAccessible = true
        return method.invoke(LogSaver, context) as String
    }

    @Test
    fun `release cujo pacote contem debug e reportado como Release`() {
        // Regression: the old packageName.contains("debug") check reported this as Debug.
        val text = appInfo(contextWith(0, "com.example.app.debug_game_core"))

        assertTrue(text, text.contains("Build Type: Release"))
    }

    @Test
    fun `app debuggable sem debug no pacote e reportado como Debug`() {
        val text = appInfo(contextWith(ApplicationInfo.FLAG_DEBUGGABLE, "com.example.app"))

        assertTrue(text, text.contains("Build Type: Debug"))
    }

    @get:Rule val tempDir = TemporaryFolder()

    private val appContext: Context get() = ApplicationProvider.getApplicationContext()

    // 2026-09-26 21:30:05 in the default time zone.
    private val at: Date =
            Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 26, 21, 30, 5) }.time

    /** The app context, with an asset manager that has (or lacks) the packaged ROM. */
    private fun contextWithRom(present: Boolean): Context {
        val assets = mockk<AssetManager> {
            if (present) every { open(any()) } returns ByteArrayInputStream(byteArrayOf(1))
            else every { open(any()) } throws FileNotFoundException("no rom")
        }
        return object : ContextWrapper(appContext) {
            override fun getAssets(): AssetManager = assets
        }
    }

    @Test
    fun `writeLog grava o relatorio completo com o nome da data de geracao`() {
        val path = LogSaver.writeLog(contextWithRom(present = true), tempDir.root, at) { "FAKE-LOGCAT-LINE" }

        val file = File(requireNotNull(path))
        assertEquals(tempDir.root, file.parentFile)
        assertEquals(LogReport.fileName(at), file.name)
        val text = file.readText()
        for (expected in
                listOf(
                        "DEVICE INFORMATION",
                        "Screen Orientation: ",
                        "Memory - Total: ",
                        "Build Type: ",
                        "Game ID: ",
                        "ROM Status: Available",
                        "Input Method: ",
                        "FAKE-LOGCAT-LINE",
                )) {
            assertTrue(expected, text.contains(expected))
        }
    }

    @Test
    fun `ROM de assets ausente e reportada como nao encontrada`() {
        // Regression: the check used to look for a raw resource, where the ROM never is, so the
        // report always said "Not found" even with the ROM packaged in assets/rom/.
        val path = LogSaver.writeLog(contextWithRom(present = false), tempDir.root, at) { "" }

        assertTrue(File(requireNotNull(path)).readText().contains("ROM Status: Not found or inaccessible"))
    }

    @Test
    fun `procura a ROM em assets rom`() {
        val context = contextWithRom(present = true)

        LogSaver.writeLog(context, tempDir.root, at) { "" }

        verify { context.assets.open(match { it.startsWith("rom/") }) }
    }

    @Test
    fun `writeLog devolve null quando o diretorio nao pode ser gravado`() {
        val missingDir = File(tempDir.root, "does-not-exist")

        assertNull(LogSaver.writeLog(contextWithRom(present = true), missingDir, at) { "" })
    }

    @Test
    fun `captura do log do sistema mantem as ultimas linhas da saida do comando`() {
        val logs = LogSaver.captureSystemLogs(arrayOf("sh", "-c", "seq 1 1005"))

        // The output ends in a newline, so its empty last line is one of the 1000 kept: 7..1005.
        assertEquals(LogReport.MAX_LOG_LINES, logs.lines().size)
        assertTrue(logs.startsWith("7\n"))
        assertTrue(logs.endsWith("\n1005\n"))
    }

    @Test
    fun `captura do log do sistema com comando inexistente explica a falha`() {
        val logs = LogSaver.captureSystemLogs(arrayOf("/nonexistent/command"))

        assertTrue(logs, logs.startsWith("Unable to capture system logs: "))
    }

    @Test
    fun `sem AppConfig inicializado a secao de configuracao diz que nao foi possivel ler`() {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig").apply { isAccessible = true }
        val original = field.get(null)
        field.set(null, null)
        try {
            val path = LogSaver.writeLog(contextWithRom(present = true), tempDir.root, at) { "" }

            assertTrue(File(checkNotNull(path)).readText().contains("Configuration: Unable to retrieve"))
        } finally {
            field.set(null, original)
        }
    }

    @Test
    fun `saveCompleteLog grava o relatorio na pasta de downloads`() {
        val path = checkNotNull(LogSaver.saveCompleteLog(appContext))
        try {
            val file = File(path)
            assertEquals(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    file.parentFile
            )
            assertTrue(file.readText().contains("Package Name: ${appContext.packageName}"))
        } finally {
            File(path).delete()
        }
    }
}
