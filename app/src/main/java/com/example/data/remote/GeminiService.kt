package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.PersonalMemory
import com.example.data.model.PersonaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateAiResponse(
        prompt: String,
        history: List<ChatMessage>,
        persona: PersonaType,
        memories: List<PersonalMemory>,
        customApiKey: String? = null,
        temperature: Float = 0.7f
    ): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim().takeIf { !it.isNullOrBlank() }
            ?: BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid Gemini API key found, generating personalized local response.")
            return@withContext generateLocalAssistantFallback(prompt, persona, memories)
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            // Construct system instruction combining Persona & Personal Memories
            val systemInstructionText = buildString {
                appendLine(persona.systemPrompt)
                appendLine()
                if (memories.isNotEmpty()) {
                    appendLine("Personal Memories & Context about this user (Use these naturally to personalize your answers):")
                    memories.forEach { memory ->
                        appendLine("- [${memory.category}]: ${memory.fact}")
                    }
                    appendLine()
                }
                appendLine("Keep formatting clean with clear paragraphs or bullet points. Respond conversationally as their personal AI.")
            }

            val requestJson = JSONObject().apply {
                // System instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstructionText)
                        })
                    })
                })

                // Contents (multi-turn conversation)
                val contentsArray = JSONArray()

                // Include previous context turns (up to last 10 messages for token efficiency)
                val recentHistory = history.takeLast(10)
                for (msg in recentHistory) {
                    val role = if (msg.sender == "USER") "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", msg.content)
                            })
                        })
                    })
                }

                // Current user message
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })

                put("contents", contentsArray)

                // Generation config
                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature)
                    put("topP", 0.95)
                    put("topK", 40)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string()

            if (!response.isSuccessful || responseBodyString == null) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBodyString")
                val errorMessage = parseErrorMessage(responseBodyString)
                return@withContext "⚠️ Gemini API note: $errorMessage\n\n${generateLocalAssistantFallback(prompt, persona, memories)}"
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) {
                        return@withContext text.trim()
                    }
                }
            }

            return@withContext "I processed your request, but could not generate a response. Please try rephrasing."
        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing exception during Gemini request", e)
            return@withContext "⚠️ Connection notice (${e.localizedMessage ?: "Network issue"}).\n\n${generateLocalAssistantFallback(prompt, persona, memories)}"
        }
    }

    private fun parseErrorMessage(jsonString: String?): String {
        if (jsonString.isNullOrBlank()) return "Empty response from server"
        return try {
            val json = JSONObject(jsonString)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: "Request failed"
        } catch (e: Exception) {
            "Request failed"
        }
    }

    private fun generateLocalAssistantFallback(
        prompt: String,
        persona: PersonaType,
        memories: List<PersonalMemory>
    ): String {
        val lower = prompt.lowercase()
        val memoryHighlights = if (memories.isNotEmpty()) {
            val top = memories.take(2).joinToString("; ") { it.fact }
            "\n\n*Recalling from your AI Memory:* ($top)"
        } else ""

        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                "Hello! I am your personal AI companion in **${persona.title}** mode. How can I assist you with your day, goals, or projects today?$memoryHighlights"
            }
            lower.contains("goal") || lower.contains("plan") -> {
                "Let's break this down effectively:\n\n1. **Define the core objective**: What does success look like today?\n2. **Immediate Next Action**: Take the single highest leverage step first.\n3. **Remove friction**: Minimize context switching.\n\nWhat specific milestone would you like to focus on right now?$memoryHighlights"
            }
            lower.contains("who are you") || lower.contains("help") -> {
                "I am your Personal AI assistant! I'm here to:\n- 💬 Chat and brainstorm solutions with you\n- 🧠 Remember your preferences and goals in the **AI Memory** tab\n- ⚡ Provide structured guidance tailored to your workflow\n\nFeel free to ask me anything or configure my persona in **Settings**!"
            }
            lower.contains("summary") || lower.contains("summarize") -> {
                "Here is a concise structured breakdown:\n\n• **Core Principle**: Clarity drives execution\n• **Key Takeaway**: Prioritize progress over perfection\n• **Action Item**: Focus on the next immediate deliverable$memoryHighlights"
            }
            else -> {
                "That's an interesting question regarding \"$prompt\". As your personal assistant, I suggest analyzing the primary constraints first, exploring 2-3 alternate pathways, and choosing the one that aligns best with your core priorities.$memoryHighlights\n\n*(Tip: Connect your Gemini API key in Settings to unlock unlimited live cloud AI responses!)*"
            }
        }
    }
}
