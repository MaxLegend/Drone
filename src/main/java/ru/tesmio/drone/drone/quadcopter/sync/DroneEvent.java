package ru.tesmio.drone.drone.quadcopter.sync;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.registry.InitEntity;


@Mod.EventBusSubscriber(modid = Dronecraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DroneEvent {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(InitEntity.DRONE.get(), DroneEntity.createAttributes().build());

    }
}
