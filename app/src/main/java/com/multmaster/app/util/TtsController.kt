package com.multmaster.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var language = "en"

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        applyLanguage()
    }

    fun setLanguage(lang: String) {
        language = lang
        applyLanguage()
    }

    private fun applyLanguage() {
        if (!ready) return
        runCatching { tts.language = if (language == "hi") Locale("hi", "IN") else Locale.US }
    }

    fun speak(text: String) {
        if (!ready) return
        runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mm") }
    }

    fun speakRow(a: Int, b: Int, product: Int) {
        val text = if (language == "hi") "$a गुणा $b बराबर $product" else "$a times $b equals $product"
        speak(text)
    }

    fun stop() = runCatching { tts.stop() }

    fun shutdown() = runCatching { tts.shutdown() }
}