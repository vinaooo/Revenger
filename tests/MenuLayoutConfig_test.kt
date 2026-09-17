package com.vinaooo.revenger.ui.retromenu3.config

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [MenuLayoutConfig], the `object` that parses "XXYYZZ" layout-proportion strings and
 * applies them to menu view trees. Had zero test coverage before this file.
 *
 * Follows the [com.vinaooo.revenger.models.PipConfigProfile_test] pattern of testing a pure
 * parsing `object` directly, plus Robolectric-backed structural tests for the view-mutating
 * functions (which need real [android.widget.LinearLayout]/[android.view.View] instances).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuLayoutConfig_test {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // ========== parseLayoutProportions ==========

    @Test
    fun `parseLayoutProportions com string valida retorna pesos normalizados`() {
        val proportions = MenuLayoutConfig.parseLayoutProportions("108010")

        assertNotNull(proportions)
        assertEquals(0.10f, proportions!!.leftWeight, 0.0001f)
        assertEquals(0.80f, proportions.centerWeight, 0.0001f)
        assertEquals(0.10f, proportions.rightWeight, 0.0001f)
    }

    @Test
    fun `parseLayoutProportions com comprimento diferente de 6 retorna null`() {
        assertNull(MenuLayoutConfig.parseLayoutProportions("1080"))
        assertNull(MenuLayoutConfig.parseLayoutProportions("10801020"))
        assertNull(MenuLayoutConfig.parseLayoutProportions(""))
    }

    @Test
    fun `parseLayoutProportions com soma diferente de 100 retorna null`() {
        assertNull(MenuLayoutConfig.parseLayoutProportions("109010")) // 10+90+10=110
        assertNull(MenuLayoutConfig.parseLayoutProportions("100000")) // 10+00+00=10
    }

    @Test
    fun `parseLayoutProportions com caracteres nao numericos retorna null`() {
        assertNull(MenuLayoutConfig.parseLayoutProportions("abcdef"))
    }

    @Test
    fun `parseLayoutProportions aceita string com soma exatamente 100`() {
        val proportions = MenuLayoutConfig.parseLayoutProportions("257525")
        // 25 + 75 + 25 = 125, invalid on purpose to contrast with a valid one below
        assertNull(proportions)

        val valid = MenuLayoutConfig.parseLayoutProportions("255025")
        assertNotNull(valid)
        assertEquals(0.25f, valid!!.leftWeight, 0.0001f)
        assertEquals(0.50f, valid.centerWeight, 0.0001f)
        assertEquals(0.25f, valid.rightWeight, 0.0001f)
    }

    // ========== parseVerticalProportions ==========

    @Test
    fun `parseVerticalProportions com string valida retorna pesos normalizados`() {
        val proportions = MenuLayoutConfig.parseVerticalProportions("304030")

        assertNotNull(proportions)
        assertEquals(0.30f, proportions!!.topWeight, 0.0001f)
        assertEquals(0.40f, proportions.contentWeight, 0.0001f)
        assertEquals(0.30f, proportions.bottomWeight, 0.0001f)
    }

    @Test
    fun `parseVerticalProportions com comprimento invalido retorna null`() {
        assertNull(MenuLayoutConfig.parseVerticalProportions("3040"))
    }

    @Test
    fun `parseVerticalProportions com soma diferente de 100 retorna null`() {
        assertNull(MenuLayoutConfig.parseVerticalProportions("500001")) // 50+00+01=51
    }

    @Test
    fun `parseVerticalProportions com caracteres nao numericos retorna null`() {
        assertNull(MenuLayoutConfig.parseVerticalProportions("xx4030"))
    }

    // ========== toString() of the data classes (documented contract) ==========

    @Test
    fun `LayoutProportions toString formata como percentuais inteiros`() {
        val proportions = MenuLayoutConfig.parseLayoutProportions("108010")!!
        assertEquals("LayoutProportions(left=10%, center=80%, right=10%)", proportions.toString())
    }

    @Test
    fun `VerticalProportions toString formata como percentuais inteiros`() {
        val proportions = MenuLayoutConfig.parseVerticalProportions("304030")!!
        assertEquals(
                "VerticalProportions(top=30%, content=40%, bottom=30%)",
                proportions.toString()
        )
    }

    // ========== applyLayoutProportions ==========

    private fun spacerChild(): View =
            Space(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0f)
            }

    @Test
    fun `applyLayoutProportions aplica pesos aos 3 primeiros filhos`() {
        val parent =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(spacerChild())
                    addView(spacerChild())
                    addView(spacerChild())
                }

        val proportions = MenuLayoutConfig.parseLayoutProportions("108010")!!
        MenuLayoutConfig.applyLayoutProportions(parent, proportions)

        assertEquals(0.10f, (parent.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.80f, (parent.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.10f, (parent.getChildAt(2).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    @Test
    fun `applyLayoutProportions com menos de 3 filhos nao altera nada e nao lanca excecao`() {
        val parent =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(spacerChild())
                    addView(spacerChild())
                }

        val proportions = MenuLayoutConfig.parseLayoutProportions("108010")!!
        MenuLayoutConfig.applyLayoutProportions(parent, proportions)

        assertEquals(0f, (parent.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0f, (parent.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    // ========== getConfiguredProportions / getConfiguredVerticalProportions ==========

    @Test
    fun `getConfiguredProportions em portrait usa rm_portrait_horizontal_proportions`() {
        val anyView = View(context)
        // Robolectric's default device configuration is portrait.
        assertEquals(
                android.content.res.Configuration.ORIENTATION_PORTRAIT,
                anyView.resources.configuration.orientation
        )

        val expected =
                MenuLayoutConfig.parseLayoutProportions(
                        context.getString(R.string.rm_portrait_horizontal_proportions)
                )
        val actual = MenuLayoutConfig.getConfiguredProportions(anyView)

        assertEquals(expected, actual)
    }

    @Test
    fun `getConfiguredVerticalProportions em portrait usa rm_portrait_vertical_proportions`() {
        val anyView = View(context)

        val expected =
                MenuLayoutConfig.parseVerticalProportions(
                        context.getString(R.string.rm_portrait_vertical_proportions)
                )
        val actual = MenuLayoutConfig.getConfiguredVerticalProportions(anyView)

        assertEquals(expected, actual)
    }

    // ========== applyProportionsToMenuLayout (finds the horizontal 3-column layout) ==========

    @Test
    fun `applyProportionsToMenuLayout encontra o LinearLayout horizontal com 3+ filhos e aplica pesos`() {
        val root = FrameLayout(context)

        // A vertical LinearLayout that should be ignored (wrong orientation).
        val verticalDecoy =
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(spacerChild())
                    addView(spacerChild())
                    addView(spacerChild())
                }
        root.addView(verticalDecoy)

        // The real horizontal 3-column row that should receive the proportions.
        val mainRow =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(spacerChild())
                    addView(spacerChild())
                    addView(spacerChild())
                }
        root.addView(mainRow)

        MenuLayoutConfig.applyProportionsToMenuLayout(root)

        // Portrait default proportions: 10/80/10
        assertEquals(0.10f, (mainRow.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.80f, (mainRow.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.10f, (mainRow.getChildAt(2).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)

        // The decoy must be untouched.
        assertEquals(0f, (verticalDecoy.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    @Test
    fun `applyProportionsToMenuLayout ignora LinearLayout horizontal com menos de 3 filhos`() {
        val root = FrameLayout(context)

        // Horizontal, but only 2 children — must be skipped (MIN_LAYOUT_CHILD_COUNT = 3).
        val tooFewChildren =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(spacerChild())
                    addView(spacerChild())
                }
        root.addView(tooFewChildren)

        // The real candidate, with 3 children.
        val mainRow =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(spacerChild())
                    addView(spacerChild())
                    addView(spacerChild())
                }
        root.addView(mainRow)

        MenuLayoutConfig.applyProportionsToMenuLayout(root)

        // Portrait default proportions: 10/80/10, applied to mainRow, not to tooFewChildren.
        assertEquals(0.10f, (mainRow.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.80f, (mainRow.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0f, (tooFewChildren.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0f, (tooFewChildren.getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    @Test
    fun `applyProportionsToMenuLayout sem LinearLayout horizontal candidato nao lanca excecao`() {
        val root = FrameLayout(context)
        try {
            MenuLayoutConfig.applyProportionsToMenuLayout(root)
        } catch (e: Exception) {
            fail("applyProportionsToMenuLayout should not throw when no candidate layout exists: ${e.message}")
        }
    }

    // ========== applyVerticalProportions (wraps the content container vertically) ==========

    @Test
    fun `applyVerticalProportions envolve o container em um LinearLayout vertical preservando o peso original`() {
        val parentRow =
                LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        val leftSpace = spacerChild()
        val menuContainer =
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    id = R.id.menu_container
                    layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.80f)
                }
        val rightSpace = spacerChild()

        parentRow.addView(leftSpace)
        parentRow.addView(menuContainer)
        parentRow.addView(rightSpace)

        val verticalProportions = MenuLayoutConfig.parseVerticalProportions("304030")!!
        MenuLayoutConfig.applyVerticalProportions(menuContainer, verticalProportions)

        // Same slot in the parent, still 3 children.
        assertEquals(3, parentRow.childCount)
        val wrapper = parentRow.getChildAt(1)
        assertTrue("Expected a LinearLayout wrapper at the original index", wrapper is LinearLayout)
        wrapper as LinearLayout
        assertEquals(LinearLayout.VERTICAL, wrapper.orientation)
        // Original horizontal weight (0.80) must be preserved on the wrapper.
        assertEquals(0.80f, (wrapper.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)

        // Inside the wrapper: [topSpace, menuContainer, bottomSpace] with the vertical weights.
        assertEquals(3, wrapper.childCount)
        assertSame(menuContainer, wrapper.getChildAt(1))
        assertEquals(0.30f, (wrapper.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.40f, (menuContainer.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.30f, (wrapper.getChildAt(2).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)

        // menuContainer's parent is now the wrapper, not the original row.
        assertSame(wrapper, menuContainer.parent)
    }

    @Test
    fun `applyVerticalProportions com parent nao horizontal nao lanca excecao e nao modifica a arvore`() {
        val verticalParent =
                LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val menuContainer =
                LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
        verticalParent.addView(menuContainer)

        val verticalProportions = MenuLayoutConfig.parseVerticalProportions("304030")!!
        try {
            MenuLayoutConfig.applyVerticalProportions(menuContainer, verticalProportions)
        } catch (e: Exception) {
            fail("Should not throw when parent is not horizontal: ${e.message}")
        }

        assertEquals(1, verticalParent.childCount)
        assertSame(menuContainer, verticalParent.getChildAt(0))
    }

    // ========== applyAllProportionsToMenuLayout (horizontal + vertical, end to end) ==========

    @Test
    fun `applyAllProportionsToMenuLayout aplica proporcoes horizontais e verticais numa arvore realista`() {
        val root = FrameLayout(context)
        val mainRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        val leftSpace = spacerChild()
        val menuContainer =
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    id = R.id.menu_container
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0f)
                }
        val rightSpace = spacerChild()

        mainRow.addView(leftSpace)
        mainRow.addView(menuContainer)
        mainRow.addView(rightSpace)
        root.addView(mainRow)

        MenuLayoutConfig.applyAllProportionsToMenuLayout(root)

        // Horizontal proportions (portrait 10/80/10) applied directly to mainRow's children.
        assertEquals(0.10f, (leftSpace.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.10f, (rightSpace.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)

        // menuContainer was then re-wrapped vertically (portrait 30/40/30), preserving the
        // horizontal weight (0.80) it had just received.
        assertEquals(3, mainRow.childCount)
        val wrapper = mainRow.getChildAt(1) as LinearLayout
        assertEquals(0.80f, (wrapper.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(3, wrapper.childCount)
        assertSame(menuContainer, wrapper.getChildAt(1))
        assertEquals(0.40f, (menuContainer.layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    // ========== applyDialogProportions (keeps wrap_content, only positions the dialog) ==========

    @Test
    fun `applyDialogProportions posiciona o dialog sem esticar seu conteudo`() {
        val root = FrameLayout(context)
        val mainRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        val leftSpace = spacerChild()
        val dialogContainer =
                LinearLayout(context).apply {
                    id = R.id.dialog_container
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.80f)
                }
        val rightSpace = spacerChild()

        mainRow.addView(leftSpace)
        mainRow.addView(dialogContainer)
        mainRow.addView(rightSpace)
        root.addView(mainRow)

        MenuLayoutConfig.applyDialogProportions(root)

        val wrapper = mainRow.getChildAt(1) as LinearLayout
        assertSame(dialogContainer, wrapper.getChildAt(1))

        // Dialog itself keeps WRAP_CONTENT height (not stretched).
        assertEquals(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (dialogContainer.layoutParams as LinearLayout.LayoutParams).height
        )

        // Top space uses topWeight (0.30); bottom space absorbs content+bottom (0.40+0.30=0.70).
        assertEquals(0.30f, (wrapper.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
        assertEquals(0.70f, (wrapper.getChildAt(2).layoutParams as LinearLayout.LayoutParams).weight, 0.0001f)
    }

    @Test
    fun `applyDialogProportions sem dialog_container nao lanca excecao`() {
        val root = FrameLayout(context)
        try {
            MenuLayoutConfig.applyDialogProportions(root)
        } catch (e: Exception) {
            fail("applyDialogProportions should not throw without a dialog_container: ${e.message}")
        }
    }
}
