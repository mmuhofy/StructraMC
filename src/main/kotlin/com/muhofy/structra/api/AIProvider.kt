package com.muhofy.structra.api

// AI provider interface — implemented by GeminiService and OpenAiService
// All implementations are CLIENT-SIDE ONLY
interface AiProvider {
    suspend fun generate(request: PromptRequest): StructureData
    suspend fun testConnection(apiKey: String): Boolean
    fun getProviderName(): String
}