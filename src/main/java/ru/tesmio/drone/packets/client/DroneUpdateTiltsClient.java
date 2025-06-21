package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneUpdateTiltsClient {
    public final int droneId;
    public final float tiltX, tiltZ;

    public DroneUpdateTiltsClient(int droneId, float tiltX, float tiltZ) {
        this.droneId = droneId;
        this.tiltX = tiltX;
        this.tiltZ = tiltZ;
    }

    public static void encode(DroneUpdateTiltsClient msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.droneId);
        buf.writeShort((short)(msg.tiltX * 100));
        buf.writeShort((short)(msg.tiltZ * 100));
    }

    public static DroneUpdateTiltsClient decode(FriendlyByteBuf buf) {
        int droneId = buf.readInt();
        float tiltX = buf.readShort() / 100f;
        float tiltZ = buf.readShort() / 100f;
        return new DroneUpdateTiltsClient(droneId, tiltX, tiltZ);
    }

    public static void handle(DroneUpdateTiltsClient msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneUpdateTiltsClient(msg));
        ctx.get().setPacketHandled(true);
    }
}
