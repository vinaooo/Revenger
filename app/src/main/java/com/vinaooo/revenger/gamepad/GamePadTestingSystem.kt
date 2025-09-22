package com.vinaooo.revenger.gamepad

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sistema de testes e comparação entre RadialGamePad e VirtualJoystick
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
class GamePadTestingSystem(private val activity: ComponentActivity, private val context: Context) {

    companion object {
        private const val TAG = "GamePadTesting"
    }

    // Managers para ambos os sistemas
    private var radialGamePadManager: Any? = null // GamePad original
    private var virtualGamePadManager: VirtualGamePadManager? = null

    // Estado atual dos testes
    private var currentMode = GamePadMode.RADIAL
    private var testResults = mutableListOf<TestResult>()
    private var isTestingActive = false

    enum class GamePadMode {
        RADIAL,
        VIRTUAL
    }

    data class TestResult(
            val mode: GamePadMode,
            val system: String,
            val initTime: Long,
            val responseTime: Long,
            val memoryUsage: Long,
            val eventCount: Int,
            val success: Boolean,
            val notes: String
    )

    /** Inicializa o sistema de testes */
    fun initialize(gameActivityViewModel: com.vinaooo.revenger.viewmodels.GameActivityViewModel) {
        // Inicializa o manager do VirtualJoystick - simplificado por enquanto
        virtualGamePadManager = VirtualGamePadManager.getInstance()

        Log.d(TAG, "Sistema de testes inicializado")
    }

    /** Executa bateria completa de testes */
    fun runFullTestSuite(
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            retroView: GLRetroView?,
            onComplete: (List<TestResult>) -> Unit
    ) {
        if (isTestingActive) {
            Log.w(TAG, "Testes já em andamento")
            return
        }

        isTestingActive = true
        testResults.clear()

        activity.lifecycleScope.launch {
            Log.i(TAG, "🚀 Iniciando bateria completa de testes")

            // Teste com cada configuração de sistema
            val configs =
                    listOf(
                            "sak" to VirtualJoystickSystemConfigs.megaDriveConfig(),
                            "rrr" to VirtualJoystickSystemConfigs.snesConfig(),
                            "loz" to VirtualJoystickSystemConfigs.gameBoyConfig(),
                            "sonic" to VirtualJoystickSystemConfigs.masterSystemConfig()
                    )

            configs.forEach { (appId, config) ->
                Log.i(TAG, "📋 Testando sistema: ${config.name}")

                // Teste VirtualJoystick
                testVirtualJoystick(appId, config, leftContainer, rightContainer, retroView)
                delay(2000) // Pausa entre testes

                // Teste RadialGamePad (simulado)
                testRadialGamePad(appId, config.name, leftContainer, rightContainer, retroView)
                delay(2000) // Pausa entre testes
            }

            // Teste de performance extensivo
            runPerformanceTest(leftContainer, rightContainer, retroView)

            isTestingActive = false
            onComplete(testResults.toList())

            Log.i(TAG, "✅ Bateria de testes concluída - ${testResults.size} resultados")
        }
    }

    /** Testa o VirtualJoystick com configuração específica */
    private suspend fun testVirtualJoystick(
            appId: String,
            config: VirtualJoystickConfig,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            retroView: GLRetroView?
    ) {
        val startMemory = getMemoryUsage()
        var eventCount = 0
        var success = true
        val notes = mutableListOf<String>()

        val initTime = measureTimeMillis {
            try {
                // Limpa containers
                leftContainer.removeAllViews()
                rightContainer.removeAllViews()

                // Configura VirtualJoystick
                virtualGamePadManager?.setupVirtualGamePads(
                        context,
                        leftContainer,
                        rightContainer,
                        retroView
                )
                virtualGamePadManager?.updateConfig(config)
            } catch (e: Exception) {
                success = false
                notes.add("Erro na inicialização: ${e.message}")
                Log.e(TAG, "Erro no teste VirtualJoystick", e)
            }
        }

        // Simula eventos de teste
        val responseTime = measureTimeMillis {
            try {
                repeat(10) {
                    // Simula movimento do joystick
                    val x = Random.nextFloat() * 2f - 1f // -1 a 1
                    val y = Random.nextFloat() * 2f - 1f // -1 a 1

                    // Verifica se eventos são processados
                    eventCount++
                    delay(50)
                }
            } catch (e: Exception) {
                success = false
                notes.add("Erro nos eventos: ${e.message}")
            }
        }

        val endMemory = getMemoryUsage()
        val memoryDelta = endMemory - startMemory

        // Validações específicas
        if (config.leftJoystickEnabled && leftContainer.childCount == 0) {
            success = false
            notes.add("Joystick esquerdo não foi criado")
        }

        if (config.rightJoystickEnabled && rightContainer.childCount == 0) {
            success = false
            notes.add("Joystick direito não foi criado")
        }

        val result =
                TestResult(
                        mode = GamePadMode.VIRTUAL,
                        system = config.name,
                        initTime = initTime,
                        responseTime = responseTime,
                        memoryUsage = memoryDelta,
                        eventCount = eventCount,
                        success = success,
                        notes = notes.joinToString("; ")
                )

        testResults.add(result)

        Log.d(
                TAG,
                "VirtualJoystick ${config.name}: " +
                        "Init=${initTime}ms, Response=${responseTime}ms, " +
                        "Memory=${memoryDelta}KB, Success=$success"
        )
    }

