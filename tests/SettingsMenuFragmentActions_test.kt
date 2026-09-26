package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.TextView
import com.vinaooo.revenger.R
import io.mockk.every
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What each Settings menu entry does, with a mocked ViewModel ([MenuFragmentHost]). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsMenuFragmentActions_test {

    private lateinit var host: MenuFragmentHost<SettingsMenuFragment>

    @After
    fun tearDown() {
        host.destroy()
    }

    @Before
    fun setUp() {
        host = MenuFragmentHost(SettingsMenuFragment.newInstance())
        every { host.viewModel.getShaderDisplayName() } returns "Sharp"
    }

    private val fragment get() = host.fragment

    private fun confirm(index: Int) {
        fragment.setSelectedIndex(index)
        fragment.onConfirm()
    }

    private fun text(id: Int) = fragment.requireView().findViewById<TextView>(id).text.toString()

    private fun string(id: Int) = fragment.getString(id)

    @Test
    fun `Sound inverte o audio e atualiza o titulo`() {
        every { host.viewModel.getAudioState() } returns true

        confirm(0)

        verify { host.viewModel.setAudioEnabled(false) }
        assertEquals(string(R.string.audio_on).lowercase(), text(R.id.sound_title).lowercase())

        every { host.viewModel.getAudioState() } returns false
        confirm(0)

        verify { host.viewModel.setAudioEnabled(true) }
        assertEquals(string(R.string.audio_off).lowercase(), text(R.id.sound_title).lowercase())
    }

    @Test
    fun `Shader troca o shader e mostra o nome novo`() {
        every { host.viewModel.getShaderDisplayName() } returns "Crt"

        confirm(1)

        verify { host.viewModel.onToggleShader() }
        assertEquals(
                "${string(R.string.settings_shader)}: Crt".lowercase(),
                text(R.id.shader_title).lowercase()
        )
    }

    @Test
    fun `Game speed inverte o fast forward e atualiza o titulo`() {
        every { host.viewModel.getFastForwardState() } returns false

        confirm(2)

        verify { host.viewModel.setFastForwardEnabled(true) }

        every { host.viewModel.getFastForwardState() } returns true
        confirm(2)

        verify { host.viewModel.setFastForwardEnabled(false) }
        assertEquals(
                string(R.string.fast_forward_active).lowercase(),
                text(R.id.game_speed_title).lowercase()
        )
    }

    @Test
    fun `Back volta pelo NavigationController`() {
        confirm(3)

        verify { host.navigationController.navigateBack() }
    }

    @Test
    fun `toque seleciona o item na hora e ativa depois do atraso`() {
        fragment.requireView().findViewById<View>(R.id.settings_shader).performClick()

        verify { host.navigationController.selectItem(1) }
        verify(exactly = 0) { host.navigationController.activateItem() }

        host.advance(MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS)
        verify { host.navigationController.activateItem() }
    }

    @Test
    fun `navegacao circula pelos 4 itens e move a seta de selecao`() {
        fragment.onNavigateUp()
        assertEquals(3, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, fragment.requireView().findViewById<View>(R.id.selection_arrow_back).visibility)

        fragment.onNavigateDown()
        fragment.onNavigateDown()
        assertEquals(1, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, fragment.requireView().findViewById<View>(R.id.selection_arrow_shader).visibility)
        assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.selection_arrow_back).visibility)
    }

    @Test
    fun `hideMainMenu e showMainMenu escondem e mostram o conteudo`() {
        val container = fragment.requireView().findViewById<View>(R.id.settings_menu_container)

        fragment.hideMainMenu()
        assertEquals(View.INVISIBLE, container.visibility)

        container.alpha = 0.3f
        fragment.showMainMenu()
        assertEquals(View.VISIBLE, container.visibility)
        assertEquals(1.0f, container.alpha)
    }

    @Test
    fun `onBack devolve false para o processor tratar a volta`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `dismissMenuPublic remove o fragment`() {
        fragment.dismissMenuPublic()
        host.idle()

        assertFalse(fragment.isAdded)
    }
}
