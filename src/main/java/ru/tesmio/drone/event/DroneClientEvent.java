package ru.tesmio.drone.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.drone.DroneHUD;
import ru.tesmio.drone.packets.server.DroneFlightModeServerPacket;

import static ru.tesmio.drone.drone.DroneController.*;

//убрать пересадку - если в режиме полета дрона тыкнуть по другому дрону, пульт перепривяжется.
//иногда проскакивают ультразначения углов наклона и поворота, разобраться
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {
    static Minecraft mc = Minecraft.getInstance();
    static boolean guiOpen = mc.screen != null;

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
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        DroneEntity drone;

        if(mc.getCameraEntity() instanceof DroneEntity) drone = (DroneEntity) mc.getCameraEntity();
        else return;

        if (event.phase != TickEvent.Phase.END) return;
        stopPlayer(mc.player);
        mouseGrabber(guiOpen);
        moveDrone(drone);
        turnDrone(guiOpen, drone);
        exitControl();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        DroneHUD.render(event.getGuiGraphics(), event.getPartialTick());
    }
}

