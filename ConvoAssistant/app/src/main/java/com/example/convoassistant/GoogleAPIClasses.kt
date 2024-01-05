package com.example.convoassistant

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val sampleRate = 16000;
    private val audioChannels = 2;
    private val bitDepth = 16;

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
            .setMaxSpeakerCount(2)
            .setMinSpeakerCount(2)
            .build();

        recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.MP3)
            .setAudioChannelCount(audioChannels)
            .setSampleRateHertz(sampleRate)
            .setLanguageCode("en-US")
            .setDiarizationConfig(diarizationConfig)
//            .setModel("phone_call")
            .build();

        // Get the storage file to be used.
        recordingFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString(), "/assistantCacheRecording.m4a"
        );

        debugFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString(), "/recordingTranscript.txt" //todo
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
    fun processRecording(filename: File?){
        // Read in the file in a format google can read use. The recorded file if no file is passed.
        val recordingInputStream = if (filename != null) FileInputStream(filename) else FileInputStream(recordingFile); // Kotlin doesn't support ternary operators and instead recommends this :(.
        if(recordingInputStream.available() <= 0){
            return;
        }
        recognitionInput = RecognitionAudio.newBuilder().setContent(
            ByteString.copyFrom(recordingInputStream.readBytes())
        ).build();
        Log.i("info", "Google Request Size: "+ recognitionInput.serializedSize / 1024.0 + "KB");

        // Perform STT and digitization.
        val speechToTextClientResponse = googleSpeechClient.recognize(recognitionConfig, recognitionInput);
        if(speechToTextClientResponse.resultsList.isEmpty()){
            return;
        }


        var fullTranscript = "";
        for(transciptResult in  speechToTextClientResponse.resultsList){
            fullTranscript += transciptResult.getAlternatives(0).transcript
        }
        val transciptWords = fullTranscript.split(" ");


        // Fetch the diarization output.
        val finalResult = speechToTextClientResponse.resultsList.last().alternativesList[0];
        // Split the dialogue up based on speakers.
        var currentSpeaker = -1;
        var speechSnippet = "";
        var diarizationIndex = 0;
        val newSnippet = "\n\""
        var pasteTheRest = false;
        for(word in transciptWords){
            // Panic mode -> paste the rest of the transcript under a new user.
            if(pasteTheRest){
                speechSnippet += word + " ";
                continue;
            }

            // Check to see if the diarized word matches the transcript.
            var currentDiarizationWord = finalResult.getWords(diarizationIndex);
            if(word == currentDiarizationWord.word){
                // Speaker change detected.
                if( currentDiarizationWord.speakerTag != currentSpeaker){
                    // End the previous speaker and add to the output.
                    if(speechSnippet != ""){
                        outputData.recongizedText += speechSnippet + '"';
                    }

                    // Set up the new speaker.
                    currentSpeaker = currentDiarizationWord.speakerTag;
                    speechSnippet = newSnippet;
                }

                // Build the snippet.
                speechSnippet += currentDiarizationWord.word + " ";
                diarizationIndex++;
            } else{
                // Mismatched word -> enter panic mode.
                pasteTheRest = true;

                // Set up the new speaker for the panic text.
                outputData.recongizedText += speechSnippet + '"';
                speechSnippet = newSnippet;
                currentSpeaker++;
            }
        }

        // End the transcription.
        outputData.recongizedText += speechSnippet + '"';
        outputData.lastSpeaker = currentSpeaker;

        Log.i("undiarizedText", fullTranscript )
        Log.i("diarizedText", outputData.recongizedText )

        // Write the transcript to a file for debugging.
        val outputStreamWriter =
            OutputStreamWriter(debugFile.outputStream());
        outputStreamWriter.write(outputData.recongizedText);
        outputStreamWriter.close();
    }

    fun startRecording(CurrActivity: Activity) {
        if (recording) return;
        outputData.clear();

        //request permissions
        //todo, this creates the popup, but does not even wait for response, so guaranteed to throw an error if permission bad
        if (ContextCompat.checkSelfPermission(CurrActivity, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CurrActivity, arrayOf(android.Manifest.permission.RECORD_AUDIO), 123);
        }
        if (ContextCompat.checkSelfPermission(CurrActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CurrActivity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 124);
        }

        // Set up the recorder. Requires different constructors based on the SDK version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioRecord = MediaRecorder(context);

        } else {
            audioRecord = MediaRecorder();
        }

        // Set up the recorder properties.
        audioRecord.setAudioSamplingRate(sampleRate);
        audioRecord.setAudioEncodingBitRate(sampleRate * bitDepth);
        audioRecord.setAudioChannels(audioChannels);
        audioRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecord.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        audioRecord.setOutputFile(recordingFile);

        // Begin Recording
        audioRecord.prepare();
        audioRecord.start();
        recording = true;
    }

    fun isSpeaking(threshold: Int): Boolean{
        if (!recording) return false;

        return audioRecord.maxAmplitude >= threshold;
    }
    fun getMicAmplitude(): Int{
        if (!recording) return 0;
        return audioRecord.maxAmplitude;
    }

    fun onDestroy(){
        if(recording){
            audioRecord.stop();
            audioRecord.release();
        }

        googleSpeechClient.close();
    }
}