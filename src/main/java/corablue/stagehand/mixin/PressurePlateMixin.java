package corablue.stagehand.mixin;

import corablue.stagehand.world.ProxyStateManager;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.WeightedPressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin({PressurePlateBlock.class, WeightedPressurePlateBlock.class})
public abstract class PressurePlateMixin {

    @Inject(method = "getRedstoneOutput(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private void stagehand$proxyPressurePlateCheck(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {

        // If the plate already found someone natively, or it's the client side, do nothing.
        if (cir.getReturnValue() > 0 || !(world instanceof ServerWorld serverWorld)) return;

        ProxyStateManager stateManager = ProxyStateManager.getServerState(serverWorld);
        Set<BlockPos> sources = stateManager.getSourcesForTarget(pos);

        if (sources != null) {
            for (BlockPos sourcePos : sources) {

                // Create a generous 1x2x1 box that covers the proxy block AND the block above it
                Box generousBox = new Box(
                        sourcePos.getX(), sourcePos.getY(), sourcePos.getZ(),
                        sourcePos.getX() + 1.0, sourcePos.getY() + 2.0, sourcePos.getZ() + 1.0
                );

                // Ask the world if ANY entity is standing anywhere inside this massive box
                List<Entity> entities = world.getNonSpectatingEntities(Entity.class, generousBox);

                if (!entities.isEmpty()) {
                    // Force the plate to turn on!
                    cir.setReturnValue(15);
                    return;
                }
            }
        }
    }
}