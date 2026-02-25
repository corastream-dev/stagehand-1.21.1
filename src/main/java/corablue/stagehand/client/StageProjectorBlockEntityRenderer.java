package corablue.stagehand.client;

import corablue.stagehand.StagehandClient;
import corablue.stagehand.block.entity.StageProjectorBlockEntity;
import corablue.stagehand.client.model.StageProjectorModel;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class StageProjectorBlockEntityRenderer implements BlockEntityRenderer<StageProjectorBlockEntity> {
    private final StageProjectorModel model;

    public StageProjectorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        // Point this to your new ModModelLayers class!
        this.model = new StageProjectorModel(ctx.getLayerModelPart(corablue.stagehand.client.model.ModModelLayers.PROJECTOR));
    }

    @Override
    public void render(StageProjectorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();

        // 1. Center the model in the block space and flip it upright (Blockbench models render upside down by default!)
        matrices.translate(0.5f, 1.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

        // 2. Rotate the model to face the direction the player placed it (Assuming your block has a FACING property)
        // Note: If you don't have a FACING property yet, you can comment these two lines out for now!
        float rot = entity.getCachedState().get(net.minecraft.state.property.Properties.HORIZONTAL_FACING).asRotation();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot));

        // 3. Apply the animated angle to the hinge!
        float smoothedAngle = net.minecraft.util.math.MathHelper.lerp(tickDelta, entity.prevHingeAngle, entity.hingeAngle);
        model.setHingeAngle(smoothedAngle);

        // 4. Draw it!
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(model.getLayer(StageProjectorModel.TEXTURE));
        model.render(matrices, vertexConsumer, light, overlay, 0xFFFFFFFF);

        matrices.pop();
    }
}