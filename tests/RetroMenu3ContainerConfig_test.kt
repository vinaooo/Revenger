package com.vinaooo.revenger.viewmodels.menu

import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.MenuViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RetroMenu3ContainerConfig]'s container references are read/written through provider lambdas
 * backed by local vars here, standing in for `GameActivityViewModel`'s own container fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroMenu3ContainerConfig_test {

    private lateinit var menuViewModel: MenuViewModel
    private var currentMenuContainer: FrameLayout? = null
    private var currentGamePadContainer: LinearLayout? = null
    private lateinit var controller: RetroMenu3ContainerConfig

    @Before
    fun setUp() {
        menuViewModel = mockk(relaxed = true)
        currentMenuContainer = null
        currentGamePadContainer = null
        controller =
                RetroMenu3ContainerConfig(
                        menuContainerView = { currentMenuContainer },
                        setMenuContainerView = { currentMenuContainer = it },
                        setGamePadContainerView = { currentGamePadContainer = it },
                        menuViewModel = { menuViewModel }
                )
    }

    @Test
    fun `setMenuContainer guarda o container e notifica o menuViewModel`() {
        val container = FrameLayout(ApplicationProvider.getApplicationContext())

        controller.setMenuContainer(container)

        assertSame(container, currentMenuContainer)
        verify(exactly = 1) { menuViewModel.setMenuContainer(container) }
    }

    @Test
    fun `getMenuContainerId cai no fallback quando nenhum container foi configurado`() {
        assertEquals(R.id.menu_container, controller.getMenuContainerId())
    }

    @Test
    fun `getMenuContainerId retorna o id do container configurado`() {
        val container = FrameLayout(ApplicationProvider.getApplicationContext())
        container.id = 999
        currentMenuContainer = container

        assertEquals(999, controller.getMenuContainerId())
    }

    @Test
    fun `setGamePadContainer guarda a referencia do container`() {
        val container = LinearLayout(ApplicationProvider.getApplicationContext())

        controller.setGamePadContainer(container)

        assertSame(container, currentGamePadContainer)
    }
}
