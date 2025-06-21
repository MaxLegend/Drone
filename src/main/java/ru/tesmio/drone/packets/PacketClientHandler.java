package ru.tesmio.drone.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.client.*;

import java.util.ArrayList;
import java.util.List;

import static ru.tesmio.drone.drone.quadcopter.control.DroneController.mouseGrabbed;
@OnlyIn(Dist.CLIENT)
public class PacketClientHandler {
    private static final List<DroneUpdateTiltsClient> pendingTilts = new ArrayList<>();
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
    public static void handleDroneInventorySyncPacket(DroneInventorySyncPacket msg) {
        DroneEntity drone = player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64)).stream()
                                  .filter(d -> msg.droneUUID.equals(d.getUUID()))
                                  .findFirst().orElse(null);
        if (drone != null) {
            drone.readNBT(msg.inventoryTag);
        }
    }
    public static void handleDroneUpdateTiltsClient(DroneUpdateTiltsClient msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Entity e = level.getEntity(msg.droneId);
        if (e instanceof DroneEntity drone) {

            drone.setTiltX(msg.tiltX);
            drone.setTiltZ(msg.tiltZ);
        }else {
            // Сохраняем в очередь, сущности ещё нет
            pendingTilts.add(msg);
        }
    }
    public static void handleAnimationClientPacket(AnimationClientPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Entity e = level.getEntity(msg.droneId);
        if (e instanceof DroneEntity drone) {

            drone.angularVelocity = msg.angularVelocity;

            drone.bodyXRot = msg.bodyXRot;
            drone.bodyZRot = msg.bodyZRot;
        }
    }
    public static void processPendingTilts() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        pendingTilts.removeIf(msg -> {
            Entity e = level.getEntity(msg.droneId);
            if (e instanceof DroneEntity drone) {
                applyTilt(drone, msg);
                return true; // успешно применено → удалить из очереди
            }
            return false; // оставить для следующего тика
        });
    }
    private static void applyTilt(DroneEntity drone, DroneUpdateTiltsClient msg) {
        drone.setTiltX(msg.tiltX);
        drone.setTiltZ(msg.tiltZ);
    }
    public static void handleDroneSyncViewPacket(DroneSyncViewPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity e = level.getEntity(msg.droneId);
        if (e instanceof DroneEntity drone) {
            drone.getViewXRot(mc.getPartialTick());
            drone.getViewYRot(mc.getPartialTick());
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
    public static void handleDroneModesSyncPacket(DroneModesPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(msg.droneUUID.hashCode()); // Попробуем найти по ID
        if (entity == null) {
            // Альтернативный поиск по UUID
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof DroneEntity drone && msg.droneUUID.equals(drone.getUUID())) {
                    entity = e;
                    break;
                }
            }
        }

        if (entity instanceof DroneEntity drone) {
            drone.setFlightMode(msg.flightMode);
            drone.setStabMode(msg.stabMode);
            drone.setZoomMode(msg.zoomMode);
        }
    }

    public static void handleActionBarMessagePacket(ActionBarMessagePacket msg) {
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(Component.translatable(msg.message), true);
        }
    }
}
