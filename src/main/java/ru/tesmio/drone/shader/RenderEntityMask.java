package ru.tesmio.drone.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import java.lang.reflect.Method;
import java.util.List;


public class RenderEntityMask  {
    public static RenderTarget ENTITY_MASK;
    public static RenderTarget thermalMaskTarget;
    static Minecraft mc = Minecraft.getInstance();
    MultiBufferSource.BufferSource parent = mc.renderBuffers().bufferSource();

    public static final RenderStateShard.TransparencyStateShard TRANCPARENCY = new RenderStateShard.TransparencyStateShard("custom_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }
    );
    public static final RenderStateShard.OutputStateShard OUTPUT_IN_MASK = new RenderStateShard.OutputStateShard("output_in_mask", () -> {
        if (thermalMaskTarget != null) {
            thermalMaskTarget.bindWrite(false);
        }
    }, () -> {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    });

    static final RenderType ENTITY_MASK_TYPE = RenderType.create(
            "entity_mask_render",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                                     .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                                     .setTransparencyState(TRANCPARENCY)
                                     .setOutputState(OUTPUT_IN_MASK)
                                     .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false)) // пишем только цвет
                                     .createCompositeState(false)
    );

    public static void initRenderTargets() {
        if (mc.getMainRenderTarget() != null) {
            int width = mc.getMainRenderTarget().width;
            int height = mc.getMainRenderTarget().height;

            if (thermalMaskTarget == null || thermalMaskTarget.width != width || thermalMaskTarget.height != height) {
                if (thermalMaskTarget != null) {
                    thermalMaskTarget.destroyBuffers();
                }
                thermalMaskTarget = new SimpleRenderTarget(width, height, true, Minecraft.ON_OSX);
                thermalMaskTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            }

        }
    }
    static void renderEntityMask(RenderLevelStageEvent event) {
        if (thermalMaskTarget == null) return;

        RenderTarget currentTarget = mc.getMainRenderTarget();
        thermalMaskTarget.bindWrite(true);
        thermalMaskTarget.copyDepthFrom(currentTarget);

        thermalMaskTarget.clear(Minecraft.ON_OSX);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Quaternionf camRot = camera.rotation();
        Entity entityCamera = mc.getCameraEntity();
        PoseStack poseStack = new PoseStack();
        RenderSystem.setProjectionMatrix(RenderSystem.getProjectionMatrix(), VertexSorting.byDistance(camera.getPosition().toVector3f()));
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));


        EntityRenderDispatcher entityRenderer = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        if (mc.level != null) {

            double renderDistance = mc.options.getEffectiveRenderDistance() *16;
            AABB searchBox = new AABB(
                    cameraPos.subtract(renderDistance, renderDistance, renderDistance),
                    cameraPos.add(renderDistance, renderDistance, renderDistance)
            );

            List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, searchBox);

            for (LivingEntity entity : entities) {
                if (entity instanceof DroneEntity) continue;
                entityRenderer.setRenderShadow(false);
                renderEntityWithRenderer(entity, poseStack, bufferSource, entityRenderer, event.getPartialTick(), cameraPos);
                entityRenderer.setRenderShadow(true);
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
        currentTarget.bindWrite(true);

    }
    private static Entity skipEntities(Entity entity) {
        if (entity instanceof Monster && (!(entity instanceof Hoglin) || (entity instanceof Piglin))) {
            return entity;
        }
        return entity;
    }
    private static void renderEntityWithRenderer(LivingEntity entity, PoseStack poseStack,
            MultiBufferSource bufferSource, EntityRenderDispatcher entityRenderer,
            float partialTick, Vec3 cameraPos) {
        poseStack.pushPose();

        Vec3 entityPos = entity.getPosition(partialTick);


        double relativeX = entityPos.x - cameraPos.x;
        double relativeY = entityPos.y - cameraPos.y;
        double relativeZ = entityPos.z - cameraPos.z;

        MaskBufferSource maskBufferSource = new MaskBufferSource(bufferSource);

        entityRenderer.render(entity, relativeX, relativeY, relativeZ,
                entity.getYRot(), partialTick, poseStack, maskBufferSource,
                LevelRenderer.getLightColor(mc.level, entity.blockPosition()));


        poseStack.popPose();
    }


}