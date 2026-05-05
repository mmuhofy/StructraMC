package com.muhofy.structra.export

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

// CLIENT-SIDE ONLY
// Serializes/deserializes .structra project files
// Full implementation: Phase 6
@Environment(EnvType.CLIENT)
object ProjectSerializer {
    // TODO Phase 6: serialize StructureData + version history + prompt history → .structra JSON
    // TODO Phase 6: deserialize .structra → full project state
}