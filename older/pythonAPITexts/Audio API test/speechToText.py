#Based on https://www.geeksforgeeks.org/python-convert-speech-to-text-and-text-to-speech/
#####################################################

# Python program to translate
# speech to text and text to speech


import speech_recognition as sr
import pyttsx3

# Initialize the recognizer
r = sr.Recognizer()
# Initialize the engine
engine = pyttsx3.init()

# Function to convert text to
# speech
#TODO Maybe run in different thread
def SpeakText(command):
    engine.say(command)
    engine.runAndWait()


with sr.Microphone() as source2:
    #ambient noise
    r.adjust_for_ambient_noise(source2, duration=0.2)

    # Loop infinitely for user to speak
    while(1):

        # Exception handling to handle
        # exceptions at the runtime
        try:
            # wait for a second to let the recognizer
            # adjust the energy threshold based on
            # the surrounding noise level
            
            
            # use the microphone as source for input.
                
            #listens for the user's input
            audio2 = r.listen(source2)
            
            # Using google to recognize audio
            # TODO pick best api, google isnt the only one https://pypi.org/project/SpeechRecognition/
            #TODO get proper API key so we dont randomly get kicked out
            MyText = r.recognize_google(audio2)
            # MyText = r.recognize_whisper(audio2)
            MyText = MyText.lower()

            print(MyText)
            SpeakText(MyText)
                
        except sr.RequestError as e:
            print("Could not request results; {0}".format(e))
            
        except sr.UnknownValueError:
            print("Speech unintelligeable")
            SpeakText("Speech unintelligeable")
