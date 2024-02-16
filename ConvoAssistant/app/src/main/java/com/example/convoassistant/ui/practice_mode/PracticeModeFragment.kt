package com.example.convoassistant.ui.practice_mode

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.convoassistant.R
import com.example.convoassistant.STTFragment
import com.example.convoassistant.SettingWrapper
import com.example.convoassistant.TTSInterfaceClass
import com.example.convoassistant.databinding.FragmentPracticeModeBinding
import com.example.convoassistant.makeChatGPTRequest
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread
import kotlin.random.Random


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
    private lateinit var streakTV: TextView
    private lateinit var micIB: Button
    private lateinit var generatePromptB: Button
    private lateinit var testModeB: Button

    //settings and streaks
    private lateinit var settings: SettingWrapper
    private var streak = 0;



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
        micIB = requireView().findViewById(R.id.rate_reflection_button)
        testModeB = requireView().findViewById((R.id.pratice_test))
        streakTV = requireView().findViewById((R.id.StreakText))

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
        testModeB.setOnClickListener {
            testPracticeMode()
        }
        //set up text to speech
        ttsInterface = TTSInterfaceClass(requireContext())

        //load settings
        settings = SettingWrapper(requireActivity())

        scenarioPrompt = settings.get("Pra_Scenario_LLM_Prompt")
        scenarioTokens = settings.get("Pra_Scenario_LLM_Output_Token_Count").toInt()

        ratingPrompt = settings.get("Pra_Rating_LLM_Prompt")
        ratingTokens = settings.get("Pra_Rating_LLM_Output_Token_Count").toInt()

        //check status of streak, no increment since they have not practiced yet
        streakTV.text = settings.getStreakPracticeMode(allowIncrement=false).toString();
    }

    //runs when speech to text returns result
    override fun onMicResult(input: String){
        //increment streak
        streakTV.text = settings.getStreakPracticeMode(allowIncrement=true).toString();

        //run in thread so we don't block main
        thread(start = true) {
            try{
            val combinedInput = ratingPrompt +
                                "\nOrignal Statment:\"" + currentPracticeScenario +"\"" +
                                "\nResponse:\"" + input +"\""

            // Run the OpenAI request in a subroutine.
            val outputText = makeChatGPTRequest(combinedInput, ratingTokens, temperature = 0.1)

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

            } catch(e: Exception) {
                Log.e("Error", e.toString());
                try {
                    requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                        outputTV.text =
                            "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                    });
                } catch (e: Exception) {
                    Log.e("Error", e.toString());
                }
            }
        }
    }

    // Callback for the new scenario prompt button.
    fun generatePracticePrompt() {

        // Display a loading message.
        outputTV.text = "Generating Practice Scenario..."

        // 50/50 chance of remembering last scenario. (Helps with repetitiveness)
        var scenarioPromptNew = currentPracticeScenario + "\n" + scenarioPrompt;
        if (Random.nextBoolean()) {
            scenarioPromptNew = scenarioPrompt
        }

        // Clear the existing scenario.
        currentPracticeScenario = ""
        checkIfResponseButtonShouldBeEnabled()

        // Disable the generate prompt button.
        generatePromptB.isEnabled = false;

        // run in thread so we don't block main
        thread(start = true) {
            try {
                // Run the OpenAI request in a subroutine.
                currentPracticeScenario =
                    makeChatGPTRequest(scenarioPromptNew, scenarioTokens, temperature = 1.5)

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

            } catch (e: Exception) {
                Log.e("Error", e.toString());
                try {
                    requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                        outputTV.text =
                            "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                    });
                } catch (e: Exception) {
                    Log.e("Error", e.toString());
                }
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

    fun testPracticeMode(){
        // Run in thread so we don't block main.
        thread(start = true) {  try {
            val testFilePath = "practiceModeTestCases.txt";
            val testFile = requireContext().assets.open(testFilePath);
            val testFileReader = BufferedReader(InputStreamReader(testFile));

            val badSubStrings = arrayOf(
                "The given response does not meet the criteria for a good reflection",
                "not good",
                "not a good"
            );
            val goodSubStrings = arrayOf(
                "The reflection is good because",
                "This is a good reflective",
                "The given response is a good reflection"
            )

            // Get the first test case.
            var line = testFileReader.readLine()
            var testCaseCount = 0
            var correctlyClassified = 0
            var inconclusivelyClassified = 0
            // Loop until we have run out of test cases.
            while(line != null){

                testCaseCount += 1;

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                        // Display output text on screen.
                        outputTV.text = "On Testcase $testCaseCount..."
                    });
                }

                // Parse the entry.
                var testCaseSplit = line.split(";");
                if (testCaseSplit.size != 3) {
                    Log.e("Error", "Cannot process test case $testCaseCount: $line");
                    // Get the next test case.
                    line = testFileReader.readLine()
                    continue;
                }

                for (i in 0..2) {
                    // Make the OpenAI request.
                    val combinedInput = ratingPrompt +
                            "\nOrignal Statment:\"" + testCaseSplit[0].trim() + "\"" +
                            "\nResponse:\"" + testCaseSplit[1].trim() + "\""
                    val outputText =
                        makeChatGPTRequest(combinedInput, ratingTokens, temperature = 0.1)

                    // Try and classify if OpenAI said this was a good or bad response.
                    var classifiedBad = false
                    for (substring in badSubStrings) {
                        if (outputText.contains(substring)) {
                            classifiedBad = true; break;
                        }
                    }
                    var classifiedGood = false
                    for (substring in goodSubStrings) {
                        if (outputText.contains(substring)) {
                            classifiedGood = true; break;
                        }
                    }

                    Log.i("Testinfo", "Testcase $testCaseCount: $line")
                    Log.i("Testinfo", "Testcase $testCaseCount classification: $outputText")

                    // If both classified as both or neither, error out.
                    if (classifiedBad == classifiedGood) {
                        inconclusivelyClassified += 1;
                        Log.i("Testinfo", "Testcase $testCaseCount result: Inconclusive!");
                    }
                    // Correctly classified.
                    else if (
                        (classifiedBad && testCaseSplit[2].trim() == "bad") ||
                        (classifiedGood && testCaseSplit[2].trim() == "good")
                    ) {
                        correctlyClassified += 1
                        Log.i("Testinfo", "Testcase $testCaseCount result: Correctly Classified!");
                    }
                    // Incorrect.
                    else {
                        Log.i(
                            "Testinfo",
                            "Testcase $testCaseCount result: Incorrectly Classified!"
                        );
                    }
                }

                // Get the next test case.
                line = testFileReader.readLine()
            }

            // Report the final result.
            if(inconclusivelyClassified>0) Log.i("TestResult", "couldNotClassifyAutomatically:    $inconclusivelyClassified");
            Log.i("TestResult", "accuracyRatio:    $correctlyClassified / ${testCaseCount*3-inconclusivelyClassified}");
            Log.i("TestResult", "accuracy:    ${correctlyClassified * 100.0 / (testCaseCount*3-inconclusivelyClassified)}%");

            testFileReader.close();

            if (getActivity() != null) {
                requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                    // Display output text on screen.
                    outputTV.text = "Done Testing Practice Mode. View Logcat for results!"
                });
            }
        } catch (e: Exception) {
            Log.e("Error", e.toString());
            try {
                requireActivity().runOnUiThread(kotlinx.coroutines.Runnable {
                    outputTV.text =
                        "Error occured, maybe you need to enable permissions\n INFO: " + e.message
                });
            } catch (e: Exception) {
                Log.e("Error", e.toString());
            }
        }
    } }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsInterface.onDestroy()
        _binding = null
    }
}