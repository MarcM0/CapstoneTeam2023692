package com.example.convoassistant.ui.rta

import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ScheduledFuture
import kotlin.concurrent.thread

// Real time assistant mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

class RTAFragment: Fragment(){ 

    private var _binding: FragmentRtaBinding? = null
    private lateinit  var autoStopRequest: ScheduledFuture<*>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit private var googleAPI: GoogleSpeechToTextInterface;
    private lateinit var ttsInterface: TTSInterfaceClass;
    private var max_tokens = 50;
    private var pre_prompt = "";

    //views
    private lateinit var outputTV: TextView;
    private lateinit var recordingB: Button;
    private lateinit var testRTA: Button;

    // App settings.
    private lateinit var settings: SettingWrapper;

    // Recording managing thread.
    private var recordingBackgroundJob: Job? = null;

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
        settings = SettingWrapper(requireActivity());
        max_tokens = settings.get("RTA_LLM_Output_Token_Count").toInt();
        pre_prompt = settings.get("RTA_LLM_Prompt");

        // initialize views
        outputTV =  requireView().findViewById(R.id.speech_1_text_out);
        recordingB = requireView().findViewById(R.id.toggle_recording);

        testRTA = requireView().findViewById(R.id.rta_test);

        testRTA.setOnClickListener {
            testRTAMode();
        }

        // call speech to text when button clicked
        recordingB.setOnClickListener {
            recordingButtonCallback();
        }
    }

    fun recordingButtonCallback(isAutoStop: Boolean = false){
        // Handle Starting the recording.
        if (!googleAPI.recording) {
            // Run in thread so we don't block main.
            thread(start = true) { try {
                // Clean any recording threads.
                if(recordingBackgroundJob!=null){
                    recordingBackgroundJob?.cancel();
                    recordingBackgroundJob = null;
                }

                // Run the following on the UI thread safely.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Recording ...";
                        // Change button text.
                        recordingB.text = "Stop Recording"
                    });
                }

                googleAPI.startRecording(this.requireActivity());

                // Create a thread.
                recordingBackgroundJob = startRecordingBackgroundThread();

            } catch(e: Exception) {
                Log.e("Error", e.toString());
                try {
                    requireActivity().runOnUiThread(Runnable {
                        recordingB.text = "Start Recording"
                        outputTV.text =
                            "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                    });
                } catch (e: Exception) {
                    Log.e("Error", e.toString());
                }
            }}
        } else {
            stopRecording();
        }
    }

    fun stopRecording(){
        thread(start = true) { try {
            // Handle ending the recording.
            googleAPI.stopRecording();

            // Clean the external thread.
            if(recordingBackgroundJob!=null){
                recordingBackgroundJob?.cancel();
                recordingBackgroundJob = null;
            }

            // Run the following on the UI thread safely.
            if (getActivity() != null) {
                requireActivity().runOnUiThread(Runnable {
                    // Display output text on screen.
                    outputTV.text = "Recording stopped. Processing input...";
                    // Change button text.
                    recordingB.text = "Start Recording"
                });
            }

            googleAPI.processRecording(null);

            // SST Processing failed.
            if (googleAPI.outputData.recongizedText == "") {
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
                        googleAPI.outputData.recongizedText
            //                            " Generate the response using user " +
            //                            googleAPI.outputData.lastSpeaker +
            //                            " words.";

            // Run the OpenAI request in a subroutine.
            val outputText = makeChatGPTRequest(gptPrompt, max_tokens, temperature = 1.1);

            // Run the following on the UI thread safely.
            if (getActivity() != null) {
                // display output text on screen
                requireActivity().runOnUiThread(Runnable {
                    outputTV.text = outputText;
                });
            }

            // Text to speech.
            ttsInterface.speakOut(outputText);
        } catch(e: Exception) {
            Log.e("Error", e.toString());
            try {
                requireActivity().runOnUiThread(Runnable {
                    recordingB.text = "Start Recording"
                    outputTV.text =
                        "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                });
            } catch (e: Exception) {
                Log.e("Error", e.toString());
            }
        }}
    }

    fun startRecordingBackgroundThread(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis();
            var timeLastSpoke: Long = -1;
            while (googleAPI.recording) {

                // Timeout after 50 seconds.
                val currentTime = System.currentTimeMillis();
                val elapsedTime = currentTime - startTime;
                if (elapsedTime >= settings.get("RTA_Max_Record_Time_Count").toLong()) {
                    stopRecording();
                    break;
                }

                // Check current mic levels.
                val currentMicAmp = googleAPI.getMicAmplitude();
                if(currentMicAmp >= settings.get("RTA_Microphone_Threshold_Count").toInt()){
                    timeLastSpoke = currentTime;
                }

                // Timeout after not speaking for a while.
                if(timeLastSpoke > 0 && (currentTime - timeLastSpoke) >= settings.get("RTA_Max_Time_Without_Speaking_Count").toLong()){
                    stopRecording();
                    break;
                }

                // Display recording time safely on UI thread.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Recording...\nMicrophone Level: " + currentMicAmp+
                                        "\nElpased Recording Time: " +elapsedTime / 1000.0+"s";
                    });
                }

                // Wait a couple ms to not overload the CPU.
                delay(100);
            }
        }
    }

    fun testRTAMode() {
        try {
            val recordingDirPath = "rtaMode/recordings";
            var filesInDir = requireContext().assets.list(recordingDirPath);
            if(filesInDir == null) return;

            for(file in filesInDir){
                var recording = requireContext().assets.openFd("$recordingDirPath/$file");
                googleAPI.processRecording(recording.createInputStream());

                recording.close();

                googleAPI.outputData.recongizedText// output here


            }

        } catch (e: Exception) {
            Log.e("Error", e.toString());
            try {
                requireActivity().runOnUiThread(Runnable {
                    outputTV.text =
                        "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                });
            } catch (e: Exception) {
                Log.e("Error", e.toString());
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView();
        // Clean up recording thread.
        if(recordingBackgroundJob!=null){
            recordingBackgroundJob?.cancel();
            recordingBackgroundJob = null;
        }
        ttsInterface.onDestroy();
        googleAPI.onDestroy();
        _binding = null;
    }
}