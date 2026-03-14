package site.leawsic.betterjop.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.leawsic.betterjop.client.ProjectionManager;
import xerca.xercapaint.entity.EntityCanvas;

@Mixin(ClientPacketListener.class)
public class MixinClientGamePacketListener {
    @Inject(method = "handleRemoveEntities",at = @At("HEAD"))
    private void onHandleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci){
        ClientLevel level= Minecraft.getInstance().level;
        if (level==null) return;

        for (int id:packet.getEntityIds()){
            Entity entity=level.getEntity(id);
            if (entity instanceof EntityCanvas canvas){
                ProjectionManager.onCanvasRemoved(
                        canvas.getCanvasName(),
                        entity.getX(), entity.getY(), entity.getZ(),
                        canvas.getDirection(),canvas.getRotation()
                );
            }
        }
    }
}
