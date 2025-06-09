package ru.tesmio.drone.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.stringtemplate.v4.ST;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.drone.client.DroneHUD;
import ru.tesmio.drone.packets.server.DroneFlightModeServerPacket;
import ru.tesmio.drone.packets.server.DroneStabModeServerPacket;

import static ru.tesmio.drone.droneold.DroneController.*;

//убрать пересадку - если в режиме полета дрона тыкнуть по другому дрону, пульт перепривяжется.
//иногда проскакивают ультразначения углов наклона и поворота, разобраться
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {
    static Minecraft mc = Minecraft.getInstance();
    static boolean guiOpen = mc.screen != null;
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;
        if(drone.getStabMode() == DroneEntity.StabMode.FPV) {
            float partialTicks = mc.getFrameTime();
            float interpolatedRoll = Mth.lerp(partialTicks, drone.prevRoll, drone.getDroneRoll());
            float smoothedRoll = interpolatedRoll * 0.8f;
            event.setRoll(-smoothedRoll);
        }
    }
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (mc.getCameraEntity() instanceof DroneEntity) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (FLIGHT_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                DroneFlightModeServerPacket.sendToServer(drone.getUUID());
            }
        }
        if (STAB_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                DroneStabModeServerPacket.sendToServer(drone.getUUID());
            }
        }
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        DroneEntity drone;

        if(mc.getCameraEntity() instanceof DroneEntity) drone = (DroneEntity) mc.getCameraEntity();
        else return;
        if (event.phase != TickEvent.Phase.END) return;
        stopPlayer(mc.player);
        mouseGrabber(guiOpen);
        if (!guiOpen) {
            moveDrone(drone);
        }
        exitControl();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        DroneHUD.renderDroneHud(event.getGuiGraphics());
    }
}

