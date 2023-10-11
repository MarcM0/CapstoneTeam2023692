package com.example.convoassistant

import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.convoassistant.databinding.ActivityMainBinding

import io.ktor.client.*
import io.ktor.client.engine.android.*
import com.google.gson.Gson

import kotlin.concurrent.thread

val gsonParser = Gson()
val httpClient = HttpClient(Android)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ttsInterface: TTSInterfaceClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_rta, R.id.navigation_practice_mode, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupMic()
        ttsInterface = TTSInterfaceClass(this)
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
        outputTV = findViewById(R.id.speech_2_text_out)
        micIB = findViewById(R.id.mic_button)

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
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    public override fun onDestroy() {
        ttsInterface.onDestroy()
        super.onDestroy()
    }

    // on below line we are calling on activity result method.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //https://www.geeksforgeeks.org/speech-to-text-application-in-android-with-kotlin/

        // in this method we are checking request
        // code with our result code.
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == RESULT_OK && data != null) {

                thread(start = true) {
                    // in that case we are extracting the
                    // data from our array list
                    val res: ArrayList<String> =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                    // Run the OpenAI request in a subroutine.
                    val outputText = makeChatGPTRequest(Objects.requireNonNull(res)[0])

                    // on below line we are setting data
                    // to our output text view.
                    runOnUiThread(Runnable {
                        outputTV.text = (outputText)
                    })

                    //text to speech
                    ttsInterface.speakOut(outputText)


                }
            }

        }
    }
}