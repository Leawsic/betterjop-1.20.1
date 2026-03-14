package site.leawsic.betterjop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import site.leawsic.betterjop.BetterJoP;
import site.leawsic.betterjop.config.ModConfig;
import site.leawsic.betterjop.config.ModConfigManager;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.item.ItemCanvas;

import java.util.*;

public class ProjectionManager {
    private static final Map<String, CanvasInfo> canvasInfos = new HashMap<>();
    private static final Map<String, CanvasProjection> activeProjections = new HashMap<>();

    /**
     * 当画布实体被破坏时调用，记录其名称和位置。
     */
    public static void onCanvasRemoved(String canvasName, double x, double y, double z, Direction facing, int rotation) {
        canvasInfos.put(canvasName, new CanvasInfo(x, y, z, facing, rotation));
    }

    /**
     * 当画布被放置（重新生成实体）时调用，移除记录和投影。
     */
    public static void onCanvasPlaced(String canvasName) {
        canvasInfos.remove(canvasName);
        CanvasProjection projection = activeProjections.remove(canvasName);
        if (projection != null) {
            projection.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * 客户端每 tick 调用，检查手中画布并管理投影。
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        // 获取当前检测范围内的所有画布名称
        Set<String> namesInRange = getCanvasNamesInRange(player);

        // 遍历所有已记录位置的画布
        Iterator<Map.Entry<String, CanvasInfo>> it = canvasInfos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CanvasInfo> entry = it.next();
            String name = entry.getKey();
            CanvasInfo info = entry.getValue();

            if (namesInRange.contains(name)) {
                // 画布在范围内，确保投影存在
                CanvasProjection proj = activeProjections.get(name);
                if (proj == null || !proj.isAlive() || level.getEntity(proj.getId()) == null) {
                    // 获取该画布对应的物品（用于像素数据）
                    ItemStack canvasStack = findCanvasByName(player, name);
                    if (canvasStack != null) {
                        CanvasType type = ((ItemCanvas) canvasStack.getItem()).getCanvasType();
                        int[] pixels = canvasStack.getTag().getIntArray("pixels");
                        int version = canvasStack.getTag().getInt("v");
                        CanvasProjection projection = new CanvasProjection(level,
                                info.x, info.y, info.z,
                                info.facing, info.rotation,
                                name, type, pixels, version);
                        if (level instanceof ClientLevel clientLevel) {
                            clientLevel.putNonPlayerEntity(projection.getId(), projection);
                            activeProjections.put(name, projection);
                            BetterJoP.LOGGER.info("[BetterJoP] Projection created for: {}", name);
                        }
                    } else {
                        // 理论上不应该发生，但若找不到物品则移除记录
                        BetterJoP.LOGGER.warn("[BetterJoP] Canvas item not found for recorded name: {}", name);
                        it.remove();
                    }
                }
            } else {
                // 画布不在范围内，移除投影
                CanvasProjection proj = activeProjections.remove(name);
                if (proj != null) {
                    proj.remove(Entity.RemovalReason.DISCARDED);
                    BetterJoP.LOGGER.info("[BetterJoP] Projection removed for: {}", name);
                }
                // 注意：不删除 canvasInfos 记录，以便下次再次持有画布时能恢复投影
            }
        }
    }

    private static Set<String> getCanvasNamesInRange(Player player) {
        ModConfig.DetectionMode mode = ModConfigManager.getConfig().detectionMode;
        Set<String> names = new HashSet<>();

        addCanvasName(player.getMainHandItem(), names);
        if (mode != ModConfig.DetectionMode.MAIN_HAND_ONLY) {
            addCanvasName(player.getOffhandItem(), names);
        }
        if (mode == ModConfig.DetectionMode.FULL_INVENTORY) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                addCanvasName(player.getInventory().getItem(i), names);
            }
        }
        return names;
    }

    private static void addCanvasName(ItemStack stack, Set<String> names) {
        if (stack.getItem() instanceof ItemCanvas && stack.hasTag()) {
            String name = stack.getTag().getString("name");
            names.add(name);
        }
    }

    public static void restoreProjections(Map<String, CanvasInfo> loaded) {
        canvasInfos.clear();
        canvasInfos.putAll(loaded);
    }

    public static Map<String, CanvasInfo> getCanvasInfos() {
        return new HashMap<>(canvasInfos);
    }

    private static ItemStack findCanvasByName(Player player, String name) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ItemCanvas && stack.hasTag()) {
                if (name.equals(stack.getTag().getString("name"))) {
                    return stack;
                }
            }
        }
        return null;
    }

    public record CanvasInfo(double x, double y, double z, Direction facing, int rotation) {
    }
}
