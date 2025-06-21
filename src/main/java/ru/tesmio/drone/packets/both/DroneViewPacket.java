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
import ru.tesmio.drone.packets.client.DroneSyncViewPacket;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneViewPacket {
    public final UUID droneId;
    public final float yaw, pitch, roll;

    public DroneViewPacket(UUID droneId, float yaw, float pitch, float roll) {
        this.droneId = droneId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public static void encode(DroneViewPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.droneId);
        buf.writeShort((short)(pkt.yaw * 100));
        buf.writeShort((short)(pkt.pitch * 100));
        buf.writeShort((short)(pkt.roll * 100));

    }

    public static DroneViewPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        float yaw = buf.readShort() / 100.0f;
        float pitch = buf.readShort() / 100.0f;
        float roll = buf.readShort() / 100.0f;
        return new DroneViewPacket(id, yaw, pitch, roll);
    }
    public static void handle(DroneViewPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Level level = player.level();

            Entity e = ((ServerLevel) level).getEntity(pkt.droneId);
            if (e instanceof DroneEntity drone) {
                drone.applyView(pkt.yaw, pkt.pitch, pkt.roll);
                PacketDistributor.PacketTarget target = PacketDistributor.ALL.noArg();
                PacketSystem.CHANNEL.send(target, new DroneSyncViewPacket(drone.getId(), drone.getDroneYaw(), drone.getDronePitch(), drone.getDroneRoll()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
