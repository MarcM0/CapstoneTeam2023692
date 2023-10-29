package com.example.convoassistant.ui.practice_mode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.R
import com.example.convoassistant.STTFragment
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.TTSInterfaceClass
import com.example.convoassistant.databinding.FragmentPracticeModeBinding
import com.example.convoassistant.makeChatGPTRequest
import com.example.convoassistant.ui.practice_mode.PracticeModeViewModel
import kotlin.concurrent.thread

// Practice mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/


class PracticeModeFragment : STTFragment(){ // Fragment() {

    private var _binding: FragmentPracticeModeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var scenario_tokens = 50;
    private var rating_tokens = 50;
    private var scenarioPrompt = "";
    private var ratingPrompt = "";


    private lateinit var ttsInterface: TTSInterfaceClass


    //views
    private lateinit var outputTV: TextView
    private lateinit var micIB: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val practiceModeViewModel =
            ViewModelProvider(this).get(PracticeModeViewModel::class.java)

        _binding = FragmentPracticeModeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize views
        outputTV =  requireView().findViewById(R.id.speech_2_text_out)
        micIB = requireView().findViewById(R.id.mic_button)

        // call speech to text when button clicked
        micIB.setOnClickListener {
            callSTT()
        }

        //set up text to speech
        ttsInterface = TTSInterfaceClass(requireContext())

        //load settings
        val settings = SettingWrapper(requireActivity())

        scenario_tokens = settings.get("Pra_Scenario_LLM_Output_Token_Count").toInt()
        rating_tokens = settings.get("Pra_Rating_LLM_Output_Token_Count").toInt()
        scenarioPrompt = settings.get("Pra_Scenario_LLM_Prompt")
        ratingPrompt = settings.get("Pra_Rating_LLM_Prompt")

    }

    //runs when speech to text returns result
    override fun onMicResult(input: String){
        //run in thread so we don't block main
        thread(start = true) {

            // Run the OpenAI request in a subroutine.
            val outputText = makeChatGPTRequest(input,rating_tokens)

            // display output text on screen
            requireActivity().runOnUiThread(Runnable {
                outputTV.text = (outputText)
            })

            //text to speech
            ttsInterface.speakOut(outputText)


        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsInterface.onDestroy()
        _binding = null
    }
}