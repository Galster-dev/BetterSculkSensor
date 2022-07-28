package ru.galster.bettersculksensor;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.sound.BlockSoundGroup;
import ru.galster.bettersculksensor.block.BetterSculkSensorBlock;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

public class ModBlocks {
    public static final BetterSculkSensorBlock BETTER_SCULK_SENSOR_BLOCK = new BetterSculkSensorBlock(
            FabricBlockSettings.of(Material.SCULK)
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.SCULK_SENSOR)
                    .luminance(1),
            8
    );
    public static BlockEntityType<BetterSculkSensorBlockEntity> BETTER_SCULK_SENSOR_BLOCK_ENTITY;
}
