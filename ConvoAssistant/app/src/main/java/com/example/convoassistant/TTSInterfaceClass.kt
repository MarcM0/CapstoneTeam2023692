package com.example.convoassistant

// implements text to speech
// based on
// https://www.geeksforgeeks.org/how-to-convert-text-to-speech-in-android-using-kotlin/

import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import android.content.Context

class TTSInterfaceClass(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    //initialize text to speech
    init {
        Log.d("TTS", "Ran init")
        tts = TextToSpeech(context, this)
    }

    //runs when text to speech is initialized
    override fun onInit(status: Int) {

        Log.d("TTS", "Ran Oninit")
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            }
        }
    }

    // say textToSpeaak out loud
    fun speakOut(textToSpeak: String) {
        Log.d("TTS", "Ran Speakout")
        tts!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    //cleanup
    fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }
}

