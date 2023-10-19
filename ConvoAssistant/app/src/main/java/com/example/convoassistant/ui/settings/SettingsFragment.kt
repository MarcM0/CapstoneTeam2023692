package com.example.convoassistant.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.R
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private lateinit var settingsObj: SettingWrapper;

    //views
    private lateinit var View_RTA_LLM_Prompt: EditText
    private lateinit var View_RTA_LLM_Output_Token_Count: EditText
    private lateinit var View_Voice: Spinner

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

        populateBoxes()

        //set button functions
        val defaultsButton: Button = requireView().findViewById(R.id.reset_button)
        defaultsButton.setOnClickListener {
            settingsObj.resetDefaults() //reset settings
            fixInputs()
            populateBoxes() //used so ui updates
        }

        //write settings to phone
        val saveButton: Button = requireView().findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            fixInputs()
            settingsObj.write(mapOf(
                "RTA_LLM_Prompt" to View_RTA_LLM_Prompt.getText().toString(),
                "RTA_LLM_Output_Token_Count" to View_RTA_LLM_Output_Token_Count.getText().toString(),
                "Voice" to View_Voice.getSelectedItem().toString(),
            ))
        }

    }

    //fix app breaking inputs
    private fun fixInputs()
    {
        //num tokens must be 1 or higher
        val numTokens = View_RTA_LLM_Output_Token_Count.getText().toString()
        if((numTokens.equals("")) || (numTokens.toInt()<1) ){
            View_RTA_LLM_Output_Token_Count.setText("1")
        }
    }

    // populate boxes from settings
    private fun populateBoxes() {
        View_RTA_LLM_Prompt = requireView().findViewById(R.id.promptBox)
        View_RTA_LLM_Output_Token_Count = requireView().findViewById(R.id.tokenCountBox)
        View_Voice = requireView().findViewById(R.id.VoiceSelection)

        View_RTA_LLM_Prompt.setText(settingsObj.get("RTA_LLM_Prompt"))
        View_RTA_LLM_Output_Token_Count.setText(settingsObj.get("RTA_LLM_Output_Token_Count"))

        setSpinners()
    }

    private fun setSpinners()
    {
        //set spinner items
        var voiceStrList = ""
        try{
            voiceStrList = settingsObj.get("Voice_Options")
        }
        catch(e: Exception) {}

        val voiceList = voiceStrList.split(",").map { it.trim() }
        val optionsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_item, voiceList.toList())
        optionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        View_Voice.setAdapter(optionsAdapter)

        //set spinner selection
        var voiceSet = false;
        var targetVoice = settingsObj.get("Voice")
        if(!targetVoice.equals(""))
        {
            try {
                View_Voice.setSelection(optionsAdapter.getPosition(targetVoice));
                voiceSet = true
            }
            catch (ex: Exception){}

        }

        if(!voiceSet)
        {
            targetVoice = View_Voice.getItemAtPosition(0).toString()
            settingsObj.write("Voice", targetVoice)
            View_Voice.setSelection(0)
        }
    }


        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}