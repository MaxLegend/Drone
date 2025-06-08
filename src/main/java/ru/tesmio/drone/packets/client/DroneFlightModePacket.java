package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.EnumFlightMode;

import java.util.function.Supplier;

public class DroneFlightModePacket {
    private final EnumFlightMode mode;

    public DroneFlightModePacket(EnumFlightMode mode) {
        this.mode = mode;
    }

    public static void encode(DroneFlightModePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.mode.ordinal());
    }

    public static DroneFlightModePacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        return new DroneFlightModePacket(EnumFlightMode.values()[ordinal]);
    }

    public static void handle(DroneFlightModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                // Тут можно отобразить сообщение или обновить GUI
                mc.player.displayClientMessage(packet.mode.getDisplayText(), true); // для HUD
                // Или вызвать обновление GUI, если оно есть
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
