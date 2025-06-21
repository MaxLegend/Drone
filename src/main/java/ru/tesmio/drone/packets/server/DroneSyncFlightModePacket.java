package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import java.util.UUID;
import java.util.function.Supplier;

import static ru.tesmio.drone.drone.quadcopter.DroneEntity.DATA_FLIGHT_MODE;

public class DroneSyncFlightModePacket {
    public final UUID droneUUID;
    public final DroneEntity.FlightMode flightMode;

    public DroneSyncFlightModePacket(UUID droneUUID, DroneEntity.FlightMode flightMode) {
        this.droneUUID = droneUUID;
        this.flightMode = flightMode;
    }
    public static void encode(DroneSyncFlightModePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeEnum(msg.flightMode);
    }
    public static DroneSyncFlightModePacket decode(FriendlyByteBuf buf) {
        UUID droneUUID = buf.readUUID();
        DroneEntity.FlightMode flightMode = buf.readEnum(DroneEntity.FlightMode.class);
        return new DroneSyncFlightModePacket(droneUUID, flightMode);
    }
    public static void handle(DroneSyncFlightModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.droneUUID);

            if (entity instanceof DroneEntity drone) {
                // Проверка, что этот игрок управляет этим дроном
                if (drone.isLinked() && player.getUUID().equals(drone.getControllerUUID())) {
                    drone.getEntityData().set(DATA_FLIGHT_MODE, msg.flightMode.name());
                }}
        });
        ctx.get().setPacketHandled(true);
    }
}
