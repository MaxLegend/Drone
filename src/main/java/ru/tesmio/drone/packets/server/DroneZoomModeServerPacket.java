package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneZoomModeServerPacket {
    private final UUID droneId;
    public DroneZoomModeServerPacket(UUID droneId) {
        this.droneId = droneId;
    }

    public static void sendToServer(UUID droneId) {
        PacketSystem.CHANNEL.sendToServer(new DroneZoomModeServerPacket(droneId));
    }

    public static void encode(DroneZoomModeServerPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneId);
    }

    public static DroneZoomModeServerPacket decode(FriendlyByteBuf buf) {
        UUID droneId = buf.readUUID();
        return new DroneZoomModeServerPacket(droneId);
    }

    public static void handle(DroneZoomModeServerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.droneId);

            if (entity instanceof DroneEntity drone) {
                if (drone.getControllerUUID() != null && drone.getControllerUUID().equals(player.getUUID())) {
                    drone.cycleZoomMode();
                }
            }

        });
        ctx.get().setPacketHandled(true);
    }
}
