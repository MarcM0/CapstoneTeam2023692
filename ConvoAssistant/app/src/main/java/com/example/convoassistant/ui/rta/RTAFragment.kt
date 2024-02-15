package com.example.convoassistant.ui.rta

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent;
import android.media.MediaPlayer
import android.media.session.MediaController
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import com.example.convoassistant.WordSequenceAligner
import com.example.convoassistant.databinding.FragmentRtaBinding
import com.example.convoassistant.makeChatGPTRequest
import kotlinx.coroutines.*
import java.util.concurrent.ScheduledFuture
import kotlin.concurrent.thread
import kotlin.time.DurationUnit
import kotlin.time.measureTime


// Real time assistant mode interface
// Vaguely based on //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

class HeadphoneButtonClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("asdadasd2", "asdasdasd2");
    }
}


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

    private lateinit var mediaPlayer: MediaPlayer;
    private lateinit var mediaSession: MediaSessionCompat;
    private lateinit var mediaController: MediaController;
    private lateinit var stateBuilder: PlaybackStateCompat.Builder;

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
        var audioFile = requireContext().assets.openFd("rtaMode/recordings/test1.m4a");


        val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.i("media", "play")
                super.onPlay()
            }

            override fun onPause() {
                Log.i("media", "pause")
                super.onPause()
                // Handle pause event
            }

            override fun onSkipToNext() {
                Log.i("media", "next")
                super.onSkipToNext()
            }

            override fun onSkipToPrevious() {
                Log.i("media", "prev")
                super.onSkipToPrevious()
            }

            override fun onStop() {
                super.onStop()
            }
        }


        mediaSession = MediaSessionCompat(requireContext(), "MusicService")
        mediaSession.setCallback(mMediaSessionCallback)
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )



        stateBuilder = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
        ).setState(
            PlaybackStateCompat.STATE_PLAYING,
            0,
            1.0F
        )

        mediaSession.setPlaybackState(stateBuilder.build())

        mediaSession.isActive = true

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(audioFile.fileDescriptor, audioFile.startOffset, audioFile.length)
        mediaPlayer.prepare()
        mediaPlayer.start()

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

    fun stopRecordingHelper(outputToUIandSound:Boolean=true): Boolean {
        // SST Processing failed.
        if (googleAPI.outputData.recongizedText == "") {
            // Run the following on the UI thread safely.
            if (getActivity() != null) {
                requireActivity().runOnUiThread(Runnable {
                    // Display output text on screen.
                    outputTV.text = "Sorry. We could not hear you!";
                });
            }
            return false;
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

        if(outputToUIandSound) {
            // Run the following on the UI thread safely.
            if (getActivity() != null) {
                // display output text on screen
                requireActivity().runOnUiThread(Runnable {
                    outputTV.text = outputText;
                });
            }

            // Text to speech.
            ttsInterface.speakOut(outputText);
        }

        return true;
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

            if(!stopRecordingHelper()){
                return@thread
            }
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
            try {
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
                    if (currentMicAmp >= settings.get("RTA_Microphone_Threshold_Count").toInt()) {
                        timeLastSpoke = currentTime;
                    }

                    // Timeout after not speaking for a while.
                    if (timeLastSpoke > 0 && (currentTime - timeLastSpoke) >= settings.get("RTA_Max_Time_Without_Speaking_Count")
                            .toLong()
                    ) {
                        stopRecording();
                        break;
                    }

                    // Display recording time safely on UI thread.
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(Runnable {
                            // Display output text on screen.
                            outputTV.text = "Recording...\nMicrophone Level: " + currentMicAmp +
                                    "\nElpased Recording Time: " + elapsedTime / 1000.0 + "s";
                        });
                    }

                    // Wait a couple ms to not overload the CPU.
                    delay(100);
                }
            }catch(e:CancellationException ){
                //this is fine and expected when we cancel the job
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

    fun testRTAMode() {
        // Run in thread so we don't block main.
        thread(start = true) {  try {
            val recordingDirPath = "rtaMode/recordings";
            val expectedDirPath = "rtaMode/expectedTranscripts";
            var filesInDir = requireContext().assets.list(recordingDirPath);
            if(filesInDir == null) return@thread;
            val timesListGoogle: MutableList<Double> = mutableListOf()
            val timesListOpenAI: MutableList<Double> = mutableListOf()
            val timesListTotal: MutableList<Double> = mutableListOf()
            val diarizationAccList: MutableList<Double> = mutableListOf()
            val transcribeAccList: MutableList<Double> = mutableListOf()

            val reNotAlphaNum = Regex("[^A-Za-z0-9 ]") //alphanumeric
            val reWhiteSpace = Regex("\\s+") //whitespace
            val reInQuotes = Regex("\".*?\"")

            var count = 0;
            for(file in filesInDir){
                val recording = requireContext().assets.open("$recordingDirPath/$file");
                count+=1;
                Log.i("Testinfo","\n\nfilename: "+file)

                val textFilePath = "$expectedDirPath/$file".substringBeforeLast(".")+".txt";
                val textTruth = reWhiteSpace.replace(requireContext().assets.open(textFilePath).bufferedReader().use {
                    it.readText()
                }, " ").toLowerCase().trim()

                    // Display recording time safely on UI thread.
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(Runnable {
                        // Display output text on screen.
                        outputTV.text = "Testing file "+ count + "/" + filesInDir.size + "...";
                    });
                }

                val timeTakenGoogle = measureTime {
                    googleAPI.processRecording(recording);
                }
                val timeTakenOpenAI = measureTime {
                    if(!stopRecordingHelper(outputToUIandSound=false)){
                        Log.e("Error","Error occured while testing")
                        return@thread
                    }
                }
                Log.i("Testinfo","timeTakenGoogle:"+timeTakenGoogle);
                timesListGoogle.add(timeTakenGoogle.toDouble(DurationUnit.SECONDS));
                Log.i("Testinfo","timeTakenOpenAI:"+timeTakenOpenAI);
                timesListOpenAI.add(timeTakenOpenAI.toDouble(DurationUnit.SECONDS));
                Log.i("Testinfo","timeTakenTotal:"+(timeTakenGoogle+timeTakenOpenAI));
                timesListTotal.add((timeTakenGoogle+timeTakenOpenAI).toDouble(DurationUnit.SECONDS));

                val detectedText = reWhiteSpace.replace(googleAPI.outputData.recongizedText, " ").toLowerCase().trim();

                //word lists with only alphanumeric stuff
                val truthWordList = reNotAlphaNum.replace(textTruth, "").trim().split("\\s+".toRegex())
                val detectedWordList = reNotAlphaNum.replace(detectedText, "").trim().split("\\s+".toRegex())
                Log.i("Testinfo","truthWordList:   "+truthWordList.toString());
                Log.i("Testinfo","detectedWordList:"+detectedWordList.toString());

                //transcript
                val compareWords = WordSequenceAligner();
                val alignmentWords = compareWords.align(truthWordList.toTypedArray(),detectedWordList.toTypedArray())
                //can also get word error rate from this library but this is more intuitive
                val percentCorrect = (100*alignmentWords.numCorrect).toDouble()/truthWordList.size
                transcribeAccList.add(percentCorrect);
                Log.i("Testinfo","percentCorrectTranscribe:"+percentCorrect.toString());


                //diarization
                //split into words per speaker
                val speaker1Truth: MutableList<String> = mutableListOf()
                val speaker2Truth: MutableList<String> = mutableListOf()
                var isSpeaker1 = true;
                for (utterance in reInQuotes.findAll(textTruth)){
                    val wordsToAdd = reNotAlphaNum.replace(utterance.value, "").trim().split("\\s+".toRegex())
                    if(isSpeaker1){
                        speaker1Truth.addAll(wordsToAdd)
                    }else{
                        speaker2Truth.addAll(wordsToAdd)
                    }
                    isSpeaker1 = !isSpeaker1;
                }

                val speaker1Predict: MutableList<String> = mutableListOf()
                val speaker2Predict: MutableList<String> = mutableListOf()
                isSpeaker1 = true;
                for (utterance in reInQuotes.findAll(detectedText)){
                    val wordsToAdd = reNotAlphaNum.replace(utterance.value, "").trim().split("\\s+".toRegex())
                    if(isSpeaker1){
                        speaker1Predict.addAll(wordsToAdd)
                    }else{
                        speaker2Predict.addAll(wordsToAdd)
                    }
                    isSpeaker1 = !isSpeaker1;

                }

//                Log.i("Testinfo","speaker1Truth"+speaker1Truth.toString());
//                Log.i("Testinfo","speaker2Truth"+speaker2Truth.toString());
//                Log.i("Testinfo","speaker1Predict"+speaker1Predict.toString());
//                Log.i("Testinfo","speaker2Predict"+speaker2Predict.toString());

                //get percentCorrect
                val alignmentWordsS1 = compareWords.align(speaker1Truth.toTypedArray(),speaker1Predict.toTypedArray())
                val alignmentWordsS2 = compareWords.align(speaker2Truth.toTypedArray(),speaker2Predict.toTypedArray())
                //can also get word error rate from this library but this is more intuitive
                val percentCorrectDiarize = (100*(alignmentWordsS1.numCorrect+alignmentWordsS2.numCorrect)).toDouble()/truthWordList.size
                diarizationAccList.add(percentCorrectDiarize);
                Log.i("Testinfo","percentCorrectDiarize:"+percentCorrectDiarize.toString());


                recording.close();
            }
            //print stats
            Log.i("TestResult", "avgTimeGoogle:    "+timesListGoogle.average());
            Log.i("TestResult", "avgTimeOpenAI:    "+timesListOpenAI.average());
            Log.i("TestResult", "avgTimeTotal:     "+timesListTotal.average());
            Log.i("TestResult", "worstTimeTotal:   "+timesListTotal.max());
            Log.i("TestResult", "diarizationAcc:   "+diarizationAccList.average());
            Log.i("TestResult", "transcribeAccList:"+transcribeAccList.average());

            if (getActivity() != null) {
                requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                    // Display output text on screen.
                    outputTV.text = "Done Testing RTA Mode. View Logcat for results!"
                });
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
    } }

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