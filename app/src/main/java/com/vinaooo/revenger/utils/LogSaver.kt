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

/** Utility for saving system logs with device information */
object LogSaver {

    private const val TAG = "LogSaver"

    /** Saves a complete log file with device information and system logs */
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

    /** Builds the complete log content with all information */
    private fun buildLogContent(context: Context): String {
        val builder = StringBuilder()

        // Header
        builder.append("========================================\n")
        builder.append("        REVENGER LOG REPORT\n")
        builder.append("========================================\n\n")

        // Timestamp
        builder.append(
                "Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n"
        )

        // Device Information
        builder.append("========================================\n")
        builder.append("        DEVICE INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getDeviceInfo(context))
        builder.append("\n")

        // Application Information
        builder.append("========================================\n")
        builder.append("        APPLICATION INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getAppInfo(context))
        builder.append("\n")

        // Configuration Information
        builder.append("========================================\n")
        builder.append("        CONFIGURATION INFORMATION\n")
        builder.append("========================================\n\n")

        builder.append(getConfigurationInfo(context))
        builder.append("\n")

        // System Logs
        builder.append("========================================\n")
        builder.append("        SYSTEM LOGS\n")
        builder.append("========================================\n\n")

        builder.append(getSystemLogs())

        return builder.toString()
    }

    /** Collects device information */
    private fun getDeviceInfo(context: Context): String {
        val builder = StringBuilder()

        builder.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        builder.append("Device Model: ${Build.MODEL}\n")
        builder.append("Device Brand: ${Build.BRAND}\n")
        builder.append("Device Manufacturer: ${Build.MANUFACTURER}\n")
        builder.append("Product Name: ${Build.PRODUCT}\n")
        builder.append("Hardware: ${Build.HARDWARE}\n")
        val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Build.getSerial()
            } catch (e: SecurityException) {
                "Unavailable (Permission Required)"
            }
        } else {
            @Suppress("DEPRECATION")
            Build.SERIAL
        }
        builder.append("Serial: $serial\n")
        builder.append("Board: ${Build.BOARD}\n")
        builder.append("Bootloader: ${Build.BOOTLOADER}\n")
        val supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
        builder.append("Supported ABIs: $supportedAbis\n")

        // Screen information
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            DisplayMetrics().apply {
                widthPixels = windowMetrics.bounds.width()
                heightPixels = windowMetrics.bounds.height()
                density = context.resources.displayMetrics.density
                densityDpi = context.resources.displayMetrics.densityDpi
            }
        } else {
            DisplayMetrics().apply {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(this)
            }
        }

        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.density
        val densityDpi = displayMetrics.densityDpi

        builder.append("Screen Size: ${width}x${height} pixels\n")
        builder.append("Screen Density: ${density} (${densityDpi} dpi)\n")

        // Orientation
        val orientation = context.resources.configuration.orientation
        val orientationStr =
                when (orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                    Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                    else -> "Unknown"
                }
        builder.append("Screen Orientation: $orientationStr\n")

        // Memory
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024

        builder.append(
                "Memory - Total: ${totalMemory}MB, Free: ${freeMemory}MB, Max: ${maxMemory}MB\n"
        )

        return builder.toString()
    }

    /** Collects application information */
    private fun getAppInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            builder.append("App Version: ${packageInfo.versionName} ($versionCode)\n")
            builder.append("Package Name: ${context.packageName}\n")
            builder.append(
                    "Build Type: ${if (context.packageName.contains("debug", ignoreCase = true)) "Debug" else "Release"}\n"
            )
        } catch (e: Exception) {
            builder.append("App Version: Unable to retrieve\n")
        }

        return builder.toString()
    }

    /** Collects game configuration information */
    private fun getConfigurationInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            val resources = context.resources

            // Settings from config.xml
            val configId = resources.getString(com.vinaooo.revenger.R.string.conf_id)
            val configName = resources.getString(com.vinaooo.revenger.R.string.conf_name)
            val configCore = resources.getString(com.vinaooo.revenger.R.string.conf_core)
            val configRom = resources.getString(com.vinaooo.revenger.R.string.conf_rom)

            builder.append("Game ID: $configId\n")
            builder.append("Game Name: $configName\n")
            builder.append("LibRetro Core: $configCore\n")
            builder.append("ROM File: $configRom\n")

            // Check if ROM exists
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

        // Input information
        builder.append("Input Method: ${getInputMethodInfo(context)}\n")

        return builder.toString()
    }

    /** Determines if using virtual or physical gamepad */
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

                    // Check if it's a physical gamepad
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

    /** Captures system logs using logcat */
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
