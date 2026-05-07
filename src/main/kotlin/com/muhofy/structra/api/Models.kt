package com.muhofy.structra.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Conversation ---

@Serializable
data class ChatMessage(
    val role: String,   // "user" or "model"
    val text: String
)

// --- Prompt ---

data class PromptRequest(
    val userPrompt: String,
    val history: List<ChatMessage> = emptyList(),
    val blockRegistry: Set<String> = emptySet()
)

// --- Structure Data (parsed AI output) ---

@Serializable
data class StructureMeta(
    val name: String,
    val description: String,
    val dimensions: StructureDimensions,
    @SerialName("block_count") val blockCount: Int
)

@Serializable
data class StructureDimensions(val x: Int, val y: Int, val z: Int)

@Serializable
data class PaletteEntry(val index: Int, val id: String)

@Serializable
data class BlockEntry(val x: Int, val y: Int, val z: Int, val p: Int)

@Serializable
data class StructureData(
    val meta: StructureMeta,
    val palette: List<PaletteEntry>,
    val blocks: List<BlockEntry>
)

// --- Gemini API request/response models ---

@Serializable
data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiContent? = null)

@Serializable
data class GeminiContent(val role: String, val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null, val error: GeminiError? = null)

@Serializable
data class GeminiCandidate(val content: GeminiContent)

@Serializable
data class GeminiError(val code: Int, val message: String)