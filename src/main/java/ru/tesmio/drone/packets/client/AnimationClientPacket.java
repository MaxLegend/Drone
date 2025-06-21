package ru.tesmio.drone.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.packets.PacketClientHandler;
import ru.tesmio.drone.packets.both.AnimationSyncPacket;

import java.util.UUID;
import java.util.function.Supplier;

public class AnimationClientPacket {
    public final int droneId;
    public final float angularVelocity, bodyXRot, bodyZRot;
    public AnimationClientPacket(int droneId, float angularVelocity, float bodyXRot, float bodyZRot) {
        this.droneId = droneId;
        this.angularVelocity = angularVelocity;

        this.bodyXRot = bodyXRot;
        this.bodyZRot = bodyZRot;
    }
    public static void encode(AnimationClientPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.droneId);
        buf.writeFloat(msg.angularVelocity);
        buf.writeShort((short)(msg.bodyXRot * 100));
        buf.writeShort((short)(msg.bodyZRot * 100));
    }
    public static AnimationClientPacket decode(FriendlyByteBuf buf) {
        int droneId = buf.readInt();
        float angularVelocity = buf.readFloat();

        float bodyXRot = buf.readShort() / 100.0f;
        float bodyZRot = buf.readShort() / 100.0f;
        return new AnimationClientPacket(droneId, angularVelocity, bodyXRot, bodyZRot);
    }
    public static void handle(AnimationClientPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleAnimationClientPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
