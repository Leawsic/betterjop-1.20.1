package site.leawsic.betterjop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderCanvasProjection extends EntityRenderer<CanvasProjection> {
    private static final ResourceLocation BACK_LOCATION = new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, "textures/block/birch_planks.png");

    public RenderCanvasProjection(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(CanvasProjection entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        ResourceLocation location = entity.getTextureLocation();
        int width = entity.getWidth();
        int height = entity.getHeight();
        Direction facing = entity.getFacing();
        int rotation = entity.getRotation();

        poseStack.pushPose();

        float wScale = width / 16.0f;
        float hScale = height / 16.0f;

        // 根据朝向和旋转调整（仿照原渲染器）
        float xOffset = facing.getStepX();
        float yOffset = facing.getStepY();
        float zOffset = facing.getStepZ();

        // 应用画布自身的旋转（rotation）
        if (rotation > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.f - entity.getYRot()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.f * rotation));
            poseStack.mulPose(Axis.YP.rotationDegrees(-180.f + entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        }

        // 将模型原点移动到画板左下角（原渲染中的平移）
        if (facing.getAxis().isHorizontal()) {
            poseStack.translate(zOffset * 0.5d * wScale, -0.5d * hScale, -xOffset * 0.5d * wScale);
        } else {
            poseStack.translate(0.5 * wScale, 0, (yOffset > 0 ? 0.5 : -0.5) * wScale);
        }

        // 旋转到朝向
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - entity.getYRot()));

        float f = 1.0f / 32.0f;
        poseStack.scale(f, f, f);

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // 绘制前面（画布内容）
        VertexConsumer frontConsumer = buffer.getBuffer(RenderType.entityTranslucent(location));
        addVertex(frontConsumer, pose, normal, 0, 32 * hScale, -1, 1, 0, packedLight, 0, 0, -1);
        addVertex(frontConsumer, pose, normal, 32 * wScale, 32 * hScale, -1, 0, 0, packedLight, 0, 0, -1);
        addVertex(frontConsumer, pose, normal, 32 * wScale, 0, -1, 0, 1, packedLight, 0, 0, -1);
        addVertex(frontConsumer, pose, normal, 0, 0, -1, 1, 1, packedLight, 0, 0, -1);

        // 绘制背面和侧面（使用木质纹理）
        VertexConsumer backConsumer = buffer.getBuffer(RenderType.entitySolid(BACK_LOCATION));
        final float sideWidth = 1.0f / 16.0f;
        // 背面
        addVertex(backConsumer, pose, normal, 0, 0, 1, 0, 0, packedLight, 0, 0, 1);
        addVertex(backConsumer, pose, normal, 32 * wScale, 0, 1, 1, 0, packedLight, 0, 0, 1);
        addVertex(backConsumer, pose, normal, 32 * wScale, 32 * hScale, 1, 1, 1, packedLight, 0, 0, 1);
        addVertex(backConsumer, pose, normal, 0, 32 * hScale, 1, 0, 1, packedLight, 0, 0, 1);
        // 左侧
        addVertex(backConsumer, pose, normal, 0, 0, 1, sideWidth, 0, packedLight, -1, 0, 0);
        addVertex(backConsumer, pose, normal, 0, 32 * hScale, 1, sideWidth, 1, packedLight, -1, 0, 0);
        addVertex(backConsumer, pose, normal, 0, 32 * hScale, -1, 0, 1, packedLight, -1, 0, 0);
        addVertex(backConsumer, pose, normal, 0, 0, -1, 0, 0, packedLight, -1, 0, 0);
        // 右侧
        addVertex(backConsumer, pose, normal, 32 * wScale, 0, -1, 0, 0, packedLight, 1, 0, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 32 * hScale, -1, 0, 1, packedLight, 1, 0, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 32 * hScale, 1, sideWidth, 1, packedLight, 1, 0, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 0, 1, sideWidth, 0, packedLight, 1, 0, 0);
        // 顶部
        addVertex(backConsumer, pose, normal, 0, 32 * hScale, 1, 0, 0, packedLight, 0, 1, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 32 * hScale, 1, 1, 0, packedLight, 0, 1, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 32 * hScale, -1, 1, sideWidth, packedLight, 0, 1, 0);
        addVertex(backConsumer, pose, normal, 0, 32 * hScale, -1, 0, sideWidth, packedLight, 0, 1, 0);
        // 底部
        addVertex(backConsumer, pose, normal, 0, 0, -1, 0, 1, packedLight, 0, -1, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 0, -1, 1, 1, packedLight, 0, -1, 0);
        addVertex(backConsumer, pose, normal, 32 * wScale, 0, 1, 1, 1 - sideWidth, packedLight, 0, -1, 0);
        addVertex(backConsumer, pose, normal, 0, 0, 1, 0, 1 - sideWidth, packedLight, 0, -1, 0);

        poseStack.popPose();
    }

    private void addVertex(VertexConsumer vb, Matrix4f m, Matrix3f mn,
                           float x, float y, float z, float u, float v, int light,
                           float nx, float ny, float nz) {
        vb.vertex(m, x, y, z).color(255, 255, 255, 200) // 半透明
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(mn, nx, ny, nz).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CanvasProjection entity) {
        return entity.getTextureLocation();
    }
}
