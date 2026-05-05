package com.muhofy.structra.api

// AI provider interface — implemented by GeminiService and OpenAiService
// All implementations must be CLIENT-SIDE ONLY
// See: com.muhofy.structra.api.GeminiService (Phase 3)
interface AiProvider {
    suspend fun generate(request: PromptRequest): StructureData
    suspend fun testConnection(apiKey: String): Boolean
    fun getProviderName(): String
}