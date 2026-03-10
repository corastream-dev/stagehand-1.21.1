package corablue.stagehand.event;

import corablue.stagehand.world.data.ProxyStateManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ProxyEvents {

    public static void register() {

// --- 1. THE CLICK INTERCEPTOR ---
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            net.minecraft.item.ItemStack stack = player.getStackInHand(hand);

            // Intercept the Wand!
            if (stack.getItem() instanceof corablue.stagehand.item.custom.BlockLinkerItem) {
                ActionResult result = stack.getItem().useOnBlock(
                        new net.minecraft.item.ItemUsageContext(player, hand, hitResult)
                );
                return result == ActionResult.PASS ? ActionResult.SUCCESS : result;
            }

            // FIX: Prevent the client desync double-fire!
            // We ignore the off-hand packet so the proxy only fires once per physical mouse click.
            if (hand == net.minecraft.util.Hand.OFF_HAND) {
                return ActionResult.PASS;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            ProxyStateManager stateManager = ProxyStateManager.getServerState(serverWorld);

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos targetPos = stateManager.getTarget(clickedPos);

            if (targetPos != null) {
                BlockState targetState = serverWorld.getBlockState(targetPos);

                net.minecraft.util.math.Vec3d hitOffset = hitResult.getPos().subtract(net.minecraft.util.math.Vec3d.of(clickedPos));
                net.minecraft.util.math.Vec3d newHitVec = net.minecraft.util.math.Vec3d.of(targetPos).add(hitOffset);
                BlockHitResult proxyHit = new BlockHitResult(newHitVec, hitResult.getSide(), targetPos, hitResult.isInsideBlock());

                ActionResult result = targetState.onUse(serverWorld, player, proxyHit);
                return result != ActionResult.PASS ? result : ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }
}