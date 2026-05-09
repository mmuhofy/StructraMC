package com.muhofy.structra.world

import com.muhofy.structra.api.StructureData
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos

// CLIENT-SIDE ONLY
// Holds the current ghost preview state.
// Ghost is never written to world until confirmed via WorldPlacer.
@Environment(EnvType.CLIENT)
object GhostState {

    var active: Boolean = false
    var structure: StructureData? = null
    var offset: BlockPos = BlockPos.ZERO
    var rotation: Int = 0  // 0, 90, 180, 270 degrees

    fun activate(data: StructureData, origin: BlockPos) {
        structure = data
        offset = origin
        rotation = 0
        active = true
    }

    fun deactivate() {
        active = false
        structure = null
        offset = BlockPos.ZERO
        rotation = 0
    }

    fun moveBy(dx: Int, dy: Int, dz: Int) {
        offset = offset.offset(dx, dy, dz)
    }

    fun rotate90() {
        rotation = (rotation + 90) % 360
    }
}