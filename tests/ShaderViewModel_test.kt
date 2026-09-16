package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.controllers.ShaderController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ShaderViewModel_test {

    private lateinit var viewModel: ShaderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("revenger_prefs", Application.MODE_PRIVATE).edit().clear().commit()
        viewModel = ShaderViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleShader sem controller configurado retorna o estado atual sem lancar`() {
        assertEquals(viewModel.getShaderState(), viewModel.toggleShader())
    }

    @Test
    fun `toggleShader delega ao controller, atualiza o estado e persiste`() {
        val controller = mockk<ShaderController>(relaxed = true)
        every { controller.getCurrentShader() } returns "disabled"
        every { controller.cycleShader() } returns "crt"
        viewModel.setShaderController(controller)

        val result = viewModel.toggleShader()

        assertEquals("crt", result)
        assertEquals("crt", viewModel.getShaderState())
        verify { controller.cycleShader() }
    }

    @Test
    fun `setShader sem controller configurado nao lanca e nao altera o estado`() {
        // O estado inicial "default" é sobrescrito pelo valor carregado das preferências em
        // init{} (SharedPreferencesRepository cai para ShaderType.SHARP.configName quando não
        // há nada salvo), então o estado após a carga é "sharp", não o literal "default".
        val stateAfterInit = viewModel.getShaderState()

        viewModel.setShader("crt")

        assertEquals(stateAfterInit, viewModel.getShaderState())
    }

    @Test
    fun `setShader delega ao controller e atualiza o estado`() {
        val controller = mockk<ShaderController>(relaxed = true)
        every { controller.getCurrentShader() } returns "disabled"
        viewModel.setShaderController(controller)

        viewModel.setShader("lcd")

        assertEquals("lcd", viewModel.getShaderState())
        verify { controller.setShader("lcd") }
    }

    @Test
    fun `setShaderController sincroniza o estado do ViewModel com o shader atual do controller`() {
        val controller = mockk<ShaderController>(relaxed = true)
        every { controller.getCurrentShader() } returns "sharp"

        viewModel.setShaderController(controller)

        assertEquals("sharp", viewModel.getShaderState())
        assertEquals(controller, viewModel.getShaderController())
    }

    @Test
    fun `getCurrentShaderDisplayName delega ao controller quando disponivel`() {
        val controller = mockk<ShaderController>(relaxed = true)
        every { controller.getCurrentShader() } returns "crt"
        every { controller.getCurrentShaderDisplayName() } returns "CRT"
        viewModel.setShaderController(controller)

        assertEquals("CRT", viewModel.getCurrentShaderDisplayName())
    }

    @Test
    fun `getCurrentShaderDisplayName cai para o estado local quando nao ha controller`() {
        assertEquals(viewModel.getShaderState(), viewModel.getCurrentShaderDisplayName())
    }
}
