package corablue.stagehand.item.custom;

import corablue.stagehand.item.BlockLinkerComponent;
import corablue.stagehand.item.ModComponents;
import corablue.stagehand.world.ProxyStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class BlockLinkerItem extends Item {

    public BlockLinkerItem(Settings settings) { super(settings); }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getWorld().isClient) return ActionResult.PASS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        ItemStack stack = context.getStack();

        // Sneak-click anywhere to clear the wand's memory
        if (player.isSneaking()) {
            stack.remove(ModComponents.BLOCK_LINKER);
            player.sendMessage(Text.literal("Blocklinker memory cleared.").formatted(Formatting.YELLOW), true);
            return ActionResult.SUCCESS;
        }

        BlockLinkerComponent data = stack.get(ModComponents.BLOCK_LINKER);

        if (data == null) {
            // STEP 1: Clicked a Source Block (No component exists yet)
            stack.set(ModComponents.BLOCK_LINKER, new BlockLinkerComponent(clickedPos));
            player.sendMessage(Text.literal("Source selected. Now click the target to mimic.").formatted(Formatting.GREEN), true);

        } else {
            // STEP 2: Clicked a Target Block (Component has the source coordinates)
            BlockPos sourcePos = data.sourcePos();

            if (sourcePos.equals(clickedPos)) {
                player.sendMessage(Text.literal("Cannot link a block to itself!").formatted(Formatting.RED), true);
                stack.remove(ModComponents.BLOCK_LINKER);
                return ActionResult.SUCCESS;
            }

            // Save the link to the world file
            ProxyStateManager stateManager = ProxyStateManager.getServerState(world);
            stateManager.addLink(sourcePos, clickedPos);

            player.sendMessage(Text.literal("Proxy Link established!").formatted(Formatting.GOLD), true);

            // Clear wand memory for the next link
            stack.remove(ModComponents.BLOCK_LINKER);
        }

        return ActionResult.SUCCESS;
    }
}