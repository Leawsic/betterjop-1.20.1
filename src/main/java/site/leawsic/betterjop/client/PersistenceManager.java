package site.leawsic.betterjop.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import site.leawsic.betterjop.BetterJoP;
import site.leawsic.betterjop.config.ModConfigManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PersistenceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PROJECTIONS_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            BetterJoP.MOD_ID + "/projections");

    private static String getServerIdentifier() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            String address = mc.getCurrentServer().ip;
            return address.replace(':', '_').replace('.', '_');
        } else if (mc.hasSingleplayerServer() && mc.isConnectedToRealms()) {
            return "realms";
        } else {
            return null;
        }
    }

    private static File getSaveFile() {
        String id = getServerIdentifier();
        if (id == null) return null;
        if (!PROJECTIONS_DIR.exists()) PROJECTIONS_DIR.mkdirs();
        return new File(PROJECTIONS_DIR, id + ".json");
    }

    // 保存所有投影信息
    public static void saveProjections(Map<String, ProjectionManager.CanvasInfo> canvasInfos) {
        if (!ModConfigManager.getConfig().persistenceEnabled) return;
        File file = getSaveFile();
        if (file == null) return;

        Map<String, SavedCanvasInfo> toSave = new HashMap<>();
        for (var entry : canvasInfos.entrySet()) {
            String name = entry.getKey();
            var info = entry.getValue();
            toSave.put(name, new SavedCanvasInfo(name, info.x(), info.y(), info.z(), info.facing(), info.rotation()));
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(toSave, writer);
            BetterJoP.LOGGER.info("[BetterJoP] Saved {} projections to {}", toSave.size(), file);
        } catch (IOException e) {
            BetterJoP.LOGGER.error("Failed to save projections", e);
        }
    }

    // 加载投影信息
    public static Map<String, ProjectionManager.CanvasInfo> loadProjections() {
        if (!ModConfigManager.getConfig().persistenceEnabled) return new HashMap<>();
        File file = getSaveFile();
        if (file == null || !file.exists()) return new HashMap<>();

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, SavedCanvasInfo>>() {}.getType();
            Map<String, SavedCanvasInfo> loaded = GSON.fromJson(reader, type);
            if (loaded == null) return new HashMap<>();

            Map<String, ProjectionManager.CanvasInfo> result = new HashMap<>();
            for (var entry : loaded.entrySet()) {
                SavedCanvasInfo saved = entry.getValue();
                Direction facing = Direction.valueOf(saved.facing);
                result.put(entry.getKey(), new ProjectionManager.CanvasInfo(saved.x, saved.y, saved.z, facing, saved.rotation));
            }
            BetterJoP.LOGGER.info("[BetterJoP] Loaded {} projections from {}", result.size(), file);
            return result;
        } catch (IOException e) {
            BetterJoP.LOGGER.error("Failed to load projections", e);
            return new HashMap<>();
        }
    }

    public static class SavedCanvasInfo {
        public String name;
        public double x, y, z;
        public String facing;
        public int rotation;

        public SavedCanvasInfo(String name, double x, double y, double z, Direction facing, int rotation) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing.name();
            this.rotation = rotation;
        }
    }
}
