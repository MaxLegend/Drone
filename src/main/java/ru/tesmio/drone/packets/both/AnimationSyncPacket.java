package ru.tesmio.drone.packets.both;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.AnimationClientPacket;
import ru.tesmio.drone.packets.client.DroneUpdateTiltsClient;

import java.util.UUID;
import java.util.function.Supplier;

public class AnimationSyncPacket {
    public final UUID droneUUID;
    public final float angularVelocity, bodyXRot, bodyZRot;
    public AnimationSyncPacket(UUID droneUUID, float angularVelocity, float bodyXRot, float bodyZRot) {
        this.droneUUID = droneUUID;
        this.angularVelocity = angularVelocity;

        this.bodyXRot = bodyXRot;
        this.bodyZRot = bodyZRot;
    }
    public static void encode(AnimationSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeFloat(msg.angularVelocity);
        buf.writeShort((short)(msg.bodyXRot * 100));
        buf.writeShort((short)(msg.bodyZRot * 100));
    }
    public static AnimationSyncPacket decode(FriendlyByteBuf buf) {
        UUID droneUUID = buf.readUUID();
        float angularVelocity = buf.readFloat();
        float bodyXRot = buf.readShort() / 100.0f;
        float bodyZRot = buf.readShort() / 100.0f;
        return new AnimationSyncPacket(droneUUID, angularVelocity, bodyXRot, bodyZRot);
    }
    public static void handle(AnimationSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            Level level = player.level();
            Entity e = ((ServerLevel) level).getEntity(msg.droneUUID);
            if (e instanceof DroneEntity drone) {
               // drone.updateTilt(msg.tiltX, msg.tiltZ);
                drone.applyAnimations();
                PacketDistributor.PacketTarget target = PacketDistributor.ALL.noArg();
                PacketSystem.CHANNEL.send(target, new AnimationClientPacket(drone.getId(), drone.angularVelocity, drone.bodyXRot, drone.bodyZRot));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
