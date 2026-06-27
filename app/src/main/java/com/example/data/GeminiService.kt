package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String, systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or using placeholder.")
            return@withContext "API Key is missing. Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                if (systemInstruction.isNotEmpty()) {
                    val systemInstructionObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", systemInstruction)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put("systemInstruction", systemInstructionObj)
                }

                // Low temperature for structured coaching advice
                val genConfig = JSONObject().apply {
                    put("temperature", 0.4)
                }
                put("generationConfig", genConfig)
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Error response: $errBody")
                    return@withContext "Failed to generate coaching. (HTTP ${response.code})"
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "Empty response from AI Coach."
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                        }
                    }
                }
                return@withContext "No response from AI Coach."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating coaching advice", e)
            return@withContext "Error: Could not connect to AI Coach. Please check your network connection."
        }
    }

    suspend fun getDailyTip(logs: List<DailyLog>): String {
        val summary = formatLogsSummary(logs)
        val prompt = """
            Here are my tracking logs for healthy habits (Water, Exercise, Sleep, Work Tasks):
            $summary
            
            Based on these logs, please give me a single, highly actionable, friendly wellness tip or hydration reminder (max 2 sentences). Start directly with the tip, no intro like "Here is your tip:".
        """.trimIndent()

        val system = "You are a friendly, encouraging wellness coach. You keep recommendations brief, actionable, and warm."
        return generateContent(prompt, system)
    }

    suspend fun getCoachingReport(logs: List<DailyLog>): String {
        val summary = formatLogsSummary(logs)
        val prompt = if (logs.isEmpty()) {
            "I am just starting my wellness journey today. Please write a warm, encouraging welcome message and a 3-step recommendation plan on how to build good tracking habits for water, daily exercise, and task completion."
        } else {
            """
                Here are my tracking logs:
                $summary
                
                Please provide:
                1. A brief analysis of my consistency (e.g., highlights of what I did well).
                2. Friendly recommendations on where to improve (e.g., if I missed water goals or exercise).
                3. A personalized motivational focus of the week.
                Keep the layout clean, elegant, with readable sections, using bullet points or clean markdown lists.
            """.trimIndent()
        }

        val system = "You are an expert personalized health and habit coach. You write elegant, concise, and professional feedback with high-quality motivational formatting. Avoid excessive length."
        return generateContent(prompt, system)
    }

    private fun formatLogsSummary(logs: List<DailyLog>): String {
        if (logs.isEmpty()) return "No logs recorded yet."
        val recentLogs = logs.takeLast(7) // Last 7 days
        return recentLogs.joinToString("\n") { log ->
            "Date: ${log.date} | Water: ${log.waterMl}/${log.waterGoalMl} mL | Exercise: ${log.exerciseMinutes} mins (${log.exerciseType}) | Sleep: ${log.sleepMinutes / 60}h ${log.sleepMinutes % 60}m | Habits: ${log.habitsChecked}"
        }
    }
}
