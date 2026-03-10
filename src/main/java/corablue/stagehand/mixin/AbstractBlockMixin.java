package corablue.stagehand.mixin;

import corablue.stagehand.world.data.ProxyStateManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {

    // Handles walking through blocks (Cobwebs, Sweet Berry Bushes, Pressure Plates)
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void stagehand$forwardEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            ProxyStateManager stateManager = ProxyStateManager.getServerState(serverWorld);
            BlockPos targetPos = stateManager.getTarget(pos);

            if (targetPos != null) {
                BlockState targetState = serverWorld.getBlockState(targetPos);
                // Call the collision method directly on the target block
                // Cast the block to our Invoker to bypass the protected access rule
                ((AbstractBlockInvoker) targetState.getBlock()).invokeOnEntityCollision(targetState, world, targetPos, entity);
                ci.cancel();
            }
        }
    }
}