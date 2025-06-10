package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;
import java.util.UUID;
import java.util.function.Supplier;



public class DroneDeathPacket {
    public final boolean enterControl;
    private final UUID droneUUID;
    public DroneDeathPacket(boolean enterControl, UUID droneUUID) {
        this.enterControl = enterControl;
        this.droneUUID = droneUUID;
    }

    public static void encode(DroneDeathPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enterControl);
        buf.writeUUID(msg.droneUUID);
    }

    public static DroneDeathPacket decode(FriendlyByteBuf buf) {
        boolean enter = buf.readBoolean();
        UUID uuid = buf.readUUID();
        return new DroneDeathPacket(enter, uuid);
    }

    public static void handle(DroneDeathPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDeathPacket(msg));
        ctx.get().setPacketHandled(true);
    }

}