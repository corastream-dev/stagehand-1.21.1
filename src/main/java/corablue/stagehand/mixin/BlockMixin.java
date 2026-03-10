package corablue.stagehand.mixin;

import corablue.stagehand.world.data.ProxyStateManager;
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

@Mixin(Block.class)
public abstract class BlockMixin {

    // Handles walking on top of blocks (Magma damage, Big Dripleaf tilting)
    @Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
    private void stagehand$forwardSteppedOn(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            ProxyStateManager stateManager = ProxyStateManager.getServerState(serverWorld);
            BlockPos targetPos = stateManager.getTarget(pos);

            if (targetPos != null) {
                BlockState targetState = serverWorld.getBlockState(targetPos);
                targetState.getBlock().onSteppedOn(world, targetPos, targetState, entity);
                ci.cancel();
            }
        }
    }
}