package ru.galster.bettersculksensor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

public class SculkModInitializer implements ModInitializer {
    public static final String NAMESPACE = "bettersculk";

    @Override
    public void onInitialize() {
        Registry.register(Registry.BLOCK, new Identifier(NAMESPACE, "better_sculk_sensor"), ModBlocks.BETTER_SCULK_SENSOR_BLOCK);
        Registry.register(Registry.ITEM, new Identifier(NAMESPACE, "better_sculk_sensor"), new BlockItem(ModBlocks.BETTER_SCULK_SENSOR_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));

        ModBlocks.BETTER_SCULK_SENSOR_BLOCK_ENTITY = Registry.register(
                Registry.BLOCK_ENTITY_TYPE,
                new Identifier(NAMESPACE, "better_sculk_sensor_entity"),
                FabricBlockEntityTypeBuilder.create(BetterSculkSensorBlockEntity::new, ModBlocks.BETTER_SCULK_SENSOR_BLOCK).build()
        );
    }
}
