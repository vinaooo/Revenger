package com.vinaooo.revenger.performance

import android.os.SystemClock
import com.vinaooo.revenger.ui.retromenu3.MenuConfigurationBuilder
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.utils.MenuLogger

/**
 * Benchmarks de performance para o sistema de menu RetroMenu3. Mede tempos de renderização,
 * navegação e animações.
 */
object MenuPerformanceBenchmark {

        private const val WARMUP_ITERATIONS = 5
        private const val MEASUREMENT_ITERATIONS = 20

        data class BenchmarkResult(
                val operation: String,
                val averageTimeMs: Double,
                val minTimeMs: Long,
                val maxTimeMs: Long,
                val standardDeviation: Double,
                val iterations: Int
        )

        data class MenuBenchmarkResults(
                val menuCreationTime: BenchmarkResult,
                val navigationTime: BenchmarkResult,
                val animationTime: BenchmarkResult,
                val memoryUsage: MemoryStats,
                val timestamp: Long = System.currentTimeMillis()
        )

        data class MemoryStats(
                val allocatedMemoryKb: Long,
                val freeMemoryKb: Long,
                val totalMemoryKb: Long
        )

        /** Executa benchmark completo do sistema de menu */
        fun runCompleteMenuBenchmark(fragment: RetroMenu3Fragment): MenuBenchmarkResults {
                MenuLogger.performance("Starting complete menu benchmark")

                val menuCreationTime = benchmarkMenuCreation()
                val navigationTime = benchmarkNavigation(fragment)
                val animationTime = benchmarkAnimations(fragment)
                val memoryUsage = measureMemoryUsage()

                val results =
                        MenuBenchmarkResults(
                                menuCreationTime = menuCreationTime,
                                navigationTime = navigationTime,
                                animationTime = animationTime,
                                memoryUsage = memoryUsage
                        )

                logBenchmarkResults(results)
                return results
        }

        /** Benchmark de criação do menu */
        private fun benchmarkMenuCreation(): BenchmarkResult {
                val times = mutableListOf<Long>()

                // Warmup
                repeat(WARMUP_ITERATIONS) { RetroMenu3Fragment.newInstance() }

                // Measurement
                repeat(MEASUREMENT_ITERATIONS) {
                        val startTime = SystemClock.elapsedRealtime()
                        RetroMenu3Fragment.newInstance()
                        val endTime = SystemClock.elapsedRealtime()
                        times.add(endTime - startTime)
                }

                return calculateStats("Menu Creation", times)
        }

        /** Benchmark de navegação no menu */
        private fun benchmarkNavigation(fragment: RetroMenu3Fragment): BenchmarkResult {
                val times = mutableListOf<Long>()

                // Preparar fragment
                fragment.showMainMenu()

                // Warmup
                repeat(WARMUP_ITERATIONS) {
                        fragment.navigateDown()
                        fragment.navigateUp()
                }

                // Measurement
                repeat(MEASUREMENT_ITERATIONS) {
                        val startTime = SystemClock.elapsedRealtime()
                        fragment.navigateDown()
                        fragment.navigateUp()
                        val endTime = SystemClock.elapsedRealtime()
                        times.add(endTime - startTime)
                }

                return calculateStats("Menu Navigation", times)
        }

        /** Benchmark de animações do menu */
        private fun benchmarkAnimations(fragment: RetroMenu3Fragment): BenchmarkResult {
                val times = mutableListOf<Long>()

                // Preparar fragment
                fragment.showMainMenu()

                // Warmup
                repeat(WARMUP_ITERATIONS) {
                        fragment.hideMainMenu()
                        Thread.sleep(250) // Esperar animação
                        fragment.showMainMenu()
                        Thread.sleep(250) // Esperar animação
                }

                // Measurement
                repeat(
                        MEASUREMENT_ITERATIONS / 2
                ) { // Menos iterações pois animações são mais lentas
                        val startTime = SystemClock.elapsedRealtime()
                        fragment.hideMainMenu()
                        Thread.sleep(250) // Esperar animação completar
                        val endTime = SystemClock.elapsedRealtime()
                        times.add(endTime - startTime)

                        val startTime2 = SystemClock.elapsedRealtime()
                        fragment.showMainMenu()
                        Thread.sleep(250) // Esperar animação completar
                        val endTime2 = SystemClock.elapsedRealtime()
                        times.add(endTime2 - startTime2)
                }

                return calculateStats("Menu Animation", times)
        }

        /** Mede uso de memória */
        private fun measureMemoryUsage(): MemoryStats {
                val runtime = Runtime.getRuntime()

                // Forçar garbage collection para medição mais precisa
                System.gc()
                Thread.sleep(100)
                System.gc()

                val allocatedMemory = runtime.totalMemory() - runtime.freeMemory()
                val freeMemory = runtime.freeMemory()
                val totalMemory = runtime.totalMemory()

                return MemoryStats(
                        allocatedMemoryKb = allocatedMemory / 1024,
                        freeMemoryKb = freeMemory / 1024,
                        totalMemoryKb = totalMemory / 1024
                )
        }

