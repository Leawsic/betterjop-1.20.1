package site.leawsic.betterjop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import site.leawsic.betterjop.client.CanvasProjection;
import site.leawsic.betterjop.client.PersistenceManager;
import site.leawsic.betterjop.client.ProjectionManager;
import site.leawsic.betterjop.client.RenderCanvasProjection;
import site.leawsic.betterjop.config.ModConfigManager;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class BetterJoPClient implements ClientModInitializer {
    public static EntityType<CanvasProjection> CANVAS_PROJECTION_TYPE;

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        CANVAS_PROJECTION_TYPE = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation(BetterJoP.MOD_ID, "canvas_projection"),
                FabricEntityTypeBuilder.<CanvasProjection>create(MobCategory.MISC, CanvasProjection::new)
                        .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                        .trackRangeChunks(4)          // 追踪范围（可选）
                        .trackedUpdateRate(20)        // 更新间隔（tick）
                        .disableSaving()               // 不保存到世界
                        .disableSummon()               // 禁止 /summon
                        .build()
        );
        EntityRendererRegistry.register(CANVAS_PROJECTION_TYPE, RenderCanvasProjection::new);

        // 每客户端 Tick 更新投影管理器
        ClientTickEvents.END_CLIENT_TICK.register(client -> ProjectionManager.tick());

        //注册config
        ModConfigManager.init();

        //注册事件
        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
            Map<String,ProjectionManager.CanvasInfo> loaded= PersistenceManager.loadProjections();
            ProjectionManager.restoreProjections(loaded);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PersistenceManager.saveProjections(ProjectionManager.getCanvasInfos()));
    }
}
