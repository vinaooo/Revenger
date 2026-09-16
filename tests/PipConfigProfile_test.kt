package com.vinaooo.revenger.models

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipConfigProfile_test {

    @Test
    fun `fromJson mapeia platformId externo e a proporcao do JSON`() {
        val json = JSONObject().apply {
            put("ratio_w", 16)
            put("ratio_h", 9)
        }

        val profile = PipConfigProfile.fromJson("platform_a", json)

        assertEquals("platform_a", profile.platformId)
        assertEquals(16, profile.ratioW)
        assertEquals(9, profile.ratioH)
    }

    @Test
    fun `fromJson com proporcao quadrada`() {
        val json = JSONObject().apply {
            put("ratio_w", 1)
            put("ratio_h", 1)
        }

        val profile = PipConfigProfile.fromJson("platform_b", json)

        assertEquals(1, profile.ratioW)
        assertEquals(1, profile.ratioH)
    }

    @Test
    fun `duas instancias com os mesmos valores sao iguais`() {
        val json = JSONObject().apply {
            put("ratio_w", 4)
            put("ratio_h", 3)
        }

        val first = PipConfigProfile.fromJson("platform_a", json)
        val second = PipConfigProfile.fromJson("platform_a", json)

        assertEquals(first, second)
    }
}
