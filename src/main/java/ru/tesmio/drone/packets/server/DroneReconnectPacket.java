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
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Entity drone = level.getEntity(msg.droneUUID);
            if (drone instanceof DroneEntity droneEntity) {
                // Восстанавливаем связь
                droneEntity.setControllerUUID(player.getUUID());

                // Отправляем все необходимые пакеты для полной синхронизации
                ServerPlayer sp = player;

                // 1. Основной пакет контроллера
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneControllerPacket(droneEntity.getUUID(), droneEntity.getControllerUUID()));

                // 2. Синхронизация дистанций
                int simChunks = sp.getServer().getPlayerList().getSimulationDistance();
                int viewChunks = sp.getServer().getPlayerList().getViewDistance();
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DistanceControlPacket(simChunks, viewChunks, droneEntity.getId()));

                // 3. Синхронизация текущего состояния дрона
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneViewPacket(droneEntity.getUUID(),
                                droneEntity.getDroneYaw(),
                                droneEntity.getDronePitch(),
                                droneEntity.getDroneRoll()));

                // 4. Синхронизация всех режимов дрона
                // Создаем пакет для синхронизации режимов (если его нет, нужно создать)
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneModesSyncPacket(droneEntity.getUUID(),
                                droneEntity.getFlightMode(),
                                droneEntity.getStabMode(),
                                droneEntity.getZoomMode(),droneEntity.getVisionMode()));

            }
        });
        ctx.get().setPacketHandled(true);
    }
}
