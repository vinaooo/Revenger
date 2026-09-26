package com.vinaooo.revenger.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.view.WindowManager
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * Saves a report (device, app and configuration info plus the recent system log) to Downloads.
 * The report's text and formatting rules live in [LogReport]; this object only reads the values
 * from the framework and writes the file.
 */
object LogSaver {

    private const val TAG = "LogSaver"

    /** Where the app's packaged ROM lives in assets (same path `RetroView` loads it from). */
    private const val ROM_ASSET_DIR = "rom"

    private val LOGCAT_COMMAND = arrayOf("logcat", "-d", "-v", "time", "*:V")

    /**
     * Saves a complete log file with device information and system logs to the public Downloads
     * directory. Blocks on file and process I/O, so call it off the main thread.
     *
     * @return the saved file's absolute path, or null if it couldn't be written
     */
    fun saveCompleteLog(context: Context): String? =
            writeLog(
                    context = context,
                    outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    now = Date(),
                    systemLogs = { captureSystemLogs() }
            )

    /** [saveCompleteLog] with its output directory, clock and log source injectable (for tests). */
    internal fun writeLog(context: Context, outputDir: File, now: Date, systemLogs: () -> String): String? {
        return try {
            val logFile = File(outputDir, LogReport.fileName(now))
            logFile.writeText(
                    LogReport.build(
                            generatedAt = now,
                            sections =
                                    LogReport.Sections(
                                            deviceInfo = getDeviceInfo(context),
                                            appInfo = getAppInfo(context),
                                            configurationInfo = getConfigurationInfo(context),
                                            systemLogs = systemLogs()
                                    )
                    )
            )

            android.util.Log.d(TAG, "Log saved successfully to: ${logFile.absolutePath}")
            logFile.absolutePath
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Failed to save log file", e)
            null
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "Failed to save log file", e)
            null
        }
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
        val serial =
                try {
                    Build.getSerial()
                } catch (ignoredMissingSerialPermission: SecurityException) {
                    // Devices without READ_PHONE_STATE (or on newer Android, never) simply deny
                    // this call -- routine and not worth logging.
                    "Unavailable (Permission Required)"
                }
        builder.append("Serial: $serial\n")
        builder.append("Board: ${Build.BOARD}\n")
        builder.append("Bootloader: ${Build.BOOTLOADER}\n")
        builder.append("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")

        // Screen information
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = windowManager.currentWindowMetrics.bounds
        val displayMetrics = context.resources.displayMetrics
        builder.append("Screen Size: ${bounds.width()}x${bounds.height()} pixels\n")
        builder.append("Screen Density: ${displayMetrics.density} (${displayMetrics.densityDpi} dpi)\n")
        builder.append(
                "Screen Orientation: ${LogReport.orientationName(context.resources.configuration.orientation)}\n"
        )

        val runtime = Runtime.getRuntime()
        builder.append(LogReport.memoryLine(runtime.totalMemory(), runtime.freeMemory(), runtime.maxMemory()))

        return builder.toString()
    }

    /** Collects application information */
    private fun getAppInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = packageInfo.longVersionCode
            builder.append("App Version: ${packageInfo.versionName ?: "unknown"} ($versionCode)\n")
            builder.append("Package Name: ${context.packageName}\n")
            val isDebugBuild = BuildTypeDetector.isDebuggable(context)
            builder.append("Build Type: ${if (isDebugBuild) "Debug" else "Release"}\n")
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            android.util.Log.w(TAG, "Could not read package info", e)
            builder.append("App Version: Unable to retrieve\n")
        }

        return builder.toString()
    }

    /** Collects game configuration information */
    private fun getConfigurationInfo(context: Context): String {
        val builder = StringBuilder()

        try {
            // Settings from config.xml
            val configName = com.vinaooo.revenger.RevengerApplication.appConfig.getName()
            val configCore = com.vinaooo.revenger.RevengerApplication.appConfig.getCore()
            val configRom = com.vinaooo.revenger.RevengerApplication.appConfig.getRomName()

            val configId = ConfigIdGenerator.generate(configName, configCore)

            builder.append("Game ID: $configId\n")
            builder.append("Game Name: $configName\n")
            builder.append("LibRetro Core: $configCore\n")
            builder.append("ROM File: $configRom\n")

            // The build packages the ROM under assets/rom/ (RetroView loads it from there).
            try {
                context.assets.open("$ROM_ASSET_DIR/$configRom").close()
                builder.append("ROM Status: Available\n")
            } catch (e: IOException) {
                android.util.Log.w(TAG, "ROM asset not found: $configRom", e)
                builder.append("ROM Status: Not found or inaccessible\n")
            }
            // RevengerApplication.appConfig is a lateinit var; accessing it before
            // Application.onCreate() completes throws UninitializedPropertyAccessException.
        } catch (e: UninitializedPropertyAccessException) {
            android.util.Log.w(TAG, "AppConfig not yet initialized", e)
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
            val devices = mutableListOf<LogReport.InputDeviceSummary>()
            for (deviceId in inputManager.inputDeviceIds) {
                val device = inputManager.getInputDevice(deviceId) ?: continue
                devices += LogReport.InputDeviceSummary(device.name, device.sources)
            }
            LogReport.describeInputMethod(devices)
            // Some OEM input-driver implementations of InputManager/InputDevice are known to
            // throw unpredictable RuntimeExceptions for buggy virtual devices; not enumerable
            // from here, so kept broad via the escape hatch.
        } catch (expectedInputQueryFailure: Exception) {
            android.util.Log.w(TAG, "Could not determine input method", expectedInputQueryFailure)
            "Unable to determine"
        }
    }

    /**
     * Captures the recent system log by running [command] (logcat by default) and keeping its
     * last [LogReport.MAX_LOG_LINES] lines; on failure, a line saying why.
     */
    internal fun captureSystemLogs(command: Array<String> = LOGCAT_COMMAND): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val logs = process.inputStream.bufferedReader().use { it.readText() }
            LogReport.lastLines(logs)
        } catch (e: IOException) {
            "Unable to capture system logs: ${e.message ?: e.javaClass.simpleName}"
        } catch (e: SecurityException) {
            "Error capturing system logs: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
