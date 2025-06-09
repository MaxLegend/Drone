package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.DroneEntity;

import java.util.function.Supplier;

public class DroneStabModePacket {
    private final DroneEntity.StabMode mode;

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

    public static void handle(DroneStabModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                mc.player.displayClientMessage(packet.mode.getDisplayMode(), true);
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
