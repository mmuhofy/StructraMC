package com.muhofy.structra.config

import com.muhofy.structra.StructraClient
import com.muhofy.structra.util.ModConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import java.nio.file.Files
import java.nio.file.Path

// CLIENT-SIDE ONLY
// Manages Structra config — API key (plaintext for now, encrypt in Phase 8),
// provider selection, language preference, and first-launch detection.
// Config file: .minecraft/config/structra/config.json
@Environment(EnvType.CLIENT)
object ConfigManager {

    // SECURITY: never log api key — even though plaintext for now, keep this habit
    private val configDir: Path = FabricLoader.getInstance()
        .configDir
        .resolve(ModConstants.CONFIG_DIR_NAME)

    private val configFile: Path = configDir.resolve("config.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // In-memory config state
    private var config: StructraConfig = StructraConfig()

    fun init() {
        try {
            Files.createDirectories(configDir)
            if (Files.exists(configFile)) {
                val raw = Files.readString(configFile)
                config = json.decodeFromString<StructraConfig>(raw)
                StructraClient.LOGGER.info("Structra config loaded.")
            } else {
                // First launch — no config file yet
                StructraClient.LOGGER.info("Structra first launch detected — no config file found.")
                save()
            }
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to load Structra config, using defaults: ${e.message}")
            config = StructraConfig()
        }
    }

    fun save() {
        try {
            Files.createDirectories(configDir)
            // SECURITY: never log api key
            Files.writeString(configFile, json.encodeToString(config))
            StructraClient.LOGGER.info("Structra config saved.")
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to save Structra config: ${e.message}")
        }
    }

    // --- Accessors ---

    fun isFirstLaunch(): Boolean =
        config.apiKey.isBlank()

    // SECURITY: never log api key
    fun getApiKey(): String = config.apiKey

    fun setApiKey(key: String) {
        // SECURITY: never log api key
        config = config.copy(apiKey = key)
        save()
    }

    fun getProvider(): AiProviderType = config.provider

    fun setProvider(provider: AiProviderType) {
        config = config.copy(provider = provider)
        save()
    }

    fun isSetupComplete(): Boolean = config.setupComplete

    fun markSetupComplete() {
        config = config.copy(setupComplete = true)
        save()
    }
}

// Supported AI providers
@Serializable
enum class AiProviderType {
    GEMINI,
    OPENAI
}

// Config data model — all fields have safe defaults
@Serializable
data class StructraConfig(
    // SECURITY: never log api key
    val apiKey: String = "",
    val provider: AiProviderType = AiProviderType.GEMINI,
    val setupComplete: Boolean = false
)