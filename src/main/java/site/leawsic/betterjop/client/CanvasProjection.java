package site.leawsic.betterjop.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import site.leawsic.betterjop.BetterJoP;
import site.leawsic.betterjop.BetterJoPClient;
import site.leawsic.betterjop.config.ModConfig;
import site.leawsic.betterjop.config.ModConfigManager;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.item.ItemCanvas;

import java.util.UUID;

public class CanvasProjection extends Entity {
    private String canvasName;
    private CanvasType canvasType;
    private int[] pixels;
    private int width, height;
    private int version;
    private Direction facing;
    private int rotation;

    private int tickCount = 0;

    private DynamicTexture texture;
    private ResourceLocation textureLocation;

    public CanvasProjection(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public CanvasProjection(Level level, double x, double y, double z, Direction facing, int rotation, String name, CanvasType type, int[] pixels, int version) {
        this(BetterJoPClient.CANVAS_PROJECTION_TYPE, level);
        this.setPos(x, y, z);
        this.facing = facing;
        this.rotation = rotation;
        this.canvasName = name;
        this.canvasType = type;
        this.width = CanvasType.getWidth(type);
        this.height = CanvasType.getHeight(type);
        this.version = version;
        this.pixels = pixels.clone();

        if (facing.getAxis().isHorizontal()) {
            this.setYRot(facing.get2DDataValue() * 90);
        } else {
            this.setXRot(-90 * facing.getAxisDirection().getStep());
        }

        createTexture();
    }

    @Override
    protected void defineSynchedData() {

    }

    private void createTexture() {
        texture = new DynamicTexture(width, height, true);
        textureLocation = new ResourceLocation(BetterJoP.MOD_ID, "projection_" + UUID.randomUUID().toString().replace("-", ""));
        fillTextureWhite();
        updateTexture();
        Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
    }

    private void fillTextureWhite() {
        NativeImage image = texture.getPixels();
        if (image != null) {
            int white = 0xFFFFFFFF;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setPixelRGBA(x, y, white);
                }
            }
            texture.upload();
        }
    }

    private void updateTexture() {
        if (pixels == null || pixels.length == 0) return;
        NativeImage image = texture.getPixels();
        if (image != null) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = x + y * width;
                    if (idx < pixels.length) {
                        int color = swapColor(pixels[idx]);
                        image.setPixelRGBA(x, y, color);
                    }
                }
            }
            texture.upload();
        }
    }

    private int swapColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        if (!isCanvasInHand(player, canvasName)) {
            tickCount++;
            int delay = ModConfigManager.getConfig().removalDelayInTicks;
            if (tickCount > delay) {
                this.remove(RemovalReason.DISCARDED);
            }
            return;
        }
        tickCount = 0;

        ItemStack canvasStack = getCanvasFromHandOrInventory(player, canvasName);
        if (canvasStack.hasTag()) {
            CompoundTag tag = canvasStack.getTag();
            int newVersion = tag.getInt("v");
            if (newVersion != version) {
                int[] newPixels = tag.getIntArray("pixels");
                if (newPixels.length == width * height) {
                    this.pixels = newPixels;
                    this.version = newVersion;
                    updateTexture();
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    private boolean isCanvasInHand(Player player, String name) {
        return getCanvasFromHandOrInventory(player, name) != null;
    }

    @Override
    public void remove(RemovalReason removalReason) {
        super.remove(removalReason);
        if (texture != null) {
            texture.close();
            Minecraft.getInstance().getTextureManager().release(textureLocation);
        }
    }

    private ItemStack getCanvasFromHandOrInventory(Player player, String name) {
        ModConfig.DetectionMode mode = ModConfigManager.getConfig().detectionMode;

        ItemStack main = player.getMainHandItem();
        if (isMatchingCanvas(main, name)) {
            return main;
        }

        if (mode!= ModConfig.DetectionMode.MAIN_HAND_ONLY){
            ItemStack off = player.getOffhandItem();
            if (isMatchingCanvas(off,name)) return off;
        }

        if (mode== ModConfig.DetectionMode.FULL_INVENTORY){
            for (int i=0;i<player.getInventory().getContainerSize();i++){
                ItemStack stack=player.getInventory().getItem(i);
                if (isMatchingCanvas(stack,name)) return stack;
            }
        }
        return null;
    }

    private boolean isMatchingCanvas(ItemStack stack, String name) {
        return stack.getItem() instanceof ItemCanvas && stack.hasTag() && name.equals(stack.getTag().getString("name"));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException("Client-only entity should not be synced");
    }

    public Direction getFacing() {
        return facing;
    }

    public int getRotation() {
        return rotation;
    }
}
