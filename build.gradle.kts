plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

// Java 21 — required by Loom 1.14
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

loom {
    // Mojang Mappings — forward compatible with MC 26.1+
    // Yarn ends at 1.21.11; Mojang Mappings is the correct choice
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap") { name = "Ktor EAP" }
}

dependencies {
    // Minecraft + Loom
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Fabric API
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // Kotlin — fabric-language-kotlin bundles coroutines + serialization
    // DO NOT add kotlinx-coroutines-core or kotlinx-serialization-json separately
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("fabric_language_kotlin_version")}")

    // Ktor Client — async HTTP for Gemini/OpenAI API calls
    // CLIENT-SIDE ONLY — all AI calls are client-side
    // VERIFY: ktor_version confirmed at https://ktor.io/changelog/
    include(implementation("io.ktor:ktor-client-core:${project.property("ktor_version")}")!!)
    include(implementation("io.ktor:ktor-client-cio:${project.property("ktor_version")}")!!)
    include(implementation("io.ktor:ktor-client-content-negotiation:${project.property("ktor_version")}")!!)
    include(implementation("io.ktor:ktor-serialization-kotlinx-json:${project.property("ktor_version")}")!!)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", project.property("minecraft_version"))
        inputs.property("loader_version", project.property("loader_version"))
        inputs.property("fabric_language_kotlin_version", project.property("fabric_language_kotlin_version"))

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "minecraft_version" to project.property("minecraft_version")!!,
                "loader_version" to project.property("loader_version")!!,
                "fabric_language_kotlin_version" to project.property("fabric_language_kotlin_version")!!
            )
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }
}