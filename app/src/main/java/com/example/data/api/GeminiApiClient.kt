package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST request/response structures ---

data class Part(val text: String)
data class Content(val parts: List<Part>)

data class ResponseFormatText(
    val mimeType: String
)

data class ResponseFormat(
    val type: String = "OBJECT",
    val text: ResponseFormatText? = null
)

data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

data class Candidate(val content: Content)
data class GenerateContentResponse(val candidates: List<Candidate>?)

// --- Clean structure representing the parsed Fact Check result ---
data class FactCheckResult(
    val verdict: String,       // True, False, Misleading, Partially True, Unverified
    val confidence: Int,       // 0 to 100
    val analysis: String,      // Detailed reasoning
    val context: String,       // Background context
    val references: String     // List of references/sources
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Fact-checks a claim using Gemini and returns a parsed FactCheckResult or throws an exception.
     */
    suspend fun factCheckClaim(claimText: String): FactCheckResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing or default placeholder. Please configure your GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        val prompt = """
            Claim to verify: "$claimText"
            
            Perform a thorough fact check. Act as an expert open-source news verification agency like AltNews, BoomLive, or Fact Crescendo.
            Analyze details, check for common hoaxes, assess the level of manipulation, and verify facts.
            
            Return a JSON object with this exact schema:
            {
              "verdict": "False" | "True" | "Misleading" | "Partially True" | "Unverified",
              "confidence": number (rating 0 to 100),
              "analysis": "detailed explanation of why this is true or false, discussing original sources or photo manipulation details",
              "context": "background explanation, context, and where the hoax originated",
              "references": "numbered list of key validation sources"
            }
            
            Important note for "verdict": select ONLY from these 5 values.
        """.trimIndent()

        val systemInstruction = """
            You are Fact Live Agent, an Elite Journalist and Fact-Checking engine. 
            You must analyze the claims and reply ONLY with the requested JSON object format.
            Do not output any introductory words, explanation, or markdown wrapping. Just pure, parseable JSON.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        val response = service.generateContent(apiKey, request)
        val rawResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Empty response received from verification server.")

        return parseFactResponse(rawResponseText)
    }

    private fun parseFactResponse(rawText: String): FactCheckResult {
        // Clean markdown code blocks if any
        val cleanText = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanText) ?: throw Exception("Failed to deserialize JSON")
            
            val verdict = (map["verdict"] as? String)?.trim() ?: "Unverified"
            val confidence = (map["confidence"] as? Double)?.toInt() 
                ?: (map["confidence"] as? String)?.toIntOrNull() 
                ?: 50
            val analysis = (map["analysis"] as? String) ?: "No analysis provided."
            val context = (map["context"] as? String) ?: "No context provided."
            val references = (map["references"] as? String) ?: "Verify using primary news sites."

            FactCheckResult(verdict, confidence, analysis, context, references)
        } catch (e: Exception) {
            // Handle parsing fallback gracefully
            FactCheckResult(
                verdict = "Unverified",
                confidence = 30,
                analysis = "Evaluation failed due to response format constraints. Raw Output:\n$cleanText",
                context = "Unparseable response received from validation model.",
                references = "Try performing a search query on official fact check channels."
            )
        }
    }
}
