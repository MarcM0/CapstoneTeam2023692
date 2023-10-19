package com.example.convoassistant

// implements text to speech
// based on
// https://www.geeksforgeeks.org/how-to-convert-text-to-speech-in-android-using-kotlin/

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlin.concurrent.thread


class TTSInterfaceClass(context: Context, voice: String) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    val voiceStr: String
    init {
        voiceStr = voice
    }

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

            //set voice if requested
            if(!voiceStr.equals("")){
                for (tmpVoice in tts!!.voices) {
                    if (tmpVoice.name == voiceStr) {
                        tts!!.setVoice(tmpVoice)
                        break
                    }
                }
            }


            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            }
        }
    }

    // say textToSpeak out loud
    fun speakOut(textToSpeak: String) {
        Log.d("TTS", "Ran Speakout")
        tts!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    //returns populate settings wrapper with voice options)
    fun populateVoicesOptions(settings: SettingWrapper) {
        thread(start = true) {
            var voice_list = ""

            //wait for voice value
            var voices = tts!!.getVoices()
            while (voices == null) {
                Thread.sleep(250)
                voices = tts!!.getVoices()
            }
            for (voice in voices) {
                if(voice.locale == Locale.US){
                    voice_list += voice.name+","
                }
            }

            //populate setting
            settings.write("Voice_Options", voice_list)
        }
    }

    //cleanup
    fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }
}

