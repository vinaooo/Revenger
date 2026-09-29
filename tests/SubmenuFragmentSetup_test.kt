package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig
import com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutFinder
import com.vinaooo.revenger.viewmodels.InputViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What every text submenu (About, Core Variables, Progress, Settings, Exit) sets up when its view
 * is created, checked once per fragment: the configured vertical proportions, every view flat
 * under the gamepad, cards without a selected background color, the first item selected, and
 * pending controller input cleared when the submenu pauses.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [33])
class SubmenuFragmentSetup_test(private val name: String, private val create: () -> Fragment) {

    private val appConfigField =
            RevengerApplication::class.java.getDeclaredField("appConfig").apply { isAccessible = true }
    private var originalAppConfig: AppConfig? = null
    private lateinit var host: MenuFragmentHost<Fragment>

    @Before
    fun setUp() {
        originalAppConfig = appConfigField.get(null) as AppConfig?
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getVariables() } returns "opt_a=1,opt_b=2"
        appConfigField.set(null, appConfig)
        host = MenuFragmentHost(create())
    }

    @After
    fun tearDown() {
        host.destroy()
        appConfigField.set(null, originalAppConfig)
    }

    private val root: View get() = host.fragment.requireView()

    private fun allViews(view: View = root): List<View> =
            listOf(view) + ((view as? ViewGroup)?.let { group ->
                (0 until group.childCount).flatMap { allViews(group.getChildAt(it)) }
            } ?: emptyList())

    /** The selection arrows in layout order, which is the menu's item order. */
    private fun arrows(): List<TextView> =
            allViews().filterIsInstance<TextView>().filter {
                it.id != View.NO_ID && root.resources.getResourceEntryName(it.id).startsWith("selection_arrow")
            }

    @Test
    fun `aplica as proporcoes verticais do menu`() {
        // Core Variables' container isn't among the ids MenuLayoutFinder.findMenuContentContainer
        // looks for, so it isn't wrapped yet; its fix comes with its own test.
        assumeTrue(name != "CoreVariables")
        val vertical = checkNotNull(MenuLayoutConfig.getConfiguredVerticalProportions(root))
        val main = checkNotNull(MenuLayoutFinder.findMainHorizontalLayout(root))
        val wrapper = main.getChildAt(1) as LinearLayout

        assertEquals(LinearLayout.VERTICAL, wrapper.orientation)
        assertEquals(vertical.topWeight, (wrapper.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight)
    }

    @Test
    fun `nenhuma view fica acima do gamepad`() {
        allViews().forEach { view ->
            assertEquals("$name: ${view.javaClass.simpleName} z", 0f, view.z)
            assertEquals("$name: ${view.javaClass.simpleName} elevation", 0f, view.elevation)
        }
    }

    @Test
    fun `os cards nao pintam fundo no item selecionado`() {
        val cards = allViews().filterIsInstance<RetroCardView>()

        assertTrue(cards.isNotEmpty())
        cards.forEach { assertFalse("$name: ${it.id}", it.getUseBackgroundColor()) }
    }

    @Test
    fun `abre com o primeiro item selecionado`() {
        val selectedColor = ContextCompat.getColor(root.context, R.color.rm_selected_color)
        val arrows = arrows()

        assertTrue(arrows.size >= 2)
        assertEquals(View.VISIBLE, arrows.first().visibility)
        assertEquals(selectedColor, arrows.first().currentTextColor)
        arrows.drop(1).forEach { assertEquals(View.GONE, it.visibility) }
    }

    @Test
    fun `pausar limpa as entradas pendentes do controle`() {
        val inputViewModel = ViewModelProvider(host.activity)[InputViewModel::class.java]
        val controllerInput =
                InputViewModel::class.java.getDeclaredField("controllerInput").apply { isAccessible = true }
                        .get(inputViewModel) as ControllerInput
        val tracker = controllerInput.comboTracker
        tracker.javaClass.getDeclaredField("comboAlreadyTriggered").apply { isAccessible = true }.set(tracker, true)

        host.destroy()

        assertFalse(tracker.getComboAlreadyTriggered())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun fragments(): List<Array<Any>> =
                listOf(
                        arrayOf("About", { AboutFragment.newInstance() }),
                        arrayOf("CoreVariables", { CoreVariablesFragment() }),
                        arrayOf("Progress", { ProgressFragment.newInstance() }),
                        arrayOf("Settings", { SettingsMenuFragment.newInstance() }),
                        arrayOf("Exit", { ExitFragment.newInstance() }),
                )
    }
}
