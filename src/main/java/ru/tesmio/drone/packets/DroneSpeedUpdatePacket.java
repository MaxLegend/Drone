package ru.tesmio.drone.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.entity.DroneEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneSpeedUpdatePacket {
    private final UUID droneId;
    private final float speed;

    public DroneSpeedUpdatePacket(UUID droneId, float speed) {
        this.droneId = droneId;
        this.speed = speed;
    }

    public DroneSpeedUpdatePacket(FriendlyByteBuf buf) {
        this.droneId = buf.readUUID();
        this.speed = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(droneId);
        buf.writeFloat(speed);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerLevel level = ctx.getSender().serverLevel();
            Entity entity = level.getEntity(droneId);
            if (entity instanceof DroneEntity drone) {
                drone.setSpeed(speed);
            }
        });
        ctx.setPacketHandled(true);
    }
}
