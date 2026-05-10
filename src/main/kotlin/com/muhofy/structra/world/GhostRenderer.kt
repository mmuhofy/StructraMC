package com.muhofy.structra.world

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import com.muhofy.structra.StructraClient
import com.muhofy.structra.util.ModConstants
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MappableRingBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import java.util.OptionalDouble
import java.util.OptionalInt

// CLIENT-SIDE ONLY
// Renders ghost block preview using WorldRenderEvents.BEFORE_TRANSLUCENT (FAPI 1.21.11)
// Uses new extraction/drawing pipeline — confirmed from Fabric Docs 1.21.11
// UNTESTED — verify rendering visuals before use
@Environment(EnvType.CLIENT)
object GhostRenderer {

    // Custom render pipeline — translucent ghost blocks with depth test disabled
    // so ghost is always visible through terrain
    private val GHOST_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "pipeline/ghost_blocks"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    private val allocator = ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE)
    private var buffer: BufferBuilder? = null

    private val COLOR_MODULATOR = Vector4f(1f, 1f, 1f, 1f)
    private val MODEL_OFFSET = Vector3f()
    private val TEXTURE_MATRIX = Matrix4f()
    private var vertexBuffer: MappableRingBuffer? = null

    fun register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register { context ->
            if (GhostState.active) {
                extractGhost(context)
                drawGhost(Minecraft.getInstance())
            }
        }
        StructraClient.LOGGER.info("GhostRenderer registered.")
    }

    // --- Extraction phase ---
    // UNTESTED — verify ghost block positions and tint before use
    private fun extractGhost(context: WorldRenderContext) {
        val structure = GhostState.structure ?: return
        val matrices: PoseStack = context.matrices()
        val camera: Vec3 = context.worldState().cameraRenderState.pos
        val offset = GhostState.offset

        matrices.pushPose()
        matrices.translate(
            offset.x - camera.x,
            offset.y - camera.y,
            offset.z - camera.z
        )

        if (buffer == null) {
            buffer = BufferBuilder(allocator, GHOST_PIPELINE.vertexFormatMode, GHOST_PIPELINE.vertexFormat)
        }

        val paletteMap = structure.palette.associate { it.index to it.id }
        val sizeX = structure.meta.dimensions.x
        val sizeZ = structure.meta.dimensions.z

        for (block in structure.blocks) {
            val (rx, rz) = applyRotation(block.x, block.z, sizeX, sizeZ, GhostState.rotation)
            val blockId = paletteMap[block.p] ?: ModConstants.FALLBACK_BLOCK_ID

            // Validate block exists — fallback if not
            val identifier = Identifier.tryParse(blockId)
                ?: Identifier.parse(ModConstants.FALLBACK_BLOCK_ID)

            if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
                renderGhostBlock(
                    matrices.last().pose(),
                    buffer!!,
                    rx.toFloat(), block.y.toFloat(), rz.toFloat(),
                    rx + 1f, block.y + 1f, rz + 1f,
                    0f, 0.4f, 1f, 0.35f   // blue tint, semi-transparent
                )
            }
        }

        matrices.popPose()
    }

    // Renders a filled box (one block) into the buffer
    private fun renderGhostBlock(
        pose: Matrix4fc, buf: BufferBuilder,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        // Front
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
        // Back
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
        // Left
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
        // Right
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
        // Top
        buf.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
        // Bottom
        buf.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
        buf.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
        buf.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
    }

    // --- Drawing phase ---
    private fun drawGhost(client: Minecraft) {
        val buf = buffer ?: return
        val builtBuffer: MeshData = buf.buildOrThrow()
        val drawParams = builtBuffer.drawState()
        val format = drawParams.format()

        val vertices = upload(drawParams, format, builtBuffer)
        draw(client, builtBuffer, drawParams, vertices, format)

        vertexBuffer?.rotate()
        buffer = null
    }

    private fun upload(drawParams: MeshData.DrawState, format: VertexFormat, builtBuffer: MeshData): GpuBuffer {
        val size = drawParams.vertexCount() * format.vertexSize

        if (vertexBuffer == null || vertexBuffer!!.size() < size) {
            vertexBuffer?.close()
                            vertexBuffer = MappableRingBuffer(
                { "${ModConstants.MOD_ID} ghost render pipeline" },
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE,
                size
            )
        }

        val encoder: CommandEncoder = RenderSystem.getDevice().createCommandEncoder()
        val vertexBuf = builtBuffer.vertexBuffer()!!
        encoder.mapBuffer(
            vertexBuffer!!.currentBuffer().slice(0, vertexBuf.remaining().toLong()),
            false, true
        ).use { mapped ->
            MemoryUtil.memCopy(vertexBuf, mapped.data())
        }

        return vertexBuffer!!.currentBuffer()
    }

    private fun draw(client: Minecraft, builtBuffer: MeshData, drawParams: MeshData.DrawState, vertices: GpuBuffer, format: VertexFormat) {
        val indices: GpuBuffer
        val indexType: VertexFormat.IndexType

        if (GHOST_PIPELINE.vertexFormatMode == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting())
            indices = GHOST_PIPELINE.vertexFormat.uploadImmediateIndexBuffer(builtBuffer.indexBuffer())
            indexType = builtBuffer.drawState().indexType()
        } else {
            val shapeBuffer = RenderSystem.getSequentialBuffer(GHOST_PIPELINE.vertexFormatMode)
            indices = shapeBuffer.getBuffer(drawParams.indexCount())
            indexType = shapeBuffer.type()
        }

        val dynamicTransforms: GpuBufferSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX)

        RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(
                { "${ModConstants.MOD_ID} ghost rendering" },
                client.mainRenderTarget.colorTextureView!!,
                OptionalInt.empty(),
                client.mainRenderTarget.depthTextureView!!,
                OptionalDouble.empty()
            ).use { pass ->
                pass.setPipeline(GHOST_PIPELINE)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("DynamicTransforms", dynamicTransforms)
                pass.setVertexBuffer(0, vertices)
                pass.setIndexBuffer(indices, indexType)
                pass.drawIndexed(0 / format.vertexSize, 0, drawParams.indexCount(), 1)
            }

        builtBuffer.close()
    }

    // Cleanup — inject into GameRenderer#close via mixin
    fun close() {
        allocator.close()
        vertexBuffer?.close()
        vertexBuffer = null
    }

    private fun applyRotation(x: Int, z: Int, sizeX: Int, sizeZ: Int, rotation: Int): Pair<Int, Int> {
        return when (rotation) {
            90  -> Pair(sizeZ - 1 - z, x)
            180 -> Pair(sizeX - 1 - x, sizeZ - 1 - z)
            270 -> Pair(z, sizeX - 1 - x)
            else -> Pair(x, z)
        }
    }
}