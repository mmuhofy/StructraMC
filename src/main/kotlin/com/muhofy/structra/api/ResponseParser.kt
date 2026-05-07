package com.muhofy.structra.api

import com.muhofy.structra.StructraClient
import com.muhofy.structra.block.BlockRegistry
import com.muhofy.structra.util.ModConstants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Parses and validates raw JSON from Gemini into StructureData.
// Validates all block IDs against BlockRegistry — replaces unknowns with fallback.
object ResponseParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // UNTESTED — verify with real Gemini responses before use
    fun parse(rawJson: String): StructureData {
        // Strip markdown fences if Gemini ignores system instruction
        val cleaned = rawJson
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = json.parseToJsonElement(cleaned).jsonObject
        val structureNode = root["structure"]?.jsonObject
            ?: throw IllegalArgumentException("Missing 'structure' key in response")

        val parsed = json.decodeFromJsonElement(StructureData.serializer(), structureNode)

        return validateBlockIds(parsed)
    }

    // Validates all palette block IDs — replaces unknown IDs with fallback
    private fun validateBlockIds(data: StructureData): StructureData {
        val validatedPalette = data.palette.map { entry ->
            val validId = BlockRegistry.validate(entry.id)
            if (validId != entry.id) {
                StructraClient.LOGGER.warn(
                    "ResponseParser: replaced unknown block '${entry.id}' with '${ModConstants.FALLBACK_BLOCK_ID}'"
                )
            }
            entry.copy(id = validId)
        }

        // Validate dimensions against max limits
        val dims = data.meta.dimensions
        if (dims.x > ModConstants.STRUCTURE_MAX_X ||
            dims.y > ModConstants.STRUCTURE_MAX_Y ||
            dims.z > ModConstants.STRUCTURE_MAX_Z
        ) {
            StructraClient.LOGGER.warn(
                "ResponseParser: structure dimensions ${dims.x}x${dims.y}x${dims.z} exceed " +
                "max ${ModConstants.STRUCTURE_MAX_X}x${ModConstants.STRUCTURE_MAX_Y}x${ModConstants.STRUCTURE_MAX_Z}"
            )
        }

        return data.copy(palette = validatedPalette)
    }
}