package ru.tesmio.drone.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;

public class InitEntity {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Dronecraft.MODID);

    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register("drone_entity",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                                    .sized(0.5f, 0.5f) // размеры дрона
                                    .clientTrackingRange(32)
                                    .updateInterval(1)
                                    .canSpawnFarFromPlayer()
                                    .build("drone_entity"));

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        ENTITIES.register(bus);
    }
}
