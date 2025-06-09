package ru.tesmio.drone.droneold;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.drone.DroneEntity;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Core.MODID, "textures/entity/drone.png");
    Minecraft mc = Minecraft.getInstance();
    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION)), 0.3f);
    }
    @Override
    public void render(DroneEntity drone, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(drone, entityYaw, partialTicks, poseStack, buffer, packedLight);

    }
    @Override
    protected void setupRotations(DroneEntity drone, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(drone, poseStack, ageInTicks, rotationYaw, partialTicks);

        Minecraft mc = Minecraft.getInstance();
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
    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }
}