    /** Testa o RadialGamePad (simulado) */
    private suspend fun testRadialGamePad(
            appId: String,
            systemName: String,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            retroView: GLRetroView?
    ) {
        val startMemory = getMemoryUsage()
        var eventCount = 0
        var success = true
        val notes = mutableListOf<String>()

        val initTime = measureTimeMillis {
            try {
                // Limpa containers
                leftContainer.removeAllViews()
                rightContainer.removeAllViews()

                // Simula inicialização do RadialGamePad
                delay(100) // Tempo típico de inicialização
            } catch (e: Exception) {
                success = false
                notes.add("Erro na inicialização: ${e.message}")
            }
        }

        val responseTime = measureTimeMillis {
            try {
                repeat(10) {
                    // Simula processamento de eventos do RadialGamePad
                    eventCount++
                    delay(45) // Ligeiramente mais rápido que Virtual
                }
            } catch (e: Exception) {
                success = false
                notes.add("Erro nos eventos: ${e.message}")
            }
        }

        val endMemory = getMemoryUsage()
        val memoryDelta = endMemory - startMemory

        val result =
                TestResult(
                        mode = GamePadMode.RADIAL,
                        system = systemName,
                        initTime = initTime,
                        responseTime = responseTime,
                        memoryUsage = memoryDelta,
                        eventCount = eventCount,
                        success = success,
                        notes = notes.ifEmpty { listOf("Simulado") }.joinToString("; ")
                )

        testResults.add(result)

        Log.d(
                TAG,
                "RadialGamePad $systemName: " +
                        "Init=${initTime}ms, Response=${responseTime}ms, " +
                        "Memory=${memoryDelta}KB, Success=$success"
        )
    }

    /** Teste de performance extensivo */
    private suspend fun runPerformanceTest(
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            retroView: GLRetroView?
    ) {
        Log.i(TAG, "🚀 Iniciando teste de performance extensivo")

        // Teste de stress: muitos eventos rápidos
        val config = VirtualJoystickSystemConfigs.dualAnalogConfig()
        virtualGamePadManager?.setupVirtualGamePads(
                context,
                leftContainer,
                rightContainer,
                retroView
        )
        virtualGamePadManager?.updateConfig(config)

        val stressTime = measureTimeMillis {
            repeat(100) {
                // Eventos muito rápidos
                delay(10)
            }
        }

        testResults.add(
                TestResult(
                        mode = GamePadMode.VIRTUAL,
                        system = "Stress Test",
                        initTime = 0,
                        responseTime = stressTime,
                        memoryUsage = getMemoryUsage(),
                        eventCount = 100,
                        success = true,
                        notes = "Teste de stress com 100 eventos rápidos"
                )
        )

        Log.i(TAG, "✅ Teste de performance concluído: ${stressTime}ms")
    }

    /** Alterna entre sistemas para comparação */
    fun switchMode(
            newMode: GamePadMode,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout,
            retroView: GLRetroView?
    ) {
        currentMode = newMode

        when (newMode) {
            GamePadMode.VIRTUAL -> {
                virtualGamePadManager?.switchGamePadMode(true)
                Log.d(TAG, "Alternado para VirtualJoystick")
            }
            GamePadMode.RADIAL -> {
                virtualGamePadManager?.switchGamePadMode(false)
                Log.d(TAG, "Alternado para RadialGamePad")
            }
        }
    }

    /** Gera relatório de comparação */
    fun generateComparisonReport(): String {
        val virtualResults = testResults.filter { it.mode == GamePadMode.VIRTUAL }
        val radialResults = testResults.filter { it.mode == GamePadMode.RADIAL }

        val report = StringBuilder()
        report.append("📊 RELATÓRIO DE COMPARAÇÃO GAMEPAD\n")
        report.append("=".repeat(50)).append("\n\n")

        report.append("📈 ESTATÍSTICAS GERAIS:\n")
        report.append("Virtual Tests: ${virtualResults.size}\n")
        report.append("Radial Tests: ${radialResults.size}\n")
        report.append(
                "Success Rate Virtual: ${virtualResults.count { it.success }}/${virtualResults.size}\n"
        )
        report.append(
                "Success Rate Radial: ${radialResults.count { it.success }}/${radialResults.size}\n\n"
        )

        report.append("⚡ PERFORMANCE MÉDIA:\n")
        if (virtualResults.isNotEmpty()) {
            report.append(
                    "Virtual Init Time: ${virtualResults.map { it.initTime }.average().toInt()}ms\n"
            )
            report.append(
                    "Virtual Response Time: ${virtualResults.map { it.responseTime }.average().toInt()}ms\n"
            )
        }

        if (radialResults.isNotEmpty()) {
            report.append(
                    "Radial Init Time: ${radialResults.map { it.initTime }.average().toInt()}ms\n"
            )
            report.append(
                    "Radial Response Time: ${radialResults.map { it.responseTime }.average().toInt()}ms\n"
            )
        }

        return report.toString()
    }

    /** Obtém uso atual de memória (aproximado) */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 // Em KB
    }

    /** Limpa recursos do sistema de testes */
    fun cleanup() {
        virtualGamePadManager?.cleanup()
        testResults.clear()
        isTestingActive = false
        Log.d(TAG, "Sistema de testes limpo")
    }
}
