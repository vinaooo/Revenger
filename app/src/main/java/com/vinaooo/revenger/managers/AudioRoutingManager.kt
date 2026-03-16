package com.vinaooo.revenger.managers

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class AudioRoutingManager(
    private val audioManager: AudioManager
) {
    private var audioFocusRequest: AudioFocusRequest? = null

    fun requestFocus(): Boolean {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                // Handle focus change if needed
            }
            .build()

        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
    }
}
