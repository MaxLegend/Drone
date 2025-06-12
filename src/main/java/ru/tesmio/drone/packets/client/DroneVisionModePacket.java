package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.function.Supplier;

public class DroneVisionModePacket {
    public final DroneEntity.VisionMode mode;

    public DroneVisionModePacket(DroneEntity.VisionMode mode) {
        this.mode = mode;
    }

    public static void encode(DroneVisionModePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.mode.ordinal());
    }

    public static DroneVisionModePacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        return new DroneVisionModePacket(DroneEntity.VisionMode.values()[ordinal]);
    }

    public static void handle(DroneVisionModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneVisionModePacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
