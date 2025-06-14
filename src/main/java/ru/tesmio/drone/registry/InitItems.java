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
    public static RegistryObject<Item> CAMERA = ITEMS.register("camera", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> FLY_CONTROLLER = ITEMS.register("fly_controller", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> GPS_CONTROLLER = ITEMS.register("gps_controller", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> LEGS = ITEMS.register("legs", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> IR_CONTROLLER = ITEMS.register("ir_controller", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> MANUAL_CHIP = ITEMS.register("manual_chip", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> PROPELLER = ITEMS.register("propeller", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> SPEED_CHIP = ITEMS.register("speed_chip", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> SPEED_CHIP2 = ITEMS.register("speed_chip2", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> STAB_CHIP = ITEMS.register("stab_chip", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> TI_CONTROLLER = ITEMS.register("ti_controller", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> ZOOM1 = ITEMS.register("zoom1", () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> ZOOM2 = ITEMS.register("zoom2", () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
