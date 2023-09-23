from SuperSecretAPIKey import openAIAPIKey 
import openai
import time

# Set Authentication Key.
openai.api_key = openAIAPIKey


startTime = time.time()

# Send a request to count to 100.
response = openai.ChatCompletion.create(
    model="gpt-3.5-turbo",
    messages=[
        {"role": "user", "content": "Count to 100, with only a space between each number and no newlines."}
    ],
    temperature=0,
    stream=True  
)

starIteration = time.time()

print(f"Overhead Time: {starIteration - startTime:.2f} s")

chunkBeginTime = 0
chunkEndTime = 0

# Iterate through the stream of events.
chunkCount = 0
for chunk in response:
    chunkTime = time.time() 

    if chunkEndTime != 0:
        print(f"Time Between Chunk {chunkCount-1} and Chunk {chunkCount} is {chunkTime - chunkEndTime:.2f} seconds.")

    # End case.
    if not "content" in chunk["choices"][0]["delta"]:
        print(f"Total Execution Time: {chunkTime-startTime:.2f}s.")
        break

    # Extract the content.
    chunkMessage = chunk["choices"][0]["delta"]["content"]

    print(f"Chunk {chunkCount} received {chunkTime-startTime:.2f} s after request. Content received: \"{chunkMessage}\"") 
    chunkCount+=1

    chunkEndTime = time.time()
   
