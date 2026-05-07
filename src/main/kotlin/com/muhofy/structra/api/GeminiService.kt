package com.muhofy.structra.api

import com.muhofy.structra.StructraClient
import com.muhofy.structra.block.BlockRegistry
import com.muhofy.structra.config.ConfigManager
import com.muhofy.structra.util.ModConstants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

// CLIENT-SIDE ONLY
// Calls Gemini REST API asynchronously via Ktor.
// Implements retry with exponential backoff on 429 rate limit.
// UNTESTED — verify with real API key before use
object GeminiService : AiProvider {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    override fun getProviderName(): String = "Gemini"

    override suspend fun generate(request: PromptRequest): StructureData {
        val apiKey = ConfigManager.getApiKey()
        // SECURITY: never log api key

        val geminiRequest = PromptBuilder.build(
            request.copy(blockRegistry = BlockRegistry.getAll())
        )

        val rawJson = callWithRetry(apiKey, geminiRequest)
        return ResponseParser.parse(rawJson)
    }

    override suspend fun testConnection(apiKey: String): Boolean {
        // SECURITY: never log api key
        return try {
            val testRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart("Reply with: OK"))
                    )
                )
            )
            val response = callApi(apiKey, testRequest)
            response.candidates?.isNotEmpty() == true
        } catch (e: Exception) {
            StructraClient.LOGGER.warn("Gemini test connection failed: ${e.message}")
            false
        }
    }

    // Calls Gemini API with exponential backoff on 429
    // UNTESTED — verify retry behavior before use
    private suspend fun callWithRetry(apiKey: String, request: GeminiRequest): String {
        var attempt = 0
        while (attempt < ModConstants.GEMINI_MAX_RETRIES) {
            try {
                val response = callApi(apiKey, request)

                if (response.error != null) {
                    throw RuntimeException("Gemini error ${response.error.code}: ${response.error.message}")
                }

                val text = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?: throw RuntimeException("Gemini returned empty response")

                return text

            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.TooManyRequests) {
                    val backoffMs = ModConstants.GEMINI_BACKOFF_BASE_MS * (1L shl attempt)
                    StructraClient.LOGGER.warn(
                        "Gemini 429 rate limit — retrying in ${backoffMs}ms (attempt ${attempt + 1}/${ModConstants.GEMINI_MAX_RETRIES})"
                    )
                    delay(backoffMs)
                    attempt++
                } else {
                    throw RuntimeException("Gemini request failed: ${e.response.status}")
                }
            }
        }
        throw RuntimeException("Gemini max retries exceeded (${ModConstants.GEMINI_MAX_RETRIES})")
    }

    private suspend fun callApi(apiKey: String, request: GeminiRequest): GeminiResponse {
        // SECURITY: never log api key
        val url = "${ModConstants.GEMINI_ENDPOINT}?key=$apiKey"
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<GeminiResponse>()
    }
}