package com.muhofy.structra.mixin

import com.muhofy.structra.world.GhostRenderer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.GameRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

// CLIENT-SIDE ONLY
// Cleans up GhostRenderer GPU resources when GameRenderer closes
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer::class)
class GameRendererMixin {

    @Inject(method = ["close"], at = [At("RETURN")])
    private fun onClose(ci: CallbackInfo) {
        GhostRenderer.close()
    }
}