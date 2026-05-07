package com.muhofy.structra.api

import com.muhofy.structra.util.ModConstants

// Builds the full Gemini API request from a PromptRequest.
// Combines system instruction + block registry + conversation history + user prompt.
object PromptBuilder {

    private fun buildSystemInstruction(blockRegistry: Set<String>): String {
        val registrySnippet = blockRegistry
            .sorted()
            .joinToString(",")

        return """
            You are a Minecraft structure generator. Your only job is to output valid JSON describing a 3D structure.
            
            STRICT RULES:
            - Respond ONLY with raw JSON. No markdown, no explanation, no backticks.
            - Maximum structure size: ${ModConstants.STRUCTURE_MAX_X}x${ModConstants.STRUCTURE_MAX_Y}x${ModConstants.STRUCTURE_MAX_Z} blocks.
            - Only use block IDs from this list: $registrySnippet
            - Never invent block IDs. If unsure, use minecraft:stone.
            - All coordinates start at 0,0,0.
            - Use the palette pattern to reduce response size.
            
            RESPONSE FORMAT (strict):
            {
              "structure": {
                "meta": {
                  "name": "string",
                  "description": "string",
                  "dimensions": { "x": int, "y": int, "z": int },
                  "block_count": int
                },
                "palette": [
                  { "index": int, "id": "minecraft:block_id" }
                ],
                "blocks": [
                  { "x": int, "y": int, "z": int, "p": int }
                ]
              }
            }
        """.trimIndent()
    }

    fun build(request: PromptRequest): GeminiRequest {
        val systemInstruction = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(buildSystemInstruction(request.blockRegistry)))
        )

        // Build conversation history as Gemini contents
        val historyContents = request.history.map { msg ->
            GeminiContent(
                role = msg.role,
                parts = listOf(GeminiPart(msg.text))
            )
        }

        // Add current user prompt
        val userContent = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(request.userPrompt))
        )

        return GeminiRequest(
            contents = historyContents + userContent,
            systemInstruction = systemInstruction
        )
    }
}