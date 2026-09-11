package com.vinaooo.revenger.models

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DefaultSettingsProfile_test {

    private fun completeProfileJson(enablePip: Boolean? = true): JSONObject {
        val json =
            JSONObject().apply {
                put("platform_id", "platform_a")
                put("extensions", JSONArray(listOf(".rom", ".bin")))
                put("core", "test_core")
                put("variables", "key=value")
                put("fast_forward_multiplier", 4)
                put("fullscreen", true)
                put("orientation", "landscape")
                put("menu_mode", "combo,gamepad")
                put("gamepad", true)
                put("gp_haptic", false)
                put("button_allow_multiple_presses_action", false)
                put("button_a", true)
                put("button_b", true)
                put("button_x", false)
                put("button_y", false)
                put("button_start", true)
                put("button_select", true)
                put("button_l1", false)
                put("button_r1", false)
                put("button_l2", false)
                put("button_r2", false)
                put("left_analog", false)
                put("shader", "crt")
                put("performance_overlay", false)
            }
        if (enablePip != null) json.put("enable_pip", enablePip)
        return json
    }

    @Test
    fun `fromJson mapeia todos os campos corretamente`() {
        val profile = DefaultSettingsProfile.fromJson(completeProfileJson())

        assertEquals("platform_a", profile.platformId)
        assertEquals(listOf(".rom", ".bin"), profile.extensions)
        assertEquals("test_core", profile.core)
        assertEquals("key=value", profile.confVariables)
        assertEquals(4, profile.confFastForwardMultiplier)
        assertTrue(profile.confFullscreen)
        assertEquals("landscape", profile.confOrientation)
        assertEquals("combo,gamepad", profile.confMenuMode)
        assertTrue(profile.confGamepad)
        assertFalse(profile.confGpHaptic)
        assertEquals("crt", profile.confShader)
        assertFalse(profile.confPerformanceOverlay)
    }

    @Test
    fun `fromJson usa enable_pip explicito quando presente`() {
        val profile = DefaultSettingsProfile.fromJson(completeProfileJson(enablePip = false))

        assertFalse(profile.confEnablePip)
    }

    @Test
    fun `fromJson usa true como padrao para enable_pip quando ausente`() {
        val profile = DefaultSettingsProfile.fromJson(completeProfileJson(enablePip = null))

        assertTrue(profile.confEnablePip)
    }

    @Test
    fun `parseProfiles converte um array JSON em uma lista na mesma ordem`() {
        val array =
            JSONArray().apply {
                put(
                    JSONObject(completeProfileJson().toString()).apply {
                        put("platform_id", "platform_a")
                    }
                )
                put(
                    JSONObject(completeProfileJson().toString()).apply {
                        put("platform_id", "platform_b")
                    }
                )
            }

        val profiles = DefaultSettingsProfile.parseProfiles(array)

        assertEquals(2, profiles.size)
        assertEquals("platform_a", profiles[0].platformId)
        assertEquals("platform_b", profiles[1].platformId)
    }

    @Test
    fun `parseProfiles com array vazio retorna lista vazia`() {
        assertTrue(DefaultSettingsProfile.parseProfiles(JSONArray()).isEmpty())
    }
}