        /** Calcula estatísticas dos tempos medidos */
        private fun calculateStats(operation: String, times: List<Long>): BenchmarkResult {
                val average = times.average()
                val min = times.minOrNull() ?: 0L
                val max = times.maxOrNull() ?: 0L

                // Calcular desvio padrão
                val variance = times.map { (it - average) * (it - average) }.average()
                val standardDeviation = kotlin.math.sqrt(variance)

                return BenchmarkResult(
                        operation = operation,
                        averageTimeMs = average,
                        minTimeMs = min,
                        maxTimeMs = max,
                        standardDeviation = standardDeviation,
                        iterations = times.size
                )
        }

        /** Log dos resultados do benchmark */
        private fun logBenchmarkResults(results: MenuBenchmarkResults) {
                MenuLogger.performance("=== MENU PERFORMANCE BENCHMARK RESULTS ===")
                MenuLogger.performance(
                        "Menu Creation: ${results.menuCreationTime.averageTimeMs}ms (avg)"
                )
                MenuLogger.performance(
                        "Menu Navigation: ${results.navigationTime.averageTimeMs}ms (avg)"
                )
                MenuLogger.performance(
                        "Menu Animation: ${results.animationTime.averageTimeMs}ms (avg)"
                )
                MenuLogger.performance(
                        "Memory - Allocated: ${results.memoryUsage.allocatedMemoryKb}KB, Free: ${results.memoryUsage.freeMemoryKb}KB"
                )
                MenuLogger.performance("Benchmark completed at ${results.timestamp}")
        }

        /** Compara resultados de benchmark com baseline */
        fun compareWithBaseline(
                current: MenuBenchmarkResults,
                baseline: MenuBenchmarkResults?
        ): String {
                if (baseline == null) {
                        return "No baseline available for comparison"
                }

                val comparison = StringBuilder()
                comparison.append("=== PERFORMANCE COMPARISON ===\n")

                // Comparar criação de menu
                val creationDiff =
                        current.menuCreationTime.averageTimeMs -
                                baseline.menuCreationTime.averageTimeMs
                comparison.append(
                        "Menu Creation: ${String.format("%.2f", creationDiff)}ms ${if (creationDiff > 0) "slower" else "faster"}\n"
                )

                // Comparar navegação
                val navigationDiff =
                        current.navigationTime.averageTimeMs - baseline.navigationTime.averageTimeMs
                comparison.append(
                        "Menu Navigation: ${String.format("%.2f", navigationDiff)}ms ${if (navigationDiff > 0) "slower" else "faster"}\n"
                )

                // Comparar animações
                val animationDiff =
                        current.animationTime.averageTimeMs - baseline.animationTime.averageTimeMs
                comparison.append(
                        "Menu Animation: ${String.format("%.2f", animationDiff)}ms ${if (animationDiff > 0) "slower" else "faster"}\n"
                )

                // Comparar memória
                val memoryDiff =
                        current.memoryUsage.allocatedMemoryKb -
                                baseline.memoryUsage.allocatedMemoryKb
                comparison.append(
                        "Memory Usage: ${memoryDiff}KB ${if (memoryDiff > 0) "more" else "less"} allocated\n"
                )

                return comparison.toString()
        }

        /** Benchmark de configuração customizada vs padrão */
        fun benchmarkConfigurationComparison(): BenchmarkResult {
                val times = mutableListOf<Long>()

                // Criar configuração customizada complexa
                val customConfig =
                        MenuConfigurationBuilder.create()
                                .menuId("benchmark_config")
                                .title("Benchmark Configuration")
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item1",
                                                "Item 1",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.CONTINUE
                                        )
                                )
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item2",
                                                "Item 2",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.RESET
                                        )
                                )
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item3",
                                                "Item 3",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.NAVIGATE(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .MenuState.PROGRESS_MENU
                                                        )
                                        )
                                )
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item4",
                                                "Item 4",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.NAVIGATE(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .MenuState.SETTINGS_MENU
                                                        )
                                        )
                                )
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item5",
                                                "Item 5",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.NAVIGATE(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .MenuState.EXIT_MENU
                                                        )
                                        )
                                )
                                .addItem(
                                        com.vinaooo.revenger.ui.retromenu3.MenuItem(
                                                "item6",
                                                "Item 6",
                                                action =
                                                        com.vinaooo.revenger.ui.retromenu3
                                                                .MenuAction.SAVE_LOG
                                        )
                                )
                                .build()

                // Warmup
                repeat(WARMUP_ITERATIONS) { RetroMenu3Fragment.newInstance() }

                // Measurement
                repeat(MEASUREMENT_ITERATIONS) {
                        val startTime = SystemClock.elapsedRealtime()
                        RetroMenu3Fragment.newInstance()
                        val endTime = SystemClock.elapsedRealtime()
                        times.add(endTime - startTime)
                }

                return calculateStats("Custom Configuration Creation", times)
        }
}
