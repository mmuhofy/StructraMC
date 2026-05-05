package com.muhofy.structra.config

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

// CLIENT-SIDE ONLY
// Manages API key (encrypted), language preference, and first-launch detection
// Full implementation: Phase 1
@Environment(EnvType.CLIENT)
object ConfigManager {
    // SECURITY: never log api key
    // TODO Phase 1: load/save config from Fabric config directory
    // TODO Phase 1: detect first launch (no API key stored)
}