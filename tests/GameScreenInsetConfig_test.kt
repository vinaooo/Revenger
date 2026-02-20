package com.vinaooo.revenger.config

import android.graphics.RectF
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class GameScreenInsetConfigTest {
    @Test
    fun `legacy numeric parser still works`() {
        assertEquals(GameScreenInsetConfig.Inset(0, 0, 0, 0),
            GameScreenInsetConfig.parseInset("0"))
        assertEquals(GameScreenInsetConfig.Inset(5, 5, 5, 5),
            GameScreenInsetConfig.parseInset("5"))
    }

    @Test
    fun `alignment enums parse correctly`() {
        assertEquals(GameScreenInsetConfig.AlignH.LEFT,
            GameScreenInsetConfig::class.java.getDeclaredMethod("parseAlignH", String::class.java)
                .apply { isAccessible = true }
                .invoke(null, "left"))
        // rather than reflect, just call public via resources overload? We can test calculateInset directly
        // simpler: just ensure calculateInset handles enums
        assertEquals(GameScreenInsetConfig.Inset(5, 0, 0, 0),
            GameScreenInsetConfig.calculateInset(
                GameScreenInsetConfig.AlignH.CENTER,
                GameScreenInsetConfig.AlignV.TOP,
                GameScreenInsetConfig.CameraSide.TOP,
                5
            ))
    }

    @Test
    fun `camera margin on different sides`() {
        val pct = 10
        val topInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.CENTER,
            GameScreenInsetConfig.CameraSide.TOP,
            pct
        )
        assertEquals(GameScreenInsetConfig.Inset(10, 0, 0, 0), topInset)

        val rightInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.CENTER,
            GameScreenInsetConfig.CameraSide.RIGHT,
            pct
        )
        assertEquals(GameScreenInsetConfig.Inset(0, 10, 0, 0), rightInset)
    }

    @Test
    fun `alignment and camera add together`() {
        val pct = 7
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.LEFT,
            GameScreenInsetConfig.AlignV.BOTTOM,
            GameScreenInsetConfig.CameraSide.BOTTOM,
            pct
        )
        // bottom from both align (vAlign bottom) and camera side bottom -> 14
        assertEquals(GameScreenInsetConfig.Inset(0, 0, 14, 0), inset)
    }

    @Test
    fun `clamping still works with new calculator`() {
        // specify huge pct that would overflow each side
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.RIGHT,
            GameScreenInsetConfig.AlignV.TOP,
            GameScreenInsetConfig.CameraSide.RIGHT,
            80
        )
        assertTrue(inset.isValid())
        assertTrue(inset.left <= 99 && inset.top <= 99)
    }

    @Test
    fun `viewport conversion remains correct`() {
        val inset = GameScreenInsetConfig.Inset(10, 10, 10, 10)
        val viewport = GameScreenInsetConfig.insetToViewport(inset)
        assertEquals(RectF(0.10f, 0.10f, 0.80f, 0.80f), viewport)
    }
}
