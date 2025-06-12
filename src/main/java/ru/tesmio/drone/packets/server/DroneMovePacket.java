package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneMovePacket  {
    public final UUID droneId;
    public final Vec3 movement;


    public DroneMovePacket(UUID droneId, Vec3 movement) {
    this.droneId = droneId;
    this.movement = movement;

}

public static void encode(DroneMovePacket pkt, FriendlyByteBuf buf) {
    buf.writeUUID(pkt.droneId);
    buf.writeShort((short)(pkt.movement.x * 100));
    buf.writeShort((short)(pkt.movement.y * 100));
    buf.writeShort((short)(pkt.movement.z * 100));
}

public static DroneMovePacket decode(FriendlyByteBuf buf) {
    UUID id = buf.readUUID();
    Vec3 mv = new Vec3(
            buf.readShort() / 100.0,
            buf.readShort() / 100.0,
            buf.readShort() / 100.0
    );
    return new DroneMovePacket(id, mv);
}
    public static void handle(DroneMovePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Level level = player.level();
            Entity e = ((ServerLevel) level).getEntity(pkt.droneId);
            if (e instanceof DroneEntity drone) {
                drone.applyMovement(pkt.movement);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
