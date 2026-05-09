package com.muhofy.structra.world

import com.mojang.blaze3d.vertex.PoseStack
import com.muhofy.structra.StructraClient
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState

// CLIENT-SIDE ONLY
// Renders ghost block preview using WorldRenderEvents.AFTER_TRANSLUCENT (FAPI)
// Ghost blocks rendered as semi-transparent blue overlay
// UNTESTED — verify rendering pipeline before use
@Environment(EnvType.CLIENT)
object GhostRenderer {

    fun register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            if (GhostState.active) {
                renderGhost(context)
            }
        }
        StructraClient.LOGGER.info("GhostRenderer registered.")
    }

    // UNTESTED — verify block rendering approach for 1.21.11 before use
    private fun renderGhost(context: WorldRenderContext) {
        val structure = GhostState.structure ?: return
        val matrixStack = context.matrixStack() ?: return
        val camera = context.camera()
        val bufferSource = context.vertexConsumers() ?: return

        val cameraPos = camera.position
        val offsetPos = GhostState.offset

        matrixStack.pushPose()

        // Translate to world position relative to camera
        matrixStack.translate(
            offsetPos.x - cameraPos.x,
            offsetPos.y - cameraPos.y,
            offsetPos.z - cameraPos.z
        )

        // Build palette map: index → BlockState
        val paletteMap = buildPaletteMap(structure.palette.map { it.index to it.id }.toMap())

        for (block in structure.blocks) {
            val rotated = applyRotation(block.x, block.z, structure.meta.dimensions.x, structure.meta.dimensions.z, GhostState.rotation)
            val blockState = paletteMap[block.p] ?: continue

            renderGhostBlock(
                matrixStack,
                bufferSource,
                blockState,
                rotated.first,
                block.y,
                rotated.second
            )
        }

        matrixStack.popPose()
    }

    // UNTESTED — verify BlockRenderDispatcher usage for 1.21.11 Mojang Mappings
    private fun renderGhostBlock(
        matrixStack: PoseStack,
        bufferSource: MultiBufferSource,
        blockState: BlockState,
        x: Int, y: Int, z: Int
    ) {
        val dispatcher = Minecraft.getInstance().blockRenderer
        matrixStack.pushPose()
        matrixStack.translate(x.toDouble(), y.toDouble(), z.toDouble())

        // Render with translucent overlay tint
        // VERIFY: RenderType.translucent() correct for ghost overlay in 1.21.11
        dispatcher.renderSingleBlock(
            blockState,
            matrixStack,
            bufferSource,
            ModConstants.COLOR_GHOST_TINT,
            0xF000F0  // overlay: full brightness
        )

        matrixStack.popPose()
    }

    private fun buildPaletteMap(palette: Map<Int, String>): Map<Int, BlockState> {
        return palette.mapValues { (_, id) ->
            val identifier = Identifier.tryParse(id)
            if (identifier != null) {
                BuiltInRegistries.BLOCK.get(identifier)?.defaultBlockState()
                    ?: BuiltInRegistries.BLOCK.get(
                        Identifier.parse(ModConstants.FALLBACK_BLOCK_ID)
                    )!!.defaultBlockState()
            } else {
                BuiltInRegistries.BLOCK.get(
                    Identifier.parse(ModConstants.FALLBACK_BLOCK_ID)
                )!!.defaultBlockState()
            }
        }
    }

    // Rotates x,z around the structure center for 0/90/180/270 degrees
    private fun applyRotation(x: Int, z: Int, sizeX: Int, sizeZ: Int, rotation: Int): Pair<Int, Int> {
        return when (rotation) {
            90  -> Pair(sizeZ - 1 - z, x)
            180 -> Pair(sizeX - 1 - x, sizeZ - 1 - z)
            270 -> Pair(z, sizeX - 1 - x)
            else -> Pair(x, z)
        }
    }
}