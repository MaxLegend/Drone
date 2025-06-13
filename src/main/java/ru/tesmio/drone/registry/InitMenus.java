package ru.tesmio.drone.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.drone.quadcopter.container.DroneEntityMenu;

public class InitMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Dronecraft.MODID);

//    public static final RegistryObject<MenuType<DroneEntityMenu>> DRONE_ENTITY_MENU =
//            MENUS.register("drone_entity_menu", () -> IForgeMenuType.create(DroneEntityMenu::new));

    public static final RegistryObject<MenuType<DroneEntityMenu>> DRONE_ENTITY_MENU =
            MENUS.register("drone_entity_menu", () ->
                    IForgeMenuType.create((windowId, inv, buf) -> {
                        // читаем ID сущности
                        int entityId = buf.readVarInt();
                        Entity e = inv.player.level().getEntity(entityId);
                        if (!(e instanceof DroneEntity drone)) {
                            throw new IllegalStateException("Expected ContainerEntity, got " + e);
                        }
                        // создаём меню сразу с готовой сущностью
                        return new DroneEntityMenu(windowId, inv, drone);
                    })
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
