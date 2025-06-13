package ru.tesmio.drone.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneItem;
import ru.tesmio.drone.drone.quadcopter.RemoteItem;

public class InitItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Dronecraft.MODID);

    public static RegistryObject<Item> ITEM_DRONE = ITEMS.register("drone_item", () -> new DroneItem(new Item.Properties()));
    public static RegistryObject<Item> ITEM_REMOTE = ITEMS.register("remote_item", () -> new RemoteItem(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
