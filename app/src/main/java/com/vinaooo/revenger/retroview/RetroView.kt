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
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetroView(private val context: Context, private val coroutineScope: CoroutineScope) {
    companion object {
        private const val TAG = "RetroView"
        var romBytes: ByteArray? = null
    }

    private val resources = context.resources
    private val storage = Storage.getInstance(context)

    private val _frameRendered = MutableLiveData(false)
    val frameRendered: LiveData<Boolean> = _frameRendered

    private val retroViewData =
            GLRetroViewData(context).apply {
                Log.i(TAG, "🚀 STARTING RetroView initialization...")
                Log.d(TAG, "Creating GLRetroViewData...")

                coreFilePath = "libcore.so"
                Log.d(TAG, "✅ Set coreFilePath: $coreFilePath")

                /* Prepare the ROM bytes */
                val romName = context.getString(R.string.config_rom)
                Log.d(TAG, "📁 ROM name from config: '$romName'")

                val romResourceId = resources.getIdentifier(romName, "raw", context.packageName)
                Log.d(TAG, "🔍 ROM resource ID: $romResourceId")

                if (romResourceId == 0) {
                    val error =
                            "ROM resource '$romName' not found in raw resources. Check config.xml and ensure the file exists in res/raw/"
                    Log.e(TAG, "❌ FATAL ROM ERROR: $error")
                    throw IllegalArgumentException(error)
                }
                Log.d(TAG, "✅ ROM resource found successfully")

                Log.d(TAG, "📂 Opening ROM input stream...")
                val romInputStream = context.resources.openRawResource(romResourceId)
                Log.d(TAG, "✅ ROM input stream opened")

                val loadBytes = resources.getBoolean(R.bool.config_load_bytes)
                Log.d(TAG, "🔄 Load bytes mode: $loadBytes")

                if (loadBytes) {
                    Log.d(TAG, "💾 Loading ROM bytes into memory...")
                    if (romBytes == null) {
                        Log.d(TAG, "ROM bytes not cached, reading from stream...")
                        romBytes = romInputStream.use { it.readBytes() }
                        Log.d(TAG, "✅ ROM bytes loaded: ${romBytes?.size} bytes")
                    } else {
                        Log.d(TAG, "✅ Using cached ROM bytes: ${romBytes?.size} bytes")
                    }
                    gameFileBytes = romBytes
                    Log.d(TAG, "✅ Set gameFileBytes")
                } else {
                    Log.d(TAG, "💿 Using ROM file path mode...")
                    val romFile = storage.rom
                    Log.d(TAG, "ROM file path: ${romFile.absolutePath}")
                    Log.d(TAG, "ROM file exists: ${romFile.exists()}")

                    if (!romFile.exists()) {
                        Log.d(TAG, "📝 Copying ROM to storage...")
                        romFile.outputStream().use { romInputStream.copyTo(it) }
                        Log.d(TAG, "✅ ROM copied to storage: ${romFile.length()} bytes")
                    } else {
                        Log.d(TAG, "✅ Using existing ROM file: ${romFile.length()} bytes")
                    }

                    gameFilePath = romFile.absolutePath
                    Log.d(TAG, "✅ Set gameFilePath: $gameFilePath")
                }

                Log.d(TAG, "🎨 Setting shader config...")
                shader = ShaderConfig.Sharp
                Log.d(TAG, "✅ Shader set: Sharp")

                Log.d(TAG, "⚙️ Getting core variables...")
                val coreVars = getCoreVariables()
                variables = coreVars
                Log.d(TAG, "✅ Core variables set: ${coreVars.size} variables")

                Log.d(TAG, "💾 Checking for existing SRAM...")
                val sramFile = storage.sram
                Log.d(TAG, "SRAM file: ${sramFile.absolutePath}")
                Log.d(TAG, "SRAM exists: ${sramFile.exists()}")

                if (sramFile.exists()) {
                    Log.d(TAG, "📥 Loading SRAM state...")
                    sramFile.inputStream().use { saveRAMState = it.readBytes() }
                    Log.d(TAG, "✅ SRAM loaded: ${saveRAMState?.size} bytes")
                } else {
                    Log.d(TAG, "⚠️ No SRAM found, starting fresh")
                }

                Log.i(TAG, "🎯 GLRetroViewData configuration completed!")
            }

    /** GLRetroView instance itself */
    val view = GLRetroView(context, retroViewData)

    init {
        Log.i(TAG, "🔨 Initializing RetroView GLRetroView...")

        try {
            Log.d(TAG, "🎯 Creating GLRetroView with context and data...")
            // view já foi criado acima
            Log.d(TAG, "✅ GLRetroView created successfully")

            Log.d(TAG, "📐 Setting up layout parameters...")
            val params =
                    FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                    )
            params.gravity = Gravity.CENTER
            view.layoutParams = params
            Log.d(TAG, "✅ Layout parameters set: WRAP_CONTENT, CENTER gravity")

            Log.i(TAG, "🎮 RetroView initialization COMPLETED!")
        } catch (e: Exception) {
            Log.e(TAG, "💥 FATAL ERROR during RetroView init: ${e.message}", e)
            throw e
        }
    }

    /** Register listener for when first frame is rendered */
    fun registerFrameRenderedListener() {
        Log.i(TAG, "🎬 Registering frame rendered listener...")

        try {
            // Adicionar delay para garantir que a Surface está pronta
            coroutineScope.launch {
                Log.d(TAG, "⏰ Waiting 500ms for Surface to be ready...")
                delay(500)
                Log.d(TAG, "✅ Surface readiness delay completed")

                Log.d(TAG, "🔄 Forcing GLRetroView refresh on UI thread...")
                withContext(Dispatchers.Main) {
                    view.requestLayout()
                    view.invalidate()
                    Log.d(TAG, "✅ GLRetroView UI refresh completed")
                }

                Log.d(TAG, "📡 Starting GLRetroEvents listener coroutine...")

                // TIMEOUT OTIMIZADO: 2 segundos é suficiente para detectar se LibRetro funciona
                coroutineScope.launch {
                    delay(2000)
                    if (_frameRendered.value != true) {
                        Log.w(TAG, "🚨 LibRetro core não renderiza frames - usando fallback para compatibilidade")
                        _frameRendered.postValue(true)
                    }
                }

                view.getGLRetroEvents().takeWhile { _frameRendered.value != true }.collectLatest {
                        event ->
                    Log.d(TAG, "🎯 GLRetroEvent received: $event")
                    Log.d(TAG, "Current frameRendered status: ${_frameRendered.value}")

                    if (event == GLRetroView.GLRetroEvents.FrameRendered &&
                                    _frameRendered.value == false
                    ) {
                        Log.i(TAG, "🖼️ FIRST FRAME RENDERED! Setting frameRendered to true")
                        _frameRendered.postValue(true)
                    } else if (event == GLRetroView.GLRetroEvents.FrameRendered) {
                        Log.d(TAG, "🖼️ Additional frame rendered (already marked as true)")
                    } else {
                        Log.d(TAG, "📋 Other GLRetroEvent: $event")
                    }
                }

                Log.d(TAG, "⚠️ GLRetroEvents listener completed (frameRendered became true)")
            }

            Log.d(TAG, "✅ Frame rendered listener registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR registering frame rendered listener: ${e.message}", e)
        }
    }

    /** Parse core variables from config */
    private fun getCoreVariables(): Array<Variable> {
        Log.d(TAG, "🔧 Parsing core variables from config...")

        val variables = arrayListOf<Variable>()
        val rawVariablesString = context.getString(R.string.config_variables)
        Log.d(TAG, "Raw variables string: '$rawVariablesString'")

        val rawVariables = rawVariablesString.split(",")
        Log.d(TAG, "Split variables: $rawVariables (${rawVariables.size} items)")

        for ((index, rawVariable) in rawVariables.withIndex()) {
            Log.d(TAG, "Processing variable $index: '$rawVariable'")

            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2) {
                Log.w(TAG, "⚠️ Skipping malformed variable: '$rawVariable' (expected key=value)")
                continue
            }

            val key = rawVariableSplit[0].trim()
            val value = rawVariableSplit[1].trim()
            Log.d(TAG, "✅ Adding variable: '$key' = '$value'")
            variables.add(Variable(key, value))
        }

        Log.d(TAG, "🎯 Core variables parsed: ${variables.size} total")
        return variables.toTypedArray()
    }
}
