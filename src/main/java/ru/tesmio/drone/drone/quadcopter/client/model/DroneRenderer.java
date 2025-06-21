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

        if (!drone.isInWater()) {
            float angle = (drone.rotorAngle + drone.angularVelocity * partialTicks) % 360f;
            float angleRad = (float) Math.toRadians(angle);

            this.model.prop1.yRot = angleRad;
            this.model.prop2.yRot = -angleRad;
            this.model.prop3.yRot = -angleRad;
            this.model.prop4.yRot = angleRad;

            this.model.drone.xRot = drone.bodyXRot;
            this.model.drone.zRot = drone.bodyZRot;
        }
        super.render(drone, entityYaw, partialTicks, poseStack, buffer, packedLight);

    }

    @Override
    protected void setupRotations(DroneEntity drone, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(drone, poseStack, ageInTicks, rotationYaw, partialTicks);
            float lerpFactor = 0.2f;
            float tiltX = Mth.lerp(lerpFactor, drone.prevTiltX, drone.getTiltX());
            float tiltZ = Mth.lerp(lerpFactor, drone.prevTiltZ, drone.getTiltZ());
            drone.prevTiltX = tiltX;
            drone.prevTiltZ = tiltZ;
            poseStack.mulPose(Axis.ZP.rotation(-tiltZ));
            poseStack.mulPose(Axis.XP.rotation(tiltX));
    }
    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }
}
