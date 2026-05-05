package com.muhofy.structra.world

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

// CLIENT-SIDE ONLY
// Renders ghost block preview via WorldRenderEvents (FAPI)
// Full implementation: Phase 4
@Environment(EnvType.CLIENT)
object GhostRenderer {
    // TODO Phase 4: register WorldRenderEvents.AFTER_TRANSLUCENT_BLOCK
    // TODO Phase 4: render semi-transparent ghost blocks (blue tint)
}