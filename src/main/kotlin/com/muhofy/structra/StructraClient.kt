package com.muhofy.structra

import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.slf4j.LoggerFactory

// CLIENT-SIDE ONLY — Structra has zero server-side logic
@Environment(EnvType.CLIENT)
object StructraClient : ClientModInitializer {

    val LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID)

    override fun onInitializeClient() {
        LOGGER.info("Structra initializing...")

        // Phase 2: BlockRegistry.init()
        // Phase 1: ConfigManager.init()
        // Phase 0: KeyBindings registered here in next step

        LOGGER.info("Structra initialized successfully.")
    }
}