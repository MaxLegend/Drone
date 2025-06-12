package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;


import java.util.function.Supplier;

public class DistanceControlPacket {
    public final int droneId;
    public float simChunks;
    public float viewChunks;
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
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDistanceControlPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
