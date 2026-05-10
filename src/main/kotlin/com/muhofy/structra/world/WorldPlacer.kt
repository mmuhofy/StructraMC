package com.muhofy.structra.world

import com.muhofy.structra.StructraClient
import com.muhofy.structra.api.StructureData
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState

// CLIENT-SIDE ONLY
// Places blocks from StructureData into the world.
// Stores previous block states for undo support.
// UNTESTED — verify thread safety and setBlockState usage before use
@Environment(EnvType.CLIENT)
object WorldPlacer {

    // Undo stack — stores previous block states per placement session
    private val undoStack: ArrayDeque<List<UndoEntry>> = ArrayDeque()

    data class UndoEntry(val pos: BlockPos, val previousState: BlockState)

    // Places all blocks from structure at GhostState.offset with GhostState.rotation
    // Must be called from game thread
    fun place(structure: StructureData) {
        val level = Minecraft.getInstance().level
        if (level == null) {
            StructraClient.LOGGER.error("WorldPlacer.place() called with null level")
            return
        }

        val offset = GhostState.offset
        val rotation = GhostState.rotation
        val paletteMap = buildPaletteMap(structure.palette.map { it.index to it.id }.toMap())
        val undoEntries = mutableListOf<UndoEntry>()

        for (block in structure.blocks) {
            val (rx, rz) = applyRotation(
                block.x, block.z,
                structure.meta.dimensions.x,
                structure.meta.dimensions.z,
                rotation
            )

            val worldPos = offset.offset(rx, block.y, rz)
            val newState = paletteMap[block.p] ?: continue

            // Store previous state for undo
            val previousState = level.getBlockState(worldPos)
            undoEntries.add(UndoEntry(worldPos, previousState))

            // VERIFY: setBlockState flags — 3 = update + notify neighbors
            level.setBlockAndUpdate(worldPos, newState)
        }

        undoStack.addLast(undoEntries)
        StructraClient.LOGGER.info(
            "WorldPlacer: placed ${undoEntries.size} blocks at $offset"
        )
    }

    // Undoes the last placement session
    fun undo() {
        val level = Minecraft.getInstance().level
        if (level == null) {
            StructraClient.LOGGER.error("WorldPlacer.undo() called with null level")
            return
        }

        if (undoStack.isEmpty()) {
            StructraClient.LOGGER.info("WorldPlacer: nothing to undo")
            return
        }

        val entries = undoStack.removeLast()
        for (entry in entries) {
            level.setBlockAndUpdate(entry.pos, entry.previousState)
        }

        StructraClient.LOGGER.info("WorldPlacer: undid ${entries.size} blocks")
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    private fun buildPaletteMap(palette: Map<Int, String>): Map<Int, BlockState> {
        return palette.mapValues { (_, id) ->
            val identifier = Identifier.tryParse(id)
            if (identifier != null && BuiltInRegistries.BLOCK.containsKey(identifier)) {
                BuiltInRegistries.BLOCK.getValue(identifier).defaultBlockState()
            } else {
                fallbackState()
            }
        }
    }

    private fun fallbackState(): BlockState =
        BuiltInRegistries.BLOCK.getValue(
            Identifier.parse(ModConstants.FALLBACK_BLOCK_ID)
        ).defaultBlockState()

    private fun applyRotation(x: Int, z: Int, sizeX: Int, sizeZ: Int, rotation: Int): Pair<Int, Int> {
        return when (rotation) {
            90  -> Pair(sizeZ - 1 - z, x)
            180 -> Pair(sizeX - 1 - x, sizeZ - 1 - z)
            270 -> Pair(z, sizeX - 1 - x)
            else -> Pair(x, z)
        }
    }
}