package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.function.Supplier;

public class ActionBarMessagePacket {
    public final String message;

    public ActionBarMessagePacket(String message) {
        this.message = message;
    }

    public static void encode(ActionBarMessagePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.message);
    }

    public static ActionBarMessagePacket decode(FriendlyByteBuf buf) {
        return new ActionBarMessagePacket(buf.readUtf(32767));
    }

    public static void handle(ActionBarMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleActionBarMessagePacket(msg));
        ctx.get().setPacketHandled(true);
    }
}