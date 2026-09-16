package com.vinaooo.revenger.ui.retromenu3.navigation

import android.view.KeyEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * KeyboardInputAdapter traduz KeyEvents de teclado físico em NavigationEvents e os repassa ao
 * NavigationController. Estes testes cobrem o mapeamento de teclas em si e, principalmente, o
 * "press cycle tracking" (PHASE 4.x no código-fonte): o mecanismo que garante que segurar uma
 * tecla de navegação dispare apenas 1 navegação por ciclo, enquanto toques rápidos (com KEY_UP
 * entre eles) continuam navegando a cada toque.
 *
 * `pressCycleStates` e `actionKeyDownTimestamps` são `mutableMapOf` estáticos no companion object
 * (comentado no próprio código como "GLOBAL STATIC STATE - Compartilhado por TODAS as
 * instâncias"), então são limpos via reflection antes/depois de cada teste para isolar os casos —
 * do contrário, o estado de um teste vazaria para o próximo através da mesma tecla.
 *
 * O timeout de "novo ciclo" (`PRESS_CYCLE_TIMEOUT_MS` = 500ms) é medido com
 * `System.currentTimeMillis()` real (não com `event.eventTime`), então os testes que dependem de
 * "mesmo ciclo" ou "ciclo resetado" funcionam naturalmente: chamadas sucessivas dentro de um
 * método de teste acontecem em microssegundos, bem abaixo do timeout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KeyboardInputAdapter_test {

    private lateinit var navigationController: NavigationController
    private lateinit var adapter: KeyboardInputAdapter
    private var menuOpen = false

    @Before
    fun setUp() {
        navigationController = mockk(relaxed = true)
        menuOpen = false
        adapter = KeyboardInputAdapter(navigationController) { menuOpen }
        clearGlobalKeyState()
    }

    @After
    fun tearDown() {
        clearGlobalKeyState()
    }

    /**
     * `pressCycleStates` e `actionKeyDownTimestamps` são campos estáticos (companion object) —
     * precisam ser zerados entre testes para que o estado de uma tecla usada em um teste não
     * vaze para o próximo teste que usa o mesmo keyCode.
     */
    private fun clearGlobalKeyState() {
        val pressCycleField = KeyboardInputAdapter::class.java.getDeclaredField("pressCycleStates")
        pressCycleField.isAccessible = true
        (pressCycleField.get(null) as MutableMap<*, *>).clear()

        val actionField =
            KeyboardInputAdapter::class.java.getDeclaredField("actionKeyDownTimestamps")
        actionField.isAccessible = true
        (actionField.get(null) as MutableMap<*, *>).clear()
    }

    @Suppress("UNCHECKED_CAST")
    private fun seedActionKeyDownTimestamp(keyCode: Int, timestamp: Long) {
        val actionField =
            KeyboardInputAdapter::class.java.getDeclaredField("actionKeyDownTimestamps")
        actionField.isAccessible = true
        (actionField.get(null) as MutableMap<Int, Long>)[keyCode] = timestamp
    }

    /** Instância interna de `PressCycleState` para [keyCode], criada por uma chamada anterior. */
    @Suppress("UNCHECKED_CAST")
    private fun pressCycleState(keyCode: Int): Any {
        val field = KeyboardInputAdapter::class.java.getDeclaredField("pressCycleStates")
        field.isAccessible = true
        val map = field.get(null) as MutableMap<Int, Any>
        return requireNotNull(map[keyCode]) { "no PressCycleState recorded for keyCode=$keyCode" }
    }

    private fun setLastDownTime(keyCode: Int, timestamp: Long) {
        val state = pressCycleState(keyCode)
        val field = state.javaClass.getDeclaredField("lastDownTime")
        field.isAccessible = true
        field.setLong(state, timestamp)
    }

    private fun hasNavigatedInCycle(keyCode: Int): Boolean {
        val state = pressCycleState(keyCode)
        val field = state.javaClass.getDeclaredField("hasNavigatedInCycle")
        field.isAccessible = true
        return field.getBoolean(state)
    }

    private fun keyEvent(
        action: Int,
        code: Int,
        repeatCount: Int = 0,
        eventTime: Long = 1_000L,
        downTime: Long = eventTime
    ): KeyEvent = KeyEvent(downTime, eventTime, action, code, repeatCount)

    private fun capturedEvents(times: Int = 1): List<NavigationEvent> {
        val slot = mutableListOf<NavigationEvent>()
        verify(exactly = times) { navigationController.handleNavigationEvent(capture(slot)) }
        return slot
    }

    // --- Mapeamento de teclas de navegação (onKeyDown) ---

    @Test
    fun `seta para cima navega UP na primeira pressao`() {
        val consumed =
            adapter.onKeyDown(
                KeyEvent.KEYCODE_DPAD_UP,
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, eventTime = 100)
            )

        assertTrue(consumed)
        val event = capturedEvents().single() as NavigationEvent.Navigate
        assertEquals(Direction.UP, event.direction)
        assertEquals(InputSource.KEYBOARD, event.inputSource)
    }

    @Test
    fun `tecla W e um alias para seta para cima`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_W,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_W, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.Navigate
        assertEquals(Direction.UP, event.direction)
    }

    @Test
    fun `tecla D e um alias para seta para direita`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_D,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_D, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.Navigate
        assertEquals(Direction.RIGHT, event.direction)
    }

    @Test
    fun `seta para esquerda navega LEFT`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_LEFT,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.Navigate
        assertEquals(Direction.LEFT, event.direction)
    }

    @Test
    fun `seta para baixo navega DOWN`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_DOWN,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.Navigate
        assertEquals(Direction.DOWN, event.direction)
    }

    // --- Press cycle tracking: repeat / segurar tecla / toques rápidos ---

    @Test
    fun `evento de repeat para tecla de navegacao e ignorado`() {
        val consumed =
            adapter.onKeyDown(
                KeyEvent.KEYCODE_DPAD_UP,
                keyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_UP,
                    repeatCount = 1,
                    eventTime = 100
                )
            )

        assertTrue(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    @Test
    fun `segurar tecla sem KEY_UP navega apenas 1 vez no mesmo ciclo`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_LEFT,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, eventTime = 100)
        )
        // Segundo KEY_DOWN com repeatCount=0 mas SEM KEY_UP entre eles (simula o app reenviando
        // eventos rapidamente, conforme comentário V4.3 no código-fonte) e eventTime diferente
        // (não é dedup) — deve ser bloqueado pelo press-cycle tracking.
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_LEFT,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, eventTime = 150)
        )

        val events = capturedEvents(times = 1)
        assertEquals(Direction.LEFT, (events.single() as NavigationEvent.Navigate).direction)
    }

    @Test
    fun `KEY_UP tardio (apos o timeout de ciclo) nao reseta hasNavigatedInCycle`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_RIGHT,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, eventTime = 100)
        )
        assertTrue(hasNavigatedInCycle(KeyEvent.KEYCODE_DPAD_RIGHT))

        // Recua lastDownTime para simular um KEY_UP que chegou bem depois de
        // PRESS_CYCLE_TIMEOUT_MS (500ms) — sem depender de dormir de verdade no teste.
        setLastDownTime(KeyEvent.KEYCODE_DPAD_RIGHT, System.currentTimeMillis() - 10_000)

        adapter.onKeyUp(
            KeyEvent.KEYCODE_DPAD_RIGHT,
            keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, eventTime = 101)
        )

        // Branch "else" de onKeyUp (timeSinceLastDown >= timeout): NÃO reseta o ciclo.
        assertTrue(hasNavigatedInCycle(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    @Test
    fun `KEY_UP reseta o ciclo e permite navegar de novo imediatamente`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_UP,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, eventTime = 100)
        )
        adapter.onKeyUp(
            KeyEvent.KEYCODE_DPAD_UP,
            keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, eventTime = 101)
        )
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_UP,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, eventTime = 102)
        )

        val events = capturedEvents(times = 2)
        assertTrue(events.all { (it as NavigationEvent.Navigate).direction == Direction.UP })
    }

    @Test
    fun `KEY_DOWN duplicado com mesmo eventTime nao navega duas vezes`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_DOWN,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, eventTime = 200)
        )
        // Mesmo eventTime: reentrega do mesmo evento de hardware (dedup), não um novo toque.
        val consumed =
            adapter.onKeyDown(
                KeyEvent.KEYCODE_DPAD_DOWN,
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, eventTime = 200)
            )

        assertTrue(consumed)
        capturedEvents(times = 1)
    }

    // --- Teclas de ação (onKeyDown) ---

    @Test
    fun `Enter ativa o item selecionado`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_ENTER,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.ActivateSelected
        assertEquals(KeyEvent.KEYCODE_ENTER, event.keyCode)
        assertEquals(InputSource.KEYBOARD, event.inputSource)
    }

    @Test
    fun `Espaco tambem ativa o item selecionado`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_SPACE,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.ActivateSelected
        assertEquals(KeyEvent.KEYCODE_SPACE, event.keyCode)
    }

    @Test
    fun `DPAD_CENTER tambem ativa o item selecionado`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DPAD_CENTER,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.ActivateSelected
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, event.keyCode)
    }

    @Test
    fun `Escape fecha todos os menus`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_ESCAPE,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.CloseAllMenus
        assertEquals(KeyEvent.KEYCODE_ESCAPE, event.keyCode)
    }

    @Test
    fun `Backspace navega para tras`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DEL,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.NavigateBack
        assertEquals(KeyEvent.KEYCODE_DEL, event.keyCode)
    }

    @Test
    fun `evento de repeat para tecla de acao e ignorado`() {
        val consumed =
            adapter.onKeyDown(
                KeyEvent.KEYCODE_ENTER,
                keyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER,
                    repeatCount = 2,
                    eventTime = 100
                )
            )

        assertTrue(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    @Test
    fun `tecla nao mapeada nao e consumida`() {
        val consumed =
            adapter.onKeyDown(
                KeyEvent.KEYCODE_0,
                keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0, eventTime = 100)
            )

        assertFalse(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    // --- F12: toggle do menu ---

    @Test
    fun `F12 abre o menu quando ele esta fechado`() {
        menuOpen = false

        adapter.onKeyDown(
            KeyEvent.KEYCODE_F12,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.OpenMenu
        assertEquals(InputSource.KEYBOARD, event.inputSource)
    }

    @Test
    fun `F12 fecha todos os menus quando ja esta aberto`() {
        menuOpen = true

        adapter.onKeyDown(
            KeyEvent.KEYCODE_F12,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12, eventTime = 100)
        )

        val event = capturedEvents().single() as NavigationEvent.CloseAllMenus
        assertEquals(KeyEvent.KEYCODE_F12, event.keyCode)
    }

    // --- onKeyUp: Backspace com validação de KEY_DOWN correspondente ---

    @Test
    fun `KEY_UP do Backspace logo apos o KEY_DOWN nao gera segundo evento`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_DEL,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, eventTime = 100)
        )
        val consumed =
            adapter.onKeyUp(
                KeyEvent.KEYCODE_DEL,
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, eventTime = 110)
            )

        assertTrue(consumed)
        // Apenas o evento do KEY_DOWN — o KEY_UP correspondente não deve gerar um segundo.
        capturedEvents(times = 1)
    }

    @Test
    fun `KEY_UP orfao do Backspace (timeout excedido) e descartado silenciosamente`() {
        // Simula um KEY_DOWN "antigo" sem um onKeyDown() real ter acontecido neste teste,
        // para exercitar o branch de KEY_UP órfão sem depender de esperar 500ms de verdade.
        seedActionKeyDownTimestamp(
            KeyEvent.KEYCODE_DEL,
            System.currentTimeMillis() - 10_000
        )

        val consumed =
            adapter.onKeyUp(
                KeyEvent.KEYCODE_DEL,
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, eventTime = 100)
            )

        assertTrue(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    @Test
    fun `KEY_UP do Backspace sem KEY_DOWN registrado usa o fallback legado`() {
        val consumed =
            adapter.onKeyUp(
                KeyEvent.KEYCODE_DEL,
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, eventTime = 100)
            )

        assertTrue(consumed)
        val event = capturedEvents().single() as NavigationEvent.NavigateBack
        assertEquals(KeyEvent.KEYCODE_DEL, event.keyCode)
    }

    // --- Candidatos a bug: BACK e ESCAPE emitem o evento duas vezes (KEY_DOWN e KEY_UP) ---
    //
    // Ao contrário do Backspace (KEYCODE_DEL), cujo KEY_DOWN grava um timestamp em
    // `actionKeyDownTimestamps` para que o KEY_UP correspondente seja reconhecido como "já
    // processado" e não dispare de novo, KEYCODE_BACK e KEYCODE_ESCAPE NÃO gravam esse
    // timestamp no KEY_DOWN (ver KeyboardInputAdapter.kt linha ~331 e ~311). O comentário da
    // FIX ERRO 1 (linha ~55) documenta a intenção como cobrindo "Back/Backspace", mas a
    // implementação só cobre Backspace. Resultado: para um único toque físico de Back/Escape,
    // o onKeyUp cai no branch de "fallback legado" (nenhum KEY_DOWN registrado) e reenvia o
    // MESMO tipo de NavigationEvent que o onKeyDown já havia enviado — o adaptador emite o
    // evento DUAS vezes por toque.
    //
    // Se isso vira uma navegação dupla visível depende de EventQueue (ver EventQueue.kt): ela
    // aplica `debounceWindowMs` (200ms, ver NavigationController.DEBOUNCE_WINDOW_MS) a
    // NavigateBack/CloseAllMenus, então um toque físico com DOWN→UP mais rápido que 200ms tem
    // o segundo evento descartado por debounce; um toque mantido por mais de 200ms deixa
    // passar os dois e causa uma navegação/fechamento duplo real. `KeyboardInputAdapter` é
    // chamado a partir de `GameActivityViewModel.processKeyEvent` (routing real de
    // ACTION_DOWN/ACTION_UP), então o caminho é alcançável em produção. Estes testes cobrem
    // apenas a emissão no nível do adaptador (2 chamadas), não o comportamento pós-debounce;
    // veja o relatório da Task 12 para detalhes e para a ressalva sobre alcance real do bug.

    @Test
    fun `tecla Back gera NavigateBack duplicado no down e no up (bug candidato)`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_BACK,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, eventTime = 100)
        )
        adapter.onKeyUp(
            KeyEvent.KEYCODE_BACK,
            keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, eventTime = 110)
        )

        val events = capturedEvents(times = 2)
        assertTrue(events.all { it is NavigationEvent.NavigateBack })
    }

    @Test
    fun `tecla Escape gera CloseAllMenus duplicado no down e no up (bug candidato)`() {
        adapter.onKeyDown(
            KeyEvent.KEYCODE_ESCAPE,
            keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE, eventTime = 100)
        )
        adapter.onKeyUp(
            KeyEvent.KEYCODE_ESCAPE,
            keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE, eventTime = 110)
        )

        val events = capturedEvents(times = 2)
        assertTrue(events.all { it is NavigationEvent.CloseAllMenus })
    }

    // --- onKeyUp: teclas de navegação e teclas não mapeadas ---

    @Test
    fun `onKeyUp de tecla de navegacao e consumido sem gerar evento`() {
        val consumed =
            adapter.onKeyUp(
                KeyEvent.KEYCODE_DPAD_UP,
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, eventTime = 100)
            )

        assertTrue(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    @Test
    fun `onKeyUp de tecla nao mapeada nao e consumido`() {
        val consumed =
            adapter.onKeyUp(
                KeyEvent.KEYCODE_0,
                keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0, eventTime = 100)
            )

        assertFalse(consumed)
        verify(exactly = 0) { navigationController.handleNavigationEvent(any()) }
    }

    // --- isNavigationKey(): classificação pública de teclas suportadas ---

    @Test
    fun `isNavigationKey reconhece todas as teclas mapeadas`() {
        val supported =
            listOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_W,
                KeyEvent.KEYCODE_A,
                KeyEvent.KEYCODE_S,
                KeyEvent.KEYCODE_D,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_SPACE,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_DEL,
                KeyEvent.KEYCODE_F12
            )

        supported.forEach { code -> assertTrue("keyCode=$code", adapter.isNavigationKey(code)) }
    }

    @Test
    fun `isNavigationKey rejeita tecla nao suportada`() {
        assertFalse(adapter.isNavigationKey(KeyEvent.KEYCODE_0))
    }
}
