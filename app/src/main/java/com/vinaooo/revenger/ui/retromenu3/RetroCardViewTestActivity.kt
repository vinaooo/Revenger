package com.vinaooo.revenger.ui.retromenu3


import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.vinaooo.revenger.R

/**
 * Atividade de teste para validar a RetroCardView isoladamente. Permite testar os diferentes
 * estados visuais da view customizada.
 */
class RetroCardViewTestActivity : AppCompatActivity() {

    private lateinit var cardNormal: RetroCardView
    private lateinit var cardSelected: RetroCardView
    private lateinit var cardPressed: RetroCardView
    private lateinit var btnToggle: Button

    private var currentState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.retro_cardview_test)

        setupViews()
        setupInitialStates()
        setupClickListeners()
    }

    private fun setupViews() {
        cardNormal = findViewById(R.id.test_card_normal)
        cardSelected = findViewById(R.id.test_card_selected)
        cardPressed = findViewById(R.id.test_card_pressed)
        btnToggle = findViewById(R.id.btn_toggle_states)
    }

    private fun setupInitialStates() {
        cardNormal.setState(RetroCardView.State.NORMAL)
        cardSelected.setState(RetroCardView.State.SELECTED)
        cardPressed.setState(RetroCardView.State.PRESSED)
    }

    private fun setupClickListeners() {
        btnToggle.setOnClickListener {
            currentState = (currentState + 1) % 3
            when (currentState) {
                0 -> {
                    cardNormal.setState(RetroCardView.State.NORMAL)
                    cardSelected.setState(RetroCardView.State.SELECTED)
                    cardPressed.setState(RetroCardView.State.PRESSED)
                }
                1 -> {
                    cardNormal.setState(RetroCardView.State.SELECTED)
                    cardSelected.setState(RetroCardView.State.PRESSED)
                    cardPressed.setState(RetroCardView.State.NORMAL)
                }
                2 -> {
                    cardNormal.setState(RetroCardView.State.PRESSED)
                    cardSelected.setState(RetroCardView.State.NORMAL)
                    cardPressed.setState(RetroCardView.State.SELECTED)
                }
            }
        }

        // Teste de toque direto nos cards
        cardNormal.setOnClickListener { cardNormal.setState(RetroCardView.State.SELECTED) }

        cardSelected.setOnClickListener { cardSelected.setState(RetroCardView.State.PRESSED) }

        cardPressed.setOnClickListener { cardPressed.setState(RetroCardView.State.NORMAL) }
    }
}
