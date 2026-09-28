package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A minimal [MenuFragmentBase] with three items that navigates with the base class' circular
 * helpers and counts visual refreshes. It is never attached: selection and navigation don't need
 * a view.
 */
class ThreeItemMenuFragment(private val backResult: Boolean = false) : MenuFragmentBase() {
    var visualUpdates = 0
    var confirms = 0

    override fun getMenuItems(): List<MenuItem> =
            listOf(MenuItem("a", "A"), MenuItem("b", "B"), MenuItem("c", "C"))

    override fun performNavigateUp() = navigateUpCircular(getMenuItems().size)

    override fun performNavigateDown() = navigateDownCircular(getMenuItems().size)

    override fun performConfirm() {
        confirms++
    }

    override fun performBack(): Boolean = backResult

    override fun updateSelectionVisualInternal() {
        visualUpdates++
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuFragmentBase_test {

    @Test
    fun `setSelectedIndex dentro da faixa seleciona o item e atualiza o visual`() {
        val fragment = ThreeItemMenuFragment()

        fragment.setSelectedIndex(2)

        assertEquals(2, fragment.getCurrentSelectedIndex())
        assertEquals(1, fragment.visualUpdates)
    }

    @Test
    fun `setSelectedIndex fora da faixa e ignorado`() {
        val fragment = ThreeItemMenuFragment()
        fragment.setSelectedIndex(1)

        fragment.setSelectedIndex(-1)
        fragment.setSelectedIndex(3)

        assertEquals(1, fragment.getCurrentSelectedIndex())
        assertEquals(1, fragment.visualUpdates)
    }

    @Test
    fun `navegar para cima desce o indice e do primeiro item vai para o ultimo`() {
        val fragment = ThreeItemMenuFragment()
        fragment.setSelectedIndex(2)

        assertTrue(fragment.onNavigateUp())
        assertEquals(1, fragment.getCurrentSelectedIndex())

        fragment.onNavigateUp()
        fragment.onNavigateUp()
        assertEquals(2, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `navegar para baixo sobe o indice e do ultimo item volta para o primeiro`() {
        val fragment = ThreeItemMenuFragment()

        assertTrue(fragment.onNavigateDown())
        assertEquals(1, fragment.getCurrentSelectedIndex())

        fragment.onNavigateDown()
        fragment.onNavigateDown()
        assertEquals(0, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `onConfirm sempre consome o evento e chama performConfirm`() {
        val fragment = ThreeItemMenuFragment()

        assertTrue(fragment.onConfirm())
        assertEquals(1, fragment.confirms)
    }

    @Test
    fun `onBack devolve o resultado de performBack`() {
        assertTrue(ThreeItemMenuFragment(backResult = true).onBack())
        assertFalse(ThreeItemMenuFragment(backResult = false).onBack())
    }
}
