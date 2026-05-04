package site.leawsic.betterjop.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.io.File;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandManageProjection {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("projection")
                .then(literal("list")
                        .executes(ctx -> listProjections()))
                .then(literal("clear")
                        .then(argument("name", StringArgumentType.string())
                                .executes(ctx -> clearProjection(StringArgumentType.getString(ctx, "name"))))
                        .then(literal("all")
                                .executes(ctx -> clearAllProjections())))
                .then(literal("reload")
                        .executes(ctx -> reloadProjections()))
                .then(literal("debug")
                        .executes(ctx -> debugInfo()))
        );
    }

    private static int listProjections() {
        Map<String, ProjectionManager.CanvasInfo> active = ProjectionManager.getActiveCanvasInfos();
        if (active.isEmpty()) {
            sendMessage(Component.literal("没有活跃的投影。").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        sendMessage(Component.literal("当前活跃投影 (" + active.size() + " 个):").withStyle(ChatFormatting.GREEN));
        for (Map.Entry<String, ProjectionManager.CanvasInfo> entry : active.entrySet()) {
            String name = entry.getKey();
            ProjectionManager.CanvasInfo info = entry.getValue();
            String coordText = String.format("x=%.1f, y=%.1f, z=%.1f", info.x(), info.y(), info.z());

            // 使用 Style 构建带悬浮的组件
            Style style = Style.EMPTY
                    .withColor(ChatFormatting.GRAY)
                    .withUnderlined(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(coordText)));

            Component msg = Component.literal("  - " + name)
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(" [位置]").withStyle(style));
            sendMessage(msg);
        }
        return active.size();
    }

    private static int clearProjection(String name) {
        ProjectionManager.removeProjection(name);
        sendMessage(Component.literal("已清除投影: " + name).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int clearAllProjections() {
        ProjectionManager.clearAllProjections();
        sendMessage(Component.literal("已清除所有投影记录。").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int reloadProjections() {
        var loaded = PersistenceManager.loadProjections();
        ProjectionManager.restoreProjections(loaded);
        sendMessage(Component.literal("已重新加载投影持久化数据，共 " + loaded.size() + " 条记录。").withStyle(ChatFormatting.GREEN));
        return loaded.size();
    }

    private static int debugInfo() {
        sendMessage(Component.literal("===== BetterJoP 投影调试信息 =====").withStyle(ChatFormatting.GOLD));
        int canvasInfosCount = ProjectionManager.getCanvasInfos().size();
        int activeCount = ProjectionManager.getActiveProjectionNames().size();
        sendMessage(Component.literal("canvasInfos (所有记录位置) 数量: " + canvasInfosCount).withStyle(ChatFormatting.AQUA));
        sendMessage(Component.literal("activeProjections (当前显示) 数量: " + activeCount).withStyle(ChatFormatting.AQUA));

        File saveFile = PersistenceManager.getSaveFileForDebug();
        if (saveFile != null && saveFile.exists()) {
            long size = saveFile.length();
            sendMessage(Component.literal("持久化文件: " + saveFile.getAbsolutePath()).withStyle(ChatFormatting.GRAY));
            sendMessage(Component.literal("文件大小: " + size + " 字节").withStyle(ChatFormatting.GRAY));
        } else {
            sendMessage(Component.literal("持久化文件不存在或无法访问。").withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static void sendMessage(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }
}