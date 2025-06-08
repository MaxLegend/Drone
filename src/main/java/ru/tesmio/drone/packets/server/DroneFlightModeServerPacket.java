package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneFlightModeServerPacket {
    private final UUID droneId;
    public DroneFlightModeServerPacket(UUID droneId) {
        this.droneId = droneId;
    }

    public static void sendToServer(UUID droneId) {
        PacketSystem.CHANNEL.sendToServer(new DroneFlightModeServerPacket(droneId));
    }

    // ------------------------
    // Serialization
    public static void encode(DroneFlightModeServerPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneId);
    }

    public static DroneFlightModeServerPacket decode(FriendlyByteBuf buf) {
        UUID droneId = buf.readUUID();
        return new DroneFlightModeServerPacket(droneId);
    }

    public static void handle(DroneFlightModeServerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.droneId);

            if (entity instanceof DroneEntity drone) {
                if (drone.getControllingPlayerUUID() != null && drone.getControllingPlayerUUID().equals(player.getUUID())) {
                    drone.cycleFlightMode();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}