package ru.tesmio.drone.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.client.*;

import static ru.tesmio.drone.drone.control.DroneController.mouseGrabbed;
@OnlyIn(Dist.CLIENT)
public class PacketClientHandler {
    static Minecraft mc = net.minecraft.client.Minecraft.getInstance();
    static Level level = net.minecraft.client.Minecraft.getInstance().level;
    static Player player = Minecraft.getInstance().player;
    public static void handleDeathPacket(DroneDeathPacket msg) {

        if (!msg.enterControl) {
            if (player != null) mc.setCameraEntity(mc.player);
            if (mouseGrabbed) {
                mc.mouseHandler.releaseMouse();
                mouseGrabbed = false;
            }
        }
    }
    public static void handleDroneSyncViewPacket(DroneSyncViewPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity e = level.getEntity(msg.droneId);
        if (e instanceof DroneEntity drone) {
            drone.applyClientView(msg.yaw, msg.pitch, msg.roll);
        }
    }
    public static void handleDroneControllerPacket(DroneControllerPacket msg) {
        if (player == null) return;
        player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64))
              .stream()
              .filter(d -> d.getUUID().equals(msg.droneUUID))
              .findFirst()
              .ifPresent(d -> {
                  d.setControllerUUID(msg.controllerUUID);
              });
    }
    public static void  handleDistanceControlPacket(DistanceControlPacket msg) {

        if (level == null) return;

        Entity entity = level.getEntity(msg.droneId);
        if (entity instanceof DroneEntity drone) {
            drone.syncViewAndSimDistance(msg.viewChunks, msg.simChunks);
        }
    }
    public static void handleDroneZoomModePacket(DroneZoomModePacket msg) {
        if (mc.player != null && mc.level != null) {
            mc.player.displayClientMessage(Component.literal(msg.mode.name()), true);
        }
    }
    public static void handleDroneStabModePacket(DroneStabModePacket msg) {
        if (mc.player != null && mc.level != null) {
            mc.player.displayClientMessage(msg.mode.getDisplayMode(), true);
        }
    }
    public static void handleDroneFlightModePacket(DroneFlightModePacket msg) {
        if (mc.player != null && mc.level != null) {
            mc.player.displayClientMessage(msg.mode.getDisplayText(), true);
        }
    }
    public static void handleActionBarMessagePacket(ActionBarMessagePacket msg) {
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(Component.translatable(msg.message), true);
        }
    }
}
