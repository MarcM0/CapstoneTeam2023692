package com.example.convoassistant.ui.rta

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
import com.example.convoassistant.databinding.FragmentRtaBinding
import com.example.convoassistant.makeChatGPTRequest
import kotlin.concurrent.thread

// Real time assistant mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

class RTAFragment : STTFragment(){ // Fragment() { //todo put back to fragment once we do diarization

    private var _binding: FragmentRtaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    
    private lateinit var ttsInterface: TTSInterfaceClass
    private var max_tokens = 50;
    private var pre_prompt = "";

    //views
    private lateinit var outputTV: TextView
    private lateinit var micIB: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rtaViewModel =
            ViewModelProvider(this).get(RTAViewModel::class.java)

        _binding = FragmentRtaBinding.inflate(inflater, container, false)
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

        val settings = SettingWrapper(requireActivity())
        max_tokens = settings.get("RTA_LLM_Output_Token_Count").toInt()
        pre_prompt = settings.get("RTA_LLM_Prompt")

    }

    override fun onMicResult(input: String){
        //run in thread so we don't block main
        thread(start = true) {

            // Run the OpenAI request in a subroutine.
            val outputText = makeChatGPTRequest(pre_prompt+input,max_tokens)

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