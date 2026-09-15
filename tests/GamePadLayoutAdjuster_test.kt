package com.vinaooo.revenger.gamepad

import android.app.Application
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.GamePadAssetsConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GamePadLayoutAdjuster], extracted verbatim from
 * `GameActivity` (Task 9 of the split-god-classes refactor).
 *
 * These pin the exact pixel-math and branch decisions of the 5 moved methods. Written and
 * run against the already-extracted class rather than the pre-extraction `GameActivity`
 * copy: the move is a mechanical, self-contained lift (no Activity coupling, identical
 * bodies) so "written and passing immediately" against the new class is the characterization
 * baseline here, not a pre/post diff against `GameActivity`.
 *
 * `RevengerApplication.appConfig` is a `lateinit var` on a companion object, only populated by
 * `RevengerApplication.onCreate()` (which never runs for the default Robolectric test
 * Application) -- seeded via reflection the same way `InputViewModel_test` does, since
 * `GamePadLayoutAdjuster` reads it directly (per the extraction brief) rather than taking it
 * as a constructor dependency.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePadLayoutAdjuster_test {

    private lateinit var adjuster: GamePadLayoutAdjuster
    private lateinit var context: Application

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun seedAppConfig(portraitOffset: Int = 50, landscapeOffset: Int = 50) {
        val appConfig = mockk<AppConfig>()
        every { appConfig.gamePadConfigModel } returns
            GamePadAssetsConfig(
                gp_offset_portrait = portraitOffset,
                gp_offset_landscape = landscapeOffset,
            )
        setRevengerAppConfig(appConfig)
    }

    @Before
    fun setUp() {
        adjuster = GamePadLayoutAdjuster()
        context = ApplicationProvider.getApplicationContext()
        seedAppConfig()
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // --- test fixture helpers ---

    /** Measures + lays out a view at an exact pixel size, so .width/.height are real, not 0. */
    private fun sizeView(view: View, width: Int, height: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
    }

    /**
     * Builds the real view hierarchy `GameActivity`'s XML uses for the gamepad row: a
     * `FrameLayout` parent containing the `LinearLayout` gamepad container, which in turn
     * holds `left_container` / a center `View` / `right_container` (matches
     * `activity_game.xml`'s `containers` LinearLayout).
     */
    private fun buildHierarchy(
        parentHeight: Int,
        containerHeight: Int,
        leftHeight: Int = 50,
        rightHeight: Int = 50,
    ): Pair<FrameLayout, LinearLayout> {
        val parent = FrameLayout(context)
        val container = LinearLayout(context)
        container.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )

        val left = FrameLayout(context).apply { id = R.id.left_container }
        val center = View(context)
        val right = FrameLayout(context).apply { id = R.id.right_container }

        left.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
        center.layoutParams = LinearLayout.LayoutParams(0, 0, 0.5f)
        right.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)

        container.addView(left)
        container.addView(center)
        container.addView(right)
        parent.addView(container)

        sizeView(parent, 1080, parentHeight)
        sizeView(container, 1080, containerHeight)
        sizeView(left, 300, leftHeight)
        sizeView(right, 300, rightHeight)

        return parent to container
    }

    // --- reflection helpers for the 4 private methods ---

    private fun callApplyPortraitOffset(container: LinearLayout) {
        val m = GamePadLayoutAdjuster::class.java.getDeclaredMethod("applyPortraitOffset", LinearLayout::class.java)
        m.isAccessible = true
        m.invoke(adjuster, container)
    }

    private fun callApplyLandscapeOffset(container: LinearLayout) {
        val m = GamePadLayoutAdjuster::class.java.getDeclaredMethod("applyLandscapeOffset", LinearLayout::class.java)
        m.isAccessible = true
        m.invoke(adjuster, container)
    }

    private fun callEqualizeGamePadHeights(container: LinearLayout) {
        val m = GamePadLayoutAdjuster::class.java.getDeclaredMethod("equalizeGamePadHeights", LinearLayout::class.java)
        m.isAccessible = true
        m.invoke(adjuster, container)
    }

    private fun callAdjustGamePadSizes(container: LinearLayout, gamePadWeight: Float, centerWeight: Float) {
        val m =
            GamePadLayoutAdjuster::class.java.getDeclaredMethod(
                "adjustGamePadSizes",
                LinearLayout::class.java,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            )
        m.isAccessible = true
        m.invoke(adjuster, container, gamePadWeight, centerWeight)
    }

    private fun leftContainerOf(container: LinearLayout) =
        container.findViewById<FrameLayout>(R.id.left_container)

    private fun rightContainerOf(container: LinearLayout) =
        container.findViewById<FrameLayout>(R.id.right_container)

    // --- applyPortraitOffset / applyLandscapeOffset: exact formula ---

    @Test
    fun `applyPortraitOffset calcula bottomMargin exatamente pela formula maxMovement vezes 100 menos offset sobre 100`() {
        seedAppConfig(portraitOffset = 30)
        // parent height 1000, container height 400 -> maxMovement = 600
        // bottomMargin = (600 * (100 - 30) / 100.0).toInt() = 420
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 400)

        callApplyPortraitOffset(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(420, params.bottomMargin)
    }

    @Test
    fun `applyLandscapeOffset calcula topMargin exatamente pela formula maxMovement vezes offset sobre 100`() {
        seedAppConfig(landscapeOffset = 25)
        // parent height 1000, container height 400 -> maxMovement = 600
        // topMargin = (600 * 25 / 100.0).toInt() = 150
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 400)

        callApplyLandscapeOffset(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(150, params.topMargin)
    }

    @Test
    fun `applyPortraitOffset nao faz nada quando a altura do container ainda e 0`() {
        seedAppConfig(portraitOffset = 30)
        // containerHeight = 0 (not yet measured/laid out by the real window) -> guard returns early
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 0)

        callApplyPortraitOffset(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(0, params.bottomMargin)
    }

    @Test
    fun `applyPortraitOffset nao faz nada quando a altura do parent ainda e 0`() {
        seedAppConfig(portraitOffset = 30)
        val (parent, container) = buildHierarchy(parentHeight = 0, containerHeight = 400)
        // Re-affirm parent's height is genuinely 0 for this scenario.
        assertEquals(0, parent.height)

        callApplyPortraitOffset(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(0, params.bottomMargin)
    }

    @Test
    fun `applyLandscapeOffset nao faz nada quando a altura do container ainda e 0`() {
        seedAppConfig(landscapeOffset = 25)
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 0)

        callApplyLandscapeOffset(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(0, params.topMargin)
    }

    // --- equalizeGamePadHeights ---

    @Test
    fun `equalizeGamePadHeights ajusta minimumHeight do lado mais curto para igualar o mais alto`() {
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 150, leftHeight = 100, rightHeight = 150)

        callEqualizeGamePadHeights(container)

        assertEquals(150, leftContainerOf(container).minimumHeight)
        // Right side was already the tallest -> untouched (stays at the View default, 0).
        assertEquals(0, rightContainerOf(container).minimumHeight)
    }

    @Test
    fun `equalizeGamePadHeights nao faz nada quando os dois lados ja tem a mesma altura`() {
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 120, leftHeight = 120, rightHeight = 120)

        callEqualizeGamePadHeights(container)

        assertEquals(0, leftContainerOf(container).minimumHeight)
        assertEquals(0, rightContainerOf(container).minimumHeight)
    }

    // --- adjustGamePadSizes ---

    @Test
    fun `adjustGamePadSizes aplica os pesos exatos recebidos em left, right e no view central`() {
        val (_, container) = buildHierarchy(parentHeight = 1000, containerHeight = 150)

        callAdjustGamePadSizes(container, gamePadWeight = 0.40f, centerWeight = 0.2f)

        val leftParams = leftContainerOf(container).layoutParams as LinearLayout.LayoutParams
        val rightParams = rightContainerOf(container).layoutParams as LinearLayout.LayoutParams
        val centerParams = container.getChildAt(1).layoutParams as LinearLayout.LayoutParams

        assertEquals(0.40f, leftParams.weight)
        assertEquals(0.40f, rightParams.weight)
        assertEquals(0.2f, centerParams.weight)
    }

    // --- adjustPositionForOrientation: stale-state reset + branch selection ---

    @Test
    fun `adjustPositionForOrientation zera margens antigas e minimumHeight dos containers antes de reaplicar`() {
        RuntimeEnvironment.setQualifiers("+port")
        val (_, container) = buildHierarchy(parentHeight = 0, containerHeight = 0)
        // Simulate leftover state from a previous orientation, applied by a prior call.
        (container.layoutParams as FrameLayout.LayoutParams).apply {
            bottomMargin = 500
            topMargin = 300
        }
        leftContainerOf(container).minimumHeight = 200
        rightContainerOf(container).minimumHeight = 200

        adjuster.adjustPositionForOrientation(container)

        // This reset happens synchronously at the top of the method, before the orientation
        // branch and before the post{}-deferred re-application -- so it's observable immediately.
        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(0, params.topMargin)
        assertEquals(0, params.bottomMargin)
        assertEquals(0, leftContainerOf(container).minimumHeight)
        assertEquals(0, rightContainerOf(container).minimumHeight)

        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `adjustPositionForOrientation em portrait usa gravity BOTTOM e pesos 0,40 e 0,2`() {
        RuntimeEnvironment.setQualifiers("+port")
        val (_, container) = buildHierarchy(parentHeight = 0, containerHeight = 0)
        assertEquals(
            android.content.res.Configuration.ORIENTATION_PORTRAIT,
            container.resources.configuration.orientation,
        )

        adjuster.adjustPositionForOrientation(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM, params.gravity)
        assertEquals(0.40f, (leftContainerOf(container).layoutParams as LinearLayout.LayoutParams).weight)
        assertEquals(0.2f, (container.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight)

        // The height-equalization/offset calls are deferred via container.post{}; confirm they
        // can run (with heights still 0, both hit their own no-op guards) without crashing.
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `adjustPositionForOrientation em landscape usa gravity TOP e pesos 0,25 e 0,5`() {
        RuntimeEnvironment.setQualifiers("+land")
        val (_, container) = buildHierarchy(parentHeight = 0, containerHeight = 0)
        assertNotEquals(
            android.content.res.Configuration.ORIENTATION_PORTRAIT,
            container.resources.configuration.orientation,
        )

        adjuster.adjustPositionForOrientation(container)

        val params = container.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP, params.gravity)
        assertEquals(0.25f, (leftContainerOf(container).layoutParams as LinearLayout.LayoutParams).weight)
        assertEquals(0.5f, (container.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight)

        shadowOf(Looper.getMainLooper()).idle()
    }
}
