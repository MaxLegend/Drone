package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.client.DroneModesPacket;

import java.util.UUID;
import java.util.function.Supplier;

import static ru.tesmio.drone.drone.quadcopter.DroneEntity.*;

public class DroneModesC2SP {
    public final UUID droneUUID;
    public final DroneEntity.FlightMode flightMode;
    public final DroneEntity.ZoomMode zoomMode;
    public final DroneEntity.VisionMode visionMode;
    public final DroneEntity.StabMode stabMode;
    public DroneModesC2SP(UUID droneUUID, DroneEntity.FlightMode flightMode,
            DroneEntity.StabMode stabMode, DroneEntity.ZoomMode zoomMode, DroneEntity.VisionMode visionMode) {
        this.droneUUID = droneUUID;
        this.flightMode = flightMode;
        this.stabMode = stabMode;
        this.zoomMode = zoomMode;
        this.visionMode = visionMode;
    }
    public static void encode(DroneModesC2SP msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeEnum(msg.flightMode);
        buf.writeEnum(msg.stabMode);
        buf.writeEnum(msg.zoomMode);
        buf.writeEnum(msg.visionMode);
    }

    public static DroneModesC2SP decode(FriendlyByteBuf buf) {
        UUID droneUUID = buf.readUUID();
        DroneEntity.FlightMode flightMode = buf.readEnum(DroneEntity.FlightMode.class);
        DroneEntity.StabMode stabMode = buf.readEnum(DroneEntity.StabMode.class);
        DroneEntity.ZoomMode zoomMode = buf.readEnum(DroneEntity.ZoomMode.class);
        DroneEntity.VisionMode visionMode = buf.readEnum(DroneEntity.VisionMode.class);
        return new DroneModesC2SP(droneUUID, flightMode, stabMode, zoomMode,visionMode);
    }
    public static void handle(DroneModesC2SP msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.droneUUID);
            if (entity instanceof DroneEntity drone) {
                if (drone.isLinked() && player.getUUID().equals(drone.getControllerUUID())) {
                    drone.getEntityData().set(DATA_FLIGHT_MODE, msg.flightMode.name());
                    drone.getEntityData().set(DATA_STAB_MODE, msg.stabMode.name());
                    drone.getEntityData().set(DATA_VISION_MODE, msg.visionMode.name());
                    drone.getEntityData().set(DATA_ZOOM_MODE, msg.zoomMode.name());
                }}
        });
        ctx.get().setPacketHandled(true);
    }
}
