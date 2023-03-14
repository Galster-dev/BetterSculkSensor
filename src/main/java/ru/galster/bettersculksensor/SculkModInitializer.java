package ru.galster.bettersculksensor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

public class SculkModInitializer implements ModInitializer {
    public static final String NAMESPACE = "bettersculk";

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier(NAMESPACE, "better_sculk_sensor"), ModBlocks.BETTER_SCULK_SENSOR_BLOCK);
        var blockItem = new BlockItem(ModBlocks.BETTER_SCULK_SENSOR_BLOCK, new FabricItemSettings());
        Registry.register(Registries.ITEM, new Identifier(NAMESPACE, "better_sculk_sensor"), blockItem);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(blockItem);
        });

        ModBlocks.BETTER_SCULK_SENSOR_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(NAMESPACE, "better_sculk_sensor_entity"),
                FabricBlockEntityTypeBuilder.create(BetterSculkSensorBlockEntity::new, ModBlocks.BETTER_SCULK_SENSOR_BLOCK).build()
        );
    }
}
