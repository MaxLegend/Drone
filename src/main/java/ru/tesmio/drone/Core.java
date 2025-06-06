package ru.tesmio.drone;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.entity.DroneEntity;
import ru.tesmio.drone.entity.DroneItem;

import ru.tesmio.drone.entity.RemoteItem;
import ru.tesmio.drone.event.DroneClientEvent;
import ru.tesmio.drone.packets.DroneMovePacket;
import ru.tesmio.drone.packets.DroneSpeedUpdatePacket;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Core.MODID)
public class Core {

    public static final String MODID = "drone";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register("drone_entity",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                                    .sized(0.8f, 0.5f) // размеры дрона
                                    .clientTrackingRange(32)
                                    .updateInterval(1)
                                    .build("drone_entity"));
    public static RegistryObject<Item> ITEM_DRONE = ITEMS.register("drone_item", () -> new DroneItem(new Item.Properties()));
    public static RegistryObject<Item> ITEM_REMOTE = ITEMS.register("remote_item", () -> new RemoteItem(new Item.Properties()));

    private static int id = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1.0",
            s -> true,
            s -> true
    );


    public Core() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(id++, DroneMovePacket.class,
                DroneMovePacket::encode,
                DroneMovePacket::decode,
                DroneMovePacket::handle);
        CHANNEL.registerMessage(id++, DroneSpeedUpdatePacket.class,
                DroneSpeedUpdatePacket::toBytes,
                DroneSpeedUpdatePacket::new,
                DroneSpeedUpdatePacket::handle);
        }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
                event.accept(ITEM_DRONE);
                event.accept(ITEM_REMOTE);
            }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                KeyMapping[] mappings = new KeyMapping[]{DroneClientEvent.EXIT_CONTROL_KEY};
                for (KeyMapping km : mappings) net.minecraft.client.Minecraft.getInstance().options.keyMappings =
                        java.util.Arrays.copyOf(net.minecraft.client.Minecraft.getInstance().options.keyMappings,
                                net.minecraft.client.Minecraft.getInstance().options.keyMappings.length + 1);
                net.minecraft.client.Minecraft.getInstance().options.keyMappings[net.minecraft.client.Minecraft.getInstance().options.keyMappings.length - 1] = DroneClientEvent.EXIT_CONTROL_KEY;
            });
        }
    }
}
