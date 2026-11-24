package com.multmaster.app.util

import android.media.AudioManager
import android.media.ToneGenerator

class SoundController {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)

    fun beep(correct: Boolean) {
        runCatching {
            tone.startTone(
                if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 90
            )
        }
    }
}