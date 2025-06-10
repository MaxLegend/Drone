package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.function.Supplier;

public class DroneFlightModePacket {
    public final DroneEntity.FlightMode mode;

    public DroneFlightModePacket(DroneEntity.FlightMode mode) {
        this.mode = mode;
    }

    public static void encode(DroneFlightModePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.mode.ordinal());
    }

    public static DroneFlightModePacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        return new DroneFlightModePacket(DroneEntity.FlightMode.values()[ordinal]);
    }

    public static void handle(DroneFlightModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneFlightModePacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
