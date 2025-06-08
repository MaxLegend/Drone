package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ActionBarMessagePacket {
    private final String message;

    public ActionBarMessagePacket(String message) {
        this.message = message;
    }

    public static void encode(ActionBarMessagePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.message);
    }

    public static ActionBarMessagePacket decode(FriendlyByteBuf buf) {
        return new ActionBarMessagePacket(buf.readUtf(32767));
    }

    public static void handle(ActionBarMessagePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // На клиенте
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(pkt.message), true); // true = actionbar
            }
        });
        ctx.get().setPacketHandled(true);
    }
}