package com.vinaooo.revenger.retroview

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.Storage
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import com.swordfish.libretrodroid.ShaderConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleOwner

class RetroView(
    private val context: Context, 
    private val coroutineScope: CoroutineScope
) {
    companion object {
        var romBytes: ByteArray? = null
    }

    private val resources = context.resources
    private val storage = Storage.getInstance(context)

    private val _frameRendered = MutableLiveData(false)
    val frameRendered: LiveData<Boolean> = _frameRendered

    private val retroViewData = GLRetroViewData(context).apply {
        coreFilePath = "libcore.so"

        /* Prepare the ROM bytes */
        val romName = context.getString(R.string.config_rom)
        val romResourceId = resources.getIdentifier(romName, "raw", context.packageName)
        
        if (romResourceId == 0) {
            throw IllegalArgumentException("ROM resource '$romName' not found in raw resources. Check config.xml and ensure the file exists in res/raw/")
        }
        
        val romInputStream = context.resources.openRawResource(romResourceId)
        if (resources.getBoolean(R.bool.config_load_bytes)) {
            if (romBytes == null)
                romBytes = romInputStream.use {it.readBytes() }
            gameFileBytes = romBytes
        } else {
            if (!storage.rom.exists()) {
                storage.rom.outputStream().use {
                    romInputStream.copyTo(it)
                }
            }

            gameFilePath = storage.rom.absolutePath
        }

        shader = ShaderConfig.Sharp
        variables = getCoreVariables()

        if (storage.sram.exists()) {
            storage.sram.inputStream().use {
                saveRAMState = it.readBytes()
            }
        }
    }

    /**
     * GLRetroView instance itself
     */
    val view = GLRetroView(context, retroViewData)

    init {
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        view.layoutParams = params
    }

    /**
     * Register listener for when first frame is rendered
     */
    fun registerFrameRenderedListener() {
        coroutineScope.launch {
            view.getGLRetroEvents()
                .takeWhile { _frameRendered.value != true }
                .collect { event ->
                    if (event == GLRetroView.GLRetroEvents.FrameRendered && _frameRendered.value == false) {
                        _frameRendered.postValue(true)
                    }
                }
        }
    }

    /**
     * Parse core variables from config
     */
    private fun getCoreVariables(): Array<Variable> {
        val variables = arrayListOf<Variable>()
        val rawVariablesString = context.getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }
}