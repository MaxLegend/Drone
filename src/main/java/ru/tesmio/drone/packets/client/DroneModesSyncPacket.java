package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneModesSyncPacket {
    public final UUID droneUUID;
    public final DroneEntity.FlightMode flightMode;
    public final DroneEntity.StabMode stabMode;
    public final DroneEntity.ZoomMode zoomMode;
    public final DroneEntity.VisionMode visionMode;
    public DroneModesSyncPacket(UUID droneUUID, DroneEntity.FlightMode flightMode,
            DroneEntity.StabMode stabMode, DroneEntity.ZoomMode zoomMode, DroneEntity.VisionMode visionMode) {
        this.droneUUID = droneUUID;
        this.flightMode = flightMode;
        this.stabMode = stabMode;
        this.zoomMode = zoomMode;
        this.visionMode = visionMode;
    }

    public static void encode(DroneModesSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeEnum(msg.flightMode);
        buf.writeEnum(msg.stabMode);
        buf.writeEnum(msg.zoomMode);
        buf.writeEnum(msg.visionMode);
    }

    public static DroneModesSyncPacket decode(FriendlyByteBuf buf) {
        UUID droneUUID = buf.readUUID();
        DroneEntity.FlightMode flightMode = buf.readEnum(DroneEntity.FlightMode.class);
        DroneEntity.StabMode stabMode = buf.readEnum(DroneEntity.StabMode.class);
        DroneEntity.ZoomMode zoomMode = buf.readEnum(DroneEntity.ZoomMode.class);
        DroneEntity.VisionMode visionMode = buf.readEnum(DroneEntity.VisionMode.class);
        return new DroneModesSyncPacket(droneUUID, flightMode, stabMode, zoomMode,visionMode);
    }

    public static void handle(DroneModesSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneModesSyncPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}