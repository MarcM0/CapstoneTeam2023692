from google.cloud import speech_v1p1beta1 as speech
import os.path

client = speech.SpeechClient()

diarization_config = speech.SpeakerDiarizationConfig(
    enable_speaker_diarization=True,
    min_speaker_count=2,
    max_speaker_count=2,
)

config = speech.RecognitionConfig(
    encoding=speech.RecognitionConfig.AudioEncoding.AMR_WB,
    sample_rate_hertz=48000,
    audio_channel_count=2,
    language_code="en-US",
    diarization_config=diarization_config,
    use_enhanced=True,
    
    model="phone_call",
)


filePath = os.path.join(os.path.abspath(os.path.dirname(__file__)), "./myCoolRecording.wav")
with open(filePath, "rb") as audio_file:
    content = audio_file.read()

audio = speech.RecognitionAudio(content=content)
print("Waiting for operation to complete...")
response = client.recognize(config=config, audio=audio)


# The transcript within each result is separate and sequential per result.
# However, the words list within an alternative includes all the words
# from all the results thus far. Thus, to get all the words with speaker
# tags, you only have to take the words list from the last result:
result = response.results[-1]
words_info = result.alternatives[0].words

# Printing out the output:
for word_info in words_info:
    print(f"word: '{word_info.word}', speaker_tag: {word_info.speaker_tag}")

