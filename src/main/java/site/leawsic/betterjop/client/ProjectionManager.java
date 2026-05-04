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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectionManager {
    private static final Map<String, CanvasInfo> canvasInfos = new ConcurrentHashMap<>();
    private static final Map<String, CanvasProjection> activeProjections = new ConcurrentHashMap<>();

    public static void onCanvasRemoved(String canvasName, double x, double y, double z, Direction facing, int rotation) {
        canvasInfos.put(canvasName, new CanvasInfo(x, y, z, facing, rotation));
    }

    public static void onCanvasPlaced(String canvasName) {
        canvasInfos.remove(canvasName);
        CanvasProjection projection = activeProjections.remove(canvasName);
        if (projection != null) {
            projection.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        Set<String> namesInRange = getCanvasNamesInRange(player);

        Iterator<Map.Entry<String, CanvasInfo>> it = canvasInfos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CanvasInfo> entry = it.next();
            String name = entry.getKey();
            CanvasInfo info = entry.getValue();

            if (namesInRange.contains(name)) {
                CanvasProjection proj = activeProjections.get(name);
                if (proj == null || !proj.isAlive() || level.getEntity(proj.getId()) == null) {
                    ItemStack canvasStack = findCanvasByName(player, name);
                    if (canvasStack != null) {
                        CanvasType type = ((ItemCanvas) canvasStack.getItem()).getCanvasType();
                        int[] pixels = canvasStack.getTag().getIntArray("pixels");
                        int version = canvasStack.getTag().getInt("v");
                        CanvasProjection projection = new CanvasProjection(level,
                                info.x(), info.y(), info.z(),
                                info.facing(), info.rotation(),
                                name, type, pixels, version);
                        if (level instanceof ClientLevel clientLevel) {
                            clientLevel.putNonPlayerEntity(projection.getId(), projection);
                            activeProjections.put(name, projection);
                        }
                    } else {
                        it.remove();
                        BetterJoP.LOGGER.warn("[BetterJoP] Canvas item not found for recorded name: {}", name);
                    }
                }
            } else {
                CanvasProjection proj = activeProjections.remove(name);
                if (proj != null) {
                    proj.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    private static Set<String> getCanvasNamesInRange(Player player) {
        ModConfig.DetectionMode mode = ModConfigManager.getConfig().detectionMode;
        Set<String> names = ConcurrentHashMap.newKeySet();

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

    // === 新增管理方法 ===
    public static Map<String, CanvasInfo> getCanvasInfos() {
        return new ConcurrentHashMap<>(canvasInfos);
    }

    public static Set<String> getActiveProjectionNames() {
        return activeProjections.keySet();
    }

    /**
     * 返回当前活跃投影（正在显示）的 CanvasInfo，用于持久化保存。
     */
    public static Map<String, CanvasInfo> getActiveCanvasInfos() {
        Map<String, CanvasInfo> active = new ConcurrentHashMap<>();
        for (String name : activeProjections.keySet()) {
            CanvasInfo info = canvasInfos.get(name);
            if (info != null) {
                active.put(name, info);
            }
        }
        return active;
    }

    public static void removeProjection(String canvasName) {
        canvasInfos.remove(canvasName);
        CanvasProjection proj = activeProjections.remove(canvasName);
        if (proj != null) {
            proj.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    public static void clearAllProjections() {
        for (CanvasProjection proj : activeProjections.values()) {
            proj.remove(Entity.RemovalReason.DISCARDED);
        }
        canvasInfos.clear();
        activeProjections.clear();
    }

    public static void restoreProjections(Map<String, CanvasInfo> loaded) {
        canvasInfos.clear();
        canvasInfos.putAll(loaded);
        // 此处不重建 activeProjections 等待 tick 自然重建
    }

    public record CanvasInfo(double x, double y, double z, Direction facing, int rotation) {
    }
}