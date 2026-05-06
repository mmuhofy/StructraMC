package com.muhofy.structra.block

import com.muhofy.structra.StructraClient
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

// CLIENT-SIDE ONLY
// Loads all valid block IDs from Minecraft's own registry at mod init.
// All AI response block IDs must be validated here before world placement.
// Fallback for unknown IDs: minecraft:stone
@Environment(EnvType.CLIENT)
object BlockRegistry {

    private val validIds = mutableSetOf<String>()
    private var initialized = false

    fun init() {
        if (initialized) return

        // Load all block IDs from Minecraft's own registry at runtime
        // This is the most reliable approach — no hardcoded lists
        for (block in BuiltInRegistries.BLOCK) {
            val key = BuiltInRegistries.BLOCK.getKey(block)
            if (key != null) {
                validIds.add(key.toString())
            }
        }

        initialized = true
        StructraClient.LOGGER.info(
            "BlockRegistry initialized: ${validIds.size} blocks loaded."
        )
    }

    // Returns the block ID if valid, or the fallback ID if not.
    // Logs a warning for invalid IDs — never crashes.
    fun validate(blockId: String): String {
        if (!initialized) {
            StructraClient.LOGGER.warn(
                "BlockRegistry.validate() called before init() — returning fallback."
            )
            return ModConstants.FALLBACK_BLOCK_ID
        }

        return if (validIds.contains(blockId)) {
            blockId
        } else {
            StructraClient.LOGGER.warn(
                "Unknown block ID from AI response: '$blockId' — replaced with fallback '${ModConstants.FALLBACK_BLOCK_ID}'"
            )
            ModConstants.FALLBACK_BLOCK_ID
        }
    }

    fun isValid(blockId: String): Boolean = validIds.contains(blockId)

    fun getAll(): Set<String> = validIds.toSet()
}