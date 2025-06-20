package ru.tesmio.drone.drone.quadcopter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.server.*;
import ru.tesmio.drone.registry.InitItems;
import ru.tesmio.drone.shader.RenderEntityMask;

import static ru.tesmio.drone.drone.quadcopter.control.DroneController.*;

//TODO: надо добавить кнопки в меню игры
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {


    @SubscribeEvent
    public static void onCameraZoom(ViewportEvent.ComputeFov event) {
         Minecraft mc = Minecraft.getInstance();

        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;
        double baseFov = event.getFOV();
        double zoomFov = baseFov * drone.getZoom();
        event.setFOV(zoomFov);
    }

    @SubscribeEvent
    public static void setupViewport(ViewportEvent.ComputeCameraAngles event) {

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;

        float partialTicks = mc.getFrameTime();
        float interpolatedRoll = Mth.lerp(partialTicks, drone.prevRoll, drone.getDroneRoll());
        float smoothedRoll = interpolatedRoll * 0.8f;
        float interpolatedYaw = Mth.lerp(partialTicks, drone.prevYaw, drone.getDroneYaw());
        float interpolatedPitch = Mth.lerp(partialTicks, drone.prevPitch, drone.getDronePitch());

        event.setRoll(-smoothedRoll);
        if (mc.options.getCameraType().isFirstPerson()) {
            event.setYaw(interpolatedYaw);
            event.setPitch(interpolatedPitch);
        }
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        boolean guiOpen = mc.screen != null;
        DroneEntity drone;
        if(mc.getCameraEntity() instanceof DroneEntity) drone = (DroneEntity) mc.getCameraEntity();
        else return;
        if (event.phase != TickEvent.Phase.END) {

            RenderEntityMask.initRenderTargets();
            return;
        }
        freezePlayer(mc.player);
        if (!guiOpen) {
            if(!drone.validateUpdates(InitItems.FLY_CONTROLLER.get(), 4)) {
                mc.player.displayClientMessage(Component.translatable("warn.set_fly_controller"), true);
                stopControl();
                return;
            }

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() instanceof DroneEntity) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        useKey();
    }
//    @SubscribeEvent
//    public static void registerKeyBindings(final RegisterKeyMappingsEvent event) {
//        event.register(EXIT_CONTROL_KEY);
//        event.register(CTRL_KEY);
//        event.register(FLIGHT_MODE_KEY);
//        event.register(STAB_MODE_KEY);
//        event.register(VISION_MODE_KEY);
//        event.register(ZOOM_MODE_KEY);
//    }
}

