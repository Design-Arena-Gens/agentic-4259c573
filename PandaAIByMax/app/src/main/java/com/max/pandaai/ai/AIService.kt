package com.max.pandaai.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles communication with the configured LLM endpoint (OpenAI by default).
 * Replace [API_KEY_PLACEHOLDER] with a secure key before release.
 */
class AIService(
    private val client: OkHttpClient = defaultClient,
    private val gson: Gson = Gson()
) {

    suspend fun generateResponse(prompt: String, userName: String, assistantName: String): String {
        val contextualPrompt = """
            You are $assistantName, a playful but respectful Panda-themed assistant.
            The user is $userName. Respond warmly, keep answers concise (max 120 words),
            and offer to help with smart actions when appropriate.
        """.trimIndent()

        val bodyJson = JsonObject().apply {
            addProperty("model", "gpt-3.5-turbo")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to contextualPrompt),
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("temperature", 0.8)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY_PLACEHOLDER")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "AI error: $errorBody")
                throw IOException("AI request failed: ${response.code}")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty AI response")
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            val messageObj = choices?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("message")
            return messageObj?.get("content")?.asString?.trim()
                ?: throw IOException("Malformed AI response")
        }
    }

    companion object {
        private const val API_KEY_PLACEHOLDER = "YOUR_OPENAI_API_KEY"
        private const val TAG = "AIService"
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
