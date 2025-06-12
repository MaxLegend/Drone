package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneControllerPacket {
    public final UUID droneUUID;
    public final UUID controllerUUID;

    public DroneControllerPacket(UUID droneUUID, UUID controllerUUID) {
        this.droneUUID = droneUUID;
        this.controllerUUID = controllerUUID;
    }

    public static void encode(DroneControllerPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeUUID(msg.controllerUUID);
    }

    public static DroneControllerPacket decode(FriendlyByteBuf buf) {
        return new DroneControllerPacket(buf.readUUID(), buf.readUUID());
    }

    public static void handle(DroneControllerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneControllerPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
