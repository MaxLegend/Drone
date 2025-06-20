package ru.tesmio.drone.packets.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketClientHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneInventorySyncPacket {
    public final UUID droneUUID;
    public final CompoundTag inventoryTag;

    public DroneInventorySyncPacket(UUID droneUUID, CompoundTag inventoryTag) {
        this.droneUUID = droneUUID;
        this.inventoryTag = inventoryTag;
    }

    public static void encode(DroneInventorySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
        buf.writeNbt(msg.inventoryTag);
    }

    public static DroneInventorySyncPacket decode(FriendlyByteBuf buf) {
        return new DroneInventorySyncPacket(buf.readUUID(), buf.readNbt());
    }

    public static void handle(DroneInventorySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PacketClientHandler.handleDroneInventorySyncPacket(msg));
        ctx.get().setPacketHandled(true);
    }
}
