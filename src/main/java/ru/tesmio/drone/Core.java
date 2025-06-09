package ru.tesmio.drone;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.droneold.DroneItem;

import ru.tesmio.drone.droneold.RemoteItem;

import ru.tesmio.drone.packets.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Core.MODID)
public class Core {

    public static final String MODID = "drone";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register("drone_entity",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                                    .sized(0.5f, 0.5f) // размеры дрона
                                    .clientTrackingRange(32)
                                    .updateInterval(1)
                                    .canSpawnFarFromPlayer()
                                    .build("drone_entity"));
    public static RegistryObject<Item> ITEM_DRONE = ITEMS.register("drone_item", () -> new DroneItem(new Item.Properties()));
    public static RegistryObject<Item> ITEM_REMOTE = ITEMS.register("remote_item", () -> new RemoteItem(new Item.Properties()));




    public Core() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }




    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketSystem.initPackets();
        }

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

        }
    }
}
