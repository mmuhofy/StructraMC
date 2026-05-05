package com.muhofy.structra.ui

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

// CLIENT-SIDE ONLY
// Main Structra UI screen — opened/closed via Y keybind
// Full implementation: Phase 5
@Environment(EnvType.CLIENT)
class StructraScreen : Screen(Component.literal("Structra")) {

    override fun isPauseScreen(): Boolean = false // game continues behind screen

}