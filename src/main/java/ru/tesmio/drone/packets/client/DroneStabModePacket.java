package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.function.Supplier;

public class DroneStabModePacket {
    public final DroneEntity.StabMode mode;

    public DroneStabModePacket(DroneEntity.StabMode mode) {
        this.mode = mode;
    }

    public static void encode(DroneStabModePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.mode.ordinal());
    }

    public static DroneStabModePacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        return new DroneStabModePacket(DroneEntity.StabMode.values()[ordinal]);
    }

    public static void handle(DroneStabModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneStabModePacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
