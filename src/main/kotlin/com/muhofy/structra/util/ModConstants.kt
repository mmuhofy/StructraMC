package com.muhofy.structra.util

/**
 * All named constants for Structra.
 * No magic numbers or hardcoded strings anywhere else in the codebase.
 */
object ModConstants {

    // Mod identity
    const val MOD_ID = "structra"
    const val MOD_NAME = "Structra"

    // Keybinds
    const val KEY_OPEN_SCREEN = "key.structra.open_screen"
    const val KEY_CATEGORY = "key.categories.structra"

    // Ghost controls (handled in client tick — not standard keybinds)
    const val GHOST_CONFIRM_KEY = "g"
    const val GHOST_ROTATE_KEY = "r"

    // AI — Gemini
    const val GEMINI_MODEL = "gemini-3.1-flash-lite-preview"
    const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"
    const val GEMINI_MAX_RETRIES = 3
    const val GEMINI_BACKOFF_BASE_MS = 1000L

    // Structure limits (enforced in system instruction + validated in ResponseParser)
    const val STRUCTURE_MAX_X = 30
    const val STRUCTURE_MAX_Y = 30
    const val STRUCTURE_MAX_Z = 30

    // Fallback block ID for unknown/invalid AI-returned block IDs
    const val FALLBACK_BLOCK_ID = "minecraft:stone"

    // Config
    const val CONFIG_DIR_NAME = MOD_ID
    const val PROJECTS_DIR_NAME = "projects"
    const val PROJECT_FILE_EXTENSION = ".structra"

    // UI colors (ARGB)
    const val COLOR_BACKGROUND = 0xFF1A1A1A.toInt()
    const val COLOR_ACCENT_BROWN = 0xFF8B6914.toInt()
    const val COLOR_LIGHT_GRAY = 0xFFC6C6C6.toInt()
    const val COLOR_ACCENT_GREEN = 0xFF55FF55.toInt()
    const val COLOR_TEXT_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_GHOST_TINT = 0x660000FF  // semi-transparent blue for ghost blocks

    // UI layout
    const val CHAT_PANEL_WIDTH_RATIO = 0.55f  // fraction of screen width
    const val STATUS_BAR_HEIGHT = 14
    const val MESSAGE_BUBBLE_PADDING = 4
    const val SCROLL_SPEED = 10

    // Translation key prefixes
    const val KEY_PREFIX = "$MOD_ID."
}