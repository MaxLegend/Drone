package ru.tesmio.drone.packets.both;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DroneUpdateTiltsClient;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneUpdateTilts {
    public final UUID droneUUID;
    public final float tiltX, tiltZ;
    public DroneUpdateTilts(UUID droneUUID, float tiltX, float tiltZ) {
        this.droneUUID = droneUUID;
        this.tiltX = tiltX;
        this.tiltZ = tiltZ;
    }
    public static void encode(DroneUpdateTilts msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeShort((short)(msg.tiltX * 100));
        buf.writeShort((short)(msg.tiltZ * 100));
    }
    public static DroneUpdateTilts decode(FriendlyByteBuf buf) {
        UUID droneUUID = buf.readUUID();
        float tiltX = buf.readShort() / 100.0f;
        float tiltZ = buf.readShort() / 100.0f;
        return new DroneUpdateTilts(droneUUID, tiltX, tiltZ);
    }
    public static void handle(DroneUpdateTilts msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Level level = player.level();
            Entity e = ((ServerLevel) level).getEntity(msg.droneUUID);
            if (e instanceof DroneEntity drone) {
                drone.updateTilt(msg.tiltX, msg.tiltZ);

                PacketDistributor.PacketTarget target = PacketDistributor.ALL.noArg();
                PacketSystem.CHANNEL.send(target, new DroneUpdateTiltsClient(drone.getId(), drone.getTiltX(), drone.getTiltZ()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
