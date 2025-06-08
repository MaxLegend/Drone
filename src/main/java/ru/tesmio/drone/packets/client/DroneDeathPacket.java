package ru.tesmio.drone.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static ru.tesmio.drone.drone.DroneController.mouseGrabbed;

public class DroneDeathPacket {
    private final boolean enterControl;
    private final UUID droneUUID;
    public DroneDeathPacket(boolean enterControl, UUID droneUUID) {
        this.enterControl = enterControl;
        this.droneUUID = droneUUID;
    }

    public static void encode(DroneDeathPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enterControl);
        buf.writeUUID(msg.droneUUID);
    }

    public static DroneDeathPacket decode(FriendlyByteBuf buf) {
        boolean enter = buf.readBoolean();
        UUID uuid = buf.readUUID();
        return new DroneDeathPacket(enter, uuid);
    }

    public static void handle(DroneDeathPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (!msg.enterControl) {

                mc.setCameraEntity(mc.player);

                if (mouseGrabbed) {

                    mc.mouseHandler.releaseMouse();
                    mouseGrabbed = false;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}