package com.example.convoassistant

// Template Classes based off of https://platform.openai.com/docs/api-reference/authentication.
class OpenAIUsage(
    val prompt_tokens: Number,
    val completion_tokens: Number,
    val total_tokens: Number
){}

class OpenAIMessage(
    val role: String,
    val content: String
){}

class OpenAIChoices(
    val message: OpenAIMessage,
    val finish_reason: String,
    val index: Number
){}

class OpenAIResponse(
    val id: String,
    val created: Number,
    val model: String,
    val usage: OpenAIUsage,
    val choices: List<OpenAIChoices>
){}