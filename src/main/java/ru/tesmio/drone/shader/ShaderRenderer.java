package ru.tesmio.drone.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import static ru.tesmio.drone.shader.RenderEntityMask.*;
import static ru.tesmio.drone.shader.ShaderRegistry.*;

@Mod.EventBusSubscriber(modid = Dronecraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderRenderer {
    static Minecraft mc = Minecraft.getInstance();



    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (MONOCHROME == null) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();


        DroneEntity drone;
        if(mc.getCameraEntity() instanceof DroneEntity) {
            drone = (DroneEntity) mc.getCameraEntity();
            switch (drone.getVisionMode()) {
                case NORMAL -> {
                    return;
                }
                case MONOCHROME -> {

                    RenderSystem.setShader(() -> MONOCHROME);
                    MONOCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case THERMOCHROME -> {

                    RenderSystem.setShader(() -> THERMOCHROME);
                    THERMOCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case GREENCHROME -> {

                    RenderSystem.setShader(() -> GREENCHROME);
                    GREENCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case THERMAL -> {

                    renderEntityMask(event);
                    RenderSystem.setShader(() -> THERMAL);
                    THERMAL.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    THERMAL.setSampler("EntityMask", RenderEntityMask.thermalMaskTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
            }

        }
    }

    public static void renderFullScreen() {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(-1, -1, 0).uv(0, 0).endVertex();
        buf.vertex( 1, -1, 0).uv(1, 0).endVertex();
        buf.vertex( 1,  1, 0).uv(1, 1).endVertex();
        buf.vertex(-1,  1, 0).uv(0, 1).endVertex();
        Tesselator.getInstance().end();
    }
}
