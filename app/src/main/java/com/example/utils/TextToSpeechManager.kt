package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentVoicePref = "Female 1"
    private var currentPersonality = "Helpful"

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isInitialized = true
            applyVoiceSettings(currentVoicePref, currentPersonality)
        }
    }

    fun setPreferences(voice: String, personality: String) {
        currentVoicePref = voice
        currentPersonality = personality
        applyVoiceSettings(voice, personality)
    }

    private fun applyVoiceSettings(voiceName: String, personality: String) {
        if (!isInitialized) return
        
        // Simulating voices via pitch and speech rate 
        // Female 1 (default), Female 2 (higher pitch), Male 1 (lower pitch), Male 2 (very low), Robot (monotone, slightly lower)
        var pitch = 1.0f
        var rate = 1.0f

        when (voiceName) {
            "Female 1" -> { pitch = 1.1f; rate = 1.0f }
            "Female 2" -> { pitch = 1.4f; rate = 1.0f }
            "Male 1" -> { pitch = 0.8f; rate = 0.9f }
            "Male 2" -> { pitch = 0.6f; rate = 0.8f }
            "Robot" -> { pitch = 0.5f; rate = 1.0f }
        }

        when (personality) {
            "Enthusiastic" -> { rate += 0.2f }
            "Formal" -> { rate -= 0.1f }
            "Warm" -> { pitch -= 0.1f }
        }

        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
