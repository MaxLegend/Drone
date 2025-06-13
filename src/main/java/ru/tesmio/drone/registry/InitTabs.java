package ru.tesmio.drone.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.Dronecraft;

public class InitTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dronecraft.MODID);

    public static final RegistryObject<CreativeModeTab> DRONECRAFT_TAB = CREATIVE_TABS.register("tab_dronecraft",
            () -> CreativeModeTab.builder()
                                 .icon(() -> new ItemStack(InitItems.ITEM_DRONE.get()))
                                 .title(Component.translatable("itemGroup.tab_dronecraft"))
                                 .displayItems((parameters, output) -> {
                                     InitItems.ITEMS.getEntries().stream()
                                                     .map(RegistryObject::get)
                                                     .forEach(output::accept);
                                 })
                                 .build()
    );
    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
