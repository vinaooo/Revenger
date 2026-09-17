package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for SettingsMenuFragment, following the pattern proven by MenuIntegration_test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsMenuFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: SettingsMenuFragment

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = SettingsMenuFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "settings")
                .commitNow()
    }

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment inherits from MenuFragmentBase`() {
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `menu de Settings tem 4 items`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(4, menuItems.size)
    }

    @Test
    fun `menu items tem IDs e acoes corretos`() {
        val menuItems = fragment.getMenuItems()
        val expected =
                listOf(
                        "sound" to MenuAction.TOGGLE_AUDIO,
                        "shader" to MenuAction.TOGGLE_SHADER,
                        "speed" to MenuAction.TOGGLE_SPEED,
                        "back" to MenuAction.BACK
                )
        menuItems.forEachIndexed { index, item ->
            assertEquals(expected[index], item.id to item.action)
        }
    }

    @Test
    fun `menu items tem titulos nao vazios`() {
        fragment.getMenuItems().forEach { item -> assertFalse(item.title.isEmpty()) }
    }

    @Test
    fun `selecionar item back nao lanca excecao`() {
        val backItem = fragment.getMenuItems().first { it.id == "back" }
        try {
            fragment.onMenuItemSelected(backItem)
            assertTrue(true)
        } catch (e: Exception) {
            fail("onMenuItemSelected(back) should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `performConfirm no indice 3 delega para navigateBack, sem tocar audio, shader ou velocidade`() {
        // Pins BACK_TO_MAIN_MENU_INDEX = 3: performConfirm() on the "back" item must call
        // navigationController.navigateBack() -- and, unlike the audio/shader/speed toggle
        // branches (0, 1, 2), must NOT change any of those states. A mocked NavigationController
        // makes this discriminating: if the index constant regressed to a value that falls
        // through to the `else` branch instead, navigateBack() would never be invoked and this
        // assertion (not just the state-unchanged ones below) would catch it.
        val viewModel = getViewModel()
        val navigationController = mockk<NavigationController>(relaxed = true)
        viewModel.navigationController = navigationController

        val audioBefore = viewModel.getAudioState()
        val fastForwardBefore = viewModel.getFastForwardState()

        fragment.setSelectedIndex(3)
        fragment.onConfirm()

        verify(exactly = 1) { navigationController.navigateBack() }
        assertEquals(audioBefore, viewModel.getAudioState())
        assertEquals(fastForwardBefore, viewModel.getFastForwardState())
    }

    private fun getViewModel(): GameActivityViewModel {
        val viewModelField = SettingsMenuFragment::class.java.getDeclaredField("viewModel")
        viewModelField.isAccessible = true
        return viewModelField.get(fragment) as GameActivityViewModel
    }

    @Test
    fun `fragment pode ser destruido sem lancar excecao`() {
        try {
            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
            assertTrue(true)
        } catch (e: Exception) {
            fail("Destroying the fragment should not throw exception: ${e.message}")
        }
    }
}
