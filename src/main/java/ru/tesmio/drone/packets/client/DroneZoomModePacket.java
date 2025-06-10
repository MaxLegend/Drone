package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.function.Supplier;

public class DroneZoomModePacket {
    public final DroneEntity.ZoomMode mode;

    public DroneZoomModePacket(DroneEntity.ZoomMode mode) {
        this.mode = mode;
    }

    public static void encode(DroneZoomModePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.mode.ordinal());
    }

    public static DroneZoomModePacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        return new DroneZoomModePacket(DroneEntity.ZoomMode.values()[ordinal]);
    }

    public static void handle(DroneZoomModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneZoomModePacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
