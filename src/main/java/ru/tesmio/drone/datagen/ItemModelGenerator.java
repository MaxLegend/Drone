package ru.tesmio.drone.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.registry.InitItems;

import java.util.LinkedHashMap;

public class ItemModelGenerator extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();

    public ItemModelGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Dronecraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ModelFile itemGenerated = getExistingFile(mcLoc("item/generated"));
        defaultItemGenerator(itemGenerated);

    }

    private void defaultItemGenerator(ModelFile parent) {
        for (RegistryObject<Item> item : InitItems.ITEMS.getEntries()) {
            String name = ForgeRegistries.ITEMS.getKey(item.get()).getPath();
            if (existingFileHelper.exists(modLoc("item/" + name), ItemModelProvider.MODEL)) {
                continue;
            }
            try {
                getBuilder(name)
                        .parent(parent)
                        .texture("layer0", modLoc("item/" + name));
            } catch (Exception e) {
                // Логируем ошибку, но продолжаем обработку других предметов
                Dronecraft.LOGGER.warn("Failed to generate model for item {}: {}", name, e.getMessage());
            }
            getBuilder(name)
                    .parent(parent)
                    .texture("layer0", modLoc("item/" + name));
        }
    }

}
