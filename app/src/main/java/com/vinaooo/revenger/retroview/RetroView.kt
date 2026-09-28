package com.vinaooo.revenger.retroview

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.Variable
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.utils.ShaderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class RetroView(
    private val context: Context, 
    private val coroutineScope: CoroutineScope,
    private val appConfig: AppConfig
) {
    private val resources = context.resources
    private val storage = Storage.getInstance(context)

    // Dynamic shader is now always available for user selection
    private var _dynamicShader: String = "disabled"
    var dynamicShader: String
        get() = _dynamicShader
        set(value) {
            _dynamicShader = value
            // Always apply shader in real time (shader selection always enabled)
            applyShaderInRealtime(value)
        }

    private fun applyShaderInRealtime(shaderName: String) {
        // Apply shader via GLRetroView property; an unknown name falls back to Sharp
        view.shader = ShaderType.fromConfigName(shaderName)?.toShaderConfig() ?: ShaderConfig.Sharp
        Log.i("RetroView", "Shader aplicado em tempo real: $shaderName")
    }

    /**
     * Get shader configuration from the app config
     *
     * Maps the configured shader name to its LibretroDroid [ShaderConfig] through [ShaderType].
     * Provides fallback to Sharp shader for invalid configurations.
     *
     * @return ShaderConfig value for video rendering
     */
    private fun getShaderConfig(): ShaderConfig {
        val shaderString = appConfig.getShader()
        val shaderType = ShaderType.fromConfigName(shaderString)
        if (shaderType == null) {
            Log.w("RetroView", "Invalid shader configuration: '$shaderString'. Using Sharp as fallback.")
            return ShaderConfig.Sharp
        }
        Log.i("RetroView", "Shader configured: ${shaderType.displayName}")
        return shaderType.toShaderConfig()
    }

    private val _frameRendered = MutableLiveData(false)
    val frameRendered: LiveData<Boolean> = _frameRendered

    private val retroViewData =
            GLRetroViewData(context).apply {
                coreFilePath = "libcore.so"

                /* Prepare the ROM bytes */
                val romName = appConfig.getRomName()

                // Load ROM from assets/rom/ (faster builds — assets bypass AAPT2 processing)
                val romAssetPath = "rom/$romName"
                val romInputStream =
                        try {
                            context.assets.open(romAssetPath)
                        } catch (e: java.io.FileNotFoundException) {
                            throw IllegalArgumentException(
                                    "ROM '$romName' not found in assets/rom/. Ensure the file " +
                                            "exists at app/src/main/assets/rom/$romName",
                                    e
                            )
                        }

                // Always overwrite ROM file to storage to ensure latest version is loaded
                romInputStream.use { input ->
                    storage.rom.outputStream().use { output -> input.copyTo(output) }
                }
                Log.i("RetroView", "ROM file updated: $romName -> ${storage.rom.absolutePath}")

                gameFilePath = storage.rom.absolutePath

                shader = getShaderConfig()
                variables = getCoreVariables()

                if (storage.sram.exists()) {
                    storage.sram.inputStream().use { saveRAMState = it.readBytes() }
                }
            }

    /** GLRetroView instance itself */
    val view: GLRetroView

    init {
        view = GLRetroView(context, retroViewData)
        view.preserveEGLContextOnPause = true

        val params =
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                )
        params.gravity = Gravity.CENTER
        view.layoutParams = params

        // FIX: Initialize frameSpeed = 1 to ensure emulation starts running
        // Without this, LibretroDroid may start paused (frameSpeed = 0) causing black screen
        view.frameSpeed = 1
    }

    /** Register listener for when first frame is rendered */
    fun registerFrameRenderedListener() {
        coroutineScope.launch {
            view.getGLRetroEvents().takeWhile { _frameRendered.value != true }.collectLatest { event
                ->
                if (event == GLRetroView.GLRetroEvents.FrameRendered &&
                                _frameRendered.value == false
                ) {
                    _frameRendered.postValue(true)
                }
            }
        }
    }

    /** Register listener for frame rendering events to track FPS */
    fun registerFrameCallback() {
        coroutineScope.launch {
            view.getGLRetroEvents().collectLatest { event ->
                if (event == GLRetroView.GLRetroEvents.FrameRendered) {
                    AdvancedPerformanceProfiler.onFrameRendered()
                }
            }
        }
    }

    /** Parse core variables from config */
    private fun getCoreVariables(): Array<Variable> {
        val variables = arrayListOf<Variable>()
        val rawVariablesString = appConfig.getVariables()
        val rawVariables = rawVariablesString.split(",")

        Log.d("RetroView", "Configuring core variables: '$rawVariablesString'")

        for (rawVariable in rawVariables) {
            // limit = 2 so a value that itself contains "=" (e.g. a base64-encoded core
            // option) is kept intact instead of being silently dropped.
            val rawVariableSplit = rawVariable.split("=", limit = 2)
            if (rawVariableSplit.size != 2) {
                if (rawVariable.isNotBlank()) {
                    Log.w("RetroView", "Skipping malformed core variable entry: '$rawVariable'")
                }
                continue
            }

            val key = rawVariableSplit[0].trim()
            val value = rawVariableSplit[1].trim()
            variables.add(Variable(key, value))
            Log.d("RetroView", "Core variable configured: $key = $value")
        }

        Log.d("RetroView", "Total core variables configured: ${variables.size}")
        return variables.toTypedArray()
    }

    fun resume() {
        view.onResume()
    }

    fun pause() {
        view.onPause()
    }

    fun destroy() {
        view.onDestroy()
    }
}
