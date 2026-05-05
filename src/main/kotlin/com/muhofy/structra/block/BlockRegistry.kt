package com.muhofy.structra.block

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

// CLIENT-SIDE ONLY
// Loads valid block IDs from Minecraft's own registry at mod init
// All AI response block IDs validated here before world placement
// Full implementation: Phase 2
@Environment(EnvType.CLIENT)
object BlockRegistry {
    // TODO Phase 2: init() — populate from Minecraft block registry at runtime
    // TODO Phase 2: expose validate(blockId: String): String (fallback: minecraft:stone)
}