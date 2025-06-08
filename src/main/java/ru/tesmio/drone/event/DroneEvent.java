package ru.tesmio.drone.event;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.drone.DroneEntity;

@Mod.EventBusSubscriber(modid = Core.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DroneEvent {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(Core.DRONE.get(), DroneEntity.createAttributes().build());
    }
}
