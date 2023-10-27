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
    //DEFAULTS, all must be strings
    private val defaultVals = mapOf(
                                "RTA_LLM_Prompt" to "", //todo set to defaultRTAPrompt
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

    //write single setting, Slow, avoid using
    fun writeSingle(key: String, value: String){
        with (sharedPref.edit()) {
            putString(key, value)
            commit()
        }
    }

}