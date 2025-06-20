package ru.tesmio.drone.drone.quadcopter.sync;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

import ru.tesmio.drone.drone.quadcopter.RemoteItem;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DistanceControlPacket;
import ru.tesmio.drone.packets.client.DroneControllerPacket;

import java.util.UUID;

//TODO: разобраться почему после перезахода в мир не сохраняется uuid. Чистка кода, стилизация
@Mod.EventBusSubscriber
public class DroneSyncEvent {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof RemoteItem) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("DroneUUID")) {
                    UUID droneUUID = tag.getUUID("DroneUUID");

                    DroneEntity drone = player.level()
                                              .getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(256))
                                              .stream()
                                              .filter(d -> d.getUUID().equals(droneUUID))
                                              .findFirst()
                                              .orElse(null);

                    if (drone != null) {
                        //    System.out.println("onPlayerLogin drone.getUUID() " + drone.getUUID() + "  " + player.getUUID());
                        PacketSystem.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new DroneControllerPacket(drone.getUUID(), player.getUUID())
                        );
                        PacketSystem.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new DistanceControlPacket(
                                        player.server.getPlayerList().getSimulationDistance(),
                                        player.server.getPlayerList().getViewDistance(),
                                        drone.getId()
                                )
                        );

                        return;
                    }
                }
            }
        }
    }
}
