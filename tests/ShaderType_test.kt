package com.vinaooo.revenger.utils

import com.swordfish.libretrodroid.ShaderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShaderType_test {

    @Test
    fun `fromConfigName encontra cada tipo pelo proprio configName`() {
        ShaderType.entries.forEach { type -> assertEquals(type, ShaderType.fromConfigName(type.configName)) }
    }

    @Test
    fun `fromConfigName ignora maiusculas e minusculas`() {
        assertEquals(ShaderType.CRT, ShaderType.fromConfigName("CRT"))
        assertEquals(ShaderType.UPSCALE2, ShaderType.fromConfigName("UpScale2"))
    }

    @Test
    fun `fromConfigName devolve null para um nome desconhecido`() {
        assertNull(ShaderType.fromConfigName("shader_que_nao_existe"))
        assertNull(ShaderType.fromConfigName(""))
    }

    @Test
    fun `toShaderConfig mapeia cada tipo para o ShaderConfig do LibretroDroid`() {
        val expected =
                mapOf(
                        ShaderType.DISABLED to ShaderConfig.Default,
                        ShaderType.SHARP to ShaderConfig.Sharp,
                        ShaderType.CRT to ShaderConfig.CRT,
                        ShaderType.LCD to ShaderConfig.LCD,
                        ShaderType.UPSCALE1 to ShaderConfig.CUT(),
                        ShaderType.UPSCALE2 to ShaderConfig.CUT2(),
                        ShaderType.UPSCALE3 to ShaderConfig.CUT3()
                )

        assertEquals(ShaderType.entries.toSet(), expected.keys)
        expected.forEach { (type, config) -> assertEquals(config, type.toShaderConfig()) }
    }

    @Test
    fun `nomes de exibicao dos upscales`() {
        assertEquals(
                listOf("Upscale 1", "Upscale 2", "Upscale 3"),
                listOf(ShaderType.UPSCALE1, ShaderType.UPSCALE2, ShaderType.UPSCALE3).map { it.displayName }
        )
    }
}
