package com.vinaooo.revenger.retroview

import android.annotation.SuppressLint
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
import com.vinaooo.revenger.R
import com.vinaooo.revenger.config.GameScreenInsetConfig
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.repositories.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class RetroView(private val context: Context, private val coroutineScope: CoroutineScope) {
    companion object {
        var romBytes: ByteArray? = null
    }

    private val resources = context.resources
    private val storage = Storage.getInstance(context)

    // Dynamic shader for "settings" mode
    private var _dynamicShader: String = "sharp"
    var dynamicShader: String
        get() = _dynamicShader
        set(value) {
            _dynamicShader = value
            // Apply shader in real time if in settings mode
            if (isSettingsMode()) {
                applyShaderInRealtime(value)
            }
        }

    private fun isSettingsMode(): Boolean {
        return context.getString(R.string.conf_shader).lowercase() == "settings"
    }

    /** Public method to check if shader selection is enabled */
    fun isShaderSelectionEnabled(): Boolean {
        return isSettingsMode()
    }

    private fun applyShaderInRealtime(shaderName: String) {
        val shaderConfig =
                when (shaderName) {
                    "disabled" -> ShaderConfig.Default
                    "sharp" -> ShaderConfig.Sharp
                    "crt" -> ShaderConfig.CRT
                    "lcd" -> ShaderConfig.LCD
                    else -> ShaderConfig.Sharp
                }

        // Apply shader via GLRetroView property
        view.shader = shaderConfig
        Log.i("RetroView", "Shader aplicado em tempo real: $shaderName")
    }

    /**
     * Get shader configuration from config.xml
     *
     * Maps string values from config.xml to LibretroDroid ShaderConfig enum values. Provides
     * fallback to Sharp shader for invalid configurations.
     *
     * @return ShaderConfig enum value for video rendering
     */
    private fun getShaderConfig(): ShaderConfig {
        val shaderString = context.getString(R.string.conf_shader).lowercase()

        return when (shaderString) {
            "disabled" -> {
                Log.i("RetroView", "Shader configurado: Disabled (sem shader aplicado)")
                ShaderConfig.Default
            }
            "sharp" -> {
                Log.i("RetroView", "Shader configured: Sharp (sharp bilinear filtering)")
                ShaderConfig.Sharp
            }
            "crt" -> {
                Log.i("RetroView", "Shader configured: CRT (CRT monitor simulation)")
                ShaderConfig.CRT
            }
            "lcd" -> {
                Log.i("RetroView", "Shader configurado: LCD (efeito de matriz LCD)")
                ShaderConfig.LCD
            }
            "settings" -> {
                Log.i(
                        "RetroView",
                        "Shader configurado: Settings (modo dinâmico) - usando: $_dynamicShader"
                )
                // Settings mode: use dynamic shader
                when (_dynamicShader) {
                    "disabled" -> ShaderConfig.Default
                    "sharp" -> ShaderConfig.Sharp
                    "crt" -> ShaderConfig.CRT
                    "lcd" -> ShaderConfig.LCD
                    else -> ShaderConfig.Sharp
                }
            }
            else -> {
                Log.w(
                        "RetroView",
                        "Invalid shader configuration: '$shaderString'. Using Sharp as fallback."
                )
                ShaderConfig.Sharp
            }
        }
    }

    private val _frameRendered = MutableLiveData(false)
    val frameRendered: LiveData<Boolean> = _frameRendered

    private val retroViewData =
            GLRetroViewData(context).apply {
                coreFilePath = "libcore.so"

                /* Prepare the ROM bytes */
                val romName = context.getString(R.string.conf_rom)
                @SuppressLint(
                        "DiscouragedApi"
                ) // Reflection necessary to maintain genericity - allows any ROM without
                // recompilar
                val romResourceId = resources.getIdentifier(romName, "raw", context.packageName)

                if (romResourceId == 0) {
                    throw IllegalArgumentException(
                            "ROM resource '$romName' not found in raw resources. Check config.xml and ensure the file exists in res/raw/"
                    )
                }

                val romLoadStartTime = System.currentTimeMillis()
                val romInputStream = context.resources.openRawResource(romResourceId)
                if (resources.getBoolean(R.bool.conf_load_bytes)) {
                    if (romBytes == null) romBytes = romInputStream.use { it.readBytes() }
                    gameFileBytes = romBytes
                } else {
                    // Always overwrite ROM file to ensure latest version is loaded
                    // This ensures that configuration changes in config.xml are respected
                    storage.rom.outputStream().use { romInputStream.copyTo(it) }
                    Log.i("RetroView", "ROM file updated: $romName -> ${storage.rom.absolutePath}")

                    gameFilePath = storage.rom.absolutePath
                }

                shader = getShaderConfig()
                variables = getCoreVariables()

                val sramLoadStartTime = System.currentTimeMillis()
                if (storage.sram.exists()) {
                    storage.sram.inputStream().use { saveRAMState = it.readBytes() }
                }
            }

    /** GLRetroView instance itself */
    val view: GLRetroView
    
    init {
        view = GLRetroView(context, retroViewData)
        
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
        val rawVariablesString = context.getString(R.string.conf_variables)
        val rawVariables = rawVariablesString.split(",")

        Log.d("RetroView", "Configuring core variables: '$rawVariablesString'")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2) continue

            val key = rawVariableSplit[0].trim()
            val value = rawVariableSplit[1].trim()
            variables.add(Variable(key, value))
            Log.d("RetroView", "Core variable configured: $key = $value")
        }

        Log.d("RetroView", "Total core variables configured: ${variables.size}")
        return variables.toTypedArray()
    }

    /**
     * Apply viewport configuration from XML resources.
     *
     * Reads inset configuration (gs_inset_portrait/gs_inset_landscape) and applies it to the
     * RetroView. The viewport defines the rendering area, and LibretroDroid automatically centers
     * the game within this area while maintaining native aspect ratio.
     *
     * @param isPortrait True if current orientation is portrait, false if landscape
     */
    fun applyViewportFromConfig(isPortrait: Boolean) {
        GameScreenInsetConfig.applyToRetroView(view, resources, isPortrait)
        Log.d(
                "RetroView",
                "Viewport configuration applied for ${if (isPortrait) "portrait" else "landscape"} orientation"
        )
    }
}
