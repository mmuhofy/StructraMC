package com.muhofy.structra.world

import com.muhofy.structra.StructraClient
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

// CLIENT-SIDE ONLY
// Ghost rendering is deferred — full implementation in Phase 8
// WorldRenderEvents pipeline requires additional type resolution
@Environment(EnvType.CLIENT)
object GhostRenderer {

    fun register() {
        // TODO Phase 8: implement ghost block rendering with WorldRenderEvents
        // Blocked on: MeshData.vertexBuffer() Kotlin nullable stub issue
        StructraClient.LOGGER.info("GhostRenderer: stub registered, rendering deferred to Phase 8.")
    }

    fun close() {
        // TODO Phase 8: cleanup GPU resources
    }
}