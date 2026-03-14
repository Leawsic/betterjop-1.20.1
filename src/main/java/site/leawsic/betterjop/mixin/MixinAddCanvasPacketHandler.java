package site.leawsic.betterjop.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.leawsic.betterjop.client.ProjectionManager;
import xerca.xercapaint.entity.EntityCanvas;
import xerca.xercapaint.packets.ClientboundAddCanvasPacket;
import xerca.xercapaint.packets.ClientboundAddCanvasPacketHandler;

@Mixin(ClientboundAddCanvasPacketHandler.class)
public abstract class MixinAddCanvasPacketHandler {
    @Inject(method = "processMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;putNonPlayerEntity(ILnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private static void afterPutEntity(ClientboundAddCanvasPacket msg, Minecraft client, CallbackInfo ci) {
        Entity entity = client.level.getEntity(msg.getId());
        if (entity instanceof EntityCanvas canvas) {
            String name = canvas.getCanvasName();
            BlockPos pos = canvas.getPos();
            ProjectionManager.onCanvasPlaced(name);
        }
    }
}
