package com.vinaooo.revenger.gamepad

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vinaooo.revenger.R
import kotlinx.coroutines.launch

/**
 * Activity de teste para validar os novos joysticks customizados
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
class VirtualJoystickTestActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VirtualJoystickTest"
    }

    private lateinit var virtualGamePad: VirtualJoystickGamePad
    private lateinit var leftJoystick: CustomJoystickView
    private lateinit var rightJoystick: CustomJoystickView
    private lateinit var infoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_virtual_joysticks)

        initializeViews()
        setupVirtualGamePad()
        setupEventListeners()

        Log.d(TAG, "VirtualJoystickTestActivity criada")
    }

    private fun initializeViews() {
        leftJoystick = findViewById(R.id.leftJoystick)
        rightJoystick = findViewById(R.id.rightJoystick)
        infoText = findViewById(R.id.joystickInfo)
    }

    private fun setupVirtualGamePad() {
        // Cria o wrapper do gamepad
        virtualGamePad = VirtualJoystickGamePad(this, this)

        // Configura com configuração padrão
        val config = VirtualJoystickConfig.defaultConfig()
        virtualGamePad.configure(config)

        // Conecta os joysticks
        virtualGamePad.attachToJoysticks(
                leftJoystick,
                null
        ) // Apenas joystick esquerdo por enquanto

        Log.d(TAG, "VirtualGamePad configurado")
    }

    private fun setupEventListeners() {
        // Observa eventos do gamepad
        lifecycleScope.launch {
            virtualGamePad.events.collect { event ->
                event?.let {
                    when (it) {
                        is GamePadEvent.AnalogStickMove -> {
                            val info =
                                    "Stick: ${it.stick}\nX: %.2f Y: %.2f".format(it.xAxis, it.yAxis)
                            infoText.text = info
                            Log.d(TAG, "Evento: $info")
                        }
                        is GamePadEvent.ButtonPress -> {
                            Log.d(
                                    TAG,
                                    "Botão: ${it.button} - ${if (it.pressed) "Pressed" else "Released"}"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        virtualGamePad.cleanup()
        super.onDestroy()
    }
}
