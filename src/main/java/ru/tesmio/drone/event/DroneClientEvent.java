package ru.tesmio.drone.event;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import ru.tesmio.drone.Core;

import ru.tesmio.drone.entity.DroneEntity;
import ru.tesmio.drone.entity.DroneModel;
import ru.tesmio.drone.entity.DroneRenderer;
import ru.tesmio.drone.packets.DroneMovePacket;
import ru.tesmio.drone.packets.DroneSpeedUpdatePacket;

import static ru.tesmio.drone.entity.DroneController.*;

//убрать пересадку - если в режиме полета дрона тыкнуть по другому дрону, пульт перепривяжется.
//иногда проскакивают ультразначения углов наклона и поворота, разобраться
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {


      @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() instanceof DroneEntity) {
            event.setCanceled(true);
        }
    }

    static Minecraft mc = Minecraft.getInstance();
    static boolean guiOpen = mc.screen != null;

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



}

