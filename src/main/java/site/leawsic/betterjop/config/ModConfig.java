package site.leawsic.betterjop.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "betterjop")
@Config.Gui.Background("minecraft:textures/block/birch_planks.png")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 60)
    public int removalDelayInTicks = 15;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DetectionMode detectionMode = DetectionMode.MAIN_HAND_AND_OFFHAND;

    @ConfigEntry.Gui.Tooltip
    public boolean persistenceEnabled = true;

    public enum DetectionMode {
        MAIN_HAND_ONLY,
        MAIN_HAND_AND_OFFHAND,
        FULL_INVENTORY
    }
}
