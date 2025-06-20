package ru.tesmio.drone.packets.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DistanceControlPacket;
import ru.tesmio.drone.packets.client.DroneControllerPacket;
import ru.tesmio.drone.packets.client.DroneModesSyncPacket;
import ru.tesmio.drone.packets.server.DroneViewPacket;

import java.util.UUID;
import java.util.function.Supplier;

public class DroneReconnectPacket {
    private final UUID droneUUID;

    public DroneReconnectPacket(UUID droneUUID) {
        this.droneUUID = droneUUID;
    }

    public static void encode(DroneReconnectPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.droneUUID);
    }

    public static DroneReconnectPacket decode(FriendlyByteBuf buf) {
        return new DroneReconnectPacket(buf.readUUID());
    }

    public static void handle(DroneReconnectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        //      }
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
                Entity drone = level.getEntity(msg.droneUUID);

                if (drone instanceof DroneEntity droneEntity) {

                    droneEntity.setControllerUUID(player.getUUID());
                    System.out.println("RESYNC" + droneEntity.getUUID() + "    "+ droneEntity.getControllerUUID());
                    // 1. Основной пакет контроллера
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new DroneControllerPacket(droneEntity.getUUID(), droneEntity.getControllerUUID()));

                    // 2. Синхронизация дистанций
                    int simChunks = player.getServer().getPlayerList().getSimulationDistance();
                    int viewChunks = player.getServer().getPlayerList().getViewDistance();
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new DistanceControlPacket(simChunks, viewChunks, droneEntity.getId()));

                    // 3. Синхронизация текущего состояния дрона
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new DroneViewPacket(droneEntity.getUUID(),
                                    droneEntity.getDroneYaw(),
                                    droneEntity.getDronePitch(),
                                    droneEntity.getDroneRoll()));

                    // 4. Синхронизация всех режимов дрона
                    // Создаем пакет для синхронизации режимов (если его нет, нужно создать)
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new DroneModesSyncPacket(droneEntity.getUUID(),
                                    droneEntity.getFlightMode(),
                                    droneEntity.getStabMode(),
                                    droneEntity.getZoomMode(), droneEntity.getVisionMode()));
                }
            });
        ctx.get().setPacketHandled(true);
    }
}
