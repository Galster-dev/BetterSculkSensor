package ru.galster.bettersculksensor;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.sound.BlockSoundGroup;
import ru.galster.bettersculksensor.block.BetterSculkSensorBlock;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

public class ModBlocks {
    public static final BetterSculkSensorBlock BETTER_SCULK_SENSOR_BLOCK = new BetterSculkSensorBlock(
            FabricBlockSettings.of(Material.AMETHYST)       // the whole point of purple blocks is not being a fan of purple
                    .strength(1.5f)                         // in 1.17 snapshots Mojang had textures of so called "calibrated sculk sensor"
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK) // and it's actually just a piece of amethyst block
                    .luminance(1),
            8
    );
    public static BlockEntityType<BetterSculkSensorBlockEntity> BETTER_SCULK_SENSOR_BLOCK_ENTITY;
}
