package com.vinaooo.revenger.gamepad

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vinaooo.revenger.R
import kotlinx.coroutines.launch

/**
 * Activity completa de testes para validação da migração RadialGamePad → VirtualJoystick
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
class ComprehensiveTestActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ComprehensiveTest"
    }

    // Sistema de testes
    private lateinit var testingSystem: GamePadTestingSystem

    // Views da UI
    private lateinit var systemSpinner: Spinner
    private lateinit var modeToggle: ToggleButton
    private lateinit var runTestsButton: Button
    private lateinit var resultsText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var leftContainer: FrameLayout
    private lateinit var rightContainer: FrameLayout

    // Estado atual
    private var currentConfig: VirtualJoystickConfig = VirtualJoystickConfig.defaultConfig()
    private var testResults: List<GamePadTestingSystem.TestResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprehensive_test)

        initializeViews()
        setupTestingSystem()
        setupControls()

        Log.d(TAG, "ComprehensiveTestActivity iniciada")
    }

    private fun initializeViews() {
        systemSpinner = findViewById(R.id.systemSpinner)
        modeToggle = findViewById(R.id.modeToggle)
        runTestsButton = findViewById(R.id.runTestsButton)
        resultsText = findViewById(R.id.resultsText)
        progressBar = findViewById(R.id.progressBar)
        leftContainer = findViewById(R.id.leftContainer)
        rightContainer = findViewById(R.id.rightContainer)
    }

    private fun setupTestingSystem() {
        // Inicializa sistema de testes
        testingSystem = GamePadTestingSystem(this, this)

        // Mock do GameActivityViewModel (para testes)
        // Em implementação real, seria injetado
        val gameActivityViewModel =
                com.vinaooo.revenger.viewmodels.GameActivityViewModel(application)
        testingSystem.initialize(gameActivityViewModel)
    }

    private fun setupControls() {
        // Configura spinner de sistemas
        val systems =
                arrayOf(
                        "Mega Drive (Sonic & Knuckles)",
                        "Super Nintendo (Rock & Roll Racing)",
                        "Game Boy (Legend of Zelda)",
                        "Master System (Sonic The Hedgehog)",
                        "Dual Analog (Teste)",
                        "Padrão"
                )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, systems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        systemSpinner.adapter = adapter

        systemSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        currentConfig =
                                when (position) {
                                    0 -> VirtualJoystickSystemConfigs.megaDriveConfig()
                                    1 -> VirtualJoystickSystemConfigs.snesConfig()
                                    2 -> VirtualJoystickSystemConfigs.gameBoyConfig()
                                    3 -> VirtualJoystickSystemConfigs.masterSystemConfig()
                                    4 -> VirtualJoystickSystemConfigs.dualAnalogConfig()
                                    else -> VirtualJoystickConfig.defaultConfig()
                                }

                        updateConfigDisplay()
                        Log.d(TAG, "Sistema selecionado: ${currentConfig.name}")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

        // Toggle entre RadialGamePad e VirtualJoystick
        modeToggle.setOnCheckedChangeListener { _, isChecked ->
            val mode =
                    if (isChecked) {
                        modeToggle.text = "VirtualJoystick"
                        GamePadTestingSystem.GamePadMode.VIRTUAL
                    } else {
                        modeToggle.text = "RadialGamePad"
                        GamePadTestingSystem.GamePadMode.RADIAL
                    }

            testingSystem.switchMode(mode, leftContainer, rightContainer, null)
            Log.d(TAG, "Modo alternado para: $mode")
        }

        // Botão de executar testes
        runTestsButton.setOnClickListener { runComprehensiveTests() }

        // Estado inicial
        modeToggle.isChecked = true // Inicia com VirtualJoystick
        updateConfigDisplay()
    }

    private fun updateConfigDisplay() {
        val info =
                """
            📋 CONFIGURAÇÃO ATUAL
            
            Sistema: ${currentConfig.name}
            Joystick Esquerdo: ${if (currentConfig.leftJoystickEnabled) "✅" else "❌"}
            Joystick Direito: ${if (currentConfig.rightJoystickEnabled) "✅" else "❌"}
            Raio: ${currentConfig.joystickRadius}px
            Sensibilidade: ${currentConfig.sensitivity}x
            Auto Visibilidade: ${if (currentConfig.autoVisibility) "✅" else "❌"}
            
            🎨 CORES:
            Fundo: #${String.format("%08X", currentConfig.backgroundColor)}
            Borda: #${String.format("%08X", currentConfig.borderColor)}
            Botão: #${String.format("%08X", currentConfig.knobColor)}
        """.trimIndent()

        resultsText.text = info
    }

    private fun runComprehensiveTests() {
        if (progressBar.visibility == View.VISIBLE) {
            Log.w(TAG, "Testes já em andamento")
            return
        }

        // UI de loading
        progressBar.visibility = View.VISIBLE
        runTestsButton.isEnabled = false
        resultsText.text =
                "🚀 Executando bateria completa de testes...\nIsto pode levar alguns minutos."

        lifecycleScope.launch {
            try {
                testingSystem.runFullTestSuite(leftContainer, rightContainer, null) { results ->
                    // Callback quando testes terminam
                    testResults = results
                    displayTestResults()

                    // UI cleanup
                    progressBar.visibility = View.GONE
                    runTestsButton.isEnabled = true

                    Log.i(TAG, "Testes concluídos: ${results.size} resultados")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro durante os testes", e)
                resultsText.text = "❌ Erro durante os testes: ${e.message}"

                progressBar.visibility = View.GONE
                runTestsButton.isEnabled = true
            }
        }
    }

    private fun displayTestResults() {
        val report = testingSystem.generateComparisonReport()
        val detailedResults = buildDetailedReport()

        val fullReport =
                """
            $report
            
            📋 RESULTADOS DETALHADOS:
            $detailedResults
            
            🎯 RECOMENDAÇÕES:
            ${generateRecommendations()}
        """.trimIndent()

        resultsText.text = fullReport

        // Salva relatório em arquivo
        saveReportToFile(fullReport)
    }

    private fun buildDetailedReport(): String {
        val report = StringBuilder()

        testResults.groupBy { it.system }.forEach { (system, results) ->
            report.append("\n🎮 $system:\n")

            results.forEach { result ->
                val status = if (result.success) "✅" else "❌"
                report.append("  $status ${result.mode.name}: ")
                report.append("Init=${result.initTime}ms, ")
                report.append("Response=${result.responseTime}ms, ")
                report.append("Memory=${result.memoryUsage}KB\n")

                if (result.notes.isNotEmpty()) {
                    report.append("    📝 ${result.notes}\n")
                }
            }
        }

        return report.toString()
    }

    private fun generateRecommendations(): String {
        val virtualResults =
                testResults.filter { it.mode == GamePadTestingSystem.GamePadMode.VIRTUAL }
        val radialResults =
                testResults.filter { it.mode == GamePadTestingSystem.GamePadMode.RADIAL }

        val recommendations = mutableListOf<String>()

        // Análise de performance
        if (virtualResults.isNotEmpty() && radialResults.isNotEmpty()) {
            val virtualAvgInit = virtualResults.map { it.initTime }.average()
            val radialAvgInit = radialResults.map { it.initTime }.average()

            if (virtualAvgInit < radialAvgInit) {
                recommendations.add("✅ VirtualJoystick tem inicialização mais rápida")
            } else {
                recommendations.add("⚠️ RadialGamePad tem inicialização mais rápida")
            }

            val virtualAvgResponse = virtualResults.map { it.responseTime }.average()
            val radialAvgResponse = radialResults.map { it.responseTime }.average()

            if (virtualAvgResponse < radialAvgResponse) {
                recommendations.add("✅ VirtualJoystick tem melhor tempo de resposta")
            }
        }

        // Análise de sucesso
        val virtualSuccessRate =
                if (virtualResults.isNotEmpty())
                        virtualResults.count { it.success }.toDouble() / virtualResults.size
                else 0.0
        val radialSuccessRate =
                if (radialResults.isNotEmpty())
                        radialResults.count { it.success }.toDouble() / radialResults.size
                else 0.0

        if (virtualSuccessRate >= 0.9) {
            recommendations.add(
                    "✅ VirtualJoystick tem alta confiabilidade (${(virtualSuccessRate * 100).toInt()}%)"
            )
        }

        // Recomendações específicas por sistema
        val systemsWithIssues = testResults.filter { !it.success }.map { it.system }.distinct()
        if (systemsWithIssues.isNotEmpty()) {
            recommendations.add(
                    "⚠️ Sistemas com problemas: ${systemsWithIssues.joinToString(", ")}"
            )
        }

        return if (recommendations.isNotEmpty()) {
            recommendations.joinToString("\n")
        } else {
            "✅ Todos os testes passaram sem problemas detectados"
        }
    }

    private fun saveReportToFile(report: String) {
        try {
            val fileName = "gamepad_test_report_${System.currentTimeMillis()}.txt"
            openFileOutput(fileName, MODE_PRIVATE).use { output ->
                output.write(report.toByteArray())
            }

            Log.i(TAG, "Relatório salvo: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar relatório", e)
        }
    }

    override fun onDestroy() {
        testingSystem.cleanup()
        super.onDestroy()
    }
}
