package ru.tesmio.drone.drone.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.opengl.GL11;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DroneDeathPacket;
import ru.tesmio.drone.packets.server.*;

import static ru.tesmio.drone.drone.control.DroneController.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {
    static Minecraft mc = Minecraft.getInstance();
    static boolean guiOpen = mc.screen != null;

    @SubscribeEvent
    public static void onCameraZoom(ViewportEvent.ComputeFov event) {
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;
        double baseFov = event.getFOV();
        double zoomFov = baseFov * drone.getZoom();
        event.setFOV(zoomFov);
    }
    @SubscribeEvent
    public static void setupViewport(ViewportEvent.ComputeCameraAngles event) {
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) { return; }
            float partialTicks = mc.getFrameTime();
            float interpolatedYaw = Mth.lerp(partialTicks, drone.prevYaw, drone.getDroneYaw());
            float interpolatedPitch = Mth.lerp(partialTicks, drone.prevPitch, drone.getDronePitch());
            float interpolatedRoll = Mth.lerp(partialTicks, drone.prevRoll, drone.getDroneRoll());
            float smoothedRoll = interpolatedRoll * 0.8f;
        if (mc.options.getCameraType().isFirstPerson()) {
            event.setRoll(-smoothedRoll);
            event.setYaw(interpolatedYaw);
            event.setPitch(interpolatedPitch);
        } else {
            drone.setDronePitch(interpolatedPitch);
            drone.setDroneYaw(interpolatedYaw);
            event.setRoll(-smoothedRoll);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        DroneEntity drone;
        if(mc.getCameraEntity() instanceof DroneEntity) drone = (DroneEntity) mc.getCameraEntity();
        else return;
        if (event.phase != TickEvent.Phase.END) return;
        freezePlayer(mc.player);
        if (!guiOpen) {
            moveDrone(drone);
            turnDrone(drone);
            PacketSystem.CHANNEL.sendToServer(new DroneViewPacket(drone.getUUID(), drone.getDroneYaw(), drone.getDronePitch(), drone.getDroneRoll()));
        }
        stopControl();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        DroneHUD.renderDroneHud(event.getGuiGraphics());
    }
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (mc.getCameraEntity() instanceof DroneEntity) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        useKey();
    }
}

