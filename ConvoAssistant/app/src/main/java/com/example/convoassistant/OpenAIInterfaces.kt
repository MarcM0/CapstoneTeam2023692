package com.example.convoassistant

import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking



class OpenAIUsage(
    // based off of https://platform.openai.com/docs/api-reference/authentication.
    val prompt_tokens: Number,
    val completion_tokens: Number,
    val total_tokens: Number
){}

class OpenAIMessage(
    // based off of https://platform.openai.com/docs/api-reference/authentication.
    val role: String,
    val content: String
){}

class OpenAIChoices(
    // based off of https://platform.openai.com/docs/api-reference/authentication.
    val message: OpenAIMessage,
    val finish_reason: String,
    val index: Number
){}

class OpenAIResponse(
    // based off of https://platform.openai.com/docs/api-reference/authentication.
    val id: String,
    val created: Number,
    val model: String,
    val usage: OpenAIUsage,
    val choices: List<OpenAIChoices>
){}

fun makeChatGPTRequest(prompt: String, max_tokens: Int = 50, temperature: Double = 1.0) : String
{
    lateinit var output: String
    val cleanPrompt = prompt.replace("\"", "'").replace("\n", " ")
    runBlocking {
        launch {
            val response: HttpResponse = httpClient.request("https://api.openai.com/v1/chat/completions") {
                method = HttpMethod.Post
                headers {
                    append("Content-Type", "application/json")
                    append(
                        "Authorization",
                        "Bearer $openAIAPIKey"
                    )
                }
                setBody(
                    """{
                                "model": "gpt-3.5-turbo",
                                "messages": [{"role": "user", "content": "$cleanPrompt"}],
                                "temperature": $temperature,
                                "max_tokens": $max_tokens
                            }"""
                )
            }

            var responseObject = gsonParser.fromJson(response.bodyAsText(), OpenAIResponse::class.java)
            if(responseObject.choices == null)
            {
                output = "Error occured in chatgpt call"
            }
            else
            {
                output = responseObject.choices[0].message.content
            }

        }
    }

    return output
}