package ru.galster.bettersculksensor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import ru.galster.bettersculksensor.ModBlocks;
import ru.galster.bettersculksensor.client.renderer.BetterSculkSensorEntityRenderer;

public class ClientSculkModInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // make background transparent
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETTER_SCULK_SENSOR_BLOCK, RenderLayer.getCutout());
        BlockEntityRendererRegistry.register(ModBlocks.BETTER_SCULK_SENSOR_BLOCK_ENTITY, ctx -> new BetterSculkSensorEntityRenderer());
    }
}
