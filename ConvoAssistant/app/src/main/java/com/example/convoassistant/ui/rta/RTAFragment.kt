package com.example.convoassistant.ui.rta

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.R
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.TTSInterfaceClass
import com.example.convoassistant.databinding.FragmentRtaBinding
import com.example.convoassistant.makeChatGPTRequest
import java.util.ArrayList
import java.util.Locale
import java.util.Objects
import kotlin.concurrent.thread

class RTAFragment : Fragment() {

    private var _binding: FragmentRtaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var ttsInterface: TTSInterfaceClass
    private var max_tokens = 50;
    private var pre_prompt = "";

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
        //set up microphone
        setupMic()
        //set up text to speech
        ttsInterface = TTSInterfaceClass(requireContext())

        val settings = SettingWrapper(requireActivity())
        max_tokens = settings.get("RTA_LLM_Output_Token_Count").toInt()
        pre_prompt = settings.get("RTA_LLM_Prompt")

    }



    //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/
    // on below line we are creating variables
    // for text view and image view
    private lateinit var outputTV: TextView
    private lateinit var micIB: ImageButton
    private val REQUEST_CODE_SPEECH_INPUT = 1

    private fun setupMic()
    {
        //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/
        // initializing variables of list view with their ids.
        outputTV =  requireView().findViewById(R.id.speech_2_text_out)
        micIB = requireView().findViewById(R.id.mic_button)

        // on below line we are adding on click
        // listener for mic image view.
        micIB.setOnClickListener {
            // on below line we are calling speech recognizer intent.
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // on below line we are passing language model
            // and model free form in our intent
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // on below line we are passing our
            // language as a default language.
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            // on below line we are specifying a prompt
            // message as speak to text on below line.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            // on below line we are specifying a try catch block.
            // in this block we are calling a start activity
            // for result method and passing our result code.
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                // on below line we are displaying error message in toast
                Toast
                    .makeText(
                        requireContext(), " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    // on below line we are calling on activity result method.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

        // in this method we are checking request
        // code with our result code.
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == AppCompatActivity.RESULT_OK && data != null) {

                thread(start = true) {
                    // in that case we are extracting the
                    // data from our array list
                    val res: ArrayList<String> =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                    // Run the OpenAI request in a subroutine.
                    val outputText = makeChatGPTRequest(pre_prompt+Objects.requireNonNull(res)[0],max_tokens)

                    // on below line we are setting data
                    // to our output text view.
                    requireActivity().runOnUiThread(Runnable {
                        outputTV.text = (outputText)
                    })

                    //text to speech
                    ttsInterface.speakOut(outputText)


                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsInterface.onDestroy()
        _binding = null
    }
}