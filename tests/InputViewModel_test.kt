package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RevengerApplication.appConfig é um `lateinit var` (companion object, setter privado)
 * preenchido em RevengerApplication.onCreate() -- que nunca roda no Application de teste padrão
 * do Robolectric. InputViewModel lê esse valor direto no construtor, então precisa ser semeado
 * via reflexão antes de cada teste, e limpo depois para não vazar para outras classes de teste
 * na mesma JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class InputViewModel_test {

    private lateinit var viewModel: InputViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        // O backing field de `companion object { lateinit var appConfig }` é compilado como
        // campo estático na classe externa (RevengerApplication), não em RevengerApplication$Companion.
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = InputViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    @Test
    fun `getControllerInput retorna sempre a mesma instancia`() {
        assertTrue(viewModel.getControllerInput() === viewModel.getControllerInput())
    }

    @Test
    fun `getLeftGamePad e getRightGamePad comecam nulos antes de setupGamePads`() {
        assertTrue(viewModel.getLeftGamePad() == null)
        assertTrue(viewModel.getRightGamePad() == null)
    }

    @Test
    fun `setSelectStartComboCallback registra o callback invocado pelo ControllerInput`() {
        var invoked = false
        viewModel.setSelectStartComboCallback { invoked = true }

        viewModel.getControllerInput().selectStartComboCallback.invoke()

        assertTrue(invoked)
    }

    @Test
    fun `clearControllerInputState emite o evento de reset do combo`() {
        viewModel.clearControllerInputState()

        assertTrue(viewModel.eventFlow.value is InputViewModel.InputEvent.ResetComboAlreadyTriggered)
    }

    @Test
    fun `updateGamePadVisibility nao lanca quando nenhum container foi configurado`() {
        viewModel.updateGamePadVisibility(true)
        viewModel.updateGamePadVisibility(false)
    }

    @Test
    fun `updateGamePadVisibility aplica VISIBLE ou GONE no container configurado`() {
        val container = android.widget.LinearLayout(ApplicationProvider.getApplicationContext())
        viewModel.setGamePadContainer(container)

        viewModel.updateGamePadVisibility(true)
        assertTrue(container.visibility == android.view.View.VISIBLE)

        viewModel.updateGamePadVisibility(false)
        assertTrue(container.visibility == android.view.View.GONE)
    }

    @Test
    fun `controllerInput dispara HandleSelectStartCombo quando o combo shouldHandle callback e chamado`() {
        val handled = viewModel.getControllerInput().shouldHandleSelectStartCombo.invoke()

        assertTrue(handled)
        assertTrue(viewModel.eventFlow.value is InputViewModel.InputEvent.HandleSelectStartCombo)
    }
}
