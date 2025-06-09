package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;


import java.util.function.Supplier;

public class DistanceControlPacket {
    private final int droneId;
    private float simChunks;
    private float viewChunks;
    public DistanceControlPacket(float simChunks, float viewChunks, int droneId) {
        this.simChunks = simChunks;
        this.viewChunks = viewChunks;
        this.droneId = droneId;
    }

    public static void encode(DistanceControlPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.simChunks);
        buf.writeFloat(msg.viewChunks);
        buf.writeInt(msg.droneId);
    }

    public static DistanceControlPacket decode(FriendlyByteBuf buf) {
        float simChunks = buf.readFloat();
        float viewChunks = buf.readFloat();
        int id = buf.readInt();
        return new DistanceControlPacket(simChunks, viewChunks, id);
    }

    public static void handle(DistanceControlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            Entity entity = level.getEntity(msg.droneId);
            if (entity instanceof DroneEntity drone) {
                drone.syncViewAndSimDistance(msg.viewChunks, msg.simChunks);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
