package com.vinaooo.revenger

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Direct unit tests for [AppConfigMenuModeImpl] -- the only one of [AppConfig]'s domain delegates
 * with real parsing logic (the `menu_mode` comma-string), as opposed to a plain pass-through
 * getter. [AppConfig_test] already covers this behavior through the facade; these tests pin the
 * delegate's own contract in isolation.
 */
class AppConfigMenuMode_test {

    private val assetContents = mutableMapOf<String, String>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        assetContents.clear()

        val assetManager = mockk<AssetManager>()
        every { assetManager.open(any()) } answers {
            val path = firstArg<String>()
            val json = assetContents[path] ?: throw FileNotFoundException(path)
            ByteArrayInputStream(json.toByteArray())
        }

        context = mockk()
        every { context.assets } returns assetManager
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun menuModeImpl(menuMode: String): AppConfigMenuMode {
        assetContents["config/config_manual.json"] =
            """{"core": "test_core", "menu_mode": "$menuMode"}"""
        return AppConfigMenuModeImpl(ConfigSources(context))
    }

    @Test
    fun `menu_mode vazio nao ativa nenhuma flag e fab fica vazio`() {
        val menu = menuModeImpl("")

        assertEquals("", menu.getMenuModeFab())
        assertFalse(menu.getMenuModeGamepad())
        assertFalse(menu.getMenuModeBack())
        assertFalse(menu.getMenuModeCombo())
    }

    @Test
    fun `menu_mode com todas as flags e fab combinados`() {
        val menu = menuModeImpl("combo,gamepad,back,fab=bottom-right")

        assertEquals("bottom-right", menu.getMenuModeFab())
        assertTrue(menu.getMenuModeGamepad())
        assertTrue(menu.getMenuModeBack())
        assertTrue(menu.getMenuModeCombo())
    }

    @Test
    fun `menu_mode ignora espacos ao redor de cada token`() {
        val menu = menuModeImpl(" combo , gamepad , back ")

        assertTrue(menu.getMenuModeGamepad())
        assertTrue(menu.getMenuModeBack())
        assertTrue(menu.getMenuModeCombo())
    }
}
