package corablue.stagehand.mixin;

import corablue.stagehand.world.ProxyStateManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
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

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void stagehand$proxyEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClient) return;

        // Grab the proxy manager
        corablue.stagehand.world.ProxyStateManager stateManager =
                corablue.stagehand.world.ProxyStateManager.getServerState((ServerWorld) world);

        BlockPos targetPos = stateManager.getTarget(pos);

        if (targetPos != null) {
            BlockState targetState = world.getBlockState(targetPos);
            Block targetBlock = targetState.getBlock();

            // CROSS-TRIGGER: Fire both events on the target block so it catches it no matter what!
            ((AbstractBlockInvoker) targetState.getBlock()).invokeOnEntityCollision(targetState, world, targetPos, entity);
            targetBlock.onSteppedOn(world, targetPos, targetState, entity);

            // Cancel the original non-solid block's logic
            ci.cancel();
        }
    }
}