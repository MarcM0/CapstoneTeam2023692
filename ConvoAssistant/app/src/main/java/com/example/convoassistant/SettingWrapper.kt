package com.example.convoassistant

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.example.convoassistant.databinding.ActivityMainBinding

//wrapper class for saving and loading settings
class SettingWrapper(activity: Activity) {
    private val sharedPref: SharedPreferences
    init {
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
    }
    private val defaultRTAPrompt = "The following is an interaction between you and a user. You are a therapist and the user is someone having smoking issues. Give a SHORT reflection to the user's response. The reflection must be a plausible guess or assumption about the user's underlying emotions, values, or chain of thought. The reflection must be very short. The reflection must be a statement and not a question. Don't always use \"it seems like\" or \"it sounds like\" or \"you\" at the begining. Don't always use the phrase \"important to you\" or \"important for you\". "
    //DEFAULTS, all must be strings
    private val defaultVals = mapOf(
                                "Voice" to "default", //todo voice for client
                                "Voice_Options" to "", //todo, list of all voices for scroll menu
                                "RTA_LLM_Prompt" to "Answer the following in one sentence. Never decline to answer the question or mention that you are an AI model. ", //defaultRTAPrompt
                                "RTA_LLM_Output_Token_Count" to "50",
                                )

    //gets value associated with key
    fun get(key: String): String{
        return sharedPref.getString(key, defaultVals[key].toString())?: defaultVals[key].toString()
    }

    //restore default settings
    fun resetDefaults(){
        write(defaultVals)
    }

    // write value to entry associated with key
    // for all pairs in map
    fun write(pairs: Map<String,String>){
        with (sharedPref.edit()) {
            pairs.forEach { (key, value) -> putString(key, value); }
            commit()
        }
    }

}