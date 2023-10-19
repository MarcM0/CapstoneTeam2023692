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

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private lateinit var settingsObj: SettingWrapper;

    //views
    private lateinit var View_RTA_LLM_Prompt: EditText
    private lateinit var View_RTA_LLM_Output_Token_Count: EditText

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
            populateBoxes() //used so ui updates
        }

        //write settings to phone
        val saveButton: Button = requireView().findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            settingsObj.write(mapOf(
                "RTA_LLM_Prompt" to View_RTA_LLM_Prompt.getText().toString(),
                "RTA_LLM_Output_Token_Count" to View_RTA_LLM_Output_Token_Count.getText().toString(),
            ))
        }

    }

    // populate boxes from settings
    private fun populateBoxes() {
        View_RTA_LLM_Prompt = requireView().findViewById(R.id.promptBox)
        View_RTA_LLM_Output_Token_Count = requireView().findViewById(R.id.tokenCountBox)

        View_RTA_LLM_Prompt.setText(settingsObj.get("RTA_LLM_Prompt"))
        View_RTA_LLM_Output_Token_Count.setText(settingsObj.get("RTA_LLM_Output_Token_Count"))
    }


        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}