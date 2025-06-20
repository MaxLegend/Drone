package ru.tesmio.drone.drone.quadcopter.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Dronecraft.MODID, "textures/entity/drone.png");
    Minecraft mc = Minecraft.getInstance();
    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION)), 0.3f);
    }
    @Override
    public void render(DroneEntity drone, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        float interpolatedYaw = Mth.lerp(partialTicks, drone.prevYaw, drone.getDroneYaw());
        if (drone.isLinked() && !drone.isInWater()) {

            // ageInTicks — можно взять как totalTicks + partialTicks
            float ageInTicks = drone.tickCount + partialTicks;

            // Вращение пропеллеров
            this.model.prop1.yRot = ageInTicks * 2f;
            this.model.prop2.yRot = ageInTicks * -2f;
            this.model.prop3.yRot = ageInTicks * -2f;
            this.model.prop4.yRot = ageInTicks * 2f;

            // «Плавание» дрона
            float hoverAmplitude = 0.02f;
            float hoverSpeed = 0.1f;
            float hoverX = Mth.sin(ageInTicks * hoverSpeed) * hoverAmplitude;
            float hoverZ = Mth.cos(ageInTicks * hoverSpeed) * hoverAmplitude;
            float lerpFactor = 0.02f;

            // Интерполируем к нужным углам наклона
            this.model.drone.xRot = Mth.lerp(lerpFactor, this.model.drone.xRot, hoverX);
            this.model.drone.zRot = Mth.lerp(lerpFactor, this.model.drone.zRot, hoverZ);
        } else {
            // Если пульт не привязан — сбрасываем повороты, чтобы модель не «висела» в воздухе
            this.model.prop1.yRot = 0;
            this.model.prop2.yRot = 0;
            this.model.prop3.yRot = 0;
            this.model.prop4.yRot = 0;
            this.model.drone.xRot = 0;
            this.model.drone.zRot = 0;
        }
        super.render(drone, entityYaw, partialTicks, poseStack, buffer, packedLight);

    }

    @Override
    protected void setupRotations(DroneEntity drone, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {


        super.setupRotations(drone, poseStack, ageInTicks, rotationYaw, partialTicks);

        Minecraft mc = Minecraft.getInstance();
        if (Minecraft.getInstance().getCameraEntity() == drone) {

        if (mc.getCameraEntity() == drone) {
            float maxTilt = 0.38f;

            float forwardInput = 0;
            float sidewaysInput = 0;

            if (mc.options.keyUp.isDown()) forwardInput -= 1;
            if (mc.options.keyDown.isDown()) forwardInput += 1;
            if (mc.options.keyLeft.isDown()) sidewaysInput += 1;
            if (mc.options.keyRight.isDown()) sidewaysInput -= 1;

            float length = Mth.sqrt(forwardInput * forwardInput + sidewaysInput * sidewaysInput);
            if (length > 0) {
                forwardInput /= length;
                sidewaysInput /= length;
            }

            float combinedTilt = maxTilt * length;
            sidewaysInput = -sidewaysInput;

            float targetTiltX = forwardInput * combinedTilt;
            float targetTiltZ = sidewaysInput * combinedTilt;

            float lerpFactor = 0.2f;
            float tiltX = Mth.lerp(lerpFactor, drone.prevTiltX, targetTiltX);
            float tiltZ = Mth.lerp(lerpFactor, drone.prevTiltZ, targetTiltZ);

            drone.prevTiltX = tiltX;
            drone.prevTiltZ = tiltZ;

            // Сначала наклон по Z (влево-вправо), потом по X (вперёд-назад)
            poseStack.mulPose(Axis.ZP.rotation(-tiltZ));
            poseStack.mulPose(Axis.XP.rotation(tiltX));
        }
        }

    }
    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }
}
