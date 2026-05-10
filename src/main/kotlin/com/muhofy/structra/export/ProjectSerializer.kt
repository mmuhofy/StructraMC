package com.muhofy.structra.export

import com.muhofy.structra.StructraClient
import com.muhofy.structra.api.ChatMessage
import com.muhofy.structra.api.StructureData
import com.muhofy.structra.util.ModConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

// CLIENT-SIDE ONLY
// Serializes and deserializes .structra project files
// Save location: .minecraft/config/structra/projects/<uuid>.structra
object ProjectSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val projectsDir: Path = FabricLoader.getInstance()
        .configDir
        .resolve(ModConstants.CONFIG_DIR_NAME)
        .resolve(ModConstants.PROJECTS_DIR_NAME)

    fun save(project: StructraProject): Boolean {
        return try {
            Files.createDirectories(projectsDir)
            val file = projectsDir.resolve("${project.id}${ModConstants.PROJECT_FILE_EXTENSION}")
            Files.writeString(file, json.encodeToString(project))
            StructraClient.LOGGER.info("Project saved: ${project.id}")
            true
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to save project ${project.id}: ${e.message}")
            false
        }
    }

    fun load(id: String): StructraProject? {
        return try {
            val file = projectsDir.resolve("$id${ModConstants.PROJECT_FILE_EXTENSION}")
            if (!Files.exists(file)) return null
            json.decodeFromString<StructraProject>(Files.readString(file))
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to load project $id: ${e.message}")
            null
        }
    }

    fun listAll(): List<StructraProject> {
        return try {
            Files.createDirectories(projectsDir)
            Files.list(projectsDir)
                .filter { it.toString().endsWith(ModConstants.PROJECT_FILE_EXTENSION) }
                .mapNotNull { file ->
                    try {
                        json.decodeFromString<StructraProject>(Files.readString(file))
                    } catch (e: Exception) {
                        StructraClient.LOGGER.warn("Skipping corrupt project file: ${file.fileName}")
                        null
                    }
                }
                .toList()
                .sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to list projects: ${e.message}")
            emptyList()
        }
    }

    fun delete(id: String): Boolean {
        return try {
            val file = projectsDir.resolve("$id${ModConstants.PROJECT_FILE_EXTENSION}")
            Files.deleteIfExists(file)
        } catch (e: Exception) {
            StructraClient.LOGGER.error("Failed to delete project $id: ${e.message}")
            false
        }
    }

    fun newProject(name: String): StructraProject {
        return StructraProject(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
    }
}

// --- .structra project model ---

@Serializable
data class StructraProject(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastModified: Long,
    val versions: List<StructraVersion> = emptyList(),
    val promptHistory: List<ChatMessage> = emptyList()
)

@Serializable
data class StructraVersion(
    val versionNumber: Int,
    val prompt: String,
    val structure: StructureData,
    val timestamp: Long = System.currentTimeMillis()
)