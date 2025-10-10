package com.vinaooo.revenger.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utilitário para salvar logs do sistema com informações do dispositivo */
object LogSaver {

    private const val TAG = "LogSaver"

    /** Salva um arquivo de log completo com informações do dispositivo e logs do sistema */
    fun saveCompleteLog(context: Context): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "revenger_log_$timestamp.txt"

            val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadDir, filename)

            val logContent = buildLogContent(context)

            logFile.writeText(logContent)

            android.util.Log.d(TAG, "Log saved successfully to: ${logFile.absolutePath}")
            logFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save log file", e)
            null
        }
    }

    /** Constrói o conteúdo completo do log com todas as informações */
    private fun buildLogContent(context: Context): String {
        val builder = StringBuilder()

        // Cabeçalho
        builder.append("========================================\n")
        builder.append("        REVENGER LOG REPORT\n")
        builder.append("========================================\n\n")

        // Timestamp
        builder.append(
                "Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n"
        )

        // Informações do Dispositivo
        builder.append("========================================\n")
        builder.append("        DEVICE INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getDeviceInfo(context))
        builder.append("\n")

        // Informações da Aplicação
        builder.append("========================================\n")
        builder.append("        APPLICATION INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getAppInfo(context))
        builder.append("\n")

        // Informações de Configuração
        builder.append("========================================\n")
        builder.append("        CONFIGURATION INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getConfigurationInfo(context))
        builder.append("\n")

        // Logs do Sistema
        builder.append("========================================\n")
        builder.append("        SYSTEM LOGS\n")
        builder.append("========================================\n\n")

        builder.append(getSystemLogs())

        return builder.toString()
    }

    /** Coleta informações do dispositivo */
    private fun getDeviceInfo(context: Context): String {
        val builder = StringBuilder()

        builder.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        builder.append("Device Model: ${Build.MODEL}\n")
        builder.append("Device Brand: ${Build.BRAND}\n")
        builder.append("Device Manufacturer: ${Build.MANUFACTURER}\n")
        builder.append("Product Name: ${Build.PRODUCT}\n")
        builder.append("Hardware: ${Build.HARDWARE}\n")
        builder.append("Serial: ${Build.SERIAL}\n")
        builder.append("Board: ${Build.BOARD}\n")
        builder.append("Bootloader: ${Build.BOOTLOADER}\n")
        builder.append("CPU ABI: ${Build.CPU_ABI}\n")
        builder.append("CPU ABI2: ${Build.CPU_ABI2}\n")

        // Informações de tela
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.density
        val densityDpi = displayMetrics.densityDpi

        builder.append("Screen Size: ${width}x${height} pixels\n")
        builder.append("Screen Density: ${density} (${densityDpi} dpi)\n")

        // Orientação
        val orientation = context.resources.configuration.orientation
        val orientationStr =
                when (orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                    Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                    else -> "Unknown"
                }
        builder.append("Screen Orientation: $orientationStr\n")

        // Memória
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024

        builder.append(
                "Memory - Total: ${totalMemory}MB, Free: ${freeMemory}MB, Max: ${maxMemory}MB\n"
        )

        return builder.toString()
    }

    /** Coleta informações da aplicação */
    private fun getAppInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            builder.append("App Version: ${packageInfo.versionName} (${packageInfo.versionCode})\n")
            builder.append("Package Name: ${context.packageName}\n")
            builder.append(
                    "Build Type: ${if (context.packageName.contains("debug", ignoreCase = true)) "Debug" else "Release"}\n"
            )
        } catch (e: Exception) {
            builder.append("App Version: Unable to retrieve\n")
        }

        return builder.toString()
    }

    /** Coleta informações de configuração do jogo */
    private fun getConfigurationInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            val resources = context.resources

            // Configurações do config.xml
            val configId = resources.getString(com.vinaooo.revenger.R.string.config_id)
            val configName = resources.getString(com.vinaooo.revenger.R.string.config_name)
            val configCore = resources.getString(com.vinaooo.revenger.R.string.config_core)
            val configRom = resources.getString(com.vinaooo.revenger.R.string.config_rom)

            builder.append("Game ID: $configId\n")
            builder.append("Game Name: $configName\n")
            builder.append("LibRetro Core: $configCore\n")
            builder.append("ROM File: $configRom\n")

            // Verificar se ROM existe
            try {
                val romStream =
                        resources.openRawResource(
                                resources.getIdentifier(configRom, "raw", context.packageName)
                        )
                romStream.close()
                builder.append("ROM Status: Available\n")
            } catch (e: Exception) {
                builder.append("ROM Status: Not found or inaccessible\n")
            }
        } catch (e: Exception) {
            builder.append("Configuration: Unable to retrieve\n")
        }

        // Informações de input
        builder.append("Input Method: ${getInputMethodInfo(context)}\n")

        return builder.toString()
    }

    /** Determina se está usando gamepad virtual ou físico */
    private fun getInputMethodInfo(context: Context): String {
        return try {
            val inputManager =
                    context.getSystemService(Context.INPUT_SERVICE) as
                            android.hardware.input.InputManager
            val inputDevices = inputManager.inputDeviceIds

            var hasPhysicalGamepad = false
            var hasVirtualGamepad = false

            for (deviceId in inputDevices) {
                val device = inputManager.getInputDevice(deviceId)
                if (device != null) {
                    val sources = device.sources

                    // Verificar se é um gamepad físico
                    if (sources and android.view.InputDevice.SOURCE_GAMEPAD != 0 ||
                                    sources and android.view.InputDevice.SOURCE_JOYSTICK != 0
                    ) {
                        if (!device.name.contains("virtual", ignoreCase = true)) {
                            hasPhysicalGamepad = true
                        } else {
                            hasVirtualGamepad = true
                        }
                    }
                }
            }

            when {
                hasPhysicalGamepad && hasVirtualGamepad -> "Physical + Virtual Gamepad"
                hasPhysicalGamepad -> "Physical Gamepad"
                hasVirtualGamepad -> "Virtual Gamepad"
                else -> "Touch/Other Input"
            }
        } catch (e: Exception) {
            "Unable to determine"
        }
    }

    /** Captura logs do sistema usando logcat */
    private fun getSystemLogs(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v time *:V")
            val inputStream = process.inputStream
            val logs = inputStream.bufferedReader().use { it.readText() }

            // Filtrar apenas logs relevantes (últimas 1000 linhas para não ficar muito grande)
            val lines = logs.lines()
            val relevantLines = lines.takeLast(1000)

            relevantLines.joinToString("\n")
        } catch (e: IOException) {
            "Unable to capture system logs: ${e.message}"
        } catch (e: Exception) {
            "Error capturing system logs: ${e.message}"
        }
    }
}
