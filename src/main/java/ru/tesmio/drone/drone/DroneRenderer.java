package ru.tesmio.drone.drone;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.tesmio.drone.Core;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Core.MODID, "textures/entity/drone.png");

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION)), 0.3f);
    }
    @Override
    public void render(DroneEntity drone, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        float yaw = drone.getDroneYaw();
//        float pitch = drone.getDronePitch();
//
//        // Применяем поворот модели дрона
//        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
//        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch * 0.3f)); // небольшой наклон по X для вида

       super.render(drone, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }
}
