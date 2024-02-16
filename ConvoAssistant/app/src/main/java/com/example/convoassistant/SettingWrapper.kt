package com.example.convoassistant

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

//wrapper class for saving and loading settings
//also keeps track of streaks
class SettingWrapper(activity: Activity) {
    private val sharedPref: SharedPreferences
    init {
        sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
    }
    //DEFAULTS, all must be strings
    private val defaultVals = mapOf(
                                "RTA_LLM_Prompt" to NewReflectPrompt,
                                "RTA_LLM_Output_Token_Count" to "50",
                                "RTA_Max_Record_Time_Count" to "35000",
                                "RTA_Max_Time_Without_Speaking_Count" to "3000",
                                "RTA_Microphone_Threshold_Count" to "700",
                                "Pra_Scenario_LLM_Prompt" to  scenarioPrompt,
                                "Pra_Rating_LLM_Prompt" to  NewRatePrompt,
                                "Pra_Scenario_LLM_Output_Token_Count" to  "70",
                                "Pra_Rating_LLM_Output_Token_Count" to  "100",
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
    fun writeInts(pairs: Map<String,Int>){
        with (sharedPref.edit()) {
            pairs.forEach { (key, value) -> putInt(key, value); }
            commit()
        }
    }

    //get streak status and increment if necessary
    //only increment if allowIncrement is true
    // will reset streak whether or not allowIncrement is true
    //returns current streak
    fun getStreakPracticeMode(allowIncrement: Boolean): Int{

        //get date
        //today
        val today: Calendar = Calendar.getInstance()
        val dayOfYear = today.get(Calendar.DAY_OF_YEAR)
        val year = today.get(Calendar.YEAR)
        //yesterday
        val yesterday = Calendar.getInstance() // today
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        Log.i("StreakUpdate", "DayOfYear:$today , Year:$year")

        val streakKey = "practice_mode_streak";
        val streakDayKey = "practice_mode_streak_day";
        val streakYearKey = "practice_mode_streak_year";

        var streak = sharedPref.getInt(streakKey, -1);
        var streakDay = sharedPref.getInt(streakDayKey, dayOfYear)
        var streakYear = sharedPref.getInt(streakYearKey, year)

        var noChange = false;
        if(streak == -1){
            //no previously recorded streak
            streak = 0;
        }else if((streakDay == dayOfYear) && (streakYear == year)){
            //no change, same day
            noChange = true;
        }else if(streakYear == yesterday.get(Calendar.YEAR) && streakDay == yesterday.get(Calendar.DAY_OF_YEAR)) {
            //yesterday and we are allowed to increment
            if(allowIncrement){
                streak+=1
            }else{
                noChange = true;
            }
        }else{
            //not today or yesterday, lost streak (unless you are a time traveller :) )
            streak=0
        }

        if(!noChange){
            var newDayOfYear = dayOfYear;
            var newYear = year
            if(!allowIncrement){
                //make sure this day doesnt count towards streak if not incrementing
                newDayOfYear = 0
                newYear = 0
            }

            writeInts(mapOf(
                streakKey to streak,
                streakDayKey to newDayOfYear,
                streakYearKey to newYear,
            ))
        }

        return streak
    }

    //write single setting, Slow, avoid using
    fun writeSingle(key: String, value: String){
        with (sharedPref.edit()) {
            putString(key, value)
            commit()
        }
    }

}