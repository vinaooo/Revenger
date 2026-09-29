package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.utils.FontUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What the Core Variables list does, with a mocked ViewModel ([MenuFragmentHost]) and two
 * variables from a mocked [AppConfig]: the variable rows are read-only, only the last item (back)
 * acts, and navigation wraps around the list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoreVariablesFragmentActions_test {

    private val appConfigField =
            RevengerApplication::class.java.getDeclaredField("appConfig").apply { isAccessible = true }
    private var originalAppConfig: AppConfig? = null
    private lateinit var host: MenuFragmentHost<CoreVariablesFragment>

    @Before
    fun setUp() {
        originalAppConfig = appConfigField.get(null) as AppConfig?
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getVariables() } returns "opt_a=1,opt_b=2"
        appConfigField.set(null, appConfig)
        host = MenuFragmentHost(CoreVariablesFragment())
    }

    @After
    fun tearDown() {
        host.destroy()
        appConfigField.set(null, originalAppConfig)
    }

    private val fragment get() = host.fragment

    private fun rows(): List<View> {
        val list = fragment.requireView().findViewById<LinearLayout>(R.id.core_variables_list)
        return (0 until list.childCount).map { list.getChildAt(it) }.filterIsInstance<RetroCardView>()
    }

    private fun backButton() = fragment.requireView().findViewById<View>(R.id.variable_back)

    private fun arrowVisibility(row: View) = row.findViewById<TextView>(R.id.selection_arrow).visibility

    @Test
    fun `registra o fragment com as variaveis mais o item de voltar`() {
        verify { host.navigationController.registerFragment(fragment, 3) }
    }

    @Test
    fun `o titulo segue a capitalizacao configurada`() {
        val expected = FontUtils.getCapitalizedString(fragment.requireContext(), R.string.core_variables_menu_title)

        assertEquals(
                expected,
                fragment.requireView().findViewById<TextView>(R.id.core_variables_title).text.toString()
        )
    }

    @Test
    fun `a navegacao da a volta na lista e move a seta`() {
        fragment.onNavigateUp()
        assertEquals(2, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, backButton().findViewById<TextView>(R.id.selection_arrow_back).visibility)
        assertEquals(View.GONE, arrowVisibility(rows()[0]))

        fragment.onNavigateDown()
        assertEquals(0, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrowVisibility(rows()[0]))

        fragment.onNavigateDown()
        assertEquals(1, fragment.getCurrentSelectedIndex())
        val selectedColor = ContextCompat.getColor(fragment.requireContext(), R.color.rm_selected_color)
        assertEquals(selectedColor, rows()[1].findViewById<TextView>(R.id.item_title).currentTextColor)
    }

    @Test
    fun `confirmar uma variavel nao faz nada`() {
        fragment.onConfirm()

        verify(exactly = 0) { host.navigationController.navigateBack() }
    }

    @Test
    fun `confirmar o item de voltar volta pelo NavigationController`() {
        fragment.setSelectedIndex(2)

        fragment.onConfirm()

        verify(exactly = 1) { host.navigationController.navigateBack() }
    }

    @Test
    fun `tocar numa linha a seleciona e so o voltar navega`() {
        rows()[1].performClick()
        assertEquals(1, fragment.getCurrentSelectedIndex())
        verify(exactly = 0) { host.navigationController.navigateBack() }

        backButton().performClick()
        assertEquals(2, fragment.getCurrentSelectedIndex())
        verify(exactly = 1) { host.navigationController.navigateBack() }
    }

    @Test
    fun `as linhas das variaveis usam a fonte configurada`() {
        val typeface = FontUtils.getSelectedTypeface(fragment.requireContext())

        rows().dropLast(1).forEach { row ->
            assertSame(typeface, row.findViewById<TextView>(R.id.item_title).typeface)
            assertSame(typeface, row.findViewById<TextView>(R.id.selection_arrow).typeface)
        }
    }

    // The fragment instance survives a detach/attach (a submenu opened over it and closed
    // again), so the view lists must be rebuilt from scratch rather than appended to.
    @Test
    fun `recriar a view nao acumula as linhas antigas`() {
        val fragmentManager = host.activity.supportFragmentManager
        fragmentManager.beginTransaction().detach(fragment).commitNow()
        fragmentManager.beginTransaction().attach(fragment).commitNow()

        verify(exactly = 2) { host.navigationController.registerFragment(fragment, 3) }
        assertEquals(View.VISIBLE, arrowVisibility(rows()[0]))
        val selectedColor = ContextCompat.getColor(fragment.requireContext(), R.color.rm_selected_color)
        assertEquals(selectedColor, rows()[0].findViewById<TextView>(R.id.item_title).currentTextColor)

        fragment.onNavigateUp()
        assertEquals(2, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, backButton().findViewById<TextView>(R.id.selection_arrow_back).visibility)
    }

    @Test
    fun `selecionar um item fora da tela rola a lista ate ele`() {
        val many = mockk<AppConfig>(relaxed = true)
        every { many.getVariables() } returns (1..40).joinToString(",") { "opt_$it=$it" }
        appConfigField.set(null, many)
        host.destroy()
        host = MenuFragmentHost(CoreVariablesFragment())
        val root = fragment.requireView()
        root.measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 480, 320)
        val scroll = root.findViewById<android.widget.ScrollView>(R.id.core_variables_scroll)
        assertEquals(0, scroll.scrollY)

        fragment.onNavigateUp() // wraps to the back item, at the bottom of the list
        host.advance(1_000)

        assertTrue(scroll.scrollY > 0)
    }

    @Test
    fun `onBack devolve false para o processor tratar a volta`() {
        assertFalse(fragment.onBack())
    }
}
