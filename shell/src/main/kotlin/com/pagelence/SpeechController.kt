package com.pagelence

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * System TTS for single words and sentences. No audio assets ship with the app.
 *
 * The controller owns audio focus; [stop] is called on page/section changes and
 * when leaving the reader. Missing English data is reported to the UI instead of
 * failing silently.
 */
class SpeechController(context: Context) {
    enum class Result { SPOKEN, NOT_READY, NO_ENGLISH_ENGINE, EMPTY }

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private var engine: TextToSpeech? = null
    private var ready = false
    private var initFailed = false
    private var englishAvailable = false
    private var focusRequest: AudioFocusRequest? = null
    private var onReady: ((Boolean) -> Unit)? = null

    init {
        engine = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            initFailed = !ready
            englishAvailable = ready && englishAvailable()
            onReady?.invoke(englishAvailable)
        }
    }

    fun setOnReadyListener(listener: (Boolean) -> Unit) {
        onReady = listener
        if (ready) listener(englishAvailable)
    }

    fun isEnglishAvailable(): Boolean = ready && englishAvailable

    fun speak(text: String, rate: Float): Result {
        val value = text.trim()
        if (value.isEmpty()) return Result.EMPTY
        val tts = engine ?: return Result.NOT_READY
        if (initFailed) return Result.NO_ENGLISH_ENGINE
        if (!ready) return Result.NOT_READY
        if (!englishAvailable) englishAvailable = englishAvailable()
        if (!englishAvailable) return Result.NO_ENGLISH_ENGINE

        val result = tts.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            englishAvailable = false
            return Result.NO_ENGLISH_ENGINE
        }
        tts.setSpeechRate(rate.coerceIn(0.7f, 1.3f))
        requestFocus()
        val spoken = tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        return if (spoken == TextToSpeech.SUCCESS) Result.SPOKEN else Result.NOT_READY
    }

    fun stop() {
        engine?.stop()
        abandonFocus()
    }

    fun shutdown() {
        stop()
        engine?.shutdown()
        engine = null
        ready = false
        englishAvailable = false
    }

    private fun englishAvailable(): Boolean {
        val tts = engine ?: return false
        val result = tts.isLanguageAvailable(Locale.US)
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    private fun requestFocus() {
        val manager = audioManager ?: return
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { focusRequest = it }
        manager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { manager.abandonAudioFocusRequest(it) }
    }

    private companion object {
        const val UTTERANCE_ID = "pagelence-speech"
    }
}
