package corablue.stagehand.client;

import corablue.stagehand.block.custom.StageChestBlock;
import corablue.stagehand.block.entity.StageChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class StageChestBlockEntityRenderer implements BlockEntityRenderer<StageChestBlockEntity> {

    private final ChestBlockEntityRenderer<ChestBlockEntity> vanillaRenderer;

    private class AnimatedDummyChest extends ChestBlockEntity {
        public StageChestBlockEntity targetEntity = null;

        public AnimatedDummyChest() {
            // Keep it as a vanilla chest so the renderer stays happy
            super(BlockPos.ORIGIN, Blocks.CHEST.getDefaultState());
        }

        @Override
        public float getAnimationProgress(float tickDelta) {
            // This is the only part the renderer actually needs from us
            return targetEntity != null ? targetEntity.lidAngle : 0.0f;
        }
    }

    private final AnimatedDummyChest dummyChest;

    public StageChestBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.vanillaRenderer = new ChestBlockEntityRenderer<>(ctx);
        this.dummyChest = new AnimatedDummyChest();
    }

    @Override
    public void render(StageChestBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

        // 1. Get the rotation from the block state
        BlockState state = entity.getCachedState();
        Direction direction = state.get(StageChestBlock.FACING);
        matrices.push();
        matrices.translate(0.5D, 0.5D, 0.5D);
        float rotation = direction.asRotation();
        // We rotate -rotation because Minecraft's coordinate system is inverted for degrees
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-rotation));
        matrices.translate(-0.5D, -0.5D, -0.5D);

        MinecraftClient client = MinecraftClient.getInstance();
        //Determine if the chest is open (Local Player Check)
        entity.clientIsOpen = false;
        if (client.player != null && client.player.currentScreenHandler instanceof corablue.stagehand.screen.StageChestScreenHandler handler) {
            if (handler.pos.equals(entity.getPos())) {
                entity.clientIsOpen = true;
            }
        }

        //Calculate Framerate-Independent Time Delta
        long currentTime = System.currentTimeMillis();
        if (entity.lastAnimationTime == 0) entity.lastAnimationTime = currentTime;
        float deltaSeconds = (currentTime - entity.lastAnimationTime) / 1000.0f;
        entity.lastAnimationTime = currentTime;

        //Animate the Angle (Visually Only)
        if (deltaSeconds > 0.5f) {
            entity.lidAngle = entity.clientIsOpen ? 1.0f : 0.0f;
        } else {
            float step = 2.0f * deltaSeconds;
            if (entity.clientIsOpen) {
                entity.lidAngle = Math.min(1.0f, entity.lidAngle + step);
            } else {
                entity.lidAngle = Math.max(0.0f, entity.lidAngle - step);
            }
        }

        this.dummyChest.targetEntity = entity;
        this.vanillaRenderer.render(this.dummyChest, tickDelta, matrices, vertexConsumers, light, overlay);
        this.dummyChest.targetEntity = null;

        matrices.pop();
    }
}