package com.muhofy.structra.ui

import com.muhofy.structra.api.AiProvider
import com.muhofy.structra.api.ChatMessage
import com.muhofy.structra.api.GeminiService
import com.muhofy.structra.api.PromptRequest
import com.muhofy.structra.api.StructureData
import com.muhofy.structra.config.ConfigManager
import com.muhofy.structra.util.ModConstants
import com.muhofy.structra.world.GhostState
import com.muhofy.structra.world.WorldPlacer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

// CLIENT-SIDE ONLY
// Main Structra UI screen — opened/closed via Y keybind
// Game continues running behind screen (isPauseScreen = false)
@Environment(EnvType.CLIENT)
class StructraScreen : Screen(Component.translatable("structra.screen.title")) {

    // --- State ---
    private val messages = mutableListOf<ChatEntry>()
    private val history = mutableListOf<ChatMessage>()
    private var isGenerating = false
    private var scrollOffset = 0
    private var activeStructure: StructureData? = null

    private val aiProvider: AiProvider = GeminiService
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Suggestion chips
    private val suggestions = listOf(
        "structra.suggestion.roof",
        "structra.suggestion.enlarge",
        "structra.suggestion.shrink",
        "structra.suggestion.window",
        "structra.suggestion.second_floor"
    )

    // Widgets
    private var promptInput: EditBox? = null

    // Layout
    private var chatPanelW = 0
    private var rightPanelX = 0
    private val statusBarH = ModConstants.STATUS_BAR_HEIGHT
    private val suggestionRowH = 20
    private val inputBoxH = 22
    private val bottomAreaH = suggestionRowH + inputBoxH + 8

    // --- Chat entry model ---
    data class ChatEntry(
        val role: Role,
        val text: String,
        val structure: StructureData? = null,
        val version: Int = 0,
        var confirmed: Boolean = false
    ) {
        enum class Role { USER, AI, SYSTEM }
    }

    override fun init() {
        chatPanelW = (width * ModConstants.CHAT_PANEL_WIDTH_RATIO).toInt()
        rightPanelX = chatPanelW + 4

        buildWidgets()
    }

    private fun buildWidgets() {
        clearWidgets()

        val chatBottom = height - statusBarH - bottomAreaH

        // --- Prompt input box ---
        val inputY = height - statusBarH - inputBoxH - 4
        val input = EditBox(
            font,
            4, inputY,
            chatPanelW - 30, inputBoxH,
            Component.translatable("structra.chat.placeholder")
        ).also {
            it.setMaxLength(512)
        }
        promptInput = input
        addRenderableWidget(input)

        // Send button
        addRenderableWidget(
            Button.builder(Component.literal("➤")) {
                sendPrompt()
            }
                .pos(chatPanelW - 24, inputY)
                .size(22, inputBoxH)
                .build()
        )

        // --- Suggestion chips ---
        val chipY = inputY - suggestionRowH - 2
        var chipX = 4
        suggestions.forEach { key ->
            val label = Component.translatable(key)
            val chipW = font.width(label) + 10
            addRenderableWidget(
                Button.builder(label) {
                    promptInput?.value = minecraft?.languageManager?.getSelected()
                        ?.let { lang -> label.string } ?: label.string
                }
                    .pos(chipX, chipY)
                    .size(chipW, 16)
                    .build()
            )
            chipX += chipW + 4
        }

        // --- Onayla / İptal buttons ---
        // Only shown on latest unconfirmed AI message
        val latestUnconfirmed = messages.lastOrNull { it.role == ChatEntry.Role.AI && !it.confirmed }
        if (latestUnconfirmed != null) {
            val btnY = chatBottom - 22
            addRenderableWidget(
                Button.builder(Component.translatable("structra.chat.confirm")) {
                    confirmStructure(latestUnconfirmed)
                }
                    .pos(4, btnY)
                    .size(80, 18)
                    .build()
            )
            addRenderableWidget(
                Button.builder(Component.translatable("structra.chat.cancel")) {
                    cancelStructure(latestUnconfirmed)
                }
                    .pos(88, btnY)
                    .size(60, 18)
                    .build()
            )
        }

        // --- Settings button ---
        addRenderableWidget(
            Button.builder(Component.literal("⚙")) {
                // TODO Phase 6: open settings overlay
            }
                .pos(width - 22, 2)
                .size(20, 16)
                .build()
        )
    }

