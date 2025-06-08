package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneMovePacket  {
    public final UUID droneId;
    public final Vec3 movement;
    public final float yaw, pitch;

    public DroneMovePacket(UUID droneId, Vec3 movement, float yaw, float pitch) {
    this.droneId = droneId;
    this.movement = movement;
       this.yaw = yaw;
    this.pitch = pitch;
}

public static void encode(DroneMovePacket pkt, FriendlyByteBuf buf) {
    buf.writeUUID(pkt.droneId);
    buf.writeDouble(pkt.movement.x);
    buf.writeDouble(pkt.movement.y);
    buf.writeDouble(pkt.movement.z);
    buf.writeFloat(pkt.yaw);
    buf.writeFloat(pkt.pitch);
}

public static DroneMovePacket decode(FriendlyByteBuf buf) {
    UUID id = buf.readUUID();
    Vec3 mv = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    float yaw = buf.readFloat();
    float pitch = buf.readFloat();
    return new DroneMovePacket(id, mv, yaw, pitch);
}
    public static void handle(DroneMovePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Level level = player.level();

            Entity e = ((ServerLevel) level).getEntity(pkt.droneId);
            if (e instanceof DroneEntity drone) {
                drone.applyServerMovement(pkt.movement, pkt.yaw, pkt.pitch);
                drone.setYRot(pkt.yaw);
                drone.setYHeadRot(pkt.yaw);
                drone.setYBodyRot(pkt.yaw);
                drone.setXRot(pkt.pitch);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
