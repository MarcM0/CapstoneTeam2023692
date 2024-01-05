package com.example.convoassistant.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.R
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.databinding.FragmentSettingsBinding

//Settings interface

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private lateinit var settingsObj: SettingWrapper;

    //views
    private lateinit var View_RTA_LLM_Prompt: EditText
    private lateinit var View_RTA_LLM_Output_Token_Count: EditText
    private lateinit var View_RTA_Max_Record_Time_Count: EditText
    private lateinit var View_RTA_Max_Time_Without_Speaking_Count: EditText
    private lateinit var View_RTA_Microphone_Threshold_Count: EditText
    private lateinit var View_Pra_Scenario_LLM_Prompt: EditText
    private lateinit var View_Pra_Rating_LLM_Prompt: EditText
    private lateinit var View_Pra_Scenario_LLM_Output_Token_Count: EditText
    private lateinit var View_Pra_Rating_LLM_Output_Token_Count: EditText

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsObj = SettingWrapper(requireActivity())

        // Fetch views.
        View_RTA_LLM_Prompt = requireView().findViewById(R.id.promptBox)
        View_RTA_LLM_Output_Token_Count = requireView().findViewById(R.id.tokenCountBox)
        View_RTA_Max_Record_Time_Count = requireView().findViewById(R.id.maxRecordTimeBox)
        View_RTA_Max_Time_Without_Speaking_Count = requireView().findViewById(R.id.maxWithoutSpeakingTimeBox)
        View_RTA_Microphone_Threshold_Count = requireView().findViewById(R.id.microphoneThresholdBox)

        View_Pra_Scenario_LLM_Prompt= requireView().findViewById(R.id.Pra_Scenario_LLM_Prompt)
        View_Pra_Rating_LLM_Prompt= requireView().findViewById(R.id.Pra_Rating_LLM_Prompt)
        View_Pra_Scenario_LLM_Output_Token_Count= requireView().findViewById(R.id.Pra_Scenario_LLM_Output_Token_Count)
        View_Pra_Rating_LLM_Output_Token_Count= requireView().findViewById(R.id.Pra_Rating_LLM_Output_Token_Count)

        populateBoxes()

        //set button functions
        val defaultsButton: Button = requireView().findViewById(R.id.reset_button)
        defaultsButton.setOnClickListener {
            fixInputs()
            settingsObj.resetDefaults() //reset settings
            populateBoxes() //used so ui updates
        }

        //write settings to phone
        val saveButton: Button = requireView().findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            fixInputs()
            settingsObj.write(mapOf(
                "RTA_LLM_Prompt" to View_RTA_LLM_Prompt.getText().toString(),
                "RTA_LLM_Output_Token_Count" to View_RTA_LLM_Output_Token_Count.getText().toString(),
                "RTA_Max_Record_Time_Count" to View_RTA_Max_Record_Time_Count.getText().toString(),
                "RTA_Max_Time_Without_Speaking_Count" to View_RTA_Max_Time_Without_Speaking_Count.getText().toString(),
                "RTA_Microphone_Threshold_Count" to View_RTA_Microphone_Threshold_Count.getText().toString(),
                "Pra_Scenario_LLM_Prompt" to  View_Pra_Scenario_LLM_Prompt.getText().toString(),
                "Pra_Rating_LLM_Prompt" to  View_Pra_Rating_LLM_Prompt.getText().toString(),
                "Pra_Scenario_LLM_Output_Token_Count" to  View_Pra_Scenario_LLM_Output_Token_Count.getText().toString(),
                "Pra_Rating_LLM_Output_Token_Count" to  View_Pra_Rating_LLM_Output_Token_Count.getText().toString(),
            ))
        }

    }

    private fun fixInputs()
    {
        // Make sure numeric values are 1 or more.
        setMinNumericValue(View_RTA_LLM_Output_Token_Count);
        setMinNumericValue(View_RTA_Max_Record_Time_Count);
        setMinNumericValue(View_RTA_Max_Time_Without_Speaking_Count);
        setMinNumericValue(View_RTA_Microphone_Threshold_Count);
        setMinNumericValue(View_Pra_Scenario_LLM_Output_Token_Count);
        setMinNumericValue(View_Pra_Rating_LLM_Output_Token_Count);
    }

    fun setMinNumericValue(element: EditText){
        var tokenStr = element.getText().toString()
        if(tokenStr.equals("") || tokenStr.toInt()<1){
            element.setText("1")
        }
    }

    // populate boxes from settings
    private fun populateBoxes() {
        // Populate values.
        View_RTA_LLM_Prompt.setText(settingsObj.get("RTA_LLM_Prompt"))
        View_RTA_LLM_Output_Token_Count.setText(settingsObj.get("RTA_LLM_Output_Token_Count"))
        View_RTA_Max_Record_Time_Count.setText(settingsObj.get("RTA_Max_Record_Time_Count"))
        View_RTA_Max_Time_Without_Speaking_Count.setText(settingsObj.get("RTA_Max_Time_Without_Speaking_Count"))
        View_RTA_Microphone_Threshold_Count.setText(settingsObj.get("RTA_Microphone_Threshold_Count"))

        View_Pra_Scenario_LLM_Prompt.setText(settingsObj.get("Pra_Scenario_LLM_Prompt"))
        View_Pra_Rating_LLM_Prompt.setText(settingsObj.get("Pra_Rating_LLM_Prompt"))
        View_Pra_Scenario_LLM_Output_Token_Count.setText(settingsObj.get("Pra_Scenario_LLM_Output_Token_Count"))
        View_Pra_Rating_LLM_Output_Token_Count.setText(settingsObj.get("Pra_Rating_LLM_Output_Token_Count"))
    }


        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}