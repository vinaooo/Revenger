package com.vinaooo.revenger.ui.retromenu3

import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [KeyboardNavigationController] was extracted out of [RetroKeyboard] (grid cursor
 * position/gamepad-vs-touch mode/selection highlight), which had zero test coverage before this
 * file. Uses small integer "key ids" (view ids created via `View.generateViewId()`) rather than
 * the real `R.id.key_*` layout, since only the grid math and mode/highlight bookkeeping are under
 * test here -- not the real keyboard layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KeyboardNavigationController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var rows: Array<IntArray>
    private lateinit var keyboardView: LinearLayout
    private lateinit var controller: KeyboardNavigationController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()
        val context = fragment.requireContext()

        // 3 rows of 3/2/4 keys respectively, to exercise column-clamping on row change.
        rows =
                arrayOf(
                        intArrayOf(1, 2, 3),
                        intArrayOf(4, 5),
                        intArrayOf(6, 7, 8, 9)
                )
        keyboardView =
                LinearLayout(context).apply {
                    rows.forEach { row ->
                        row.forEach { keyId -> addView(TextView(context).apply { id = keyId }) }
                    }
                }
        controller =
                KeyboardNavigationController(
                        keyboardRows = rows,
                        selectedColor = 0xFFFFFF00.toInt(),
                        normalColor = 0xFFFFFFFF.toInt()
                )
        controller.keyboardView = keyboardView
    }

    @Test
    fun `posicao inicial e linha 0 coluna 0`() {
        assertEquals(0, controller.currentRow)
        assertEquals(0, controller.currentCol)
    }

    @Test
    fun `navigateDown avanca linha e navigateUp retorna, retornando false no limite`() {
        assertTrue(controller.navigateDown())
        assertEquals(1, controller.currentRow)

        assertTrue(controller.navigateUp())
        assertEquals(0, controller.currentRow)

        assertFalse(controller.navigateUp())
        assertEquals(0, controller.currentRow)
    }

    @Test
    fun `navigateDown ate a ultima linha retorna false`() {
        controller.navigateDown()
        controller.navigateDown()
        assertEquals(2, controller.currentRow)

        assertFalse(controller.navigateDown())
        assertEquals(2, controller.currentRow)
    }

    @Test
    fun `mudar de linha mais longa para mais curta trava a coluna no ultimo indice valido`() {
        controller.navigateRight()
        controller.navigateRight()
        assertEquals(2, controller.currentCol)

        // Row 1 only has 2 columns (indices 0-1); moving there must clamp currentCol.
        controller.navigateDown()

        assertEquals(1, controller.currentCol)
    }

    @Test
    fun `navigateLeft e navigateRight respeitam os limites da linha`() {
        assertFalse(controller.navigateLeft())

        assertTrue(controller.navigateRight())
        assertTrue(controller.navigateRight())
        assertEquals(2, controller.currentCol)
        assertFalse(controller.navigateRight())
    }

    @Test
    fun `reset volta para linha e coluna 0`() {
        controller.navigateDown()
        controller.navigateRight()

        controller.reset()

        assertEquals(0, controller.currentRow)
        assertEquals(0, controller.currentCol)
    }

    @Test
    fun `navegar aplica o destaque de selecao na view da chave atual`() {
        controller.navigateRight()

        val selectedKey = keyboardView.findViewById<TextView>(2)
        assertTrue(selectedKey.isSelected)
        assertEquals(0xFFFFFF00.toInt(), selectedKey.currentTextColor)
    }

    @Test
    fun `enterTouchMode limpa a selecao visual e ignora navegacao ate voltar ao modo gamepad`() {
        controller.navigateRight() // key id 2 selected

        controller.enterTouchMode()

        val previouslySelected = keyboardView.findViewById<TextView>(2)
        assertFalse(previouslySelected.isSelected)
    }

    @Test
    fun `navegar apos enterTouchMode reativa o modo gamepad e o destaque`() {
        controller.enterTouchMode()

        controller.navigateRight()

        val selectedKey = keyboardView.findViewById<TextView>(2)
        assertTrue(selectedKey.isSelected)
    }

    @Test
    fun `metodos nao lancam quando nenhuma view foi anexada`() {
        val detached =
                KeyboardNavigationController(
                        keyboardRows = rows,
                        selectedColor = 0,
                        normalColor = 0
                )

        detached.navigateDown()
        detached.navigateRight()
        detached.enterTouchMode()
        detached.reset()
        detached.activateGamepadSelection()
    }
}
