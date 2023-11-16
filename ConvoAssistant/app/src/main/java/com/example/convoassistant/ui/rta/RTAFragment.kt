package com.example.convoassistant.ui.rta


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.GoogleSpeechToTextInterface
import com.example.convoassistant.R
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.TTSInterfaceClass
import com.example.convoassistant.databinding.FragmentRtaBinding
import com.example.convoassistant.makeChatGPTRequest
import kotlin.concurrent.thread

// Real time assistant mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

class RTAFragment: Fragment(){ // () {

    private var _binding: FragmentRtaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var googleAPI: GoogleSpeechToTextInterface;
    private lateinit var ttsInterface: TTSInterfaceClass;
    private var max_tokens = 50;
    private var pre_prompt = "";

    //views
    private lateinit var outputTV: TextView;
    private lateinit var recordingB: Button;

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
        super.onViewCreated(view, savedInstanceState);

        // Set up google interface.
        googleAPI = GoogleSpeechToTextInterface(requireContext());

        //set up text to speech
        ttsInterface = TTSInterfaceClass(requireContext());

        //load settings
        val settings = SettingWrapper(requireActivity());
        max_tokens = settings.get("RTA_LLM_Output_Token_Count").toInt();
        pre_prompt = settings.get("RTA_LLM_Prompt");

        // initialize views
        outputTV =  requireView().findViewById(R.id.speech_1_text_out);
        recordingB = requireView().findViewById(R.id.toggle_recording);

        // call speech to text when button clicked
        recordingB.setOnClickListener {
            recordingButtonCallback();
        }
    }

    fun recordingButtonCallback(){
        //run in thread so we don't block main
        thread(start = true) {

            // Handle Starting the recording.
            if(!googleAPI.recording){
                // Run the following on the UI thread safely.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Recording ...";
                        // Change button text.
                        recordingB.text = "Stop Recording"
                    });
                }

                googleAPI.startRecording();

            // Handle stopping the recording.
            } else{
                googleAPI.stopRecording();

                // Run the following on the UI thread safely.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Recording stopped. Processing input...";
                        // Change button text.
                        recordingB.text = "Start Recording"
                    });
                }

                googleAPI.processRecording();

                // SST Processing failed.
                if(googleAPI.outputData.recongizedText == ""){
                    // Run the following on the UI thread safely.
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(Runnable {
                            // Display output text on screen.
                            outputTV.text = "Sorry. We could not hear you!";
                        });
                    }
                    return@thread;
                }

                // Run the following on the UI thread safely.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Speech processed. Generating reflection...";
                    });
                }

                val gptPrompt =
                    pre_prompt + " " +
                            googleAPI.outputData.recongizedText +
                            " Generate the response using user " +
                            googleAPI.outputData.lastSpeaker +
                            " words.";

                // Run the OpenAI request in a subroutine.
                val outputText = makeChatGPTRequest(gptPrompt, max_tokens);

                // Run the following on the UI thread safely.
                if (getActivity() != null) {
                    // display output text on screen
                    requireActivity().runOnUiThread(Runnable {
                        outputTV.text = outputText;
                    })

                    //text to speech
                    ttsInterface.speakOut(outputText)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();
        ttsInterface.onDestroy();
        googleAPI.onDestroy();
        _binding = null;
    }
}