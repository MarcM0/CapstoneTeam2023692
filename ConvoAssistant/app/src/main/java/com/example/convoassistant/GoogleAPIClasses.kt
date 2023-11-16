package com.example.convoassistant

import android.R.attr.data
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import com.google.api.gax.core.CredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.speech.v1p1beta1.RecognitionAudio
import com.google.cloud.speech.v1p1beta1.RecognitionConfig
import com.google.cloud.speech.v1p1beta1.SpeakerDiarizationConfig
import com.google.cloud.speech.v1p1beta1.SpeechClient
import com.google.cloud.speech.v1p1beta1.stub.GrpcSpeechStub
import com.google.cloud.speech.v1p1beta1.stub.SpeechStubSettings
import com.google.protobuf.ByteString
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter


// TODO!!! make recording and diarization happen on another thread.

// Used to fetch the API key from the app's asset folder.
class SpeechCredentialsProvider(private val context: Context) : CredentialsProvider {
    override fun getCredentials(): GoogleCredentials {
        val credentialsFile = context.assets.open("SuperSecretAPIKey.json");
        return ServiceAccountCredentials.fromStream(credentialsFile);
    }
}

class OutputStruct() {
    var recongizedText: String = "";
    var lastSpeaker: Number = -1;

    fun clear(){
        recongizedText = "";
        lastSpeaker = -1;
    }
}
//
class GoogleSpeechToTextInterface(private val context: Context) {
    private val sampleRate = 48000;
    private val audioChannels = 2;

    private lateinit var audioRecord: MediaRecorder;

    private lateinit var recognitionInput: RecognitionAudio;
    private var recognitionConfig: RecognitionConfig;
    private var diarizationConfig: SpeakerDiarizationConfig;

    private var googleSpeechClient: SpeechClient;
    private var grpcStub: GrpcSpeechStub;

    var recording: Boolean = false;
    private var recordingFile: File;
    private var debugFile: File;

    var outputData: OutputStruct = OutputStruct();


    // Sets up the service.
    init {
        // Authenticate with google cloud.
        SpeechStubSettings.newBuilder().apply {
            credentialsProvider = SpeechCredentialsProvider(context);
            endpoint = "speech.googleapis.com:443";
            grpcStub = GrpcSpeechStub.create(build());
        }
        googleSpeechClient = SpeechClient.create(grpcStub);

        // Set up diarziation and STT.
        diarizationConfig = SpeakerDiarizationConfig.newBuilder()
            .setEnableSpeakerDiarization(true)
            .setMaxSpeakerCount(10)
            .setMinSpeakerCount(3)
            .build();

        recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.MP3)
            .setAudioChannelCount(audioChannels)
            .setSampleRateHertz(sampleRate)
            .setLanguageCode("en-US")
            .setDiarizationConfig(diarizationConfig)
            .setModel("phone_call")
            .build();

        // Get the storage file to be used.
        recordingFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString(), "/assistantCacheRecording.wav"
        );

        debugFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString(), "/recordingTranscript.txt"
        );
    }

    fun stopRecording() {
        if (!recording) return;
        outputData.clear();

        // Stop and destroy the recorder.
        audioRecord.stop();
        audioRecord.release();
        recording = false;
    }
    fun processRecording(){
        // Read in the file in a format google can read.
        val recordingInputStream = FileInputStream(recordingFile);
        recognitionInput = RecognitionAudio.newBuilder().setContent(
            ByteString.copyFrom(recordingInputStream.readBytes())
        ).build();

        // Perform STT and digitization.
        val speechToTextClientResponse = googleSpeechClient.recognize(recognitionConfig, recognitionInput);
        if(speechToTextClientResponse.resultsList.isEmpty()){
            return;
        }

        // Construct the output of the model.
        val finalResult = speechToTextClientResponse.resultsList.last();
        val wordsSpoken = finalResult.alternativesList[0].wordsList;

        outputData.lastSpeaker = wordsSpoken.last().speakerTag;

        // Split the dialogue up based on speakers.
        var currentSpeaker = -1;
        var speechSnippet = "";
        for(word in wordsSpoken) {
            // Speaker change detected.
            if(word.speakerTag != currentSpeaker){
                // End the previous speaker and add to the output.
                if(speechSnippet != ""){
                    outputData.recongizedText = speechSnippet + '"';
                }

                // Set up the new speaker.
                currentSpeaker = word.speakerTag
                speechSnippet = "User " + currentSpeaker +  ":\"";
            }

            // Build the snippet.
            speechSnippet += word.word + " ";
        }

        // End the transcription.
        outputData.recongizedText = speechSnippet + '"';
        outputData.lastSpeaker = currentSpeaker;

        // Write the transcript to a file for debugging.
        val outputStreamWriter =
            OutputStreamWriter(debugFile.outputStream());
        outputStreamWriter.write(outputData.recongizedText);
        outputStreamWriter.close();
    }

    fun startRecording() {
        if (recording) return;
        outputData.clear();

        // Set up the recorder. Requires different constructors based on the SDK version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioRecord = MediaRecorder(context);

        } else {
            audioRecord = MediaRecorder();
        }

        // Set up the recorder properties.
        audioRecord.setAudioSamplingRate(sampleRate);
        audioRecord.setAudioChannels(audioChannels);
        audioRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecord.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        audioRecord.setOutputFile(recordingFile);

        // Begin Recording
        audioRecord.prepare();
        audioRecord.start();
        recording = true;
    }

    fun onDestroy(){
        if(recording){
            audioRecord.stop();
            audioRecord.release();
        }

        googleSpeechClient.close();
    }
}
