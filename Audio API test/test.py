#used to time multiple speech to text protocols
import speech_recognition as sr
import time
import json
import os

r = sr.Recognizer()
audio_file = "./audio.wav"

def vosk022(audio2):
    os.chdir(os.path.join(".","models","vosk-model-en-us-0.22-lgraph"))
    result = r.recognize_vosk(audio2)
    os.chdir("../..")
    return result

def vosk015(audio2):
    os.chdir(os.path.join(".","models","vosk-model-small-en-us-0.15"))
    result = r.recognize_vosk(audio2)
    os.chdir("../..")
    return result

def whisperTiny(audio2):
    #.en makes it work for english only but makes it faster
    return r.recognize_whisper(audio2,model="tiny.en")

def whisperBase(audio2):
    #.en makes it work for english only but makes it faster
    return r.recognize_whisper(audio2,model="base.en")

def whisperSmall(audio2):
    #.en makes it work for english only but makes it faster
    return r.recognize_whisper(audio2,model="small.en")

# def test(audio):
#     return r.recognize_amazon(audio2,region="us-east-2")

#all
# r.recognize_amazon,r.recognize_assemblyai,r.recognize_azure,r.recognize_bing,r.recognize_google,r.recognize_google_cloud,r.recognize_houndify,r.recognize_ibm,r.recognize_lex,r.recognize_sphinx,r.recognize_tensorflow,r.recognize_vosk,r.recognize_whisper,r.recognize_whisper

# alternative list
# os.environ["OPENAI_API_KEY"] = ""
# functionList = [r.recognize_tensorflow,r.recognize_whisper_api,r.recognize_google,r.recognize_sphinx,vosk022,vosk015,whisperTiny,whisperBase,whisperSmall]

#offline
functionList = [r.recognize_google,r.recognize_sphinx,vosk022,vosk015,whisperTiny,whisperBase,whisperSmall]

with sr.AudioFile(audio_file) as source2:
    # #ambient noise
    # r.adjust_for_ambient_noise(source2, duration=0.2)

    #load file
    audio2 = r.listen(source2)
    repeats = 10 #number of times to repeat each test

    functionStats = dict()

    #test each function several times
    for function in functionList:
        times = []
        print("testing: ",function.__name__)
        function(audio2) #warmup
        for i in range(repeats):
            start = time.time()
            text = function(audio2)
            times.append(time.time()-start)
        
        functionStats[function.__name__] = [sum(times)/len(times),text] #average time, transcription

    #write results
    with open("stats.txt", "w") as fp:
        json.dump(obj=functionStats, fp=fp,indent=4)
