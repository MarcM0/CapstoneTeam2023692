package com.example.convoassistant


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