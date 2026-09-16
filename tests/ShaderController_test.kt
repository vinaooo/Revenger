package com.vinaooo.revenger.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ShaderController_test {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun newController(configShader: String = "disabled"): ShaderController {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getShader() } returns configShader
        return ShaderController(context, prefs, appConfig)
    }

    // --- Resolução do shader inicial ---

    @Test
    fun `sem preferencia salva usa o shader configurado no AppConfig como padrao`() {
        val controller = newController(configShader = "crt")

        assertEquals("crt", controller.getCurrentShader())
    }

    @Test
    fun `sem preferencia salva e com shader invalido no AppConfig cai para disabled`() {
        val controller = newController(configShader = "shader_que_nao_existe")

        assertEquals("disabled", controller.getCurrentShader())
    }

    @Test
    fun `com preferencia ja salva ela tem prioridade sobre o AppConfig`() {
        prefs.edit().putString("current_shader", "lcd").commit()

        val controller = newController(configShader = "crt")

        assertEquals("lcd", controller.getCurrentShader())
    }

    // --- setShader ---

    @Test
    fun `setShader valido atualiza o estado, persiste e aplica no retroView conectado`() {
        val controller = newController()
        val retroView = mockk<RetroView>(relaxed = true)
        controller.connect(retroView)

        controller.setShader("crt")

        assertEquals("crt", controller.getCurrentShader())
        assertEquals("crt", prefs.getString("current_shader", null))
        verify { retroView.dynamicShader = "crt" }
    }

    @Test
    fun `setShader invalido cai para o shader default`() {
        val controller = newController()

        controller.setShader("shader_invalido")

        assertEquals("disabled", controller.getCurrentShader())
        assertEquals("disabled", prefs.getString("current_shader", null))
    }

    @Test
    fun `setShader sem retroView conectado nao lanca excecao`() {
        val controller = newController()

        controller.setShader("crt")

        assertEquals("crt", controller.getCurrentShader())
    }

    // --- cycleShader ---

    @Test
    fun `cycleShader avanca para o proximo shader disponivel`() {
        val controller = newController(configShader = "disabled")

        val next = controller.cycleShader()

        assertEquals("sharp", next)
        assertEquals("sharp", controller.getCurrentShader())
    }

    @Test
    fun `cycleShader da a volta ao passar do ultimo shader disponivel`() {
        val controller = newController()
        controller.setShader(controller.availableShaders.last())

        val next = controller.cycleShader()

        assertEquals(controller.availableShaders.first(), next)
    }

    // --- connect ---

    @Test
    fun `connect aplica o shader atual imediatamente no retroView`() {
        val controller = newController(configShader = "lcd")
        val retroView = mockk<RetroView>(relaxed = true)

        controller.connect(retroView)

        verify { retroView.dynamicShader = "lcd" }
    }

    // --- Nome de exibição ---

    @Test
    fun `getCurrentShaderDisplayName mapeia o shader atual para o nome legivel`() {
        val controller = newController()

        controller.setShader("upscale2")

        assertEquals("Upscale 2", controller.getCurrentShaderDisplayName())
    }
}
