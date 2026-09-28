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
    fun `clearControllerInputState limpa o key log do combo`() {
        val keyLog = viewModel.getControllerInput().comboTracker.keyLog
        keyLog.add(android.view.KeyEvent.KEYCODE_BUTTON_SELECT)

        viewModel.clearControllerInputState()

        assertTrue(keyLog.isEmpty())
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
    fun `o callback shouldHandle do combo sempre aceita o combo`() {
        assertTrue(viewModel.getControllerInput().shouldHandleSelectStartCombo.invoke())
    }
}
