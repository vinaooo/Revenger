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
        // simply exercise calculateInset signature
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.TOP,
            GameScreenInsetConfig.CameraSide.TOP,
            cameraPct = 5,
            alignPct = 3
        )
        assertEquals(GameScreenInsetConfig.Inset(3, 0, 5, 0), inset) // top align + camera top
    }

    @Test
    fun `camera margin on different sides`() {
        val pct = 10
        val topInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.CENTER,
            GameScreenInsetConfig.CameraSide.TOP,
            cameraPct = pct,
            alignPct = 0
        )
        assertEquals(GameScreenInsetConfig.Inset(10, 0, 0, 0), topInset)

        val rightInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.CENTER,
            GameScreenInsetConfig.CameraSide.RIGHT,
            cameraPct = pct,
            alignPct = 0
        )
        assertEquals(GameScreenInsetConfig.Inset(0, 10, 0, 0), rightInset)
    }

    @Test
    fun `alignment alone moves when offset provided`() {
        val offset = 12
        val leftInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.LEFT,
            GameScreenInsetConfig.AlignV.CENTER,
            GameScreenInsetConfig.CameraSide.TOP, // camera aligns top but cameraPct zero
            cameraPct = 0,
            alignPct = offset
        )
        assertEquals(GameScreenInsetConfig.Inset(0, 0, 0, 12), leftInset)

        val bottomInset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.CENTER,
            GameScreenInsetConfig.AlignV.BOTTOM,
            GameScreenInsetConfig.CameraSide.LEFT,
            cameraPct = 0,
            alignPct = offset
        )
        assertEquals(GameScreenInsetConfig.Inset(0, 0, 12, 0), bottomInset)
    }

    @Test
    fun `alignment uses default offset when none specified`() {
        // ZERO alignPct but non-center alignment should use DEFAULT_ALIGN_PCT
        val default = 10
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.RIGHT,
            GameScreenInsetConfig.AlignV.TOP,
            GameScreenInsetConfig.CameraSide.LEFT,
            cameraPct = 0,
            alignPct = 0
        )
        assertEquals(GameScreenInsetConfig.Inset(default, 0, 0, 0), inset)
    }

    @Test
    fun `alignment and camera add together`() {
        val pct = 7
        val offset = 5
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.LEFT,
            GameScreenInsetConfig.AlignV.BOTTOM,
            GameScreenInsetConfig.CameraSide.BOTTOM,
            cameraPct = pct,
            alignPct = offset
        )
        // bottom from both align (vAlign bottom) and camera side bottom -> 12
        assertEquals(GameScreenInsetConfig.Inset(0, 0, 12, 0), inset)
    }

    @Test
    fun `clamping still works with new calculator`() {
        val inset = GameScreenInsetConfig.calculateInset(
            GameScreenInsetConfig.AlignH.RIGHT,
            GameScreenInsetConfig.AlignV.TOP,
            GameScreenInsetConfig.CameraSide.RIGHT,
            cameraPct = 80,
            alignPct = 80
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
