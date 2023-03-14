package ru.galster.bettersculksensor.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

public class BetterSculkSensorEntityRenderer implements BlockEntityRenderer<BetterSculkSensorBlockEntity> {
    public BetterSculkSensorEntityRenderer() {}
    @Override
    public void render(BetterSculkSensorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if(entity.getClickedItem() == null ||
                entity.getWorld() == null
        ) {
            return;
        }

        matrices.push();

        if(entity.shouldRenderFlat()) {
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getAngle()));
        } else {
            matrices.translate(0.5, 0.625, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getAngle()));
        }

        // Scale item because fixed mode is too big
        matrices.scale(0.5f, 0.5f, 0.5f);

        int lightAbove = WorldRenderer.getLightmapCoordinates(entity.getWorld(), entity.getPos().up());
        MinecraftClient.getInstance().getItemRenderer().renderItem(entity.getClickedItem(), ModelTransformation.Mode.FIXED, lightAbove, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0);

        matrices.pop();
    }
}