    // --- Rendering ---
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(graphics, mouseX, mouseY, delta)
        renderChatPanel(graphics)
        renderRightPanel(graphics)
        renderStatusBar(graphics)
        super.render(graphics, mouseX, mouseY, delta)
    }

    private fun renderChatPanel(graphics: GuiGraphics) {
        // Panel background
        graphics.fill(0, 0, chatPanelW, height - statusBarH, ModConstants.COLOR_BACKGROUND)

        // Divider
        graphics.fill(chatPanelW, 0, chatPanelW + 2, height, ModConstants.COLOR_ACCENT_BROWN)

        // Messages
        val msgAreaTop = 4
        val msgAreaBottom = height - statusBarH - bottomAreaH - 26
        val msgAreaH = msgAreaBottom - msgAreaTop

        graphics.enableScissor(0, msgAreaTop, chatPanelW, msgAreaBottom)

        var y = msgAreaTop - scrollOffset
        messages.forEach { entry ->
            y = renderMessage(graphics, entry, y, msgAreaTop, msgAreaBottom)
        }

        graphics.disableScissor()
    }

    private fun renderMessage(
        graphics: GuiGraphics,
        entry: ChatEntry,
        startY: Int,
        areaTop: Int,
        areaBottom: Int
    ): Int {
        val pad = ModConstants.MESSAGE_BUBBLE_PADDING
        val maxW = chatPanelW - 16
        val lines = font.split(Component.literal(entry.text), maxW)
        val bubbleH = lines.size * (font.lineHeight + 1) + pad * 2

        val isUser = entry.role == ChatEntry.Role.USER
        val bgColor = if (isUser) 0xFF2A2A2A.toInt() else 0xFF1E3A2A.toInt()
        val textColor = if (isUser) ModConstants.COLOR_LIGHT_GRAY else ModConstants.COLOR_ACCENT_GREEN

        val x = if (isUser) chatPanelW - maxW - 8 else 4

        if (startY + bubbleH > areaTop && startY < areaBottom) {
            graphics.fill(x, startY, x + maxW + pad * 2, startY + bubbleH, bgColor)
            lines.forEachIndexed { i, line ->
                graphics.drawString(font, line, x + pad, startY + pad + i * (font.lineHeight + 1), textColor, false)
            }

            // Version label for AI messages
            if (entry.role == ChatEntry.Role.AI && entry.version > 0) {
                graphics.drawString(
                    font,
                    Component.literal("V${entry.version}"),
                    x + maxW - 20,
                    startY + bubbleH - font.lineHeight - pad,
                    ModConstants.COLOR_LIGHT_GRAY,
                    false
                )
            }
        }

        return startY + bubbleH + 4
    }

    private fun renderRightPanel(graphics: GuiGraphics) {
        val panelW = width - rightPanelX - 2
        graphics.fill(rightPanelX, 0, width - 2, height - statusBarH, ModConstants.COLOR_BACKGROUND)

        val structure = activeStructure

        // Preview area (top half of right panel)
        val previewH = (height - statusBarH) / 2
        graphics.fill(rightPanelX + 2, 2, width - 4, previewH, 0xFF111111.toInt())
        graphics.drawCenteredString(
            font,
            Component.translatable("structra.preview.label"),
            rightPanelX + panelW / 2,
            previewH / 2,
            ModConstants.COLOR_LIGHT_GRAY
        )

        // Ghost badge
        if (GhostState.active) {
            graphics.drawString(
                font,
                Component.translatable("structra.ghost.active"),
                rightPanelX + 4,
                previewH + 4,
                ModConstants.COLOR_ACCENT_GREEN,
                false
            )
        }

        // Structure info
        if (structure != null) {
            val infoY = previewH + 20
            graphics.drawString(font,
                Component.literal("${structure.meta.name} · ${structure.meta.dimensions.x}×${structure.meta.dimensions.y}×${structure.meta.dimensions.z} · ${structure.meta.blockCount} blocks"),
                rightPanelX + 4, infoY, ModConstants.COLOR_LIGHT_GRAY, false
            )

            // Materials list (palette)
            structure.palette.take(5).forEachIndexed { i, entry ->
                graphics.drawString(font,
                    Component.literal(entry.id),
                    rightPanelX + 4, infoY + 16 + i * 12,
                    ModConstants.COLOR_LIGHT_GRAY, false
                )
            }
        }
    }

    private fun renderStatusBar(graphics: GuiGraphics) {
        val y = height - statusBarH
        graphics.fill(0, y, width, height, 0xFF111111.toInt())

        val versionCount = messages.count { it.role == ChatEntry.Role.AI }
        val statusText = if (isGenerating)
            Component.translatable("structra.status.generating")
        else if (GhostState.active)
            Component.translatable("structra.status.ghost_active")
        else
            Component.translatable("structra.status.idle", versionCount)

        graphics.drawString(font, statusText, 4, y + 2, ModConstants.COLOR_LIGHT_GRAY, false)

        // MC version (right side)
        graphics.drawString(font,
            Component.literal("Fabric ${ModConstants.MOD_NAME} 1.21.11"),
            width - font.width("Fabric ${ModConstants.MOD_NAME} 1.21.11") - 4,
            y + 2,
            ModConstants.COLOR_LIGHT_GRAY,
            false
        )
    }

    // --- Actions ---
    private fun sendPrompt() {
        val text = promptInput?.value?.trim() ?: return
        if (text.isBlank() || isGenerating) return

        promptInput?.value = ""
        messages.add(ChatEntry(ChatEntry.Role.USER, text))
        history.add(ChatMessage("user", text))
        isGenerating = true
        buildWidgets()

        coroutineScope.launch {
            try {
                val result = aiProvider.generate(
                    PromptRequest(userPrompt = text, history = history.dropLast(1))
                )
                val version = messages.count { it.role == ChatEntry.Role.AI } + 1
                Minecraft.getInstance().execute {
                    messages.add(ChatEntry(ChatEntry.Role.AI, result.meta.description, result, version))
                    history.add(ChatMessage("model", result.meta.description))
                    activeStructure = result
                    GhostState.activate(result, Minecraft.getInstance().player?.blockPosition() ?: net.minecraft.core.BlockPos.ZERO)
                    isGenerating = false
                    buildWidgets()
                }
            } catch (e: Exception) {
                Minecraft.getInstance().execute {
                    messages.add(ChatEntry(ChatEntry.Role.SYSTEM, "Error: ${e.message}"))
                    isGenerating = false
                    buildWidgets()
                }
            }
        }
    }

    private fun confirmStructure(entry: ChatEntry) {
        val structure = entry.structure ?: return
        entry.confirmed = true
        WorldPlacer.place(structure)
        GhostState.deactivate()
        buildWidgets()
    }

    private fun cancelStructure(entry: ChatEntry) {
        entry.confirmed = true
        GhostState.deactivate()
        buildWidgets()
    }

    // --- Scrolling ---
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (mouseX < chatPanelW) {
            scrollOffset = (scrollOffset - (scrollY * ModConstants.SCROLL_SPEED).toInt()).coerceAtLeast(0)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        GhostState.deactivate()
        Minecraft.getInstance().setScreen(null)
    }
}