package ru.tesmio.drone.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import static ru.tesmio.drone.shader.ShaderRegistry.*;

@Mod.EventBusSubscriber(modid = Core.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderRenderer {
    static Minecraft mc = Minecraft.getInstance();
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (MONOCHROME == null) return;
        DroneEntity drone;
        if(mc.getCameraEntity() instanceof DroneEntity) {
            drone = (DroneEntity) mc.getCameraEntity();
            switch (drone.getVisionMode()) {
                case NORMAL -> {
                    return;
                }
                case MONOCHROME -> {
                    Minecraft mc = Minecraft.getInstance();
                    RenderTarget mainTarget = mc.getMainRenderTarget();
                    RenderSystem.setShader(() -> MONOCHROME);
                    MONOCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case THERMOCHROME -> {
                    Minecraft mc = Minecraft.getInstance();
                    RenderTarget mainTarget = mc.getMainRenderTarget();
                    RenderSystem.setShader(() -> THERMOCHROME);
                    THERMOCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case GREENCHROME -> {
                    Minecraft mc = Minecraft.getInstance();
                    RenderTarget mainTarget = mc.getMainRenderTarget();
                    RenderSystem.setShader(() -> GREENCHROME);
                    GREENCHROME.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
                    renderFullScreen();
                    RenderSystem.setShader(() -> null);
                }
                case THERMAL -> {
                    Minecraft mc = Minecraft.getInstance();
                    RenderTarget mainTarget = mc.getMainRenderTarget();
                    RenderSystem.setShader(() -> THERMAL);
                    THERMAL.setSampler("DiffuseSampler", mainTarget.getColorTextureId());
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
