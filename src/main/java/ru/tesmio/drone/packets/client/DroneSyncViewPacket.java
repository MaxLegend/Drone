package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;
import java.util.function.Supplier;

public class DroneSyncViewPacket  {
    public final int droneId;
    public final float yaw, pitch, roll;

    public DroneSyncViewPacket(int droneId, float yaw, float pitch, float roll) {
        this.droneId = droneId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public static void encode(DroneSyncViewPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.droneId);
        buf.writeShort((short)(pkt.yaw * 100));
        buf.writeShort((short)(pkt.pitch * 100));
        buf.writeShort((short)(pkt.roll * 100));
    }

    public static DroneSyncViewPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        float yaw = buf.readShort() / 100.0f;
        float pitch = buf.readShort() / 100.0f;
        float roll = buf.readShort() / 100.0f;
        return new DroneSyncViewPacket(id, yaw, pitch, roll);
    }

    public static void handle(DroneSyncViewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneSyncViewPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
