package com.vinaooo.revenger.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantsTest {

    @Test
    fun testMenuConstants() {
        assertEquals(200L, MenuConstants.ANIMATION_DURATION_MS)
        assertEquals(0.8f, MenuConstants.SCALE_ANIMATION_FACTOR)
        assertEquals(0f, MenuConstants.ALPHA_ANIMATION_START)
        assertEquals(1f, MenuConstants.ALPHA_ANIMATION_END)
    }

    @Test
    fun testMenuModeValues() {
        assertEquals(0, MenuMode.DISABLED.value)
        assertEquals(1, MenuMode.BACK_BUTTON_ONLY.value)
        assertEquals(2, MenuMode.SELECT_START_COMBO_ONLY.value)
        assertEquals(3, MenuMode.BOTH_BACK_AND_COMBO.value)
    }

    @Test
    fun testMenuModeFromValue() {
        assertEquals(MenuMode.DISABLED, MenuMode.fromValue(0))
        assertEquals(MenuMode.BACK_BUTTON_ONLY, MenuMode.fromValue(1))
        assertEquals(MenuMode.SELECT_START_COMBO_ONLY, MenuMode.fromValue(2))
        assertEquals(MenuMode.BOTH_BACK_AND_COMBO, MenuMode.fromValue(3))
        assertEquals(MenuMode.DISABLED, MenuMode.fromValue(999)) // Default fallback
    }

    @Test
    fun testShaderTypeValues() {
        assertEquals("Disabled", ShaderType.DISABLED.displayName)
        assertEquals("disabled", ShaderType.DISABLED.configName)
        assertEquals("Sharp", ShaderType.SHARP.displayName)
        assertEquals("sharp", ShaderType.SHARP.configName)
        assertEquals("CRT", ShaderType.CRT.displayName)
        assertEquals("crt", ShaderType.CRT.configName)
        assertEquals("LCD", ShaderType.LCD.displayName)
        assertEquals("lcd", ShaderType.LCD.configName)
    }

    @Test
    fun testShaderTypeFromName() {
        assertEquals(ShaderType.DISABLED, ShaderType.fromName("disabled"))
        assertEquals(ShaderType.SHARP, ShaderType.fromName("sharp"))
        assertEquals(ShaderType.CRT, ShaderType.fromName("crt"))
        assertEquals(ShaderType.LCD, ShaderType.fromName("lcd"))
        assertEquals(ShaderType.SHARP, ShaderType.fromName("invalid")) // Default fallback
        assertEquals(ShaderType.SHARP, ShaderType.fromName("SHARP")) // Case insensitive
    }

    @Test
    fun testShaderTypeFromDisplayName() {
        assertEquals(ShaderType.DISABLED, ShaderType.fromDisplayName("Disabled"))
        assertEquals(ShaderType.SHARP, ShaderType.fromDisplayName("Sharp"))
        assertEquals(ShaderType.CRT, ShaderType.fromDisplayName("CRT"))
        assertEquals(ShaderType.LCD, ShaderType.fromDisplayName("LCD"))
        assertEquals(ShaderType.SHARP, ShaderType.fromDisplayName("invalid")) // Default fallback
    }
}
