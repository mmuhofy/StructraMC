package com.muhofy.structra.ui

import com.muhofy.structra.config.AiProviderType
import com.muhofy.structra.config.ConfigManager
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence

// CLIENT-SIDE ONLY
// Setup screen shown on first launch (no API key stored)
// Flow: WELCOME → PROVIDER → API_KEY → TEST → DONE
// Accessible later via: StructraScreen → Settings
@Environment(EnvType.CLIENT)
class SetupScreen(private val previousScreen: Screen? = null) : Screen(
    Component.translatable("structra.setup.title")
) {

    // --- State machine ---
    private enum class SetupStep { WELCOME, PROVIDER, API_KEY, TEST, DONE }
    private var currentStep = SetupStep.WELCOME

    // UI state
    private var selectedProvider: AiProviderType = ConfigManager.getProvider()
    private var apiKeyInput: EditBox? = null
    private var testResultMessage: Component? = null
    private var testInProgress = false

    // Layout constants
    private val panelW = 320
    private val panelH = 240
    private var panelX = 0
    private var panelY = 0

    override fun init() {
        panelX = (width - panelW) / 2
        panelY = (height - panelH) / 2
        buildWidgets()
    }

    // Rebuild all widgets for the current step
    private fun buildWidgets() {
        clearWidgets()
        testResultMessage = null

        when (currentStep) {
            SetupStep.WELCOME -> buildWelcomeWidgets()
            SetupStep.PROVIDER -> buildProviderWidgets()
            SetupStep.API_KEY -> buildApiKeyWidgets()
            SetupStep.TEST -> buildTestWidgets()
            SetupStep.DONE -> buildDoneWidgets()
        }
    }

    // --- WELCOME ---
    private fun buildWelcomeWidgets() {
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.welcome.next")) {
                currentStep = SetupStep.PROVIDER
                buildWidgets()
            }
                .pos(panelX + panelW / 2 - 75, panelY + panelH - 50)
                .size(150, 20)
                .build()
        )
    }

    // --- PROVIDER ---
    private fun buildProviderWidgets() {
        val centerX = panelX + panelW / 2

        // Gemini button
        addRenderableWidget(
            Button.builder(Component.literal("Gemini")) {
                selectedProvider = AiProviderType.GEMINI
                buildWidgets()
            }
                .pos(centerX - 155, panelY + 110)
                .size(150, 20)
                .build()
        )

        // OpenAI button
        addRenderableWidget(
            Button.builder(Component.literal("OpenAI")) {
                selectedProvider = AiProviderType.OPENAI
                buildWidgets()
            }
                .pos(centerX + 5, panelY + 110)
                .size(150, 20)
                .build()
        )

        // Next — only enabled when a provider is selected
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.provider.next")) {
                ConfigManager.setProvider(selectedProvider)
                currentStep = SetupStep.API_KEY
                buildWidgets()
            }
                .pos(centerX - 75, panelY + panelH - 50)
                .size(150, 20)
                .build()
        )

        // Back
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.back")) {
                currentStep = SetupStep.WELCOME
                buildWidgets()
            }
                .pos(panelX + 10, panelY + panelH - 50)
                .size(70, 20)
                .build()
        )
    }

    // --- API KEY ---
    private fun buildApiKeyWidgets() {
        val centerX = panelX + panelW / 2

        // Masked API key input
        val box = EditBox(
            font,
            centerX - 130,
            panelY + 110,
            260,
            20,
            Component.translatable("structra.setup.apikey.placeholder")
        ).also {
            it.setMaxLength(256)
            it.value = ""
            // Mask input — show dots instead of actual key characters
            // addFormatter confirmed from MC 1.21.11 source (mcsrc.dev)
            it.addFormatter { text, _ ->
                FormattedCharSequence.forward("•".repeat(text.length), Style.EMPTY)
            }
        }
        apiKeyInput = box
        addRenderableWidget(box)
        setInitialFocus(box)

        // Next
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.apikey.next")) {
                val key = apiKeyInput?.value?.trim() ?: ""
                if (key.isNotBlank()) {
                    ConfigManager.setApiKey(key)
                    currentStep = SetupStep.TEST
                    buildWidgets()
                }
            }
                .pos(centerX - 75, panelY + panelH - 50)
                .size(150, 20)
                .build()
        )

        // Back
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.back")) {
                currentStep = SetupStep.PROVIDER
                buildWidgets()
            }
                .pos(panelX + 10, panelY + panelH - 50)
                .size(70, 20)
                .build()
        )
    }

    // --- TEST ---
    private fun buildTestWidgets() {
        val centerX = panelX + panelW / 2

        // Test connection button
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.test.button")) {
                runConnectionTest()
            }
                .pos(centerX - 75, panelY + 110)
                .size(150, 20)
                .build()
        )

        // Skip / continue anyway
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.test.skip")) {
                finishSetup()
            }
                .pos(centerX - 75, panelY + panelH - 50)
                .size(150, 20)
                .build()
        )

        // Back
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.back")) {
                currentStep = SetupStep.API_KEY
                buildWidgets()
            }
                .pos(panelX + 10, panelY + panelH - 50)
                .size(70, 20)
                .build()
        )
    }

    // --- DONE ---
    private fun buildDoneWidgets() {
        addRenderableWidget(
            Button.builder(Component.translatable("structra.setup.done.button")) {
                ConfigManager.markSetupComplete()
                Minecraft.getInstance().setScreen(StructraScreen())
            }
                .pos(panelX + panelW / 2 - 75, panelY + panelH - 50)
                .size(150, 20)
                .build()
        )
    }

    // --- Test connection ---
    // UNTESTED — verify async behavior before use
    private fun runConnectionTest() {
        if (testInProgress) return
        testInProgress = true
        testResultMessage = Component.translatable("structra.setup.test.running")

        // Run off game thread — never block render thread
        Thread {
            try {
                // TODO Phase 3: replace with real AiProvider.testConnection()
                // Placeholder: simulate a quick check
                val apiKey = ConfigManager.getApiKey()
                // SECURITY: never log api key
                val success = apiKey.isNotBlank() // placeholder logic

                Minecraft.getInstance().execute {
                    testInProgress = false
                    if (success) {
                        testResultMessage = Component.translatable("structra.setup.test.success")
                        // Auto-advance to DONE after successful test
                        currentStep = SetupStep.DONE
                        buildWidgets()
                    } else {
                        testResultMessage = Component.translatable("structra.setup.test.fail")
                    }
                }
            } catch (e: Exception) {
                Minecraft.getInstance().execute {
                    testInProgress = false
                    testResultMessage = Component.translatable("structra.setup.test.fail")
                }
            }
        }.start()
    }

    private fun finishSetup() {
        ConfigManager.markSetupComplete()
        Minecraft.getInstance().setScreen(StructraScreen())
    }

    // --- Rendering ---
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Dim background
        renderBackground(graphics, mouseX, mouseY, delta)

        // Panel background
        graphics.fill(
            panelX, panelY,
            panelX + panelW, panelY + panelH,
            ModConstants.COLOR_BACKGROUND
        )

        // Panel border
        graphics.renderOutline(
            panelX, panelY,
            panelW, panelH,
            ModConstants.COLOR_ACCENT_BROWN
        )

        // Step title
        val stepTitle = getStepTitle()
        graphics.drawCenteredString(
            font,
            stepTitle,
            panelX + panelW / 2,
            panelY + 16,
            ModConstants.COLOR_TEXT_WHITE
        )

        // Step description
        val stepDesc = getStepDescription()
        graphics.drawCenteredString(
            font,
            stepDesc,
            panelX + panelW / 2,
            panelY + 60,
            ModConstants.COLOR_LIGHT_GRAY
        )

        // Provider selection indicator
        if (currentStep == SetupStep.PROVIDER) {
            val providerLabel = Component.literal(
                "▶ ${selectedProvider.name}"
            )
            graphics.drawCenteredString(
                font,
                providerLabel,
                panelX + panelW / 2,
                panelY + 85,
                ModConstants.COLOR_ACCENT_GREEN
            )
        }

        // Test result message
        testResultMessage?.let {
            graphics.drawCenteredString(
                font, it,
                panelX + panelW / 2,
                panelY + 140,
                ModConstants.COLOR_ACCENT_GREEN
            )
        }

        // Step indicator: e.g. "2 / 4"
        val stepIndex = SetupStep.entries.indexOf(currentStep) + 1
        val stepTotal = SetupStep.entries.size - 1 // exclude DONE from count
        if (currentStep != SetupStep.DONE) {
            graphics.drawCenteredString(
                font,
                Component.literal("$stepIndex / $stepTotal"),
                panelX + panelW / 2,
                panelY + panelH - 20,
                ModConstants.COLOR_LIGHT_GRAY
            )
        }

        super.render(graphics, mouseX, mouseY, delta)
    }

    private fun getStepTitle(): Component = when (currentStep) {
        SetupStep.WELCOME  -> Component.translatable("structra.setup.welcome.title")
        SetupStep.PROVIDER -> Component.translatable("structra.setup.provider.title")
        SetupStep.API_KEY  -> Component.translatable("structra.setup.apikey.title")
        SetupStep.TEST     -> Component.translatable("structra.setup.test.title")
        SetupStep.DONE     -> Component.translatable("structra.setup.done.title")
    }

    private fun getStepDescription(): Component = when (currentStep) {
        SetupStep.WELCOME  -> Component.translatable("structra.setup.welcome.desc")
        SetupStep.PROVIDER -> Component.translatable("structra.setup.provider.desc")
        SetupStep.API_KEY  -> Component.translatable("structra.setup.apikey.desc",
            selectedProvider.name)
        SetupStep.TEST     -> Component.translatable("structra.setup.test.desc")
        SetupStep.DONE     -> Component.translatable("structra.setup.done.desc")
    }

    override fun isPauseScreen(): Boolean = true

    override fun onClose() {
        Minecraft.getInstance().setScreen(previousScreen)
    }
}