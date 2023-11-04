package com.example.convoassistant.ui.practice_mode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Button
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
import android.text.method.ScrollingMovementMethod

// Practice mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/


class PracticeModeFragment : STTFragment(){ // Fragment() {

    private var _binding: FragmentPracticeModeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var scenarioTokens = 50;
    private var ratingTokens = 50;
    private var scenarioPrompt = "";
    private var ratingPrompt = "";

    private var currentPracticeScenario = "";

    private lateinit var ttsInterface: TTSInterfaceClass


    //views
    private lateinit var outputTV: TextView
    private lateinit var micIB: ImageButton
    private lateinit var generatePromptB: Button

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

        generatePromptB = requireView().findViewById(R.id.new_practice_prompt)

        outputTV.movementMethod = ScrollingMovementMethod()

        // call speech to text when button clicked
        micIB.setOnClickListener {
            callSTT()
        }
        // Disable the mic button if there currently isn't a scenario.
        checkIfResponseButtonShouldBeEnabled()

        generatePromptB.setOnClickListener{
            generatePracticePrompt()
        }
        //set up text to speech
        ttsInterface = TTSInterfaceClass(requireContext())

        //load settings
        val settings = SettingWrapper(requireActivity())

        scenarioPrompt = settings.get("Pra_Scenario_LLM_Prompt")
        scenarioTokens = settings.get("Pra_Scenario_LLM_Output_Token_Count").toInt()

        ratingPrompt = settings.get("Pra_Rating_LLM_Prompt")
        ratingTokens = settings.get("Pra_Rating_LLM_Output_Token_Count").toInt()
    }

    //runs when speech to text returns result
    override fun onMicResult(input: String){
        //run in thread so we don't block main
        thread(start = true) {
            val combinedInput = ratingPrompt +
                                "\nOrignal Statment:\"" + currentPracticeScenario +"\"" +
                                "\nResponse:\"" + input +"\""

            // Run the OpenAI request in a subroutine.
            val outputText = makeChatGPTRequest(combinedInput, ratingTokens)

            // Clear the current scenario.
            currentPracticeScenario = ""


            // display output text on screen
            /** check if activity still exist */
            if (getActivity() != null) {
                requireActivity().runOnUiThread(Runnable {
                    checkIfResponseButtonShouldBeEnabled()
                    outputTV.text = (outputText)
                })

                //text to speech
                ttsInterface.speakOut(outputText)
            }

        }
    }

    // Callback for the new scenario prompt button.
    fun generatePracticePrompt(){

        // Display a loading message.
        outputTV.text = "Generating Practice Scenario..."

        // Clear the existing scenario.
        currentPracticeScenario = ""
        checkIfResponseButtonShouldBeEnabled()

        // Disable the generate prompt button.
        generatePromptB.isEnabled = false;

        // run in thread so we don't block main
        thread(start = true) {
            // Run the OpenAI request in a subroutine.
            currentPracticeScenario = makeChatGPTRequest(scenarioPrompt, scenarioTokens)

            /** check if activity still exist */
            if (getActivity() != null) {

                requireActivity().runOnUiThread(Runnable {
                    // display output text on screen
                    outputTV.text = (currentPracticeScenario)
                    // Re-enable the mic and new prompt buttons after generating a scenario.
                    checkIfResponseButtonShouldBeEnabled()
                    generatePromptB.isEnabled = true;
                })

                //text to speech
                ttsInterface.speakOut(currentPracticeScenario)
            }
        }
    }

    // Enables or disables the mic button.
    fun checkIfResponseButtonShouldBeEnabled(){
        micIB.isEnabled = currentPracticeScenario.isNotEmpty()
        if(micIB.isEnabled) {
            micIB.visibility = View.VISIBLE
        } else {
            micIB.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsInterface.onDestroy()
        _binding = null
    }
}