package ru.tesmio.drone;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.tesmio.drone.drone.quadcopter.container.DroneEntityScreen;
import ru.tesmio.drone.packets.*;
import ru.tesmio.drone.registry.InitEntity;
import ru.tesmio.drone.registry.InitItems;
import ru.tesmio.drone.registry.InitMenus;
import ru.tesmio.drone.registry.InitTabs;
import ru.tesmio.drone.shader.RenderEntityMask;

@Mod(Dronecraft.MODID)
public class Dronecraft {
    //TODO: DefferedRegister вынести все в один класс.
    // Вынести в отдельный класс регистрацию предметов и сущностей. Сделать свою вкладку
    //
    public static final String MODID = "drone";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Dronecraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        InitTabs.register(bus);

        MinecraftForge.EVENT_BUS.register(this);

        InitEntity.register(bus);
        InitItems.register(bus);
        InitMenus.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::onClientSetup);
    }




    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketSystem.initPackets();
        }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(InitMenus.DRONE_ENTITY_MENU.get(), DroneEntityScreen::new);
            System.out.println("DroneEntityScreen registered.");
        });

        RenderEntityMask.initRenderTargets();
    }

}
