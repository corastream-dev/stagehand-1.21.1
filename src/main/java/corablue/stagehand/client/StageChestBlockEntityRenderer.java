package corablue.stagehand.client;

import corablue.stagehand.block.entity.StageChestBlockEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public class StageChestBlockEntityRenderer implements BlockEntityRenderer<StageChestBlockEntity> {

    private final ChestBlockEntityRenderer<ChestBlockEntity> vanillaRenderer;

    private class AnimatedDummyChest extends ChestBlockEntity {
        public StageChestBlockEntity targetEntity = null;

        public AnimatedDummyChest() {
            super(BlockPos.ORIGIN, Blocks.CHEST.getDefaultState());
        }

        @Override
        public float getAnimationProgress(float tickDelta) {
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
        MinecraftClient client = MinecraftClient.getInstance();

        // 1. Determine if the chest is open (Local Player Check)
        entity.clientIsOpen = false;
        if (client.player != null && client.player.currentScreenHandler instanceof corablue.stagehand.screen.StageChestScreenHandler handler) {
            if (handler.pos.equals(entity.getPos())) {
                entity.clientIsOpen = true;
            }
        }

        // 2. Calculate Framerate-Independent Time Delta
        long currentTime = System.currentTimeMillis();
        if (entity.lastAnimationTime == 0) entity.lastAnimationTime = currentTime;
        float deltaSeconds = (currentTime - entity.lastAnimationTime) / 1000.0f;
        entity.lastAnimationTime = currentTime;

        // 3. Animate the Angle (Visually Only)
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

        // 4. Bind the target and render
        this.dummyChest.targetEntity = entity;
        this.vanillaRenderer.render(this.dummyChest, tickDelta, matrices, vertexConsumers, light, overlay);
        this.dummyChest.targetEntity = null;
    }
}