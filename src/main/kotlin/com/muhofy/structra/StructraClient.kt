package com.muhofy.structra

import com.mojang.blaze3d.platform.InputConstants
import com.muhofy.structra.config.ConfigManager
import com.muhofy.structra.ui.SetupScreen
import com.muhofy.structra.ui.StructraScreen
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

// CLIENT-SIDE ONLY
@Environment(EnvType.CLIENT)
object StructraClient : ClientModInitializer {

    val LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID)

    // KeyMapping.Category.register(Identifier) — confirmed from MC 1.21.11 source (mcsrc.dev)
    private val STRUCTRA_CATEGORY: KeyMapping.Category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "structra")
    )

    private lateinit var openScreenKey: KeyMapping

    override fun onInitializeClient() {
        LOGGER.info("Structra initializing...")

        registerKeybinds()

        ConfigManager.init()
        // Phase 2: BlockRegistry.init()

        LOGGER.info("Structra initialized.")
    }

    private fun registerKeybinds() {
        // CLIENT-SIDE ONLY
        // Constructor: KeyMapping(String, InputConstants.Type, Int, KeyMapping.Category)
        // Confirmed from MC 1.21.11 source via mcsrc.dev
        openScreenKey = KeyMapping(
            ModConstants.KEY_OPEN_SCREEN,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            STRUCTRA_CATEGORY
        )

        KeyBindingHelper.registerKeyBinding(openScreenKey)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openScreenKey.consumeClick()) {
                when {
                    client.screen is StructraScreen -> Minecraft.getInstance().setScreen(null)
                    client.screen is SetupScreen -> Minecraft.getInstance().setScreen(null)
                    ConfigManager.isFirstLaunch() -> Minecraft.getInstance().setScreen(SetupScreen())
                    else -> Minecraft.getInstance().setScreen(StructraScreen())
                }
            }
        }
    }
}